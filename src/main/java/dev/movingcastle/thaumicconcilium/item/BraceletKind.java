package dev.movingcastle.thaumicconcilium.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Rarity;

/**
 * The casting bracelets. Capacities are the originals': Thaumic Bases' iron, thaumium and void bracelets (10, 20, 27
 * vis) and Concilium's own (30 to 90). The livingwood, dreamwood and terrasteel bracelets exist only with Botania, as
 * in the original; Tainted Magic's warpwood bracelet becomes the eldritch one.
 *
 * @param cap      the Thaumaturge wand cap whose cost modifier the bracelet uses
 * @param band     block-atlas sprite for the band
 * @param rim      block-atlas sprite for the cap-metal rims
 * @param volley   casts an instant spell ten times at once, at triple cooldown, each cast at the original's 3.0 cost
 *                 modifier
 */
public enum BraceletKind {
    IRON("iron", 10, "thaumaturge:iron", "minecraft:block/iron_block", "minecraft:block/iron_block", Rarity.COMMON, Recharge.NONE, false, false),
    THAUMIUM("thaumium", 20, "thaumaturge:thaumium", "thaumaturge:block/metal_thaumium", "minecraft:block/gold_block", Rarity.COMMON, Recharge.NONE, false, false),
    VOID("void", 27, "thaumaturge:void", "thaumaturge:block/metal_void", "thaumaturge:block/metal_thaumium", Rarity.UNCOMMON, Recharge.NONE, false, false),
    // Botania (Botania Revived) only, on Forbidden Magic's Botania caps and rods, as in the original.
    LIVINGWOOD("livingwood", 30, "forbiddenmagic:manasteel", "botania:block/livingwood_log", "botania:block/manasteel_block", Rarity.UNCOMMON, Recharge.MANA, false, false),
    DREAMWOOD("dreamwood", 50, "forbiddenmagic:elementium", "botania:block/dreamwood_log", "botania:block/elementium_block", Rarity.RARE, Recharge.MANA, false, false),
    TERRASTEEL("terrasteel", 50, "forbiddenmagic:terrasteel", "botania:block/terrasteel_block", "botania:block/terrasteel_block", Rarity.EPIC, Recharge.TERRASTEEL, false, true),
    INFERNAL("infernal", 35, "thaumaturge:thaumium", "minecraft:block/netherrack", "thaumaturge:block/metal_thaumium", Rarity.RARE, Recharge.FM_INFERNAL, true, false),
    TAINTED("tainted", 35, "thaumaturge:thaumium", "thaumaturge:block/taint_crust_0", "thaumaturge:block/metal_thaumium", Rarity.RARE, Recharge.FM_TAINTED, false, false),
    ELDRITCH("eldritch", 90, "thaumaturge:void", "thaumaturge:block/eldritch_stone", "thaumaturge:block/metal_void", Rarity.EPIC, Recharge.WARP, true, false),
    // Neo Vitae only (phase 10-5): the original's "alchemist" bracelet, on Forbidden Magic's alchemical cap and blood staff rod,
    // its band the large bloodstone brick (Neo Vitae has only the plain brick). The alchemical cap has no block sprite: gold rims.
    BLOOD("blood", 50, "forbiddenmagic:alchemical", "neovitae:block/bloodstone_brick", "minecraft:block/gold_block", Rarity.UNCOMMON, Recharge.BLOOD, false, false);

    /** How a bracelet refills on its own. */
    public enum Recharge {
        NONE,
        /**
         * Forbidden Magic's livingwood rod and dreamwood staff: one vis of each primal every two seconds from the
         * wearer's mana items, 8 mana per centivis (6 with a manasteel or elementium cap).
         */
        MANA,
        /** Concilium's terrasteel core: the same, at 20 mana per centivis. */
        TERRASTEEL,
        /** Forbidden Magic's infernal and tainted rods, looked up at run time. */
        FM_INFERNAL, FM_TAINTED,
        /** Tainted Magic's warpwood: one vis of each primal every 10000 / permanent warp ticks, unless warded. */
        WARP,
        /** Forbidden Magic's blood staff rod: soul-network life points while worn, health as a last resort (Neo Vitae only). */
        BLOOD
    }

    private final String id;
    private final int capacity;
    private final ResourceLocation cap;
    private final ResourceLocation band;
    private final ResourceLocation rim;
    private final Rarity rarity;
    private final Recharge recharge;
    private final boolean glow;
    private final boolean volley;

    BraceletKind(String id, int capacity, String cap, String band, String rim, Rarity rarity, Recharge recharge, boolean glow,
                 boolean volley) {
        this.id = id;
        this.capacity = capacity;
        this.cap = ResourceLocation.parse(cap);
        this.band = ResourceLocation.parse(band);
        this.rim = ResourceLocation.parse(rim);
        this.rarity = rarity;
        this.recharge = recharge;
        this.glow = glow;
        this.volley = volley;
    }

    public String id() {
        return this.id;
    }

    /** Item id path: {@code iron_bracelet}, ... */
    public String itemId() {
        return this.id + "_bracelet";
    }

    public int capacity() {
        return this.capacity;
    }

    public ResourceLocation cap() {
        return this.cap;
    }

    public ResourceLocation band() {
        return this.band;
    }

    public ResourceLocation rim() {
        return this.rim;
    }

    public Rarity rarity() {
        return this.rarity;
    }

    public Recharge recharge() {
        return this.recharge;
    }

    public boolean glow() {
        return this.glow;
    }

    public boolean volley() {
        return this.volley;
    }

    /** Needs Botania (Botania Revived) and Forbidden Magic's Botania caps. */
    public boolean botanical() {
        return this.recharge == Recharge.MANA || this.recharge == Recharge.TERRASTEEL;
    }

    /** Whether this bracelet exists in this game: the Botania ones only alongside Botania, the blood one only with Neo Vitae. */
    public boolean available() {
        if (this.recharge == Recharge.BLOOD) {
            return net.neoforged.fml.ModList.get().isLoaded("neovitae");
        }
        return !this.botanical() || net.neoforged.fml.ModList.get().isLoaded("botania");
    }
}
