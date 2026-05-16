package dev.twme.worldeditdisplay.region;

import java.util.ArrayList;
import java.util.List;

import dev.twme.worldeditdisplay.player.PlayerData;

/**
 * Polyhedron region (3D polygon with faces)
 */
public class PolyhedronRegion extends Region {

    private final List<Vector3> vertices = new ArrayList<>();
    private final List<int[]> faces = new ArrayList<>();

    public PolyhedronRegion(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public RegionType getType() {
        return RegionType.POLYHEDRON;
    }

    /**
     * Set a vertex of the polyhedron
     */
    @Override
    public void setCuboidPoint(int id, double x, double y, double z) {
        while (vertices.size() <= id) vertices.add(null);
        vertices.set(id, Vector3.at(x, y, z));
        markDirty();
    }

    /**
     * Add a polygon face for the polyhedron
     */
    @Override
    public void addPolygon(int[] vertexIds) {
        faces.add(vertexIds.clone());
        markDirty();
    }

    public List<Vector3> getVertices() {
        List<Vector3> copy = new ArrayList<>();
        for (Vector3 v : vertices) copy.add(v);
        return copy;
    }

    public List<int[]> getFaces() {
        List<int[]> copy = new ArrayList<>();
        for (int[] face : faces) copy.add(face.clone());
        return copy;
    }

    @Override
    public BoundingBox getBoundingBox() {
        if (!isDefined()) return null;
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (Vector3 v : vertices) {
            if (v == null) continue;
            minX = Math.min(minX, v.getX()); minY = Math.min(minY, v.getY()); minZ = Math.min(minZ, v.getZ());
            maxX = Math.max(maxX, v.getX()); maxY = Math.max(maxY, v.getY()); maxZ = Math.max(maxZ, v.getZ());
        }
        if (minX == Double.MAX_VALUE) return null;
        return new BoundingBox(Vector3.at(minX, minY, minZ), Vector3.at(maxX, maxY, maxZ));
    }

    @Override
    public boolean isDefined() {
        return !vertices.isEmpty() && !faces.isEmpty();
    }

    @Override
    public String getInfo() {
        StringBuilder sb = new StringBuilder("Polyhedron Region:\n");
        sb.append("  Vertices: ").append(vertices.size()).append('\n');
        sb.append("  Faces: ").append(faces.size()).append('\n');

        int validVertices = 0;
        for (Vector3 v : vertices) {
            if (v != null) validVertices++;
        }
        sb.append("  Valid vertices: ").append(validVertices);
        return sb.toString();
    }
}
