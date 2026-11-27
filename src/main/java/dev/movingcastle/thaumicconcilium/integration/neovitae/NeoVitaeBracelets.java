package dev.movingcastle.thaumicconcilium.integration.neovitae;

import dev.movingcastle.thaumicconcilium.spell.BraceletCasting;
import dev.visfox.forbiddenreforged.integration.neovitae.BloodRods;
import dev.visfox.forbiddenreforged.integration.neovitae.NvLp;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Recharge for the Blood Bracelet: Forbidden Magic's blood staff rod, as the original bracelet used it (it overrode the rod by
 * meta). Every {@link BloodRods#STAFF_PERIOD} ticks, each primal that isn't full takes up to one vis from the owner's soul
 * network (5 life points per centivis, 4 with the alchemical cap), falling back on the wearer's health and killing at 3 health
 * points or fewer, exactly as the staff does. The original ran this "on the hotbar"; a bracelet is worn instead (executive call,
 * as for every bracelet). Only loaded with Neo Vitae: the bracelet is only registered then.
 */
public final class NeoVitaeBracelets {
    private NeoVitaeBracelets() {
    }

    public static void refill(ItemStack bracelet, Player player) {
        if (player.level().isClientSide() || player.tickCount % BloodRods.STAFF_PERIOD != 0 || BraceletCasting.worn(player) != bracelet) {
            return;
        }
        NvLp.bindIfFree(player, bracelet);
        BloodRods.refill(bracelet, player, true);
    }
}
