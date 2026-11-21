package dev.movingcastle.thaumicconcilium.client.mob;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Model layers for the phase 4 mobs, with the original geometry. The Thaumaturges are plain 1.7 bipeds (64x32 UVs; their
 * textures are 128x64, the same layout at double resolution); the corpses are the original's lying bipeds.
 */
public final class TCModelLayers {
    public static final ModelLayerLocation THAUMATURGE = new ModelLayerLocation(ThaumicConcilium.id("thaumaturge"), "main");
    public static final ModelLayerLocation NETHER_EXPLORER = new ModelLayerLocation(ThaumicConcilium.id("nether_explorer"), "main");
    public static final ModelLayerLocation WITHERED_BOTANIST = new ModelLayerLocation(ThaumicConcilium.id("withered_botanist"), "main");

    private TCModelLayers() {
    }

    public static LayerDefinition biped() {
        return LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 32);
    }

    /** Original NetherExplorerModel: skeleton-thin limbs, everything inflated by 0.5. */
    public static LayerDefinition explorer() {
        CubeDeformation d = new CubeDeformation(0.5F);
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8, d), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4, 0, -2, 8, 12, 4, d), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-1, -2, -1, 2, 12, 2, d), PartPose.offset(-5, 2, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1, -2, -1, 2, 12, 2, d), PartPose.offset(5, 2, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-1, 0, -1, 2, 12, 2, d), PartPose.offset(-2, 12, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1, 0, -1, 2, 12, 2, d), PartPose.offset(2, 12, 0));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Original WitheredBotanistModel: a standard biped. */
    public static LayerDefinition botanist() {
        CubeDeformation d = CubeDeformation.NONE;
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8, d), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4, 0, -2, 8, 12, 4, d), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3, -2, -2, 4, 12, 4, d), PartPose.offset(-5, 2, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1, -2, -2, 4, 12, 4, d), PartPose.offset(5, 2, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 12, 4, d), PartPose.offset(-1.9F, 12, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2, 0, -2, 4, 12, 4, d), PartPose.offset(1.9F, 12, 0));
        return LayerDefinition.create(mesh, 64, 32);
    }
}
