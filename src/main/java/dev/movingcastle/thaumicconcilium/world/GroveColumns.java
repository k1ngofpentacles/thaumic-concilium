package dev.movingcastle.thaumicconcilium.world;

import com.mojang.serialization.Codec;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Phase 5d cycle 2b: the grove record. A chunk attachment holding a 16-bit mask over the chunk's 4x4 quart columns (the biome grid): a
 * set bit means "this column was painted Concilium Grove". It is set when the painter paints a column and never cleared by taint, so
 * when taint is cleared (or a pure node purifies) a column that was grove goes back to grove, not to the worldgen biome. Serialised with
 * the chunk, not synced (the client only needs the biome, which the vanilla biome packet carries).
 */
public final class GroveColumns {
    public static final ResourceKey<Biome> KEY = ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(ThaumicConcilium.MODID, "concilium_grove"));
    public static final TagKey<Biome> TAG = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(ThaumicConcilium.MODID, "concilium_grove"));
    public static final int ALL = 0xFFFF;

    public static final Codec<GroveColumns> CODEC = Codec.INT.optionalFieldOf("mask", 0).xmap(GroveColumns::new, g -> g.mask).codec();

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ThaumicConcilium.MODID);
    public static final Supplier<AttachmentType<GroveColumns>> ATTACHMENT = ATTACHMENTS.register("grove_columns",
            () -> AttachmentType.builder(() -> new GroveColumns()).serialize(CODEC).build());

    private int mask;

    public GroveColumns() {
        this(0);
    }

    private GroveColumns(int mask) {
        this.mask = mask & ALL;
    }

    private static int bit(int localQuartX, int localQuartZ) {
        return 1 << (localQuartZ & 3) * 4 + (localQuartX & 3);
    }

    public boolean has(int localQuartX, int localQuartZ) {
        return (this.mask & bit(localQuartX, localQuartZ)) != 0;
    }

    public void set(int localQuartX, int localQuartZ) {
        this.mask |= bit(localQuartX, localQuartZ);
    }

    public int mask() {
        return this.mask;
    }

    public boolean full() {
        return this.mask == ALL;
    }

    // ---------------------------------------------------------------- level helpers (loaded chunks only, never loads one)
    @Nullable
    private static LevelChunk loaded(ServerLevel level, int blockX, int blockZ) {
        return level.getChunkSource().getChunkNow(blockX >> 4, blockZ >> 4);
    }

    /** Whether the quart column holding this block was painted grove (false for an unloaded chunk). */
    public static boolean isGrove(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = loaded(level, pos.getX(), pos.getZ());
        if (chunk == null) {
            return false;
        }
        GroveColumns g = chunk.getExistingDataOrNull(ATTACHMENT.get());
        return g != null && g.has(QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getZ()));
    }

    /** The mask of a loaded chunk (0 when none or not loaded). */
    public static int maskOf(ServerLevel level, int chunkX, int chunkZ) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (chunk == null) {
            return 0;
        }
        GroveColumns g = chunk.getExistingDataOrNull(ATTACHMENT.get());
        return g == null ? 0 : g.mask;
    }
}
