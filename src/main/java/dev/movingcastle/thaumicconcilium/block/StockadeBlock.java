package dev.movingcastle.thaumicconcilium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Phase 10-4 (R12 C): the Stockade, a sharpened wooden post of Witchery 1.7.10 ({@code BlockStockade}, meaning only: this is original code).
 * A thin post that joins other stockades and vanilla fence gates in the four horizontal directions, shows its point only on the top post
 * ({@link #TIP}: the block above is not a stockade), and hurts a living creature that steps on it (3 damage, cactus type, server side).
 * Registered only with Just Another Witchery Remake ({@code compat.witchery.SiteBlocks}); it names no remake class.
 *
 * <p>Shapes (user decision 2026-11-27, a deliberate departure from the original's 0.3..0.7 and 0.9-high post): a lone post 0.25..0.75 on x and
 * z and full height (the collision is 1.0 too); along a joined axis it runs 0..1 and keeps 0.25..0.75 across it. One shape serves outline and collision.
 */
public class StockadeBlock extends Block {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    /** True when the block above is not a stockade: the sharpened point is drawn on this post. */
    public static final BooleanProperty TIP = BooleanProperty.create("tip");
    public static final float STEP_DAMAGE = 3.0F;

    private final VoxelShape[] shapes = new VoxelShape[16];

    public StockadeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(TIP, true));
        for (int i = 0; i < 16; i++) {
            shapes[i] = shapeOf((i & 1) != 0, (i & 2) != 0, (i & 4) != 0, (i & 8) != 0);
        }
    }

    private static VoxelShape shapeOf(boolean n, boolean e, boolean s, boolean w) {
        VoxelShape shape = Block.box(4, 0, 4, 12, 16, 12);
        if (n || s) {
            shape = Shapes.or(shape, Block.box(4, 0, 0, 12, 16, 16));
        }
        if (e || w) {
            shape = Shapes.or(shape, Block.box(0, 0, 4, 16, 16, 12));
        }
        return shape;
    }

    private static int index(BlockState state) {
        return (state.getValue(NORTH) ? 1 : 0) | (state.getValue(EAST) ? 2 : 0) | (state.getValue(SOUTH) ? 4 : 0) | (state.getValue(WEST) ? 8 : 0);
    }

    /** Original {@code canConnectFenceTo}: another stockade or a vanilla fence gate. */
    public boolean connectsTo(BlockState other) {
        return other.is(this) || other.getBlock() instanceof FenceGateBlock;
    }

    public static BooleanProperty side(Direction d) {
        return switch (d) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            default -> WEST;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, TIP);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = this.defaultBlockState();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            state = state.setValue(side(d), connectsTo(level.getBlockState(pos.relative(d))));
        }
        return state.setValue(TIP, !level.getBlockState(pos.above()).is(this));
    }

    /** A post set by a command, a dispenser or a test (not through {@link #getStateForPlacement}) still takes its sides and point from its surroundings. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        // only an untouched default state is fixed: a structure template stores its states (a stack's lower posts have no point) and keeps them.
        // The nested setBlock does the neighbour notification the outer one skips (it sees the state changed under it).
        if (!level.isClientSide && !oldState.is(this) && state == this.defaultBlockState()) {
            BlockState fixed = Block.updateFromNeighbourShapes(state, level, pos);
            if (fixed != state) {
                level.setBlock(pos, fixed, 2);
            }
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.UP) {
            return state.setValue(TIP, !neighbor.is(this));
        }
        if (direction.getAxis().isHorizontal()) {
            return state.setValue(side(direction), connectsTo(neighbor));
        }
        return state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes[index(state)];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes[index(state)];
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes[index(state)];
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    /** Original {@code func_149724_b} (onEntityWalking): 3 damage of the cactus type to a living entity, server side. */
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living) {
            living.hurt(level.damageSources().cactus(), STEP_DAMAGE);
        }
        super.stepOn(level, pos, state, entity);
    }
}
