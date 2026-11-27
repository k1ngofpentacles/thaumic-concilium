package dev.movingcastle.thaumicconcilium.integration.neovitae;

import com.breakinblocks.neovitae.api.soul.AnimaTicket;
import com.breakinblocks.neovitae.api.soul.IAnima;
import com.breakinblocks.neovitae.common.item.BloodOrbItem;
import dev.visfox.forbiddenreforged.integration.neovitae.NvLp;
import dev.movingcastle.thaumicconcilium.world.BloodCeiling;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The player's soul network as the Astral Monitor, the party HUD and the Drainage Syringe read it (original: Blood Magic's
 * {@code SoulNetworkHandler}; Neo Vitae, MIT; only called when it is loaded). Neo Vitae keeps no "highest orb used" on a network,
 * which Blood Magic's {@code getCurrentMaxOrb} did, so the ceiling is a saved per-player record of the best orb
 * ever had ({@link BloodCeiling}).
 */
public final class NeoVitaeBlood {
    private NeoVitaeBlood() {
    }

    /**
     * The network's ceiling: the best orb the player has ever had ({@link BloodCeiling}), after raising it with any bound orb they
     * carry now (inventory and off hand); 0 when they never had one. Neo Vitae's {@code AnimaEvent} carries no orb (its maximum is
     * whatever the caller passes), so the record is raised where we read it: the party stats, the syringe's give-back.
     */
    public static int maxFor(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            BloodCeiling.raise(player, capacity(player, stack));
        }
        for (ItemStack stack : player.getInventory().offhand) {
            BloodCeiling.raise(player, capacity(player, stack));
        }
        return BloodCeiling.get(player);
    }

    private static int capacity(Player player, ItemStack stack) {
        if (stack.getItem() instanceof BloodOrbItem orb && player.getUUID().equals(NvLp.owner(stack))) {
            return orb.getAnimaCapacity(stack);
        }
        return 0;
    }

    /**
     * The party HUD's blood frame (original {@code TCEntityEventHandler}:579-582): the network's content over the orb ceiling in
     * tenths, minus one, 0 to 9; an empty network is frame 0. -1 (hidden) when the player has no orb.
     */
    public static int tenths(Player player) {
        int max = maxFor(player);
        if (max <= 0) {
            return -1;
        }
        int current = NvLp.current(player.getUUID());
        return Mth.clamp(Math.round((float) current / (float) max * 10.0F) - 1, 0, 9);
    }

    /** The syringe's draw from a player (original {@code syphonAndDamageFromNetwork}): takes the life points, hurting for a shortfall. */
    public static void drainPlayer(Player victim, int amount) {
        IAnima network = NvLp.network(victim.getUUID());
        if (network != null) {
            network.syphonAndDamage(victim, AnimaTicket.create(amount));
        }
    }

    /** The syringe's return (original {@code addCurrentEssenceToMaximum}): adds life points up to the user's orb ceiling. */
    public static int giveBack(Player user, int amount) {
        IAnima network = NvLp.network(user.getUUID());
        int max = maxFor(user);
        return network == null || max <= 0 ? 0 : network.add(AnimaTicket.create(amount), max);
    }
}
