package dev.movingcastle.thaumicconcilium.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Primordial Life: the "will amplifier" dropped by the Brightest One. It is the key component of the Ring of
 * Blustering Light and the three foci that draw on it. The boss arrives with the rift content (phase 6); until
 * then this item only exists in the creative tab.
 */
public class PrimordialLife extends Item {
    public PrimordialLife(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.thaumicconcilium.primordial_life.desc").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
