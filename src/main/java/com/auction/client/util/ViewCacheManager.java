package com.auction.client.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
// Tải sẵn file FXML đúng 1 lần duy nhất
public class ViewCacheManager {
    private static final Map<String, Parent> viewCache = new HashMap<>();

    public static Parent getView(String fxmlPath) {
        if (!viewCache.containsKey(fxmlPath)) {
            try {
                Parent root = FXMLLoader.load(ViewCacheManager.class.getResource(fxmlPath));
                viewCache.put(fxmlPath, root);
            } catch (IOException e) {
                throw new RuntimeException("Không thể tải giao diện: " + fxmlPath, e);
            }
        }
        return viewCache.get(fxmlPath);
    }

    public static void clear() {
        viewCache.clear();
    }
}
