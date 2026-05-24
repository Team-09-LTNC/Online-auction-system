package com.auction.server.networkserver;

import com.auction.common.enums.ActionType;
import com.auction.common.model.bid.Auction;
import com.auction.common.model.bid.BidTransaction;
import com.auction.common.model.user.User;
import com.auction.common.observer.AuctionObserver;
import com.auction.common.util.LocalDateTimeAdapter;
import com.auction.server.manager.AuctionManager;
import com.auction.server.manager.UserManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles one client socket connection lifecycle.
 */
public class ClientHandler implements Runnable, AuctionObserver {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClientHandler.class);

  private final Socket socketClient;
  private final Gson gson = new GsonBuilder()
      .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
      .create();
  private PrintWriter out;
  private User nguoiDungHienTai;

  public ClientHandler(Socket socketClient) {
    this.socketClient = socketClient;
  }

  public void datNguoiDungHienTai(User user) {
    if (this.nguoiDungHienTai != null) {
      UserManager.getInstance().huyKetNoi(this.nguoiDungHienTai.getId(), this);
    }
    this.nguoiDungHienTai = user;
    if (user != null) {
      UserManager.getInstance().dangKyKetNoi(user.getId(), this);
    }
  }

  public User layNguoiDungHienTai() {
    return nguoiDungHienTai;
  }

  @Override
  public void run() {
    thucThiKetNoi();
  }

  private void thucThiKetNoi() {
    try (BufferedReader in = new BufferedReader(
        new InputStreamReader(socketClient.getInputStream(), StandardCharsets.UTF_8));
         PrintWriter outWriter = new PrintWriter(
             new OutputStreamWriter(socketClient.getOutputStream(), StandardCharsets.UTF_8), true)) {
      this.out = outWriter;
      String chuoiJson;
      LOGGER.debug("Dang cho du lieu tu client {}: {}",
          socketClient.getInetAddress(), socketClient.getPort());
      while ((chuoiJson = in.readLine()) != null) {
        LOGGER.trace("Du lieu nhan tu {}: {}", socketClient.getInetAddress(), chuoiJson);
        xuLyRequest(chuoiJson);
      }
    } catch (IOException e) {
      LOGGER.warn("Client {} da ngat ket noi dot ngot.", socketClient.getInetAddress());
    } finally {
      donDepKetNoi();
    }
  }

  private void xuLyRequest(String chuoiJson) {
    JsonObject yeuCau = null;
    try {
      yeuCau = JsonParser.parseString(chuoiJson).getAsJsonObject();
      if (!yeuCau.has("type") || yeuCau.get("type").isJsonNull()) {
        out.println(taoPhanHoiLoiYeuCau(yeuCau, "Thieu truong type trong request."));
        return;
      }
      String loaiYeuCau = yeuCau.get("type").getAsString();
      String phanHoi = RequestDispatcher.layInstance().dieuPhoi(loaiYeuCau, yeuCau, this);
      if (phanHoi != null) {
        out.println(phanHoi);
      }
    } catch (RuntimeException e) {
      LOGGER.error("Loi xu ly request tu client {}: {}",
          socketClient.getInetAddress(), chuoiJson, e);
      out.println(taoPhanHoiLoi(yeuCau));
    }
  }

  private void donDepKetNoi() {
    if (nguoiDungHienTai != null) {
      UserManager.getInstance().huyKetNoi(nguoiDungHienTai.getId(), this);
      UserManager.getInstance().dangXuat(nguoiDungHienTai.getId());
      LOGGER.info("Nguoi dung {} da thoat he thong.", nguoiDungHienTai.getUsername());
    }
    try {
      socketClient.close();
    } catch (IOException ignored) {
      // no-op
    }
  }

  private String taoPhanHoiLoi(JsonObject yeuCau) {
    return taoPhanHoiLoiYeuCau(yeuCau, "Server khong xu ly duoc yeu cau.");
  }

  private String taoPhanHoiLoiYeuCau(JsonObject yeuCau, String message) {
    JsonObject loi = new JsonObject();
    loi.addProperty("type", "ERROR_RESPONSE");
    loi.addProperty("success", false);
    loi.addProperty("message", message);
    if (yeuCau != null && yeuCau.has("requestId") && !yeuCau.get("requestId").isJsonNull()) {
      loi.addProperty("requestId", yeuCau.get("requestId").getAsString());
    }
    return gson.toJson(loi);
  }

  @Override
  public void onNewBid(BidTransaction giaoDich) {
    if (out == null) {
      return;
    }
    JsonObject update = new JsonObject();
    update.addProperty("type", ActionType.AUCTION_BID_UPDATE);
    update.add("transaction", gson.toJsonTree(giaoDich));
    update.addProperty("serverNow", LocalDateTime.now().toString());

    Auction phien = AuctionManager.getInstance().layPhienTheoId(giaoDich.getAuctionId());
    if (phien != null) {
      update.addProperty("auctionId", phien.getId());
      update.addProperty("status", phien.getStatus().name());
      update.addProperty("endTime", phien.getEndTime() != null ? phien.getEndTime().toString() : null);
    }

    out.println(gson.toJson(update));
  }

  @Override
  public void onStatusChanged(com.auction.common.enums.AuctionStatus trangThaiMoi) {
    if (out == null) {
      return;
    }
    JsonObject update = new JsonObject();
    update.addProperty("type", ActionType.AUCTION_RESULT);
    update.addProperty("newStatus", trangThaiMoi.name());
    out.println(gson.toJson(update));
  }

  @Override
  public void onChatMessage(String senderName, String message, boolean isSystem) {
    if (out == null) {
      return;
    }
    JsonObject update = new JsonObject();
    update.addProperty("type", ActionType.RECEIVE_CHAT_MESSAGE);
    update.addProperty("senderName", senderName);
    update.addProperty("message", message);
    update.addProperty("isSystem", isSystem);
    out.println(gson.toJson(update));
  }

  public void guiThongBaoHeThong(JsonObject payload) {
    if (out == null) {
      return;
    }
    payload.addProperty("type", "SYSTEM_NOTIFICATION");
    out.println(gson.toJson(payload));
  }
}
