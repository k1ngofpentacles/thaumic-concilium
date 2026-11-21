package dev.movingcastle.thaumicconcilium.block;

import com.mojang.serialization.MapCodec;
import dev.movingcastle.thaumicconcilium.menu.LithographerMenu;
import dev.movingcastle.thaumicconcilium.registry.TCBlocks;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Lithographer (original: {@code LithographerBlock}). It is built over the head of a bed (see
 * {@link dev.movingcastle.thaumicconcilium.lithograph.LithographerBuilder}), opens a four-slot screen for Astral
 * Monitors, and on a redstone signal starts its projection. The original's block had a tile-dependent orientation
 * (long along X or along Z); here it is the {@code axis} state, set from the way the player faces when placing it.
 */
public class LithographerBlock extends BaseEntityBlock {
    public static final MapCodec<LithographerBlock> CODEC = simpleCodec(LithographerBlock::new);
    /** The axis the machine is long along (original: orientation true = X). */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final VoxelShape SHAPE = Shapes.block();

    public LithographerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Original: facing north or south makes it long along X.
        return this.defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis() == Direction.Axis.Z
                ? Direction.Axis.X : Direction.Axis.Z);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90
                ? state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X) : state;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state;
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
        return new LithographerBlockEntity(pos, state);
    }

    /** Idle costs nothing: the ticker returns at once while the machine is at rest. */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, TCBlocks.LITHOGRAPHER_BLOCK_ENTITY.get(), LithographerBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof LithographerBlockEntity lithographer && player instanceof ServerPlayer server) {
            server.openMenu(new SimpleMenuProvider((id, inventory, p) -> new LithographerMenu(id, inventory, lithographer),
                    Component.translatable("container.thaumicconcilium.lithographer")), buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }

    /** Original: a redstone signal while the machine is at rest starts the projection. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbour, BlockPos from, boolean moving) {
        super.neighborChanged(state, level, pos, neighbour, from, moving);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof LithographerBlockEntity lithographer
                && lithographer.idle() && level.hasNeighborSignal(pos)) {
            lithographer.project();
        }
    }

    /** The original lost the monitors when the block broke; they drop here. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof LithographerBlockEntity lithographer) {
            Containers.dropContents(level, pos, lithographer);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
