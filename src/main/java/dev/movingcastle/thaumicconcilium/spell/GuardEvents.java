package dev.movingcastle.thaumicconcilium.spell;

import com.leclowndu93150.thaumaturge.registry.TTDamageTypeTags;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.registry.TCEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

/**
 * The one shield-block listener of the shield foci. {@code LivingShieldBlockEvent} fires on every hurt of every living entity,
 * so it returns at once unless the entity wears the Aegis effect or holds an armed Riposte.
 * <p>
 * Aegis comes first, so a block it forces counts for Riposte. It does exactly what Thaumaturge's {@code ShieldEvents.onShieldBlock}
 * does for its warding shields (a hit the shield did not stop is stopped, at double shield wear), but for every source with an
 * attacker or in {@code thaumaturge:warded_by_shields}, from any side, and only while the shield is raised. Never fall, fire,
 * drowning, starvation or the void. Runs after Thaumaturge's own listener (low priority).
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class GuardEvents {
    private GuardEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        LivingEntity who = event.getEntity();
        if (who.level().isClientSide) {
            return;
        }
        boolean aegis = who.hasEffect(TCEffects.AEGIS);
        boolean armed = Riposte.isArmed(who);
        if (!aegis && !armed) {
            return;
        }
        if (aegis) {
            force(event, who);
        }
        if (armed && who instanceof ServerPlayer player) {
            Riposte.release(event, player);
        }
    }

    private static void force(LivingShieldBlockEvent event, LivingEntity who) {
        if (event.getOriginalBlock() || !who.isBlocking() || event.getBlockedDamage() <= 0.0F) {
            return;
        }
        var source = event.getDamageSource();
        if (source.getEntity() != null || source.is(TTDamageTypeTags.WARDED_BY_SHIELDS)) {
            event.setBlocked(true);
            event.setShieldDamage(event.getBlockedDamage() * 2.0F);
            GuardSync.aegisBlock(who, source.getSourcePosition());
            // Thaumaturge's runic shield sound, as its own RunicShieldingEvents plays it
            who.level().playSound(null, who.blockPosition(), com.leclowndu93150.thaumaturge.registry.TTSounds.RUNICSHIELDEFFECT.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.66F, 1.1F + who.getRandom().nextFloat() * 0.1F);
        }
    }
}
