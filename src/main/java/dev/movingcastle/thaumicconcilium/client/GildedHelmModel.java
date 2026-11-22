package dev.movingcastle.thaumicconcilium.client;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * The Gilded Crimson Helm's worn model (our own art, memo section C of docs/design/5a-mob-overlay-art.md): a dark cube shell with a gold
 * cross slit, and a gold fin on each side sweeping up and back. Hung on the biped skeleton's head, so it follows head turns on players and
 * mobs alike; texture {@code textures/models/armor/gilded_crimson_layer_1.png} (64x32), written by the phase 5a generator.
 */
public final class GildedHelmModel extends HumanoidModel<LivingEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ThaumicConcilium.id("gilded_helm"), "main");

    public GildedHelmModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.75F)), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));
        for (boolean left : new boolean[]{false, true}) {
            CubeListBuilder fin = CubeListBuilder.create().texOffs(0, 16);
            CubeListBuilder tip = CubeListBuilder.create().texOffs(16, 16);
            if (left) {
                fin = fin.mirror();
                tip = tip.mirror();
            }
            PartDefinition f = head.addOrReplaceChild(left ? "left_fin" : "right_fin",
                    fin.addBox(0.0F, -2.0F, -1.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(left ? 4.75F : -5.75F, -5.0F, 0.0F, 0.35F, 0.0F, 0.0F));
            f.addOrReplaceChild("tip", tip.addBox(0.0F, -4.0F, 2.0F, 1.0F, 2.0F, 4.0F), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Registered for the helm in {@link TCClient#itemExtensions}: one baked model, shared. */
    public static final class Extensions implements IClientItemExtensions {
        private HumanoidModel<?> model;

        @Override
        public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
            if (model == null) {
                model = new GildedHelmModel(Minecraft.getInstance().getEntityModels().bakeLayer(LAYER));
            }
            return model;
        }
    }
}
