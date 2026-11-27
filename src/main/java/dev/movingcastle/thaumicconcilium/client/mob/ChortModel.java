package dev.movingcastle.thaumicconcilium.client.mob;

import com.google.common.collect.Iterables;
import java.util.List;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.world.entity.LivingEntity;

/**
 * Original ChortModel: the 1.7 biped plus a flat tail plane (56,18; 0 x 11 x 3) hung from the shoulders at 45 degrees, turning with the
 * body. Seated (riding the Sloppy Alchemist) it takes the original's pose, not 1.21's riding pose. Look memo: docs/design/phase10-look-prebrief.md 0, 1.
 */
public class ChortModel<T extends LivingEntity> extends HumanoidModel<T> {
    private final ModelPart tail;

    public ChortModel(ModelPart root) {
        super(root);
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        mesh.getRoot().addOrReplaceChild("tail", CubeListBuilder.create().texOffs(56, 18).addBox(0.0F, -4.0F, 8.0F, 0.0F, 11.0F, 3.0F),
                PartPose.rotation(-0.7853982F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    protected Iterable<ModelPart> bodyParts() {
        return Iterables.concat(super.bodyParts(), List.of(this.tail));
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.riding = false; // 1.21's riding pose differs from the original's (see seatPose)
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        this.tail.xRot = -0.7853982F;
        this.tail.yRot = this.body.yRot;
        if (entity.isPassenger()) {
            seatPose(this);
        }
    }

    /** The original's seat pose (Chort when riding): arms forward-up a little, legs forward and slightly apart. */
    public static void seatPose(HumanoidModel<?> m) {
        m.rightArm.xRot += -0.62831855F;
        m.leftArm.xRot += -0.62831855F;
        m.rightLeg.xRot = -1.2566371F;
        m.leftLeg.xRot = -1.2566371F;
        m.rightLeg.yRot = 0.31415927F;
        m.leftLeg.yRot = -0.31415927F;
        m.rightLeg.zRot = 0.0F;
        m.leftLeg.zRot = 0.0F;
    }
}
