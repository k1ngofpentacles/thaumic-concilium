package dev.movingcastle.thaumicconcilium.client.mob;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.mob.Chort;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Original ChortRenderer: a bare model, no held item and no armour layers. */
public final class ChortRenderer extends MobRenderer<Chort, ChortModel<Chort>> {
    private static final ResourceLocation TEXTURE = ThaumicConcilium.id("textures/entity/chort.png");

    public ChortRenderer(EntityRendererProvider.Context context) {
        super(context, new ChortModel<>(context.bakeLayer(TCModelLayers.CHORT)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(Chort entity) {
        return TEXTURE;
    }
}
