package dev.twme.worldeditdisplay.region;

import dev.twme.worldeditdisplay.player.PlayerData;
import java.util.ArrayList;
import java.util.List;

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
    }

    /**
     * Add a polygon face for the polyhedron
     */
    @Override
    public void addPolygon(int[] vertexIds) {
        faces.add(vertexIds.clone());
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
