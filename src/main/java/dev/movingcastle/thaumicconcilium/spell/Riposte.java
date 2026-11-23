package dev.movingcastle.thaumicconcilium.spell;

import com.leclowndu93150.thaumaturge.api.spell.Spells;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellContinuation;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellStats;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.leclowndu93150.thaumaturge.api.spell.event.SpellCastEvent;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Riposte's armed state and release (design: docs/design/shield-foci-design.md section 2).
 * <p>
 * The delivery part ({@code RiposteDelivery}) calls {@link #arm}: the spell's continuation waits in a server-side map keyed by
 * the caster, with a number of charges and a game-time expiry. Nothing ticks: the expiry is checked lazily when the shield
 * blocks, and the entry is dropped on logout, death and server stop. {@link GuardEvents} calls {@link #release} from the one
 * {@code LivingShieldBlockEvent} listener, after Aegis.
 * <p>
 * A hit releases one charge when the shield blocked it, within {@code riposte.parryTicks} of being raised (the parry), the
 * attacker can be resolved (the direct attacker; a projectile's owner within {@code riposte.range}), and no charge was
 * already released in this raise. The continuation runs at power x1.5 on the attacker and the shield loses one extra
 * durability. Casting without a shield in the off hand is refused before any vis is paid.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class Riposte {
    /** A volley (ten casts) adds a charge per cast, up to this many. */
    public static final int MAX_CHARGES = 10;
    public static final float PARRY_POWER = 1.5F;

    private static final Map<UUID, Armed> ARMED = new HashMap<>();

    /** The spell waiting in a shield. {@code lastRaise} is the game tick the raise that last released began (-1: none yet). */
    private static final class Armed {
        SpellContinuation continuation;
        int charges;
        int windowTicks;
        long expiresAt;
        Object volley;
        long lastRaise = -1;
    }

    private Riposte() {
    }

    // ------------------------------------------------------------------------------ arming

    /**
     * Stores the spell. A new arming replaces the old one, except casts of the same volley (same non-null token), which add a
     * charge each up to {@link #MAX_CHARGES}. Returns the charges now held.
     */
    public static int arm(ServerPlayer player, SpellContinuation continuation, int windowTicks, Object volleyToken) {
        long now = player.level().getGameTime();
        Armed held = ARMED.get(player.getUUID());
        if (held != null && volleyToken != null && held.volley == volleyToken && now < held.expiresAt) {
            held.charges = Math.min(MAX_CHARGES, held.charges + 1);
            held.expiresAt = now + held.windowTicks;
            GuardSync.armed(player, held.charges, held.expiresAt);
            return held.charges;
        }
        Armed armed = new Armed();
        armed.continuation = continuation;
        armed.charges = 1;
        armed.windowTicks = Math.max(1, windowTicks);
        armed.expiresAt = now + armed.windowTicks;
        armed.volley = volleyToken;
        ARMED.put(player.getUUID(), armed);
        play(player, TTSounds.RUNICSHIELDCHARGE.get(), 0.8F);
        GuardSync.armed(player, 1, armed.expiresAt);
        return 1;
    }

    /** Charges held by a player whose arming has not run out, else 0. */
    public static int charges(ServerPlayer player) {
        Armed armed = ARMED.get(player.getUUID());
        return armed == null || player.level().getGameTime() >= armed.expiresAt ? 0 : armed.charges;
    }

    public static boolean anyArmed() {
        return !ARMED.isEmpty();
    }

    public static boolean isArmed(LivingEntity entity) {
        return !ARMED.isEmpty() && ARMED.containsKey(entity.getUUID());
    }

    /** The armed state as the message a client is told it by (null: not armed, or run out). */
    static dev.movingcastle.thaumicconcilium.network.GuardFxPayload state(ServerPlayer player) {
        Armed armed = ARMED.get(player.getUUID());
        if (armed == null || player.level().getGameTime() >= armed.expiresAt) {
            return null;
        }
        return dev.movingcastle.thaumicconcilium.network.GuardFxPayload.of(player.getId(),
                dev.movingcastle.thaumicconcilium.network.GuardFxPayload.ARMED, armed.charges, armed.expiresAt);
    }

    public static void clear(UUID id) {
        ARMED.remove(id);
    }

    public static void clearAll() {
        ARMED.clear();
    }

    /**
     * QA and render-harness scaffolding ({@code /thaumicconcilium qa guard ...}): {@code arm} holds {@code n} charges of an empty
     * spell for 30 seconds; {@code release} and {@code aegis_block} send the look's messages toward where the player faces
     * (nothing is cast or blocked).
     */
    public static void qa(ServerPlayer player, String what, int n) {
        Vec3 look = player.getLookAngle();
        switch (what) {
            case "arm" -> {
                Object token = new Object();
                SpellContinuation empty = new SpellContinuation(List.of(), com.leclowndu93150.thaumaturge.api.spell.cast.SpellState.DEFAULT,
                        java.util.Optional.of(player.getUUID()), UUID.randomUUID());
                for (int i = 0; i < Math.max(1, n); i++) {
                    arm(player, empty, 600, token);
                }
            }
            case "release" -> GuardSync.released(player, Math.max(0, charges(player) - 1), player.level().getGameTime() + 600, look);
            case "aegis_block" -> GuardSync.aegisBlock(player, player.getBoundingBox().getCenter().add(look.scale(3.0)));
            default -> {
                ARMED.remove(player.getUUID());
                GuardSync.disarmed(player);
            }
        }
    }

    // ------------------------------------------------------------------------------ release

    /** Called for every shield-block event of an armed player (the caller has already returned for everyone else). */
    static void release(LivingShieldBlockEvent event, ServerPlayer player) {
        Armed armed = ARMED.get(player.getUUID());
        if (armed == null) {
            return;
        }
        long now = player.level().getGameTime();
        if (now >= armed.expiresAt) {
            ARMED.remove(player.getUUID());   // ran out unspent: the vis is lost
            GuardSync.disarmed(player);
            return;
        }
        if (!event.getBlocked()) {
            return;
        }
        int raised = player.getTicksUsingItem();
        if (!player.isUsingItem() || raised > TCConfig.RIPOSTE_PARRY_TICKS.get()) {
            return;
        }
        long raiseStart = now - raised;
        if (armed.lastRaise == raiseStart) {
            return;   // one release per raise
        }
        LivingEntity attacker = attacker(event.getDamageSource(), player);
        if (attacker == null) {
            return;
        }
        armed.lastRaise = raiseStart;
        armed.expiresAt = now + armed.windowTicks;
        if (--armed.charges <= 0) {
            ARMED.remove(player.getUUID());
        }
        SpellContinuation base = armed.continuation;
        SpellContinuation parried = new SpellContinuation(base.nodes(), base.state().multiply(SpellStats.POWER, PARRY_POWER),
                base.caster(), base.castId());
        Vec3 direction = attacker.getBoundingBox().getCenter().subtract(player.getEyePosition());
        event.setShieldDamage(event.shieldDamage() + 1.0F);
        play(player, TTSounds.RUNICSHIELDEFFECT.get(), 1.0F);
        GuardSync.released(player, Math.max(0, armed.charges), armed.expiresAt, direction);
        Spells.resume(player.serverLevel(), parried, List.of(SpellTarget.entity(attacker, direction)));
    }

    /** The creature to strike: the melee attacker, or a projectile's owner within range; null if none can be reached. */
    private static LivingEntity attacker(DamageSource source, ServerPlayer player) {
        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile projectile) {
            if (projectile.getOwner() instanceof LivingEntity owner && owner != player && owner.isAlive()
                    && owner.distanceTo(player) <= TCConfig.RIPOSTE_RANGE.get()) {
                return owner;
            }
            return null;
        }
        if (direct instanceof LivingEntity living && living != player && living.isAlive()) {
            return living;
        }
        return null;
    }

    private static void play(ServerPlayer player, SoundEvent sound, float volume) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, 1.0F);
    }

    // ------------------------------------------------------------------------------ rules and cleanup

    /** Riposte needs a shield in the off hand: refused before any vis is paid. */
    @SubscribeEvent
    public static void onCast(SpellCastEvent.Pre event) {
        if (SpellTrees.contains(event.spell(), SpellTrees.part("riposte"))
                && !(event.caster() instanceof ServerPlayer p && ShieldFoci.holdsShield(p))) {
            event.setCanceled(true);
            if (event.caster() instanceof ServerPlayer player) {
                player.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("message.thaumicconcilium.riposte_needs_shield")
                        .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC)));
            }
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!ARMED.isEmpty()) {
            ARMED.remove(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void died(LivingDeathEvent event) {
        if (!ARMED.isEmpty() && event.getEntity() instanceof ServerPlayer player && ARMED.remove(player.getUUID()) != null) {
            GuardSync.disarmed(player);
        }
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        ARMED.clear();
    }
}
