package dev.twme.worldeditdisplay.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlayerSettingsCommandTest {

    private PlayerSettingsCommand command;
    private Method parseValue;
    private Method isValidSetting;
    private Method requiresSettingsPermission;

    @BeforeEach
    void setUp() throws Exception {
        command = new PlayerSettingsCommand(null);

        parseValue = PlayerSettingsCommand.class.getDeclaredMethod("parseValue", String.class, String.class);
        parseValue.setAccessible(true);

        isValidSetting = PlayerSettingsCommand.class.getDeclaredMethod("isValidSetting", String.class, String.class);
        isValidSetting.setAccessible(true);

        requiresSettingsPermission = PlayerSettingsCommand.class.getDeclaredMethod("requiresSettingsPermission", String.class);
        requiresSettingsPermission.setAccessible(true);
    }

    @Test
    void parseValueAcceptsOnlyStrictBooleans() throws Exception {
        assertEquals(Boolean.TRUE, parseValue.invoke(command, "fill_enabled", "true"));
        assertEquals(Boolean.FALSE, parseValue.invoke(command, "fill_enabled", "FALSE"));
        assertNull(parseValue.invoke(command, "fill_enabled", "maybe"));
    }

    @Test
    void parseValueNormalizesHexColor() throws Exception {
        assertEquals("#FF0000FF", parseValue.invoke(command, "edge_color", "FF0000FF"));
        assertNull(parseValue.invoke(command, "edge_color", "not-a-color"));
    }

    @Test
    void settingWhitelistRejectsUnknownKeys() throws Exception {
        assertTrue((Boolean) isValidSetting.invoke(command, "cuboid", "edge_color"));
        assertFalse((Boolean) isValidSetting.invoke(command, "cuboid", "unknown_setting"));
        assertFalse((Boolean) isValidSetting.invoke(command, "missing", "edge_color"));
    }

    @Test
    void onlySettingsCommandsRequireSettingsPermission() throws Exception {
        assertTrue((Boolean) requiresSettingsPermission.invoke(command, "set"));
        assertTrue((Boolean) requiresSettingsPermission.invoke(command, "reloadplayer"));
        assertTrue((Boolean) requiresSettingsPermission.invoke(command, "language"));

        assertFalse((Boolean) requiresSettingsPermission.invoke(command, "toggle"));
        assertFalse((Boolean) requiresSettingsPermission.invoke(command, "render"));
        assertFalse((Boolean) requiresSettingsPermission.invoke(command, "share"));
        assertFalse((Boolean) requiresSettingsPermission.invoke(command, "view"));
    }
}