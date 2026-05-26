package com.auction.client.networkclient;

import com.auction.common.dto.BaseDTOs;
import com.auction.common.util.GsonConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientSocket {
  private static final Logger logger = LoggerFactory.getLogger(ClientSocket.class);
  private static String serverIp = "127.0.0.1";
  private static int serverPort = 8080;

  static {
    try (InputStream input = ClientSocket.class.getClassLoader()
        .getResourceAsStream("application.properties")) {
      if (input != null) {
        Properties props = new Properties();
        props.load(input);
        serverIp = props.getProperty("server.ip", "127.0.0.1");
        serverPort = Integer.parseInt(props.getProperty("server.port", "8080"));
        logger.info("Da nap cau hinh mang: IP = {}, Port = {}", serverIp, serverPort);
      }
    } catch (Exception e) {
      logger.error("Loi khi doc cau hinh mang, su dung mac dinh.", e);
    }
  }

  private static ClientSocket instance;

  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;
  private final Gson gson = GsonConfig.getInstance();
  private final Map<String, Consumer<JsonObject>> responseCallbacks = new ConcurrentHashMap<>();

  private ClientSocket() {
    connect();
  }

  public static synchronized ClientSocket getInstance() {
    if (instance == null) {
      instance = new ClientSocket();
    }
    return instance;
  }

  public void sendJsonRequest(
      JsonObject jsonObject,
      String expectedResponseType,
      Consumer<JsonObject> onResponse
  ) {
    if (!isSocketReady()) {
      connect();
    }
    if (out == null) {
      logger.error("Socket chua san sang!");
      return;
    }

    String requestId = jsonObject.has("requestId") && !jsonObject.get("requestId").isJsonNull()
        ? jsonObject.get("requestId").getAsString()
        : null;
    if (requestId == null || requestId.isBlank()) {
      requestId = UUID.randomUUID().toString();
      jsonObject.addProperty("requestId", requestId);
      logger.warn("Request thieu requestId, da tu sinh requestId={}", requestId);
    }

    if (onResponse != null) {
      if (requestId != null) {
        responseCallbacks.put(requestId, onResponse);
      } else if (expectedResponseType != null) {
        responseCallbacks.put(expectedResponseType, onResponse);
      }
    }

    String jsonPayload = gson.toJson(jsonObject);
    out.println(jsonPayload);
    logger.debug("Request -> Server: {}", jsonPayload);
  }

  public void sendRequest(BaseDTOs.Request request) {
    JsonObject jsonObject = gson.toJsonTree(request).getAsJsonObject();
    sendJsonRequest(jsonObject, null, null);
  }

  private void connect() {
    for (int attempt = 1; attempt <= 30; attempt++) {
      if (connectOnce()) {
        return;
      }
      try {
        Thread.sleep(250);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
    }
  }

  private boolean connectOnce() {
    try {
      socket = new Socket(serverIp, serverPort);
      out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
      logger.info("Da ket noi Server {}:{}", serverIp, serverPort);
      startListeningThread();
      return true;
    } catch (IOException e) {
      logger.warn("Ket noi Server that bai: {}", e.getMessage());
      return false;
    }
  }

  private boolean isSocketReady() {
    return socket != null
        && socket.isConnected()
        && !socket.isClosed()
        && out != null
        && in != null;
  }

  private void startListeningThread() {
    Thread listenerThread = new Thread(() -> {
      try {
        String responseLine;
        while ((responseLine = in.readLine()) != null) {
          handleServerResponse(responseLine);
        }
      } catch (IOException e) {
        logger.error("Mat ket noi doc: {}", e.getMessage());
      }
    });
    listenerThread.setDaemon(true);
    listenerThread.start();
  }

  private void handleServerResponse(String json) {
    try {
      JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
      String type = jsonObject.has("type") && !jsonObject.get("type").isJsonNull()
          ? jsonObject.get("type").getAsString()
          : null;
      String requestId = jsonObject.has("requestId") && !jsonObject.get("requestId").isJsonNull()
          ? jsonObject.get("requestId").getAsString()
          : null;

      if (type != null && isPushEvent(type)) {
        PushHandler.handle(type, jsonObject);
        return;
      }

      Consumer<JsonObject> callback = null;
      if (requestId != null && responseCallbacks.containsKey(requestId)) {
        callback = responseCallbacks.remove(requestId);
      } else if (type != null && responseCallbacks.containsKey(type)) {
        callback = responseCallbacks.remove(type);
      }

      if (callback != null) {
        callback.accept(jsonObject);
      } else {
        logger.debug(
            "Nhan phan hoi nhung khong co Callback (requestId: {}, type: {})",
            requestId,
            type);
      }
    } catch (JsonSyntaxException e) {
      logger.error("JSON loi tu Server: {}", e.getMessage());
    }
  }

  private boolean isPushEvent(String type) {
    return com.auction.common.enums.ActionType.AUCTION_BID_UPDATE.equals(type)
        || com.auction.common.enums.ActionType.AUCTION_RESULT.equals(type)
        || com.auction.common.enums.ActionType.AUCTION_CHANGED.equals(type)
        || com.auction.common.enums.ActionType.RECEIVE_CHAT_MESSAGE.equals(type)
        || "SYSTEM_NOTIFICATION".equals(type);
  }
}
