package dev.movingcastle.thaumicconcilium.registry;

import dev.movingcastle.thaumicconcilium.entity.ClearWaterBottle;
import dev.movingcastle.thaumicconcilium.entity.ShardPowder;
import dev.movingcastle.thaumicconcilium.entity.ThickTaintBottle;

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

    private TCEntities() {
    }
}
