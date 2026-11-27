package dev.movingcastle.thaumicconcilium;

import dev.movingcastle.thaumicconcilium.network.TCNetwork;
import dev.movingcastle.thaumicconcilium.registry.TCArmorMaterials;
import dev.movingcastle.thaumicconcilium.registry.TCCreativeTabs;
import dev.movingcastle.thaumicconcilium.registry.TCDataComponents;
import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import dev.movingcastle.thaumicconcilium.registry.TCItems;
import dev.movingcastle.thaumicconcilium.registry.TCSounds;
import dev.movingcastle.thaumicconcilium.registry.TCSpellBehaviors;
import dev.movingcastle.thaumicconcilium.registry.TCWandParts;
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
        dev.movingcastle.thaumicconcilium.lithograph.Lithograph.ATTACHMENTS.register(modBus);
        dev.movingcastle.thaumicconcilium.world.CrimsonState.ATTACHMENTS.register(modBus);
        dev.movingcastle.thaumicconcilium.world.GroveColumns.ATTACHMENTS.register(modBus);
        TCWandParts.RODS.register(modBus);
        dev.movingcastle.thaumicconcilium.registry.TCEffects.EFFECTS.register(modBus);
        dev.movingcastle.thaumicconcilium.registry.TCAuraEffects.TYPES.register(modBus);
        TCItems.ITEMS.register(modBus);
        dev.movingcastle.thaumicconcilium.registry.TCSeals.SEALS.register(modBus);
        modBus.addListener(TCItems::defaultComponents);
        TCEntities.ENTITY_TYPES.register(modBus);
        dev.movingcastle.thaumicconcilium.world.ConfiguredSpawns.SERIALIZERS.register(modBus);
        TCSounds.SOUNDS.register(modBus);
        TCCreativeTabs.TABS.register(modBus);
        TCSpellBehaviors.BEHAVIORS.register(modBus);
        dev.movingcastle.thaumicconcilium.registry.TCMenus.MENUS.register(modBus);
        dev.movingcastle.thaumicconcilium.registry.TCRecipes.SERIALIZERS.register(modBus);
        dev.movingcastle.thaumicconcilium.registry.TCRecipes.TYPES.register(modBus);
        dev.movingcastle.thaumicconcilium.registry.TCFluids.FLUID_TYPES.register(modBus);
        dev.movingcastle.thaumicconcilium.registry.TCFluids.FLUIDS.register(modBus);
        dev.movingcastle.thaumicconcilium.registry.TCBlocks.BLOCKS.register(modBus);
        dev.movingcastle.thaumicconcilium.registry.TCBlocks.BLOCK_ENTITIES.register(modBus);
        dev.movingcastle.thaumicconcilium.world.TCStructures.STRUCTURE_TYPES.register(modBus);
        dev.movingcastle.thaumicconcilium.world.TCStructures.STRUCTURE_PIECES.register(modBus);
        modBus.addListener(TCNetwork::register);
        // phase 10: the Chort, the Sloppy Alchemist and the imp pact exist only with Just Another Witchery Remake (R1)
        if (dev.movingcastle.thaumicconcilium.compat.witchery.WitcheryGate.loaded()) {
            dev.movingcastle.thaumicconcilium.compat.witchery.NetherPair.init(modBus);
        }
        // the grove's own trees: the template growth logic and lineage (only with Dynamic Trees)
        if (net.neoforged.fml.ModList.get().isLoaded("dynamictrees")) {
            dev.movingcastle.thaumicconcilium.compat.DynamicTreesGrove.init(modBus);
        }
        // the Verdant Capital's mana receiver and spark socket: Botania's types are loaded only when Botania is
        modBus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent e) -> {
            if (net.neoforged.fml.ModList.get().isLoaded("botania")) {
                dev.movingcastle.thaumicconcilium.compat.BotaniaCapital.register(e);
                dev.movingcastle.thaumicconcilium.compat.BotaniaPond.register(e);
            }
        });
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent e) -> e.enqueueWork(() -> {
            TCItems.PRIMAL_ARROWS.values().forEach(i -> net.minecraft.world.level.block.DispenserBlock.registerProjectileBehavior(i.get()));
            // what grows in a painted grove (phase 5d cycle 2c)
            dev.movingcastle.thaumicconcilium.world.GrovePainter.setHooks(dev.movingcastle.thaumicconcilium.world.GroveLife.INSTANCE);
        }));
        container.registerConfig(ModConfig.Type.COMMON, TCConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
