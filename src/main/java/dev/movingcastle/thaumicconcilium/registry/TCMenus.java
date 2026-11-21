package dev.movingcastle.thaumicconcilium.registry;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.menu.ThaumaturgeMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Menus. */
public final class TCMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ThaumicConcilium.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ThaumaturgeMenu>> THAUMATURGE =
            MENUS.register("thaumaturge", () -> IMenuTypeExtension.create(ThaumaturgeMenu::new));

    private TCMenus() {
    }
}
