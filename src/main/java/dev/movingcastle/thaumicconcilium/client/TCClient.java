package dev.movingcastle.thaumicconcilium.client;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.entity.PositiveBurstOrb;
import dev.movingcastle.thaumicconcilium.entity.ShardPowder;
import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import dev.movingcastle.thaumicconcilium.registry.TCItems;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;

/** Client-only registration. */
@EventBusSubscriber(modid = ThaumicConcilium.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TCClient {
    private TCClient() {
    }

    @SubscribeEvent
    public static void itemExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new MillClientExtensions(), TCItems.SHARD_MILL.get(), TCItems.SILVERWOOD_SHARD_MILL.get());
    }

    /** The Shard Mill's body and crank models, drawn by {@link MillRenderer}. */
    @SubscribeEvent
    public static void extraModels(ModelEvent.RegisterAdditional event) {
        MillRenderer.MODELS.forEach(event::register);
    }

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent event) {
        event.register(BraceletInput.CAST);
    }

    @SubscribeEvent
    public static void guiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ThaumicConcilium.id("bracelet_hud"), new BraceletHud());
        event.registerAboveAll(ThaumicConcilium.id("bracelet_radial"), new BraceletRadial());
    }

    /** Bracelets on the wrists, held or worn ({@link BraceletArmLayer}), for both player models. */
    @SubscribeEvent
    public static void playerLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new BraceletArmLayer(renderer));
            }
        }
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        // Original: white sprite at 80% opacity, 0.3 scale.
        event.registerEntityRenderer(TCEntities.COMPRESSED_BLAST.get(), ctx -> new SparkleBillboardRenderer<>(ctx,
                e -> 0xCCFFFFFF, (e, pt) -> 0.6, false));
        // Original: additive, green (heal) / yellow (fulfillment) / blue (vitaminize), shrinking with age.
        event.registerEntityRenderer(TCEntities.POSITIVE_BURST_ORB.get(), ctx -> new SparkleBillboardRenderer<PositiveBurstOrb>(ctx,
                e -> switch (e.mode()) {
                    case PositiveBurstOrb.MODE_FULFILLMENT -> 0xFFFFFF00;
                    case PositiveBurstOrb.MODE_VITAMINIZE -> 0xFF1111FF;
                    default -> 0xFF00EE00;
                },
                (e, pt) -> {
                    float max = TCConfig.POSITIVE_ORB_LIFETIME.get();
                    float left = Math.max(0.0F, max - (e.tickCount + pt));
                    return 0.2 + 0.6 * (left / max);
                }, true));
        event.registerEntityRenderer(TCEntities.FLURRY_SHOT.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(TCEntities.WARP_CHARGE.get(), WarpChargeRenderer::new);
        event.registerEntityRenderer(TCEntities.THICK_TAINT_BOTTLE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(TCEntities.CLEAR_WATER_BOTTLE.get(), ThrownItemRenderer::new);
        // Original: additive particle-atlas sprite in the shard's aspect colour, half opacity, 0.1 scale.
        event.registerEntityRenderer(TCEntities.SHARD_POWDER.get(), ctx -> new SparkleBillboardRenderer<ShardPowder>(ctx,
                e -> 0x80000000 | e.kind().color(e.level().registryAccess()), (e, pt) -> 0.25, true));
    }
}
