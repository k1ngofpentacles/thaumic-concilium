package dev.movingcastle.thaumicconcilium.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.thaumicconcilium.TCConfig;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * The Crimson Avanpost (cultist tower). One attempt per structure-set cell; the original's ground rule is checked on the
 * generator's base column (no chunk reads), so a refused attempt costs only the footprint scan.
 */
public final class CrimsonAvanpostStructure extends Structure {
    public static final MapCodec<CrimsonAvanpostStructure> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(settingsCodec(i)).apply(i, CrimsonAvanpostStructure::new));

    public CrimsonAvanpostStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (!TCConfig.GENERATE_AVANPOSTS.get()) {
            return Optional.empty();
        }
        ChunkPos chunk = context.chunkPos();
        RandomSource random = context.random();
        int cx = chunk.getMinBlockX() + random.nextInt(16);
        int cz = chunk.getMinBlockZ() + random.nextInt(16);
        Rotation rotation = Rotation.getRandom(random);
        Vec3i size = context.structureTemplateManager().getOrCreate(CrimsonAvanpostPiece.TEMPLATE).getSize();
        int y = context.chunkGenerator().getFirstOccupiedHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        BlockPos corner = new BlockPos(cx - size.getX() / 2, y, cz - size.getZ() / 2);
        BlockPos zero = StructureTemplate.getZeroPositionWithTransform(corner, Mirror.NONE, rotation, size.getX(), size.getZ());
        boolean ok = AvanpostGround.footprintOk(rotation, zero, cell -> context.chunkGenerator()
                .getBaseColumn(cell.getX(), cell.getZ(), context.heightAccessor(), context.randomState()).getBlock(y - 1));
        if (!ok) {
            return Optional.empty();
        }
        return Optional.of(new GenerationStub(new BlockPos(cx, y, cz),
                builder -> builder.addPiece(new CrimsonAvanpostPiece(context.structureTemplateManager(), zero, rotation))));
    }

    @Override
    public StructureType<?> type() {
        return TCStructures.CRIMSON_AVANPOST.get();
    }
}
