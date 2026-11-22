package dev.movingcastle.thaumicconcilium.client;

import com.leclowndu93150.thaumaturge.client.entity.TTModelLayers;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** The Crimson mobs: Thaumaturge's cultist model (a plain biped) in their own texture, with worn armour and held items. */
public final class CrimsonCultistRenderer<T extends Mob> extends HumanoidMobRenderer<T, HumanoidModel<T>> {
    private final ResourceLocation texture;

    public CrimsonCultistRenderer(EntityRendererProvider.Context context, ResourceLocation texture) {
        this(context, texture, null, null);
    }

    /** With {@code overlayLayer} and {@code overlayTexture}: our extra parts (visor, quiver, spikes, cuffs) drawn after the worn armour. */
    public CrimsonCultistRenderer(EntityRendererProvider.Context context, ResourceLocation texture,
                                  net.minecraft.client.model.geom.ModelLayerLocation overlayLayer, ResourceLocation overlayTexture) {
        super(context, new HumanoidModel<>(context.bakeLayer(TTModelLayers.CULTIST)), 0.5F);
        this.texture = texture;
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
        if (overlayLayer != null) {
            addLayer(new CrimsonOverlayLayer<>(this, context.bakeLayer(overlayLayer), overlayTexture));
        }
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
