package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/**
 * The Crimson Archer's and Ranger's extra parts (visor and quiver; shoulder spikes and wrist cuffs), drawn on top of Thaumaturge's worn
 * armour (memo docs/design/5a-mob-overlay-art.md section 2). One shared layer: a biped whose standard parts are empty (so only the extra
 * children draw) takes its pose from the parent model each frame. Textures are ours, written by the phase 5a generator.
 */
public final class CrimsonOverlayLayer<T extends Mob> extends RenderLayer<T, HumanoidModel<T>> {
    public static final ModelLayerLocation ARCHER = new ModelLayerLocation(ThaumicConcilium.id("crimson_archer_overlay"), "main");
    public static final ModelLayerLocation RANGER = new ModelLayerLocation(ThaumicConcilium.id("crimson_ranger_overlay"), "main");

    private final HumanoidModel<T> overlay;
    private final ResourceLocation texture;

    public CrimsonOverlayLayer(RenderLayerParent<T, HumanoidModel<T>> parent, ModelPart root, ResourceLocation texture) {
        super(parent);
        this.overlay = new HumanoidModel<>(root);
        this.texture = texture;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        getParentModel().copyPropertiesTo(overlay);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        overlay.renderToBuffer(pose, vc, light, LivingEntityRenderer.getOverlayCoords(entity, 0.0F), -1);
    }

    private static PartDefinition[] biped(MeshDefinition mesh) {
        PartDefinition root = mesh.getRoot();
        return new PartDefinition[]{
                root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO),
                root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO),
                root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F)),
                root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F))};
    }

    private static void legsAndHat(MeshDefinition mesh) {
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));
    }

    /** Archer: a visor over the face (head) and a slanted quiver with three arrow ends on the back (body). */
    public static LayerDefinition archer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition[] p = biped(mesh);
        legsAndHat(mesh);
        p[0].addOrReplaceChild("visor", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-5.0F, -5.0F, -5.75F, 10.0F, 6.0F, 1.0F, new CubeDeformation(-0.3F)), PartPose.ZERO);
        PartDefinition quiver = p[1].addOrReplaceChild("quiver", CubeListBuilder.create().texOffs(0, 8)
                .addBox(-2.0F, -5.5F, 0.0F, 4.0F, 11.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 5.5F, QUIVER_Z, 0.0F, 0.0F, 0.61F));
        quiver.addOrReplaceChild("arrows", CubeListBuilder.create().texOffs(16, 8)
                .addBox(-1.5F, -8.0F, 1.0F, 3.0F, 3.0F, 1.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Distance of the quiver's front face behind the body centre (memo: 5.2; tuned against the robe chest's back). */
    private static final float QUIVER_Z = 5.2F;

    /** Ranger: three rods fanning out of each shoulder with a collar each, and a wide cuff at each wrist (arms). */
    public static LayerDefinition ranger() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition[] p = biped(mesh);
        legsAndHat(mesh);
        float[][] spikes = {{-3.5F, -2.5F, -1.5F, 1.25F}, {-4.0F, -0.5F, 0.0F, 1.0F}, {-3.5F, 1.5F, 1.5F, 0.75F}};
        for (boolean left : new boolean[]{false, true}) {
            PartDefinition arm = p[left ? 3 : 2];
            float sign = left ? -1.0F : 1.0F;
            for (int i = 0; i < spikes.length; i++) {
                float[] s = spikes[i];
                CubeListBuilder spike = CubeListBuilder.create().texOffs(0, 0);
                CubeListBuilder collar = CubeListBuilder.create().texOffs(4, 0);
                if (left) {
                    spike = spike.mirror();
                    collar = collar.mirror();
                }
                PartDefinition sp = arm.addOrReplaceChild("spike" + i, spike.addBox(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F),
                        PartPose.offsetAndRotation(s[0] * sign, s[1], s[2], 0.0F, 0.0F, s[3] * sign));
                sp.addOrReplaceChild("collar", collar.addBox(-1.0F, 3.0F, -1.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(-0.2F)), PartPose.ZERO);
            }
            CubeListBuilder cuff = CubeListBuilder.create().texOffs(0, 8);
            if (left) {
                cuff = cuff.mirror();
            }
            arm.addOrReplaceChild("cuff", cuff.addBox(left ? -1.5F : -3.5F, 6.5F, -2.5F, 5.0F, 4.0F, 5.0F), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 64, 32);
    }
}
