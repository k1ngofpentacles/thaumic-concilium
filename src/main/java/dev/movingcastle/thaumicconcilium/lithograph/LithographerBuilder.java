package dev.movingcastle.thaumicconcilium.lithograph;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.capability.PlayerResearchAccess;
import com.leclowndu93150.thaumaturge.content.wands.ItemWand;
import com.leclowndu93150.thaumaturge.content.wands.WandVisHelper;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.block.LithographerBlock;
import dev.movingcastle.thaumicconcilium.item.CastingBracelet;
import dev.movingcastle.thaumicconcilium.registry.TCBlocks;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Building the Lithographer (original: {@code WandHandler} 83-160). With the lithographer research, a wand struck on an
 * Alchemical Construct that stands on a thaumium block, over the head of a bed, with gold blocks to either side of the
 * thaumium block and of the bed (along X or along Z), pays 100 ignis, 100 ordo, 50 aqua and 100 aer and turns the
 * thaumium block into the Lithographer. The construct and the four gold blocks are consumed; the bed stays.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class LithographerBuilder {
    public static final ResourceLocation RESEARCH = ThaumicConcilium.id("lithographer");

    private LithographerBuilder() {
    }

    private static boolean wand(ItemStack stack) {
        return stack.getItem() instanceof ItemWand && !(stack.getItem() instanceof CastingBracelet);
    }

    private static boolean bedHead(BlockState state) {
        return state.getBlock() instanceof BedBlock && state.getValue(BedBlock.PART) == BedPart.HEAD;
    }

    /** Whether the gold blocks stand either side of the thaumium block and the bed along {@code side} (an axis direction). */
    private static boolean golds(ServerLevel level, BlockPos thaumium, Direction side) {
        for (Direction d : new Direction[]{side, side.getOpposite()}) {
            if (!level.getBlockState(thaumium.relative(d)).is(Blocks.GOLD_BLOCK)
                    || !level.getBlockState(thaumium.below().relative(d)).is(Blocks.GOLD_BLOCK)) {
                return false;
            }
        }
        return true;
    }

    private static Map<ResourceKey<IAspect>, Integer> cost() {
        // Centivis, as the Hex pays: the original listed whole vis.
        return Map.of(TTAspects.IGNIS, 10000, TTAspects.ORDO, 10000, TTAspects.AQUA, 5000, TTAspects.AER, 10000);
    }

    @SubscribeEvent
    public static void onWand(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!wand(stack) || !(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BlockPos pos = event.getPos();
        if (!level.getBlockState(pos).is(TTBlocks.ALCHEMICAL_CONSTRUCT.get())) {
            return;
        }
        BlockPos thaumium = pos.below();
        if (!level.getBlockState(thaumium).is(TTBlocks.METAL_THAUMIUM_BLOCK.get()) || !bedHead(level.getBlockState(thaumium.below()))
                || !PlayerResearchAccess.of(player).hasCompleted(RESEARCH)) {
            return;
        }
        Direction.Axis axis;
        if (golds(level, thaumium, Direction.EAST)) {
            axis = Direction.Axis.X;
        } else if (golds(level, thaumium, Direction.SOUTH)) {
            axis = Direction.Axis.Z;
        } else {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!WandVisHelper.payCosts(stack, player, cost(), true, true)) {
            player.displayClientMessage(Component.translatable("message.thaumicconcilium.lithographer_no_vis"), true);
            return;
        }
        Direction side = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        BlockPos[] consumed = {pos, thaumium, thaumium.relative(side), thaumium.below().relative(side),
                thaumium.relative(side.getOpposite()), thaumium.below().relative(side.getOpposite())};
        for (BlockPos p : consumed) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            level.sendParticles(ParticleTypes.ENCHANT, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 16, 0.4, 0.4, 0.4, 0.5);
        }
        level.setBlock(thaumium, TCBlocks.LITHOGRAPHER.get().defaultBlockState().setValue(LithographerBlock.AXIS, axis), 3);
        level.playSound(null, pos, TTSounds.WAND.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
