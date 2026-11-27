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
import dev.movingcastle.thaumicconcilium.client.mob.VengefulGolemModel;
import dev.movingcastle.thaumicconcilium.client.mob.VengefulGolemRenderer;
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
        event.registerItem(new VerdantCapitalRenderer.Extensions(), TCItems.VERDANT_CAPITAL.get()); // phase 5d cycle 4b: the closed star
        event.registerItem(new VoidItemRenderer.Extensions(), TCItems.PROTOBODY.get(), TCItems.SOLID_VOID.get()); // phase 8-4
    }

    /** Phase 5d cycle 3: the mana pond fluid, drawn by the standard fluid path (see {@link ManaPondClient}). */
    @SubscribeEvent
    public static void fluidExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new ManaPondClient(), dev.movingcastle.thaumicconcilium.registry.TCFluids.MANA_POND_TYPE.get());
    }

    @SubscribeEvent
    public static void fluidRenderTypes(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(dev.movingcastle.thaumicconcilium.registry.TCFluids.MANA_POND.get(),
                    net.minecraft.client.renderer.RenderType.translucent());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(dev.movingcastle.thaumicconcilium.registry.TCFluids.MANA_POND_FLOWING.get(),
                    net.minecraft.client.renderer.RenderType.translucent());
        });
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
            // Phase 5d cycle 4b: the capital's crackle list, the pond's sparkle, the particles of the seed's death
            dev.movingcastle.thaumicconcilium.block.VerdantCapitalBlockEntity.clientState = CapitalCrackles::update;
            dev.movingcastle.thaumicconcilium.block.ManaPondBlock.clientGlint = GlintParticle::pond;
            dev.movingcastle.thaumicconcilium.entity.GroveBind.clientTick = GroveBindRenderer::fx;
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
        event.registerAboveAll(ThaumicConcilium.id("vessel_flash"), new VesselFlashClient());
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
                renderer.addLayer(new GuardFx.Layer(renderer));   // the armed Riposte on the off-hand shield
            }
        }
    }

    private static final net.minecraft.resources.ResourceLocation HOST_CULTIST_SKIN =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumaturge", "textures/entity/cultist.png");

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TCEntities.PRIMAL_ARROW.get(), PrimalArrowRenderer::new);
        event.registerEntityRenderer(TCEntities.CRIMSON_PALADIN.get(), ctx -> new CrimsonCultistRenderer<>(ctx, ThaumicConcilium.id("textures/entity/crimson_paladin.png")));
        event.registerEntityRenderer(TCEntities.CRIMSON_ARCHER.get(), ctx -> new CrimsonCultistRenderer<>(ctx, HOST_CULTIST_SKIN,
                CrimsonOverlayLayer.ARCHER, ThaumicConcilium.id("textures/entity/overlay/crimson_archer.png")));
        // 0.16.0: the Ranger and Archer take the host cultist skin by reference; their armour comes from data (crimson_outfits).
        event.registerEntityRenderer(TCEntities.CRIMSON_RANGER.get(), ctx -> new CrimsonCultistRenderer<>(ctx, HOST_CULTIST_SKIN));
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
        // Phase 5d cycle 4b: the Verdant Capital's star and ray, the grove's hold on a dying Taint Seed.
        event.registerBlockEntityRenderer(dev.movingcastle.thaumicconcilium.registry.TCBlocks.VERDANT_CAPITAL_BLOCK_ENTITY.get(), VerdantCapitalRenderer::new);
        event.registerEntityRenderer(TCEntities.GROVE_BIND.get(), GroveBindRenderer::new);
        // Phase 4 mobs.
        event.registerEntityRenderer(TCEntities.THAUMATURGE.get(), ctx -> new ThaumaturgeRenderer<>(ctx, "thaumaturge"));
        event.registerEntityRenderer(TCEntities.MAD_THAUMATURGE.get(), ctx -> new ThaumaturgeRenderer<>(ctx, "mad_thaumaturge"));
        event.registerEntityRenderer(TCEntities.NETHER_EXPLORER.get(),
                ctx -> new CorpseRenderer<>(ctx, TCModelLayers.NETHER_EXPLORER, "nether_explorer"));
        event.registerEntityRenderer(TCEntities.VENGEFUL_GOLEM.get(), VengefulGolemRenderer::new);
        event.registerEntityRenderer(TCEntities.GOLEM_BYDLO.get(), dev.movingcastle.thaumicconcilium.client.mob.GolemBydloRenderer::new);
        // Phase 8-4: the dropped Protobody is an ordinary item entity drawn by its builtin/entity model, its limbs and the Solid Void.
        event.registerEntityRenderer(TCEntities.PROTOBODY_ITEM.get(), net.minecraft.client.renderer.entity.ItemEntityRenderer::new);
        event.registerEntityRenderer(TCEntities.PROTOLIMB.get(), ProtolimbRenderer::new);
        event.registerBlockEntityRenderer(dev.movingcastle.thaumicconcilium.registry.TCBlocks.SOLID_VOID_BLOCK_ENTITY.get(), SolidVoidRenderer::new);
        if (dev.movingcastle.thaumicconcilium.compat.witchery.WitcheryGate.loaded()) {
            // 10-1b: the original models (docs/design/phase10-look-prebrief.md).
            event.registerEntityRenderer(dev.movingcastle.thaumicconcilium.compat.witchery.NetherPair.CHORT.get(),
                    dev.movingcastle.thaumicconcilium.client.mob.ChortRenderer::new);
            event.registerEntityRenderer(dev.movingcastle.thaumicconcilium.compat.witchery.NetherPair.SLOPPY_ALCHEMIST.get(),
                    dev.movingcastle.thaumicconcilium.client.mob.SloppyAlchemistRenderer::new);
            event.registerEntityRenderer(dev.movingcastle.thaumicconcilium.compat.witchery.NetherPair.BURNED_WITCH.get(),
                    dev.movingcastle.thaumicconcilium.client.mob.BurnedWitchRenderer::new);
            event.registerEntityRenderer(dev.movingcastle.thaumicconcilium.compat.witchery.NetherPair.CHORT_SPIT.get(),
                    net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        }
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

    /** Item tints. */
    @SubscribeEvent
    public static void itemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        // The Nether Membrane is the vanilla phantom membrane tinted nether pink (referenced art, no copy).
        event.register((stack, tint) -> tint == 0 ? 0xFFC8405A : 0xFFFFFFFF, TCItems.NETHER_MEMBRANE.get());
        // Crimson Plating is the host's iron plate tinted cult crimson (referenced art, no copy).
        event.register((stack, tint) -> tint == 0 ? 0xFFB83040 : 0xFFFFFFFF, TCItems.CRIMSON_PLATING.get());
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(TCModelLayers.THAUMATURGE, TCModelLayers::biped);
        event.registerLayerDefinition(dev.movingcastle.thaumicconcilium.client.mob.GolemBydloModel.LAYER, dev.movingcastle.thaumicconcilium.client.mob.GolemBydloModel::create);
        event.registerLayerDefinition(PontifexRobeModel.LAYER, PontifexRobeModelData::create);
        event.registerLayerDefinition(GildedHelmModel.LAYER, GildedHelmModel::create);
        event.registerLayerDefinition(CrimsonOverlayLayer.ARCHER, CrimsonOverlayLayer::archer);
        event.registerLayerDefinition(TCModelLayers.NETHER_EXPLORER, TCModelLayers::explorer);
        event.registerLayerDefinition(TCModelLayers.WITHERED_BOTANIST, TCModelLayers::botanist);
        event.registerLayerDefinition(TCModelLayers.VENGEFUL_GOLEM, VengefulGolemModel::body);
        event.registerLayerDefinition(TCModelLayers.VENGEFUL_DECORATION, TCModelLayers::vengefulDecoration);
        if (dev.movingcastle.thaumicconcilium.compat.witchery.WitcheryGate.loaded()) {
            event.registerLayerDefinition(TCModelLayers.CHORT, dev.movingcastle.thaumicconcilium.client.mob.ChortModel::create);
            event.registerLayerDefinition(TCModelLayers.BURNED_WITCH, TCModelLayers::biped);
            event.registerLayerDefinition(TCModelLayers.SLOPPY_ALCHEMIST, TCModelLayers::inflatedBiped);
            event.registerLayerDefinition(TCModelLayers.SLOPPY_ALCHEMIST_RIDDEN,
                    dev.movingcastle.thaumicconcilium.client.mob.RiddenAlchemistModel::create);
        }
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
