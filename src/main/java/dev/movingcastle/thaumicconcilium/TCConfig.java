package dev.movingcastle.thaumicconcilium;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config. Values mirror the original TCConfig where a setting still makes sense on Thaumaturge.
 * Focus upgrade IDs are gone: upgrades are now spell-part settings.
 */
public final class TCConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue IMPULSE_BREAKS_BLOCKS = B
            .comment("Whether the Impulse spell's explosion breaks blocks (it still obeys the mobGriefing game rule).")
            .define("foci.impulseBreaksBlocks", true);

    public static final ModConfigSpec.IntValue POSITIVE_ORB_LIFETIME = B
            .comment("Ticks a Positive Burst orb lives before fading (original: 150).")
            .defineInRange("foci.positiveOrbLifetime", 150, 20, 1200);

    public static final ModConfigSpec.DoubleValue WANDERER_CHANCE = B
            .comment("Chance, checked once a minute in the overworld, that a wandering Thaumaturge (sometimes two) turns up near a",
                    "random player. 0 disables them. The default works out to about one visit every three days. (Not in the original:",
                    "its Thaumaturges spawned naturally, which here happens only as the chunk of a structure in",
                    "#thaumicconcilium:thaumaturge_haunts generates.)")
            .defineInRange("mobs.wandererChance", 0.015, 0.0, 1.0);

    public static final ModConfigSpec.IntValue MAD_REPLACES_BRAINY = B
            .comment("Percent of Brainy Zombies replaced by a Mad Thaumaturge as they spawn (original: madThaumaturgeReplacesBrainyZombieChance, 20).")
            .defineInRange("mobs.madThaumaturgeReplacesBrainyZombieChance", 20, 0, 100);

    public static final ModConfigSpec.IntValue NETHER_EXPLORERS = B
            .comment("Nether Explorers drift on the lava sea until this many are loaded (original: more spawn while at most 3 exist). 0 disables them.")
            .defineInRange("mobs.netherExplorerCap", 4, 0, 64);

    public static final ModConfigSpec.IntValue RIFT_CHANCE = B
            .comment("Percent chance that breaking an aura node tears open a spatial rift (original: chanceOfRiftOpening, 100).",
                    "0 is not recommended: the original warned it softlocks unmodified survival progress (the Rifts research is found",
                    "by scanning one; Thaumaturge's own flux rifts also count).")
            .defineInRange("balance.chanceOfRiftOpening", 100, 0, 100);

    public static final ModConfigSpec.IntValue QUICKSILVER_ELEMENTAL_SPAWN = B
            .comment("Percent chance that a natural Quicksilver Elemental spawn attempt (the biome modifier: weight 5, groups of 1-2, every",
                    "overworld biome, as the original) goes ahead. 0 disables them; they are the Quicksilver Reactor's only source.")
            .defineInRange("mobs.quicksilverElementalSpawnChance", 100, 0, 100);

    public static final ModConfigSpec.DoubleValue ARCHER_DAMAGE_MULTIPLIER = B
            .comment("Scales the base damage of the Crimson Archer's primal arrows (original 2 to 9 at speed 2.5; 1.0 keeps that).")
            .defineInRange("mobs.crimsonArcherDamageMultiplier", 1.0, 0.0, 10.0);

    public static final ModConfigSpec.IntValue PALADIN_REPLACES_KNIGHT = B
            .comment("Percent chance that a Crimson Knight (Thaumaturge's cultist knight) joining the world as a fresh spawn becomes a",
                    "Crimson Paladin instead (original: crimsonPaladinReplacesCultistWarriorChance, 20). 0 disables the replacement.")
            .defineInRange("mobs.crimsonPaladinReplacesKnightChance", 20, 0, 100);

    public static final ModConfigSpec.DoubleValue DRUM_RADIUS_MULTIPLIER = B
            .comment("Drum stance (casting bracelet in the main hand, a drum in the off hand): while the drum is being played (the use key",
                    "held) the area radius of the stance's cast is multiplied by this. Server-side. 1 turns the bonus off.")
            .defineInRange("drum.drumRadiusMultiplier", 1.5, 1.0, 4.0);

    public static final ModConfigSpec.DoubleValue DRUM_VIS_COST_MULTIPLIER = B
            .comment("Drum stance: while the drum is being played, the vis cost of the stance's cast is multiplied by this. Server-side.",
                    "A drum held silent casts at the plain wand cost.")
            .defineInRange("drum.drumVisCostMultiplier", 1.5, 0.0, 10.0);

    public static final ModConfigSpec.BooleanValue GENERATE_AVANPOSTS = B
            .comment("Whether Crimson Avanposts (cultist towers with their archers and loot chest) generate in newly made overworld chunks",
                    "(original: generateCrimsonAvanposts, true). Their rarity is the structure set's spacing (data), about one attempt per 289",
                    "chunks as the original's 1 in 300. Takes effect for chunks generated after the change.")
            .define("world.generateCrimsonAvanposts", true);

    public static final ModConfigSpec SPEC = B.build();

    private TCConfig() {
    }
}
