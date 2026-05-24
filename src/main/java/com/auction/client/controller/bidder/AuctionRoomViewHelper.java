package com.auction.client.controller.bidder;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.Optional;

final class AuctionRoomViewHelper {
    private AuctionRoomViewHelper() {
    }

    static void updateCountdownLabel(Label lblCountdown, boolean started, int totalSeconds) {
        if (lblCountdown == null) {
            return;
        }
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;

        if (!started) {
            lblCountdown.setText(String.format("Sap mo: %02d:%02d:%02d", h, m, s));
            lblCountdown.setStyle("-fx-text-fill: #E65100; -fx-font-weight: bold;");
        } else {
            lblCountdown.setText(String.format("Con lai: %02d:%02d:%02d", h, m, s));
            lblCountdown.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
        }
    }

    static void setExpiredUI(
            Label lblCountdown,
            Button btnPlaceBid,
            Button btnEnableAutoBid,
            TextField txtBidAmount
    ) {
        if (lblCountdown != null) {
            lblCountdown.setText("DA KET THUC!");
            lblCountdown.setStyle("-fx-text-fill: #888888; -fx-font-weight: bold;");
        }
        if (btnPlaceBid != null) {
            btnPlaceBid.setDisable(true);
            btnPlaceBid.setText("HET HAN");
        }
        if (btnEnableAutoBid != null) {
            btnEnableAutoBid.setDisable(true);
        }
        if (txtBidAmount != null) {
            txtBidAmount.setEditable(false);
        }
    }

    static long parseMoneyLabel(Label label) {
        if (label == null || label.getText() == null) {
            return 0;
        }
        return Long.parseLong(label.getText().replaceAll("\\D", ""));
    }

    static void showWarning(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    static void showInfo(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getDialogPane().setStyle("-fx-background-color: #FFFDFC; -fx-font-size: 13px;");
        alert.showAndWait();
    }

    static boolean confirmBuyNow() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Xac nhan mua dut");
        dialog.setHeaderText(null);

        ButtonType cancelButton = new ButtonType("Huy", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirmButton = new ButtonType("Xac nhan", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(cancelButton, confirmButton);

        Label message = new Label(
                "Ban da dat gia vuot qua gia mua dut cua san pham.\n\n"
                        + "Xac nhan neu ban muon so huu san pham nay ngay lap tuc.\n\n"
                        + "Huy neu ban muon dat muc gia thap hon."
        );
        message.setWrapText(true);
        message.setStyle("-fx-font-size: 14px; -fx-text-fill: #342724; -fx-line-spacing: 2px;");
        dialog.getDialogPane().setContent(message);
        dialog.getDialogPane().setPrefWidth(450);
        dialog.getDialogPane().setStyle("-fx-background-color: #FFFDFC; -fx-padding: 14px;");

        Button cancel = (Button) dialog.getDialogPane().lookupButton(cancelButton);
        cancel.setStyle("-fx-background-color: #F0ECE8; -fx-text-fill: #3E2723; "
                + "-fx-font-weight: bold; -fx-background-radius: 7; -fx-padding: 9 20;");
        Button confirm = (Button) dialog.getDialogPane().lookupButton(confirmButton);
        confirm.setStyle("-fx-background-color: #B32638; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 7; -fx-padding: 9 20;");

        Optional<ButtonType> selected = dialog.showAndWait();
        return selected.isPresent() && selected.get() == confirmButton;
    }
}
