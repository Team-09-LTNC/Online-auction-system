package com.auction.client.controller.seller;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

record PostAuctionPreviewControls(
    TextField txtProductName,
    ComboBox<String> cbCategory,
    TextArea txtDescription,
    TextField txtStartingPrice,
    TextField txtIncrement,
    TextField txtBuyNowPrice,
    DatePicker dpStartDate,
    TextField txtStartTime,
    DatePicker dpEndDate,
    TextField txtEndTime,
    CheckBox chkAntiSniping,
    Label lblPreviewName,
    Label lblPreviewCategory,
    Label lblPreviewPrice,
    Label lblPreviewIncrement,
    Label lblPreviewBuyNow,
    Label lblPreviewTime,
    Label lblPreviewAntiSniping,
    Label lblDescriptionCount,
    Label lblProfileName,
    Label lblProfileRole
) {
}
