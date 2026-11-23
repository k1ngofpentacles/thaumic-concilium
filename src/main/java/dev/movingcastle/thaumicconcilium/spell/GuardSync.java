package dev.movingcastle.thaumicconcilium.spell;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.network.GuardFxPayload;
import dev.movingcastle.thaumicconcilium.registry.TCEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Tells clients what the shield foci's look needs (docs/design/shield-foci-look.md section 2): one {@link GuardFxPayload} per
 * event, to the entity's trackers and itself. Nothing ticks. Vanilla sends a mob effect only to its holder, so the Aegis is
 * announced here; a client that starts tracking an armed or warded entity later is caught up from {@code StartTracking}.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class GuardSync {
    private GuardSync() {
    }

    /** Tests listen here (a fake player has no connection); null otherwise. */
    public static java.util.function.Consumer<GuardFxPayload> tap;

    static void send(Entity about, GuardFxPayload payload) {
        if (tap != null) {
            tap.accept(payload);
        }
        if (!about.level().isClientSide) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(about, payload);
        }
    }

    static void armed(ServerPlayer player, int charges, long until) {
        send(player, GuardFxPayload.of(player.getId(), GuardFxPayload.ARMED, charges, until));
    }

    static void released(ServerPlayer player, int charges, long until, Vec3 toAttacker) {
        Vec3 d = unit(toAttacker, player);
        send(player, new GuardFxPayload(player.getId(), GuardFxPayload.RELEASED, charges, until, (float) d.x, (float) d.y, (float) d.z));
    }

    static void disarmed(ServerPlayer player) {
        send(player, GuardFxPayload.of(player.getId(), GuardFxPayload.DISARMED, 0, 0L));
    }

    /** The Aegis forced a block of a hit that came from {@code from} (null: unknown, shown where the holder faces). */
    static void aegisBlock(LivingEntity who, Vec3 from) {
        Vec3 d = unit(from == null ? null : from.subtract(who.getBoundingBox().getCenter()), who);
        send(who, new GuardFxPayload(who.getId(), GuardFxPayload.AEGIS_BLOCK, 0, 0L, (float) d.x, (float) d.y, (float) d.z));
    }

    private static Vec3 unit(Vec3 v, LivingEntity fallbackFacing) {
        if (v == null || v.lengthSqr() < 1.0E-6) {
            return Vec3.directionFromRotation(0.0F, fallbackFacing.getYRot());
        }
        return v.normalize();
    }

    private static long aegisUntil(LivingEntity who, MobEffectInstance effect) {
        return effect.isInfiniteDuration() ? Long.MAX_VALUE : who.level().getGameTime() + effect.getDuration();
    }

    @SubscribeEvent
    public static void effectAdded(MobEffectEvent.Added event) {
        MobEffectInstance added = event.getEffectInstance();
        if (added == null || !added.is(TCEffects.AEGIS) || event.getEntity().level().isClientSide) {
            return;
        }
        LivingEntity who = event.getEntity();
        long until = aegisUntil(who, added);
        MobEffectInstance old = event.getOldEffectInstance();
        if (old != null) {
            until = Math.max(until, aegisUntil(who, old));   // a shorter one does not shorten the one held
        }
        send(who, GuardFxPayload.of(who.getId(), GuardFxPayload.AEGIS_ON, 0, until));
    }

    @SubscribeEvent
    public static void effectRemoved(MobEffectEvent.Remove event) {
        if (event.getEffect() == null || !event.getEffect().is(TCEffects.AEGIS) || event.getEntity().level().isClientSide) {
            return;
        }
        send(event.getEntity(), GuardFxPayload.of(event.getEntity().getId(), GuardFxPayload.AEGIS_OFF, 0, 0L));
    }

    /** A player who comes into view of an armed or warded entity learns its state (the earlier messages never reached them). */
    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer watcher) || !(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        MobEffectInstance aegis = target.getEffect(TCEffects.AEGIS);
        if (aegis != null) {
            PacketDistributor.sendToPlayer(watcher, GuardFxPayload.of(target.getId(), GuardFxPayload.AEGIS_ON, 0, aegisUntil(target, aegis)));
        }
        if (target instanceof ServerPlayer armed && Riposte.isArmed(armed)) {
            GuardFxPayload state = Riposte.state(armed);
            if (state != null) {
                PacketDistributor.sendToPlayer(watcher, state);
            }
        }
    }
}
