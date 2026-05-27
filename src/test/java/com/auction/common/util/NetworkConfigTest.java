package com.auction.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NetworkConfigTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        System.clearProperty("network.config.file");
        System.clearProperty("app.config.file");
        System.clearProperty("server.ip");
        System.clearProperty("server.port");
    }

    @Test
    void readsNetworkConfigFromExternalFile() throws IOException {
        Path configFile = tempDir.resolve("client.properties");
        Files.writeString(configFile, "server.ip=192.168.1.15\nserver.port=9090\n");
        System.setProperty("network.config.file", configFile.toString());

        assertEquals("192.168.1.15", NetworkConfig.getServerIp());
        assertEquals(9090, NetworkConfig.getServerPort());
    }

    @Test
    void systemPropertiesOverrideExternalFile() throws IOException {
        Path configFile = tempDir.resolve("client.properties");
        Files.writeString(configFile, "server.ip=192.168.1.15\nserver.port=9090\n");
        System.setProperty("network.config.file", configFile.toString());
        System.setProperty("server.ip", "10.0.0.8");
        System.setProperty("server.port", "8181");

        assertEquals("10.0.0.8", NetworkConfig.getServerIp());
        assertEquals(8181, NetworkConfig.getServerPort());
    }
}
