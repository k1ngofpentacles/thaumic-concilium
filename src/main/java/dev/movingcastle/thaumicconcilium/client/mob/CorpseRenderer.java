package dev.movingcastle.thaumicconcilium.client.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.mob.Corpse;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Original NetherExplorerRenderer / WitheredBotanistRenderer: the body drawn flat on its back, 0.1 above its position,
 * turned to its yaw, without culling. Like the original it skips the living renderer's mirror flip.
 */
public final class CorpseRenderer<T extends Corpse> extends EntityRenderer<T> {
    private final ModelPart model;
    private final ResourceLocation texture;

    public CorpseRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer, String texture) {
        super(context);
        this.model = context.bakeLayer(layer);
        this.texture = ThaumicConcilium.id("textures/entity/" + texture + ".png");
        this.shadowRadius = 0.5F;
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0.0, 0.1, 0.0);
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot())));
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        this.model.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(this.texture)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return this.texture;
    }
}
