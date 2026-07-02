package dev.twme.worldeditdisplay.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Color;
import org.junit.jupiter.api.Test;

class ColorUtilTest {

    @Test
    void parseHexColorSupportsRgbAndRgba() {
        assertEquals(Color.fromARGB(255, 255, 0, 0), ColorUtil.parseHexColor("#FF0000"));
        assertEquals(Color.fromARGB(128, 0, 255, 0), ColorUtil.parseHexColor("#00FF0080"));
    }

    @Test
    void parseHexColorRejectsInvalidInput() {
        assertNull(ColorUtil.parseHexColor(null));
        assertNull(ColorUtil.parseHexColor("FF0000"));
        assertNull(ColorUtil.parseHexColor("#GG0000"));
        assertNull(ColorUtil.parseHexColor("#12345"));
    }

    @Test
    void isValidHexColorRequiresHashAndSixOrEightHexDigits() {
        assertTrue(ColorUtil.isValidHexColor("#AABBCC"));
        assertTrue(ColorUtil.isValidHexColor("#AABBCCDD"));
        assertFalse(ColorUtil.isValidHexColor("AABBCC"));
        assertFalse(ColorUtil.isValidHexColor("#AABBCCD"));
        assertFalse(ColorUtil.isValidHexColor("#AABBCCZZ"));
    }
}