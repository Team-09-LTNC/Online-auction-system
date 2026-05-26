package com.auction.client.controller.bidder;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

final class AuctionRoomMoneyFormatter {
    private AuctionRoomMoneyFormatter() {
    }

    static long extractMoneyValue(String text) {
        return parseMoneyValue(text);
    }

    static long parseMoneyValue(String text) {
        String digits = getDigitsOnly(text);
        if (digits.isEmpty()) {
            throw new NumberFormatException("Empty money value");
        }
        return Long.parseLong(digits);
    }

    static String formatVnd(long value) {
        return formatMoney(value) + " đ";
    }

    static String formatMoney(long value) {
        return formatDigitsWithDots(String.valueOf(value));
    }

    static void install(TextField textField) {
        if (textField == null) {
            return;
        }

        textField.setTextFormatter(new TextFormatter<String>(change -> {
            String proposedText = change.getControlNewText();
            String digits = getDigitsOnly(proposedText);
            String formatted = formatDigitsWithDots(digits);

            int proposedCaret = Math.max(0, Math.min(change.getCaretPosition(), proposedText.length()));
            int digitsBeforeCaret = countDigits(proposedText.substring(0, proposedCaret));
            int newCaret = calculateCaretPosition(formatted, digitsBeforeCaret);

            change.setRange(0, change.getControlText().length());
            change.setText(formatted);
            change.setCaretPosition(newCaret);
            change.setAnchor(newCaret);
            return change;
        }));
    }

    private static String getDigitsOnly(String text) {
        return text == null ? "" : text.replaceAll("\\D", "");
    }

    private static String formatDigitsWithDots(String digits) {
        if (digits == null || digits.isBlank()) {
            return "";
        }

        String normalized = digits.replaceFirst("^0+(?!$)", "");
        StringBuilder formatted = new StringBuilder();
        int firstGroupLength = normalized.length() % 3;
        if (firstGroupLength == 0) {
            firstGroupLength = 3;
        }

        formatted.append(normalized, 0, firstGroupLength);
        for (int i = firstGroupLength; i < normalized.length(); i += 3) {
            formatted.append('.').append(normalized, i, i + 3);
        }
        return formatted.toString();
    }

    private static int countDigits(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    private static int calculateCaretPosition(String text, int digitsBeforeCaret) {
        if (digitsBeforeCaret <= 0) {
            return 0;
        }

        int digitsSeen = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) {
                digitsSeen++;
                if (digitsSeen == digitsBeforeCaret) {
                    return i + 1;
                }
            }
        }
        return text.length();
    }
}
