package dev.movingcastle.thaumicconcilium.client;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * Third person: the Crimson Dagger's arm poses. One {@link HumanoidModel.ArmPose} (added through
 * {@code META-INF/enumextensions.json}, as {@link MillArmPose}) set on both arms just before the player is drawn; the
 * transformer asks the built option ({@link DaggerPoses#ACTIVE}) for the holding arm's and the free arm's targets and
 * blends each into whatever vanilla set. Set in {@link RenderPlayerEvent.Pre} (not {@code getArmPose}) because the stab and
 * the cut are not item use. The free arm is posed only when its hand is empty.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID, value = Dist.CLIENT)
public final class DaggerArmPose {
    public static final EnumProxy<HumanoidModel.ArmPose> POSE = new EnumProxy<>(HumanoidModel.ArmPose.class,
            false, (IArmPoseTransformer) DaggerArmPose::apply);

    private DaggerArmPose() {
    }

    private static void apply(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        float pt = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        DaggerAnim.Frame frame = DaggerAnim.frame(entity, pt);
        if (frame.mode() == DaggerAnim.Mode.REST) {
            return;
        }
        boolean holding = arm == DaggerAnim.holdingArm(entity, frame);
        float s = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        ModelPart part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        DaggerAnim.Arm target = holding ? DaggerPoses.ACTIVE.holdingArm(frame, model, s) : DaggerPoses.ACTIVE.freeArm(frame, model, s);
        if (holding) {
            // Our pose replaced vanilla's ITEM pose on this arm: start from it, so weight 0 is exactly the resting arm
            // (HumanoidModel.poseRightArm, ITEM) and the raise does not begin with an 18 degree jump.
            part.xRot = part.xRot * 0.5F - (float) (Math.PI / 10.0);
            part.yRot = 0.0F;
        }
        if (target.w() <= 0.0F) {
            return;
        }
        float w = Mth.clamp(target.w(), 0.0F, 1.0F);
        part.xRot = Mth.lerp(w, part.xRot, target.p() * Mth.DEG_TO_RAD);
        part.yRot = Mth.lerp(w, part.yRot, -s * target.y() * Mth.DEG_TO_RAD);
        part.zRot = Mth.lerp(w, part.zRot, s * target.r() * Mth.DEG_TO_RAD);
        part.z -= w * target.forward();
        if (holding) {
            part.z -= DaggerPoses.ACTIVE.shoulderSlide(frame);
        }
    }

    @SubscribeEvent
    public static void beforePlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        DaggerAnim.Frame frame = DaggerAnim.frame(player, event.getPartialTick());
        if (frame.mode() == DaggerAnim.Mode.REST) {
            return;
        }
        PlayerModel<?> model = event.getRenderer().getModel();
        HumanoidArm holding = DaggerAnim.holdingArm(player, frame);
        InteractionHand freeHand = frame.mainHand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        boolean freeToo = player.getItemInHand(freeHand).isEmpty();
        if (holding == HumanoidArm.RIGHT) {
            model.rightArmPose = POSE.getValue();
            if (freeToo) {
                model.leftArmPose = POSE.getValue();
            }
        } else {
            model.leftArmPose = POSE.getValue();
            if (freeToo) {
                model.rightArmPose = POSE.getValue();
            }
        }
    }
}
