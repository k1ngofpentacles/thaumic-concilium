package dev.movingcastle.thaumicconcilium.client;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.entity.PositiveBurstOrb;
import dev.movingcastle.thaumicconcilium.entity.ShardPowder;
import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import dev.movingcastle.thaumicconcilium.registry.TCItems;
import dev.movingcastle.thaumicconcilium.client.mob.CorpseRenderer;
import dev.movingcastle.thaumicconcilium.client.mob.TCModelLayers;
import dev.movingcastle.thaumicconcilium.client.mob.ThaumaturgeRenderer;
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
        // Phase 6a: the charged wisp in its aspect's colour; the primal orb cycling the primal colours (original art
        // was Thaumcraft 4's wisp and orb sprites, all rights reserved).
        event.registerEntityRenderer(TCEntities.CHARGED_WISP.get(), ChargedWispRenderer::new);
        int[] primals = {0xFFFFFF7E, 0xFFFF5A01, 0xFF3CD4FC, 0xFF56C000, 0xFFD5D4EC, 0xFF404040};
        event.registerEntityRenderer(TCEntities.PRIMAL_ORB.get(), ctx -> new SparkleBillboardRenderer<dev.movingcastle.thaumicconcilium.entity.PrimalOrb>(ctx,
                e -> primals[(e.tickCount / 4) % primals.length], (e, pt) -> 0.8, true));
        event.registerBlockEntityRenderer(dev.movingcastle.thaumicconcilium.registry.TCBlocks.HEX_BLOCK_ENTITY.get(), HexRenderer::new);
        // Phase 4 mobs.
        event.registerEntityRenderer(TCEntities.THAUMATURGE.get(), ctx -> new ThaumaturgeRenderer<>(ctx, "thaumaturge"));
        event.registerEntityRenderer(TCEntities.MAD_THAUMATURGE.get(), ctx -> new ThaumaturgeRenderer<>(ctx, "mad_thaumaturge"));
        event.registerEntityRenderer(TCEntities.NETHER_EXPLORER.get(),
                ctx -> new CorpseRenderer<>(ctx, TCModelLayers.NETHER_EXPLORER, "nether_explorer"));
        if (TCEntities.hasBotanist()) {
            event.registerEntityRenderer(TCEntities.WITHERED_BOTANIST.get(),
                    ctx -> new CorpseRenderer<>(ctx, TCModelLayers.WITHERED_BOTANIST, "withered_botanist"));
        }
    }

    /** The destabilized crystal takes its aspect's colour (original: colorMultiplier from the tile's aspect). */
    @SubscribeEvent
    public static void blockColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> level != null && pos != null
                        && level.getBlockEntity(pos) instanceof dev.movingcastle.thaumicconcilium.block.DestabilizedCrystalBlockEntity crystal
                        ? crystal.color() : 0xFFFFFF,
                dev.movingcastle.thaumicconcilium.registry.TCBlocks.DESTABILIZED_CRYSTAL.get());
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(TCModelLayers.THAUMATURGE, TCModelLayers::biped);
        event.registerLayerDefinition(TCModelLayers.NETHER_EXPLORER, TCModelLayers::explorer);
        event.registerLayerDefinition(TCModelLayers.WITHERED_BOTANIST, TCModelLayers::botanist);
    }

    @SubscribeEvent
    public static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(dev.movingcastle.thaumicconcilium.registry.TCMenus.THAUMATURGE.get(), ThaumaturgeScreen::new);
    }
}
