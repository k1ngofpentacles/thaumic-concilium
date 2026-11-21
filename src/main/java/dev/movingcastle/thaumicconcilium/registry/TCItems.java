package dev.movingcastle.thaumicconcilium.registry;

import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.ClearWaterBottle;
import dev.movingcastle.thaumicconcilium.entity.ThickTaintBottle;
import dev.movingcastle.thaumicconcilium.item.BurdeningAmulet;
import dev.movingcastle.thaumicconcilium.item.DumpJackboots;
import dev.movingcastle.thaumicconcilium.item.ElementalSurferShorts;
import dev.movingcastle.thaumicconcilium.item.PrimordialLife;
import dev.movingcastle.thaumicconcilium.item.RunicWindings;
import dev.movingcastle.thaumicconcilium.item.ShardKind;
import dev.movingcastle.thaumicconcilium.item.ShardMill;
import dev.movingcastle.thaumicconcilium.item.ThrowableBottle;
import dev.movingcastle.thaumicconcilium.item.TightBelt;
import net.minecraft.world.item.ArmorItem;
import com.leclowndu93150.thaumaturge.api.wands.WandCap;
import com.leclowndu93150.thaumaturge.content.wands.WandParts;
import com.leclowndu93150.thaumaturge.registry.TTWandParts;
import dev.movingcastle.thaumicconcilium.item.BraceletKind;
import dev.movingcastle.thaumicconcilium.item.CastingBracelet;
import java.util.EnumMap;
import java.util.Map;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import dev.movingcastle.thaumicconcilium.item.RingOfBlusteringLight;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Items. Foci are not items: they are spell parts inscribed onto Thaumaturge's own focus items (see
 * {@link dev.movingcastle.thaumicconcilium.spell.FocusPresets}).
 */
public final class TCItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ThaumicConcilium.MODID);

    /** Original: runic charge 50, 3 warp, epic. */
    public static final DeferredItem<RingOfBlusteringLight> RING_OF_BLUSTERING_LIGHT = ITEMS.register("ring_of_blustering_light",
            () -> new RingOfBlusteringLight(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
                    .component(TTDataComponents.RUNIC_CHARGE.get(), 50)
                    .component(TTDataComponents.STACK_WARP.get(), 3)));

    public static final DeferredItem<PrimordialLife> PRIMORDIAL_LIFE = ITEMS.register("primordial_life",
            () -> new PrimordialLife(new Item.Properties().rarity(Rarity.EPIC)));

    // ---------------------------------------------------------------- phase 3
    /** Original: runic charge 200 each, 3 warp each, uncommon. */
    public static final DeferredItem<RunicWindings> RUNIC_BODY_WINDINGS = ITEMS.register("runic_body_windings",
            () -> new RunicWindings(TCArmorMaterials.WINDINGS, ArmorItem.Type.CHESTPLATE, windings()));
    public static final DeferredItem<RunicWindings> RUNIC_LEGS_WINDINGS = ITEMS.register("runic_legs_windings",
            () -> new RunicWindings(TCArmorMaterials.WINDINGS, ArmorItem.Type.LEGGINGS, windings()));

    public static final DeferredItem<DumpJackboots> DUMP_JACKBOOTS = ITEMS.register("dump_jackboots",
            () -> new DumpJackboots(TCArmorMaterials.DUMP_JACKBOOTS, ArmorItem.Type.BOOTS,
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE).durability(ArmorItem.Type.BOOTS.getDurability(25))));

    /** Creative-only, as in the original. */
    public static final DeferredItem<ElementalSurferShorts> ELEMENTAL_SURFER_SHORTS = ITEMS.register("elemental_surfer_shorts",
            () -> new ElementalSurferShorts(TCArmorMaterials.SURFER_SHORTS, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().stacksTo(1).durability(ArmorItem.Type.LEGGINGS.getDurability(25))));

    /** Original: necklace (TC4 amulet), 3 warp, uncommon. */
    public static final DeferredItem<BurdeningAmulet> BURDENING_AMULET = ITEMS.register("burdening_amulet",
            () -> new BurdeningAmulet(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)
                    .component(TTDataComponents.STACK_WARP.get(), 3)));
    /** Original: belt, 3 warp, uncommon. */
    public static final DeferredItem<TightBelt> TIGHT_BELT = ITEMS.register("tight_belt",
            () -> new TightBelt(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)
                    .component(TTDataComponents.STACK_WARP.get(), 3)));

    /** Original: 2 warp. */
    public static final DeferredItem<ThrowableBottle> BOTTLE_OF_THICK_TAINT = ITEMS.register("bottle_of_thick_taint",
            () -> new ThrowableBottle(new Item.Properties().component(TTDataComponents.STACK_WARP.get(), 2), ThickTaintBottle::new));
    public static final DeferredItem<ThrowableBottle> BOTTLE_OF_CLEAR_WATER = ITEMS.register("bottle_of_clear_water",
            () -> new ThrowableBottle(new Item.Properties(), ClearWaterBottle::new));

    public static final DeferredItem<ShardMill> SHARD_MILL = ITEMS.register("shard_mill",
            () -> new ShardMill(new Item.Properties().rarity(Rarity.UNCOMMON), ShardKind.GREATWOOD));
    /** Original: 1 warp. */
    public static final DeferredItem<ShardMill> SILVERWOOD_SHARD_MILL = ITEMS.register("silverwood_shard_mill",
            () -> new ShardMill(new Item.Properties().rarity(Rarity.RARE).component(TTDataComponents.STACK_WARP.get(), 1),
                    ShardKind.SILVERWOOD));

    // ---------------------------------------------------------------- phase 3b
    /** The casting bracelets, worn in the Curios bracelet slot. */
    public static final Map<BraceletKind, DeferredItem<CastingBracelet>> BRACELETS = new EnumMap<>(BraceletKind.class);

    static {
        for (BraceletKind kind : BraceletKind.values()) {
            if (!kind.available()) {
                continue;
            }
            BRACELETS.put(kind, ITEMS.register(kind.itemId(),
                    () -> new CastingBracelet(new Item.Properties().stacksTo(1).rarity(kind.rarity()), kind)));
        }
    }

    /** Each bracelet's fixed wand parts: its own rod (capacity, recharge) and the Thaumaturge cap of its kind. */
    public static void defaultComponents(ModifyDefaultComponentsEvent event) {
        BRACELETS.forEach((kind, item) -> {
            WandCap cap = TTWandParts.caps().get(kind.cap());
            if (cap == null) {
                throw new IllegalStateException("Missing wand cap " + kind.cap() + " for " + kind.itemId());
            }
            WandParts parts = new WandParts(cap, TCWandParts.BRACELET_RODS.get(kind).get(), false);
            event.modify(item.get(), builder -> builder.set(TTDataComponents.WAND_PARTS.get(), parts));
        });
    }

    private static Item.Properties windings() {
        return new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)
                .component(TTDataComponents.RUNIC_CHARGE.get(), 200)
                .component(TTDataComponents.STACK_WARP.get(), 3);
    }

    private TCItems() {
    }
}
