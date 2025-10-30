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

    @Override
    public void setCuboidPoint(int id, double x, double y, double z) {
        Vector3 vertex = Vector3.at(x, y, z);
        
        // Expand list if necessary
        while (vertices.size() <= id) {
            vertices.add(null);
        }
        
        vertices.set(id, vertex);
    }

    @Override
    public void addPolygon(int[] vertexIds) {
        faces.add(vertexIds.clone());
    }

    public List<Vector3> getVertices() {
        return new ArrayList<>(vertices);
    }

    public List<int[]> getFaces() {
        List<int[]> result = new ArrayList<>();
        for (int[] face : faces) {
            result.add(face.clone());
        }
        return result;
    }

    public boolean isDefined() {
        return !vertices.isEmpty() && !faces.isEmpty();
    }

    @Override
    public String getInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Polyhedron Region:\n");
        sb.append("  Vertices: ").append(vertices.size()).append("\n");
        sb.append("  Faces: ").append(faces.size()).append("\n");
        
        int validVertices = 0;
        for (int i = 0; i < vertices.size(); i++) {
            Vector3 vertex = vertices.get(i);
            if (vertex != null) {
                validVertices++;
            }
        }
        
        sb.append("  Valid vertices: ").append(validVertices);
        
        return sb.toString();
    }
}
