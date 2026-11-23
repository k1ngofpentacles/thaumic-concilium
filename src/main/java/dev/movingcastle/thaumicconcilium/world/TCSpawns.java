package dev.movingcastle.thaumicconcilium.world;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.mob.Corpse;
import dev.movingcastle.thaumicconcilium.entity.mob.MadThaumaturge;
import dev.movingcastle.thaumicconcilium.entity.mob.ThaumaturgeEntity;
import dev.movingcastle.thaumicconcilium.entity.mob.WitheredBotanist;
import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/**
 * Attributes and spawn rules for the phase 4 mobs. The spawn weights themselves are biome modifiers
 * ({@code data/thaumicconcilium/neoforge/biome_modifier/}), 1:1 with the original's {@code EntityRegistry.addSpawn}:
 * Thaumaturges as creatures (weight 5, 1-3), Mad Thaumaturges as monsters (weight 5, 1-3), Withered Botanists as
 * creatures (weight 5, alone, only with Botania); the last two in every biome, as in the original.
 * <p>
 * Agreed with the user (2026-11-21): Thaumaturges should be a welcome, uncommon sight, not a constant presence. Their
 * creature spawn only takes when the chunk of an overworld surface structure in
 * {@code #thaumicconcilium:thaumaturge_haunts} (villages, temples, huts, mounds...) is generated, and those stay put;
 * the periodic creature respawns never bring them. Wanderers come on top ({@link ThaumaturgeWanderers}).
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class TCSpawns {
    public static final TagKey<Structure> HAUNTS = TagKey.create(Registries.STRUCTURE, ThaumicConcilium.id("thaumaturge_haunts"));
    /** Chunks around the spawn position searched for a haunt's pieces (2 = a 5x5 chunk square). */
    private static final int HAUNT_RADIUS = 2;

    private TCSpawns() {
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(TCEntities.THAUMATURGE.get(), ThaumaturgeEntity.createAttributes().build());
        event.put(TCEntities.MAD_THAUMATURGE.get(), MadThaumaturge.createAttributes().build());
        event.put(TCEntities.QUICKSILVER_ELEMENTAL.get(),
                dev.movingcastle.thaumicconcilium.entity.mob.QuicksilverElemental.createAttributes().build());
        event.put(TCEntities.BRIGHTEST_ONE.get(), dev.movingcastle.thaumicconcilium.entity.mob.BrightestOne.createAttributes().build());
        event.put(TCEntities.CRIMSON_PALADIN.get(), dev.movingcastle.thaumicconcilium.entity.mob.CrimsonPaladin.createAttributes().build());
        event.put(TCEntities.CRIMSON_ARCHER.get(), dev.movingcastle.thaumicconcilium.entity.mob.CrimsonArcher.createAttributes().build());
        event.put(TCEntities.CRIMSON_RANGER.get(), dev.movingcastle.thaumicconcilium.entity.mob.CrimsonRanger.createAttributes().build());
        event.put(TCEntities.LESSER_CRIMSON_PORTAL.get(), dev.movingcastle.thaumicconcilium.entity.mob.LesserCrimsonPortal.createAttributes().build());
        event.put(TCEntities.CRIMSON_PONTIFEX.get(), dev.movingcastle.thaumicconcilium.entity.mob.CrimsonPontifex.createAttributes().build());
        event.put(TCEntities.NETHER_EXPLORER.get(), Corpse.createAttributes().build());
        event.put(TCEntities.GOLEM_BYDLO.get(), dev.movingcastle.thaumicconcilium.entity.GolemBydlo.createAttributes().build());
        event.put(TCEntities.VENGEFUL_GOLEM.get(), dev.movingcastle.thaumicconcilium.entity.mob.VengefulGolem.createAttributes().build());
        if (TCEntities.hasBotanist()) {
            event.put(TCEntities.WITHERED_BOTANIST.get(), Corpse.createAttributes().build());
        }
    }

    @SubscribeEvent
    public static void placements(RegisterSpawnPlacementsEvent event) {
        // Original: getCanSpawnHere is always true (no light check), so only the ground and, here, the haunt.
        event.register(TCEntities.THAUMATURGE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                TCSpawns::thaumaturgeRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(TCEntities.MAD_THAUMATURGE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // Original: EntityMob's rule (dark enough, not peaceful), here also behind the config's chance.
        event.register(TCEntities.QUICKSILVER_ELEMENTAL.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> (reason != MobSpawnType.NATURAL
                        || random.nextInt(100) < dev.movingcastle.thaumicconcilium.TCConfig.QUICKSILVER_ELEMENTAL_SPAWN.get())
                        && Monster.checkMonsterSpawnRules(type, level, reason, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // Original: the Ranger spawns by the monster rules, weight 5, groups of 1-2 in every biome (biome modifier).
        event.register(TCEntities.CRIMSON_RANGER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // Original: EntityMob's rule; the biome blacklist and weight are the ConfiguredSpawns biome modifier.
        event.register(TCEntities.VENGEFUL_GOLEM.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(TCEntities.NETHER_EXPLORER.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
                (type, level, reason, pos, random) -> true, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        if (TCEntities.hasBotanist()) {
            event.register(TCEntities.WITHERED_BOTANIST.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    TCSpawns::botanistRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
    }

    private static boolean thaumaturgeRules(EntityType<ThaumaturgeEntity> type, ServerLevelAccessor level, MobSpawnType reason,
                                            BlockPos pos, RandomSource random) {
        if (!Mob.checkMobSpawnRules(type, level, reason, pos, random)) {
            return false;
        }
        // Only when a haunt's chunk is generated (they then stay, see ThaumaturgeEntity.finalizeSpawn), never the
        // periodic respawns that would keep a village stocked with them.
        return switch (reason) {
            case NATURAL -> false;
            case CHUNK_GENERATION -> nearHaunt(level, pos);
            default -> true;
        };
    }

    /** Original: overworld only, and never within 32 blocks of another botanist. */
    private static boolean botanistRules(EntityType<WitheredBotanist> type, ServerLevelAccessor level, MobSpawnType reason,
                                         BlockPos pos, RandomSource random) {
        return Mob.checkMobSpawnRules(type, level, reason, pos, random)
                && level.getLevel().dimension() == Level.OVERWORLD
                && level.getEntitiesOfClass(WitheredBotanist.class, new net.minecraft.world.phys.AABB(pos).inflate(32.0)).isEmpty();
    }

    /**
     * Whether a piece of a haunt structure lies in the chunks around {@code pos}. Never loads or generates chunks. During
     * chunk generation only the chunk being populated can be read (asking a world-gen region for any other crashes), so
     * there the structure must reach into that chunk.
     */
    public static boolean nearHaunt(ServerLevelAccessor level, BlockPos pos) {
        Registry<Structure> structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        int cx = SectionPos.blockToSectionCoord(pos.getX());
        int cz = SectionPos.blockToSectionCoord(pos.getZ());
        int radius = level instanceof net.minecraft.server.level.WorldGenRegion ? 0 : HAUNT_RADIUS;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkAccess chunk = radius == 0 ? level.getChunk(cx, cz)
                        : level.getChunk(cx + dx, cz + dz, ChunkStatus.STRUCTURE_REFERENCES, false);
                if (chunk == null) {
                    continue;
                }
                for (Map.Entry<Structure, it.unimi.dsi.fastutil.longs.LongSet> e : chunk.getAllReferences().entrySet()) {
                    if (!e.getValue().isEmpty() && structures.wrapAsHolder(e.getKey()).is(HAUNTS)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
