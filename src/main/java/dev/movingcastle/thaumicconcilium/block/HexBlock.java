package dev.movingcastle.thaumicconcilium.block;

import com.mojang.serialization.MapCodec;
import dev.movingcastle.thaumicconcilium.registry.TCBlocks;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Hex of Predictability's blocks (original: {@code HexOfPredictabilityBlock}): 0.7 hardness, 100 resistance, a
 * little light, drops nothing. The centre's renderer draws the whole 3x3 construct. Breaking any of them unmakes the
 * hex: every block goes back to what it replaced.
 */
public class HexBlock extends BaseEntityBlock {
    public static final MapCodec<HexBlock> CODEC = simpleCodec(HexBlock::new);
    private static final VoxelShape SHAPE = Shapes.block();

    public HexBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HexBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, TCBlocks.HEX_BLOCK_ENTITY.get(), HexBlockEntity::serverTick);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!level.isClientSide() && !state.is(newState.getBlock())) {
            dev.movingcastle.thaumicconcilium.rift.Hexes.unmake(level, pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    /** Original: only the top is solid (the jarred node stands on it). */
    @Override
    protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return false;
    }

    public static boolean topSolid(Direction side) {
        return side == Direction.UP;
    }
}
