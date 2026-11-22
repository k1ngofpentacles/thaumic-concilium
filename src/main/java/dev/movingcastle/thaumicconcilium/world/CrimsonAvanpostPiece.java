package dev.movingcastle.thaumicconcilium.world;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.mob.CrimsonArcher;
import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * The tower: the template {@code thaumicconcilium:cultist_tower} with its entities left out, then the guards spawned here so they
 * are finalised (equipment) and persistent: the 3 fixed archers of the original, and 0-4 more on the surface within 8 blocks of its anchor.
 */
public final class CrimsonAvanpostPiece extends TemplateStructurePiece {
    public static final ResourceLocation TEMPLATE = ThaumicConcilium.id("cultist_tower");
    /** Template-local guard cells (the .nbt's entity list holds the same three) and the original's anchor (x, y, z). */
    static final BlockPos[] GUARDS = {new BlockPos(5, 11, 4), new BlockPos(7, 11, 4), new BlockPos(5, 11, 7)};
    static final BlockPos ANCHOR = new BlockPos(2, 0, 7);

    public CrimsonAvanpostPiece(StructureTemplateManager manager, BlockPos position, Rotation rotation) {
        super(TCStructures.CRIMSON_AVANPOST_PIECE.get(), 0, manager, TEMPLATE, TEMPLATE.toString(), settings(rotation), position);
    }

    public CrimsonAvanpostPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(TCStructures.CRIMSON_AVANPOST_PIECE.get(), tag, context.structureTemplateManager(), id -> settings(readRotation(tag)));
    }

    private static Rotation readRotation(CompoundTag tag) {
        try {
            return Rotation.valueOf(tag.getString("Rotation"));
        } catch (IllegalArgumentException e) {
            return Rotation.NONE;
        }
    }

    private static StructurePlaceSettings settings(Rotation rotation) {
        return new StructurePlaceSettings().setRotation(rotation).setMirror(Mirror.NONE).setIgnoreEntities(true);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString("Rotation", this.placeSettings.getRotation().name());
    }

    @Override
    protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos reference) {
        super.postProcess(level, structures, generator, random, chunkBox, chunkPos, reference);
        if (chunkBox.isInside(this.templatePosition)) {   // once: the chunk that holds the template origin
            spawnGuards(level, random);
        }
    }

    private BlockPos world(BlockPos local) {
        return StructureTemplate.transform(local, Mirror.NONE, this.placeSettings.getRotation(), BlockPos.ZERO).offset(this.templatePosition);
    }

    void spawnGuards(WorldGenLevel level, RandomSource random) {
        for (BlockPos local : GUARDS) {
            BlockPos p = world(local);
            spawn(level, p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
        }
        BlockPos anchor = world(ANCHOR);
        int extra = random.nextInt(5);
        for (int i = 0; i < extra; i++) {
            int x = anchor.getX() - 8 + random.nextInt(16);
            int z = anchor.getZ() - 8 + random.nextInt(16);
            if (level.hasChunk(x >> 4, z >> 4)) {
                spawn(level, x + 0.5, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z), z + 0.5);
            }
        }
    }

    private static void spawn(WorldGenLevel level, double x, double y, double z) {
        CrimsonArcher archer = TCEntities.CRIMSON_ARCHER.get().create(level.getLevel());
        if (archer == null) {
            return;
        }
        archer.moveTo(x, y, z, 0.0F, 0.0F);
        archer.finalizeSpawn(level, level.getCurrentDifficultyAt(archer.blockPosition()), MobSpawnType.STRUCTURE, null);
        archer.setPersistenceRequired();
        level.addFreshEntityWithPassengers(archer);
    }
}
