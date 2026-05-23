package com.auction.server.networkserver;

import com.auction.common.enums.ActionType;
import com.auction.common.model.bid.Auction;
import com.auction.common.model.bid.BidTransaction;
import com.auction.common.model.user.User;
import com.auction.common.observer.AuctionObserver;
import com.auction.server.manager.AuctionManager;
import com.auction.server.manager.UserManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import com.google.gson.GsonBuilder;
import com.auction.common.util.LocalDateTimeAdapter;
import java.time.LocalDateTime;

/**
 * ClientHandler: Quản lý vòng đời kết nối của một Client trên Server.
 * Nhiệm vụ chính: Lắng nghe Socket, nhận thông báo Real-time và đẩy dữ liệu về Client.
 */
public class ClientHandler implements Runnable, AuctionObserver {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private final Socket socketClient;
    Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
    private PrintWriter out;
    private User nguoiDungHienTai;

    public ClientHandler(Socket socketClient) {
        this.socketClient = socketClient;
    }

    /** Thiết lập thông tin người dùng cho phiên kết nối này. */
    public void datNguoiDungHienTai(User user) {
        if (this.nguoiDungHienTai != null) {
            UserManager.getInstance().huyKetNoi(this.nguoiDungHienTai.getId(), this);
        }
        this.nguoiDungHienTai = user;
        if (user != null) {
            UserManager.getInstance().dangKyKetNoi(user.getId(), this);
        }
    }

    /** Truy xuất người dùng hiện tại đang đăng nhập. */
    public User layNguoiDungHienTai() { return nguoiDungHienTai; }

    @Override
    public void run() {
        thucThiKetNoi();
    }

    /** Vòng lặp chính duy trì việc đọc/ghi dữ liệu qua Socket. */
    private void thucThiKetNoi() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socketClient.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter outWriter = new PrintWriter(new OutputStreamWriter(socketClient.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            this.out = outWriter;
            String chuoiJson = "";
            logger.info("Đang chờ dữ liệu từ client {}: {}", socketClient.getInetAddress(), socketClient.getPort());
            while ((chuoiJson = in.readLine()) != null) {
                logger.debug("Dữ liệu nhận từ {}: {}", socketClient.getInetAddress(), chuoiJson);

                JsonObject yeuCau = null;
                try {
                    yeuCau = JsonParser.parseString(chuoiJson).getAsJsonObject();
                    String loaiYeuCau = yeuCau.get("type").getAsString();

                    // Chuyển giao xử lý cho bộ điều phối
                    String phanHoi = RequestDispatcher.layInstance().dieuPhoi(loaiYeuCau, yeuCau, this);
                    if (phanHoi != null) {
                        out.println(phanHoi);
                    }
                } catch (RuntimeException e) {
                    logger.error("Lỗi xử lý request từ client {}: {}", socketClient.getInetAddress(), chuoiJson, e);
                    out.println(taoPhanHoiLoi(yeuCau));
                }
            }
        } catch (IOException e) {
            logger.warn("Client {} đã ngắt kết nối đột ngột.", socketClient.getInetAddress());
        } finally {
            donDepKetNoi();
        }
    }

    /** Xóa trạng thái Online khi ngắt kết nối. */
    private void donDepKetNoi() {
        if (nguoiDungHienTai != null) {
            UserManager.getInstance().huyKetNoi(nguoiDungHienTai.getId(), this);
            UserManager.getInstance().dangXuat(nguoiDungHienTai.getId());
            logger.info("Người dùng {} đã thoát hệ thống.", nguoiDungHienTai.getUsername());
        }
        try { socketClient.close(); } catch (IOException ignored) {}
    }

    private String taoPhanHoiLoi(JsonObject yeuCau) {
        JsonObject loi = new JsonObject();
        loi.addProperty("type", "ERROR_RESPONSE");
        loi.addProperty("success", false);
        loi.addProperty("message", "Server không xử lý được yêu cầu.");
        if (yeuCau != null && yeuCau.has("requestId") && !yeuCau.get("requestId").isJsonNull()) {
            loi.addProperty("requestId", yeuCau.get("requestId").getAsString());
        }
        return gson.toJson(loi);
    }

    /** Gửi thông báo giá mới (Real-time) cho Client qua Observer Pattern. */
    @Override
    public void onNewBid(BidTransaction giaodich) {
        if (out != null) {
            JsonObject update = new JsonObject();
            update.addProperty("type", ActionType.AUCTION_BID_UPDATE);
            update.add("transaction", gson.toJsonTree(giaodich));
            update.addProperty("serverNow", LocalDateTime.now().toString());

            Auction phien = AuctionManager.getInstance().layPhienTheoId(giaodich.getAuctionId());
            if (phien != null) {
                update.addProperty("auctionId", phien.getId());
                update.addProperty("status", phien.getStatus().name());
                update.addProperty("endTime", phien.getEndTime() != null ? phien.getEndTime().toString() : null);
            }

            out.println(gson.toJson(update));
        }
    }

    /** Thông báo khi trạng thái phiên thay đổi (Ví dụ: Kết thúc đấu giá). */
    @Override
    public void onStatusChanged(com.auction.common.enums.AuctionStatus trangThaiMoi) {
        if (out != null) {
            JsonObject update = new JsonObject();
            update.addProperty("type", ActionType.AUCTION_RESULT);
            update.addProperty("newStatus", trangThaiMoi.name());

            out.println(gson.toJson(update));
        }
    }

    /**
     * phiên chat của phòng đấu giá
     * @param senderName
     * @param message
     * @param isSystem
     */
    @Override
    public void onChatMessage(String senderName, String message, boolean isSystem) {
        if (out != null) {
            JsonObject update = new JsonObject();
            update.addProperty("type", ActionType.RECEIVE_CHAT_MESSAGE);
            update.addProperty("senderName", senderName);
            update.addProperty("message", message);
            update.addProperty("isSystem", isSystem);

            out.println(gson.toJson(update));
        }
    }

    /** Gửi thông báo riêng cho client đang giữ socket này. */
    public void guiThongBaoHeThong(JsonObject payload) {
        if (out != null) {
            payload.addProperty("type", "SYSTEM_NOTIFICATION");
            out.println(gson.toJson(payload));
        }
    }
}
