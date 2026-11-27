package dev.movingcastle.thaumicconcilium.client.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.movingcastle.thaumicconcilium.entity.mob.SloppyAlchemist;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Original SloppyAlchemistRenderer's second model (used while a Chort rides him): on all fours, back level, head forward.
 * Numbers from the original model code (docs/design/phase10-look-prebrief.md 2); no part is mirrored and nothing is parented.
 */
public final class RiddenAlchemistModel extends EntityModel<SloppyAlchemist> {
    private static final float DEG = (float) (Math.PI / 180.0);
    private final ModelPart root;
    private final ModelPart head, hat, body, rightArm, leftArm, rightLeg, leftLeg;

    public RiddenAlchemistModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.hat = root.getChild("hat");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8), PartPose.offset(0, 13, -9));
        r.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4, -8, -4, 8, 8, 8, new CubeDeformation(0.5F)),
                PartPose.offset(0, 13, -9));
        r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4, 0, -2, 8, 12, 4),
                PartPose.offsetAndRotation(0, 13, -5, 1.5707964F, 0, 0));
        r.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3, 0, -2, 4, 12, 4), PartPose.offset(-5, 12, -5));
        r.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-1, 0, -2, 4, 12, 4), PartPose.offset(5, 12, -5));
        r.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(-2, 12, 5));
        r.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(2, 12, 5));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(SloppyAlchemist entity, float ls, float la, float ageInTicks, float netHeadYaw, float headPitch) {
        this.head.xRot = headPitch * DEG;
        this.head.yRot = netHeadYaw * DEG;
        this.hat.copyFrom(this.head);
        this.body.xRot = 1.5707964F;
        this.rightArm.xRot = (float) Math.cos(ls * 0.6662F) * 1.4F * la;
        this.leftLeg.xRot = this.rightArm.xRot;
        this.leftArm.xRot = (float) Math.cos(ls * 0.6662F + Math.PI) * 1.4F * la;
        this.rightLeg.xRot = this.leftArm.xRot;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
        this.root.render(pose, buffer, light, overlay, color);
    }
}
