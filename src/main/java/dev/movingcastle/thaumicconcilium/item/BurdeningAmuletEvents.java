package dev.movingcastle.thaumicconcilium.item;

import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Gaze veils (port addition, agreed with the user 2026-11-21): something covering the face, a carved pumpkin or anything
 * in {@code thaumicconcilium:gaze_veils}, blinds Death Gaze. As in the original, the Burdening Amulet turns temporary
 * warp into Death Gaze (which withers every creature you look at and turns it on you, your own pets included) while
 * your pets attack whatever you look at. Veiled, the gaze can't land, so the amulet still burns your warp and commands
 * your pets, and they do the fighting.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class BurdeningAmuletEvents {
    public static final TagKey<Item> GAZE_VEILS = ItemTags.create(ThaumicConcilium.id("gaze_veils"));

    private BurdeningAmuletEvents() {
    }

    public static boolean veiled(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(GAZE_VEILS);
    }

    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().is(TTMobEffects.DEATH_GAZE) && event.getEntity() instanceof Player player && veiled(player)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /** Putting a veil on ends a gaze already running. */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide() && player.tickCount % 10 == 0 && player.hasEffect(TTMobEffects.DEATH_GAZE)
                && veiled(player)) {
            player.removeEffect(TTMobEffects.DEATH_GAZE);
        }
    }
}
