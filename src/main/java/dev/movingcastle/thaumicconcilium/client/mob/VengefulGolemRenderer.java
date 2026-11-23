package dev.movingcastle.thaumicconcilium.client.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.mob.VengefulGolem;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Original VengefulGolemRenderer: a biped with the golem texture holding a golem bell (the normal item-in-hand layer here), and
 * the brain in its jar on the shrunken head ({@link BrainLayer}).
 */
public final class VengefulGolemRenderer extends HumanoidMobRenderer<VengefulGolem, VengefulGolemModel> {
    private static final ResourceLocation TEXTURE = ThaumicConcilium.id("textures/entity/vengeful_golem.png");

    public VengefulGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new VengefulGolemModel(context.bakeLayer(TCModelLayers.VENGEFUL_GOLEM)), 0.5F);
        this.addLayer(new BrainLayer(this, context.bakeLayer(TCModelLayers.VENGEFUL_DECORATION)));
    }

    @Override
    public ResourceLocation getTextureLocation(VengefulGolem entity) {
        return TEXTURE;
    }

    /**
     * The brain and the jar: drawn with the head's rotation and scale, raised by 0.22 of a block in the head's scaled frame
     * (the original's glTranslatef(0, -0.22, 0) after its glScalef), the jar blended over the brain.
     */
    static final class BrainLayer extends RenderLayer<VengefulGolem, VengefulGolemModel> {
        private static final ResourceLocation DECORATION = ThaumicConcilium.id("textures/entity/vengeful_golem_decoration.png");
        private final ModelPart brain;
        private final ModelPart jar;

        BrainLayer(RenderLayerParent<VengefulGolem, VengefulGolemModel> parent, ModelPart root) {
            super(parent);
            this.brain = root.getChild("brain");
            this.jar = root.getChild("jar");
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, VengefulGolem entity, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            ModelPart head = getParentModel().head;
            int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);
            for (ModelPart part : new ModelPart[]{brain, jar}) {
                part.copyFrom(head);
                part.y = head.y - 0.22F * 16.0F * VengefulGolemModel.HEAD_SCALE_Y;
            }
            brain.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(DECORATION)), light, overlay);
            jar.render(pose, buffers.getBuffer(RenderType.entityTranslucent(DECORATION)), light, overlay);
        }
    }
}
