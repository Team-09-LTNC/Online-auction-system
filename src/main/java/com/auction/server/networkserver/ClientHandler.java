package com.auction.server.networkserver;

import com.auction.common.enums.ActionType;
import com.auction.common.model.bid.BidTransaction;
import com.auction.common.model.user.User;
import com.auction.common.observer.AuctionObserver;
import com.auction.server.manager.UserManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * ClientHandler: Quản lý vòng đời kết nối của một Client trên Server.
 * Nhiệm vụ chính: Lắng nghe Socket, nhận thông báo Real-time và đẩy dữ liệu về Client.
 */
public class ClientHandler implements Runnable, AuctionObserver {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private final Socket socketClient;
    private final Gson gson = new Gson();
    private PrintWriter out;
    private User nguoiDungHienTai;

    public ClientHandler(Socket socketClient) {
        this.socketClient = socketClient;
    }

    /** Thiết lập thông tin người dùng cho phiên kết nối này. */
    public void datNguoiDungHienTai(User user) { this.nguoiDungHienTai = user; }

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
            String chuoiJson;
            while ((chuoiJson = in.readLine()) != null) {
                logger.debug("Dữ liệu nhận từ {}: {}", socketClient.getInetAddress(), chuoiJson);

                JsonObject yeuCau = JsonParser.parseString(chuoiJson).getAsJsonObject();
                String loaiYeuCau = yeuCau.get("type").getAsString();

                // Chuyển giao xử lý cho bộ điều phối
                String phanHoi = RequestDispatcher.layInstance().dieuPhoi(loaiYeuCau, yeuCau, this);
                if (phanHoi != null) {
                    out.println(phanHoi);
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
            UserManager.getInstance().dangXuat(nguoiDungHienTai.getId());
            logger.info("Người dùng {} đã thoát hệ thống.", nguoiDungHienTai.getUsername());
        }
        try { socketClient.close(); } catch (IOException ignored) {}
    }

    /** Gửi thông báo giá mới (Real-time) cho Client qua Observer Pattern. */
    @Override
    public void onNewBid(BidTransaction giaodich) {
        if (out != null) {
            JsonObject update = new JsonObject();
            update.addProperty("type", ActionType.AUCTION_BID_UPDATE);
            update.add("transaction", gson.toJsonTree(giaodich));

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
}