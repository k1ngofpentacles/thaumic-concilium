package dev.movingcastle.thaumicconcilium.block;

import com.mojang.serialization.MapCodec;
import dev.movingcastle.thaumicconcilium.registry.TCBlocks;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** The Arcane Levitator: a full block that lifts a column of air above it ({@link ArcaneLevitatorBlockEntity}). */
public class ArcaneLevitatorBlock extends BaseEntityBlock {
    public static final MapCodec<ArcaneLevitatorBlock> CODEC = simpleCodec(ArcaneLevitatorBlock::new);

    public ArcaneLevitatorBlock(Properties properties) {
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcaneLevitatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, TCBlocks.ARCANE_LEVITATOR_BLOCK_ENTITY.get(), ArcaneLevitatorBlockEntity::tick);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        refresh(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moved) {
        super.neighborChanged(state, level, pos, block, fromPos, moved);
        refresh(level, pos);
    }

    private static void refresh(Level level, BlockPos pos) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ArcaneLevitatorBlockEntity be) {
            be.refresh();
        }
    }
}
