package dev.twme.worldeditdisplay.util;

import org.bukkit.Color;

public class ColorUtil {

    /**
     * 解析 #RRGGBB 或 #RRGGBBAA 格式的 HEX 顏色字串
     *
     * @param hex HEX 顏色字串
     * @return Color 物件，解析失敗返回 null
     */
    public static Color parseHexColor(String hex) {
        if (hex == null || !hex.startsWith("#")) return null;

        String h = hex.substring(1);
        try {
            if (h.length() == 6) {
                int r = Integer.parseInt(h.substring(0, 2), 16);
                int g = Integer.parseInt(h.substring(2, 4), 16);
                int b = Integer.parseInt(h.substring(4, 6), 16);
                return Color.fromARGB(255, r, g, b);
            } else if (h.length() == 8) {
                int r = Integer.parseInt(h.substring(0, 2), 16);
                int g = Integer.parseInt(h.substring(2, 4), 16);
                int b = Integer.parseInt(h.substring(4, 6), 16);
                int a = Integer.parseInt(h.substring(6, 8), 16);
                return Color.fromARGB(a, r, g, b);
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    /**
     * 將 Color 轉為 #RRGGBBAA 格式的 HEX 字串
     */
    public static String toHexString(Color color) {
        return String.format("#%02X%02X%02X%02X",
                color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    /**
     * 建立帶 alpha 的 Color
     */
    public static Color toARGB(int r, int g, int b, int a) {
        return Color.fromARGB(a, r, g, b);
    }

    /**
     * 從 CUI 協定顏色直接轉換
     */
    public static Color fromCUIColor(int r, int g, int b, int a) {
        return Color.fromARGB(a, r, g, b);
    }

    /**
     * 驗證 HEX 顏色字串格式是否正確
     */
    public static boolean isValidHexColor(String hex) {
        if (hex == null || !hex.startsWith("#")) return false;
        String h = hex.substring(1);
        if (h.length() != 6 && h.length() != 8) return false;
        try {
            for (char c : h.toCharArray()) {
                Character.digit(c, 16);
                if (Character.digit(c, 16) == -1) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
