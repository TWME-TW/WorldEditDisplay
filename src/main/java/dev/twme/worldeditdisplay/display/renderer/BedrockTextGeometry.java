package dev.twme.worldeditdisplay.display.renderer;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Axis;
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
    private static final int MAX_HORIZONTAL_ROWS = 48;
    private static final int MAX_HORIZONTAL_PANELS = 128;
    private static final float HORIZONTAL_X_TOLERANCE = 0.03f;
    // Insets compensate for client-side background overdraw seen at cuboid edges.
    private static final float CUBOID_HORIZONTAL_X_INSET = 0.027f;
    private static final float CUBOID_SIDE_X_INSET = 0.008f;
    private static final float CUBOID_SIDE_Z_INSET = 0.045f;
    private static final float SIZE_EPSILON = 0.0001f;
    private static final int MAX_SHEAR_STRIPS = 12;
    private static final float MIN_SIZE = 0.001f;

    private BedrockTextGeometry() {}

    static List<Panel> line(Vector3f start, Vector3f end, float thickness) {
        return line(start, end, thickness, Stroke.AUTO);
    }

    static List<Panel> line(Vector3f start, Vector3f end, float thickness, Stroke stroke) {
        Vector3f right = new Vector3f(end).sub(start);
        float length = right.length();
        if (!Float.isFinite(length) || length < MIN_SIZE || !Float.isFinite(thickness) || thickness < MIN_SIZE) {
            return List.of();
        }
        right.div(length);
        Vector3f referenceUp = stroke.up(right);
        Vector3f up = referenceUp.fma(-right.dot(referenceUp), right).normalize();
        boolean inward = stroke.inward() != 0;
        Vector3f bottomLeft = new Vector3f(start);
        if (!inward) bottomLeft.fma(-thickness / 2, up);
        List<Panel> panels = new ArrayList<>();
        appendDoubleSided(panels, bottomLeft, right, up, length, thickness,
                inward ? PanelType.INWARD_LINE : PanelType.LINE);
        return panels;
    }

    static List<Panel> face(Vector3f p1, Vector3f p2, Vector3f p3, boolean triangle) {
        return face(p1, p2, p3, triangle, false);
    }

    private static List<Panel> face(Vector3f p1, Vector3f p2, Vector3f p3, boolean triangle, boolean cuboid) {
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
        boolean horizontalRectangle = !triangle && shear == 0 && right.y == 0 && up.y == 0
                && ((right.z == 0 && up.x == 0) || (right.x == 0 && up.z == 0));
        PanelType type = horizontalRectangle ? PanelType.HORIZONTAL_FACE : PanelType.FACE;
        if (horizontalRectangle && cuboid) type = PanelType.CUBOID_HORIZONTAL_FACE;
        List<Panel> panels = new ArrayList<>();
        for (int i = 0; i < strips; i++) {
            float offset = (i + 0.5f) / strips;
            Vector3f bottomLeft = new Vector3f(p1).fma(offset * shear, right).fma(i * height / strips, up);
            float stripWidth = triangle ? width * (1 - offset) : width;
            appendDoubleSided(panels, bottomLeft, right, up, stripWidth, height / strips, type);
        }
        return panels;
    }

    static List<Panel> cuboidFace(Vector3f bottomLeft, Vector3f bottomRight, Vector3f topLeft) {
        if (bottomLeft.y == topLeft.y) return face(bottomLeft, bottomRight, topLeft, false, true);

        Vector3f across = new Vector3f(bottomRight).sub(bottomLeft);
        float width = across.length();
        if (!Float.isFinite(width) || width < MIN_SIZE) return List.of();

        float inset = across.x == 0 ? CUBOID_SIDE_Z_INSET : CUBOID_SIDE_X_INSET;
        Vector3f offset = across.mul(Math.min(inset / width, 0.25f));
        return face(new Vector3f(bottomLeft).add(offset), new Vector3f(bottomRight).sub(offset),
                new Vector3f(topLeft).add(offset), false);
    }

    private static void appendDoubleSided(List<Panel> panels, Vector3f bottomLeft,
                                          Vector3f right, Vector3f up, float width, float height,
                                          PanelType type) {
        appendRectangle(panels, new Rectangle(bottomLeft, right, up, width, height), type);
        appendRectangle(panels, new Rectangle(new Vector3f(bottomLeft).fma(width, right),
                new Vector3f(right).negate(), up, width, height), type);
    }

    private static void appendRectangle(List<Panel> panels, Rectangle rectangle, PanelType type) {
        boolean line = type == PanelType.LINE || type == PanelType.INWARD_LINE;
        boolean inward = type == PanelType.INWARD_LINE;
        // Put the longer edge along the text row to keep the row count and payload small.
        // Inward strokes keep their bottom edge anchored to the selection boundary.
        if (rectangle.height() > rectangle.width() && !inward) {
            rectangle = rectangle.quarterTurn();
        }

        RowLayout layout = fitRows(rectangle.width(), rectangle.height(), 1, line);
        if (!line) {
            for (int rows = 2; rows <= MAX_ROWS; rows++) {
                RowLayout candidate = fitRows(rectangle.width(), rectangle.height(), rows, false);
                if (candidate.coverage() > layout.coverage() + SIZE_EPSILON) layout = candidate;
            }
        }
        if (type == PanelType.HORIZONTAL_FACE || type == PanelType.CUBOID_HORIZONTAL_FACE) {
            FaceLayout original = new FaceLayout(rectangle, layout);
            FaceLayout fitted = fitHorizontalFace(rectangle, original, original.zSpan());
            if (type == PanelType.CUBOID_HORIZONTAL_FACE) fitted = insetHorizontalCuboidFace(fitted);
            rectangle = fitted.rectangle();
            layout = fitted.rows();
        }
        Vector3f right = rectangle.right();
        Vector3f up = rectangle.up();
        // Non-breaking spaces keep the text box sized without drawing any glyphs.
        String text = "\u00a0".repeat(layout.spaces());
        float fittedRowHeight = LINE_HEIGHT_PIXELS * PIXEL_SIZE * layout.scale();
        float fittedColumnWidth = textWidthPixels(layout.spaces()) * PIXEL_SIZE * layout.scale();
        float columnStep = columnStepPixels(layout.spaces(), line) * PIXEL_SIZE * layout.scale();
        float heightOffset = inward ? 0 : (rectangle.height() - layout.rows() * fittedRowHeight) / 2;
        float widthOffset = (rectangle.width() - (layout.columns() - 1) * columnStep - fittedColumnWidth) / 2;

        Quaternionf rotation = new Matrix3f()
                .setColumn(0, right).setColumn(1, up).setColumn(2, right.cross(up, new Vector3f()))
                .getNormalizedRotation(new Quaternionf());
        // Geyser adjusts the anchor of multiline text independently of its
        // scale. Position each single row in world space to avoid that shift.
        for (int row = 0; row < layout.rows(); row++) {
            for (int column = 0; column < layout.columns(); column++) {
                Vector3f position = new Vector3f(rectangle.bottomLeft())
                        .fma(widthOffset + fittedColumnWidth / 2 + column * columnStep, right)
                        .fma(heightOffset + row * fittedRowHeight, up);
                panels.add(new Panel(position, rotation, layout.scale(), text));
            }
        }
    }

    private static FaceLayout insetHorizontalCuboidFace(FaceLayout fitted) {
        Rectangle rectangle = fitted.rectangle();
        float x = rectangle.xSpan();
        float inset = Math.min(CUBOID_HORIZONTAL_X_INSET, x / 4);
        float targetX = x - inset * 2;
        if (fitted.xSpan() <= targetX + SIZE_EPSILON) return fitted;

        float depth = fitted.zSpan();
        Rectangle insetRectangle = rectangle.withSpans(targetX, depth);
        FaceLayout insetFit = fitHorizontalFace(insetRectangle, null, depth);
        return insetFit == null ? fitted : insetFit;
    }

    private static FaceLayout fitHorizontalFace(Rectangle rectangle, FaceLayout best, float minimumZ) {
        float targetX = rectangle.xSpan();
        if (best != null && targetX - best.xSpan() <= HORIZONTAL_X_TOLERANCE) return best;

        // Rows come in discrete glyph widths. Try both axes and round towards a
        // fit that reaches the Z bounds, keeping the best X span inside the box.
        Rectangle[] orientations = { rectangle, rectangle.quarterTurn() };
        for (int rows = 1; rows <= MAX_HORIZONTAL_ROWS; rows++) {
            for (Rectangle orientation : orientations) {
                SpaceRounding roundSpaces = orientation.rowsRunAlongX() ? SpaceRounding.DOWN : SpaceRounding.UP;
                var layout = fitRows(orientation.width(), orientation.height(), rows, false, roundSpaces);
                if (layout.rows() * layout.columns() > MAX_HORIZONTAL_PANELS) continue;
                FaceLayout candidate = new FaceLayout(orientation, layout);
                if (candidate.zSpan() >= minimumZ - SIZE_EPSILON
                        && (best == null || candidate.xSpan() > best.xSpan() + SIZE_EPSILON)) {
                    best = candidate;
                }
            }
            if (best != null && targetX - best.xSpan() <= HORIZONTAL_X_TOLERANCE) break;
        }
        return best;
    }

    private static RowLayout fitRows(float width, float height, int rows, boolean line) {
        return fitRows(width, height, rows, line, SpaceRounding.NEAREST);
    }

    private static RowLayout fitRows(float width, float height, int rows, boolean line, SpaceRounding roundSpaces) {
        float heightScale = height / (rows * LINE_HEIGHT_PIXELS * PIXEL_SIZE);
        float pixels = width / (PIXEL_SIZE * heightScale);
        float maxRowPixels = line ? MAX_LINE_SPACES * LINE_SPACE_STEP_PIXELS : MAX_ROW_PIXELS;
        int columns = Math.max(1, Math.min(MAX_COLUMNS, (int) Math.ceil(pixels / maxRowPixels)));
        float pixelsPerSpaceAcrossColumns = SPACE_WIDTH_PIXELS
                + (columns - 1) * (line ? LINE_SPACE_STEP_PIXELS : SPACE_WIDTH_PIXELS);
        float padding = (line ? 1 : columns) * BACKGROUND_PADDING_PIXELS;
        float maxWidth = line ? MAX_LINE_SPACES * pixelsPerSpaceAcrossColumns + padding : columns * MAX_ROW_PIXELS;
        float idealSpaces = (Math.min(pixels, maxWidth) - padding) / pixelsPerSpaceAcrossColumns;
        int spaces = Math.max(1, switch (roundSpaces) {
            case DOWN -> (int) Math.floor(idealSpaces);
            case UP -> (int) Math.ceil(idealSpaces);
            case NEAREST -> Math.round(idealSpaces);
        });
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

    private enum PanelType { LINE, INWARD_LINE, FACE, HORIZONTAL_FACE, CUBOID_HORIZONTAL_FACE }

    private enum SpaceRounding { DOWN, NEAREST, UP }

    private record Rectangle(Vector3f bottomLeft, Vector3f right, Vector3f up, float width, float height) {
        Rectangle quarterTurn() {
            return new Rectangle(new Vector3f(bottomLeft).fma(width, right), up, new Vector3f(right).negate(),
                    height, width);
        }

        boolean rowsRunAlongX() {
            return right.x != 0;
        }

        float xSpan() {
            return rowsRunAlongX() ? width : height;
        }

        Rectangle withSpans(float x, float z) {
            float fittedWidth = rowsRunAlongX() ? x : z;
            float fittedHeight = rowsRunAlongX() ? z : x;
            Vector3f origin = new Vector3f(bottomLeft).fma((width - fittedWidth) / 2, right)
                    .fma((height - fittedHeight) / 2, up);
            return new Rectangle(origin, right, up, fittedWidth, fittedHeight);
        }
    }

    private record FaceLayout(Rectangle rectangle, RowLayout rows) {
        float xSpan() {
            return rectangle.rowsRunAlongX() ? width() : height();
        }

        float zSpan() {
            return rectangle.rowsRunAlongX() ? height() : width();
        }

        private float width() {
            return rows.columns() * textWidthPixels(rows.spaces()) * PIXEL_SIZE * rows.scale();
        }

        private float height() {
            return rows.rows() * LINE_HEIGHT_PIXELS * PIXEL_SIZE * rows.scale();
        }
    }

    /** The axis across a stroke and the direction it extends from a face boundary. */
    record Stroke(Axis axis, int inward) {
        static final Stroke AUTO = new Stroke(null, 0);

        private Vector3f up(Vector3f right) {
            if (axis == null) {
                return Math.abs(right.y) < 0.99f ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
            }
            int direction = inward < 0 ? -1 : 1;
            return switch (axis) {
                case X -> new Vector3f(direction, 0, 0);
                case Y -> new Vector3f(0, direction, 0);
                case Z -> new Vector3f(0, 0, direction);
            };
        }
    }

    record Panel(Vector3f position, Quaternionf rotation, float scale, String text) {}
}
