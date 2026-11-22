package dev.movingcastle.thaumicconcilium.world;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.network.EnslavePayload;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The enslave flag (original: PacketEnslave and ClientEvents.isEnslaved). The server owns the flag (player persistent data, nothing
 * per tick) and tells the client, which walks itself forward ({@code client/EnslaveClient}) while it is set and an entity of type
 * tag {@link #ENSLAVERS} is within 32 blocks. Reusable: the Pontifex sets it now, the Vengeful Golem will later (add its type to the tag).
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class Enslave {
    public static final TagKey<EntityType<?>> ENSLAVERS = TagKey.create(Registries.ENTITY_TYPE, ThaumicConcilium.id("enslavers"));
    private static final String KEY = ThaumicConcilium.MODID + "_enslaved";

    private Enslave() {
    }

    public static boolean isEnslaved(Player player) {
        return player.getPersistentData().getBoolean(KEY);
    }

    /** Sets or clears the flag; tells the client only when it changed. */
    public static void set(ServerPlayer player, boolean enslaved) {
        if (isEnslaved(player) == enslaved) {
            return;
        }
        if (enslaved) {
            player.getPersistentData().putBoolean(KEY, true);
        } else {
            player.getPersistentData().remove(KEY);
        }
        send(player, enslaved);
    }

    private static void send(ServerPlayer player, boolean enslaved) {
        if (!(player instanceof FakePlayer)) {
            PacketDistributor.sendToPlayer(player, new EnslavePayload(enslaved));
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            set(player, false);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isEnslaved(player)) {
            send(player, true);
        }
    }
}
