package dev.movingcastle.thaumicconcilium.world;

import com.leclowndu93150.thaumaturge.content.entity.EntityCultist;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.mob.CrimsonPontifex;
import dev.movingcastle.thaumicconcilium.item.PontifexRobe;
import dev.movingcastle.thaumicconcilium.network.PontifexStatePayload;
import dev.movingcastle.thaumicconcilium.registry.TCSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Pontifex's robe in use (original: PacketTogglePontifexRobe, PontifexRobe.onArmorTick, TCEntityEventHandler:181, :461, :682-710).
 * The server owns the toggle (player persistent data, nothing per tick while it is off). Wearing the full set and pressing the toggle key
 * lets go of material wholeness: the wearer phases through blocks without falling, takes no damage and heals nothing, and is held
 * {@link Chained} (no using, placing, striking) while it lasts. Taking a piece off, dying or toggling again ends it. Cultists that are not
 * the Pontifex never aim at a wearer of the full set.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class PontifexToggle {
    private static final String KEY = ThaumicConcilium.MODID + "_pontifex_toggle";
    /** Original: chainedTime = 100 each armour tick while toggled. */
    public static final int CHAIN_TICKS = 100;

    /** The local player's copy, set by {@link PontifexStatePayload}; only read on the client. */
    public static volatile boolean clientOn;

    private PontifexToggle() {
    }

    public static boolean isOn(Player player) {
        return player.level().isClientSide() ? clientOn : player.getPersistentData().getBoolean(KEY);
    }

    /** What the toggle key asks for. False (and nothing changes) unless the full set is worn. */
    public static boolean toggle(ServerPlayer player) {
        if (!PontifexRobe.isFullSet(player)) {
            return false;
        }
        set(player, !isOn(player));
        return true;
    }

    public static void set(ServerPlayer player, boolean on) {
        if (player.getPersistentData().getBoolean(KEY) == on) {
            return;
        }
        if (on) {
            player.getPersistentData().putBoolean(KEY, true);
            Chained.chain(player, CHAIN_TICKS);
            player.level().playSound(null, player.blockPosition(), TCSounds.SHACKLES.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        } else {
            player.getPersistentData().remove(KEY);
            player.noPhysics = false;
        }
        send(player, on);
    }

    private static void send(ServerPlayer player, boolean on) {
        if (!(player instanceof FakePlayer)) {
            PacketDistributor.sendToPlayer(player, new PontifexStatePayload(on));
        }
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!isOn(player)) {
            return;
        }
        if (player instanceof ServerPlayer server) {
            if (!PontifexRobe.isFullSet(server)) {
                set(server, false);
                return;
            }
            if (server.tickCount % 20 == 0) {
                Chained.chain(server, CHAIN_TICKS);
            }
        }
        if (!player.isCreative() || !player.getAbilities().flying) {
            player.noPhysics = true;
            if (!player.isShiftKeyDown()) {
                player.setDeltaMovement(player.getDeltaMovement().multiply(1.0, 0.0, 1.0));
            }
        }
    }

    @SubscribeEvent
    public static void damage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && !player.isCreative() && isOn(player)
                && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void heal(LivingHealEvent event) {
        if (event.getEntity() instanceof Player player && isOn(player)) {
            event.setCanceled(true);
        }
    }

    /** Original TCEntityEventHandler:181: a cultist drops its target when that is a player in the full set. */
    @SubscribeEvent
    public static void aggro(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof EntityCultist && !(event.getEntity() instanceof CrimsonPontifex)
                && event.getNewAboutToBeSetTarget() instanceof Player player && PontifexRobe.isFullSet(player)) {
            event.setNewAboutToBeSetTarget(null);
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
        if (event.getEntity() instanceof ServerPlayer player && player.getPersistentData().getBoolean(KEY)) {
            send(player, true);
        }
    }
}
