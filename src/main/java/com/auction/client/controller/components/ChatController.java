package com.auction.client.controller.components;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

public class ChatController {
    public static ChatController instance;

    @FXML private ListView<String> lvChatMessages;

    @FXML
    public void initialize() {
        instance = this;
    }

    /**
     * Phương thức này được PushHandler gọi trực tiếp khi có SYSTEM_NOTIFICATION
     */
    public void receiveNotification(String content) {
        Platform.runLater(() -> {
            lvChatMessages.getItems().add(0, "🔔 " + content);
        });
    }

    public void shutdown() {
        instance = null;
    }
}