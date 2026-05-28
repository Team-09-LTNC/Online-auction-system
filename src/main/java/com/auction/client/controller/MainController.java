package com.auction.client.controller;

import com.auction.client.interfaces.RefreshableCenterContent;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MainController {
    public static MainController instance;
    private static final Set<String> CACHEABLE_BIDDER_VIEWS = Set.of(
            "/fxml/bidder/MainDashboard.fxml",
            "/fxml/bidder/AuctionListScreen.fxml",
            "/fxml/bidder/MyAuctions.fxml",
            "/fxml/bidder/FollowedAuctions.fxml",
            "/fxml/seller/MyProducts.fxml",
            "/fxml/components/Chat.fxml"
    );

    @FXML
    private StackPane mainContentArea;

    // Biến này để lưu controller của trang đang hiện ở giữa (ví dụ DashboardController)
    private Object currentCenterController;
    private final Map<String, LoadedCenterView> centerViewCache = new HashMap<>();

    public void initialize() {
        instance = this;
    }

    public void setCenterContent(String fxmlPath) {
        setCenterContent(fxmlPath, true);
    }

    public void setCenterContent(String fxmlPath, boolean refreshCachedContent) {
        try {
            boolean canReuse = CACHEABLE_BIDDER_VIEWS.contains(fxmlPath);
            LoadedCenterView loadedView = canReuse ? centerViewCache.get(fxmlPath) : null;
            boolean reusedView = loadedView != null;

            if (loadedView == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent root = loader.load();
                loadedView = new LoadedCenterView(root, loader.getController());
                if (canReuse) {
                    centerViewCache.put(fxmlPath, loadedView);
                }
            }

            currentCenterController = loadedView.controller();
            if (!mainContentArea.getChildren().contains(loadedView.root())) {
                showCenterNode(loadedView.root());
            }

            if (reusedView
                    && refreshCachedContent
                    && currentCenterController instanceof RefreshableCenterContent refreshableContent) {
                refreshableContent.refreshContent();
            }

        } catch (IOException e) {
            System.err.println("Lỗi load FXML: " + fxmlPath);
            e.printStackTrace();
        }
    }

    // Hàm để các controller khác lấy được controller đang hiện ở giữa
    public Object getCurrentCenterController() {
        return currentCenterController;
    }

    public void refreshRealtimeContent() {
        refreshRealtimeContent(true);
    }

    public void refreshRealtimeContent(boolean includeCurrentContent) {
        Set<Object> refreshedControllers = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (LoadedCenterView loadedView : centerViewCache.values()) {
            if (!includeCurrentContent && loadedView.controller() == currentCenterController) {
                continue;
            }
            refreshController(loadedView.controller(), refreshedControllers);
        }
        if (includeCurrentContent) {
            refreshController(currentCenterController, refreshedControllers);
        }
    }

    private void refreshController(Object controller, Set<Object> refreshedControllers) {
        if (controller instanceof RefreshableCenterContent refreshableContent
                && refreshedControllers.add(controller)) {
            refreshableContent.refreshContent();
        }
    }

    private void showCenterNode(Parent newNode) {
        newNode.setOpacity(0);
        mainContentArea.getChildren().setAll(newNode);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(130), newNode);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private record LoadedCenterView(Parent root, Object controller) {
    }
}
