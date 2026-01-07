package dev.twme.worldeditdisplay.common;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.Map;

/**
 * tries to match a color to the closest block material
 */
public class ColorMatcher {

    private static final Map<Material, Color> MATERIAL_COLOR_CACHE = new HashMap<>();
    private static boolean initialized = false;

    // init cache with colors for each material
    public static void initialize() {
        if (initialized) return;

        for (Material material : Constants.COLOR_MATERIALS) {
            try {
                BlockData blockData = material.createBlockData();
                MATERIAL_COLOR_CACHE.put(material, blockData.getMapColor());
            } catch (Exception e) {
                // fallback gray if can't get color
                MATERIAL_COLOR_CACHE.put(material, Color.fromRGB(128, 128, 128));
            }
        }

        initialized = true;
    }

    // simple find closest with rgb + alpha (alpha ignored)
    public static Material findClosestMaterial(int red, int green, int blue, int alpha) {
        return findClosestMaterial(Color.fromRGB(red, green, blue));
    }

    public static Material findClosestMaterial(int red, int green, int blue) {
        return findClosestMaterial(red, green, blue, 255);
    }

    public static Material findClosestMaterial(Color targetColor) {
        if (!initialized) initialize();

        Material closest = Constants.COLOR_MATERIALS[0];
        double minDist = Double.MAX_VALUE;

        for (Map.Entry<Material, Color> entry : MATERIAL_COLOR_CACHE.entrySet()) {
            double dist = colorDistance(targetColor, entry.getValue());
            if (dist < minDist) {
                minDist = dist;
                closest = entry.getKey();
            }
        }

        return closest;
    }

    // normal euclidean distance
    private static double colorDistance(Color c1, Color c2) {
        int dr = c1.getRed() - c2.getRed();
        int dg = c1.getGreen() - c2.getGreen();
        int db = c1.getBlue() - c2.getBlue();
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }

    // keeps weighted distance for future use
    @SuppressWarnings("unused")
    private static double weightedColorDistance(Color c1, Color c2) {
        int dr = c1.getRed() - c2.getRed();
        int dg = c1.getGreen() - c2.getGreen();
        int db = c1.getBlue() - c2.getBlue();
        return Math.sqrt(0.3 * dr * dr + 0.59 * dg * dg + 0.11 * db * db);
    }

    // get cached color
    public static Color getMaterialColor(Material material) {
        if (!initialized) initialize();
        return MATERIAL_COLOR_CACHE.get(material);
    }

    // return copy of all cached colors
    public static Map<Material, Color> getAllMaterialColors() {
        if (!initialized) initialize();
        return new HashMap<>(MATERIAL_COLOR_CACHE);
    }

    // clear and rebuild cache
    public static void refresh() {
        MATERIAL_COLOR_CACHE.clear();
        initialized = false;
        initialize();
    }
}
