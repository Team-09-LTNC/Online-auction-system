package com.auction.client.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

public class CloudStorageUtil {

    private static final Logger logger = LoggerFactory.getLogger(CloudStorageUtil.class);
    private static final String IMGBB_API_KEY = "8c514c59df230aeb2fa4d82be258a28b";
    private static final String IMGBB_UPLOAD_URL = "https://api.imgbb.com/1/upload?key=" + IMGBB_API_KEY;
    private static final int DETAIL_MAX_WIDTH = 1080;
    private static final int THUMB_MAX_WIDTH = 480;
    private static final float JPG_QUALITY = 0.78f;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public static class UploadedImage {
        private final String imageUrl;
        private final String thumbnailUrl;

        public UploadedImage(String imageUrl, String thumbnailUrl) {
            this.imageUrl = imageUrl;
            this.thumbnailUrl = thumbnailUrl;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public String getThumbnailUrl() {
            return thumbnailUrl;
        }
    }

    // Chạy bất đồng bộ (async) để không làm đơ giao diện JavaFX khi tải ảnh
    public static CompletableFuture<String> uploadImageAsync(File imageFile) {
        return uploadProductImageAsync(imageFile).thenApply(UploadedImage::getImageUrl);
    }

    public static CompletableFuture<UploadedImage> uploadProductImageAsync(File imageFile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BufferedImage source = ImageIO.read(imageFile);
                if (source == null) {
                    throw new RuntimeException("Khong doc duoc dinh dang anh: " + imageFile.getName());
                }

                byte[] detailImage = resizeAndEncodeJpeg(source, DETAIL_MAX_WIDTH);
                byte[] thumbnailImage = resizeAndEncodeJpeg(source, THUMB_MAX_WIDTH);

                logger.info("Dang tai anh san pham da toi uu len ImgBB...");
                String imageUrl = uploadJpegBytes(detailImage);
                String thumbnailUrl = uploadJpegBytes(thumbnailImage);
                logger.info("Tai anh san pham thanh cong. imageUrl={}, thumbnailUrl={}", imageUrl, thumbnailUrl);
                return new UploadedImage(imageUrl, thumbnailUrl);
            } catch (Exception e) {
                logger.error("Tai anh that bai: {}", e.getMessage());
                return null;
            }
        });
    }

    private static String uploadJpegBytes(byte[] imageBytes) throws Exception {
        String encodedImage = Base64.getEncoder().encodeToString(imageBytes);
        String requestBody = "image=" + java.net.URLEncoder.encode(encodedImage, "UTF-8");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(IMGBB_UPLOAD_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
        if (jsonResponse.has("success") && jsonResponse.get("success").getAsBoolean()) {
            return jsonResponse.getAsJsonObject("data").get("url").getAsString();
        }
        throw new RuntimeException("Loi API ImgBB: " + response.body());
    }

    private static byte[] resizeAndEncodeJpeg(BufferedImage source, int maxWidth) throws Exception {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        double scale = sourceWidth > maxWidth ? (double) maxWidth / sourceWidth : 1.0d;
        int targetWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
        int targetHeight = Math.max(1, (int) Math.round(sourceHeight * scale));

        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, targetWidth, targetHeight);
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }

        return encodeJpeg(resized);
    }

    private static byte[] encodeJpeg(BufferedImage image) throws Exception {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new RuntimeException("Khong tim thay JPEG writer.");
        }

        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam params = writer.getDefaultWriteParam();
            if (params.canWriteCompressed()) {
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(JPG_QUALITY);
            }
            writer.write(null, new IIOImage(image, null, null), params);
            return output.toByteArray();
        } finally {
            writer.dispose();
        }
    }
}
