package dev.twme.worldeditdisplay.display.renderer;

/**
 * Holds configuration for display rendering.
 * Controls appearance, visibility, and line settings.
 */
public class RenderConfig {

    private float viewRange;
    private boolean seeThrough;
    private int blockLight;
    private int skyLight;
    private float lineThickness;
    private int lineSegments;

    private RenderConfig() {
        viewRange = 1.0f;
        seeThrough = true;
        blockLight = 15;
        skyLight = 15;
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
    public float getViewRange() { return viewRange; }
    public boolean isSeeThrough() { return seeThrough; }
    public int getBlockLight() { return blockLight; }
    public int getSkyLight() { return skyLight; }
    public float getLineThickness() { return lineThickness; }
    public int getLineSegments() { return lineSegments; }

    public static class Builder {
        private final RenderConfig config;

        public Builder() {
            config = new RenderConfig();
        }

        public Builder viewRange(float range) { config.viewRange = range; return this; }
        public Builder seeThrough(boolean seeThrough) { config.seeThrough = seeThrough; return this; }
        public Builder blockLight(int blockLight) { config.blockLight = blockLight; return this; }
        public Builder skyLight(int skyLight) { config.skyLight = skyLight; return this; }
        public Builder lineThickness(float thickness) { config.lineThickness = Math.max(0.01f, thickness); return this; }
        public Builder lineSegments(int segments) { config.lineSegments = Math.max(1, segments); return this; }

        public RenderConfig build() { return config; }
    }
}
