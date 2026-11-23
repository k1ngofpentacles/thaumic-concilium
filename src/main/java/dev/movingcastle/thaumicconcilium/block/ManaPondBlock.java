package dev.movingcastle.thaumicconcilium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * Phase 5d cycle 3: the liquid block of the mana pond. A {@link LiquidBlock} whose fluid is source-only and still: it schedules no fluid
 * ticks (so nothing flows and lava never meets it), and it cannot be picked up by a bucket or any fluid handler.
 */
public final class ManaPondBlock extends LiquidBlock {
    public ManaPondBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        // LiquidBlock would schedule a fluid tick and look for lava: neither applies
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return state;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moved) {
    }

    /** Set by the client (cycle 4b): draws one glint over the pond block at {@code pos}. */
    @javax.annotation.Nullable
    public static java.util.function.BiConsumer<Level, BlockPos> clientGlint;

    /**
     * The pond's sparkle, on vanilla's random display tick (nothing runs with no player near), only under open air. Cycle 4d: the client
     * turns these calls into rare events (a few glints from one spot, then a long pause), so every call is passed on.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (clientGlint != null && level.getBlockState(pos.above()).isAir()) {
            clientGlint.accept(level, pos);
        }
    }

    @Override
    public ItemStack pickupBlock(Player player, LevelAccessor level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }
}
