package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Quad helpers for the Vessel's look (phase 5c cycle 3): camera-facing billboards and the light ribbon, written to the Thaumaturge
 * particle-format render types ({@code TTRenderTypes.fx*}, full bright). No shader of ours: Iris maps these programs already.
 */
final class VesselDraw {
    static final int LIGHT = LightTexture.pack(15, 15);

    private VesselDraw() {
    }

    /** Ends the batch of one render type now, so the layers keep the order they were drawn in (blended layers do not commute). */
    static void flush(MultiBufferSource buffers, RenderType type) {
        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(type);
        }
    }

    static int red(int hex, float k) {
        return Mth.clamp((int) ((hex >> 16 & 255) * k), 0, 255);
    }

    static int green(int hex, float k) {
        return Mth.clamp((int) ((hex >> 8 & 255) * k), 0, 255);
    }

    static int blue(int hex, float k) {
        return Mth.clamp((int) ((hex & 255) * k), 0, 255);
    }

    /** A camera-facing square of half-size {@code half} about the pose's origin, turned {@code deg} about the view axis. The pose is restored. */
    static void billboard(MultiBufferSource buffers, PoseStack pose, RenderType type, float half, float deg, int hex, float intensity, float alpha) {
        pose.pushPose();
        pose.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        if (deg != 0.0F) {
            pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(deg));
        }
        VertexConsumer vc = buffers.getBuffer(type);
        Matrix4f m = pose.last().pose();
        int r = red(hex, intensity);
        int g = green(hex, intensity);
        int b = blue(hex, intensity);
        int a = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        vc.addVertex(m, -half, -half, 0.0F).setUv(1.0F, 1.0F).setColor(r, g, b, a).setLight(LIGHT);
        vc.addVertex(m, -half, half, 0.0F).setUv(1.0F, 0.0F).setColor(r, g, b, a).setLight(LIGHT);
        vc.addVertex(m, half, half, 0.0F).setUv(0.0F, 0.0F).setColor(r, g, b, a).setLight(LIGHT);
        vc.addVertex(m, half, -half, 0.0F).setUv(0.0F, 1.0F).setColor(r, g, b, a).setLight(LIGHT);
        pose.popPose();
    }

    /**
     * A ribbon of light from {@code from} to {@code to} (relative to the pose's origin): two crossed quads, 8 segments, width {@code w0}
     * at {@code from} to {@code w1} at {@code to}, the pulse texture scrolling from {@code to} toward {@code from} by {@code scroll},
     * a sideways wobble of {@code wobble} (zero at both ends).
     */
    static void ribbon(MultiBufferSource buffers, PoseStack pose, RenderType type, Vec3 from, Vec3 to, float w0, float w1, int hex, float intensity,
            float time, float scroll, float wobble, int alpha) {
        Vec3 dir = to.subtract(from);
        double len = dir.length();
        if (len < 0.05) {
            return;
        }
        Vec3 d = dir.scale(1.0 / len);
        Vec3 ref = Math.abs(d.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 a = d.cross(ref).normalize();
        Vec3 b = d.cross(a).normalize();
        VertexConsumer vc = buffers.getBuffer(type);
        Matrix4f m = pose.last().pose();
        int r = red(hex, intensity);
        int g = green(hex, intensity);
        int bl = blue(hex, intensity);
        int segs = 8;
        for (int s = 0; s < segs; s++) {
            float t0 = s / (float) segs;
            float t1 = (s + 1) / (float) segs;
            Vec3 p0 = from.add(dir.scale(t0)).add(a.scale(wobble * Mth.sin(t0 * segs * 1.3F + time * 0.3F) * Mth.sin(t0 * Mth.PI)));
            Vec3 p1 = from.add(dir.scale(t1)).add(a.scale(wobble * Mth.sin(t1 * segs * 1.3F + time * 0.3F) * Mth.sin(t1 * Mth.PI)));
            float h0 = Mth.lerp(t0, w0, w1) * 0.5F;
            float h1 = Mth.lerp(t1, w0, w1) * 0.5F;
            float v0 = (1.0F - t0) * (float) len * 0.5F - scroll;
            float v1 = (1.0F - t1) * (float) len * 0.5F - scroll;
            for (int q = 0; q < 2; q++) {
                Vec3 e = q == 0 ? a : b;
                quad(vc, m, p0.subtract(e.scale(h0)), p0.add(e.scale(h0)), p1.add(e.scale(h1)), p1.subtract(e.scale(h1)), v0, v1, r, g, bl, alpha);
            }
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float v0, float v1, int r, int g, int bl, int alpha) {
        vc.addVertex(m, (float) a.x, (float) a.y, (float) a.z).setUv(0.0F, v0).setColor(r, g, bl, alpha).setLight(LIGHT);
        vc.addVertex(m, (float) b.x, (float) b.y, (float) b.z).setUv(1.0F, v0).setColor(r, g, bl, alpha).setLight(LIGHT);
        vc.addVertex(m, (float) c.x, (float) c.y, (float) c.z).setUv(1.0F, v1).setColor(r, g, bl, alpha).setLight(LIGHT);
        vc.addVertex(m, (float) d.x, (float) d.y, (float) d.z).setUv(0.0F, v1).setColor(r, g, bl, alpha).setLight(LIGHT);
    }

    /** A stable pseudo-random number in [0, 1) from a seed. */
    static float hash(int seed) {
        int x = seed * 0x9E3779B1;
        x ^= x >>> 15;
        x *= 0x85EBCA6B;
        x ^= x >>> 13;
        return (x & 0xFFFFFF) / 16777216.0F;
    }
}
