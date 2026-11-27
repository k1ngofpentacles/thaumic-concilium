package dev.movingcastle.thaumicconcilium.compat.witchery;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.ChortSpit;
import dev.movingcastle.thaumicconcilium.entity.mob.Chort;
import dev.movingcastle.thaumicconcilium.entity.mob.SloppyAlchemist;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Phase 10-1: the Chort, the Sloppy Alchemist and the Chort's spit. Their own registers, put on the mod bus only when
 * {@code witchery} is loaded ({@link #init}); callers check {@link WitcheryGate#loaded()} before touching this class.
 */
public final class NetherPair {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, ThaumicConcilium.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ThaumicConcilium.MODID);

    /** Original: default mob size, fire-immune (field_70178_ae is isImmuneToFire), tracked at 64. */
    public static final DeferredHolder<EntityType<?>, EntityType<Chort>> CHORT = ENTITY_TYPES.register("chort",
            () -> EntityType.Builder.<Chort>of(Chort::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).fireImmune().clientTrackingRange(8).build("chort"));

    /** Original: 0.5 x 1.8 alone (the size follows the rider, see {@link SloppyAlchemist}). */
    public static final DeferredHolder<EntityType<?>, EntityType<SloppyAlchemist>> SLOPPY_ALCHEMIST = ENTITY_TYPES.register("sloppy_alchemist",
            () -> EntityType.Builder.<SloppyAlchemist>of(SloppyAlchemist::new, MobCategory.MONSTER)
                    .sized(0.5F, 1.8F).fireImmune().clientTrackingRange(8).build("sloppy_alchemist"));

    public static final DeferredHolder<EntityType<?>, EntityType<ChortSpit>> CHORT_SPIT = ENTITY_TYPES.register("chort_spit",
            () -> EntityType.Builder.<ChortSpit>of(ChortSpit::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build("chort_spit"));

    // Original egg colours: 65535 / 139 for both.
    public static final DeferredItem<DeferredSpawnEggItem> CHORT_SPAWN_EGG = ITEMS.register("chort_spawn_egg",
            () -> new DeferredSpawnEggItem(CHORT, 0x00FFFF, 0x00008B, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> SLOPPY_ALCHEMIST_SPAWN_EGG = ITEMS.register("sloppy_alchemist_spawn_egg",
            () -> new DeferredSpawnEggItem(SLOPPY_ALCHEMIST, 0x00FFFF, 0x00008B, new Item.Properties()));

    private NetherPair() {
    }

    public static void init(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        ITEMS.register(modBus);
        ImpPact.ATTACHMENTS.register(modBus);
        ImpPact.registerListeners();
    }

    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(CHORT.get(), Chort.createAttributes().build());
        event.put(SLOPPY_ALCHEMIST.get(), SloppyAlchemist.createAttributes().build());
    }

    public static void placements(RegisterSpawnPlacementsEvent event) {
        // Original: EntityMob's rule (the Nether's dimension type lets any block light through). The Chort never spawns alone.
        event.register(SLOPPY_ALCHEMIST.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(CHORT.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
