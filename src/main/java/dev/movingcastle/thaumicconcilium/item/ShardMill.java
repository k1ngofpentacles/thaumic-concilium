package dev.movingcastle.thaumicconcilium.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.thaumicconcilium.entity.ShardPowder;
import dev.movingcastle.thaumicconcilium.registry.TCDataComponents;
import dev.movingcastle.thaumicconcilium.registry.TCSounds;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Shard Mill (Greatwood) and Silverwood Shard Mill. As in the original:
 * <ul>
 *   <li>sneak + use opens or closes the lid;</li>
 *   <li>with the lid open and the mill empty, holding use for a second tips in one shard of the selected kind from your
 *       inventory, which makes 20 ticks of powder;</li>
 *   <li>with the lid closed, holding use cranks the mill, spraying 2 to 4 motes of powder a tick along your aim until
 *       the load runs out. Each mote acts when it settles (see {@link ShardPowder}).</li>
 * </ul>
 * The shard kind is chosen with sneak + mouse wheel while holding the mill (the original used a radial menu).
 */
public class ShardMill extends Item {
    public static final int LOAD = 20;
    private static final int GRIND = 72000;

    /** Lid, selected shard (index into this mill's list), loaded shard ({@link ShardKind} ordinal or -1), powder left. */
    public record State(boolean open, int selected, int loaded, int amount) {
        public static final State EMPTY = new State(true, 0, -1, 0);
        public static final Codec<State> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.BOOL.fieldOf("open").forGetter(State::open),
                Codec.INT.fieldOf("selected").forGetter(State::selected),
                Codec.INT.fieldOf("loaded").forGetter(State::loaded),
                Codec.INT.fieldOf("amount").forGetter(State::amount)
        ).apply(i, State::new));
        public static final StreamCodec<ByteBuf, State> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, State::open, ByteBufCodecs.VAR_INT, State::selected,
                ByteBufCodecs.INT, State::loaded, ByteBufCodecs.VAR_INT, State::amount, State::new);

        State withOpen(boolean v) { return new State(v, this.selected, this.loaded, this.amount); }
        State withSelected(int v) { return new State(this.open, v, this.loaded, this.amount); }
        State withLoad(int kind, int left) { return new State(this.open, this.selected, left > 0 ? kind : -1, Math.max(0, left)); }
    }

    private final List<ShardKind> kinds;

    public ShardMill(Properties properties, List<ShardKind> kinds) {
        super(properties.stacksTo(1));
        this.kinds = kinds;
    }

    public static State state(ItemStack stack) {
        return stack.getOrDefault(TCDataComponents.MILL_STATE.get(), State.EMPTY);
    }

    private static void set(ItemStack stack, State state) {
        stack.set(TCDataComponents.MILL_STATE.get(), state);
    }

    public ShardKind selected(ItemStack stack) {
        return this.kinds.get(Math.floorMod(state(stack).selected(), this.kinds.size()));
    }

    /** Sneak + mouse wheel: step the selected shard kind. */
    public void cycle(Player player, ItemStack stack, int steps) {
        State s = state(stack);
        set(stack, s.withSelected(Math.floorMod(s.selected() + steps, this.kinds.size())));
        player.displayClientMessage(Component.translatable("message.thaumicconcilium.mill_selected",
                this.selected(stack).item().getDescription()), true);
    }

    private int findShard(Player player, ShardKind kind) {
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            if (kind.matches(player.getInventory().items.get(i))) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        State s = state(stack);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                set(stack, s.withOpen(!s.open()));
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), TCSounds.CLACK.get(), SoundSource.PLAYERS,
                    1.0F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        boolean canGrind = !s.open() && s.amount() > 0 && s.loaded() >= 0;
        boolean canLoad = s.open() && s.amount() == 0 && this.findShard(player, this.selected(stack)) >= 0;
        if (canGrind || canLoad) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return state(stack).open() ? LOAD : GRIND;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        State s = state(stack);
        int elapsed = GRIND - remaining;
        if (s.open() || s.loaded() < 0 || elapsed >= s.amount() || !(level instanceof ServerLevel server)) {
            return;
        }
        if (elapsed % 10 == 0) {
            server.playSound(null, entity.getX(), entity.getY(), entity.getZ(), TCSounds.MILL.get(), SoundSource.PLAYERS, 0.5F, 1.0F);
        }
        ShardKind kind = ShardKind.byOrdinal(s.loaded());
        Vec3 look = entity.getLookAngle();
        float yaw = entity.getYRot() * Mth.DEG_TO_RAD;
        float pitch = entity.getXRot() * Mth.DEG_TO_RAD;
        Vec3 aim = new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch)).normalize();
        int motes = 2 + server.random.nextInt(3);
        for (int i = 0; i < motes; i++) {
            Vec3 at = new Vec3(entity.getX() + look.x - Mth.cos(yaw) * 0.32, entity.getEyeY() + look.y - 0.5,
                    entity.getZ() + look.z - Mth.sin(yaw) * 0.32);
            Vec3 motion = aim.add(server.random.nextGaussian() * 0.09, server.random.nextGaussian() * 0.09,
                    server.random.nextGaussian() * 0.09);
            ShardPowder.spray(server, entity, at, motion, kind, 0);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        State s = state(stack);
        if (!s.open() && !level.isClientSide()) {
            set(stack, s.withLoad(s.loaded(), Mth.clamp(s.amount() - (GRIND - remaining), 0, LOAD)));
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        State s = state(stack);
        if (s.open() && s.amount() == 0 && entity instanceof Player player && !level.isClientSide()) {
            ShardKind kind = this.selected(stack);
            int slot = this.findShard(player, kind);
            if (slot >= 0) {
                player.getInventory().items.get(slot).shrink(1);
                set(stack, s.withLoad(kind.ordinal(), LOAD));
                level.playSound(null, player.getX(), player.getY(), player.getZ(), TCSounds.SHARD_DROP.get(), SoundSource.PLAYERS,
                        0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
            }
        } else if (!s.open() && !level.isClientSide()) {
            set(stack, s.withLoad(s.loaded(), 0));
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        State s = state(stack);
        tooltip.add(Component.translatable("item.thaumicconcilium.shard_mill.selected", this.selected(stack).item().getDescription())
                .withStyle(ChatFormatting.GOLD));
        if (s.loaded() >= 0 && s.amount() > 0) {
            tooltip.add(Component.translatable("item.thaumicconcilium.shard_mill.loaded",
                    ShardKind.byOrdinal(s.loaded()).item().getDescription(), s.amount()).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable(s.open() ? "item.thaumicconcilium.shard_mill.open" : "item.thaumicconcilium.shard_mill.closed")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.thaumicconcilium.shard_mill.desc").withStyle(ChatFormatting.DARK_GRAY));
    }
}
