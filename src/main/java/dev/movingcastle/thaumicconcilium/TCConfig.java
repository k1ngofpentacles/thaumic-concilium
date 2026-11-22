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

    // ---------------------------------------------------------------- phase 5c: the hex as a sink (The Vessel)
    public static final ModConfigSpec.IntValue VESSEL_THRESHOLD = B
            .comment("Burden (T3) a Hex of Predictability can take before it is strained. Each hex rolls its own hidden spread around it once.")
            .defineInRange("vessel.threshold", 400, 10, 100000);
    public static final ModConfigSpec.IntValue VESSEL_THRESHOLD_SPREAD = B
            .comment("Percent either way of the hex's threshold, rolled once per hex and saved.")
            .defineInRange("vessel.thresholdSpreadPercent", 15, 0, 50);
    public static final ModConfigSpec.IntValue VESSEL_MIN_SIZE = B
            .comment("Size of a hexed rift with no burden (the rift's size at the start of a hex's life).")
            .defineInRange("vessel.minRiftSize", 25, 5, 100);
    public static final ModConfigSpec.IntValue VESSEL_MAX_SIZE = B
            .comment("Size of a hexed rift at the threshold (a strained hex's rift stays here).")
            .defineInRange("vessel.maxRiftSize", 75, 5, 100);
    public static final ModConfigSpec.DoubleValue VESSEL_FLUX_PER_CHECK = B
            .comment("Route F1: aura flux the hex drinks from its chunk in each 60-tick check, at most.")
            .defineInRange("vessel.f1.fluxDrainedPerCheck", 0.5, 0.0, 100.0);
    public static final ModConfigSpec.DoubleValue VESSEL_BURDEN_PER_FLUX = B
            .comment("Route F1: burden per unit of aura flux drained.")
            .defineInRange("vessel.f1.burdenPerFlux", 1.0, 0.0, 1000.0);
    public static final ModConfigSpec.DoubleValue VESSEL_BURDEN_PER_VITIUM = B
            .comment("Route F2: burden per unit of vitium essentia a Vis Condenser pushes into the hex.")
            .defineInRange("vessel.f2.burdenPerVitium", 0.25, 0.0, 1000.0);
    public static final ModConfigSpec.DoubleValue VESSEL_BURDEN_PER_GOO = B
            .comment("Route F3: burden per quantum of flux goo or gas (1 to 8 a block) the rift drinks within 4 blocks of the hex.")
            .defineInRange("vessel.f3.burdenPerQuantum", 1.0, 0.0, 1000.0);
    public static final ModConfigSpec.DoubleValue VESSEL_BURDEN_PER_WARP = B
            .comment("Route W1: burden per point of warp (temporary first, then normal, never permanent) a player gives by using the hex sneaking with an empty hand.")
            .defineInRange("vessel.w1.burdenPerWarp", 15.0, 0.0, 1000.0);
    public static final ModConfigSpec.DoubleValue VESSEL_BURDEN_PER_ITEM_WARP = B
            .comment("Route W2: burden per point of warp an item carries (Thaumaturge's item warp) when it is thrown into the rift and destroyed.")
            .defineInRange("vessel.w2.burdenPerItemWarp", 6.0, 0.0, 1000.0);
    public static final ModConfigSpec.DoubleValue VESSEL_LEAK_FACTOR = B
            .comment("L1: a strained hex returns this multiple of everything fed to it to its chunk as aura flux.")
            .defineInRange("vessel.leak.factor", 1.5, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue VESSEL_LEAK_FLUX_PER_BURDEN = B
            .comment("L1: aura flux that one burden is worth when it leaks (1 matches route F1, where burden is flux).")
            .defineInRange("vessel.leak.fluxPerBurden", 1.0, 0.0, 100.0);
    public static final ModConfigSpec.IntValue VESSEL_LEAK_THRESHOLD = B
            .comment("L2: leaked flux after which a strained hex breaks when every player in its level sleeps through the night.")
            .defineInRange("vessel.leak.threshold", 200, 1, 1000000);
    public static final ModConfigSpec.IntValue VESSEL_LEAK_SPREAD = B
            .comment("Percent either way of the leak threshold, rolled once per hex and saved.")
            .defineInRange("vessel.leak.spreadPercent", 25, 0, 90);
    public static final ModConfigSpec.IntValue VESSEL_ABSENT_WARP_SCALE = B
            .comment("Temporary warp a contributor who was offline at the break receives on next login if they gave all of the burden",
                    "(scaled by their share, at least 1).")
            .defineInRange("vessel.absentWarpScale", 20, 1, 500);

    // ---------------------------------------------------------------- phase 5c cycle 2: the Vessel fight (memo docs/design/5c-vessel-fight.md)
    private static ModConfigSpec.DoubleValue fd(String path, double def, double min, double max, String comment) {
        return B.comment(comment).defineInRange("vessel.fight." + path, def, min, max);
    }

    private static ModConfigSpec.IntValue fi(String path, int def, int min, int max, String comment) {
        return B.comment(comment).defineInRange("vessel.fight." + path, def, min, max);
    }

    // pull (section 1). The client reads its own copy of these for the local-player pull: keep them equal across a LAN.
    public static final ModConfigSpec.DoubleValue PULL_ACCEL = fd("pull.accel", 0.08, 0.0, 1.0, "Pull acceleration at the hole's centre, blocks per tick squared (falls as (1 - d/range)^2).");
    public static final ModConfigSpec.DoubleValue PULL_RANGE = fd("pull.range", 16.0, 4.0, 64.0, "Blocks. Beyond this nothing is pulled.");
    public static final ModConfigSpec.DoubleValue PULL_SNEAK = fd("pull.sneakFactor", 0.5, 0.0, 1.0, "Pull factor while sneaking.");
    public static final ModConfigSpec.DoubleValue PULL_TETHERED = fd("pull.tetheredFactor", 1.5, 0.0, 5.0, "Pull factor while tethered (drawing). The anchor gives no shelter.");
    public static final ModConfigSpec.DoubleValue PULL_SWARM = fd("pull.swarmFactor", 0.5, 0.0, 5.0, "Pull factor on the Vessel's own swarms.");
    public static final ModConfigSpec.DoubleValue PULL_SWARM_OLD = fd("pull.oldSwarmFactor", 2.0, 0.0, 5.0, "Pull factor on a swarm older than oldSwarmAge.");
    public static final ModConfigSpec.IntValue PULL_SWARM_AGE = fi("pull.oldSwarmAge", 200, 1, 100000, "Ticks after which a swarm of the Vessel is pulled at the old factor.");
    public static final ModConfigSpec.DoubleValue HORIZON = fd("horizon.radius", 2.5, 0.5, 8.0, "Event horizon radius, to an entity's bounding-box centre.");
    public static final ModConfigSpec.DoubleValue HORIZON_MAX_DAMAGE = fd("horizon.maxDamage", 8.0, 0.0, 100.0, "Taint damage at the horizon, at most (never below 2 health left).");
    public static final ModConfigSpec.IntValue HORIZON_WARP = fi("horizon.temporaryWarp", 1, 0, 20, "Temporary warp at the horizon.");
    public static final ModConfigSpec.IntValue HORIZON_TAINT_TICKS = fi("horizon.fluxTaintTicks", 200, 0, 100000, "Flux Taint duration at the horizon.");
    public static final ModConfigSpec.DoubleValue HORIZON_THROW = fd("horizon.throwSpeed", 1.1, 0.0, 5.0, "Horizontal speed of the horizon throw, blocks per tick.");
    public static final ModConfigSpec.DoubleValue HORIZON_THROW_UP = fd("horizon.throwUp", 0.45, 0.0, 3.0, "Upward speed of the horizon throw.");
    public static final ModConfigSpec.IntValue FLUNG_TICKS = fi("horizon.flungTicks", 40, 0, 1000, "Ticks the flung effect (no pull) lasts after the throw.");

    // mass and phases (section 2); mass is kept in quarter points
    public static final ModConfigSpec.IntValue START_MASS = fi("mass.start", 8, 1, 100, "Start Mass for one player.");
    public static final ModConfigSpec.IntValue START_MASS_PER_PLAYER = fi("mass.perExtraPlayer", 2, 0, 100, "Start Mass added per extra player.");
    public static final ModConfigSpec.IntValue START_MASS_CAP = fi("mass.startCap", 14, 1, 200, "Cap on the start Mass.");
    public static final ModConfigSpec.IntValue MASS_OVERFLOW = fi("mass.maxAboveStart", 4, 0, 100, "Mass never exceeds start plus this.");
    public static final ModConfigSpec.IntValue GAIN_WINDOW = fi("mass.gainWindowTicks", 200, 1, 100000, "Mass gained from eating is capped at gainPerWindow per this many ticks.");
    public static final ModConfigSpec.DoubleValue GAIN_PER_WINDOW = fd("mass.gainPerWindow", 1.0, 0.0, 100.0, "Mass an eating window can add at most.");
    public static final ModConfigSpec.DoubleValue EAT_ITEM = fd("mass.eatItem", 0.25, 0.0, 10.0, "Mass from an item entity or arrow.");
    public static final ModConfigSpec.DoubleValue EAT_MOB = fd("mass.eatMob", 0.25, 0.0, 10.0, "Mass from a mob.");
    public static final ModConfigSpec.DoubleValue EAT_SWARM = fd("mass.eatSwarm", 0.5, 0.0, 10.0, "Mass from the Vessel's own swarm.");
    public static final ModConfigSpec.IntValue PLAYER_RANGE = fi("playerRange", 48, 8, 256, "Players within this many blocks count for the start Mass and keep it awake.");
    public static final ModConfigSpec.IntValue WAKE_RANGE = fi("wakeRange", 16, 2, 64, "A player this close wakes a dormant Vessel.");
    public static final ModConfigSpec.IntValue WAKE_SWELL = fi("wakeSwellTicks", 60, 0, 1000, "Swell before the pull starts.");
    public static final ModConfigSpec.IntValue DORMANT_TICKS = fi("dormantAfterTicks", 1200, 20, 1000000, "Ticks with no player in range before it goes dormant.");
    public static final ModConfigSpec.DoubleValue SPINUP_MASS = fd("phase.spinUpAtMass", 5.0, 0.0, 100.0, "Spin-up starts at this Mass or below.");
    public static final ModConfigSpec.DoubleValue COLLAPSE_MASS = fd("phase.collapseAtMass", 2.0, 0.0, 100.0, "Collapse starts at this Mass or below.");

    // tethers (draw)
    public static final ModConfigSpec.DoubleValue TETHER_RANGE = fd("tether.range", 2.5, 0.5, 16.0, "Horizontal blocks from the anchor within which a tether holds.");
    public static final ModConfigSpec.IntValue DRAW_TICKS = fi("tether.drawTicks", 160, 1, 100000, "Progress needed to drop the Mass by 1 (+1 a tick, +2 in a stagger).");
    public static final ModConfigSpec.IntValue DRAWS_PER_COOLDOWN = fi("tether.drawsBeforeCooldown", 2, 1, 100, "Draws an anchor allows before it cools.");
    public static final ModConfigSpec.IntValue ANCHOR_COOLDOWN = fi("tether.cooldownTicks", 400, 0, 1000000, "Game ticks an anchor cools (kept through dormancy).");
    public static final ModConfigSpec.IntValue PARTICIPANT_TETHER_TICKS = fi("tether.participantTicks", 20, 0, 100000, "A tether this old makes its drawer a participant.");

    // clots
    public static final ModConfigSpec.IntValue CLOT_WINDUP = fi("clot.windupTicks", 15, 0, 1000, "Wind-up at the disc edge before a clot flies.");
    public static final ModConfigSpec.DoubleValue CLOT_SPEED = fd("clot.speed", 0.25, 0.01, 5.0, "Flight speed, blocks per tick.");
    public static final ModConfigSpec.IntValue CLOT_LIFE = fi("clot.lifeTicks", 100, 1, 100000, "Flight life in ticks.");
    public static final ModConfigSpec.DoubleValue CLOT_BAT_SPEED = fd("clot.batSpeed", 0.6, 0.01, 5.0, "Speed of a batted clot on its way back.");
    public static final ModConfigSpec.DoubleValue CLOT_DAMAGE = fd("clot.damage", 4.0, 0.0, 100.0, "Taint damage of a hit.");
    public static final ModConfigSpec.IntValue CLOT_TAINT_TICKS = fi("clot.fluxTaintTicks", 160, 0, 100000, "Flux Taint from a hit.");
    public static final ModConfigSpec.IntValue CLOT_FEEDING = fi("clot.feedingInterval", 100, 1, 100000, "Ticks between clots in Feeding.");
    public static final ModConfigSpec.IntValue CLOT_SPINUP = fi("clot.spinUpInterval", 100, 1, 100000, "Ticks between clot pairs in Spin-up.");
    public static final ModConfigSpec.IntValue CLOT_SPINUP_GAP = fi("clot.spinUpPairGap", 10, 1, 1000, "Ticks between the two clots of a pair.");
    public static final ModConfigSpec.IntValue CLOT_COLLAPSE = fi("clot.collapseInterval", 60, 1, 100000, "Ticks between clots in Collapse.");
    public static final ModConfigSpec.IntValue STAGGER_TICKS = fi("stagger.ticks", 80, 0, 100000, "Stagger length: no pull, double draw.");

    // swarms, gulp, disc, blocks
    public static final ModConfigSpec.IntValue SWARM_INTERVAL = fi("swarm.intervalTicks", 300, 1, 100000, "Spin-up: ticks between swarm waves.");
    public static final ModConfigSpec.IntValue SWARM_MAX = fi("swarm.maxAlive", 3, 0, 64, "Most swarms alive at once.");
    public static final ModConfigSpec.DoubleValue SWARM_RADIUS = fd("swarm.radius", 6.0, 1.0, 32.0, "Radius around the hole where swarms appear.");
    public static final ModConfigSpec.IntValue GULP_INTERVAL = fi("gulp.intervalTicks", 400, 1, 100000, "Collapse: ticks between gulps.");
    public static final ModConfigSpec.IntValue GULP_SWELL = fi("gulp.swellTicks", 60, 0, 10000, "Telegraph before a gulp.");
    public static final ModConfigSpec.IntValue GULP_TICKS = fi("gulp.pullTicks", 40, 1, 10000, "Length of the gulp's strong pull.");
    public static final ModConfigSpec.DoubleValue GULP_SCALE = fd("gulp.pullScale", 3.0, 1.0, 20.0, "Pull multiplier during a gulp.");
    public static final ModConfigSpec.DoubleValue DISC_INNER = fd("disc.innerRadius", 4.0, 0.5, 32.0, "Accretion disc inner radius.");
    public static final ModConfigSpec.DoubleValue DISC_OUTER = fd("disc.outerRadius", 6.0, 0.5, 32.0, "Accretion disc outer radius.");
    public static final ModConfigSpec.DoubleValue DISC_TILT = fd("disc.tiltDegrees", 20.0, 0.0, 90.0, "Tilt of the disc plane.");
    public static final ModConfigSpec.IntValue DISC_PRECESSION = fi("disc.precessionTicks", 600, 20, 1000000, "Ticks for one precession turn.");
    public static final ModConfigSpec.IntValue DISC_INTERVAL = fi("disc.checkTicks", 10, 1, 1000, "Ticks between contact tests.");
    public static final ModConfigSpec.IntValue DISC_TAINT_TICKS = fi("disc.fluxTaintTicks", 100, 0, 100000, "Flux Taint on contact.");
    public static final ModConfigSpec.IntValue EDIBLE_RADIUS = fi("edible.radius", 3, 1, 16, "Blocks around the centre it can eat (one random block a second).");
    public static final ModConfigSpec.IntValue PRESENCE_TICKS = fi("presence.ticks", 600, 0, 1000000, "Ticks within presenceRange for a contributor to count as a participant.");
    public static final ModConfigSpec.IntValue PRESENCE_RANGE = fi("presence.range", 32, 4, 256, "Blocks.");
    public static final ModConfigSpec.IntValue DARKNESS_RADIUS = fi("darkness.radius", 8, 0, 64, "Blocks from the hole inside which an active Vessel gives vanilla Darkness (0 turns it off).");
    public static final ModConfigSpec.IntValue BLINDNESS_RADIUS = fi("darkness.blindnessRadius", 4, 0, 64, "Blocks from the hole inside which it gives Blindness (0 turns it off).");
    public static final ModConfigSpec.IntValue DARKNESS_TICKS = fi("darkness.ticks", 60, 20, 600, "Duration of each application (refreshed once a second, so it ends soon after leaving).");

    // Sable (cycle 2b)
    public static final ModConfigSpec.DoubleValue SABLE_ACCEL = fd("sable.accel", 0.075, 0.0, 10.0, "Ship velocity change per tick at the hole's centre, blocks per second (falls as (1 - d/range)^2).");
    public static final ModConfigSpec.DoubleValue SABLE_IMPULSE_CAP = fd("sable.impulseCap", 150.0, 0.0, 1.0e7, "Most impulse (mass x blocks per second) one ship gets in a tick.");
    public static final ModConfigSpec.DoubleValue SABLE_APPROACH_CAP = fd("sable.approachSpeedCap", 4.0, 0.0, 1000.0, "A ship already moving toward the hole this fast (blocks per second) is not pushed.");
    public static final ModConfigSpec.DoubleValue SABLE_ANGULAR_CAP = fd("sable.angularSpeedCap", 1.0, 0.0, 100.0, "Angular speed (radians per second) above which the tidal twist is dropped and the ship is pulled through its centre only.");

    public static final ModConfigSpec.BooleanValue BREAK_SABLE = B
            .comment("With Sable installed, the breaking hex's blocks become a sub-level for the break sequence. False: the rendered stand-in is used.")
            .define("vessel.break.sable", true);

    public static final ModConfigSpec.DoubleValue BLAST_DAMAGE = B
            .comment("Explosion damage the burst of a breaking hex does at its very centre (armour and Blast Protection apply). It falls to nothing at the crater's radius, about 14 blocks.")
            .defineInRange("vessel.break.blastDamage", 24.0, 0.0, 1000.0);
    public static final ModConfigSpec.DoubleValue BLAST_KNOCKBACK = B
            .comment("Speed (blocks per tick) the burst throws players and mobs outward at its centre; a quarter to four tenths of it goes upward. Falls to nothing at the crater's radius.")
            .defineInRange("vessel.break.blastKnockback", 1.6, 0.0, 10.0);

    // ---------------------------------------------------------------- phase 5c cycle 4: the Quiescent Vessel and the thaumaturge's rite
    private static ModConfigSpec.DoubleValue rd(String path, double def, double min, double max, String comment) {
        return B.comment(comment).defineInRange("vessel.rite." + path, def, min, max);
    }

    private static ModConfigSpec.IntValue ri(String path, int def, int min, int max, String comment) {
        return B.comment(comment).defineInRange("vessel.rite." + path, def, min, max);
    }

    public static final ModConfigSpec.IntValue RITE_CHARGE = ri("charge", 20, 1, 1000, "Units of charge the subdued Vessel holds.");
    public static final ModConfigSpec.IntValue RITE_UNIT_TICKS = ri("unitTicks", 1200, 20, 1000000,
            "Ticks one drawer needs at an anchor to drain one unit (1200 = a minute, so 20 units take 20 minutes for one drawer; four drawers, five).");
    public static final ModConfigSpec.IntValue RITE_NORMAL_EVERY = ri("normalWarpEvery", 5, 1, 1000, "Every this many units drained, the drawer also gets 1 normal warp.");
    public static final ModConfigSpec.IntValue RITE_PULSE_TICKS = ri("pulseTicks", 20, 1, 1200, "Ticks between fibre pulses while a drain is under way.");
    public static final ModConfigSpec.IntValue RITE_PULSES = ri("pulsesPerBeat", 3, 1, 32, "Fibre pulses (and pressure points) per beat.");
    public static final ModConfigSpec.IntValue RITE_RING_MIN = ri("ringMin", 8, 1, 128, "Blocks: the pulse ring's radius at the first unit.");
    public static final ModConfigSpec.IntValue RITE_RING_MAX = ri("ringMax", 24, 1, 128, "Blocks: the pulse ring's radius at the last unit.");
    public static final ModConfigSpec.DoubleValue RITE_PRESSURE = rd("pressure", 0.02, 0.0, 10.0, "Ecological pressure added at each pulse point.");
    public static final ModConfigSpec.IntValue RITE_SPILL_TICKS = ri("spillTicks", 400, 20, 1000000, "Ticks between goo spills at an anchor in use.");
    public static final ModConfigSpec.IntValue RITE_SPILL_PER_POINT = ri("spillsPerScorePoint", 15, 1, 1000, "Spills that count as one point of the Taint Seed's score (missed clots count one each).");
    public static final ModConfigSpec.IntValue RITE_PRIME_SCORE = ri("primeScore", 12, 1, 1000, "A score of this or more leaves a Prime Taint Seed, below it a plain one.");
    public static final ModConfigSpec.DoubleValue RITE_SEED_HEALTH_PER_POINT = rd("seedHealthPerPoint", 0.05, 0.0, 5.0, "Extra seed health per score point, as a fraction of its base (capped at 3 times the base).");
    public static final ModConfigSpec.IntValue RITE_SEED_RESERVE_SCORE = ri("seedReserveScore", 24, 1, 1000, "The score at which the seed starts with a full taint reserve (linear below).");
    public static final ModConfigSpec.DoubleValue RITE_MOTE_SECOND = rd("moteSecondChance", 0.10, 0.0, 1.0, "Chance each participant also receives a second Primordial Mote.");

    public static final ModConfigSpec SPEC = B.build();

    private TCConfig() {
    }
}
