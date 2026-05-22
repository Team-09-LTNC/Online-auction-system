package com.auction;

import com.auction.client.networkclient.Launcher;
import com.auction.server.ServerApplication;
import java.io.PrintStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;

public class AppRunner {
    public static void main(String[] args) {
        configureConsoleEncoding();

        int serverPort = ServerApplication.getPort();
        if (!isLocalPortBusy(serverPort)) {
            startEmbeddedServer(args);
            waitForServerStartup();
        } else {
            System.out.println("=== CỔNG " + serverPort
                    + " ĐANG ĐƯỢC DÙNG, BỎ QUA SERVER NHÚNG ===");
        }

        System.out.println("=== ĐANG KHỞI ĐỘNG CLIENT ===");
        Launcher.main(args);
    }

    private static void startEmbeddedServer(String[] args) {
        Thread serverThread = new Thread(() -> {
            try {
                System.out.println("=== ĐANG KHỞI ĐỘNG SERVER ===");
                ServerApplication.main(args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "embedded-auction-server");

        serverThread.setDaemon(true);
        serverThread.start();
    }

    private static void waitForServerStartup() {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean isLocalPortBusy(int port) {
        try (ServerSocket probeSocket = new ServerSocket()) {
            probeSocket.bind(new InetSocketAddress("127.0.0.1", port));
            return false;
        } catch (BindException e) {
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void configureConsoleEncoding() {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
    }
}
