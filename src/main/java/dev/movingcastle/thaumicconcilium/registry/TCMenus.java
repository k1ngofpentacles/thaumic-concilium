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

    public static final DeferredHolder<MenuType<?>, MenuType<dev.movingcastle.thaumicconcilium.menu.AstralMonitorMenu>> ASTRAL_MONITOR =
            MENUS.register("astral_monitor", () -> IMenuTypeExtension.create((id, inv, buf) -> new dev.movingcastle.thaumicconcilium.menu.AstralMonitorMenu(id, inv)));

    public static final DeferredHolder<MenuType<?>, MenuType<dev.movingcastle.thaumicconcilium.menu.LithographerMenu>> LITHOGRAPHER =
            MENUS.register("lithographer", () -> IMenuTypeExtension.create(dev.movingcastle.thaumicconcilium.menu.LithographerMenu::new));

    private TCMenus() {
    }
}
