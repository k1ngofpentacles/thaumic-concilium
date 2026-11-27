package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.item.DrainageSyringe;
import dev.movingcastle.thaumicconcilium.registry.TCDataComponents;
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
 * First person: the Drainage Syringe held needle-up in a raised fist, the original's pose. The 1.7.10 renderer undid
 * vanilla's hand placement and drew the arm and the syringe itself; this replays that chain call for call (1.7.10
 * ItemRenderer.renderItemInFirstPerson :434-487 with the bow use action, Forge's EQUIPPED_BLOCK helper, then
 * DrainageSyringeRenderer :68-127), starting from the event's pose, which holds only view bob and arm sway as 1.7.10's did
 * at that point (docs/design/6c-gem-held-render.md section 2). No number is tuned by eye. The left arm and the off hand
 * are mirror images (m = -1). Nothing runs unless a syringe is held.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID, value = Dist.CLIENT)
public final class DrainageSyringeFirstPerson {
    private DrainageSyringeFirstPerson() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderHand(RenderHandEvent event) {
        ItemStack stack = event.getItemStack();
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !(stack.getItem() instanceof DrainageSyringe)) {
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
        boolean using = player.isUsingItem() && player.getUsedItemHand() == hand;
        int used = using ? player.getTicksUsingItem() : 0;
        // 1.7.10's equippedProgress ran 1 (up) to 0; the event's runs 0 (up) to 1.
        float lowered = event.getEquipProgress();
        boolean has = stack.has(TCDataComponents.SYRINGE.get());
        DrainageSyringe.State state = DrainageSyringe.state(stack);
        boolean open = state.open();
        // The original's sway: the sine of the whole ticks in use, in degrees (0 when the item is not in use).
        float wave = (float) Math.sin(used);

        pose.pushPose();
        // 1. 1.7.10 ItemRenderer: swing (its translate is skipped while an item is in use), hand placement, scale.
        float s = event.getSwingProgress();
        float r = Mth.sqrt(s);
        if (!using) {
            pose.translate(m * -0.4F * Mth.sin(r * Mth.PI), 0.2F * Mth.sin(r * Mth.TWO_PI), -0.2F * Mth.sin(s * Mth.PI));
        }
        pose.translate(m * 0.56F, -0.52F - lowered * 0.6F, -0.72F);
        pose.mulPose(Axis.YP.rotationDegrees(m * 45.0F));
        pose.mulPose(Axis.YP.rotationDegrees(m * -20.0F * Mth.sin(s * s * Mth.PI)));
        pose.mulPose(Axis.ZP.rotationDegrees(m * -20.0F * Mth.sin(r * Mth.PI)));
        pose.mulPose(Axis.XP.rotationDegrees(-80.0F * Mth.sin(r * Mth.PI)));
        pose.scale(0.4F, 0.4F, 0.4F);
        if (using) {
            // The bow use action (the original item's). Its pull is computed in floats from a use duration of
            // Integer.MAX_VALUE, as the original's was: it stays 0 for the first seconds and then jumps to full.
            pose.mulPose(Axis.ZP.rotationDegrees(m * -18.0F));
            pose.mulPose(Axis.YP.rotationDegrees(m * -12.0F));
            pose.mulPose(Axis.XP.rotationDegrees(-8.0F));
            pose.translate(m * -0.9F, 0.2F, 0.0F);
            float f10 = (float) Integer.MAX_VALUE - ((float) (Integer.MAX_VALUE - used) - pt + 1.0F);
            float f11 = f10 / 20.0F;
            f11 = Math.min(1.0F, (f11 * f11 + f11 * 2.0F) / 3.0F);
            if (f11 > 0.1F) {
                pose.translate(0.0F, Mth.sin((f10 - 0.1F) * 1.3F) * 0.01F * (f11 - 0.1F), 0.0F);
            }
            pose.translate(0.0F, 0.0F, f11 * 0.1F);
            pose.mulPose(Axis.ZP.rotationDegrees(m * -335.0F));
            pose.mulPose(Axis.YP.rotationDegrees(m * -50.0F));
            pose.translate(0.0F, 0.5F, 0.0F);
            pose.scale(1.0F, 1.0F, 1.0F + f11 * 0.2F);
            pose.translate(0.0F, -0.5F, 0.0F);
            pose.mulPose(Axis.YP.rotationDegrees(m * 50.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(m * 335.0F));
        }
        // 2. Forge's EQUIPPED_BLOCK helper.
        pose.translate(m * -0.5F, -0.5F, -0.5F);
        // 3. The renderer's own frame (:68-76). Its equip term cancels vanilla's lowering: the syringe appears in place.
        pose.translate(m * 0.5F, 0.75F, -1.0F);
        pose.mulPose(Axis.YP.rotationDegrees(m * -135.0F));
        pose.translate(m * -0.56F, 0.52F + lowered * 1.5F, 0.72F);
        pose.mulPose(Axis.YP.rotationDegrees(m * 90.0F));
        pose.translate(0.0F, 0.0F, -0.72F);
        pose.mulPose(Axis.YP.rotationDegrees(m * 90.0F));
        // 4. The arm (:78-107).
        if (!player.isInvisible() && mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer) {
            pose.pushPose();
            pose.scale(2.0F, 2.0F, 2.0F);
            pose.translate(m * -0.5F, -0.7F, 0.6F);
            pose.mulPose(Axis.ZP.rotationDegrees(m * -90.0F));
            pose.scale(1.1F, 1.1F, 1.1F);
            pose.mulPose(Axis.ZP.rotationDegrees(m * 45.0F));
            pose.translate(0.0F, -0.5F, 0.5F);
            if (has) {
                if (!open) {
                    pose.mulPose(Axis.XP.rotationDegrees(wave));
                } else {
                    pose.mulPose(Axis.XP.rotationDegrees(using ? -8.0F : 0.0F));
                    pose.mulPose(Axis.YP.rotationDegrees(m * wave));
                }
            }
            if (using) {
                pose.mulPose(Axis.XP.rotationDegrees(Mth.clamp(-(used * 5.0F), -20.0F, 0.0F)));
                pose.translate(0.0F, 0.1F, 0.0F);
            }
            if (m > 0.0F) {
                renderer.renderRightHand(pose, buffers, light, player);
            } else {
                renderer.renderLeftHand(pose, buffers, light, player);
            }
            pose.popPose();
        }
        // 5. The syringe (:109-127).
        pose.translate(m * -1.0F, 0.6F, 1.8F);
        pose.mulPose(Axis.ZP.rotationDegrees(m * 35.0F));
        if (using) {
            pose.mulPose(Axis.ZP.rotationDegrees(m * -10.0F));
            pose.mulPose(Axis.YP.rotationDegrees(m * Mth.clamp(50.0F - used * 6.0F, 20.0F, 50.0F)));
            pose.translate(m * -0.1F, -0.2F, -1.0F);
        }
        if (has) {
            if (!open) {
                pose.mulPose(Axis.YP.rotationDegrees(m * wave));
            } else {
                pose.mulPose(Axis.YP.rotationDegrees(m * (using ? -10.0F : 0.0F)));
                pose.mulPose(Axis.ZP.rotationDegrees(m * wave));
            }
        }
        pose.scale(0.9F, 0.9F, 0.9F);
        DrainageSyringeRenderer.mirrorMesh(pose, m);
        DrainageSyringeRenderer.drawHeld(pose, buffers, light, has, state.type(), state.amount(), open);
        pose.popPose();
    }
}
