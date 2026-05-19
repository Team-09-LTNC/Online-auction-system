package com.auction.client.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public class CloudStorageUtil {

    private static final Logger logger = LoggerFactory.getLogger(CloudStorageUtil.class);
    private static final String IMGBB_API_KEY = "8c514c59df230aeb2fa4d82be258a28b";
    private static final String IMGBB_UPLOAD_URL = "https://api.imgbb.com/1/upload?key=" + IMGBB_API_KEY;

    // Chạy bất đồng bộ (async) để không làm đơ giao diện JavaFX khi tải ảnh
    public static CompletableFuture<String> uploadImageAsync(File imageFile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] fileContent = Files.readAllBytes(imageFile.toPath());
                String encodedImage = Base64.getEncoder().encodeToString(fileContent);
                String requestBody = "image=" + java.net.URLEncoder.encode(encodedImage, "UTF-8");

                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(15))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(IMGBB_UPLOAD_URL))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                logger.info("Đang tải ảnh lên đám mây...");
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                if (jsonResponse.has("success") && jsonResponse.get("success").getAsBoolean()) {
                    String directUrl = jsonResponse.getAsJsonObject("data").get("url").getAsString();
                    logger.info("Tải lên thành công: {}", directUrl);
                    return directUrl; // Trả về link ảnh trực tiếp
                } else {
                    throw new RuntimeException("Lỗi API: " + response.body());
                }
            } catch (Exception e) {
                logger.error("Tải ảnh thất bại: {}", e.getMessage());
                return null;
            }
        });
    }
}