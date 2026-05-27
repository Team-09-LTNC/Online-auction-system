package com.auction.client;

import com.auction.client.networkclient.ClientApplication;

public class MainApp {
    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        applyNetworkArgs(args);

        // Gọi ClientApplication
        ClientApplication.main(args);
    }

    private static void applyNetworkArgs(String[] args) {
        if (args == null) {
            return;
        }
        for (String arg : args) {
            if (arg.startsWith("--server-ip=")) {
                System.setProperty("server.ip", arg.substring("--server-ip=".length()));
            } else if (arg.startsWith("--server.ip=")) {
                System.setProperty("server.ip", arg.substring("--server.ip=".length()));
            } else if (arg.startsWith("--server-port=")) {
                System.setProperty("server.port", arg.substring("--server-port=".length()));
            } else if (arg.startsWith("--server.port=")) {
                System.setProperty("server.port", arg.substring("--server.port=".length()));
            }
        }
    }
}
