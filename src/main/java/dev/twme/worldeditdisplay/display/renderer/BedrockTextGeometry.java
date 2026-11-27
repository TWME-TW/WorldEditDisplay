package dev.twme.worldeditdisplay.display.renderer;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Fits Bedrock primitive text boxes to the selection coordinates used by the Java renderer. */
final class BedrockTextGeometry {
    private static final float PIXEL_SIZE = 0.025f;
    private static final int LINE_HEIGHT_PIXELS = 10;
    // Effective non-breaking space width after Geyser's font scale correction.
    private static final float SPACE_WIDTH_PIXELS = 9.0f;
    // Space line panels for the narrower width seen in thin rows. Fit their
    // outer ends to the full background width so the joins don't extend the line.
    private static final float LINE_SPACE_STEP_PIXELS = 8.64f;
    private static final int MAX_LINE_SPACES = 60;
    private static final int BACKGROUND_PADDING_PIXELS = 1;
    private static final int MAX_ROW_PIXELS = 625;
    private static final int MAX_ROWS = 8;
    private static final int MAX_COLUMNS = 32;
    private static final int MAX_SHEAR_STRIPS = 12;
    private static final float MIN_SIZE = 0.001f;

    private BedrockTextGeometry() {}

    static List<Panel> line(Vector3f start, Vector3f end, float thickness) {
        Vector3f right = new Vector3f(end).sub(start);
        float length = right.length();
        if (!Float.isFinite(length) || length < MIN_SIZE || !Float.isFinite(thickness) || thickness < MIN_SIZE) {
            return List.of();
        }
        right.div(length);
        Vector3f referenceUp = Math.abs(right.y) < 0.99f ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
        Vector3f up = referenceUp.fma(-right.dot(referenceUp), right).normalize();
        List<Panel> panels = new ArrayList<>();
        appendDoubleSided(panels, new Vector3f(start).fma(-thickness / 2, up),
                right, up, length, thickness, true);
        return panels;
    }

    static List<Panel> face(Vector3f p1, Vector3f p2, Vector3f p3, boolean triangle) {
        Vector3f right = new Vector3f(p2).sub(p1);
        float width = right.length();
        if (!Float.isFinite(width) || width < MIN_SIZE) return List.of();
        right.div(width);
        Vector3f up = new Vector3f(p3).sub(p1);
        float shear = up.dot(right);
        up.fma(-shear, right);
        float height = up.length();
        if (!Float.isFinite(height) || height < MIN_SIZE) return List.of();
        up.div(height);

        // Rectangular faces need one plane. For sloped edges, the width and
        // position of each strip are sampled halfway along that strip.
        float edgeShift = triangle ? Math.max(Math.abs(shear), Math.abs(width - shear)) : Math.abs(shear);
        int strips = Math.max(1, Math.min(MAX_SHEAR_STRIPS,
                (int) Math.ceil(edgeShift / Math.max(width / 24, 0.025f))));
        List<Panel> panels = new ArrayList<>();
        for (int i = 0; i < strips; i++) {
            float offset = (i + 0.5f) / strips;
            Vector3f bottomLeft = new Vector3f(p1).fma(offset * shear, right).fma(i * height / strips, up);
            float stripWidth = triangle ? width * (1 - offset) : width;
            appendDoubleSided(panels, bottomLeft, right, up, stripWidth, height / strips, false);
        }
        return panels;
    }

    private static void appendDoubleSided(List<Panel> panels, Vector3f bottomLeft,
                                          Vector3f right, Vector3f up, float width, float height, boolean line) {
        appendRectangle(panels, bottomLeft, right, up, width, height, line);
        appendRectangle(panels, new Vector3f(bottomLeft).fma(width, right),
                new Vector3f(right).negate(), up, width, height, line);
    }

    private static void appendRectangle(List<Panel> panels, Vector3f bottomLeft,
                                        Vector3f right, Vector3f up, float width, float height, boolean line) {
        // Put the longer edge along the text row to keep the row count and payload small.
        if (height > width) {
            bottomLeft = new Vector3f(bottomLeft).fma(width, right);
            Vector3f oldRight = right;
            right = up;
            up = new Vector3f(oldRight).negate();
            float oldWidth = width;
            width = height;
            height = oldWidth;
        }

        RowLayout layout = fitRows(width, height, 1, line);
        if (!line) {
            for (int rows = 2; rows <= MAX_ROWS; rows++) {
                RowLayout candidate = fitRows(width, height, rows, false);
                if (candidate.coverage() > layout.coverage() + 0.0001f) layout = candidate;
            }
        }
        // Non-breaking spaces keep the text box sized without drawing any glyphs.
        String text = "\u00a0".repeat(layout.spaces());
        float fittedRowHeight = LINE_HEIGHT_PIXELS * PIXEL_SIZE * layout.scale();
        float fittedColumnWidth = textWidthPixels(layout.spaces()) * PIXEL_SIZE * layout.scale();
        float columnStep = columnStepPixels(layout.spaces(), line) * PIXEL_SIZE * layout.scale();
        float heightOffset = (height - layout.rows() * fittedRowHeight) / 2;
        float widthOffset = (width - (layout.columns() - 1) * columnStep - fittedColumnWidth) / 2;

        Quaternionf rotation = new Matrix3f()
                .setColumn(0, right).setColumn(1, up).setColumn(2, right.cross(up, new Vector3f()))
                .getNormalizedRotation(new Quaternionf());
        // Geyser adjusts the anchor of multiline text independently of its
        // scale. Position each single row in world space to avoid that shift.
        for (int row = 0; row < layout.rows(); row++) {
            for (int column = 0; column < layout.columns(); column++) {
                Vector3f position = new Vector3f(bottomLeft)
                        .fma(widthOffset + fittedColumnWidth / 2 + column * columnStep, right)
                        .fma(heightOffset + row * fittedRowHeight, up);
                panels.add(new Panel(position, rotation, layout.scale(), text));
            }
        }
    }

    private static RowLayout fitRows(float width, float height, int rows, boolean line) {
        float heightScale = height / (rows * LINE_HEIGHT_PIXELS * PIXEL_SIZE);
        float pixels = width / (PIXEL_SIZE * heightScale);
        float maxRowPixels = line ? MAX_LINE_SPACES * LINE_SPACE_STEP_PIXELS : MAX_ROW_PIXELS;
        int columns = Math.max(1, Math.min(MAX_COLUMNS, (int) Math.ceil(pixels / maxRowPixels)));
        float pixelsPerSpaceAcrossColumns = SPACE_WIDTH_PIXELS
                + (columns - 1) * (line ? LINE_SPACE_STEP_PIXELS : SPACE_WIDTH_PIXELS);
        float padding = (line ? 1 : columns) * BACKGROUND_PADDING_PIXELS;
        float maxWidth = line ? MAX_LINE_SPACES * pixelsPerSpaceAcrossColumns + padding : columns * MAX_ROW_PIXELS;
        int spaces = Math.max(1, Math.round((Math.min(pixels, maxWidth) - padding) / pixelsPerSpaceAcrossColumns));
        float fittedWidth = textWidthPixels(spaces) + (columns - 1) * columnStepPixels(spaces, line);
        float widthScale = width / (PIXEL_SIZE * fittedWidth);
        // Faces match whichever pair of edges fits with uniform scaling. Trying
        // several row counts reduces the rounding gap on the remaining edges.
        float insideScale = Math.min(widthScale, heightScale);
        float scale = line && widthScale <= heightScale * 1.25f ? widthScale : insideScale;
        return new RowLayout(rows, columns, spaces, scale, insideScale / Math.max(widthScale, heightScale));
    }

    private static float textWidthPixels(int spaces) {
        return spaces * SPACE_WIDTH_PIXELS + BACKGROUND_PADDING_PIXELS;
    }

    private static float columnStepPixels(int spaces, boolean line) {
        return line ? spaces * LINE_SPACE_STEP_PIXELS : textWidthPixels(spaces);
    }

    private record RowLayout(int rows, int columns, int spaces, float scale, float coverage) {}

    record Panel(Vector3f position, Quaternionf rotation, float scale, String text) {}
}
