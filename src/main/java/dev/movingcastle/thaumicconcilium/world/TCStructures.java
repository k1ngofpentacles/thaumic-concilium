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

    private TCStructures() {
    }
}
