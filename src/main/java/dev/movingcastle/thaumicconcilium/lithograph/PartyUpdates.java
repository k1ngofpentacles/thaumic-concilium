package dev.movingcastle.thaumicconcilium.lithograph;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.item.AstralMonitor;
import dev.movingcastle.thaumicconcilium.network.PartyStatusPayload;
import dev.movingcastle.thaumicconcilium.registry.TCDataComponents;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The server half of both monitors (original: {@code TCEntityEventHandler} 488-650). Every 10 ticks a player holding an
 * Astral Monitor in the main hand gets a fresh reading of the watched player written into it, and a lithographed
 * (and not relieved) player is sent the stats of their four monitored players. Both write or send only when the
 * numbers changed, so a quiet party costs no network traffic and no inventory resync.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class PartyUpdates {
    private static final int PERIOD = 10;
    /** The last party each client was sent. */
    private static final Map<UUID, List<PartyStats>> SENT = new ConcurrentHashMap<>();

    private PartyUpdates() {
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer || player.tickCount % PERIOD != 0) {
            return;
        }
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (held.getItem() instanceof AstralMonitor) {
            updateMonitor(player.serverLevel(), held);
        }
        Lithograph.State state = Lithograph.get(player);
        if (state.hudVisible()) {
            updateParty(player, state);
        } else if (!SENT.isEmpty()) {
            // Switched off (or never on): resend in full when it comes back.
            SENT.remove(player.getUUID());
        }
    }

    private static void updateMonitor(ServerLevel level, ItemStack monitor) {
        Optional<UUID> target = AstralMonitor.target(monitor);
        if (target.isEmpty() || !(level.getPlayerByUUID(target.get()) instanceof ServerPlayer watched)) {
            return;
        }
        PartyStats stats = PartyStats.of(watched);
        if (!stats.equals(monitor.get(TCDataComponents.MONITOR_STATS.get()))) {
            monitor.set(TCDataComponents.MONITOR_STATS.get(), stats);
        }
    }

    private static void updateParty(ServerPlayer player, Lithograph.State state) {
        List<PartyStats> party = new ArrayList<>(Lithograph.MAX_MONITORED);
        for (int i = 0; i < Lithograph.MAX_MONITORED; i++) {
            party.add(PartyStats.of(player.serverLevel(), i < state.monitored().size() ? playerOf(state.monitored().get(i)) : null));
        }
        if (!party.equals(SENT.get(player.getUUID()))) {
            SENT.put(player.getUUID(), party);
            PacketDistributor.sendToPlayer(player, new PartyStatusPayload(party));
        }
    }

    /** The player a target descriptor names; other kinds (later phases) and empty slots give none. */
    @Nullable
    private static UUID playerOf(String target) {
        if (!target.startsWith(Lithograph.PLAYER_KIND)) {
            return null;
        }
        try {
            return UUID.fromString(target.substring(Lithograph.PLAYER_KIND.length()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().getUUID());
    }
}
