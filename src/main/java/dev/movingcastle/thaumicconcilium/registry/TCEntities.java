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

    public static boolean hasBotanist() {
        return WITHERED_BOTANIST != null;
    }

    // ---------------------------------------------------------------- phase 6c
    /** An item entity (same size and tracking as the vanilla one) that attunes a blank Aura Core. */
    public static final DeferredHolder<EntityType<?>, EntityType<dev.movingcastle.thaumicconcilium.entity.AuraCoreEntity>> AURA_CORE =
            ENTITY_TYPES.register("aura_core", () -> EntityType.Builder.<dev.movingcastle.thaumicconcilium.entity.AuraCoreEntity>of(
                    dev.movingcastle.thaumicconcilium.entity.AuraCoreEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).eyeHeight(0.2125F).clientTrackingRange(6).updateInterval(20).build("aura_core"));

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
