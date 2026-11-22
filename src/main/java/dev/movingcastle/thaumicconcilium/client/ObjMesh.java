package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

/**
 * A Wavefront OBJ drawn straight into a vertex buffer with its own UVs (for meshes whose texture repeats, which an
 * atlas-baked model can't do: the Hex's rune rings tile their texture around each ring). Faces become quads
 * (triangles repeat their last vertex); V is flipped as Blender exports it. Meshes are read once and cached.
 */
public final class ObjMesh {
    private static final Map<ResourceLocation, ObjMesh> CACHE = new HashMap<>();
    private static final Map<ResourceLocation, ObjMesh> FLAT_CACHE = new HashMap<>();
    /** Each face: four vertices of x, y, z, u, v, nx, ny, nz. */
    private final List<float[][]> faces;

    private ObjMesh(List<float[][]> faces) {
        this.faces = faces;
    }

    public static ObjMesh get(ResourceLocation location) {
        return CACHE.computeIfAbsent(location, l -> load(l, false));
    }

    /**
     * Like {@link #get} but lit by each face's own computed normal (first three vertices), ignoring the file's vn: how the
     * 1.7.10 OBJ renderer drew, and it keeps a smoothed mesh (the gem pyramid) faceted. A degenerate face points up.
     */
    public static ObjMesh getFlat(ResourceLocation location) {
        return FLAT_CACHE.computeIfAbsent(location, l -> load(l, true));
    }

    /** Dropped on resource reload so edited meshes are picked up. */
    public static void clear() {
        CACHE.clear();
        FLAT_CACHE.clear();
    }

    private static ObjMesh load(ResourceLocation location, boolean flat) {
        List<float[]> v = new ArrayList<>();
        List<float[]> vt = new ArrayList<>();
        List<float[]> vn = new ArrayList<>();
        List<float[][]> faces = new ArrayList<>();
        Resource resource = Minecraft.getInstance().getResourceManager().getResource(location).orElse(null);
        if (resource == null) {
            ThaumicConcilium.LOG.warn("Missing OBJ mesh {}", location);
            return new ObjMesh(faces);
        }
        try (BufferedReader in = new BufferedReader(new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                String[] p = line.trim().split("\\s+");
                switch (p[0]) {
                    case "v" -> v.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])});
                    case "vt" -> vt.add(new float[]{Float.parseFloat(p[1]), 1.0F - Float.parseFloat(p[2])});
                    case "vn" -> vn.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])});
                    case "f" -> {
                        int n = p.length - 1;
                        float[][] face = new float[4][];
                        for (int i = 0; i < 4; i++) {
                            String[] idx = p[1 + Math.min(i, n - 1)].split("/");
                            float[] pos = v.get(Integer.parseInt(idx[0]) - 1);
                            float[] uv = idx.length > 1 && !idx[1].isEmpty() ? vt.get(Integer.parseInt(idx[1]) - 1) : new float[]{0, 0};
                            float[] nor = idx.length > 2 ? vn.get(Integer.parseInt(idx[2]) - 1) : new float[]{0, 1, 0};
                            face[i] = new float[]{pos[0], pos[1], pos[2], uv[0], uv[1], nor[0], nor[1], nor[2]};
                        }
                        if (flat) {
                            flatten(face);
                        }
                        faces.add(face);
                    }
                    default -> {
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            ThaumicConcilium.LOG.warn("Could not read OBJ mesh {}", location, e);
        }
        return new ObjMesh(faces);
    }

    private static void flatten(float[][] f) {
        float ax = f[1][0] - f[0][0], ay = f[1][1] - f[0][1], az = f[1][2] - f[0][2];
        float bx = f[2][0] - f[0][0], by = f[2][1] - f[0][1], bz = f[2][2] - f[0][2];
        float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0E-6F) {
            nx = 0.0F;
            ny = 1.0F;
            nz = 0.0F;
        } else {
            nx /= len;
            ny /= len;
            nz /= len;
        }
        for (float[] q : f) {
            q[5] = nx;
            q[6] = ny;
            q[7] = nz;
        }
    }

    public void render(PoseStack pose, VertexConsumer out, int argb, int light) {
        PoseStack.Pose last = pose.last();
        for (float[][] face : this.faces) {
            for (float[] q : face) {
                out.addVertex(last, q[0], q[1], q[2]).setColor(argb).setUv(q[3], q[4]).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light).setNormal(last, q[5], q[6], q[7]);
            }
        }
    }
}
