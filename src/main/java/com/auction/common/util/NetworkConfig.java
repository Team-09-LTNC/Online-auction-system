package com.auction.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class NetworkConfig {
    private static final String DEFAULT_CONFIG_FILE = "application.properties";
    private static final String DEFAULT_SERVER_IP = "127.0.0.1";
    private static final int DEFAULT_SERVER_PORT = 8080;

    private NetworkConfig() {
    }

    public static String getServerIp() {
        Properties props = loadProperties();
        return firstNonBlank(
                System.getProperty("server.ip"),
                System.getenv("AUCTION_SERVER_IP"),
                props.getProperty("server.ip"),
                DEFAULT_SERVER_IP
        );
    }

    public static int getServerPort() {
        Properties props = loadProperties();
        String port = firstNonBlank(
                System.getProperty("server.port"),
                System.getenv("AUCTION_SERVER_PORT"),
                props.getProperty("server.port"),
                String.valueOf(DEFAULT_SERVER_PORT)
        );
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException e) {
            return DEFAULT_SERVER_PORT;
        }
    }

    private static Properties loadProperties() {
        String configFile = firstNonBlank(
                System.getProperty("network.config.file"),
                System.getProperty("app.config.file"),
                DEFAULT_CONFIG_FILE
        );
        Properties props = new Properties();
        try (InputStream input = openConfig(configFile)) {
            if (input != null) {
                props.load(input);
            }
        } catch (IOException ignored) {
            // Giá trị mặc định dự phòng là đủ cho môi trường phát triển cục bộ.
        }
        return props;
    }

    private static InputStream openConfig(String configFile) throws IOException {
        Path configuredPath = Path.of(configFile);
        if (Files.isRegularFile(configuredPath)) {
            return Files.newInputStream(configuredPath);
        }

        Path appDir = getApplicationDirectory();
        if (appDir != null) {
            Path appConfigPath = appDir.resolve(configuredPath);
            if (Files.isRegularFile(appConfigPath)) {
                return Files.newInputStream(appConfigPath);
            }
        }

        Path mainResourcePath = Path.of("src", "main", "resources", configFile);
        if (Files.isRegularFile(mainResourcePath)) {
            return Files.newInputStream(mainResourcePath);
        }

        ClassLoader classLoader = NetworkConfig.class.getClassLoader();
        InputStream input = classLoader.getResourceAsStream(configFile);
        if (input != null) {
            return input;
        }

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null && contextClassLoader != classLoader) {
            return contextClassLoader.getResourceAsStream(configFile);
        }

        return null;
    }

    private static Path getApplicationDirectory() {
        try {
            Path codeSource = Path.of(
                    NetworkConfig.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            );
            if (Files.isRegularFile(codeSource)) {
                return codeSource.getParent();
            }
            return codeSource;
        } catch (Exception e) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
