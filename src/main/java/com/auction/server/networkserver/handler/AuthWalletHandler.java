package com.auction.server.networkserver.handler;

import com.auction.common.dto.BaseDTOs;
import com.auction.common.enums.ErrorCode;
import com.auction.common.enums.StatusCode;
import com.auction.common.model.user.User;
import com.auction.server.dao.UserDao;
import com.auction.server.dao.WalletTransactionDao;
import com.auction.server.manager.SystemNotificationManager;
import com.auction.server.networkserver.ClientHandler;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Optional;

final class AuthWalletHandler {
  private final Gson gson;
  private final UserDao userDao;
  private final WalletTransactionDao walletTransactionDao;

  AuthWalletHandler(Gson gson, UserDao userDao, WalletTransactionDao walletTransactionDao) {
    this.gson = gson;
    this.userDao = userDao;
    this.walletTransactionDao = walletTransactionDao;
  }

  String handleDeposit(JsonObject request, ClientHandler client, String reqId) {
    User user = client.getCurrentUser();
    if (user == null) {
      return error(StatusCode.UNAUTHORIZED, "Chưa đăng nhập!", ErrorCode.UNAUTHORIZED, reqId);
    }

    long amount = request.has("amount") ? request.get("amount").getAsLong() : 0;
    if (amount <= 0) {
      return error(StatusCode.BAD_REQUEST, "Số tiền nạp không hợp lệ", ErrorCode.BAD_REQUEST, reqId);
    }

    Optional<User> userOpt = userDao.findByUsername(user.getUsername());
    if (userOpt.isPresent()) {
      User currentUser = userOpt.get();
      Long currentBalance = readBalance(currentUser, reqId);
      if (currentBalance == null) {
        return error(StatusCode.FORBIDDEN, "Tài khoản không hỗ trợ số dư", ErrorCode.FORBIDDEN, reqId);
      }

      long newBalance = currentBalance + amount;
      if (userDao.updateBalance(currentUser.getId(), newBalance)) {
        String description = "Nạp tiền vào ví";
        walletTransactionDao.logTransaction(currentUser.getId(), "DEPOSIT", amount, description);
        sendBalanceChangeNotification(currentUser.getId(), "DEPOSIT", amount, description);
        return walletResponse("TOP_UP_RESPONSE", "Nạp tiền thành công!", newBalance, reqId);
      }
    }
    return error(StatusCode.SERVER_ERROR, "Lỗi khi nạp tiền", ErrorCode.INTERNAL_SERVER_ERROR, reqId);
  }

  String handleWithdraw(JsonObject request, ClientHandler client, String reqId) {
    User user = client.getCurrentUser();
    if (user == null) {
      return error(StatusCode.UNAUTHORIZED, "Chưa đăng nhập!", ErrorCode.UNAUTHORIZED, reqId);
    }

    long amount = request.has("amount") ? request.get("amount").getAsLong() : 0;
    if (amount <= 0) {
      return error(StatusCode.BAD_REQUEST, "Số tiền rút không hợp lệ", ErrorCode.BAD_REQUEST, reqId);
    }

    Optional<User> userOpt = userDao.findByUsername(user.getUsername());
    if (userOpt.isPresent()) {
      User currentUser = userOpt.get();
      Long currentBalance = readBalance(currentUser, reqId);
      if (currentBalance == null) {
        return error(StatusCode.FORBIDDEN, "Tài khoản không hỗ trợ số dư", ErrorCode.FORBIDDEN, reqId);
      }
      if (currentBalance < amount) {
        return error(StatusCode.BAD_REQUEST, "Số dư không đủ để rút", ErrorCode.BAD_REQUEST, reqId);
      }

      long newBalance = currentBalance - amount;
      if (userDao.updateBalance(currentUser.getId(), newBalance)) {
        String description = "Rút tiền từ ví";
        walletTransactionDao.logTransaction(currentUser.getId(), "WITHDRAW", amount, description);
        sendBalanceChangeNotification(currentUser.getId(), "WITHDRAW", amount, description);
        return walletResponse("WITHDRAW_RESPONSE", "Rút tiền thành công!", newBalance, reqId);
      }
    }
    return error(StatusCode.SERVER_ERROR, "Lỗi khi rút tiền", ErrorCode.INTERNAL_SERVER_ERROR, reqId);
  }

  String handleGetWalletHistory(ClientHandler client, String reqId) {
    User user = client.getCurrentUser();
    if (user == null) {
      return error(StatusCode.UNAUTHORIZED, "Chưa đăng nhập!", ErrorCode.UNAUTHORIZED, reqId);
    }

    JsonArray history = walletTransactionDao.getTransactionHistory(user.getId());
    JsonObject response = new JsonObject();
    response.addProperty("type", "WALLET_HISTORY_RESPONSE");
    response.addProperty("success", true);
    response.add("data", history);
    addRequestId(response, reqId);

    userDao.findByUsername(user.getUsername()).ifPresent(currentUser -> {
      Long balance = readBalance(currentUser, reqId);
      if (balance != null) {
        response.addProperty("currentBalance", balance);
      }
    });
    return gson.toJson(response);
  }

  private String walletResponse(String type, String message, long newBalance, String reqId) {
    JsonObject response = new JsonObject();
    response.addProperty("type", type);
    response.addProperty("success", true);
    response.addProperty("message", message);
    response.addProperty("newBalance", newBalance);
    addRequestId(response, reqId);
    return gson.toJson(response);
  }

  private String error(int statusCode, String message, String errorCode, String reqId) {
    BaseDTOs.ErrorResponse error = new BaseDTOs.ErrorResponse(statusCode, message, errorCode);
    error.setRequestId(reqId);
    return gson.toJson(error);
  }

  private Long readBalance(User user, String reqId) {
    try {
      java.lang.reflect.Method getBalance = user.getClass().getMethod("getBalance");
      return (Long) getBalance.invoke(user);
    } catch (Exception e) {
      return null;
    }
  }

  private void addRequestId(JsonObject response, String reqId) {
    if (reqId != null) {
      response.addProperty("requestId", reqId);
    }
  }

  private void sendBalanceChangeNotification(int userId, String transactionType, long amount, String description) {
    String symbol = ("WITHDRAW".equals(transactionType) || "PAYMENT_SENT".equals(transactionType))
        ? "🔻"
        : "🔹";
    String content = String.format("%s %s: %s đ - %s",
        symbol,
        transactionType,
        formatMoney(amount),
        description);
    SystemNotificationManager.getInstance().sendPrivateNotification(
        -1,
        userId,
        "Biến động số dư\n" + content,
        false
    );
  }

  private String formatMoney(long value) {
    String digits = String.valueOf(value);
    StringBuilder formatted = new StringBuilder();
    int firstGroupLength = digits.length() % 3;
    if (firstGroupLength == 0) {
      firstGroupLength = 3;
    }

    formatted.append(digits, 0, firstGroupLength);
    for (int i = firstGroupLength; i < digits.length(); i += 3) {
      formatted.append('.').append(digits, i, i + 3);
    }
    return formatted.toString();
  }
}
