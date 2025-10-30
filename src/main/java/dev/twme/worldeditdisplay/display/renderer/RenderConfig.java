package dev.twme.worldeditdisplay.display.renderer;

import me.tofaa.entitylib.meta.display.AbstractDisplayMeta;

/**
 * 渲染配置類
 * 
 * 管理顯示實體的外觀和行為參數
 */
public class RenderConfig {
    
    // 插值設定
    private int interpolationDelay;           // 插值延遲(ticks)
    private int transformationDuration;       // 變換持續時間(ticks)
    
    // 朝向設定
    private AbstractDisplayMeta.BillboardConstraints billboardMode;
    
    // 可見性設定
    private float viewRange;                  // 可見距離倍率
    
    // 亮度設定
    private boolean alwaysBright;             // 是否始終最亮
    private int brightnessOverride;           // 亮度覆蓋值
    
    // 陰影設定
    private float shadowRadius;               // 陰影半徑
    private float shadowStrength;             // 陰影強度(0.0 - 1.0)
    
    // 發光設定
    private int glowColor;                    // 發光顏色(RGB)
    private boolean hasGlowColor;             // 是否設定發光顏色
    
    // 外觀設定
    private float lineThickness;              // 線條粗細
    private int lineSegments;                 // 線條分段數
    
    /**
     * 私有建構子,使用建造者模式
     */
    private RenderConfig() {
        // 預設值
        this.interpolationDelay = 0;
        this.transformationDuration = 5; // 0.25秒
        this.billboardMode = AbstractDisplayMeta.BillboardConstraints.CENTER;
        this.viewRange = 1.0f;
        this.alwaysBright = true;
        this.brightnessOverride = getFullBrightness();
        this.shadowRadius = 0.0f;
        this.shadowStrength = 0.0f;
        this.glowColor = -1;
        this.hasGlowColor = false;
        this.lineThickness = 0.03f;
        this.lineSegments = 4; // 每單位長度4個點
    }
    
    /**
     * 獲取預設配置
     * 
     * @return 預設 RenderConfig
     */
    public static RenderConfig getDefault() {
        return new RenderConfig();
    }
    
    /**
     * 創建建造者
     * 
     * @return Builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    
    public int getInterpolationDelay() {
        return interpolationDelay;
    }
    
    public int getTransformationDuration() {
        return transformationDuration;
    }
    
    public AbstractDisplayMeta.BillboardConstraints getBillboardMode() {
        return billboardMode;
    }
    
    public float getViewRange() {
        return viewRange;
    }
    
    public boolean isAlwaysBright() {
        return alwaysBright;
    }
    
    public int getBrightnessOverride() {
        return brightnessOverride;
    }
    
    public float getShadowRadius() {
        return shadowRadius;
    }
    
    public float getShadowStrength() {
        return shadowStrength;
    }
    
    public int getGlowColor() {
        return glowColor;
    }
    
    public boolean hasGlowColor() {
        return hasGlowColor;
    }
    
    public float getLineThickness() {
        return lineThickness;
    }
    
    public int getLineSegments() {
        return lineSegments;
    }
    
    /**
     * 計算完全明亮的亮度值
     * 格式: (blockLight << 4) | (skyLight << 20)
     * 
     * @return 亮度值
     */
    public static int getFullBrightness() {
        int blockLight = 15; // 最大方塊光照
        int skyLight = 15;   // 最大天空光照
        return (blockLight << 4) | (skyLight << 20);
    }
    
    /**
     * 從 RGB 創建發光顏色
     * 
     * @param r 紅色 (0-255)
     * @param g 綠色 (0-255)
     * @param b 藍色 (0-255)
     * @return RGB 顏色值
     */
    public static int colorFromRGB(int r, int g, int b) {
        return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }
    
    /**
     * 建造者模式
     */
    public static class Builder {
        private final RenderConfig config;
        
        public Builder() {
            this.config = new RenderConfig();
        }
        
        public Builder interpolationDelay(int delay) {
            config.interpolationDelay = delay;
            return this;
        }
        
        public Builder transformationDuration(int duration) {
            config.transformationDuration = duration;
            return this;
        }
        
        public Builder billboardMode(AbstractDisplayMeta.BillboardConstraints mode) {
            config.billboardMode = mode;
            return this;
        }
        
        public Builder viewRange(float range) {
            config.viewRange = range;
            return this;
        }
        
        public Builder alwaysBright(boolean bright) {
            config.alwaysBright = bright;
            if (!bright) {
                config.brightnessOverride = -1; // 使用環境光照
            }
            return this;
        }
        
        public Builder brightnessOverride(int brightness) {
            config.brightnessOverride = brightness;
            config.alwaysBright = true;
            return this;
        }
        
        public Builder shadowRadius(float radius) {
            config.shadowRadius = radius;
            return this;
        }
        
        public Builder shadowStrength(float strength) {
            config.shadowStrength = Math.max(0.0f, Math.min(1.0f, strength));
            return this;
        }
        
        public Builder glowColor(int rgb) {
            config.glowColor = rgb;
            config.hasGlowColor = true;
            return this;
        }
        
        public Builder glowColorRGB(int r, int g, int b) {
            return glowColor(colorFromRGB(r, g, b));
        }
        
        public Builder lineThickness(float thickness) {
            config.lineThickness = Math.max(0.01f, thickness);
            return this;
        }
        
        public Builder lineSegments(int segments) {
            config.lineSegments = Math.max(1, segments);
            return this;
        }
        
        public RenderConfig build() {
            return config;
        }
    }
}
