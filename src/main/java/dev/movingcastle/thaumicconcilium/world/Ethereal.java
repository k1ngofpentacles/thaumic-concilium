package dev.movingcastle.thaumicconcilium.world;

import com.leclowndu93150.thaumaturge.content.wands.ItemWand;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.item.PontifexRobe;
import dev.movingcastle.thaumicconcilium.item.RiftGem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The ethereal state (original: TCEntityEventHandler:683-713, :304-312 and the flesh gains at :250, :316, :339, ClientEvents:439):
 * an ethereal player flies through blocks (no-clip, vertical velocity zeroed unless sneaking), takes no damage and heals nothing,
 * and every non-wand use, interaction or attack costs a point of flesh (health held back, see {@link CrimsonState}). Creative-style
 * damage (kill, void) adds flesh too. The block-overlay cancel is client side ({@code client/EtherealClient}).
 * <p>Void damage is cancelled too, as in the original (decided with the user: a deliberate escape).
 * No idle cost: {@link #tick} returns at once for a player with no crimson state.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class Ethereal {
    /** Flesh stops coming once this many protolimbs are held (original: {@code protolimbs < 6}; phase 8 fills the count). */
    public static final int PROTOLIMB_LIMIT = 6;

    private Ethereal() {
    }

    /** No-clip while ethereal, on both sides: sets the flag and holds the player level unless they are sneaking. */
    public static void flight(Player player) {
        if (!CrimsonState.isEthereal(player) || (player.isCreative() && player.getAbilities().flying)) {
            return;
        }
        player.noPhysics = true;
        if (!player.isShiftKeyDown()) {
            player.setDeltaMovement(player.getDeltaMovement().multiply(1.0, 0.0, 1.0));
        }
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!CrimsonState.active(player)) {
            return;
        }
        CrimsonState.State s = CrimsonState.get(player);
        if (player instanceof ServerPlayer server && s.pontifexRobeToggle()) {
            if (!PontifexRobe.isFullSet(server)) {
                // original TCEntityEventHandler:461-464: robe off while toggled clears both flags
                CrimsonState.set(server, s.withToggle(false).withEthereal(false));
                s = CrimsonState.get(player);
            } else if (s.chainedTime() <= PontifexToggle.CHAIN_TICKS - 20) {
                CrimsonState.set(server, s.withChained(PontifexToggle.CHAIN_TICKS));
                s = CrimsonState.get(player);
            }
        }
        if (s.chainedTime() > 0) {
            // both sides count down their own copy; the server syncs only when it sets the value
            player.setData(CrimsonState.ATTACHMENT, s.withChained(s.chainedTime() - 1));
        }
        flight(player);
    }

    @SubscribeEvent
    public static void damage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.isCreative() || !CrimsonState.isEthereal(player)) {
            return;
        }
        // Void damage is cancelled like the rest (and adds flesh, being in the bypass tag): an ethereal player can fall forever.
        // The user keeps this on purpose, as the original had it: a PvP "blink" to escape and decide.
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            gainFlesh(player);
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void heal(LivingHealEvent event) {
        if (event.getEntity() instanceof Player player && CrimsonState.isEthereal(player)) {
            event.setCanceled(true);
        }
    }

    /** One point of flesh for a non-wand action while ethereal (server, survival, below the protolimb limit). */
    public static void gainFlesh(Player player) {
        if (player instanceof ServerPlayer && !player.isCreative()) {
            CrimsonState.State s = CrimsonState.get(player);
            if (s.ethereal() && s.protolimbs() < PROTOLIMB_LIMIT) {
                CrimsonState.set(player, s.withFlesh(s.fleshAmount() + 1));
            }
        }
    }

    private static boolean wandOrGem(ItemStack stack) {
        return stack.getItem() instanceof ItemWand || stack.getItem() instanceof RiftGem;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        interact(event.getEntity(), event.getHand(), event.getItemStack());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void rightClickItem(PlayerInteractEvent.RightClickItem event) {
        interact(event.getEntity(), event.getHand(), event.getItemStack());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void entityInteract(PlayerInteractEvent.EntityInteract event) {
        interact(event.getEntity(), event.getHand(), event.getItemStack());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void useItem(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player player && !wandOrGem(event.getItem())) {
            gainFlesh(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void attack(AttackEntityEvent event) {
        if (!wandOrGem(event.getEntity().getMainHandItem())) {
            gainFlesh(event.getEntity());
        }
    }

    private static void interact(Player player, InteractionHand hand, ItemStack held) {
        if (hand == InteractionHand.MAIN_HAND && !wandOrGem(held)) {
            gainFlesh(player);
        }
    }
}
