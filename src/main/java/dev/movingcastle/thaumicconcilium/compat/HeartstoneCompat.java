package dev.movingcastle.thaumicconcilium.compat;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Heartstone (optional; Supplementaries Team License, compiled against only): the partner a player is bound to, if any. A partner is found online
 * through the mod itself, and otherwise (offline) through {@link HeartstoneRegistry}, the other player seen with the same Heartstone id. The real
 * calls live in {@link Real}, loaded only when the mod is present. {@link #resolver} and {@link #scanner} are test hooks.
 */
@EventBusSubscriber(modid = "thaumicconcilium")
public final class HeartstoneCompat {
    /** Test hook: when set, answers instead of Heartstone and the registry (so a test needs no real Heartstone). */
    @Nullable
    public static volatile Function<ServerPlayer, UUID> resolver;
    /** Test hook: when set, replaces the scan of a player's Heartstones (their ids). */
    @Nullable
    public static volatile Function<ServerPlayer, Collection<Long>> scanner;

    private HeartstoneCompat() {
    }

    private static boolean loaded() {
        return ModList.get().isLoaded("heartstone");
    }

    /** The bound partner of a player (any dimension, online or not), or null. */
    @Nullable
    public static UUID partnerOf(ServerPlayer player) {
        Function<ServerPlayer, UUID> hook = resolver;
        if (hook != null) {
            return hook.apply(player);
        }
        if (loaded()) {
            UUID online = Real.partnerOf(player);
            if (online != null) {
                return online;
            }
        }
        MinecraftServer server = player.getServer();
        return server == null ? null : HeartstoneRegistry.get(server).partnerOf(player.getUUID());
    }

    /** The partner of a player who is not online, from the registry alone. */
    @Nullable
    public static UUID partnerOfOffline(MinecraftServer server, UUID player) {
        return HeartstoneRegistry.get(server).partnerOf(player);
    }

    /** Notes the Heartstone ids this player carries in the registry. Returns how many ids were recorded. */
    public static int recordHeld(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return 0;
        }
        Collection<Long> ids;
        Function<ServerPlayer, Collection<Long>> hook = scanner;
        if (hook != null) {
            ids = hook.apply(player);
        } else if (loaded()) {
            ids = Real.idsOf(player);
        } else {
            return 0;
        }
        HeartstoneRegistry registry = HeartstoneRegistry.get(server);
        for (Long id : ids) {
            registry.record(player.getUUID(), id);
        }
        return ids.size();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            recordHeld(player);
        }
    }

    private static final class Real {
        @Nullable
        static UUID partnerOf(ServerPlayer player) {
            for (net.minecraft.world.item.ItemStack stack : net.mehvahdjukaar.heartstone.HeartstoneItem.getAllHeartstones(player)) {
                // false: the partner need not be in the same dimension
                net.minecraft.world.entity.player.Player partner = net.mehvahdjukaar.heartstone.HeartstoneItem.getBoundPlayer(player, stack, false);
                if (partner != null) {
                    return partner.getUUID();
                }
            }
            return null;
        }

        static Collection<Long> idsOf(ServerPlayer player) {
            java.util.List<Long> ids = new java.util.ArrayList<>();
            for (net.minecraft.world.item.ItemStack stack : net.mehvahdjukaar.heartstone.HeartstoneItem.getAllHeartstones(player)) {
                Long id = net.mehvahdjukaar.heartstone.HeartstoneItem.getHeartstoneId(stack);
                if (id != null) {
                    ids.add(id);
                }
            }
            return ids;
        }
    }
}
