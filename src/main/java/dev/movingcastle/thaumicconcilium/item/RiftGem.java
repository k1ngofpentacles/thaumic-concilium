package dev.movingcastle.thaumicconcilium.item;

import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.wands.WandVisHelper;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.thaumicconcilium.registry.TCDataComponents;
import dev.movingcastle.thaumicconcilium.registry.TCItems;
import dev.movingcastle.thaumicconcilium.rift.Subspace;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The (active) Gem of Rifts (original {@code items/RiftGem}, meta 1). Held in use it opens a short-lived interdimensional rift every
 * 80 ticks where no other rift is within 16 blocks, for 10 perditio and 10 aer from a wand in the hotbar; stepping into the rift
 * with a gem in the main hand takes you to the Causal Bouillon ({@link Subspace}). The cast list (NBT {@code MAX_GEMS, CURR, NAME*,
 * META*, DIM*}) and the REVERSE flag are one data component, {@link State}. The inert gem is a plain item.
 */
public class RiftGem extends Item {
    public static final int MAX_CASTS = 16;
    public static final int INTERVAL = 80;
    /** 10 vis of each (centivis). */
    private static final Map<net.minecraft.resources.ResourceKey<com.leclowndu93150.thaumaturge.api.aspect.IAspect>, Integer> COST =
            Map.of(TTAspects.PERDITIO, 1000, TTAspects.AER, 1000);

    public RiftGem(Properties properties) {
        super(properties);
    }

    /** The casts inserted, which one is selected, and whether the gem "deflates" space (REVERSE). */
    public record State(List<GemCast> casts, int current, boolean reverse) {
        public static final State DEFAULT = new State(List.of(), 0, false);
        public static final Codec<State> CODEC = RecordCodecBuilder.create(i -> i.group(
                GemCast.CODEC.listOf().optionalFieldOf("casts", List.of()).forGetter(State::casts),
                Codec.INT.optionalFieldOf("current", 0).forGetter(State::current),
                Codec.BOOL.optionalFieldOf("reverse", false).forGetter(State::reverse)).apply(i, State::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> STREAM_CODEC = StreamCodec.composite(
                GemCast.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_CASTS)), State::casts,
                ByteBufCodecs.VAR_INT, State::current,
                ByteBufCodecs.BOOL, State::reverse,
                State::new);

        @Nullable
        public GemCast selected() {
            return casts.isEmpty() ? null : casts.get(Math.floorMod(current, casts.size()));
        }

        public State withReverse(boolean value) {
            return new State(casts, current, value);
        }

        public State next() {
            return casts.size() < 2 ? this : new State(casts, (current + 1) % casts.size(), reverse);
        }

        /** The cast appended and selected; null when the gem already holds 16. */
        @Nullable
        public State with(GemCast cast) {
            if (casts.size() >= MAX_CASTS) {
                return null;
            }
            List<GemCast> list = new ArrayList<>(casts);
            list.add(cast);
            return new State(List.copyOf(list), list.size() - 1, reverse);
        }
    }

    public static State state(ItemStack stack) {
        return stack.getOrDefault(TCDataComponents.RIFT_STATE.get(), State.DEFAULT);
    }

    /** Either gem of rifts, inert or active: the original let both through a rift (one class, two metas). */
    public static boolean isGem(ItemStack stack) {
        return stack.is(TCItems.RIFT_GEM.get()) || stack.is(TCItems.INERT_RIFT_GEM.get());
    }

    /** Renderer accessors: the active gem (the original's meta 1) turns; the inert one is a different item. */
    public static boolean active(ItemStack stack) {
        return stack.is(TCItems.RIFT_GEM.get());
    }

    public static int castCount(ItemStack stack) {
        return state(stack).casts().size();
    }

    /** The block item of the selected cast, or empty when the gem holds none. */
    public static ItemStack currentCast(ItemStack stack) {
        GemCast cast = state(stack).selected();
        return cast == null ? ItemStack.EMPTY : new ItemStack(cast.state().getBlock());
    }

    /** 1.7.10 never replayed the equip animation for an NBT change; 1.21 does for any component change (cycling, absorbing). */
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
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                stack.set(TCDataComponents.RIFT_STATE.get(), state(stack).next());
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    /** Shift + swing flips the curvature of space (the original also flipped it on the first plain swing of a fresh gem: a bug, not ported). */
    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
        if (entity instanceof Player player && player.isShiftKeyDown() && !entity.level().isClientSide()) {
            State s = state(stack);
            stack.set(TCDataComponents.RIFT_STATE.get(), s.withReverse(!s.reverse()));
        }
        return false;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof Player player)) {
            return;
        }
        int used = getUseDuration(stack, entity) - remaining;
        if (used % INTERVAL != 0 || !Subspace.noRiftNear(server, player)) {
            return;
        }
        if (WandVisHelper.consumeSpecificFromHotbar(player, COST, true)) {
            Vec3 at = target(player);
            server.playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.SHOCK.get(), SoundSource.PLAYERS, 1.0F,
                    1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F);
            Subspace.summon(server, at.add(0.0, 1.5, 0.0));
        }
        player.stopUsingItem();
    }

    /** Original: the block looked at (up to the reach), else five blocks along the look from the player's position. */
    private static Vec3 target(Player player) {
        HitResult hit = player.pick(10.0, 1.0F, false);
        if (hit instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK) {
            return block.getLocation();
        }
        return player.position().add(player.getLookAngle().scale(5.0));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("thaumicconcilium.tooltip.gem_cost").withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("aspect.thaumaturge.perditio").append(" x 10").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("aspect.thaumaturge.aer").append(" x 10").withStyle(ChatFormatting.GRAY));
        State s = state(stack);
        tooltip.add(Component.translatable(s.reverse() ? "thaumicconcilium.tooltip.riftgem.deflate" : "thaumicconcilium.tooltip.riftgem.inflate")
                .withStyle(ChatFormatting.YELLOW));
        GemCast cast = s.selected();
        if (cast != null) {
            tooltip.add(Component.translatable("thaumicconcilium.tooltip.riftgem").append(" " + s.casts().size() + "/" + MAX_CASTS)
                    .withStyle(ChatFormatting.BLUE));
            tooltip.add(Component.translatable("thaumicconcilium.tooltip.terragem").append(" ").append(cast.state().getBlock().getName())
                    .withStyle(ChatFormatting.BLUE));
        }
    }
}
