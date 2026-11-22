package dev.movingcastle.thaumicconcilium.world;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The state of the Paladin's Ethereal Shackles on a player (original: TCPlayerCapabilities.chainedTime): while chained a player
 * can not use items, place or break blocks, or hit and use things. The remaining ticks live in {@link CrimsonState} ({@code chainedTime}).
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class Chained {
    private Chained() {
    }

    /** Sets the remaining chained ticks (the state counts down on both sides, see {@link Ethereal#tick}). */
    public static void chain(Player player, int ticks) {
        CrimsonState.set(player, CrimsonState.get(player).withChained(ticks));
    }

    public static boolean isChained(Player player) {
        return CrimsonState.get(player).chainedTime() > 0;
    }

    public static void free(Player player) {
        CrimsonState.set(player, CrimsonState.get(player).withChained(0));
    }

    @SubscribeEvent
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        cancelIfChained(event, event.getEntity());
    }

    @SubscribeEvent
    public static void rightClickItem(PlayerInteractEvent.RightClickItem event) {
        cancelIfChained(event, event.getEntity());
    }

    @SubscribeEvent
    public static void leftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        cancelIfChained(event, event.getEntity());
    }

    @SubscribeEvent
    public static void entityInteract(PlayerInteractEvent.EntityInteract event) {
        cancelIfChained(event, event.getEntity());
    }

    @SubscribeEvent
    public static void attack(AttackEntityEvent event) {
        if (isChained(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void useItem(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player player && isChained(player)) {
            event.setCanceled(true);
        }
    }

    private static void cancelIfChained(net.neoforged.bus.api.ICancellableEvent event, Player player) {
        if (isChained(player)) {
            event.setCanceled(true);
        }
    }
}
