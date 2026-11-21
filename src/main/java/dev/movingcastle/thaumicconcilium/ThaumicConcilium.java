package dev.movingcastle.thaumicconcilium;

import dev.movingcastle.thaumicconcilium.network.TCNetwork;
import dev.movingcastle.thaumicconcilium.registry.TCArmorMaterials;
import dev.movingcastle.thaumicconcilium.registry.TCCreativeTabs;
import dev.movingcastle.thaumicconcilium.registry.TCDataComponents;
import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import dev.movingcastle.thaumicconcilium.registry.TCItems;
import dev.movingcastle.thaumicconcilium.registry.TCSounds;
import dev.movingcastle.thaumicconcilium.registry.TCSpellBehaviors;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thaumic Concilium, ported to NeoForge 1.21.1 on top of Thaumaturge.
 * <p>
 * The original (Ilya3000, Forge 1.7.10, WTFPL) was built on Thaumcraft 4. Its systems are re-homed onto
 * Thaumaturge's own: foci become spell parts, golem cores become seals, baubles become Curios.
 */
@Mod(ThaumicConcilium.MODID)
public final class ThaumicConcilium {
    public static final String MODID = "thaumicconcilium";
    public static final Logger LOG = LoggerFactory.getLogger("Thaumic Concilium");

    public ThaumicConcilium(IEventBus modBus, ModContainer container) {
        TCArmorMaterials.MATERIALS.register(modBus);
        TCDataComponents.COMPONENTS.register(modBus);
        TCItems.ITEMS.register(modBus);
        TCEntities.ENTITY_TYPES.register(modBus);
        TCSounds.SOUNDS.register(modBus);
        TCCreativeTabs.TABS.register(modBus);
        TCSpellBehaviors.BEHAVIORS.register(modBus);
        modBus.addListener(TCNetwork::register);
        container.registerConfig(ModConfig.Type.COMMON, TCConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
