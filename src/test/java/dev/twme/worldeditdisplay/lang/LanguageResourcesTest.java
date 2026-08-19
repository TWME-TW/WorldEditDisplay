package dev.twme.worldeditdisplay.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class LanguageResourcesTest {

    private static final List<String> DISPLAY_SETTING_PATHS = List.of(
            "cuboid.max_grid_spacing",
            "cylinder.sqrt_scale_factor",
            "cylinder.max_grid_spacing",
            "ellipsoid.sqrt_scale_factor",
            "ellipsoid.max_grid_spacing",
            "polygon.max_grid_spacing");

    @Test
    void loadsEveryBundledLanguage() throws IOException {
        assertEquals(List.of("zh_tw", "zh_cn", "en_us"), LanguageManager.DEFAULT_LANGUAGES);

        for (String language : LanguageManager.DEFAULT_LANGUAGES) {
            try (InputStream stream = getClass().getResourceAsStream("/lang/" + language + ".yml")) {
                assertNotNull(stream, language);
                Map<String, Object> root = new Yaml().load(stream);
                assertNotNull(valueAt(root, "command.wedisplay.view.usage"), language);
                for (String settingPath : DISPLAY_SETTING_PATHS) {
                    assertNotNull(valueAt(root, "settings." + settingPath), language + ": " + settingPath);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Object valueAt(Map<String, Object> root, String path) {
        Object value = root;
        for (String key : path.split("\\.")) {
            if (!(value instanceof Map<?, ?> section)) return null;
            value = ((Map<String, Object>) section).get(key);
        }
        return value;
    }
}
