package dev.movingcastle.thaumicconcilium.registry;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.spell.FocusPresets;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TCCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ThaumicConcilium.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + ThaumicConcilium.MODID))
            .icon(FocusPresets.IMPULSE::stack)
            .displayItems((params, out) -> {
                for (FocusPresets.Preset preset : FocusPresets.ALL) {
                    out.accept(preset.stack());
                }
                TCItems.ITEMS.getEntries().forEach(item -> out.accept(item.get()));
            })
            .build());

    private TCCreativeTabs() {
    }
}
