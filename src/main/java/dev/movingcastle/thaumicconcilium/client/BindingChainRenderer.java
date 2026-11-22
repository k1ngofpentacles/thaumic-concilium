package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Draws the Crimson Dagger's binding (D1 option C): the Ethereal Shackles' own chain links ({@link EtherealShacklesRenderer#CHAIN_OBJ})
 * from the matrix to the bound player's chest with the shackles mesh as the cuff, a second chain flicking to the pedestal that just
 * fed it, and the demanded item hovering above the matrix. Does nothing while no binding is in view.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID, value = Dist.CLIENT)
public final class BindingChainRenderer {
    /** Link spacing along the chain, in blocks (the mesh is 0.39 long). */
    private static final double LINK = 0.34;
    private static final float CUFF_SCALE = 0.5F;
    private static final float ICON_SCALE = 0.7F;

    private BindingChainRenderer() {
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || BindingClient.STATES.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        long now = level.getGameTime();
        for (Map.Entry<BlockPos, BindingClient.State> e : BindingClient.STATES.entrySet()) {
            BlockPos matrix = e.getKey();
            BindingClient.State s = e.getValue();
            Vec3 from = Vec3.atCenterOf(matrix);
            Player player = level.getPlayerByUUID(s.player);
            pose.pushPose();
            pose.translate(-cam.x, -cam.y, -cam.z);
            if (player != null) {
                Vec3 chest = player.getPosition(partial).add(0.0, player.getBbHeight() * 0.7, 0.0);
                boolean glow = s.yankUntil > now;
                int light = glow ? 0xF000F0 : LevelRenderer.getLightColor(level, BlockPos.containing(chest));
                // a Yank's telegraph: the chain glows and pulses amber, building to the pull
                int pulse = glow ? 0xC0 + (int) (0x3F * Math.abs(Math.sin((now + partial) * 0.6))) : 0;
                int colour = glow ? 0xFF000000 | (0xFF << 16) | (pulse << 8) | 0x40 : s.missUntil > now ? 0xFFFF6060 : 0xFFFFFFFF;
                chain(pose, buffers, from, chest, light, colour);
                cuff(pose, buffers, chest, from, light, colour);
            }
            if (s.flickUntil > now) {
                Vec3 target = Vec3.atCenterOf(s.flickTarget).add(0.0, 0.3, 0.0);
                chain(pose, buffers, from, target, LevelRenderer.getLightColor(level, s.flickTarget.above()), 0xFFFFFFFF);
            }
            if (!s.demand.isEmpty()) {
                pose.pushPose();
                double bob = Math.sin((now + partial) * 0.1) * 0.06;
                pose.translate(from.x, from.y + 1.1 + bob, from.z);
                pose.mulPose(camera.rotation());
                pose.scale(ICON_SCALE, ICON_SCALE, ICON_SCALE);
                mc.getItemRenderer().renderStatic(s.demand, ItemDisplayContext.GROUND, 0xF000F0, OverlayTexture.NO_OVERLAY, pose, buffers, level, 0);
                pose.popPose();
            }
            pose.popPose();
        }
        buffers.endBatch();
    }

    /** Links tiled from {@code from} to {@code to}, each turned a quarter about the chain so it reads from every side. */
    private static void chain(PoseStack pose, MultiBufferSource buffers, Vec3 from, Vec3 to, int light, int colour) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 0.05) {
            return;
        }
        double h = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float) Math.atan2(-d.z, d.x);
        float pitch = (float) Math.atan2(d.y, h);
        var mesh = ObjMesh.getFlat(EtherealShacklesRenderer.CHAIN_OBJ);
        var out = buffers.getBuffer(RenderType.entityCutout(EtherealShacklesRenderer.CHAIN_TEX));
        int n = Math.max(1, (int) (len / LINK));
        for (int i = 0; i < n; i++) {
            Vec3 at = from.add(d.scale((i + 0.5) / n));
            pose.pushPose();
            pose.translate(at.x, at.y, at.z);
            pose.mulPose(Axis.YP.rotation(yaw));
            pose.mulPose(Axis.ZP.rotation(pitch));
            if ((i & 1) == 1) {
                pose.mulPose(Axis.XP.rotationDegrees(90.0F));
            }
            pose.translate(0.0, 0.40625, 0.0);
            mesh.render(pose, out, colour, light);
            pose.popPose();
        }
    }

    /** The shackles mesh around the bound player's chest, facing along the chain. */
    private static void cuff(PoseStack pose, MultiBufferSource buffers, Vec3 chest, Vec3 matrix, int light, int colour) {
        Vec3 d = matrix.subtract(chest);
        pose.pushPose();
        pose.translate(chest.x, chest.y, chest.z);
        pose.mulPose(Axis.YP.rotation((float) Math.atan2(-d.z, d.x) + (float) Math.PI / 2.0F));
        pose.scale(CUFF_SCALE, CUFF_SCALE, CUFF_SCALE);
        pose.translate(0.0, 0.4, 0.0);
        ObjMesh.getFlat(EtherealShacklesRenderer.SHACKLES_OBJ)
                .render(pose, buffers.getBuffer(RenderType.entityCutout(EtherealShacklesRenderer.SHACKLES_TEX)), colour, light);
        pose.popPose();
    }
}
