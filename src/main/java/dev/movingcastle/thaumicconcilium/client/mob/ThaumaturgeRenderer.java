package dev.movingcastle.thaumicconcilium.client.mob;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** Original ThaumaturgeRenderer / MadThaumaturgeRenderer: a biped with its texture, holding its wand or tool. */
public final class ThaumaturgeRenderer<T extends Mob> extends HumanoidMobRenderer<T, HumanoidModel<T>> {
    private final ResourceLocation texture;

    public ThaumaturgeRenderer(EntityRendererProvider.Context context, String texture) {
        super(context, new HumanoidModel<>(context.bakeLayer(TCModelLayers.THAUMATURGE)), 0.5F);
        this.texture = ThaumicConcilium.id("textures/entity/" + texture + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return this.texture;
    }
}
