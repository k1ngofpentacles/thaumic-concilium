package dev.movingcastle.thaumicconcilium.spell;

import com.leclowndu93150.thaumaturge.api.casters.FocusCaster;
import com.leclowndu93150.thaumaturge.api.spell.Spell;
import com.leclowndu93150.thaumaturge.api.spell.Spells;
import com.leclowndu93150.thaumaturge.content.casters.CasterManager;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.ConcentratedWarpCharge;
import dev.movingcastle.thaumicconcilium.spell.effect.EtherealManipulatorEffect;
import dev.movingcastle.thaumicconcilium.spell.effect.WarpExtractionEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import org.jetbrains.annotations.Nullable;

/**
 * What happens when a channel ends, for the parts whose original foci reacted to the player letting go:
 * Spotlight overheats (cooldown of one second plus a tenth of a second per tick channelled), Warp Extraction hurls
 * its charge, and the Ethereal Manipulator's wheel-adjusted distance resets.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class ChannelEvents {
    private ChannelEvents() {
    }

    @Nullable
    public static Spell channelledSpell(ItemStack caster) {
        if (caster.getItem() instanceof FocusCaster focusCaster) {
            ItemStack focus = focusCaster.socketedFocus(caster);
            return focus.isEmpty() ? null : Spells.spellOf(focus);
        }
        return null;
    }

    @SubscribeEvent
    public static void onStop(LivingEntityUseItemEvent.Stop event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        Spell spell = channelledSpell(event.getItem());
        if (spell == null) {
            return;
        }
        channelEnded(entity, spell, event.getItem().getUseDuration(entity) - event.getDuration());
    }

    /** A channelled spell let go, from a held caster or a worn bracelet ({@link BraceletCasting#release}). */
    public static void channelEnded(LivingEntity entity, Spell spell, int used) {
        if (SpellTrees.contains(spell, SpellTrees.part("spotlight"))) {
            CasterManager.setCooldown(entity, Math.min(1200, 20 + 2 * Math.max(0, used)));
        }
        if (entity instanceof Player player && SpellTrees.contains(spell, SpellTrees.part("warp_extraction"))) {
            for (ConcentratedWarpCharge charge : WarpExtractionEffect.chargesOf(player, 10.0)) {
                charge.hurl(player.getLookAngle());
            }
        }
        if (entity instanceof Player player && SpellTrees.contains(spell, SpellTrees.part("vis_conductor"))) {
            dev.movingcastle.thaumicconcilium.spell.effect.VisConductorEffect.released(player);
        }
        if (SpellTrees.contains(spell, SpellTrees.part("ethereal_manipulator"))) {
            EtherealManipulatorEffect.resetRange(entity);
        }
    }
}
