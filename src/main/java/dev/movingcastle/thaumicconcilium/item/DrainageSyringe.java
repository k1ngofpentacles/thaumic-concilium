package dev.movingcastle.thaumicconcilium.item;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.wands.ItemWand;
import com.leclowndu93150.thaumaturge.content.wands.WandVisHelper;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.thaumicconcilium.integration.botania.BotaniaMana;
import dev.movingcastle.thaumicconcilium.integration.neovitae.NeoVitaeBlood;
import dev.movingcastle.thaumicconcilium.registry.TCDataComponents;
import dev.movingcastle.thaumicconcilium.registry.TCSounds;
import dev.movingcastle.thaumicconcilium.spell.Looking;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * The Drainage Syringe (original: {@code DrainageSyringe}, Concilium 1.1.1, WTFPL; Q6 B: only with Thaumic Horizons, Botania and
 * Neo Vitae). Closed, held on a living thing within three blocks, it draws vis, mana or life points out of it every two seconds
 * at the price of a tenth of its health (and nothing from a creature at half health or less, or a golem); open, it gives
 * the stored amount back to the user every second. A player drawn from also loses a million of the same.
 * <p>
 * State: {@link State} in the {@code thaumicconcilium:syringe} data component; an absent component means type 0, amount 0, closed.
 * Right click starts the draw (or the return when open); Shift + right click toggles the cap; Shift + swing cycles the type while
 * the syringe is empty. Held rendering is the chief's (see {@code // LOOK: syringe BEWLR} in {@code TCClient}).
 */
public class DrainageSyringe extends Item {
    public static final int TYPE_VIS = 0;
    public static final int TYPE_MANA = 1;
    public static final int TYPE_BLOOD = 2;
    /** The original's caps per type: 100 vis, 250000 mana, 500000 LP. */
    public static final int[] MAX = {100, 250000, 500000};
    /** What one draw adds per type (the original's 10 / 5000 / 20000); the return gives the same back each second. */
    public static final int[] STEP = {10, 5000, 20000};
    /** A player target loses 5000 centivis of each primal, a million mana, a million LP. */
    public static final int PLAYER_VIS_CENTIVIS = 5000;
    public static final int PLAYER_MANA = 1_000_000;
    public static final int PLAYER_LP = 1_000_000;
    public static final double REACH = 3.0;
    public static final int DRAW_PERIOD = 40;
    public static final int RETURN_PERIOD = 20;

    /** The syringe's NBT in the original: Type, Amount, IsOpen. */
    public record State(int type, int amount, boolean open) {
        public static final State EMPTY = new State(TYPE_VIS, 0, false);
        public static final Codec<State> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("type", 0).forGetter(State::type),
                Codec.INT.optionalFieldOf("amount", 0).forGetter(State::amount),
                Codec.BOOL.optionalFieldOf("open", false).forGetter(State::open)).apply(i, State::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, State::type, ByteBufCodecs.VAR_INT, State::amount, ByteBufCodecs.BOOL, State::open, State::new);

        public State withAmount(int value) {
            return new State(type, Mth.clamp(value, 0, MAX[safeType(type)]), open);
        }

        public State withOpen(boolean value) {
            return new State(type, amount, value);
        }

        public State withType(int value) {
            return new State(value, amount, open);
        }
    }

    private static int safeType(int type) {
        return Mth.clamp(type, 0, 2);
    }

    public DrainageSyringe(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.UNCOMMON).setNoRepair());
    }

    public static State state(ItemStack stack) {
        return stack.getOrDefault(TCDataComponents.SYRINGE.get(), State.EMPTY);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        State state = state(stack);
        if (player.isShiftKeyDown()) {
            stack.set(TCDataComponents.SYRINGE.get(), state.withOpen(!state.open()));
            return InteractionResultHolder.consume(stack);   // consume: no arm swing, which would also cycle the type
        }
        if (state.open()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (Looking.at(player, REACH, 0.1) instanceof LivingEntity) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), TCSounds.STAB.get(), SoundSource.PLAYERS, 1.0F,
                    level.random.nextFloat());
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    /**
     * Shift + swing (any swing the server hears: air, block or entity): the next kind of energy, only while the syringe holds
     * none (the original's {@code onEntitySwing}). Returns false so the swing itself goes on.
     */
    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
        if (!entity.level().isClientSide() && entity instanceof Player player && player.isShiftKeyDown()) {
            State state = state(stack);
            if (state.amount() == 0) {
                stack.set(TCDataComponents.SYRINGE.get(), state.withType((safeType(state.type()) + 1) % 3));
            }
        }
        return false;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (level.isClientSide() || !(user instanceof Player player)) {
            return;
        }
        State state = state(stack);
        if (!state.open()) {
            if (remaining % DRAW_PERIOD == 0) {
                draw(player, stack, state);
            }
        } else if (remaining % RETURN_PERIOD == 0 && state.amount() > 0) {
            giveBack(player, stack, state);
        }
    }

    private static boolean isGolem(Entity entity) {
        return entity instanceof AbstractGolem || entity instanceof com.leclowndu93150.thaumaturge.content.golem.EntityThaumaturgeGolem;
    }

    private static void draw(Player player, ItemStack stack, State state) {
        if (!(Looking.at(player, REACH, 0.1) instanceof LivingEntity target) || isGolem(target)
                || target.getHealth() <= target.getMaxHealth() / 2.0F
                || !target.hurt(player.damageSources().playerAttack(player), target.getMaxHealth() / 10.0F)) {
            return;
        }
        int type = safeType(state.type());
        stack.set(TCDataComponents.SYRINGE.get(), state.withAmount(state.amount() + STEP[type]));
        if (target instanceof Player victim) {
            switch (type) {
                case TYPE_VIS -> consumeVisFromInventory(victim, PLAYER_VIS_CENTIVIS);
                case TYPE_MANA -> BotaniaMana.take(victim, stack, PLAYER_MANA);
                default -> NeoVitaeBlood.drainPlayer(victim, PLAYER_LP);
            }
        }
    }

    /** TC4's {@code consumeVisFromInventory}: takes up to {@code centivis} of each primal from every wand the player carries. */
    public static void consumeVisFromInventory(Player victim, int centivis) {
        for (ResourceKey<IAspect> primal : TTAspects.PRIMALS) {
            int owed = centivis;
            for (ItemStack carried : victim.getInventory().items) {
                if (owed <= 0) {
                    break;
                }
                if (carried.getItem() instanceof ItemWand) {
                    int take = Math.min(owed, WandVisHelper.getVis(carried, primal));
                    if (take > 0) {
                        WandVisHelper.addRealVis(carried, primal, -take, true);
                        owed -= take;
                    }
                }
            }
        }
    }

    private static void giveBack(Player player, ItemStack stack, State state) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F,
                player.level().random.nextFloat());
        int type = safeType(state.type());
        switch (type) {
            case TYPE_VIS -> {
                ResourceKey<IAspect> primal = TTAspects.PRIMALS.get(player.getRandom().nextInt(TTAspects.PRIMALS.size()));
                ItemStack wand = WandVisHelper.findWandInHotbarWithRoom(player, primal, STEP[TYPE_VIS]);
                if (!wand.isEmpty()) {
                    WandVisHelper.addVis(wand, primal, STEP[TYPE_VIS], true);
                }
            }
            case TYPE_MANA -> BotaniaMana.give(player, stack, STEP[TYPE_MANA]);
            default -> NeoVitaeBlood.giveBack(player, STEP[TYPE_BLOOD]);
        }
        stack.set(TCDataComponents.SYRINGE.get(), state.withAmount(state.amount() - STEP[type]));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        State state = state(stack);
        String[] names = {"vis", "mana", "blood"};
        tooltip.add(Component.translatable("item.thaumicconcilium.drainage_syringe.type." + names[safeType(state.type())])
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.thaumicconcilium.drainage_syringe.amount", state.amount(), MAX[safeType(state.type())])
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
