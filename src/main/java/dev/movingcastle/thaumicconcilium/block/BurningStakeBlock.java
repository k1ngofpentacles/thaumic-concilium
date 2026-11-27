package dev.movingcastle.thaumicconcilium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Phase 10-4 (user decision 2026-11-27): the Burning Stake of the Burning Site, a separate block from the palisade Stockade (a deliberate
 * departure from the original, whose stake was four stockades). A blunt post with four looks by its place in a stack, kept from its
 * neighbours like the Stockade's point: foot (nothing below), shackle (the post just above a foot), shaft (between), top (nothing above).
 * No step damage. Registered only with the remake ({@code compat.witchery.SiteBlocks}); it names no remake class.
 */
public class BurningStakeBlock extends Block {
    public enum Part implements StringRepresentable {
        FOOT("foot"), SHACKLE("shackle"), SHAFT("shaft"), TOP("top");

        private final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    /** x and z 5..11 of 16, full height. */
    private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 16, 11);

    public BurningStakeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(PART, Part.FOOT));
    }

    /** The part of a post from what is around it: a pure function of (below is a stake, below is a foot, above is a stake). */
    public static Part partFor(boolean belowIsStake, boolean belowIsFoot, boolean aboveIsStake) {
        if (!belowIsStake) {
            return Part.FOOT;
        }
        if (!aboveIsStake) {
            return Part.TOP;
        }
        return belowIsFoot ? Part.SHACKLE : Part.SHAFT;
    }

    private Part partAt(BlockGetter level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        boolean belowStake = below.is(this);
        return partFor(belowStake, belowStake && below.getValue(PART) == Part.FOOT, level.getBlockState(pos.above()).is(this));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(PART, partAt(context.getLevel(), context.getClickedPos()));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction.getAxis() == Direction.Axis.Y) {
            // the below post's own part changes when its neighbours do, and that notifies this one again
            boolean below = direction == Direction.DOWN;
            BlockState belowState = below ? neighbor : level.getBlockState(pos.below());
            BlockState aboveState = below ? level.getBlockState(pos.above()) : neighbor;
            boolean belowStake = belowState.is(this);
            return state.setValue(PART, partFor(belowStake, belowStake && belowState.getValue(PART) == Part.FOOT, aboveState.is(this)));
        }
        return state;
    }

    /** Same as the Stockade: an untouched default state (a command, a test) takes its part from its surroundings; a template's stored part stays. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !oldState.is(this) && state == this.defaultBlockState()) {
            BlockState fixed = state.setValue(PART, partAt(level, pos));
            if (fixed != state) {
                level.setBlock(pos, fixed, 2);
            }
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
