package dev.movingcastle.thaumicconcilium.client;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.polishment.Polishment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** "Polished Infusion: N aspect" on the twelve items that need it (the registry is synced to the client). */
@EventBusSubscriber(modid = ThaumicConcilium.MODID, value = Dist.CLIENT)
public final class PolishmentTooltip {
    private PolishmentTooltip() {
    }

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        var registries = event.getContext().registries();
        if (registries == null) {
            return;
        }
        Polishment p = Polishment.find(registries, event.getItemStack());
        if (p != null) {
            event.getToolTip().add(Component.translatable("tooltip.thaumicconcilium.polished_infusion", p.amount(),
                    Component.translatable("aspect." + p.aspect().getNamespace() + "." + p.aspect().getPath())).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
