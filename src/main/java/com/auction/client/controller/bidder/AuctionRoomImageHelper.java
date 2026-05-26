package com.auction.client.controller.bidder;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

final class AuctionRoomImageHelper {
    private final ImageView imageView;
    private final StackPane imageFrame;
    private final double frameHeight;
    private Image observedImage;
    private final ChangeListener<Number> dimensionListener =
            (obs, oldValue, newValue) -> updateViewport();

    AuctionRoomImageHelper(ImageView imageView, StackPane imageFrame, double frameHeight) {
        this.imageView = imageView;
        this.imageFrame = imageFrame;
        this.frameHeight = frameHeight;
    }

    void install() {
        if (imageView == null || imageFrame == null) {
            return;
        }

        imageFrame.setMinHeight(frameHeight);
        imageFrame.setPrefHeight(frameHeight);
        imageFrame.setMaxHeight(frameHeight);
        imageFrame.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(imageFrame, Priority.NEVER);

        imageView.setManaged(false);
        imageView.setPreserveRatio(false);
        imageView.fitWidthProperty().bind(imageFrame.widthProperty());
        imageView.fitHeightProperty().bind(imageFrame.heightProperty());

        Rectangle clip = new Rectangle();
        clip.arcWidthProperty().set(20);
        clip.arcHeightProperty().set(20);
        clip.widthProperty().bind(imageFrame.widthProperty());
        clip.heightProperty().bind(imageFrame.heightProperty());
        imageFrame.setClip(clip);

        imageFrame.widthProperty().addListener(dimensionListener);
        imageFrame.heightProperty().addListener(dimensionListener);
        imageView.imageProperty().addListener((obs, oldImage, newImage) -> {
            observeImage(oldImage, newImage);
            updateViewport();
        });
    }

    private void observeImage(Image oldImage, Image newImage) {
        if (oldImage != null && oldImage == observedImage) {
            oldImage.widthProperty().removeListener(dimensionListener);
            oldImage.heightProperty().removeListener(dimensionListener);
        }
        observedImage = newImage;
        if (newImage != null) {
            newImage.widthProperty().addListener(dimensionListener);
            newImage.heightProperty().addListener(dimensionListener);
        }
    }

    private void updateViewport() {
        if (imageView == null || imageFrame == null || imageView.getImage() == null) {
            return;
        }

        Image image = imageView.getImage();
        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();
        double frameWidth = imageFrame.getWidth();
        double frameHeightValue = imageFrame.getHeight();
        if (imageWidth <= 0 || imageHeight <= 0 || frameWidth <= 0 || frameHeightValue <= 0) {
            return;
        }

        double frameRatio = frameWidth / frameHeightValue;
        double imageRatio = imageWidth / imageHeight;
        double viewportWidth = imageWidth;
        double viewportHeight = imageHeight;
        if (imageRatio > frameRatio) {
            viewportWidth = imageHeight * frameRatio;
        } else {
            viewportHeight = imageWidth / frameRatio;
        }

        double x = (imageWidth - viewportWidth) / 2;
        double y = (imageHeight - viewportHeight) / 2;
        imageView.setViewport(new Rectangle2D(x, y, viewportWidth, viewportHeight));
    }
}
