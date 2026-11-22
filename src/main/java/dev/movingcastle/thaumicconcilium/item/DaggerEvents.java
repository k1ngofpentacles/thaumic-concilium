package dev.movingcastle.thaumicconcilium.item;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.effect.Bleeding;
import dev.movingcastle.thaumicconcilium.registry.TCEffects;
import dev.movingcastle.thaumicconcilium.registry.TCItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Crimson Blood and the stun's teeth. Blood: exactly one, guaranteed, when a crimson cultist (tag
 * {@code thaumicconcilium:crimson_cultists}) dies to the dagger's melee hit or to a bleed window; every other death gives
 * nothing (user decision). It is added at the drops event, so it follows the corpse and ignores looting and loot tables.
 * Stun: a stunned creature's damage is cancelled, so arrows and spells it still looses do nothing.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class DaggerEvents {
    /** Crimson Blood a dagger kill leaves. */
    public static final int BLOOD_PER_KILL = 1;

    private DaggerEvents() {
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide() || !dead.getType().is(CrimsonDagger.CRIMSON_CULTISTS)) {
            return;
        }
        boolean stab = event.getSource().getDirectEntity() instanceof Player killer && killer.getMainHandItem().is(TCItems.CRIMSON_DAGGER.get());
        if (stab || Bleeding.damaging()) {
            event.getDrops().add(new ItemEntity(dead.level(), dead.getX(), dead.getY(), dead.getZ(),
                    new ItemStack(TCItems.CRIMSON_BLOOD.get(), BLOOD_PER_KILL)));
        }
    }

    /**
     * A stunned mob cannot take a target. Clearing it in the effect tick alone is not enough: the target goals run later
     * in the same tick and set it again, so a stunned archer still aimed (the pack test caught it).
     */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() != null && event.getEntity().hasEffect(TCEffects.STUNNED)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker.hasEffect(TCEffects.STUNNED)) {
            event.setCanceled(true);
        }
    }
}
