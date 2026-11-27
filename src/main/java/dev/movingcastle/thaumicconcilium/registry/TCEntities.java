package dev.movingcastle.thaumicconcilium.registry;

import dev.movingcastle.thaumicconcilium.entity.ClearWaterBottle;
import dev.movingcastle.thaumicconcilium.entity.ShardPowder;
import dev.movingcastle.thaumicconcilium.entity.ThickTaintBottle;
import dev.movingcastle.thaumicconcilium.entity.mob.MadThaumaturge;
import dev.movingcastle.thaumicconcilium.entity.mob.NetherExplorer;
import dev.movingcastle.thaumicconcilium.entity.mob.ThaumaturgeEntity;
import dev.movingcastle.thaumicconcilium.entity.mob.WitheredBotanist;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.CompressedBlastEntity;
import dev.movingcastle.thaumicconcilium.entity.ConcentratedWarpCharge;
import dev.movingcastle.thaumicconcilium.entity.FlurryShot;
import dev.movingcastle.thaumicconcilium.entity.PositiveBurstOrb;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TCEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, ThaumicConcilium.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<CompressedBlastEntity>> COMPRESSED_BLAST = ENTITY_TYPES.register("compressed_blast",
            () -> EntityType.Builder.<CompressedBlastEntity>of(CompressedBlastEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(8).updateInterval(1).noSave().build("compressed_blast"));

    public static final DeferredHolder<EntityType<?>, EntityType<PositiveBurstOrb>> POSITIVE_BURST_ORB = ENTITY_TYPES.register("positive_burst_orb",
            () -> EntityType.Builder.<PositiveBurstOrb>of(PositiveBurstOrb::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(6).updateInterval(2).build("positive_burst_orb"));

    public static final DeferredHolder<EntityType<?>, EntityType<dev.movingcastle.thaumicconcilium.entity.mob.CrimsonPaladin>> CRIMSON_PALADIN = ENTITY_TYPES.register("crimson_paladin",
            () -> EntityType.Builder.<dev.movingcastle.thaumicconcilium.entity.mob.CrimsonPaladin>of(dev.movingcastle.thaumicconcilium.entity.mob.CrimsonPaladin::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).eyeHeight(1.62F).clientTrackingRange(8).updateInterval(3).build("crimson_paladin"));

    public static final DeferredHolder<EntityType<?>, EntityType<dev.movingcastle.thaumicconcilium.entity.mob.CrimsonArcher>> CRIMSON_ARCHER = ENTITY_TYPES.register("crimson_archer",
            () -> EntityType.Builder.<dev.movingcastle.thaumicconcilium.entity.mob.CrimsonArcher>of(dev.movingcastle.thaumicconcilium.entity.mob.CrimsonArcher::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).eyeHeight(1.62F).clientTrackingRange(8).updateInterval(3).build("crimson_archer"));

    public static final DeferredHolder<EntityType<?>, EntityType<dev.movingcastle.thaumicconcilium.entity.mob.CrimsonRanger>> CRIMSON_RANGER = ENTITY_TYPES.register("crimson_ranger",
            () -> EntityType.Builder.<dev.movingcastle.thaumicconcilium.entity.mob.CrimsonRanger>of(dev.movingcastle.thaumicconcilium.entity.mob.CrimsonRanger::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).eyeHeight(1.62F).clientTrackingRange(8).updateInterval(3).build("crimson_ranger"));

    /** Original: 1.5 x 3.0. Sync range 64 blocks. */
    public static final DeferredHolder<EntityType<?>, EntityType<dev.movingcastle.thaumicconcilium.entity.mob.LesserCrimsonPortal>> LESSER_CRIMSON_PORTAL = ENTITY_TYPES.register("lesser_crimson_portal",
            () -> EntityType.Builder.<dev.movingcastle.thaumicconcilium.entity.mob.LesserCrimsonPortal>of(dev.movingcastle.thaumicconcilium.entity.mob.LesserCrimsonPortal::new, MobCategory.MONSTER)
                    .sized(1.5F, 3.0F).clientTrackingRange(4).updateInterval(20).fireImmune().build("lesser_crimson_portal"));

    /** Original: 0.75 x 2.25. */
    public static final DeferredHolder<EntityType<?>, EntityType<dev.movingcastle.thaumicconcilium.entity.mob.CrimsonPontifex>> CRIMSON_PONTIFEX = ENTITY_TYPES.register("crimson_pontifex",
            () -> EntityType.Builder.<dev.movingcastle.thaumicconcilium.entity.mob.CrimsonPontifex>of(dev.movingcastle.thaumicconcilium.entity.mob.CrimsonPontifex::new, MobCategory.MONSTER)
                    .sized(0.75F, 2.25F).eyeHeight(1.62F).clientTrackingRange(10).updateInterval(3).build("crimson_pontifex"));

    public static final DeferredHolder<EntityType<?>, EntityType<dev.movingcastle.thaumicconcilium.entity.CrimsonOrb>> CRIMSON_ORB = ENTITY_TYPES.register("crimson_orb",
            () -> EntityType.Builder.<dev.movingcastle.thaumicconcilium.entity.CrimsonOrb>of(dev.movingcastle.thaumicconcilium.entity.CrimsonOrb::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(1).fireImmune().build("crimson_orb"));

    public static final DeferredHolder<EntityType<?>, EntityType<dev.movingcastle.thaumicconcilium.entity.EtherealShackles>> ETHEREAL_SHACKLES = ENTITY_TYPES.register("ethereal_shackles",
            () -> EntityType.Builder.<dev.movingcastle.thaumicconcilium.entity.EtherealShackles>of(dev.movingcastle.thaumicconcilium.entity.EtherealShackles::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(1).build("ethereal_shackles"));

    public static final DeferredHolder<EntityType<?>, EntityType<dev.movingcastle.thaumicconcilium.entity.PrimalArrow>> PRIMAL_ARROW = ENTITY_TYPES.register("primal_arrow",
            () -> EntityType.Builder.<dev.movingcastle.thaumicconcilium.entity.PrimalArrow>of(dev.movingcastle.thaumicconcilium.entity.PrimalArrow::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).eyeHeight(0.13F).clientTrackingRange(4).updateInterval(20).build("primal_arrow"));

    public static final DeferredHolder<EntityType<?>, EntityType<FlurryShot>> FLURRY_SHOT = ENTITY_TYPES.register("flurry_shot",
            () -> EntityType.Builder.<FlurryShot>of(FlurryShot::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).noSave().build("flurry_shot"));

    public static final DeferredHolder<EntityType<?>, EntityType<ConcentratedWarpCharge>> WARP_CHARGE = ENTITY_TYPES.register("concentrated_warp_charge",
            () -> EntityType.Builder.<ConcentratedWarpCharge>of(ConcentratedWarpCharge::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F).clientTrackingRange(8).updateInterval(1).build("concentrated_warp_charge"));

    public static final DeferredHolder<EntityType<?>, EntityType<ThickTaintBottle>> THICK_TAINT_BOTTLE = ENTITY_TYPES.register("bottle_of_thick_taint",
            () -> EntityType.Builder.<ThickTaintBottle>of(ThickTaintBottle::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build("bottle_of_thick_taint"));

    public static final DeferredHolder<EntityType<?>, EntityType<ClearWaterBottle>> CLEAR_WATER_BOTTLE = ENTITY_TYPES.register("bottle_of_clear_water",
            () -> EntityType.Builder.<ClearWaterBottle>of(ClearWaterBottle::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build("bottle_of_clear_water"));

    public static final DeferredHolder<EntityType<?>, EntityType<ShardPowder>> SHARD_POWDER = ENTITY_TYPES.register("shard_powder",
            () -> EntityType.Builder.<ShardPowder>of(ShardPowder::new, MobCategory.MISC)
                    .sized(0.125F, 0.125F).clientTrackingRange(4).updateInterval(2).noSave().build("shard_powder"));

    // ---------------------------------------------------------------- phase 6a
    public static final DeferredHolder<EntityType<?>, EntityType<dev.movingcastle.thaumicconcilium.entity.ChargedWisp>> CHARGED_WISP =
            ENTITY_TYPES.register("charged_wisp", () -> EntityType.Builder.<dev.movingcastle.thaumicconcilium.entity.ChargedWisp>of(
                    dev.movingcastle.thaumicconcilium.entity.ChargedWisp::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(8).updateInterval(1).build("charged_wisp"));

    public static final DeferredHolder<EntityType<?>, EntityType<dev.movingcastle.thaumicconcilium.entity.PrimalOrb>> PRIMAL_ORB =
            ENTITY_TYPES.register("primal_orb", () -> EntityType.Builder.<dev.movingcastle.thaumicconcilium.entity.PrimalOrb>of(
                    dev.movingcastle.thaumicconcilium.entity.PrimalOrb::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(1).build("primal_orb"));

    // ---------------------------------------------------------------- phase 6b
    /** Original: 0.6 x 1.8 at full health (the scale attribute shrinks it with health). */
    public static final DeferredHolder<EntityType<?>, EntityType<dev.movingcastle.thaumicconcilium.entity.mob.QuicksilverElemental>> QUICKSILVER_ELEMENTAL =
            ENTITY_TYPES.register("quicksilver_elemental", () -> EntityType.Builder.<dev.movingcastle.thaumicconcilium.entity.mob.QuicksilverElemental>of(
                    dev.movingcastle.thaumicconcilium.entity.mob.QuicksilverElemental::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).clientTrackingRange(8).build("quicksilver_elemental"));

    // ---------------------------------------------------------------- phase 5c
    /** The dormant Vessel (placeholder): 3 x 3, never moves, saved. */
    public static final DeferredHolder<EntityType<?>, EntityType<dev.movingcastle.thaumicconcilium.entity.Vessel>> VESSEL =
            ENTITY_TYPES.register("vessel", () -> EntityType.Builder.<dev.movingcastle.thaumicconcilium.entity.Vessel>of(
                    dev.movingcastle.thaumicconcilium.entity.Vessel::new, MobCategory.MISC)
                    .sized(3.0F, 3.0F).clientTrackingRange(10).updateInterval(20).fireImmune().build("vessel"));

    /** A clot the Vessel throws at a tethered player: not saved, ticks every tick, small and hittable. */
    /** Phase 5d cycle 4b: the grove's roots and leaves round a dying Taint Seed; lives for the twelve seconds of that sequence. */
    public static final DeferredHolder<EntityType<?>, EntityType<dev.movingcastle.thaumicconcilium.entity.GroveBind>> GROVE_BIND =
            ENTITY_TYPES.register("grove_bind", () -> EntityType.Builder.<dev.movingcastle.thaumicconcilium.entity.GroveBind>of(
                    dev.movingcastle.thaumicconcilium.entity.GroveBind::new, MobCategory.MISC)
                    .sized(3.0F, 3.0F).clientTrackingRange(8).updateInterval(20).fireImmune().noSummon().build("grove_bind"));

    public static final DeferredHolder<EntityType<?>, EntityType<dev.movingcastle.thaumicconcilium.entity.VesselClot>> VESSEL_CLOT =
            ENTITY_TYPES.register("vessel_clot", () -> EntityType.Builder.<dev.movingcastle.thaumicconcilium.entity.VesselClot>of(
                    dev.movingcastle.thaumicconcilium.entity.VesselClot::new, MobCategory.MISC)
                    .sized(0.8F, 0.8F).clientTrackingRange(8).updateInterval(1).fireImmune().noSave().build("vessel_clot"));

    // ---------------------------------------------------------------- phase 5b
    /** Original: 1 x 1, fire-immune, tracked at 64 blocks, updates every 3 ticks. */
    public static final DeferredHolder<EntityType<?>, EntityType<dev.movingcastle.thaumicconcilium.entity.MaterialPeeler>> MATERIAL_PEELER =
            ENTITY_TYPES.register("material_peeler", () -> EntityType.Builder.<dev.movingcastle.thaumicconcilium.entity.MaterialPeeler>of(
                    dev.movingcastle.thaumicconcilium.entity.MaterialPeeler::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F).clientTrackingRange(4).updateInterval(3).fireImmune().build("material_peeler"));

    // ---------------------------------------------------------------- phase 4 mobs
    public static final DeferredHolder<EntityType<?>, EntityType<ThaumaturgeEntity>> THAUMATURGE = ENTITY_TYPES.register("thaumaturge",
            () -> EntityType.Builder.<ThaumaturgeEntity>of(ThaumaturgeEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(8).build("thaumaturge"));

    public static final DeferredHolder<EntityType<?>, EntityType<MadThaumaturge>> MAD_THAUMATURGE = ENTITY_TYPES.register("mad_thaumaturge",
            () -> EntityType.Builder.<MadThaumaturge>of(MadThaumaturge::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(8).build("mad_thaumaturge"));

    /** Original: 2 x 0.3, fire-immune. */
    public static final DeferredHolder<EntityType<?>, EntityType<NetherExplorer>> NETHER_EXPLORER = ENTITY_TYPES.register("nether_explorer",
            () -> EntityType.Builder.<NetherExplorer>of(NetherExplorer::new, MobCategory.MISC)
                    .sized(2.0F, 0.3F).eyeHeight(0.15F).fireImmune().clientTrackingRange(8).build("nether_explorer"));

    /** Registered only with Botania, as in the original. */
    public static final DeferredHolder<EntityType<?>, EntityType<WitheredBotanist>> WITHERED_BOTANIST =
            net.neoforged.fml.ModList.get().isLoaded("botania") ? ENTITY_TYPES.register("withered_botanist",
                    () -> EntityType.Builder.<WitheredBotanist>of(WitheredBotanist::new, MobCategory.CREATURE)
                            .sized(2.0F, 0.3F).eyeHeight(0.15F).clientTrackingRange(8).build("withered_botanist")) : null;

    // ---------------------------------------------------------------- phase 7
    /** Original: a biped (0.6 x 1.8), registered with Automagy; here always. */
    public static final DeferredHolder<EntityType<?>, EntityType<dev.movingcastle.thaumicconcilium.entity.mob.VengefulGolem>> VENGEFUL_GOLEM =
            ENTITY_TYPES.register("vengeful_golem", () -> EntityType.Builder.<dev.movingcastle.thaumicconcilium.entity.mob.VengefulGolem>of(
                    dev.movingcastle.thaumicconcilium.entity.mob.VengefulGolem::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).clientTrackingRange(8).build("vengeful_golem"));

    /** Original: {@code setSize(2.0, 3.5)}, an iron golem (a vanilla {@code EntityGolem}); here an {@code AbstractGolem}. */
    public static final DeferredHolder<EntityType<?>, EntityType<dev.movingcastle.thaumicconcilium.entity.GolemBydlo>> GOLEM_BYDLO =
            ENTITY_TYPES.register("golem_bydlo", () -> EntityType.Builder.<dev.movingcastle.thaumicconcilium.entity.GolemBydlo>of(
                    dev.movingcastle.thaumicconcilium.entity.GolemBydlo::new, MobCategory.MISC)
                    .sized(2.0F, 3.5F).clientTrackingRange(10).build("golem_bydlo"));

    // ---------------------------------------------------------------- phase 8-4
    /** The dropped Protobody: an item entity of the vanilla size and tracking. */
    public static final DeferredHolder<EntityType<?>, EntityType<dev.movingcastle.thaumicconcilium.entity.ProtobodyItemEntity>> PROTOBODY_ITEM =
            ENTITY_TYPES.register("protobody_item", () -> EntityType.Builder.<dev.movingcastle.thaumicconcilium.entity.ProtobodyItemEntity>of(
                    dev.movingcastle.thaumicconcilium.entity.ProtobodyItemEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).eyeHeight(0.2125F).clientTrackingRange(6).updateInterval(20).build("protobody_item"));

    /** Original: 0.6 x 0.5, fire-immune, a flying mob that was not hostile ({@code MISC}, no {@code Enemy}). */
    public static final DeferredHolder<EntityType<?>, EntityType<dev.movingcastle.thaumicconcilium.entity.mob.Protolimb>> PROTOLIMB =
            ENTITY_TYPES.register("protolimb", () -> EntityType.Builder.<dev.movingcastle.thaumicconcilium.entity.mob.Protolimb>of(
                    dev.movingcastle.thaumicconcilium.entity.mob.Protolimb::new, MobCategory.MISC)
                    .sized(0.6F, 0.5F).fireImmune().clientTrackingRange(8).updateInterval(1).build("protolimb"));

    public static boolean hasBotanist() {
        return WITHERED_BOTANIST != null;
    }

    /** Original: 3 x 3, flying, fire-immune. */
    public static final DeferredHolder<EntityType<?>, EntityType<dev.movingcastle.thaumicconcilium.entity.mob.BrightestOne>> BRIGHTEST_ONE =
            ENTITY_TYPES.register("brightest_one", () -> EntityType.Builder.<dev.movingcastle.thaumicconcilium.entity.mob.BrightestOne>of(
                    dev.movingcastle.thaumicconcilium.entity.mob.BrightestOne::new, MobCategory.MONSTER)
                    .sized(3.0F, 3.0F).fireImmune().clientTrackingRange(10).build("brightest_one"));

    public static final DeferredHolder<EntityType<?>, EntityType<dev.movingcastle.thaumicconcilium.entity.BossEmber>> BOSS_EMBER =
            ENTITY_TYPES.register("boss_ember", () -> EntityType.Builder.<dev.movingcastle.thaumicconcilium.entity.BossEmber>of(
                    dev.movingcastle.thaumicconcilium.entity.BossEmber::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(6).updateInterval(1).noSave().build("boss_ember"));

    private TCEntities() {
    }
}
