package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The Verdant Capital's body (the user's design, docs/design/5d-look.md, addendum A1 as corrected): a star of two 3D triforces. A
 * triforce here is a level-1 Sierpinski tetrahedron: four small solid tetrahedra, each a half-size copy of the whole drawn in to
 * one of its corners, touching each other at the middles of the whole's edges round an octahedral hollow. One triforce points up
 * and one down, both on the vertical axis, eight small tetrahedra in all, of cyan-gilded gold with lichen along their edges.
 *
 * <p>The mesh is built here (a block model cannot hold a tetrahedron's angles) and drawn through the entity cutout type on our
 * own texture, so it is lit like an entity and casts shadows under shader packs.
 *
 * <p>Pose (round 5). Each of the eight pieces is placed on the capital's sphere on its own: its centre at a longitude and
 * latitude on the surface, its outer corner pointing straight out, and a turn about that radial axis. {@link CapitalMotion}
 * says where: in their triforces while the star circles, on the crossing loop at full compaction. The same placing is what
 * tools/capital_clearance.py proves contact-free ({@code sphere_piece} there).
 */
public final class CapitalStar {
    public static final ResourceLocation SKIN = ThaumicConcilium.id("textures/entity/verdant_capital.png");

    /** A whole triforce's edge, its height, the radius of the circle its base corners sweep, and its corners' distance from the centre (blocks). */
    public static final float EDGE = 7.75F / 16.0F;
    public static final float HEIGHT = EDGE * 0.8164966F;
    public static final float RHO = EDGE * 0.5773503F;
    public static final float RADIUS = EDGE * 0.6123724F;
    /** The star's centre above the block's floor. */
    public static final float CENTRE_Y = 8.0F / 16.0F;

    /** A face's texture corners inside its 16x16 cell of the 32x32 skin (half a texel in from the cell's triangle), as fractions of the sheet. */
    private static final float[][] FACE_UV = {{0.5F / 32.0F, 15.5F / 32.0F}, {15.5F / 32.0F, 15.5F / 32.0F}, {8.0F / 32.0F, 0.5F / 32.0F}};

    private CapitalStar() {
    }

    // ---------------------------------------------------------------- the sphere
    private static final ResourceLocation SPHERE = ThaumicConcilium.id("textures/misc/capital_sphere.png");
    /**
     * The body, where the surface faces the viewer (both its near and far side are drawn, so what is behind is tinted twice): the cyan
     * of the mana tether (the fed ray's edge), heavily translucent. At its darkest (a capital unwinding, addendum A4) it is the
     * Vessel's own pair of colours: a practically opaque black orb with the Vessel's subdued horizon (VesselRenderer.DEEP) as a
     * narrower rim.
     */
    static final int SPHERE_CYAN = 0x4FE0B0;
    static final int SPHERE_DARK = 0x000000;
    static final float SPHERE_ALPHA = 0.14F;
    static final float SPHERE_DARK_ALPHA = 0.93F;
    static final int RIM_CYAN = 0xB8FFE6;
    static final int RIM_VIOLET = 0x6A4BB0;
    static final float RIM_ALPHA = 0.55F;
    static final float RIM_DARK_ALPHA = 1.0F;
    /** How narrow the rim is: the power of (1 - facing). */
    static final float RIM_POWER = 4.0F;
    static final float RIM_DARK_POWER = 6.0F;

    private static int mix(int a, int b, float t) {
        int r = (int) Mth.lerp(t, (a >> 16) & 255, (b >> 16) & 255);
        int g = (int) Mth.lerp(t, (a >> 8) & 255, (b >> 8) & 255);
        int bl = (int) Mth.lerp(t, a & 255, b & 255);
        return (r << 16) | (g << 8) | bl;
    }

    /**
     * The radius of the capital's sphere for a spread: the distance of every small tetrahedron's centre from the star's centre. The
     * sphere's surface passes through the middle of each piece (each piece is half inside the sphere), and spreading the star is
     * the same thing as growing the sphere.
     */
    public static float sphereRadius(float spread) {
        return RADIUS / 2.0F + spread;
    }

    /** Fine enough that the facets at the silhouette are nearly edge-on, or the narrow dark rim (power 6) all but vanishes. */
    private static final int SPHERE_LON = 48;
    private static final int SPHERE_LAT = 32;

    /**
     * The one sphere, at the star's centre, drawn after the pieces: a real mesh, so its surface cuts each small tetrahedron
     * through the middle. The half of a piece inside the sphere is seen through the surface's colour; the half outside stands
     * clear in front of it (the pieces write depth, the sphere only tests it). The vanilla translucent emissive entity type, no
     * shader of ours: the glassy look is in the vertex colours, faint where the surface faces the viewer and strong where it
     * turns away, which draws the rim (the "horizon") from every side. {@code dark} is 0 with mana, up to 1 when the capital
     * leaks. {@code flatView} is for the item, where every point is seen along the same line (an inventory slot has no eye point).
     */
    public static void sphere(PoseStack pose, MultiBufferSource buffers, float spread, float dark, boolean flatView) {
        float r = sphereRadius(spread);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(SPHERE));
        pose.pushPose();
        pose.translate(0.5F, CENTRE_Y, 0.5F);
        PoseStack.Pose p = pose.last();
        int body = mix(SPHERE_CYAN, SPHERE_DARK, dark);
        int rim = mix(RIM_CYAN, RIM_VIOLET, dark);
        float bodyAlpha = Mth.lerp(dark, SPHERE_ALPHA, SPHERE_DARK_ALPHA);
        float rimAlpha = Mth.lerp(dark, RIM_ALPHA, RIM_DARK_ALPHA);
        float power = Mth.lerp(dark, RIM_POWER, RIM_DARK_POWER);
        Vector3f n = new Vector3f();
        Vector3f at = new Vector3f();
        for (int j = 0; j < SPHERE_LAT; j++) {
            for (int i = 0; i < SPHERE_LON; i++) {
                for (int k = 0; k < 4; k++) {
                    float lat = Mth.PI * (j + (k == 1 || k == 2 ? 1 : 0)) / SPHERE_LAT;
                    float lon = Mth.TWO_PI * (i + (k >= 2 ? 1 : 0)) / SPHERE_LON;
                    float x = Mth.sin(lat) * Mth.cos(lon);
                    float y = Mth.cos(lat);
                    float z = Mth.sin(lat) * Mth.sin(lon);
                    // how far this point of the surface is turned away from the viewer: 0 facing, 1 edge-on
                    p.normal().transform(x, y, z, n).normalize();
                    float facing;
                    if (flatView) {
                        facing = Math.abs(n.z);
                    } else {
                        p.pose().transformPosition(x * r, y * r, z * r, at).normalize();
                        facing = Math.abs(n.dot(at));
                    }
                    float t = (float) Math.pow(1.0F - Math.min(facing, 1.0F), power); // the rim is narrow
                    int rgb = mix(body, rim, t);
                    int alpha = (int) (255.0F * Mth.lerp(t, bodyAlpha, rimAlpha));
                    vc.addVertex(p, x * r, y * r, z * r).setColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, alpha).setUv(0.5F, 0.5F)
                            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(p, x, y, z);
                }
            }
        }
        pose.popPose();
    }

    // ---------------------------------------------------------------- the pieces
    /**
     * The four corners of one small tetrahedron about the star's centre: its centre on the sphere of radius {@code radius} at
     * (lon, lat), corner 0 pointing straight out, the other three turned {@code turn} about that radial axis in the local
     * east-north frame (all radians, lengths in blocks). The same construction as {@code sphere_piece} in the clearance script.
     */
    static void corners(float radius, float lon, float lat, float turn, Vector3f[] out) {
        float cl = Mth.cos(lat);
        float sl = Mth.sin(lat);
        float cp = Mth.cos(lon);
        float sp = Mth.sin(lon);
        float ux = cl * cp;
        float uz = cl * sp;
        float outer = radius + RADIUS / 2.0F;
        float inner = radius - RADIUS / 6.0F;
        out[0].set(ux * outer, sl * outer, uz * outer);
        for (int j = 0; j < 3; j++) {
            float a = turn + j * Mth.TWO_PI / 3.0F;
            float e = RHO / 2.0F * Mth.cos(a);
            float n = RHO / 2.0F * Mth.sin(a);
            // east = (-sp, 0, cp); north = (-sl cp, cl, -sl sp)
            out[j + 1].set(ux * inner - sp * e - sl * cp * n, sl * inner + cl * n, uz * inner + cp * e - sl * sp * n);
        }
    }

    private static final ThreadLocal<Vector3f[]> SCRATCH = ThreadLocal.withInitial(() -> new Vector3f[] {new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()});

    /**
     * Draws eight pieces in a block's space (origin at the block's corner): 4 triangular faces each (a triangle is a quad with
     * its last corner doubled).
     */
    public static void draw(PoseStack pose, MultiBufferSource buffers, int light, float radius, float[] lon, float[] lat, float[] turn) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(SKIN));
        pose.pushPose();
        pose.translate(0.5F, CENTRE_Y, 0.5F);
        PoseStack.Pose p = pose.last();
        Vector3f[] v = SCRATCH.get();
        Vector3f n = new Vector3f();
        Vector3f ab = new Vector3f();
        Vector3f ac = new Vector3f();
        for (int i = 0; i < 8; i++) {
            corners(radius, lon[i], lat[i], turn[i], v);
            float mx = (v[0].x + v[1].x + v[2].x + v[3].x) / 4.0F;
            float my = (v[0].y + v[1].y + v[2].y + v[3].y) / 4.0F;
            float mz = (v[0].z + v[1].z + v[2].z + v[3].z) / 4.0F;
            for (int f = 0; f < 4; f++) {
                // face f is the one that leaves out corner f
                Vector3f a = v[(f + 1) % 4];
                Vector3f b = v[(f + 2) % 4];
                Vector3f c = v[(f + 3) % 4];
                b.sub(a, ab);
                c.sub(a, ac);
                ab.cross(ac, n).normalize();
                if (n.x * (a.x - mx) + n.y * (a.y - my) + n.z * (a.z - mz) < 0.0F) {
                    n.negate();
                }
                float cu = (f % 2) * 0.5F;
                float cv = (f / 2) * 0.5F;
                for (int k = 0; k < 4; k++) {
                    int corner = Math.min(k, 2);
                    Vector3f at = v[(f + 1 + corner) % 4];
                    vc.addVertex(p, at.x, at.y, at.z).setColor(255, 255, 255, 255).setUv(cu + FACE_UV[corner][0], cv + FACE_UV[corner][1])
                            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, n.x, n.y, n.z);
                }
            }
        }
        pose.popPose();
    }

    // ---- the closed star (the item): spread 0, untwisted, at rest
    private static final float[] CLOSED_LON = new float[8];
    private static final float[] CLOSED_LAT = new float[8];
    private static final float[] CLOSED_TURN = new float[8];

    static {
        for (int i = 0; i < 8; i++) {
            boolean upper = i < 4;
            int c = i % 4;
            float sign = upper ? 1.0F : -1.0F;
            if (c == 0) {
                CLOSED_LAT[i] = sign * Mth.HALF_PI;
                CLOSED_TURN[i] = 30.0F * Mth.DEG_TO_RAD;
            } else {
                CLOSED_LON[i] = ((c - 1) * 120.0F + (upper ? 0.0F : 60.0F)) * Mth.DEG_TO_RAD;
                CLOSED_LAT[i] = -sign * CapitalMotion.LAT_WAIST * Mth.DEG_TO_RAD;
                CLOSED_TURN[i] = (upper ? 90.0F : 30.0F) * Mth.DEG_TO_RAD;
            }
        }
    }

    /** The closed star: the two triforces whole and interlocked, nothing turning (so its pieces may touch). */
    public static void drawClosed(PoseStack pose, MultiBufferSource buffers, int light) {
        draw(pose, buffers, light, sphereRadius(0.0F), CLOSED_LON, CLOSED_LAT, CLOSED_TURN);
    }

    /** A random point on an edge of one of the star's eight small tetrahedra as it stands now, in a block's space (where a crackle starts). */
    static Vec3 pointOnPiece(RandomSource random, CapitalMotion m) {
        int i = random.nextInt(8);
        int a = random.nextInt(4);
        int b = (a + 1 + random.nextInt(3)) % 4;
        Vector3f[] v = SCRATCH.get();
        corners(m.radius, m.lon[i], m.lat[i], m.turn[i], v);
        Vector3f at = new Vector3f(v[a]).lerp(v[b], random.nextFloat());
        return new Vec3(0.5 + m.offsetX + at.x, CENTRE_Y + m.offsetY + at.y, 0.5 + m.offsetZ + at.z);
    }
}
