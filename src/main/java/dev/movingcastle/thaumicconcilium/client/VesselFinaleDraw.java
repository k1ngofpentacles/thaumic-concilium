package dev.movingcastle.thaumicconcilium.client;

import com.leclowndu93150.thaumaturge.client.effect.LateWorldRenderQueue;
import com.leclowndu93150.thaumaturge.client.render.TTRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.Vessel;
import dev.movingcastle.thaumicconcilium.network.VesselFlashPayload;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The finale as drawn in the world (phase 5d cycle 4; numbers and curves in {@link VesselFinale}). Two halves:
 * <ul>
 * <li>{@link #inside}: what an eye in the black is drawn in place of the black's disc: a black shell around it (the dome seen from
 * within, hiding sky and far world), the shadow as a veil before the eye, then the wisp and its contained burst, which shine through
 * both. Called by the Vessel's renderer from the late queue.</li>
 * <li>{@link #onRender}: the escaped white light, drawn from the server's flash message, because the Vessel is gone 20 ticks after the
 * collapse and the light outlasts it.</li>
 * </ul>
 * Quads on Thaumaturge's particle programs only, as the rest of the Vessel.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID, value = Dist.CLIENT)
public final class VesselFinaleDraw {
    private static final ResourceLocation BLACK = ThaumicConcilium.id("textures/misc/vessel_black.png");
    private static final ResourceLocation LIGHT = ThaumicConcilium.id("textures/misc/vessel_light.png");
    private static final int SHELL_LON = 20;
    private static final int SHELL_LAT = 10;
    /** The veil stands this far before the eye (the near plane is 0.05) and is this wide: it covers any field of view. */
    private static final float VEIL_AT = 0.3F;
    private static final float VEIL_HALF = 3.0F;

    private VesselFinaleDraw() {
    }

    /**
     * For an eye in the black, at finale age {@code t}; the pose's origin is the hole. {@code shellRadius} is the black's radius as it
     * stands (the swell's once the collapse began).
     */
    static void inside(Vessel e, float t, float shellRadius, PoseStack pose, MultiBufferSource b, Vec3 origin) {
        int c = VesselFinale.collapse(e.dyingC());
        int light = e.dyingLight();
        float left = 1.0F - VesselFinale.escape(t, c);
        RenderType black = TTRenderTypes.fxTranslucent(BLACK);
        shell(b.getBuffer(black), pose.last().pose(), shellRadius, Mth.clamp((int) (left * 255.0F), 0, 255));
        if (light >= 0 && t >= light) {
            // the shadow: from the tick the server lifts the Blindness (so it starts whole) easing to the blanket, gone with the black
            Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            Vec3 cam = camera.getPosition();
            Vector3f f = camera.getLookVector();
            pose.pushPose();
            pose.translate(cam.x - origin.x + f.x() * VEIL_AT, cam.y - origin.y + f.y() * VEIL_AT, cam.z - origin.z + f.z() * VEIL_AT);
            VesselDraw.billboard(b, pose, black, VEIL_HALF, 0.0F, 0x000000, 1.0F, VesselFinale.shadow(t, light) * left);
            pose.popPose();
        }
        VesselDraw.flush(b, black);
        if (light < 0) {
            return;
        }
        float[] w = new float[3];
        if (VesselFinale.wisp(t, light, e.dyingBurst(), w)) {
            BlockPos pond = e.dyingPond();
            float down = 1.0F - w[0];
            pose.pushPose();
            pose.translate((pond.getX() + 0.5 - origin.x) * down, (pond.getY() + 0.5 - origin.y) * down, (pond.getZ() + 0.5 - origin.z) * down);
            pose.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
            BrightestOneRenderer.draw(b, pose, CapitalStar.SPHERE_CYAN, t, (int) t, VesselFinale.WISP_SCALE * w[1], w[2]);
            pose.popPose();
        }
        if (t >= e.dyingBurst() && (c < 0 || t < c)) {
            light(b, pose, VesselFinale.contained(t, e.dyingBurst(), e.dyingRs()), VesselFinale.LIGHT_INSIDE);
        }
    }

    /** The white light: a soft camera-facing disc of {@code radius} about the pose's origin. */
    private static void light(MultiBufferSource b, PoseStack pose, float radius, float alpha) {
        if (radius <= 0.001F || alpha <= 0.001F) {
            return;
        }
        RenderType type = TTRenderTypes.fxTranslucentBlurred(LIGHT); // blurred: the disc is drawn 20 blocks wide from 64 px
        VesselDraw.billboard(b, pose, type, radius, 0.0F, VesselFinale.LIGHT_COLOR, 1.0F, alpha);
        VesselDraw.flush(b, type);
    }

    /** A black sphere about the origin; the program does not cull, so it is seen from within. */
    private static void shell(VertexConsumer vc, Matrix4f m, float r, int alpha) {
        if (alpha <= 0) {
            return;
        }
        for (int i = 0; i < SHELL_LAT; i++) {
            float p0 = Mth.PI * i / SHELL_LAT - Mth.HALF_PI;
            float p1 = Mth.PI * (i + 1) / SHELL_LAT - Mth.HALF_PI;
            for (int j = 0; j < SHELL_LON; j++) {
                float a0 = Mth.TWO_PI * j / SHELL_LON;
                float a1 = Mth.TWO_PI * (j + 1) / SHELL_LON;
                point(vc, m, r, p0, a0, alpha);
                point(vc, m, r, p1, a0, alpha);
                point(vc, m, r, p1, a1, alpha);
                point(vc, m, r, p0, a1, alpha);
            }
        }
    }

    private static void point(VertexConsumer vc, Matrix4f m, float r, float lat, float lon, int alpha) {
        float h = Mth.cos(lat) * r;
        vc.addVertex(m, Mth.cos(lon) * h, Mth.sin(lat) * r, Mth.sin(lon) * h).setUv(0.5F, 0.5F).setColor(0, 0, 0, alpha).setLight(VesselDraw.LIGHT);
    }

    /** The escaped light, from the flash message: queued each frame for the late pass, after the Vessel's own layers. */
    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        VesselFlashPayload flash = VesselFlashClient.last();
        float u = VesselFlashClient.age(event.getPartialTick().getGameTimeDeltaPartialTick(false));
        if (flash == null || u < 0.0F) {
            return;
        }
        float alpha = VesselFinale.lightAlpha(u) * (flash.inside() ? VesselFinale.LIGHT_INSIDE : 1.0F);
        float radius = VesselFinale.lightRadius(u, flash.radius(), flash.inside());
        Vec3 at = new Vec3(flash.x(), flash.y(), flash.z());
        if (flash.inside()) { // after the level, as the shell it shines through (see the Vessel's renderer)
            LateWorldRenderQueue.enqueueBlockEntityAfterTranslucent(at, (pose, buffers) -> light(buffers, pose, radius, alpha));
        } else {
            LateWorldRenderQueue.enqueue(at, (pose, buffers) -> light(buffers, pose, radius, alpha));
        }
    }
}
