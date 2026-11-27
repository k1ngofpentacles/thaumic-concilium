package dev.movingcastle.thaumicconcilium.world;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Structure type and piece type of the Crimson Avanpost. */
public final class TCStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, ThaumicConcilium.MODID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, ThaumicConcilium.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<CrimsonAvanpostStructure>> CRIMSON_AVANPOST =
            STRUCTURE_TYPES.register("crimson_avanpost", () -> () -> CrimsonAvanpostStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> CRIMSON_AVANPOST_PIECE =
            STRUCTURE_PIECES.register("crimson_avanpost", () -> CrimsonAvanpostPiece::new);

    /** Phase 10-4: the Burning Site's village pool element (registered always; it names no Witchery class). */
    public static final DeferredRegister<net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType<?>> POOL_ELEMENTS =
            DeferredRegister.create(Registries.STRUCTURE_POOL_ELEMENT, ThaumicConcilium.MODID);
    public static final DeferredHolder<net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType<?>,
            net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType<BurningSiteElement>> BURNING_SITE =
            POOL_ELEMENTS.register("burning_site", () -> () -> BurningSiteElement.CODEC);

    private TCStructures() {
    }
}
