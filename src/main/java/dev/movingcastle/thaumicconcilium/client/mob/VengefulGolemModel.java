package dev.movingcastle.thaumicconcilium.client.mob;

import dev.movingcastle.thaumicconcilium.entity.mob.VengefulGolem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Original VengefulGolemModel (a 1.7.10 ModelBiped, 64x32 UVs over a 128x64 texture) with its setupAnim translated line by line.
 * The head is drawn at 0.65 x 0.75 x 0.65 about its pivot (the original's glScalef(1.3/2, 1.5/2, 1.3/2)); the hat layer is never
 * drawn (an empty "hat" part only satisfies HumanoidModel). Held items and the bow pose were never set for this golem and are
 * left out.
 */
public final class VengefulGolemModel extends HumanoidModel<VengefulGolem> {
    public static final float HEAD_SCALE_XZ = 1.3F / 2.0F;
    public static final float HEAD_SCALE_Y = 1.5F / 2.0F;

    public VengefulGolemModel(ModelPart root) {
        super(root);
        this.head.xScale = HEAD_SCALE_XZ;
        this.head.yScale = HEAD_SCALE_Y;
        this.head.zScale = HEAD_SCALE_XZ;
    }

    /** The body: the standard biped boxes (left limbs mirrored), no hat. */
    public static LayerDefinition body() {
        CubeDeformation d = CubeDeformation.NONE;
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8, d), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4, 0, -2, 8, 12, 4, d), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3, -2, -2, 4, 12, 4, d), PartPose.offset(-5, 2, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1, -2, -2, 4, 12, 4, d), PartPose.offset(5, 2, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 12, 4, d), PartPose.offset(-1.9F, 12, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2, 0, -2, 4, 12, 4, d), PartPose.offset(1.9F, 12, 0));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(VengefulGolem entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw / (180.0F / (float) Math.PI);
        this.head.xRot = headPitch / (180.0F / (float) Math.PI);
        this.rightArm.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 2.0F * limbSwingAmount * 0.5F;
        this.leftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F;
        this.rightArm.zRot = 0.0F;
        this.leftArm.zRot = 0.0F;
        this.rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
        this.rightLeg.yRot = 0.0F;
        this.leftLeg.yRot = 0.0F;
        if (this.riding) {
            this.rightArm.xRot += (float) (-Math.PI / 5);
            this.leftArm.xRot += (float) (-Math.PI / 5);
            this.rightLeg.xRot = (float) (-Math.PI * 2.0 / 5.0);
            this.leftLeg.xRot = (float) (-Math.PI * 2.0 / 5.0);
            this.rightLeg.yRot = (float) (Math.PI / 10);
            this.leftLeg.yRot = (float) (-Math.PI / 10);
        }
        this.rightArm.yRot = 0.0F;
        this.leftArm.yRot = 0.0F;
        // The swing (the original's onGround block; at rest every term is zero, which also restores the arm pivots).
        float swing = Math.max(this.attackTime, 0.0F);
        this.body.yRot = Mth.sin(Mth.sqrt(swing) * (float) Math.PI * 2.0F) * 0.2F;
        this.rightArm.z = Mth.sin(this.body.yRot) * 5.0F;
        this.rightArm.x = -Mth.cos(this.body.yRot) * 5.0F;
        this.leftArm.z = -Mth.sin(this.body.yRot) * 5.0F;
        this.leftArm.x = Mth.cos(this.body.yRot) * 5.0F;
        this.rightArm.yRot += this.body.yRot;
        this.leftArm.yRot += this.body.yRot;
        this.leftArm.xRot += this.body.yRot;
        float f6 = 1.0F - swing;
        f6 *= f6;
        f6 *= f6;
        f6 = 1.0F - f6;
        float f7 = Mth.sin(f6 * (float) Math.PI);
        float f8 = Mth.sin(swing * (float) Math.PI) * -(this.head.xRot - 0.7F) * 0.75F;
        this.rightArm.xRot = (float) ((double) this.rightArm.xRot - ((double) f7 * 1.2 + (double) f8));
        this.rightArm.yRot += this.body.yRot * 2.0F;
        this.rightArm.zRot = Mth.sin(swing * (float) Math.PI) * -0.4F;
        if (this.crouching) {
            this.body.xRot = 0.5F;
            this.rightArm.xRot += 0.4F;
            this.leftArm.xRot += 0.4F;
            this.rightLeg.z = 4.0F;
            this.leftLeg.z = 4.0F;
            this.rightLeg.y = 9.0F;
            this.leftLeg.y = 9.0F;
            this.head.y = 1.0F;
        } else {
            this.body.xRot = 0.0F;
            this.rightLeg.z = 0.1F;
            this.leftLeg.z = 0.1F;
            this.rightLeg.y = 12.0F;
            this.leftLeg.y = 12.0F;
            this.head.y = 0.0F;
        }
        this.rightArm.zRot += Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.leftArm.zRot -= Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.rightArm.xRot += Mth.sin(ageInTicks * 0.067F) * 0.05F;
        this.leftArm.xRot -= Mth.sin(ageInTicks * 0.067F) * 0.05F;
    }
}
