package dev.movingcastle.thaumicconcilium.item;

import com.leclowndu93150.thaumaturge.api.aspect.AspectIndexAccess;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.wands.WandVisHelper;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import dev.movingcastle.thaumicconcilium.registry.TCDataComponents;
import dev.movingcastle.thaumicconcilium.rift.Subspace;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The (active) Terra-cast Gem (original {@code items/TerraCastGem}, meta 1). Held in use, every 80 ticks it absorbs the block looked at
 * if that block is solid, breakable and has terra (10 terra and 10 aer from a wand in the hotbar): the block vanishes without a drop
 * and the gem remembers it and the dimension it was in. Crafted with a Gem of Rifts it adds its cast to that gem.
 */
public class TerraCastGem extends Item {
    private static final Map<ResourceKey<IAspect>, Integer> COST = Map.of(TTAspects.TERRA, 1000, TTAspects.AER, 1000);

    public TerraCastGem(Properties properties) {
        super(properties);
    }

    @Nullable
    public static GemCast cast(ItemStack stack) {
        return stack.get(TCDataComponents.TERRA_CAST.get());
    }

    /** Renderer accessors: the active gem turns; the inert one is a different item. */
    public static boolean active(ItemStack stack) {
        return stack.is(dev.movingcastle.thaumicconcilium.registry.TCItems.TERRA_CAST_GEM.get());
    }

    /** The block item of the cast, or empty when the gem holds none. */
    public static ItemStack castStack(ItemStack stack) {
        GemCast cast = cast(stack);
        return cast == null ? ItemStack.EMPTY : new ItemStack(cast.state().getBlock());
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return Integer.MAX_VALUE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof Player player)) {
            return;
        }
        int used = getUseDuration(stack, entity) - remaining;
        if (used % RiftGem.INTERVAL != 0 || !Subspace.noRiftNear(server, player)) {
            return;
        }
        if (player.pick(10.0, 1.0F, false) instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            absorb(server, player, stack, hit.getBlockPos());
        }
    }

    /** Whether the gem may take this block: solid, breakable, no block entity (the cast could not hold its contents), terra in the aspect index. */
    public static boolean absorbable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !state.blocksMotion() || state.getDestroySpeed(level, pos) < 0.0F || state.hasBlockEntity()) {
            return false;
        }
        ItemStack item = state.getBlock().getCloneItemStack(level, pos, state);
        if (item.isEmpty()) {
            item = new ItemStack(state.getBlock().asItem());
        }
        return !item.isEmpty() && AspectIndexAccess.of(item).amountOf(TTAspects.TERRA, level.registryAccess()) > 0;
    }

    /** One absorption (the body of the original's 80-tick action): checks, pays, removes the block, stores the cast. */
    public static boolean absorb(ServerLevel level, Player player, ItemStack gem, BlockPos pos) {
        if (!absorbable(level, pos) || !level.mayInteract(player, pos)) {
            return false;
        }
        if (!WandVisHelper.consumeSpecificFromHotbar(player, COST, true)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.SHOCK.get(), SoundSource.PLAYERS, 1.0F,
                1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F);
        gem.set(TCDataComponents.TERRA_CAST.get(), new GemCast(state, level.dimension()));
        level.removeBlock(pos, false);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20, 0.4, 0.4, 0.4, 0.0);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("thaumicconcilium.tooltip.gem_cost").withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("aspect.thaumaturge.terra").append(" x 10").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("aspect.thaumaturge.aer").append(" x 10").withStyle(ChatFormatting.GRAY));
        GemCast cast = cast(stack);
        if (cast != null) {
            tooltip.add(Component.translatable("thaumicconcilium.tooltip.terragem").append(" ").append(cast.state().getBlock().getName())
                    .withStyle(ChatFormatting.BLUE));
        }
    }
}
