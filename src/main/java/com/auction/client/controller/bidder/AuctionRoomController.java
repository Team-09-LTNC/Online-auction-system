package com.auction.client.controller.bidder;

import com.auction.client.networkclient.ClientSocket;
import com.auction.client.util.AuctionTimeUtil;
import com.auction.client.util.ImageCacheManager;
import com.auction.common.dto.AuctionDTOs;
import com.auction.common.enums.ActionType;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

public class AuctionRoomController implements Initializable {
    private static final String SELLER_SELF_BID_MESSAGE = "Không được tự bid sản phẩm của chính mình.";
    private static final String AUTO_BID_REGISTER_TEXT = "ĐĂNG KÝ AUTO-BID";
    private static final String AUTO_BID_REMOVE_TEXT = "XÓA AUTO-BID";
    private static final double IMAGE_FRAME_HEIGHT = 260.0;

    @FXML private Label lblProductName;
    @FXML private Label lblDescription;
    @FXML private Label lblCategory;
    @FXML private Label lblStartingPrice;
    @FXML private Label lblBidIncrement;
    @FXML private Label lblBuyNowPrice;
    @FXML private Label lblAntiSniping;

    @FXML private Label lblCurrentPrice;
    @FXML private Label lblCountdown;
    @FXML private Label lblLeader;
    @FXML private ListView<String> lvBidHistory;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnPlaceBid;
    @FXML private TextField txtMaxAutoBid;
    @FXML private TextField txtAutoBidStep;
    @FXML private Button btnEnableAutoBid;
    @FXML private ImageView imgProduct;
    @FXML private StackPane imageFrame;

    @FXML private AreaChart<String, Number> priceChart;
    @FXML private CategoryAxis timeAxis;
    @FXML private NumberAxis priceAxis;
    private XYChart.Series<String, Number> priceSeries;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm-dd");
    private final DateTimeFormatter historyTimeFormatter = DateTimeFormatter.ofPattern("dd HH:mm:ss");

    private int totalSeconds = 0;
    private Timeline countdownTimeline;
    private int currentAuctionId = -1;
    private boolean isAuctionStarted = false;
    private String currentStatus = "OPEN";
    private Long currentBuyNowPrice;
    private int currentSellerId = -1;
    private long currentDisplayedPrice = 0;
    private long currentBidIncrement = 0;
    private boolean hasCurrentWinner = false;
    private boolean currentUserOwnsAuction = false;
    private boolean ownerBidWarningShown = false;
    private boolean autoBidActive = false;
    private final Set<String> displayedBidKeys = new HashSet<>();
    private Image observedProductImage;
    private final ChangeListener<Number> productImageDimensionListener =
            (obs, oldValue, newValue) -> updateProductImageViewport();

    private static long extractMoneyValue(String text) {
        return parseMoneyValue(text);
    }

    private static long parseMoneyValue(String text) {
        String digits = getDigitsOnly(text);
        if (digits.isEmpty()) {
            throw new NumberFormatException("Empty money value");
        }
        return Long.parseLong(digits);
    }

    private static String getDigitsOnly(String text) {
        return text == null ? "" : text.replaceAll("\\D", "");
    }

    private static String formatMoney(long value) {
        return formatDigitsWithDots(String.valueOf(value));
    }

    private static String formatVnd(long value) {
        return formatMoney(value) + " đ";
    }

    private static String formatDigitsWithDots(String digits) {
        if (digits == null || digits.isBlank()) {
            return "";
        }

        String normalized = digits.replaceFirst("^0+(?!$)", "");
        StringBuilder formatted = new StringBuilder();
        int firstGroupLength = normalized.length() % 3;
        if (firstGroupLength == 0) {
            firstGroupLength = 3;
        }

        formatted.append(normalized, 0, firstGroupLength);
        for (int i = firstGroupLength; i < normalized.length(); i += 3) {
            formatted.append('.').append(normalized, i, i + 3);
        }
        return formatted.toString();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        com.auction.client.networkclient.PushHandler.currentRoomController = this;

        setupProductImageFill();
        installMoneyFormatter(txtBidAmount);
        installMoneyFormatter(txtMaxAutoBid);
        installMoneyFormatter(txtAutoBidStep);

        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Giá đấu");
        if (priceChart != null) {
            priceChart.getData().add(priceSeries);
        }
    }

    private void setupProductImageFill() {
        if (imgProduct == null || imageFrame == null) {
            return;
        }

        imageFrame.setMinHeight(IMAGE_FRAME_HEIGHT);
        imageFrame.setPrefHeight(IMAGE_FRAME_HEIGHT);
        imageFrame.setMaxHeight(IMAGE_FRAME_HEIGHT);
        imageFrame.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(imageFrame, Priority.NEVER);

        imgProduct.setManaged(false);
        imgProduct.setPreserveRatio(false);
        imgProduct.fitWidthProperty().bind(imageFrame.widthProperty());
        imgProduct.fitHeightProperty().bind(imageFrame.heightProperty());

        Rectangle clip = new Rectangle();
        clip.arcWidthProperty().set(20);
        clip.arcHeightProperty().set(20);
        clip.widthProperty().bind(imageFrame.widthProperty());
        clip.heightProperty().bind(imageFrame.heightProperty());
        imageFrame.setClip(clip);

        imageFrame.widthProperty().addListener(productImageDimensionListener);
        imageFrame.heightProperty().addListener(productImageDimensionListener);
        imgProduct.imageProperty().addListener((obs, oldImage, newImage) -> {
            observeProductImage(oldImage, newImage);
            updateProductImageViewport();
        });
    }

    private void observeProductImage(Image oldImage, Image newImage) {
        if (oldImage != null && oldImage == observedProductImage) {
            oldImage.widthProperty().removeListener(productImageDimensionListener);
            oldImage.heightProperty().removeListener(productImageDimensionListener);
        }
        observedProductImage = newImage;
        if (newImage != null) {
            newImage.widthProperty().addListener(productImageDimensionListener);
            newImage.heightProperty().addListener(productImageDimensionListener);
        }
    }

    private void updateProductImageViewport() {
        if (imgProduct == null || imageFrame == null || imgProduct.getImage() == null) {
            return;
        }

        Image image = imgProduct.getImage();
        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();
        double frameWidth = imageFrame.getWidth();
        double frameHeight = imageFrame.getHeight();
        if (imageWidth <= 0 || imageHeight <= 0 || frameWidth <= 0 || frameHeight <= 0) {
            return;
        }

        double frameRatio = frameWidth / frameHeight;
        double imageRatio = imageWidth / imageHeight;
        double viewportWidth = imageWidth;
        double viewportHeight = imageHeight;
        if (imageRatio > frameRatio) {
            viewportWidth = imageHeight * frameRatio;
        } else {
            viewportHeight = imageWidth / frameRatio;
        }

        double x = (imageWidth - viewportWidth) / 2;
        double y = (imageHeight - viewportHeight) / 2;
        imgProduct.setViewport(new Rectangle2D(x, y, viewportWidth, viewportHeight));
    }

    private void installMoneyFormatter(TextField textField) {
        if (textField == null) {
            return;
        }

        textField.setTextFormatter(new TextFormatter<String>(change -> {
            String proposedText = change.getControlNewText();
            String digits = getDigitsOnly(proposedText);
            String formatted = formatDigitsWithDots(digits);

            int proposedCaret = Math.max(0, Math.min(change.getCaretPosition(), proposedText.length()));
            int digitsBeforeCaret = countDigits(proposedText.substring(0, proposedCaret));
            int newCaret = calculateCaretPosition(formatted, digitsBeforeCaret);

            change.setRange(0, change.getControlText().length());
            change.setText(formatted);
            change.setCaretPosition(newCaret);
            change.setAnchor(newCaret);
            return change;
        }));
    }

    private static int countDigits(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    private static int calculateCaretPosition(String text, int digitsBeforeCaret) {
        if (digitsBeforeCaret <= 0) {
            return 0;
        }

        int digitsSeen = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) {
                digitsSeen++;
                if (digitsSeen == digitsBeforeCaret) {
                    return i + 1;
                }
            }
        }
        return text.length();
    }

    public void initData(int auctionId, String imageUrl) {
        initData(auctionId, imageUrl, null);
    }

    public void initData(int auctionId, String imageUrl, JsonObject auctionSnapshot) {
        this.currentAuctionId = auctionId;
        if (auctionSnapshot != null) {
            applyAuctionSnapshot(auctionSnapshot, imageUrl);
        } else {
            loadProductImage(imageUrl);
        }

        JsonObject joinReq = new JsonObject();
        joinReq.addProperty("type", ActionType.JOIN_AUCTION);
        joinReq.addProperty("auctionId", auctionId);
        joinReq.addProperty("requestId", java.util.UUID.randomUUID().toString());
        ClientSocket.getInstance().sendJsonRequest(joinReq, null, null);

        refreshAuctionState();
        loadBidHistoryFromServer();
    }

    private void applyAuctionSnapshot(JsonObject snapshot, String fallbackImageUrl) {
        String imageUrl = getString(snapshot, "imageUrl", fallbackImageUrl);
        String imageThumbUrl = getString(snapshot, "imageThumbUrl", imageUrl);
        loadProductImage(imageUrl, imageThumbUrl);

        if (lblProductName != null) {
            lblProductName.setText(getString(snapshot, "itemName", getString(snapshot, "name", "Sản phẩm")));
        }
        if (lblDescription != null) {
            lblDescription.setText(getString(snapshot, "description", "Đang đồng bộ mô tả..."));
        }
        if (lblCategory != null) {
            lblCategory.setText("Danh mục: " + getString(snapshot, "category", "Khác"));
        }

        long startingPrice = getLong(snapshot, "startingPrice", getLong(snapshot, "currentPrice", 0));
        long bidIncrement = getLong(snapshot, "bidIncrement", 0);
        currentBidIncrement = bidIncrement;
        if (lblStartingPrice != null) {
            lblStartingPrice.setText(formatVnd(startingPrice));
        }
        if (lblBidIncrement != null) {
            lblBidIncrement.setText(formatVnd(bidIncrement));
        }

        currentBuyNowPrice = hasValue(snapshot, "buyNowPrice") ? snapshot.get("buyNowPrice").getAsLong() : null;
        if (lblBuyNowPrice != null) {
            lblBuyNowPrice.setText(currentBuyNowPrice != null ? formatVnd(currentBuyNowPrice) : "Không hỗ trợ");
        }
        if (lblAntiSniping != null) {
            lblAntiSniping.setText(getBoolean(snapshot, "antiSnipingEnabled", false) ? "Có" : "Không");
        }

        long displayPrice = getLong(snapshot, "currentHighestBid", getLong(snapshot, "currentPrice", startingPrice));
        currentDisplayedPrice = displayPrice;
        hasCurrentWinner = false;
        if (lblCurrentPrice != null) {
            lblCurrentPrice.setText(formatVnd(displayPrice));
        }
        if (lblLeader != null) {
            lblLeader.setText("Chưa có ai đặt giá");
        }

        currentSellerId = (int) getLong(snapshot, "sellerId", -1);
        currentUserOwnsAuction =
                "SELLER".equalsIgnoreCase(com.auction.client.controller.auth.UserSession.getCurrentRole())
                        && currentSellerId == com.auction.client.controller.auth.UserSession.getUserId();

        String rawStartTime = getString(snapshot, "startTime", null);
        String rawEndTime = getString(snapshot, "endTime", null);
        String rawServerNow = getString(snapshot, "serverNow", null);
        AuctionTimeUtil.AuctionState state = AuctionTimeUtil.calculateState(rawStartTime, rawEndTime, rawServerNow);
        totalSeconds = (int) getLong(snapshot, "countdownSeconds", state.countdownSeconds);
        currentStatus = getString(snapshot, "displayStatus", getString(snapshot, "status", state.finalStatus));
        applyInteractionState(false);
    }

    private void applyInteractionState(boolean showOwnerWarning) {
        if ("OPEN".equalsIgnoreCase(currentStatus) || "RUNNING".equalsIgnoreCase(currentStatus)) {
            isAuctionStarted = "RUNNING".equalsIgnoreCase(currentStatus);
            if (btnPlaceBid != null) {
                btnPlaceBid.setDisable(!isAuctionStarted || currentUserOwnsAuction);
                btnPlaceBid.setText(isAuctionStarted ? "ĐẶT GIÁ" : "CHỜ MỞ BÁN");
            }
            if (txtBidAmount != null) {
                txtBidAmount.setEditable(isAuctionStarted && !currentUserOwnsAuction);
            }
            if (txtMaxAutoBid != null) {
                txtMaxAutoBid.setEditable(isAuctionStarted && !currentUserOwnsAuction);
            }
            if (btnEnableAutoBid != null) {
                btnEnableAutoBid.setDisable(!isAuctionStarted || currentUserOwnsAuction);
            }
            if (currentUserOwnsAuction && showOwnerWarning && !ownerBidWarningShown) {
                ownerBidWarningShown = true;
                showAlert("Không thể đặt giá", SELLER_SELF_BID_MESSAGE);
            }
            startCountdown();
        } else {
            setExpiredUI();
        }
    }

    private boolean hasValue(JsonObject obj, String key) {
        return obj != null && obj.has(key) && !obj.get(key).isJsonNull();
    }

    private String getString(JsonObject obj, String key, String fallback) {
        return hasValue(obj, key) ? obj.get(key).getAsString() : fallback;
    }

    private long getLong(JsonObject obj, String key, long fallback) {
        if (!hasValue(obj, key)) {
            return fallback;
        }
        try {
            return obj.get(key).getAsLong();
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private boolean getBoolean(JsonObject obj, String key, boolean fallback) {
        if (!hasValue(obj, key)) {
            return fallback;
        }
        try {
            return obj.get(key).getAsBoolean();
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private void refreshAuctionState() {
        if (currentAuctionId == -1) {
            return;
        }

        JsonObject getReq = new JsonObject();
        getReq.addProperty("type", ActionType.GET_AUCTION_BY_ID);
        getReq.addProperty("auctionId", currentAuctionId);
        getReq.addProperty("requestId", java.util.UUID.randomUUID().toString());

        ClientSocket.getInstance().sendJsonRequest(getReq, ActionType.GET_AUCTION_BY_ID, response ->
                Platform.runLater(() -> {
                    if (!(response.has("success") && response.get("success").getAsBoolean())) {
                        return;
                    }

                    JsonObject data = response.getAsJsonObject("data");
                    JsonObject itemData = data.getAsJsonObject("item");

                    currentSellerId = itemData.has("sellerId") && !itemData.get("sellerId").isJsonNull()
                            ? itemData.get("sellerId").getAsInt()
                            : -1;
                    currentUserOwnsAuction =
                            "SELLER".equalsIgnoreCase(com.auction.client.controller.auth.UserSession.getCurrentRole())
                                    && currentSellerId == com.auction.client.controller.auth.UserSession.getUserId();

                    if (lblProductName != null) {
                        lblProductName.setText(itemData.has("name") ? itemData.get("name").getAsString() : "Sản phẩm");
                    }
                    String imageUrl = itemData.has("imageUrl") && !itemData.get("imageUrl").isJsonNull()
                            ? itemData.get("imageUrl").getAsString()
                            : null;
                    String imageThumbUrl = itemData.has("imageThumbUrl") && !itemData.get("imageThumbUrl").isJsonNull()
                            ? itemData.get("imageThumbUrl").getAsString()
                            : imageUrl;
                    loadProductImage(imageUrl, imageThumbUrl);

                    if (lblDescription != null) {
                        lblDescription.setText(itemData.has("description") ? itemData.get("description").getAsString() : "Không có mô tả.");
                    }
                    if (lblCategory != null) {
                        lblCategory.setText("Danh mục: " + (itemData.has("category") ? itemData.get("category").getAsString() : "Khác"));
                    }

                    long startingPrice = itemData.has("startingPrice") ? itemData.get("startingPrice").getAsLong() : 0;
                    long bidIncrement = itemData.has("bidIncrement") ? itemData.get("bidIncrement").getAsLong() : 0;
                    currentBidIncrement = bidIncrement;
                    if (lblStartingPrice != null) {
                        lblStartingPrice.setText(formatVnd(startingPrice));
                    }
                    if (lblBidIncrement != null) {
                        lblBidIncrement.setText(formatVnd(bidIncrement));
                    }

                    currentBuyNowPrice = data.has("buyNowPrice") && !data.get("buyNowPrice").isJsonNull()
                            ? data.get("buyNowPrice").getAsLong()
                            : null;
                    if (lblBuyNowPrice != null) {
                        lblBuyNowPrice.setText(currentBuyNowPrice != null ? formatVnd(currentBuyNowPrice) : "Không hỗ trợ");
                    }

                    if (lblAntiSniping != null) {
                        boolean antiSnipingEnabled = data.has("antiSnipingEnabled")
                                && !data.get("antiSnipingEnabled").isJsonNull()
                                && data.get("antiSnipingEnabled").getAsBoolean();
                        lblAntiSniping.setText(antiSnipingEnabled ? "Có" : "Không");
                    }

                    long displayPrice = data.has("currentHighestBid") && !data.get("currentHighestBid").isJsonNull()
                            ? data.get("currentHighestBid").getAsLong() : startingPrice;
                    currentDisplayedPrice = displayPrice;

                    String leaderText = "Chưa có ai đặt giá";
                    boolean hasWinner = false;
                    JsonObject userObj = data.has("currentWinner") && !data.get("currentWinner").isJsonNull() ? data.getAsJsonObject("currentWinner")
                            : data.has("currentHighestBidder") && !data.get("currentHighestBidder").isJsonNull() ? data.getAsJsonObject("currentHighestBidder")
                            : data.has("highestBidder") && !data.get("highestBidder").isJsonNull() ? data.getAsJsonObject("highestBidder") : null;

                    if (userObj != null) {
                        leaderText = resolveUserDisplayName(userObj);
                        hasWinner = true;
                    }
                    hasCurrentWinner = hasWinner;

                    if (lblCurrentPrice != null) {
                        lblCurrentPrice.setText(formatVnd(displayPrice));
                    }
                    if (lblLeader != null) {
                        lblLeader.setText(hasWinner ? "Người dẫn đầu: " + leaderText : leaderText);
                    }

                    String rawStartTime = data.has("startTime") && !data.get("startTime").isJsonNull() ? data.get("startTime").getAsString() : "";
                    String rawEndTime = data.has("endTime") && !data.get("endTime").isJsonNull() ? data.get("endTime").getAsString() : "";
                    String rawServerNow = response.has("serverNow") && !response.get("serverNow").isJsonNull() ? response.get("serverNow").getAsString() : null;
                    String statusFromServer = data.has("status") && !data.get("status").isJsonNull()
                            ? data.get("status").getAsString()
                            : (data.has("storedStatus") && !data.get("storedStatus").isJsonNull() ? data.get("storedStatus").getAsString() : null);

                    AuctionTimeUtil.AuctionState state = AuctionTimeUtil.calculateState(rawStartTime, rawEndTime, rawServerNow);
                    totalSeconds = state.countdownSeconds;
                    currentStatus = (statusFromServer != null && !statusFromServer.isBlank()) ? statusFromServer : state.finalStatus;

                    if ("OPEN".equalsIgnoreCase(currentStatus) || "RUNNING".equalsIgnoreCase(currentStatus)) {
                        isAuctionStarted = "RUNNING".equalsIgnoreCase(currentStatus);
                        if (btnPlaceBid != null) {
                            btnPlaceBid.setDisable(!isAuctionStarted || autoBidActive);
                            btnPlaceBid.setText(isAuctionStarted ? "ĐẶT GIÁ" : "CHỜ MỞ BÁN");
                        }
                        if (txtBidAmount != null) {
                            txtBidAmount.setEditable(isAuctionStarted && !autoBidActive);
                        }
                        if (btnEnableAutoBid != null) {
                            btnEnableAutoBid.setDisable(!isAuctionStarted);
                        }

                        if (currentUserOwnsAuction) {
                            if (txtBidAmount != null) {
                                txtBidAmount.setEditable(false);
                            }
                            if (txtMaxAutoBid != null) {
                                txtMaxAutoBid.setEditable(false);
                            }
                            if (txtAutoBidStep != null) {
                                txtAutoBidStep.setEditable(false);
                            }
                            if (!ownerBidWarningShown) {
                                ownerBidWarningShown = true;
                                showAlert("Không thể đặt giá", SELLER_SELF_BID_MESSAGE);
                            }
                        } else if (txtMaxAutoBid != null) {
                            txtMaxAutoBid.setEditable(isAuctionStarted);
                            if (txtAutoBidStep != null) {
                                txtAutoBidStep.setEditable(isAuctionStarted);
                            }
                        }

                        if (data.has("userMaxAutoBid") && !data.get("userMaxAutoBid").isJsonNull()) {
                            autoBidActive = true;
                            long savedMaxBid = data.get("userMaxAutoBid").getAsLong();
                            if (txtMaxAutoBid != null) {
                                txtMaxAutoBid.setText(String.valueOf(savedMaxBid));
                            }
                            if (txtAutoBidStep != null && data.has("userAutoBidStep")
                                    && !data.get("userAutoBidStep").isJsonNull()) {
                                txtAutoBidStep.setText(String.valueOf(data.get("userAutoBidStep").getAsLong()));
                            }
                            if (btnEnableAutoBid != null) {
                                btnEnableAutoBid.setText(AUTO_BID_REMOVE_TEXT);
                            }
                        } else {
                            autoBidActive = false;
                            if (txtMaxAutoBid != null) {
                                txtMaxAutoBid.clear();
                            }
                            if (txtAutoBidStep != null) {
                                txtAutoBidStep.clear();
                            }
                            if (btnEnableAutoBid != null) {
                                btnEnableAutoBid.setText(AUTO_BID_REGISTER_TEXT);
                            }
                        }
                        if (!currentUserOwnsAuction && isAuctionStarted) {
                            if (btnPlaceBid != null) {
                                btnPlaceBid.setDisable(autoBidActive);
                            }
                            if (txtBidAmount != null) {
                                txtBidAmount.setEditable(!autoBidActive);
                            }
                        }

                        startCountdown();
                    } else {
                        setExpiredUI();
                    }

                }));
    }

    private void loadBidHistoryFromServer() {
        if (currentAuctionId == -1) {
            return;
        }

        JsonObject historyReq = new JsonObject();
        historyReq.addProperty("type", ActionType.GET_BID_HISTORY);
        historyReq.addProperty("auctionId", currentAuctionId);
        historyReq.addProperty("requestId", java.util.UUID.randomUUID().toString());

        ClientSocket.getInstance().sendJsonRequest(historyReq, ActionType.GET_BID_HISTORY, response ->
                Platform.runLater(() -> {
                    if (!(response.has("success") && response.get("success").getAsBoolean()) || !response.has("data")) {
                        return;
                    }

                    JsonArray historyArray = response.getAsJsonArray("data");
                    if (lvBidHistory != null) {
                        lvBidHistory.getItems().clear();
                    }
                    displayedBidKeys.clear();
                    if (priceChart != null && priceSeries != null) {
                        priceChart.getData().remove(priceSeries);
                    }
                    priceSeries = new XYChart.Series<>();
                    priceSeries.setName("Giá đấu");

                    List<XYChart.Data<String, Number>> chartPoints = new ArrayList<>();
                    String latestLeader = null;

                    for (JsonElement el : historyArray) {
                        JsonObject bidObj = el.getAsJsonObject();
                        String name = bidObj.has("fullName") ? bidObj.get("fullName").getAsString()
                                : (bidObj.has("bidderName") ? bidObj.get("bidderName").getAsString()
                                : (bidObj.has("username") ? bidObj.get("username").getAsString() : "Người dùng ẩn danh"));
                        long amount = bidObj.has("bidAmount") ? bidObj.get("bidAmount").getAsLong()
                                : (bidObj.has("amount") ? bidObj.get("amount").getAsLong() : 0);
                        latestLeader = name;

                        String rawTime = bidObj.has("bidTime") ? bidObj.get("bidTime").getAsString() : LocalDateTime.now().toString();
                        String chartTimeStr;
                        String historyTimeStr;
                        try {
                            LocalDateTime dt = LocalDateTime.parse(rawTime);
                            chartTimeStr = dt.format(timeFormatter);
                            historyTimeStr = dt.format(historyTimeFormatter);
                        } catch (Exception e) {
                            chartTimeStr = LocalDateTime.now().format(timeFormatter);
                            historyTimeStr = LocalDateTime.now().format(historyTimeFormatter);
                        }

                        addBidHistoryEntry(name, amount, historyTimeStr);

                        XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(chartTimeStr, amount);
                        AuctionRoomChartHelper.setupHoverEffect(dataPoint);
                        chartPoints.add(dataPoint);
                    }

                    priceSeries.getData().addAll(chartPoints);
                    if (priceChart != null) {
                        priceChart.getData().add(priceSeries);
                    }

                    if (lblLeader != null) {
                        lblLeader.setText(latestLeader != null && !latestLeader.isBlank()
                                ? "Người dẫn đầu: " + latestLeader
                                : "Chưa có ai đặt giá");
                    }
                    hasCurrentWinner = latestLeader != null && !latestLeader.isBlank();
                }));
    }

    private void startCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (totalSeconds > 0) {
                totalSeconds--;
                updateCountdownLabel();
                if (isAuctionStarted && totalSeconds <= 30 && lblCountdown != null) {
                    lblCountdown.setStyle("-fx-text-fill: #A64452; -fx-font-weight: bold;");
                }
            } else {
                countdownTimeline.stop();
                if ("OPEN".equalsIgnoreCase(currentStatus)) {
                    refreshAuctionState();
                } else {
                    setExpiredUI();
                }
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
        updateCountdownLabel();
    }

    private void updateCountdownLabel() {
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        if (lblCountdown == null) {
            return;
        }
        if (!isAuctionStarted) {
            lblCountdown.setText(String.format("Sắp mở: %02d:%02d:%02d", h, m, s));
            lblCountdown.setStyle("-fx-text-fill: #E65100; -fx-font-weight: bold;");
        } else {
            lblCountdown.setText(String.format("Còn lại: %02d:%02d:%02d", h, m, s));
            lblCountdown.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
        }
    }

    private void setExpiredUI() {
        isAuctionStarted = false;
        totalSeconds = 0;
        if (lblCountdown != null) {
            lblCountdown.setText("ĐÃ KẾT THÚC!");
            lblCountdown.setStyle("-fx-text-fill: #888888; -fx-font-weight: bold;");
        }
        if (btnPlaceBid != null) {
            btnPlaceBid.setDisable(true);
            btnPlaceBid.setText("HẾT HẠN");
        }
        if (btnEnableAutoBid != null) {
            btnEnableAutoBid.setDisable(true);
        }
        if (txtBidAmount != null) {
            txtBidAmount.setEditable(false);
        }
    }

    @FXML
    private void handlePlaceBid() {
        if (!isAuctionStarted || currentAuctionId == -1) {
            return;
        }
        if (currentUserOwnsAuction) {
            showAlert("Không thể đặt giá", SELLER_SELF_BID_MESSAGE);
            return;
        }
        String input = txtBidAmount.getText().trim();
        if (input.isEmpty()) {
            return;
        }

        try {
            long bidAmount = parseMoneyValue(input);
            long minValidBid = getMinimumManualBid();

            if (currentBuyNowPrice != null && currentBuyNowPrice > 0 && bidAmount >= currentBuyNowPrice) {
                if (confirmBuyNow()) {
                    sendBuyNowConfirmation();
                }
                return;
            }

            if (bidAmount < minValidBid) {
                showAlert("Lỗi đặt giá", "Giá tối thiểu: " + formatVnd(minValidBid));
                return;
            }

            AuctionDTOs.BidRequest request = new AuctionDTOs.BidRequest(currentAuctionId, bidAmount, 1);
            btnPlaceBid.setDisable(true);
            JsonObject jsonRequest = new Gson().toJsonTree(request).getAsJsonObject();
            jsonRequest.addProperty("type", ActionType.PLACE_BID);
            jsonRequest.addProperty("requestId", java.util.UUID.randomUUID().toString());

            ClientSocket.getInstance().sendJsonRequest(jsonRequest, "BID_RESPONSE", response ->
                    Platform.runLater(() -> {
                        btnPlaceBid.setDisable(false);
                        if (!(response.has("success") && response.get("success").getAsBoolean())) {
                            showAlert("Lỗi", response.has("message") ? response.get("message").getAsString() : "Lỗi đặt giá.");
                        } else {
                            refreshAuctionState();
                            loadBidHistoryFromServer();
                            txtBidAmount.clear();
                        }
                    }));
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Số tiền không hợp lệ.");
        }
    }

    private boolean confirmBuyNow() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Xác nhận mua đứt");
        dialog.setHeaderText(null);

        ButtonType cancelButton = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirmButton = new ButtonType("Xác nhận", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(cancelButton, confirmButton);

        Label message = new Label(
                "Bạn đã đặt giá vượt quá giá mua đứt của sản phẩm.\n\n"
                        + "Xác nhận nếu bạn muốn sở hữu sản phẩm này ngay lập tức.\n\n"
                        + "Hủy nếu bạn muốn đặt một mức giá thấp hơn."
        );
        message.setWrapText(true);
        message.setStyle("-fx-font-size: 14px; -fx-text-fill: #342724; -fx-line-spacing: 2px;");
        dialog.getDialogPane().setContent(message);
        dialog.getDialogPane().setPrefWidth(450);
        dialog.getDialogPane().setStyle("-fx-background-color: #FFFDFC; -fx-padding: 14px;");

        Button cancel = (Button) dialog.getDialogPane().lookupButton(cancelButton);
        cancel.setStyle("-fx-background-color: #F0ECE8; -fx-text-fill: #3E2723; -fx-font-weight: bold; -fx-background-radius: 7; -fx-padding: 9 20;");
        Button confirm = (Button) dialog.getDialogPane().lookupButton(confirmButton);
        confirm.setStyle("-fx-background-color: #B32638; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 7; -fx-padding: 9 20;");

        Optional<ButtonType> selected = dialog.showAndWait();
        return selected.isPresent() && selected.get() == confirmButton;
    }

    private void sendBuyNowConfirmation() {
        if (currentUserOwnsAuction) {
            showAlert("Không thể mua đứt", SELLER_SELF_BID_MESSAGE);
            return;
        }
        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.CONFIRM_BUY_NOW);
        request.addProperty("auctionId", currentAuctionId);
        request.addProperty("requestId", java.util.UUID.randomUUID().toString());

        if (btnPlaceBid != null) {
            btnPlaceBid.setDisable(true);
        }

        ClientSocket.getInstance().sendJsonRequest(request, "BUY_NOW_RESPONSE", response ->
                Platform.runLater(() -> {
                    if (btnPlaceBid != null) {
                        btnPlaceBid.setDisable(false);
                    }
                    if (!(response.has("success") && response.get("success").getAsBoolean())) {
                        showAlert("Không thể mua đứt", response.has("message") ? response.get("message").getAsString() : "Mua đứt thất bại.");
                        return;
                    }

                    showBuyNowWinnerDialog();
                    if (txtBidAmount != null) {
                        txtBidAmount.clear();
                    }
                    refreshAuctionState();
                    loadBidHistoryFromServer();
                }));
    }

    private void showBuyNowWinnerDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Chiến thắng phiên đấu giá");
        alert.setHeaderText("Chúc mừng! Bạn đã là người chiến thắng ở phiên đấu giá này.");
        alert.setContentText("Vui lòng thanh toán để chính thức sở hữu sản phẩm.\n\nNếu hủy thanh toán, bạn sẽ chịu phạt 10% tiền đặt giá.");
        alert.getDialogPane().setStyle("-fx-background-color: #FFFDFC; -fx-font-size: 13px;");
        alert.showAndWait();
    }

    @FXML
    private void handleEnableAutoBid() {
        if (!isAuctionStarted || currentAuctionId == -1 || txtMaxAutoBid == null) {
            return;
        }
        if (currentUserOwnsAuction) {
            showAlert("Không thể Auto-bid", SELLER_SELF_BID_MESSAGE);
            return;
        }
        if (btnEnableAutoBid != null && AUTO_BID_REMOVE_TEXT.equals(btnEnableAutoBid.getText())) {
            removeAutoBid();
            return;
        }
        if (txtMaxAutoBid.getText().trim().isEmpty()) {
            return;
        }
        if (txtAutoBidStep == null || txtAutoBidStep.getText().trim().isEmpty()) {
            showAlert("Lỗi Auto-bid", "Vui lòng nhập bước giá Auto-bid.");
            return;
        }

        try {
            long maxPrice = parseMoneyValue(txtMaxAutoBid.getText());
            long bidStep = parseMoneyValue(txtAutoBidStep.getText());
            long currentPrice = extractMoneyValue(lblCurrentPrice.getText());
            long sellerBidStep = extractMoneyValue(lblBidIncrement.getText());
            long minValidBid = currentPrice + sellerBidStep;
            long autoNextBid = currentPrice + Math.max(bidStep, sellerBidStep);
            boolean autoBidWouldReachBuyNow = currentBuyNowPrice != null
                    && currentBuyNowPrice > 0
                    && currentPrice < currentBuyNowPrice
                    && autoNextBid >= currentBuyNowPrice
                    && maxPrice >= currentBuyNowPrice;

            if (!autoBidWouldReachBuyNow && maxPrice < minValidBid) {
                showAlert("Lỗi Auto-bid", "Mức giá tối đa phải >= " + formatVnd(minValidBid));
                return;
            }
            if (bidStep < sellerBidStep) {
                showAlert("Lỗi Auto-bid", "Bước giá Auto-bid phải >= bước giá người bán ("
                        + formatVnd(sellerBidStep) + ").");
                return;
            }

            JsonObject jsonRequest = new JsonObject();
            jsonRequest.addProperty("type", ActionType.REGISTER_AUTO_BID);
            jsonRequest.addProperty("auctionId", currentAuctionId);
            jsonRequest.addProperty("maxBid", maxPrice);
            jsonRequest.addProperty("bidStep", bidStep);
            jsonRequest.addProperty("requestId", java.util.UUID.randomUUID().toString());
            btnEnableAutoBid.setDisable(true);

            ClientSocket.getInstance().sendJsonRequest(jsonRequest, "AUTO_BID_RESPONSE", response ->
                    Platform.runLater(() -> {
                        btnEnableAutoBid.setDisable(false);
                        if (response.has("success") && response.get("success").getAsBoolean()) {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Thành công");
                            alert.setHeaderText(null);
                            alert.setContentText("Kích hoạt Auto-bid thành công!");
                            alert.showAndWait();
                            autoBidActive = true;
                            btnEnableAutoBid.setText(AUTO_BID_REMOVE_TEXT);
                            if (btnPlaceBid != null) {
                                btnPlaceBid.setDisable(true);
                                btnPlaceBid.setText("ĐANG AUTO-BID");
                            }
                            if (txtBidAmount != null) {
                                txtBidAmount.setEditable(false);
                            }
                        } else {
                            showAlert("Lỗi Auto-bid", response.has("message") ? response.get("message").getAsString() : "Lỗi đăng ký.");
                        }
                    }));
        } catch (Exception e) {
            showAlert("Lỗi Auto-bid", "Vui lòng nhập số hợp lệ cho mức tối đa và bước giá.");
        }
    }

    private void removeAutoBid() {
        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.REMOVE_AUTO_BID);
        request.addProperty("auctionId", currentAuctionId);
        request.addProperty("requestId", java.util.UUID.randomUUID().toString());
        btnEnableAutoBid.setDisable(true);
        ClientSocket.getInstance().sendJsonRequest(request, "AUTO_BID_RESPONSE", response ->
                Platform.runLater(() -> {
                    btnEnableAutoBid.setDisable(false);
                    if (!(response.has("success") && response.get("success").getAsBoolean())) {
                        showAlert("Lỗi Auto-bid", response.has("message")
                                ? response.get("message").getAsString() : "Không thể xóa Auto-bid.");
                        return;
                    }
                    if (txtMaxAutoBid != null) {
                        txtMaxAutoBid.clear();
                    }
                    if (txtAutoBidStep != null) {
                        txtAutoBidStep.clear();
                    }
                    autoBidActive = false;
                    btnEnableAutoBid.setText(AUTO_BID_REGISTER_TEXT);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Thành công");
                    alert.setHeaderText(null);
                    alert.setContentText("Đã xóa đăng ký Auto-bid.");
                    alert.showAndWait();
                    if (!currentUserOwnsAuction && isAuctionStarted) {
                        if (btnPlaceBid != null) {
                            btnPlaceBid.setDisable(false);
                            btnPlaceBid.setText("ĐẶT GIÁ");
                        }
                        if (txtBidAmount != null) {
                            txtBidAmount.setEditable(true);
                        }
                    }
                }));
    }

    public void updateRealtimeBid(
            long newPrice,
            String bidderName,
            String endTime,
            String serverNow,
            String status,
            int auctionId,
            String bidTime
    ) {
        if (auctionId > 0 && currentAuctionId != auctionId) {
            return;
        }

        if (lblCurrentPrice != null) {
            lblCurrentPrice.setText(formatVnd(newPrice));
        }
        currentDisplayedPrice = newPrice;
        hasCurrentWinner = true;
        if (lblLeader != null) {
            lblLeader.setText("Người dẫn đầu: " + bidderName);
        }

        LocalDateTime bidDateTime = AuctionTimeUtil.parse(bidTime);
        if (bidDateTime == null) {
            bidDateTime = LocalDateTime.now();
        }
        String historyTime = bidDateTime.format(historyTimeFormatter);
        boolean addedHistoryEntry = addBidHistoryEntry(bidderName, newPrice, historyTime);
        if (!addedHistoryEntry) {
            applyServerCountdown(endTime, serverNow, status);
            return;
        }

        applyServerCountdown(endTime, serverNow, status);

        if (priceSeries != null) {
            XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(bidDateTime.format(timeFormatter), newPrice);
            AuctionRoomChartHelper.setupHoverEffect(dataPoint);
            priceSeries.getData().add(dataPoint);
            if (priceSeries.getData().size() > 30) {
                priceSeries.getData().remove(0);
            }
        }
    }

    private boolean addBidHistoryEntry(String bidderName, long bidAmount, String historyTime) {
        String key = bidderName + "|" + bidAmount + "|" + historyTime;
        if (!displayedBidKeys.add(key)) {
            return false;
        }
        if (lvBidHistory != null) {
            lvBidHistory.getItems().add(0, "(" + historyTime + ") " + bidderName + " đã đặt: " + formatVnd(bidAmount));
        }
        return true;
    }

    private long getMinimumManualBid() {
        long currentPrice = currentDisplayedPrice > 0
                ? currentDisplayedPrice
                : extractMoneyValue(lblCurrentPrice.getText());
        long stepPrice = currentBidIncrement > 0
                ? currentBidIncrement
                : extractMoneyValue(lblBidIncrement.getText());
        return hasCurrentWinner ? currentPrice + stepPrice : currentPrice;
    }

    private String resolveUserDisplayName(JsonObject userObj) {
        if (hasValue(userObj, "fullName")) {
            return userObj.get("fullName").getAsString();
        }
        if (hasValue(userObj, "username")) {
            return userObj.get("username").getAsString();
        }
        if (hasValue(userObj, "id")) {
            return "Người dùng #" + userObj.get("id").getAsInt();
        }
        return "Người dùng ẩn danh";
    }

    private void applyServerCountdown(String endTime, String serverNow, String status) {
        if (endTime == null || endTime.isBlank()) {
            refreshAuctionState();
            return;
        }

        LocalDateTime serverTime = AuctionTimeUtil.parse(serverNow);
        LocalDateTime serverEndTime = AuctionTimeUtil.parse(endTime);
        if (serverTime == null || serverEndTime == null) {
            refreshAuctionState();
            return;
        }

        int newTotalSeconds = (int) Math.max(0, ChronoUnit.SECONDS.between(serverTime, serverEndTime));
        totalSeconds = newTotalSeconds;
        currentStatus = status == null || status.isBlank() ? "RUNNING" : status;
        isAuctionStarted = "RUNNING".equalsIgnoreCase(currentStatus);

        if (newTotalSeconds <= 0 || !"RUNNING".equalsIgnoreCase(currentStatus)) {
            setExpiredUI();
            return;
        }

        boolean allowManualBid = !autoBidActive && !currentUserOwnsAuction;
        if (btnPlaceBid != null) {
            btnPlaceBid.setDisable(!allowManualBid);
            btnPlaceBid.setText(autoBidActive ? "ĐANG AUTO-BID" : "ĐẶT GIÁ");
        }
        if (txtBidAmount != null) {
            txtBidAmount.setEditable(allowManualBid);
        }
        if (btnEnableAutoBid != null) {
            btnEnableAutoBid.setDisable(false);
        }
        startCountdown();
    }

    public void loadProductImage(String urlString) {
        loadProductImage(urlString, null);
    }

    public void loadProductImage(String detailUrl, String fallbackUrl) {
        if (imgProduct == null) {
            return;
        }

        String normalizedDetailUrl = normalizeImageUrl(detailUrl);
        String normalizedFallbackUrl = normalizeImageUrl(fallbackUrl);
        String primaryUrl = normalizedDetailUrl != null ? normalizedDetailUrl : normalizedFallbackUrl;
        if (primaryUrl == null) {
            imgProduct.setImage(null);
            return;
        }

        if (normalizedFallbackUrl != null) {
            imgProduct.setImage(ImageCacheManager.getPreviewImage(normalizedFallbackUrl));
        }

        Image image = ImageCacheManager.getDetailImage(primaryUrl);
        if (image == null) {
            setFallbackProductImage(normalizedFallbackUrl);
            return;
        }

        if (image.isError()) {
            setFallbackProductImage(normalizedFallbackUrl);
            return;
        }

        if (normalizedFallbackUrl == null) {
            imgProduct.setImage(image);
        }

        if (ImageCacheManager.isImageReady(image)) {
            imgProduct.setImage(image);
            return;
        }

        image.progressProperty().addListener((obs, oldProgress, newProgress) -> {
            if (newProgress.doubleValue() >= 1.0 && !image.isError()) {
                Platform.runLater(() -> imgProduct.setImage(image));
            }
        });
        image.errorProperty().addListener((obs, wasError, isError) -> {
            if (Boolean.TRUE.equals(isError)) {
                Platform.runLater(() -> setFallbackProductImage(normalizedFallbackUrl));
            }
        });
    }

    private void setFallbackProductImage(String fallbackUrl) {
        String normalizedFallbackUrl = normalizeImageUrl(fallbackUrl);
        if (normalizedFallbackUrl == null) {
            imgProduct.setImage(null);
            return;
        }
        imgProduct.setImage(ImageCacheManager.getPreviewImage(normalizedFallbackUrl));
    }

    private String normalizeImageUrl(String url) {
        return url == null || url.trim().isEmpty() ? null : url.trim();
    }

    public void updateRealtimeStatus(String newStatus) {
        currentStatus = newStatus;
        if ("FINISHED".equalsIgnoreCase(newStatus)
                || "PAID".equalsIgnoreCase(newStatus)
                || "CANCELED".equalsIgnoreCase(newStatus)) {
            setExpiredUI();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
