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

    public static final ModConfigSpec.IntValue RIPOSTE_PARRY_TICKS = B
            .comment("Riposte: an armed Riposte only fires on a hit blocked within this many ticks of raising the shield (the parry",
                    "window; a shield blocks from tick 5). Held longer, the shield just blocks and the charges stay. Server-side.")
            .defineInRange("riposte.parryTicks", 10, 5, 100);

    public static final ModConfigSpec.DoubleValue RIPOSTE_RANGE = B
            .comment("Riposte: how far away a projectile's shooter may be for the riposte to reach them, in blocks. Farther away, the",
                    "shield just blocks and the charges stay. Melee attackers are always reached. Server-side.")
            .defineInRange("riposte.range", 8.0, 1.0, 64.0);

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

    // ---------------------------------------------------------------- phase 5d cycle 1: the Verdant Capital and the mixed drain
    public static final ModConfigSpec.IntValue RITE_MANA_PER_UNIT = ri("manaPerUnit", 250000, 1, 100000000,
            "Botania mana a Verdant Capital spends to drain one unit (20 units = 5,000,000 for a whole Vessel by capitals alone).");
    public static final ModConfigSpec.IntValue RITE_CAPITAL_BUFFER = ri("capitalBuffer", 10000, 1, 100000000, "Mana a Verdant Capital holds.");
    public static final ModConfigSpec.IntValue RITE_STARVE_GRACE = ri("starveGraceTicks", 6000, 0, 10000000,
            "Ticks a capital may stay without enough mana before its unit's progress starts to unwind (1 a tick, a unit that unwinds to zero gives one unit of charge back).");

    // ---------------------------------------------------------------- phase 5d cycle 2a: the endings
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends Double>> RITE_MOTE_TIER_CHANCES = B
            .comment("Total chance of a second Primordial Mote by number of Verdant Capitals (0 to 4 entries, in order). A participant who got one at the subdue rolls a top-up at the ending to reach the tier's chance; one absent at the subdue rolls the tier's chance for one mote.")
            .defineList("vessel.rite.moteChanceByTier", java.util.List.of(0.10, 0.16, 0.23, 0.29, 0.35), () -> 0.1,
                    o -> o instanceof Double d && d >= 0.0 && d <= 1.0);
    public static final ModConfigSpec.IntValue RITE_WARP_SOLO = ri("warpRemovedSolo", 3, 0, 1000,
            "Tier 4 (four Verdant Capitals): permanent warp removed from a participant with no Heartstone partner among the participants.");
    public static final ModConfigSpec.IntValue RITE_WARP_PAIR = ri("warpRemovedPair", 1, 0, 1000,
            "Tier 4: permanent warp removed from each of a Heartstone-bound pair of participants.");
    public static final ModConfigSpec.IntValue RITE_BOUND_SPREAD_TICKS = ri("boundSpreadTicks", 6000, 20, 10000000,
            "Tier 1 (one capital): ticks the bound seed's fibres take to spread from the hex out to the crater rim (a first guess; they never go further).");
    public static final ModConfigSpec.IntValue RITE_BOUND_SPREAD_PER_BEAT = ri("boundSpreadPerBeat", 1, 1, 64,
            "Tier 1: fibre spreads per beat (20 ticks) at random points inside the widening disc.");
    public static final ModConfigSpec.DoubleValue RITE_BOUND_RESERVE = rd("boundReserveFraction", 0.3, 0.0, 1.0,
            "Tier 1: the bound seed's taint reserve is held at most this share of the cap, so it grows once a beat, not up to four times.");
    public static final ModConfigSpec.IntValue RITE_ACCEL_TICKS = ri("acceleratedTicks", 6000, 20, 10000000,
            "Tier 0 (no capitals, Botania loaded): ticks the accelerated seed's fibre storm lasts.");
    public static final ModConfigSpec.IntValue RITE_ACCEL_SPREADS = ri("acceleratedSpreadsPerBeat", 6, 1, 64,
            "Tier 0: fibre spreads per beat inside a disc growing from the crater rim to rim plus acceleratedReach.");
    public static final ModConfigSpec.IntValue RITE_ACCEL_REACH = ri("acceleratedReach", 16, 0, 128, "Tier 0: blocks beyond the rim the fibre disc reaches at the end.");
    public static final ModConfigSpec.IntValue RITE_ACCEL_CHILD_FIRST = ri("acceleratedChildFirstTick", 2000, 0, 10000000, "Tier 0: tick of the first child seed.");
    public static final ModConfigSpec.IntValue RITE_ACCEL_CHILD_EVERY = ri("acceleratedChildEvery", 1000, 1, 10000000, "Tier 0: ticks between child seeds (three in all, 120 degrees apart just outside the rim).");
    public static final ModConfigSpec.DoubleValue RITE_ACCEL_CHILD_MARGIN = rd("acceleratedChildMargin", 2.0, 0.0, 64.0,
            "Tier 0: blocks added to Thaumaturge's own seed spacing (0.8 x taintSpreadArea, read live) for a child seed's distance from the hex; never closer than rim + 2.");
    public static final ModConfigSpec.IntValue RITE_BOUND_HOLD_TICKS = ri("boundHoldEvery", 40, 20, 100000,
            "Tier 1: ticks between the line-holding passes (taint beyond the crater rim, within the seed's reach, is decayed). A first guess.");
    public static final ModConfigSpec.IntValue RITE_BOUND_HOLD_COLUMNS = ri("boundHoldColumns", 16, 1, 1024, "Tier 1: random 4x4 columns of the annulus rim..claim radius decayed per pass.");
    public static final ModConfigSpec.IntValue RITE_BOUND_HOLD_DEPTH = ri("boundHoldDepth", 6, 1, 32, "Tier 1: blocks above and below the surface searched for taint blocks in each held column.");
    public static final ModConfigSpec.IntValue PAINT_RADIUS_MIN = ri("grove.radius.min", 70, 4, 256, "Tier 4: the grove's radius in blocks at the lowest burden (rounded to 4-block rings). 70 since 5d trees (was grove.radiusMin 24).");
    public static final ModConfigSpec.IntValue PAINT_RADIUS_MAX = ri("grove.radius.max", 120, 4, 256, "Tier 4: the grove's radius in blocks at the highest burden. 120 since 5d trees (was grove.radiusMax 48).");
    public static final ModConfigSpec.IntValue HERO_PAST_LIP = ri("growth.heroPastLip", 4, 0, 32, "Tier 4: the heroes stand this many blocks past the crater's lip, plus up to as many again (seeded).");
    public static final ModConfigSpec.IntValue RING_SECONDS = ri("grove.ringSeconds", 4, 1, 120, "The ripple: every 4-block ring of the grove is painted in this many seconds, so the meadow spreads at one speed (a ring may take longer while chunks wait or the step budget is spent).");
    public static final ModConfigSpec.IntValue WAVE_LAG_SECONDS = ri("growth.waveLagSeconds", 2, 0, 600, "The trees' wave runs on one clock from sight returning, this many seconds behind the meadow's ripple.");
    public static final ModConfigSpec.IntValue WATCH_RANGE = ri("growth.watchRange", -1, -1, 1024, "A growing tree with no player within this many blocks grows to its height in one step (nobody can see it): -1 = the server's view distance, 0 = always in small steps.");
    public static final ModConfigSpec.DoubleValue PAINT_BURDEN_LOW = rd("grove.burdenLow", 0.85, 0.0, 10.0, "Tier 4: burden, as a multiple of the hex threshold, at which the grove has its smallest radius.");
    public static final ModConfigSpec.DoubleValue PAINT_BURDEN_SPAN = rd("grove.burdenSpan", 0.65, 0.01, 10.0, "Tier 4: how many thresholds of burden above the low point it takes to reach the largest radius.");
    public static final ModConfigSpec.IntValue PAINT_WAITING_CHECKS = ri("grove.waitingChecks", 4, 1, 64, "Chunks that were not loaded when their ring passed, re-checked per step.");
    public static final ModConfigSpec.IntValue PAINT_BUDGET_MS = ri("grove.stepBudgetMs", 4, 1, 50, "Milliseconds after which a painter step stops adding decoration (flora, in cycle 2c) and leaves the rest to the next step.");
    // ---------------------------------------------------------------- phase 5d cycle 4e: the finale (swell and blindness)
    public static final ModConfigSpec.IntValue FINALE_MIN_HOLD = ri("finale.minHold", 90, 30, 1200, "Ticks after the last unit before the swell may collapse (5.5 s with no grove to paint).");
    // ---------------------------------------------------------------- phase 5d cycle 4f: roots by capitals
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends Double>> RITE_ROOT_CHANCES = B
            .comment("Chance that strangling roots (the grove's variant C sequence) come up on a Taint Seed, by number of Verdant Capitals (0 to 4 entries, in order). Rolled once per seed: during the fight for seeds the Vessel's taint brings, and at the ending for every seed the grove's painting reaches (tier 4 is all of them).")
            .defineList("vessel.rite.rootChanceByCapitals", java.util.List.of(0.0, 0.25, 0.5, 0.75, 1.0), () -> 0.0,
                    o -> o instanceof Double d && d >= 0.0 && d <= 1.0);
    public static final ModConfigSpec.IntValue RITE_ROOT_OVERLAP = ri("rootOverlap", 3, 1, 32, "Most root sequences that may play at once within 24 blocks of each other; a seed rooted beyond that waits for the earliest to end.");

    public static final ModConfigSpec.IntValue FINALE_FIXED_HOLD = ri("finale.fixedHold", 140, 30, 1200, "Tiers 0-2 (no grove to paint): ticks of full blindness while the taint is laid, before the collapse (140 + 20 fade is about 8 s).");
    public static final ModConfigSpec.IntValue FINALE_LIGHT_TICKS = ri("finale.lightTicks", 80, 20, 1200, "Tiers 3-4: ticks of the light stage (reduced shadow, the wisp rises, pulses and bursts) between the crater being painted and the collapse.");
    public static final ModConfigSpec.BooleanValue FINALE_LIGHT_DARKNESS = B.comment("Tiers 3-4: also refresh vanilla Darkness for those inside during the light stage. Off: the client's own shadow veil carries the shadow (with Darkness stacked on it the stage reads near black).")
            .define("vessel.rite.finale.lightStageDarkness", false);
    public static final ModConfigSpec.IntValue FINALE_LIGHT_BURST = ri("finale.lightBurst", 70, 5, 1200, "Tiers 3-4: ticks into the light stage at which the wisp bursts (the approved mockup: it appears at 10, rises for 30, pulses for 30).");
    public static final ModConfigSpec.IntValue FINALE_TIMEOUT = ri("finale.timeout", 400, 60, 6000, "Ticks after which the swell collapses whatever the painter has done.");
    public static final ModConfigSpec.IntValue FINALE_BLIND_TICKS = ri("finale.blindTicks", 40, 10, 400, "Blindness and darkness given to each player inside the swell, refreshed every 10 ticks.");
    public static final ModConfigSpec.IntValue FINALE_PAINT_PACE = ri("finale.paintPace", 3, 1, 16, "Chunk groups the painter takes a beat while the swell hides the crater (1 = the normal pace).");
    public static final ModConfigSpec.IntValue FINALE_SEED_GAP = ri("finale.seedGap", 400, 0, 12000, "Ticks after sight returns before the grove may begin to kill the seed.");
    public static final ModConfigSpec.IntValue FINALE_FLASH_RANGE = ri("finale.flashRange", 192, 16, 1024, "Players this close to the collapse are told about the flash.");

    // ---------------------------------------------------------------- phase 5d cycle 4e: trees grown on screen and the growth boost
    public static final ModConfigSpec.DoubleValue GROW_FALLOFF = rd("growth.falloff", 2.5, 0.0, 20.0, "Growth target fraction g(d) = exp(-falloff * d / R), d the distance from the hex, R the grove's radius (1.00 / 0.54 / 0.29 / 0.15 / 0.08 at d/R 0 / .25 / .5 / .75 / 1).");
    public static final ModConfigSpec.IntValue GROW_APEX_PULSES = ri("growth.apexPulses", 500, 1, 100000, "Growth pulses a Thaumaturge grove sapling at the hex is given (a sapling at distance d gets this x g(d)); measured: a sapling never uses its soil up (cells grow about 3 per 100 pulses to 2000 and beyond), so this is a choice: 500 pulses is 50 s for the tree at the hex and about a 37-block young tree.");
    public static final ModConfigSpec.IntValue GROW_STAGE_TICKS = ri("growth.stageTicks", 40, 2, 24000, "Ticks between the stages of a landmark tree (8 stages).");
    public static final ModConfigSpec.IntValue GROW_STEP_TICKS = ri("growth.stepTicks", 2, 1, 200, "Ticks between growth steps (one pulse per sapling per step, within the budget).");
    public static final ModConfigSpec.IntValue GROW_BUDGET_MS = ri("growth.budgetMs", 2, 1, 50, "Milliseconds a growth step may take; the rest waits for the next step.");
    public static final ModConfigSpec.IntValue GROW_START_DELAY = ri("growth.startDelay", 40, 0, 24000, "Ticks after sight returns before the grove's trees begin to grow.");
    public static final ModConfigSpec.IntValue GROW_DAY_TICKS = ri("growth.dayTicks", 24000, 20, 10000000, "Ticks between the extra stages a landmark left young matures by, until whole.");
    // the grove's own trees and the hill (user decisions 2026-11-23, docs/design/5d-grove-trees.md)
    public static final ModConfigSpec.IntValue HILL_PAST_LIP = ri("growth.hill.pastLip", 12, 0, 64, "Tier 4: the grove reaches at least this many blocks past the crater's widest lip (its radius is raised to it, in 4-block rings).");
    public static final ModConfigSpec.IntValue HILL_TOP = ri("growth.hill.top", 26, 4, 64, "Canopy height (above the soil) of the finale's trees beside the heroes, after the finale and as their cap: the top of the hill.");
    public static final ModConfigSpec.IntValue HILL_EDGE = ri("growth.hill.edge", 2, 1, 32, "Canopy height of the finale's trees at the grove's edge right after the finale (young saplings).");
    public static final ModConfigSpec.DoubleValue HILL_POND_SHARE = rd("growth.hill.pondShare", 0.3, 0.0, 1.0, "The hill's height at the pond's edge as a share of its top (inside the crater the forest slopes down toward the pond).");
    public static final ModConfigSpec.IntValue GLADE_CAP = ri("growth.gladeCap", 19, 4, 64, "Cap of regular grove trees (a greatwood or silverwood planted in a grove other than by the finale), and the floor of the finale trees' hill cap.");
    public static final ModConfigSpec.IntValue HERO_FINALE = ri("growth.heroFinale", 30, 4, 64, "Canopy height of the three heroes right after the finale.");
    public static final ModConfigSpec.IntValue HERO_CAP = ri("growth.heroCap", 40, 4, 64, "Cap of the three heroes (the only trees that reach it).");
    public static final ModConfigSpec.IntValue FINALE_GROW_SECONDS = ri("growth.finaleSeconds", 30, 2, 600, "Seconds the finale's trees take to grow to their height on screen, in small steps (they start when sight returns).");
    public static final ModConfigSpec.IntValue FINALE_BUDGET_MS = ri("growth.finaleBudgetMs", 4, 1, 50, "Milliseconds a growth step may take while a grove has trees growing in small steps (the larger of this and growth.budgetMs).");
    public static final ModConfigSpec.IntValue SAPLING_BELOW = ri("growth.saplingBelow", 6, 1, 32, "A finale tree whose target canopy is under this stays a sapling (the grove's edge) and grows on naturally.");
    public static final ModConfigSpec.IntValue HEROES = ri("growth.heroes", 3, 1, 12, "Tier 4: hero trees per grove, whatever its radius.");
    public static final ModConfigSpec.BooleanValue GROVE_NO_DECAY = B
            .comment("Within a Concilium Grove whose job is still running or whose mana pond still gives mana, Dynamic Trees' leaves never die (hydration or light) and its branches never rot or catch disease. A tree that is cut down is still cleaned up.")
            .define("vessel.rite.growth.noDecay", true);
    public static final ModConfigSpec.DoubleValue GROVE_BOOST = rd("growth.boost", 0.25, 0.0, 1.0, "Within the Concilium Grove, crops and trees get this chance of one extra growth step (0.25 = 25% faster; 0 = off).");

    public static final ModConfigSpec.DoubleValue GROVE_NODE_CHANCE = rd("grove.nodeChance", 0.15, 0.0, 1.0,
            "Chance that a fully painted grove chunk gets one wild aura node (at most one per chunk, no cap per grove; a chunk that already holds a node gets none).");

    // ---------------------------------------------------------------- phase 5d cycle 3: the mana pond (first guesses, all config)
    private static ModConfigSpec.IntValue pi(String path, int def, int min, int max, String comment) {
        return B.comment(comment).defineInRange("grove.pond." + path, def, min, max);
    }

    public static final ModConfigSpec.IntValue POND_MANA_PER_CHUNK = pi("manaPerChunk", 10000, 0, 100000000, "Residual mana the pond starts with, per painted grove chunk (16 painted quart columns).");
    public static final ModConfigSpec.IntValue POND_MANA_PER_BURDEN = pi("manaPerBurden", 1000, 0, 100000000, "Tier 4: residual mana the pond starts with, per point of the rite's burden (user, 2026-11-23: what the Vessel swallowed comes back as mana; about 0.34-0.6 M). Tier 3 keeps the per-chunk and per-node rule.");
    public static final ModConfigSpec.IntValue POND_MANA_PER_NODE = pi("manaPerNode", 50000, 0, 100000000, "Residual mana the pond starts with, per aura node inside the grove when the pond is made.");
    public static final ModConfigSpec.IntValue POND_MANA_PER_VIS = pi("manaPerVis", 1000, 1, 100000000, "Mana one vis drained from a grove node refills the residual with.");
    public static final ModConfigSpec.IntValue POND_OVERDRAW_STEP = pi("overdrawStep", 50000, 1, 1000000000, "With every grove node empty, each this much mana drawn breaks the weakest node into a flux rift.");
    public static final ModConfigSpec.IntValue POND_OVERDRAW_OFFER = pi("overdrawOffer", 1000, 1, 1000000, "Mana the pond offers a spreader per draw while it is running on overdraw (every node empty).");
    public static final ModConfigSpec.IntValue POND_NODE_REFRESH_SECONDS = pi("nodeRefreshSeconds", 30, 0, 100000, "At most this often (seconds, on a draw) the pond re-reads the list of aura nodes inside the grove.");
    public static final ModConfigSpec.DoubleValue POND_RIFT_PER_CAPACITY = B.comment("Size of the flux rift a broken node becomes, per point of the node's capacity (the total of its base aspects), before the clamp.",
            "Thaumaturge's default wild node (base aura 100) has a total of about 50 to 99, median about 75, so 1/3 gives it 25.")
            .defineInRange("grove.pond.riftSizePerCapacity", 1.0 / 3.0, 0.0, 10.0);
    public static final ModConfigSpec.IntValue POND_RIFT_MIN = pi("riftSizeMin", 10, 5, 100, "Smallest flux rift a broken node becomes.");
    public static final ModConfigSpec.IntValue POND_RIFT_MAX = pi("riftSizeMax", 50, 5, 200, "Largest flux rift a broken node becomes.");

    public static final ModConfigSpec SPEC = B.build();

    private TCConfig() {
    }
}
