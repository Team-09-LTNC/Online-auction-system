package com.auction.server.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private final Socket socket;
    private final ServerManager serverManager;
    private final BufferedReader reader;
    private final PrintWriter writer;

    public ClientHandler(Socket socket, ServerManager serverManager) throws IOException {
        this.socket = socket;
        this.serverManager = serverManager;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    @Override
    public void run() {
        try {
            logger.info("Bắt đầu xử lý client: {}", getRemoteAddress());
            String request;
            while ((request = reader.readLine()) != null) {
                logger.info("Nhận từ client {}: {}", getRemoteAddress(), request);
                // TODO: xử lý request JSON/DTO ở đây
                writer.println("Server nhận: " + request);
            }
        } catch (IOException e) {
            logger.error("Lỗi khi đọc/ghi socket của client {}", getRemoteAddress(), e);
        } finally {
            close();
            serverManager.removeClient(this);
        }
    }

    public void sendMessage(String message) {
        writer.println(message);
    }

    public String getRemoteAddress() {
        return socket.getRemoteSocketAddress() != null ? socket.getRemoteSocketAddress().toString() : "unknown";
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            logger.warn("Lỗi khi đóng socket client {}", getRemoteAddress(), e);
        }
    }
}
