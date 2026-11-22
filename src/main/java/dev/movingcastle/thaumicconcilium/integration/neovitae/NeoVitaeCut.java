package dev.movingcastle.thaumicconcilium.integration.neovitae;

import com.breakinblocks.neovitae.common.blockentity.AraVitaeTile;
import com.breakinblocks.neovitae.util.AltarUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * The Crimson Dagger's self-cut (Neo Vitae, MIT; only called when it is loaded). Takes {@link #HEALTH} of the player's health
 * through Neo Vitae's own sacrifice path (its damage type, absorption kept) and puts the life points it is worth into the
 * Ara Vitae within {@link #ALTAR_RADIUS} blocks (Neo Vitae's search: the first one found, scanning outward in cube order).
 */
public final class NeoVitaeCut {
    /** PROVISIONAL (branch decision): health taken per cut, in half hearts. Blood Orbs take 2. */
    public static final int HEALTH = 1;
    /** PROVISIONAL: the dagger's own rate, a multiple of Neo Vitae's self-sacrifice rate (1.0 = what a Blood Orb pays per health). */
    public static final double LP_FACTOR = 1.0;
    /** PROVISIONAL: reach to the altar. */
    public static final int ALTAR_RADIUS = 8;
    /** PROVISIONAL: ticks between cuts. */
    public static final int COOLDOWN_TICKS = 20;

    private NeoVitaeCut() {
    }

    /** True when an Ara Vitae is in reach; tells the player when none is. Asked when the cut starts, before {@link #cut}. */
    public static boolean inReach(ServerPlayer player) {
        if (AltarUtil.getAltarTile(player, ALTAR_RADIUS) != null) {
            return true;
        }
        player.displayClientMessage(Component.translatable("message.thaumicconcilium.dagger_no_altar"), true);
        return false;
    }

    /** True when LP went into an altar. */
    public static boolean cut(ServerPlayer player) {
        AraVitaeTile altar = AltarUtil.getAltarTile(player, ALTAR_RADIUS);
        if (altar == null) {
            player.displayClientMessage(Component.translatable("message.thaumicconcilium.dagger_no_altar"), true);
            return false;
        }
        float lost = player.getAbilities().instabuild ? HEALTH : AltarUtil.takeSacrifice(player, HEALTH);
        if (lost <= 0.0F) {
            player.displayClientMessage(Component.translatable("message.neovitae.altar_blood_refused"), true);
            return false;
        }
        int lp = (int) (AltarUtil.calculateSelfSacrificeLP(player, HEALTH) * LP_FACTOR * Math.min(1.0F, lost / HEALTH));
        altar.addSacrificeEV(Math.max(0, lp), false);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEEHIVE_DRIP, SoundSource.PLAYERS, 0.6F,
                0.8F + player.getRandom().nextFloat() * 0.4F);
        return true;
    }
}
