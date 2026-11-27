package dev.movingcastle.thaumicconcilium.compat.witchery;

import dev.sterner.witchery.feature.affliction.vampire.VampirePlayerAttachment;
import dev.sterner.witchery.feature.affliction.werewolf.WerewolfPlayerAttachment;
import net.minecraft.world.entity.player.Player;

/**
 * The remake's two player afflictions (original: {@code ExtendedPlayer.isVampire()} and {@code getWerewolfLevel() > 0}). The only
 * place the Burned Witch's code names a {@code dev.sterner} class; loaded only when she throws at a player, so only with Witchery.
 */
public final class WitchAfflictions {
    private WitchAfflictions() {
    }

    public static boolean vampire(Player player) {
        return VampirePlayerAttachment.getData(player).isVampire();
    }

    /** Original: werewolf level above zero (a player who is bitten but not yet turned has no level). */
    public static boolean werewolf(Player player) {
        return WerewolfPlayerAttachment.getData(player).getLevel() > 0;
    }
}
