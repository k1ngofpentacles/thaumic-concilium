package dev.movingcastle.thaumicconcilium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Solid Void (original {@code SolidVoidBlock}): a stone-like block, instant to break, 6000 blast resistance, that shows the end
 * portal's starfield. The look cannot be a static model (the starfield is a render type of its own), so the block has a
 * block entity that does nothing but host its renderer: it is not ticked, saves nothing and costs one draw call per visible block.
 */
public class SolidVoidBlock extends BaseEntityBlock {
    public static final MapCodec<SolidVoidBlock> CODEC = simpleCodec(SolidVoidBlock::new);

    public SolidVoidBlock(Properties properties) {
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SolidVoidBlockEntity(pos, state);
    }
}
