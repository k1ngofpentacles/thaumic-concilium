package dev.movingcastle.thaumicconcilium.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Flesh Crucible (original: {@code FleshCrucibleBlock}): a crucible-shaped basin that keeps flesh. Standing in it (while it is
 * not powered): a player carrying flesh sips from it, a Mad Thaumaturge is smelted into it, any other creature is burned.
 * The block entity has no ticker: nothing happens unless something stands in it.
 */
public class FleshCrucibleBlock extends BaseEntityBlock {
    public static final MapCodec<FleshCrucibleBlock> CODEC = simpleCodec(FleshCrucibleBlock::new);
    private static final VoxelShape SHAPE = Shapes.join(Shapes.block(), Shapes.box(0.125, 0.25, 0.125, 0.875, 1.0, 0.875), BooleanOp.ONLY_FIRST);

    public FleshCrucibleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FleshCrucibleBlockEntity(pos, state);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || level.hasNeighborSignal(pos)
                || !(level.getBlockEntity(pos) instanceof FleshCrucibleBlockEntity crucible)) {
            return;
        }
        crucible.touch(entity);
    }

    @Override
    protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return false;
    }
}
