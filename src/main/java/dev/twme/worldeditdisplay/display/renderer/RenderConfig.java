package dev.twme.worldeditdisplay.display.renderer;

import me.tofaa.entitylib.meta.display.AbstractDisplayMeta;

/**
 * Holds configuration for display rendering.
 * Controls entity appearance, visibility, lighting, and line settings.
 */
public class RenderConfig {

    private int interpolationDelay;           // ticks between interpolation steps
    private int transformationDuration;       // ticks for transformations
    private AbstractDisplayMeta.BillboardConstraints billboardMode;

    private float viewRange;                  // visibility range multiplier

    private boolean alwaysBright;             // override light level
    private int brightnessOverride;           // custom brightness value

    private float shadowRadius;               // shadow size
    private float shadowStrength;             // 0.0 - 1.0

    private int glowColor;                    // RGB value
    private boolean hasGlowColor;             // whether glow color is set

    private float lineThickness;              // line width
    private int lineSegments;                 // number of segments per unit length

    private RenderConfig() {
        interpolationDelay = 0;
        transformationDuration = 5;
        billboardMode = AbstractDisplayMeta.BillboardConstraints.CENTER;
        viewRange = 1.0f;
        alwaysBright = true;
        brightnessOverride = getFullBrightness();
        shadowRadius = 0.0f;
        shadowStrength = 0.0f;
        glowColor = -1;
        hasGlowColor = false;
        lineThickness = 0.03f;
        lineSegments = 4;
    }

    public static RenderConfig getDefault() {
        return new RenderConfig();
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public int getInterpolationDelay() { return interpolationDelay; }
    public int getTransformationDuration() { return transformationDuration; }
    public AbstractDisplayMeta.BillboardConstraints getBillboardMode() { return billboardMode; }
    public float getViewRange() { return viewRange; }
    public boolean isAlwaysBright() { return alwaysBright; }
    public int getBrightnessOverride() { return brightnessOverride; }
    public float getShadowRadius() { return shadowRadius; }
    public float getShadowStrength() { return shadowStrength; }
    public int getGlowColor() { return glowColor; }
    public boolean hasGlowColor() { return hasGlowColor; }
    public float getLineThickness() { return lineThickness; }
    public int getLineSegments() { return lineSegments; }

    // Full brightness calculation
    public static int getFullBrightness() {
        int blockLight = 15;
        int skyLight = 15;
        return (blockLight << 4) | (skyLight << 20);
    }

    // Convert RGB to int
    public static int colorFromRGB(int r, int g, int b) {
        return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static class Builder {
        private final RenderConfig config;

        public Builder() {
            config = new RenderConfig();
        }

        public Builder interpolationDelay(int delay) { config.interpolationDelay = delay; return this; }
        public Builder transformationDuration(int duration) { config.transformationDuration = duration; return this; }
        public Builder billboardMode(AbstractDisplayMeta.BillboardConstraints mode) { config.billboardMode = mode; return this; }
        public Builder viewRange(float range) { config.viewRange = range; return this; }
        public Builder alwaysBright(boolean bright) {
            config.alwaysBright = bright;
            if (!bright) config.brightnessOverride = -1;
            return this;
        }
        public Builder brightnessOverride(int brightness) { config.brightnessOverride = brightness; config.alwaysBright = true; return this; }
        public Builder shadowRadius(float radius) { config.shadowRadius = radius; return this; }
        public Builder shadowStrength(float strength) { config.shadowStrength = Math.max(0.0f, Math.min(1.0f, strength)); return this; }
        public Builder glowColor(int rgb) { config.glowColor = rgb; config.hasGlowColor = true; return this; }
        public Builder glowColorRGB(int r, int g, int b) { return glowColor(colorFromRGB(r, g, b)); }
        public Builder lineThickness(float thickness) { config.lineThickness = Math.max(0.01f, thickness); return this; }
        public Builder lineSegments(int segments) { config.lineSegments = Math.max(1, segments); return this; }

        public RenderConfig build() { return config; }
    }
}
