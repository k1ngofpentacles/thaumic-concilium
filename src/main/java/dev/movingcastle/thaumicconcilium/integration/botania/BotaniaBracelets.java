package dev.movingcastle.thaumicconcilium.integration.botania;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.wands.WandVisHelper;
import dev.movingcastle.thaumicconcilium.spell.BraceletCasting;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import velrondevs.botania.api.mana.ManaItemHandler;

/**
 * Mana recharge for the livingwood, dreamwood and terrasteel bracelets, as the original's rods did it (Forbidden
 * Magic's {@code YandereWandUpdate}, Concilium's {@code TerrasteelCoreUpdate}): every two seconds, while worn, each
 * primal that isn't full takes up to one vis from the wearer's mana items. Livingwood and dreamwood (on manasteel and
 * elementium caps) pay 6 mana per centivis; terrasteel pays 20.
 * <p>
 * Only loaded with Botania: the bracelets whose rods call it are only registered then.
 */
public final class BotaniaBracelets {
    private static final int PERIOD = 40;
    private static final int PER_PASS = 100;
    /** Forbidden Magic's 8 mana per centivis, less 2 for a manasteel or elementium cap. */
    private static final int BOTANICAL_CAP_COST = 6;
    private static final int TERRASTEEL_COST = 20;

    private BotaniaBracelets() {
    }

    public static void refill(ItemStack bracelet, Player player, boolean terrasteel) {
        if (player.tickCount % PERIOD != 0 || BraceletCasting.worn(player) != bracelet) {
            return;
        }
        int cost = terrasteel ? TERRASTEEL_COST : BOTANICAL_CAP_COST;
        int max = WandVisHelper.getMaxVis(bracelet);
        for (ResourceKey<IAspect> primal : TTAspects.PRIMALS) {
            int deficit = Math.min(PER_PASS, max - WandVisHelper.getVis(bracelet, primal));
            if (deficit > 0 && ManaItemHandler.instance().requestManaExact(bracelet, player, cost * deficit, true)) {
                WandVisHelper.addRealVis(bracelet, primal, deficit, true);
            }
        }
    }
}
