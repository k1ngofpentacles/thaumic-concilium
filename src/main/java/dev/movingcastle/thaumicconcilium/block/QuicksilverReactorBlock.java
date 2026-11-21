package dev.movingcastle.thaumicconcilium.block;

import com.mojang.serialization.MapCodec;
import dev.movingcastle.thaumicconcilium.registry.TCBlocks;
import dev.movingcastle.thaumicconcilium.registry.TCSounds;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Quicksilver Reactor (original: {@code QuicksilverCrucibleBlock}): a crucible-shaped basin of mercury. Item entities of
 * quicksilver landing in it feed it, creatures standing in it are burned (1 fire damage per 10 ticks), and its permutatio
 * is a comparator signal. Made by a Quicksilver Elemental dying in a crucible; it has no recipe.
 */
public class QuicksilverReactorBlock extends BaseEntityBlock {
    public static final MapCodec<QuicksilverReactorBlock> CODEC = simpleCodec(QuicksilverReactorBlock::new);
    private static final VoxelShape SHAPE = Shapes.join(Shapes.block(), Shapes.box(0.125, 0.25, 0.125, 0.875, 1.0, 0.875), BooleanOp.ONLY_FIRST);

    public QuicksilverReactorBlock(Properties properties) {
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
        return new QuicksilverReactorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, TCBlocks.QUICKSILVER_REACTOR_BLOCK_ENTITY.get(),
                level.isClientSide() ? QuicksilverReactorBlockEntity::clientTick : QuicksilverReactorBlockEntity::serverTick);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof QuicksilverReactorBlockEntity reactor)) {
            return;
        }
        if (entity instanceof ItemEntity item && QuicksilverReactorBlockEntity.isNormalItem(item)) {
            reactor.attemptSmelt(item);
        } else if (entity instanceof LivingEntity && level.getGameTime() - reactor.lastHurt >= 10) {
            reactor.lastHurt = level.getGameTime();
            entity.hurt(level.damageSources().inFire(), 1.0F);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F,
                    2.0F + level.random.nextFloat() * 0.4F);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) == 0 && level.getBlockEntity(pos) instanceof QuicksilverReactorBlockEntity r && r.permutatio() > 0) {
            level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), TCSounds.MELTED.get(), SoundSource.BLOCKS,
                    1.1F + random.nextFloat() * 0.1F, 1.2F + random.nextFloat() * 0.2F, false);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** Original: permutatio / 100 of 14, plus one while it holds any. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof QuicksilverReactorBlockEntity r) {
            return Mth.floor(r.permutatio() / 100.0F * 14.0F) + (r.permutatio() > 0 ? 1 : 0);
        }
        return 0;
    }

    @Override
    protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return false;
    }
}
