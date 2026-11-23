package dev.movingcastle.thaumicconcilium.block;

import com.mojang.serialization.MapCodec;
import dev.movingcastle.thaumicconcilium.entity.Vessel;
import dev.movingcastle.thaumicconcilium.registry.TCBlocks;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Verdant Capital (phase 5d): placed on top of an anchor shard of a subdued Vessel, it feeds that anchor with Botania mana so the
 * anchor drains by itself. It does not place anywhere else. It has no ticker: the Vessel reads its buffer. Without Botania the block
 * and item still exist but are inert (they refuse to place, and nothing can fill them). Placeholder cube model; the look is cycle 4.
 */
public class VerdantCapitalBlock extends BaseEntityBlock {
    public static final MapCodec<VerdantCapitalBlock> CODEC = simpleCodec(VerdantCapitalBlock::new);

    public VerdantCapitalBlock(Properties properties) {
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
        return new VerdantCapitalBlockEntity(pos, state);
    }

    /** Only on an anchor shard, and (on the server) only one of a subdued Vessel's. */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!net.neoforged.fml.ModList.get().isLoaded("botania")) {
            return null;
        }
        BlockPos below = context.getClickedPos().below();
        if (!context.getLevel().getBlockState(below).is(TCBlocks.ANCHOR.get())) {
            return null;
        }
        if (context.getLevel() instanceof ServerLevel level && !onSubduedAnchor(level, below)) {
            return null;
        }
        return this.defaultBlockState();
    }

    /** Whether {@code anchor} is a shard of a subdued Vessel (the rule for placing a capital on it). */
    public static boolean onSubduedAnchor(ServerLevel level, BlockPos anchor) {
        Vessel vessel = Vessel.forAnchor(level, anchor);
        return vessel != null && vessel.state() == Vessel.SUBDUED;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(TCBlocks.ANCHOR.get());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos,
            BlockPos neighbourPos) {
        return direction == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof net.minecraft.world.entity.player.Player player
                && level.getBlockEntity(pos) instanceof VerdantCapitalBlockEntity capital) {
            capital.setPlacer(player.getUUID());
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                dev.movingcastle.thaumicconcilium.compat.HeartstoneCompat.recordHeld(sp); // so their partner is found while they are offline
            }
        }
    }
}
