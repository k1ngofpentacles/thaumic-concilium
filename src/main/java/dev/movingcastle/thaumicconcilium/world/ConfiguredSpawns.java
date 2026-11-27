package dev.movingcastle.thaumicconcilium.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Like {@code neoforge:add_spawns} (same {@code biomes} and {@code spawners} fields), but the weight of a spawner whose type has a
 * config entry comes from the common config and biomes on that entry's blacklist are skipped (the original's
 * {@code vengefulGolemSpawnChance} and {@code vengefulGolemBiomeBlacklist}). Spawners of other types keep their JSON weight.
 * The modifier runs once as the server starts, so there is no per-spawn cost.
 */
public record ConfiguredSpawns(HolderSet<Biome> biomes, List<SpawnerData> spawners) implements BiomeModifier {
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, ThaumicConcilium.MODID);

    public static final MapCodec<ConfiguredSpawns> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(ConfiguredSpawns::biomes),
            Codec.list(SpawnerData.CODEC).fieldOf("spawners").forGetter(ConfiguredSpawns::spawners)
    ).apply(i, ConfiguredSpawns::new));

    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<ConfiguredSpawns>> TYPE =
            SERIALIZERS.register("configured_spawns", () -> CODEC);

    /** The config's weight for a type, or -1 when the type has none (the spawner keeps its JSON weight). */
    private static int configuredWeight(EntityType<?> type) {
        if (type == TCEntities.VENGEFUL_GOLEM.get()) {
            return TCConfig.vengefulGolemSpawnWeight();
        }
        if (isAlchemist(type)) {
            return TCConfig.sloppyAlchemistSpawnWeight();
        }
        return -1;
    }

    /** The Sloppy Alchemist exists only with Witchery, so it is matched by id (this class must load without it). */
    private static boolean isAlchemist(EntityType<?> type) {
        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type).equals(ThaumicConcilium.id("sloppy_alchemist"));
    }

    private static boolean blacklisted(EntityType<?> type, Holder<Biome> biome) {
        if (type == TCEntities.VENGEFUL_GOLEM.get()) {
            return biome.unwrapKey().map(k -> TCConfig.vengefulGolemBiomeBlacklist().contains(k.location().toString())).orElse(false);
        }
        if (isAlchemist(type)) {
            return biome.unwrapKey().map(k -> TCConfig.sloppyAlchemistBiomeBlacklist().contains(k.location().toString())).orElse(false);
        }
        return false;
    }

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD || !this.biomes.contains(biome)) {
            return;
        }
        for (SpawnerData spawner : this.spawners) {
            int weight = configuredWeight(spawner.type);
            if (blacklisted(spawner.type, biome) || weight == 0) {
                continue;
            }
            SpawnerData use = weight < 0 ? spawner : new SpawnerData(spawner.type, weight, spawner.minCount, spawner.maxCount);
            builder.getMobSpawnSettings().addSpawn(use.type.getCategory(), use);
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return TYPE.get();
    }
}
