package dev.movingcastle.thaumicconcilium.spell;

import com.leclowndu93150.thaumaturge.api.spell.event.SpellCastEvent;
import com.leclowndu93150.thaumaturge.content.casters.CasterManager;
import com.leclowndu93150.thaumaturge.api.spell.Spell;
import com.leclowndu93150.thaumaturge.api.spell.Spells;
import com.leclowndu93150.thaumaturge.content.spell.casting.SpellCasting;
import com.leclowndu93150.thaumaturge.content.spell.item.FocusItems;
import com.leclowndu93150.thaumaturge.content.wands.WandVisHelper;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.item.CastingBracelet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.Supplier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import dev.movingcastle.thaumicconcilium.network.BraceletFxPayload;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

/**
 * Casting from a worn bracelet. The client sends the cast key's press and release
 * ({@link dev.movingcastle.thaumicconcilium.network.BraceletCastPayload}); this runs Thaumaturge's own casting
 * ({@link SpellCasting}) with the bracelet as the caster, so spells, vis payment and refusals behave exactly as for a
 * wand. Instant spells cast on press; channelled spells pulse every tick while the key is held; charged spells fire on
 * release.
 * <p>
 * Bracelet rules, from the original: casts cost two thirds ({@link #DISCOUNT}) and cool down in half the time; the
 * terrasteel bracelet's volley casts ten times at once at triple cooldown, each cast at the original's 3.0 cost
 * modifier ({@link #VOLLEY_MODIFIER}) instead of its cap's.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class BraceletCasting {
    /** The original divided the cap's cost modifier by 1.5. */
    private static final float DISCOUNT = 2.0F / 3.0F;
    private static final int VOLLEY = 10;
    /** The original terrasteel bracelet's consumption modifier, replacing the cap's (Thaumaturge applies the cap's). */
    private static final float VOLLEY_MODIFIER = 3.0F;
    /** Players holding the cast key on a channelled or charged spell, with the game time they pressed it. */
    private static final Map<UUID, Long> HELD = new HashMap<>();
    /** The bracelet whose cast is running on this thread, read by {@link #discount}. */
    private static final ThreadLocal<ItemStack> CASTING = new ThreadLocal<>();

    private BraceletCasting() {
    }

    /**
     * The bracelet that casts: the first one in the entity's Curios slots, else one held in the off hand (the shield
     * slot), else empty. A bracelet in the main hand only shows on the wrist; it doesn't cast.
     */
    public static ItemStack worn(@Nullable LivingEntity entity) {
        if (entity == null) {
            return ItemStack.EMPTY;
        }
        ItemStack curio = curio(entity);
        if (!curio.isEmpty()) {
            return curio;
        }
        ItemStack off = entity.getOffhandItem();
        return off.getItem() instanceof CastingBracelet ? off : ItemStack.EMPTY;
    }

    /** The first bracelet in the entity's Curios slots, or empty. */
    public static ItemStack curio(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity)
                .flatMap(inv -> inv.findFirstCurio(stack -> stack.getItem() instanceof CastingBracelet))
                .map(SlotResult::stack).orElse(ItemStack.EMPTY);
    }

    /** Raise the caster's arm on every client that can see them. */
    private static void fx(ServerPlayer player, byte mode, ItemStack focus) {
        fx(player, mode, focus, false);
    }

    private static void fx(ServerPlayer player, byte mode, ItemStack focus, boolean mainHand) {
        int color = FocusItems.isFocus(focus) ? FocusItems.color(focus, player.registryAccess()) : 0xFFFFFF;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new BraceletFxPayload(player.getId(), mode, color, mainHand));
    }

    /**
     * Right-click with a bracelet in either hand: Thaumaturge's own wand use (casting, node tapping, blocks that react
     * to a wand), with the bracelet rules applied to any cast: two thirds of the vis, half the cooldown, and the
     * terrasteel volley for instant spells.
     */
    public static InteractionResultHolder<ItemStack> handUse(ServerPlayer player, InteractionHand hand, ItemStack bracelet,
                                                             Supplier<InteractionResultHolder<ItemStack>> wandUse) {
        CastingBracelet item = (CastingBracelet) bracelet.getItem();
        ItemStack focus = item.socketedFocus(bracelet);
        boolean main = hand == InteractionHand.MAIN_HAND;
        boolean instant = FocusItems.isFocus(focus) && !SpellCasting.channels(focus);
        if (instant && item.kind().volley() && !CasterManager.isOnCooldown(player)) {
            if (volley(player, bracelet, item, focus, hand)) {
                fx(player, BraceletFxPayload.BURST, focus, main);
            }
            return InteractionResultHolder.success(bracelet);
        }
        long before = cooldownEnd(player);
        InteractionResultHolder<ItemStack>[] result = new InteractionResultHolder[1];
        run(bracelet, () -> result[0] = wandUse.get());
        int cooldown = remaining(player, before);
        if (cooldown > 0) {
            setCooldown(player, bracelet, Math.max(1, cooldown / 2));
            fx(player, BraceletFxPayload.BURST, focus, main);
        } else if (player.isUsingItem() && FocusItems.isFocus(focus) && SpellCasting.channels(focus)) {
            fx(player, BraceletFxPayload.HOLD, focus, main);
        }
        return result[0];
    }

    /** A held bracelet's channelling pulse: the cast is priced as a bracelet's. */
    public static void handTick(ItemStack bracelet, Runnable wandTick) {
        run(bracelet, wandTick);
    }

    /** A held bracelet let go: a charged spell fires at bracelet prices and half cooldown; the arm comes down. */
    public static void handRelease(ServerPlayer player, ItemStack bracelet, boolean mainHand, Runnable wandRelease) {
        ItemStack focus = ((CastingBracelet) bracelet.getItem()).socketedFocus(bracelet);
        long before = cooldownEnd(player);
        run(bracelet, wandRelease);
        int cooldown = remaining(player, before);
        if (cooldown > 0) {
            setCooldown(player, bracelet, Math.max(1, cooldown / 2));
        }
        if (FocusItems.isFocus(focus) && SpellCasting.channels(focus)) {
            fx(player, BraceletFxPayload.RELEASE, focus, mainHand);
        }
    }

    public static void press(ServerPlayer player) {
        ItemStack bracelet = worn(player);
        if (!(bracelet.getItem() instanceof CastingBracelet item)) {
            return;
        }
        ItemStack focus = item.socketedFocus(bracelet);
        if (!FocusItems.isFocus(focus)) {
            player.displayClientMessage(Component.translatable("message.thaumicconcilium.bracelet_no_focus")
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), true);
            return;
        }
        // A worn bracelet behind a shield only casts shield foci (ShieldFoci).
        if (bracelet != player.getOffhandItem() && ShieldFoci.holdsShield(player)
                && !ShieldFoci.isShieldFocus(focus, player.registryAccess())) {
            player.displayClientMessage(Component.translatable("message.thaumicconcilium.bracelet_shield_busy")
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), true);
            return;
        }
        if (CasterManager.isOnCooldown(player)) {
            return;
        }
        if (SpellCasting.channels(focus)) {
            HELD.put(player.getUUID(), player.level().getGameTime());
            fx(player, BraceletFxPayload.HOLD, focus);
            return;
        }
        if (item.kind().volley()) {
            if (volley(player, bracelet, item, focus)) {
                fx(player, BraceletFxPayload.BURST, focus);
            }
        } else {
            int cooldown = castInstant(player, bracelet, item, focus);
            if (cooldown > 0) {
                setCooldown(player, bracelet, Math.max(1, cooldown / 2));
                fx(player, BraceletFxPayload.BURST, focus);
            }
        }
    }

    public static void release(ServerPlayer player) {
        Long start = HELD.remove(player.getUUID());
        ItemStack bracelet = worn(player);
        if (start == null || !(bracelet.getItem() instanceof CastingBracelet item)) {
            if (start != null) {
                fx(player, BraceletFxPayload.RELEASE, ItemStack.EMPTY);
            }
            return;
        }
        ItemStack focus = item.socketedFocus(bracelet);
        fx(player, BraceletFxPayload.RELEASE, focus);
        int used = (int) (player.level().getGameTime() - start);
        long before = cooldownEnd(player);
        run(bracelet, () -> SpellCasting.release(player.serverLevel(), player, bracelet, focus, used));
        Spell spell = Spells.spellOf(focus);
        if (spell != null) {
            ChannelEvents.channelEnded(player, spell, used);   // Warp Extraction's throw, Spotlight's overheat...
        }
        int cooldown = remaining(player, before);
        if (cooldown > 0) {
            setCooldown(player, bracelet, Math.max(1, cooldown / 2));
        }
    }

    /** Stops any held cast, as when the focus changes. */
    public static void cancel(ServerPlayer player) {
        if (HELD.remove(player.getUUID()) != null) {
            fx(player, BraceletFxPayload.RELEASE, ItemStack.EMPTY);
        }
    }

    /** One cast; returns the cooldown it started, or 0 if nothing was cast. */
    private static int castInstant(ServerPlayer player, ItemStack bracelet, CastingBracelet item, ItemStack focus) {
        return castInstant(player, bracelet, item, focus, InteractionHand.OFF_HAND);
    }

    private static int castInstant(ServerPlayer player, ItemStack bracelet, CastingBracelet item, ItemStack focus, InteractionHand hand) {
        long before = cooldownEnd(player);
        run(bracelet, () -> SpellCasting.use(player.level(), player, hand, bracelet, focus));
        return remaining(player, before);
    }

    /** Returns whether anything was cast. */
    private static boolean volley(ServerPlayer player, ItemStack bracelet, CastingBracelet item, ItemStack focus) {
        return volley(player, bracelet, item, focus, InteractionHand.OFF_HAND);
    }

    private static boolean volley(ServerPlayer player, ItemStack bracelet, CastingBracelet item, ItemStack focus, InteractionHand hand) {
        int first = 0;
        for (int i = 0; i < VOLLEY; i++) {
            CasterManager.setCooldown(player, 0);
            int cooldown = castInstant(player, bracelet, item, focus, hand);
            if (cooldown <= 0) {
                break;
            }
            if (first == 0) {
                first = cooldown;
            }
        }
        setCooldown(player, bracelet, first * 3);
        return first > 0;
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Long start = HELD.get(player.getUUID());
        if (start == null) {
            return;
        }
        ItemStack bracelet = worn(player);
        if (!player.isAlive() || !(bracelet.getItem() instanceof CastingBracelet item)) {
            cancel(player);
            return;
        }
        ItemStack focus = item.socketedFocus(bracelet);
        if (!SpellCasting.channels(focus)) {
            cancel(player);
            return;
        }
        ServerLevel level = player.serverLevel();
        int used = (int) (level.getGameTime() - start);
        run(bracelet, () -> SpellCasting.tick(level, player, bracelet, focus, used));
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        HELD.remove(event.getEntity().getUUID());
    }

    /** Bracelet casts cost two thirds; the volley's casts are re-priced from the cap's modifier to 3.0. */
    @SubscribeEvent
    public static void discount(SpellCastEvent.Pre event) {
        ItemStack bracelet = CASTING.get();
        if (bracelet == null || !(bracelet.getItem() instanceof CastingBracelet item)) {
            return;
        }
        if (item.kind().volley()) {
            float cap = Math.max(0.1F, WandVisHelper.getParts(bracelet).cap().baseCostModifier());
            event.setVisCost(event.visCost() * VOLLEY_MODIFIER / cap);
        } else {
            event.setVisCost(event.visCost() * DISCOUNT);
        }
    }

    /** The bracelet whose cast is running on this thread, or empty. */
    public static ItemStack casting() {
        ItemStack stack = CASTING.get();
        return stack == null ? ItemStack.EMPTY : stack;
    }

    private static void run(ItemStack bracelet, Runnable cast) {
        CASTING.set(bracelet);
        try {
            cast.run();
        } finally {
            CASTING.remove();
        }
    }

    private static long cooldownEnd(Player player) {
        return player.getData(TTAttachments.CASTER_COOLDOWN);
    }

    /** Ticks of cooldown started since {@code before}, or 0 if the cast set none. */
    private static int remaining(Player player, long before) {
        long after = cooldownEnd(player);
        return after == before ? 0 : (int) Math.max(0L, after - player.level().getGameTime());
    }

    private static void setCooldown(Player player, ItemStack bracelet, int ticks) {
        CasterManager.setCooldown(player, ticks);
        if (ticks > 0) {
            player.getCooldowns().addCooldown(bracelet.getItem(), ticks);
        } else {
            player.getCooldowns().removeCooldown(bracelet.getItem());
        }
    }
}
