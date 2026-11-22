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
        event.registerItem(new AstralMonitorRenderer.Extensions(), TCItems.ASTRAL_MONITOR.get());
        event.registerItem(new PontifexClient.Extensions(), PontifexClient.pieces());
        event.registerItem(new GildedHelmModel.Extensions(), TCItems.GILDED_CRIMSON_HELM.get());
        event.registerItem(new GemRenderer.Extensions(), TCItems.INERT_RIFT_GEM.get(), TCItems.RIFT_GEM.get(),
                TCItems.INERT_TERRA_CAST_GEM.get(), TCItems.TERRA_CAST_GEM.get());
    }

    /** The Shard Mill's body and crank models, drawn by {@link MillRenderer}. */
    @SubscribeEvent
    public static void extraModels(ModelEvent.RegisterAdditional event) {
        MillRenderer.MODELS.forEach(event::register);
        event.register(AstralMonitorRenderer.FRAME);
    }

    /** Phase 5c: the Vessel's pull on the local player runs on this client (see {@link VesselClientPull}). */
    @SubscribeEvent
    public static void vesselSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            dev.movingcastle.thaumicconcilium.entity.Vessel.clientPull = VesselClientPull::pull;
            dev.movingcastle.thaumicconcilium.entity.Vessel.clientTick = VesselFx::tick;
            dev.movingcastle.thaumicconcilium.entity.Vessel.clientEvent = VesselFx::event;
            dev.movingcastle.thaumicconcilium.entity.VesselClot.clientTick = VesselFx::clotTick;
        });
    }

    @SubscribeEvent
    public static void dimensionEffects(net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent event) {
        event.register(ThaumicConcilium.id("causal_bouillon"), new CausalBouillonEffects());
    }

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent event) {
        event.register(BraceletInput.CAST);
        event.register(PontifexClient.TOGGLE);
    }

    @SubscribeEvent
    public static void guiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ThaumicConcilium.id("vessel_vignette"), new VesselVignette());
        event.registerAboveAll(ThaumicConcilium.id("bracelet_hud"), new BraceletHud());
        event.registerAboveAll(ThaumicConcilium.id("bracelet_radial"), new BraceletRadial());
        event.registerAboveAll(ThaumicConcilium.id("lithograph_hud"), new LithographHud());
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
        event.registerEntityRenderer(TCEntities.PRIMAL_ARROW.get(), PrimalArrowRenderer::new);
        event.registerEntityRenderer(TCEntities.CRIMSON_PALADIN.get(), ctx -> new CrimsonCultistRenderer<>(ctx, ThaumicConcilium.id("textures/entity/crimson_paladin.png")));
        event.registerEntityRenderer(TCEntities.CRIMSON_ARCHER.get(), ctx -> new CrimsonCultistRenderer<>(ctx, ThaumicConcilium.id("textures/entity/crimson_archer.png"),
                CrimsonOverlayLayer.ARCHER, ThaumicConcilium.id("textures/entity/overlay/crimson_archer.png")));
        // Original: the Ranger wears Thaumcraft's own cultist texture, which Thaumaturge also ships.
        event.registerEntityRenderer(TCEntities.CRIMSON_RANGER.get(), ctx -> new CrimsonCultistRenderer<>(ctx,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumaturge", "textures/entity/cultist.png"),
                CrimsonOverlayLayer.RANGER, ThaumicConcilium.id("textures/entity/overlay/crimson_ranger.png")));
        event.registerEntityRenderer(TCEntities.LESSER_CRIMSON_PORTAL.get(), LesserPortalRenderer::new);
        // Original: the Pontifex wears Thaumcraft's cultist texture (Thaumaturge ships it); its own look is the overlay cycle's.
        event.registerEntityRenderer(TCEntities.CRIMSON_PONTIFEX.get(), ctx -> new CrimsonCultistRenderer<>(ctx,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumaturge", "textures/entity/cultist.png")));
        // Orb: dark crimson additive sparkle; shackles: a small pale blue one (the original drew a shader orb and no model).
        event.registerEntityRenderer(TCEntities.CRIMSON_ORB.get(), ctx -> new SparkleBillboardRenderer<>(ctx, e -> 0xFFC01030, (e, pt) -> 0.9, true));
        event.registerEntityRenderer(TCEntities.ETHEREAL_SHACKLES.get(), EtherealShacklesRenderer::new);
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
        event.registerEntityRenderer(TCEntities.AURA_CORE.get(), net.minecraft.client.renderer.entity.ItemEntityRenderer::new);
        event.registerEntityRenderer(TCEntities.WARP_CHARGE.get(), WarpChargeRenderer::new);
        event.registerEntityRenderer(TCEntities.THICK_TAINT_BOTTLE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(TCEntities.CLEAR_WATER_BOTTLE.get(), ThrownItemRenderer::new);
        // Original: additive particle-atlas sprite in the shard's aspect colour, half opacity, 0.1 scale.
        event.registerEntityRenderer(TCEntities.SHARD_POWDER.get(), ctx -> new SparkleBillboardRenderer<ShardPowder>(ctx,
                e -> 0x80000000 | e.kind().color(e.level().registryAccess()), (e, pt) -> 0.25, true));
        // Phase 6a: the charged wisp in its aspect's colour; the primal orb cycling the primal colours (original art
        // was Thaumcraft 4's wisp and orb sprites, all rights reserved).
        event.registerEntityRenderer(TCEntities.CHARGED_WISP.get(), ChargedWispRenderer::new);
        event.registerEntityRenderer(TCEntities.BRIGHTEST_ONE.get(), BrightestOneRenderer::new);
        event.registerEntityRenderer(TCEntities.BOSS_EMBER.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        int[] primals = {0xFFFFFF7E, 0xFFFF5A01, 0xFF3CD4FC, 0xFF56C000, 0xFFD5D4EC, 0xFF404040};
        event.registerEntityRenderer(TCEntities.PRIMAL_ORB.get(), ctx -> new SparkleBillboardRenderer<dev.movingcastle.thaumicconcilium.entity.PrimalOrb>(ctx,
                e -> primals[(e.tickCount / 4) % primals.length], (e, pt) -> 0.8, true));
        event.registerBlockEntityRenderer(dev.movingcastle.thaumicconcilium.registry.TCBlocks.HEX_BLOCK_ENTITY.get(), HexRenderer::new);
        // Phase 6b: the Quicksilver Elemental (a biped, original texture) and the reactor's mercury surface.
        event.registerEntityRenderer(TCEntities.QUICKSILVER_ELEMENTAL.get(), ctx -> new ThaumaturgeRenderer<>(ctx, "quicksilver_elemental"));
        event.registerBlockEntityRenderer(dev.movingcastle.thaumicconcilium.registry.TCBlocks.QUICKSILVER_REACTOR_BLOCK_ENTITY.get(),
                QuicksilverReactorRenderer::new);
        event.registerBlockEntityRenderer(dev.movingcastle.thaumicconcilium.registry.TCBlocks.LITHOGRAPHER_BLOCK_ENTITY.get(),
                LithographerRenderer::new);
        event.registerEntityRenderer(TCEntities.MATERIAL_PEELER.get(), MaterialPeelerRenderer::new);
        event.registerEntityRenderer(TCEntities.VESSEL.get(), VesselRenderer::new);
        event.registerEntityRenderer(TCEntities.VESSEL_CLOT.get(), VesselClotRenderer::new);
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

    /** The core layer takes its aspect's colour (original: getColorFromItemStack pass 0); the border stays untinted. */
    @SubscribeEvent
    public static void itemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> {
            var type = dev.movingcastle.thaumicconcilium.item.AuraCore.type(stack);
            if (tint != 0 || type.isBlank()) {
                return 0xFFFFFFFF;
            }
            var level = net.minecraft.client.Minecraft.getInstance().level;
            if (level != null) {
                var holder = com.leclowndu93150.thaumaturge.api.aspect.Aspects.resolve(level, type.primal());
                if (holder != null) {
                    return 0xFF000000 | holder.value().color();
                }
            }
            return type.fallbackColor();
        }, TCItems.AURA_CORE.get());
    }

    /** Selects the blank model (animated blank core art) from the item model's overrides. */
    @SubscribeEvent
    public static void clientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> net.minecraft.client.renderer.item.ItemProperties.register(TCItems.AURA_CORE.get(),
                ThaumicConcilium.id("blank"), (stack, level, entity, seed) ->
                        dev.movingcastle.thaumicconcilium.item.AuraCore.type(stack).isBlank() ? 1.0F : 0.0F));
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(TCModelLayers.THAUMATURGE, TCModelLayers::biped);
        event.registerLayerDefinition(PontifexRobeModel.LAYER, PontifexRobeModelData::create);
        event.registerLayerDefinition(GildedHelmModel.LAYER, GildedHelmModel::create);
        event.registerLayerDefinition(CrimsonOverlayLayer.ARCHER, CrimsonOverlayLayer::archer);
        event.registerLayerDefinition(CrimsonOverlayLayer.RANGER, CrimsonOverlayLayer::ranger);
        event.registerLayerDefinition(TCModelLayers.NETHER_EXPLORER, TCModelLayers::explorer);
        event.registerLayerDefinition(TCModelLayers.WITHERED_BOTANIST, TCModelLayers::botanist);
    }

    @SubscribeEvent
    public static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(dev.movingcastle.thaumicconcilium.registry.TCMenus.THAUMATURGE.get(), ThaumaturgeScreen::new);
        event.register(dev.movingcastle.thaumicconcilium.registry.TCMenus.ASTRAL_MONITOR.get(), SlotScreens.Monitor::new);
        event.register(dev.movingcastle.thaumicconcilium.registry.TCMenus.LITHOGRAPHER.get(), SlotScreens.Lithographer::new);
    }

    /** The robe icons' base layer takes the dye colour. */
    @SubscribeEvent
    public static void pontifexColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register(PontifexClient::iconTint, PontifexClient.pieces());
    }
}
