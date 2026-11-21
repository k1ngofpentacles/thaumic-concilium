package dev.movingcastle.thaumicconcilium.client;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * Third person: while a player casts from a bracelet, their casting arm comes up in front of them, pointing where they
 * look with the palm out, and goes back down after the cast ({@link BraceletCastState#raise}). An
 * {@link HumanoidModel.ArmPose} added through {@code META-INF/enumextensions.json}, set on the casting arm just before
 * the player is drawn.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID, value = Dist.CLIENT)
public final class BraceletArmPose {
    public static final EnumProxy<HumanoidModel.ArmPose> CAST = new EnumProxy<>(HumanoidModel.ArmPose.class,
            false, (IArmPoseTransformer) BraceletArmPose::apply);

    private BraceletArmPose() {
    }

    private static void apply(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        float up = BraceletCastState.raise(entity, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        ModelPart part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        // Straight out along the look, turned a little in towards the middle, slightly rolled so the palm faces out.
        part.xRot = Mth.lerp(up, part.xRot, model.head.xRot - Mth.HALF_PI - 0.1F);
        part.yRot = Mth.lerp(up, part.yRot, model.head.yRot - side * 0.25F);
        part.zRot = Mth.lerp(up, part.zRot, side * 0.1F);
    }

    @SubscribeEvent
    public static void beforePlayer(RenderPlayerEvent.Pre event) {
        LivingEntity player = event.getEntity();
        if (BraceletCastState.raise(player, event.getPartialTick()) <= 0.0F) {
            return;
        }
        PlayerModel<?> model = event.getRenderer().getModel();
        if (BraceletCastState.castingArm(player) == HumanoidArm.RIGHT) {
            model.rightArmPose = CAST.getValue();
        } else {
            model.leftArmPose = CAST.getValue();
        }
    }
}
