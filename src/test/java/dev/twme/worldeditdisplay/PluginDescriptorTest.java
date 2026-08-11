package dev.twme.worldeditdisplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class PluginDescriptorTest {

    @Test
    void supportsPaper119() throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/plugin.yml")) {
            assertNotNull(stream);
            Map<String, Object> descriptor = new Yaml().load(stream);
            assertEquals("1.19", descriptor.get("api-version"));
        }
    }
}
