package dev.movingcastle.thaumicconcilium.client.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.mob.SloppyAlchemist;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Original SloppyAlchemistRenderer: the standing (inflated biped) model, or the all-fours model while a Chort rides him; chosen per frame. */
public final class SloppyAlchemistRenderer extends MobRenderer<SloppyAlchemist, EntityModel<SloppyAlchemist>> {
    private static final ResourceLocation TEXTURE = ThaumicConcilium.id("textures/entity/sloppy_alchemist.png");
    private final EntityModel<SloppyAlchemist> free;
    private final EntityModel<SloppyAlchemist> ridden;

    public SloppyAlchemistRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(TCModelLayers.SLOPPY_ALCHEMIST)), 0.5F);
        this.free = this.model;
        this.ridden = new RiddenAlchemistModel(context.bakeLayer(TCModelLayers.SLOPPY_ALCHEMIST_RIDDEN));
    }

    @Override
    public void render(SloppyAlchemist entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        this.model = entity.ridden() ? this.ridden : this.free;
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SloppyAlchemist entity) {
        return TEXTURE;
    }
}
