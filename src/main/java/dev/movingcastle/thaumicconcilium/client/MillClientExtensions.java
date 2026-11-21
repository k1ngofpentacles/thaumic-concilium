package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.movingcastle.thaumicconcilium.item.ShardMill;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Client hooks for both Shard Mills: the {@link MillRenderer}, the third-person grinding pose ({@link MillArmPose}),
 * and the first-person placement. Loading tips the mill towards you as the shard drops in; grinding holds it out in
 * front of you, bottom forward, with the other hand on the crank ({@link MillFirstPersonArm}).
 */
public final class MillClientExtensions implements IClientItemExtensions {
    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return MillRenderer.get();
    }

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        return MillRenderer.isUsing(entity, stack) ? MillArmPose.CRANK.getValue() : null;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                           float partialTick, float equipProcess, float swingProcess) {
        if (!player.isUsingItem() || player.getUseItem() != stack) {
            return false;
        }
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        // Vanilla's resting arm position (ItemInHandRenderer.applyItemArmTransform).
        pose.translate(side * 0.56F, -0.52F + equipProcess * -0.6F, -0.72F);
        if (ShardMill.state(stack).open()) {
            float t = Mth.clamp((player.getTicksUsingItem() + partialTick) / ShardMill.LOAD, 0.0F, 1.0F);
            pose.translate(-side * 0.15F * t, 0.12F * t, 0.1F * t);
            pose.mulPose(Axis.XP.rotationDegrees(-35.0F * t));
            pose.mulPose(Axis.ZP.rotationDegrees(side * 10.0F * t));
        } else {
            // Held out in the middle of the view; MillRenderer turns it bottom-forward and spins the crank.
            float shake = Mth.sin(MillRenderer.crankDegrees(player, partialTick) * Mth.DEG_TO_RAD) * 0.01F;
            pose.translate(-side * 0.3F, 0.08F + shake, 0.0F);
        }
        return true;
    }
}
