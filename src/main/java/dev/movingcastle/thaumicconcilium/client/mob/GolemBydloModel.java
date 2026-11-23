package dev.movingcastle.thaumicconcilium.client.mob;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The Golem Bydlo's own model (phase 8): a broad giant with a deep torso, a head sunk between two shoulder plates, thick arms
 * that end in heavy gauntlets at the knees, a boiler hump on its back (the rider's seat) and two stout legs. The envelope is
 * the original's (about 2 x 3.5 blocks); every box, the layout and the texture are ours (the original's model and texture
 * were Thaumcraft 4's). Units are model pixels, feet at y 24, the front is -z; the renderer scales it by 1.4 (39 px = 3.4
 * blocks). The texture is written by {@code tools/gen_phase8_data.py} with the same u,v per box (128 x 64).
 */
public final class GolemBydloModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ThaumicConcilium.id("golem_bydlo"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public GolemBydloModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.rightArm = body.getChild("right_arm");
        this.leftArm = body.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // hinged at the hips (16 px up): pelvis, the torso (14 x 15 x 10), a chest plate in front and the boiler hump behind
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 17).addBox(-7.0F, -16.0F, -5.0F, 14.0F, 15.0F, 10.0F)
                        .texOffs(48, 17).addBox(-6.0F, -1.0F, -4.0F, 12.0F, 5.0F, 8.0F)
                        .texOffs(72, 0).addBox(-5.0F, -14.0F, -6.5F, 10.0F, 7.0F, 2.0F)
                        .texOffs(100, 0).addBox(-5.0F, -15.0F, 4.5F, 10.0F, 9.0F, 3.0F),
                PartPose.offset(0.0F, 8.0F, 0.0F));
        // the head sits forward and two pixels down into the torso, below the tops of the shoulder plates
        body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -9.0F, -4.5F, 8.0F, 9.0F, 8.0F)
                        .texOffs(32, 0).addBox(-3.0F, -3.0F, -5.5F, 6.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, -14.0F, -4.0F));
        // arms: shoulder plate, upper arm, and a gauntlet thicker than the arm that hangs to the knee
        body.addOrReplaceChild("right_arm", arm(false), PartPose.offset(-10.0F, -14.0F, 0.0F));
        body.addOrReplaceChild("left_arm", arm(true), PartPose.offset(10.0F, -14.0F, 0.0F));
        root.addOrReplaceChild("right_leg", leg(false), PartPose.offset(-4.0F, 10.0F, 0.0F));
        root.addOrReplaceChild("left_leg", leg(true), PartPose.offset(4.0F, 10.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    private static CubeListBuilder arm(boolean mirror) {
        return CubeListBuilder.create().mirror(mirror)
                .texOffs(88, 17).addBox(-3.0F, -4.0F, -4.0F, 6.0F, 5.0F, 8.0F)
                .texOffs(0, 42).addBox(-2.5F, 1.0F, -3.0F, 5.0F, 9.0F, 6.0F)
                .texOffs(22, 42).addBox(-3.0F, 10.0F, -4.0F, 6.0F, 12.0F, 8.0F);
    }

    private static CubeListBuilder leg(boolean mirror) {
        return CubeListBuilder.create().mirror(mirror)
                .texOffs(50, 42).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 14.0F, 7.0F)
                .texOffs(48, 0).addBox(-3.5F, 11.0F, -5.5F, 7.0F, 3.0F, 2.0F);
    }

    public ModelPart root() {
        return root;
    }

    /**
     * The pose set. Idle: hunched, a slow breath, arms hanging a little out from the body. Walk: a heavy stride, legs and arms in
     * opposite swing, the torso rocking over the planted leg. Punch (the wand command): both arms thrown up and brought down
     * ({@code punch} is 1 at the top, 0 at rest). Ridden: the same walk with the arms carried a little forward.
     */
    public void pose(float ageInTicks, float walkPos, float walkSpeed, float headYaw, float headPitch, float punch, boolean ridden) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float breath = Mth.sin(ageInTicks * 0.08F);
        float stride = Mth.cos(walkPos * 0.5F) * walkSpeed;
        float hunch = 0.07F + 0.1F * walkSpeed;
        body.xRot = hunch;
        body.zRot = Mth.cos(walkPos * 0.5F + Mth.HALF_PI) * 0.045F * walkSpeed;
        body.y += Math.abs(Mth.sin(walkPos * 0.5F)) * 0.8F * walkSpeed;
        head.yRot = Mth.clamp(headYaw, -50.0F, 50.0F) * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(headPitch, -40.0F, 40.0F) * Mth.DEG_TO_RAD - hunch * 0.5F;
        rightLeg.xRot = stride * 0.75F;
        leftLeg.xRot = -stride * 0.75F;
        float carry = (ridden ? -0.3F : 0.0F) - hunch + breath * 0.015F;
        rightArm.xRot = carry - stride * 0.55F;
        leftArm.xRot = carry + stride * 0.55F;
        rightArm.zRot = 0.07F + breath * 0.012F;
        leftArm.zRot = -0.07F - breath * 0.012F;
        if (punch > 0.0F) {
            rightArm.xRot = Mth.lerp(punch, rightArm.xRot, -2.7F);
            leftArm.xRot = Mth.lerp(punch, leftArm.xRot, -2.7F);
            body.xRot = hunch - 0.22F * punch;
        }
    }
}
