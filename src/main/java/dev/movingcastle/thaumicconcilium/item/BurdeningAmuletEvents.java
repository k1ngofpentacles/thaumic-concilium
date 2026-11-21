package dev.movingcastle.thaumicconcilium.item;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Port addition: the Burdening Amulet turns temporary warp into Thaumaturge's Death Gaze, which withers every creature
 * in sight and turns mobs on the wearer. Since the amulet's other power is commanding your pets, the creatures you own
 * are spared while you wear it: no Wither, and never made to target you.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class BurdeningAmuletEvents {
    private BurdeningAmuletEvents() {
    }

    private static boolean ownerWearsAmulet(LivingEntity entity) {
        return entity instanceof OwnableEntity owned && owned.getOwner() instanceof Player owner && wears(owner);
    }

    private static boolean wears(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .map(inv -> inv.isEquipped(stack -> stack.getItem() instanceof BurdeningAmulet))
                .orElse(false);
    }

    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Applicable event) {
        // Death Gaze applies exactly Wither I for 80 ticks (WarpEvents.checkDeathGaze); other Wither sources still land.
        var effect = event.getEffectInstance();
        if (effect.is(MobEffects.WITHER) && effect.getDuration() == 80 && effect.getAmplifier() == 0
                && ownerWearsAmulet(event.getEntity())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target instanceof Player player && event.getEntity() instanceof OwnableEntity owned
                && owned.getOwner() == player && wears(player)) {
            event.setCanceled(true);
        }
    }
}
