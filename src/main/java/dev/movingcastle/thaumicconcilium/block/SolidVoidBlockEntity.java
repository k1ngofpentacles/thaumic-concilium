package dev.movingcastle.thaumicconcilium.block;

import dev.movingcastle.thaumicconcilium.registry.TCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Hosts the Solid Void's renderer. No state, no ticker (the original tile did the same). */
public class SolidVoidBlockEntity extends BlockEntity {
    public SolidVoidBlockEntity(BlockPos pos, BlockState state) {
        super(TCBlocks.SOLID_VOID_BLOCK_ENTITY.get(), pos, state);
    }
}
