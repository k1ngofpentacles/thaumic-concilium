package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * The Pontifex's robe on the biped skeleton (original: PontifexRobeModel). One baked mesh per slot: {@code cloth} (body group, legs
 * slot) and {@code armor} (body group, chest slot) and the boots groups are switched per slot, as the original did. Arms draw at 1.2
 * and legs at 1.05 scale and the cloth flaps with the stride, as in the original's render method.
 */
public final class PontifexRobeModel extends HumanoidModel<LivingEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ThaumicConcilium.id("pontifex_robe"), "main");

    private final ModelPart frontR1, frontR2, frontL1, frontL2, backR1, backR2, backR3, backL1, backL2, backL3;

    public PontifexRobeModel(ModelPart root, EquipmentSlot slot) {
        super(root);
        ModelPart cloth = body.getChild("cloth");
        ModelPart armor = body.getChild("armor");
        cloth.visible = slot == EquipmentSlot.LEGS;
        armor.visible = slot == EquipmentSlot.CHEST;
        rightLeg.getChild("Shape45RL").visible = slot == EquipmentSlot.LEGS;
        leftLeg.getChild("Shape50LL").visible = slot == EquipmentSlot.LEGS;
        rightLeg.getChild("rightBoot").visible = slot == EquipmentSlot.FEET;
        leftLeg.getChild("leftBoot").visible = slot == EquipmentSlot.FEET;
        frontR1 = cloth.getChild("FrontclothR1");
        frontR2 = cloth.getChild("FrontclothR2");
        frontL1 = cloth.getChild("FrontclothL1");
        frontL2 = cloth.getChild("FrontclothL2");
        backR1 = cloth.getChild("ClothBackR1");
        backR2 = cloth.getChild("ClothBackR2");
        backR3 = cloth.getChild("ClothBackR3");
        backL1 = cloth.getChild("ClothBackL1");
        backL2 = cloth.getChild("ClothBackL2");
        backL3 = cloth.getChild("ClothBackL3");
    }

    @Override
    public void setupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        float a = (float) Math.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        float b = (float) Math.cos(limbSwing * 0.6662F + Math.PI) * 1.4F * limbSwingAmount;
        float c = Math.min(a, b);
        frontR1.xRot = frontL1.xRot = c - 0.1047198F;
        frontR2.xRot = frontL2.xRot = c - 0.3316126F;
        backR1.xRot = backL1.xRot = -c + 0.1047198F;
        backR2.xRot = backR3.xRot = backL2.xRot = backL3.xRot = -c + 0.2268928F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
        pose.pushPose();
        pose.scale(1.2F, 1.2F, 1.2F);
        rightArm.render(pose, buffer, light, overlay, color);
        leftArm.render(pose, buffer, light, overlay, color);
        pose.popPose();
        head.render(pose, buffer, light, overlay, color);
        body.render(pose, buffer, light, overlay, color);
        pose.pushPose();
        pose.scale(1.05F, 1.05F, 1.05F);
        rightLeg.render(pose, buffer, light, overlay, color);
        leftLeg.render(pose, buffer, light, overlay, color);
        pose.popPose();
    }
}
