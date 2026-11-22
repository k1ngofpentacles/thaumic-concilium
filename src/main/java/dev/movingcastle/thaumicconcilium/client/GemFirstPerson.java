package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.item.RiftGem;
import dev.movingcastle.thaumicconcilium.item.TerraCastGem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * First person: the Gem of Rifts and Terra-cast Gem hover beside a stretched-out arm, the original's pose (1.7.10
 * ItemRenderer + RiftGemRenderer, collapsed to constants). Spec: docs/design/6c-gem-held-render.md; every number comes
 * from the original's GL calls, so none is tuned by eye. Left arm and off hand are exact mirrors (m = -1).
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID, value = Dist.CLIENT)
public final class GemFirstPerson {
    private static final float GEM_X = 0.64774F, GEM_Y = -0.132F, GEM_Z = -1.17626F;
    private static final float ARM_X = 1.04774F, ARM_Y = -1.612F, ARM_Z = -0.65626F;
    private static final float HAND = 0.4F;
    private static final float ARM_SCALE = 2.0F;
    /** The original's second sway; 0 switches it off. */
    private static final float SWAY2 = 1.0F;

    private GemFirstPerson() {
    }

    private static boolean isGem(ItemStack stack) {
        return RiftGem.isGem(stack) || stack.is(dev.movingcastle.thaumicconcilium.registry.TCItems.INERT_TERRA_CAST_GEM.get())
                || stack.is(dev.movingcastle.thaumicconcilium.registry.TCItems.TERRA_CAST_GEM.get());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderHand(RenderHandEvent event) {
        ItemStack stack = event.getItemStack();
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !isGem(stack)) {
            return;
        }
        event.setCanceled(true);
        InteractionHand hand = event.getHand();
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float m = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float pt = event.getPartialTick();
        PoseStack pose = event.getPoseStack();
        var buffers = event.getMultiBufferSource();
        int light = event.getPackedLight();
        boolean usingThisHand = player.isUsingItem() && player.getUsedItemHand() == hand;

        pose.pushPose();
        float s = event.getSwingProgress();
        float r = Mth.sqrt(s);
        // 1. the 1.7.10 swing; the translate is skipped while this hand's item is in use
        if (!usingThisHand) {
            pose.translate(m * -0.4F * Mth.sin(r * Mth.PI), 0.2F * Mth.sin(r * Mth.TWO_PI), -0.2F * Mth.sin(s * Mth.PI));
        }
        pose.translate(m * 0.56F, -0.52F, -0.72F);
        pose.mulPose(Axis.YP.rotationDegrees(m * 45.0F));
        pose.mulPose(Axis.YP.rotationDegrees(m * -20.0F * Mth.sin(s * s * Mth.PI)));
        pose.mulPose(Axis.ZP.rotationDegrees(m * -20.0F * Mth.sin(r * Mth.PI)));
        pose.mulPose(Axis.XP.rotationDegrees(-80.0F * Mth.sin(r * Mth.PI)));
        pose.mulPose(Axis.YP.rotationDegrees(m * -45.0F));
        pose.translate(m * -0.56F, 0.52F, 0.72F);
        // 2. the renderer's own second sway about Q
        float a = (player.getViewXRot(pt) - Mth.lerp(pt, player.xBobO, player.xBob)) * 0.1F * SWAY2;
        float b = (player.getViewYRot(pt) - Mth.lerp(pt, player.yBobO, player.yBob)) * 0.1F * SWAY2;
        pose.translate(m * 0.13574F, -0.42F, -1.14426F);
        pose.mulPose(Axis.ZP.rotationDegrees(m * a));
        pose.mulPose(Axis.YP.rotationDegrees(b));
        pose.translate(m * -0.13574F, 0.42F, 1.14426F);
        // 3. the arm
        if (!player.isInvisible() && mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer) {
            pose.pushPose();
            pose.translate(m * ARM_X, ARM_Y, ARM_Z);
            pose.mulPose(Axis.YP.rotationDegrees(m * 90.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(m * -90.0F));
            pose.scale(ARM_SCALE, ARM_SCALE, ARM_SCALE);
            if (m > 0.0F) {
                renderer.renderRightHand(pose, buffers, light, player);
            } else {
                renderer.renderLeftHand(pose, buffers, light, player);
            }
            pose.popPose();
        }
        // 4. the gem
        pose.translate(m * GEM_X, GEM_Y, GEM_Z);
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        pose.scale(HAND, HAND, HAND);
        GemRenderer.drawHeld(stack, pose, buffers, light, GemRenderer.spin(pt, true));
        pose.popPose();
    }
}
