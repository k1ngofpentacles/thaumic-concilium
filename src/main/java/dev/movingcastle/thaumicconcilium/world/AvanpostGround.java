package dev.movingcastle.thaumicconcilium.world;

import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * The Crimson Avanpost's ground rule (original: the block under every cell of the 11 x 10 footprint must be dirt, grass, sand or
 * stone). Pure functions, so the pack test can check them without a world.
 */
public final class AvanpostGround {
    /** Template-local footprint: x 0..10, z 3..12 (original x-2..x+8, z-4..z+5; the template's origin is original (x-2, z-7)). */
    public static final int X_MIN = 0, X_MAX = 10, Z_MIN = 3, Z_MAX = 12;

    private AvanpostGround() {
    }

    /** Dirt (with grass, podzol, ...), sand, or the overworld's base stone: the tags the original's four blocks widen to. */
    public static boolean allowed(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(BlockTags.BASE_STONE_OVERWORLD);
    }

    /** True when every footprint cell, rotated about {@code zero} (the template origin's world position), has an allowed block under it. */
    public static boolean footprintOk(Rotation rotation, BlockPos zero, Function<BlockPos, BlockState> groundAt) {
        for (int x = X_MIN; x <= X_MAX; x++) {
            for (int z = Z_MIN; z <= Z_MAX; z++) {
                BlockPos cell = StructureTemplate.transform(new BlockPos(x, 0, z), Mirror.NONE, rotation, BlockPos.ZERO).offset(zero);
                if (!allowed(groundAt.apply(cell))) {
                    return false;
                }
            }
        }
        return true;
    }
}
