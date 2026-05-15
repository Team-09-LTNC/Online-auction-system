package com.auction.server.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerManager {
    private final int port;
    private final List<ClientHandler> clients;
    private ServerSocket serverSocket;

    private static final Logger logger = LoggerFactory.getLogger(ServerManager.class);

    public ServerManager(int port) {
        this.port = port;
        this.clients = new CopyOnWriteArrayList<>();
    }

    public ServerManager(int port, List<ClientHandler> clients) {
        this.port = port;
        this.clients = new CopyOnWriteArrayList<>(clients);
    }

    public void startServer() {
        try (ServerSocket server = new ServerSocket(port)) {
            this.serverSocket = server;
            logger.info("Server đang lắng nghe cổng {}", port);

            while (!server.isClosed()) {
                Socket socket = server.accept();
                logger.info("Client mới kết nối: {}", socket.getRemoteSocketAddress());

                ClientHandler clientHandler = new ClientHandler(socket, this);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            logger.error("Lỗi khi khởi tạo ServerSocket hoặc accept client", e);
        }
    }

    public void removeClient(ClientHandler handler) {
        clients.remove(handler);
        logger.info("Xóa client khỏi danh sách: {}", handler.getRemoteAddress());
    }

    public void stopServer() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                logger.info("Server đã dừng");
            } catch (IOException e) {
                logger.error("Lỗi khi dừng ServerSocket", e);
            }
        }
    }

    public List<ClientHandler> getClients() {
        return clients;
    }
}
