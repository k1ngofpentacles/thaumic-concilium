package dev.movingcastle.thaumicconcilium.registry;

import com.leclowndu93150.thaumaturge.api.golems.seals.SealFilterMode;
import com.leclowndu93150.thaumaturge.api.golems.seals.SealType;
import com.leclowndu93150.thaumaturge.content.golem.seals.SealPlacements;
import com.leclowndu93150.thaumaturge.content.golem.seals.behavior.ItemMatchSettings;
import com.leclowndu93150.thaumaturge.content.golem.seals.behavior.UseBehavior;
import com.leclowndu93150.thaumaturge.registry.TTGolemTraits;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.golem.ValetBehavior;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Golem seals, registered on Thaumaturge's seal registry ({@code thaumaturge:seal}) under our namespace. */
public final class TCSeals {
    public static final DeferredRegister<SealType> SEALS = DeferredRegister.create(SealType.REGISTRY_KEY, ThaumicConcilium.MODID);

    /**
     * Valet (original: Valet golem core). Built like the Use seal, with a second filter slot for the phrase item. The in-world
     * icon is the default {@code textures/item/seal_valet.png} (the placer item's own texture).
     */
    public static final DeferredHolder<SealType, SealType> VALET = SEALS.register("valet", () -> SealType.builder(ValetBehavior::new)
            .filter(2, SealFilterMode.PLAIN)
            .settings(ItemMatchSettings.ALL)
            .settings(UseBehavior.LEFT_CLICK, UseBehavior.INTO_AIR, UseBehavior.BARE_HANDED, UseBehavior.SNEAKING, UseBehavior.REQUEST_ITEMS)
            .showSettings()
            .requires(TTGolemTraits.DEFT, TTGolemTraits.SMART)
            .placement(SealPlacements.ANYWHERE)
            .placer(TCItems.SEAL_VALET)
            .build());

    private TCSeals() {
    }
}
