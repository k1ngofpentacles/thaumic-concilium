package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.item.CrimsonDagger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * First person: the dagger's free arm. An empty off hand draws nothing in vanilla, so while the option wants an arm in
 * view ({@link DaggerPose#freeHandFirstPerson}) it is drawn here. Only when the dagger is in the main hand and the off hand
 * is empty.
 * <p>
 * The arm is placed by its fist, not by vanilla's resting-arm chain (which only suits an arm rising from the bottom
 * corner and draws a slab anywhere else): the option gives the fist's centre in view space and three angles. With all
 * angles 0 the forearm hangs straight down from the fist; {@code roll} turns it in the screen plane (counter-clockwise,
 * 180 = the arm comes down from the top edge), {@code pitch} tips the shoulder end away from the eye (negative: towards
 * it), {@code yaw} spins the arm about its own length. {@code slide} pulls the arm back along its length (out of view).
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID, value = Dist.CLIENT)
public final class DaggerFirstPerson {
    /**
     * The fist's centre in the arm part's own space, in blocks: 8 px down the arm (the cube ends at 10) and 1 px outward
     * of the shoulder pivot (5, 2, 0), turned by the 0.1 rad idle roll {@code PlayerRenderer.renderHand} leaves on the arm.
     */
    private static final float FIST_X = 6.79F / 16.0F;
    private static final float FIST_Y = 9.86F / 16.0F;

    private DaggerFirstPerson() {
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || event.getHand() != InteractionHand.OFF_HAND || !event.getItemStack().isEmpty()
                || !(player.getMainHandItem().getItem() instanceof CrimsonDagger)) {
            return;
        }
        DaggerAnim.Frame frame = DaggerAnim.frame(player, event.getPartialTick());
        if (frame.mode() == DaggerAnim.Mode.REST || !frame.mainHand()) {
            return;
        }
        float s = player.getMainArm() == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float[] at = DaggerPoses.ACTIVE.freeHandFirstPerson(frame, s);
        if (at == null) {
            return;
        }
        event.setCanceled(true);
        boolean rightArm = s < 0.0F;
        PoseStack pose = event.getPoseStack();
        PlayerRenderer renderer = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        pose.pushPose();
        pose.translate(at[0], at[1], at[2]);
        if (at.length >= 6) {
            pose.mulPose(Axis.ZP.rotationDegrees(at[5]));
            pose.mulPose(Axis.XP.rotationDegrees(at[3]));
            pose.mulPose(Axis.YP.rotationDegrees(at[4]));
        }
        // The part's space has the hand above the shoulder (model y runs down the arm, view y runs up).
        pose.translate(rightArm ? FIST_X : -FIST_X, -FIST_Y - (at.length >= 7 ? at[6] : 0.0F), 0.0F);
        if (rightArm) {
            renderer.renderRightHand(pose, event.getMultiBufferSource(), event.getPackedLight(), player);
        } else {
            renderer.renderLeftHand(pose, event.getMultiBufferSource(), event.getPackedLight(), player);
        }
        pose.popPose();
    }
}
