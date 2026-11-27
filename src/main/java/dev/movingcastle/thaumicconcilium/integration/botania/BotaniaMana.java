package dev.movingcastle.thaumicconcilium.integration.botania;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import velrondevs.botania.api.mana.ManaItem;
import velrondevs.botania.api.mana.ManaItemHandler;
import velrondevs.botania.xplat.XplatAbstractions;

/**
 * The mana a player carries, for the Astral Monitor and the lithograph HUD (original: the sum of {@code IManaItem}
 * mana over the inventory and baubles, skipping items that don't export). Botania's own handler lists the mana items
 * in the inventory and in its accessories (Curios) slots. Only called when Botania is loaded.
 */
public final class BotaniaMana {
    private BotaniaMana() {
    }

    /** The Drainage Syringe's draw from a player (original {@code suckMana}): up to {@code amount} mana out of their mana items. */
    public static int take(Player victim, ItemStack syringe, int amount) {
        return ManaItemHandler.instance().requestMana(syringe, victim, amount, true);
    }

    /** The Drainage Syringe's return (original {@code dispatchManaExact}): {@code amount} mana into the user's mana items. */
    public static boolean give(Player user, ItemStack syringe, int amount) {
        return ManaItemHandler.instance().dispatchManaExact(syringe, user, amount, true);
    }

    /** Tenths of the player's mana capacity held, minus one as the original did, or -1 (nothing to show). */
    public static int tenths(Player player) {
        long total = 0;
        long max = 0;
        ManaItemHandler handler = ManaItemHandler.instance();
        for (ItemStack stack : handler.getManaItems(player)) {
            ManaItem item = XplatAbstractions.INSTANCE.findManaItem(stack);
            if (item != null && !item.isNoExport()) {
                total += item.getMana();
                max += item.getMaxMana();
            }
        }
        for (ItemStack stack : handler.getManaAccesories(player)) {
            ManaItem item = XplatAbstractions.INSTANCE.findManaItem(stack);
            if (item != null && !item.isNoExport()) {
                total += item.getMana();
                max += item.getMaxMana();
            }
        }
        if (max <= 0) {
            return -1;
        }
        return Mth.clamp(Math.round((float) total / (float) max * 10.0F) - 1, -1, 9);
    }
}
