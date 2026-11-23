package dev.movingcastle.thaumicconcilium.entity;

import com.leclowndu93150.thaumaturge.api.taint.TaintApi;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.leclowndu93150.thaumaturge.content.entity.AbstractTaintSeed;
import com.leclowndu93150.thaumaturge.content.taint.spread.TaintFeeding;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.registry.TCItems;
import dev.movingcastle.thaumicconcilium.world.GrovePainter;
import dev.movingcastle.thaumicconcilium.world.VesselDebts;
import dev.movingcastle.thaumicconcilium.world.VesselResearch;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Phase 5c cycle 4: what happens after Mass 0. The motes at the subdue, the fibre pulses and ecological pressure of a drain in progress,
 * and the Taint Seed left when the last unit is gone. The pure rules are static and tested directly; the Vessel holds the state
 * (charge, drained, spilled, progress) and calls in here.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class VesselRite {
    /** The tag on the seed that records its score (for the tests and the QA chapter). */
    public static final String SCORE_KEY = "thaumicconcilium_vessel_score";

    private VesselRite() {
    }

    // ---------------------------------------------------------------- pure rules
    /** The scar's score: every unbatted clot counts one, every {@code perPoint} goo spills one. */
    public static int seedScore(int missedClots, int spills, int spillsPerPoint) {
        return Math.max(0, missedClots) + Math.max(0, spills) / Math.max(1, spillsPerPoint);
    }

    /** A Prime Taint Seed from {@code primeScore} up, a plain one below. */
    public static boolean isPrime(int score, int primeScore) {
        return score >= primeScore;
    }

    /** The seed's health as a multiple of its base: 1 plus {@code perPoint} a point, at most 3. */
    public static double healthFactor(int score, double perPoint) {
        return Math.min(3.0, 1.0 + Math.max(0, score) * perPoint);
    }

    /** The share of its taint reserve a new seed starts with: linear in the score, full at {@code fullScore}. */
    public static float reserveFraction(int score, int fullScore) {
        return Mth.clamp(score / (float) Math.max(1, fullScore), 0.0F, 1.0F);
    }

    /** The pulse ring's radius after {@code drained} of {@code total} units: {@code min} widening linearly to {@code max}. */
    public static double ringRadius(int drained, int total, int min, int max) {
        double f = Mth.clamp(drained / (double) Math.max(1, total), 0.0, 1.0);
        return min + (max - min) * f;
    }

    /** Whether the unit just drained (the {@code drainedAfter}-th) also gives normal warp. */
    public static boolean normalWarpDue(int drainedAfter, int every) {
        return drainedAfter > 0 && drainedAfter % Math.max(1, every) == 0;
    }

    /** Mana per tick for a capped anchor: its unit's mana over the unit's ticks, rounded up (250,000 / 1200 = 209). */
    public static int manaPerTick(int manaPerUnit, int unitTicks) {
        return (int) Math.max(1L, (manaPerUnit + (long) Math.max(1, unitTicks) - 1L) / Math.max(1, unitTicks));
    }

    /** Motes one participant receives: one, and a second at {@code chance}. */
    public static int motesFor(RandomSource random, double chance) {
        return 1 + (random.nextDouble() < chance ? 1 : 0);
    }

    // ---------------------------------------------------------------- the subdue: motes and the field note
    /** Mass 0: every participant gets motes (offline ones on their next login), and the Vessel field note completes for them. */
    public static void onSubdued(ServerLevel level, Vessel vessel) {
        for (UUID id : vessel.participants()) {
            ServerPlayer p = Vessel.playerById(level, id);
            if (p != null) {
                VesselResearch.grantNote(p, VesselResearch.THE_VESSEL, 0);
            }
        }
        vessel.subdueMotes().clear();
        vessel.subdueMotes().putAll(awardMotes(level, vessel, level.random)); // the ending's top-up roll reads what each was given
    }

    /** Rolls and hands out the motes. Returns what each participant was given or is owed (for the tests). */
    public static Map<UUID, Integer> awardMotes(ServerLevel level, Vessel vessel, RandomSource random) {
        Map<UUID, Integer> given = new LinkedHashMap<>();
        for (UUID id : vessel.participants()) {
            int n = motesFor(random, TCConfig.RITE_MOTE_SECOND.get());
            given.put(id, n);
            ServerPlayer p = Vessel.playerById(level, id);
            if (p != null) {
                giveMotes(p, n);
            } else {
                VesselDebts.get(level.getServer()).oweMotes(id, n);
            }
        }
        return given;
    }

    /** Puts motes in the inventory, or at the player's feet when there is no room. */
    public static void giveMotes(ServerPlayer player, int count) {
        ItemStack stack = new ItemStack(TCItems.PRIMORDIAL_MOTE.get(), count);
        if (!player.getInventory().add(stack) && !stack.isEmpty()) {
            player.drop(stack, false);
        }
        player.displayClientMessage(Component.translatable("message.thaumicconcilium.mote_given", count), false);
        VesselResearch.grantNote(player, VesselResearch.MOTE, 0);
    }

    /**
     * Phase 5d (cycle 2's tier-4 ending calls this): removes {@code amount} permanent warp from a participant now if they are online, otherwise
     * queues it in {@link VesselDebts} for their next login. Returns whether it was applied at once.
     */
    public static boolean removePermanentWarp(ServerLevel level, UUID id, int amount) {
        if (amount <= 0) {
            return true;
        }
        ServerPlayer p = Vessel.playerById(level, id);
        if (p != null) {
            WarpHelper.addWarp(p, -amount, WarpType.PERMANENT);
            return true;
        }
        VesselDebts.get(level.getServer()).queueWarpRemoval(id, amount);
        return false;
    }

    /** Applies (and clears) the permanent-warp removal queued for a player. Returns how much was owed. */
    public static int applyWarpRemoval(ServerPlayer player, VesselDebts data) {
        int owed = data.takeWarpRemoval(player.getUUID());
        if (owed > 0) {
            WarpHelper.addWarp(player, -owed, WarpType.PERMANENT); // Thaumaturge clamps at what they have
        }
        return owed;
    }

    /** A participant who was away when the Vessel was subdued gets what they are owed. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            VesselDebts data = VesselDebts.get(player.getServer());
            applyWarpRemoval(player, data);
            if (data.takeGlade(player.getUUID())) {
                VesselResearch.grantNote(player, VesselResearch.THE_GLADE, 0);
            }
            if (data.motes().containsKey(player.getUUID())) {
                int owed = data.takeMotes(player.getUUID());
                if (owed > 0) {
                    VesselResearch.grantNote(player, VesselResearch.THE_VESSEL, 0); // the subdue's note, missed while away
                    giveMotes(player, owed);
                }
            }
        }
    }

    // ---------------------------------------------------------------- a drain in progress
    /**
     * One beat of a drain: a few fibre pulses and ecological-pressure points at random spots in a ring around the hex that widens with
     * the units drained. Only chunks that are loaded are touched; a pause (no tether) never reaches here.
     */
    /** Pulses thrown since the game started (the tests compare it: the count does not depend on the capitals). */
    public static long pulsesThrown;

    public static void pulse(ServerLevel level, Vessel vessel) {
        BlockPos centre = vessel.hexPos();
        double radius = ringRadius(vessel.drained(), TCConfig.RITE_CHARGE.get(), TCConfig.RITE_RING_MIN.get(), TCConfig.RITE_RING_MAX.get());
        float pressure = TCConfig.RITE_PRESSURE.get().floatValue();
        for (int k = 0; k < TCConfig.RITE_PULSES.get(); k++) {
            pulsesThrown++;
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            double dist = Math.max(2.0, radius - level.random.nextDouble() * 4.0);
            int x = centre.getX() + (int) Math.round(Math.cos(angle) * dist);
            int z = centre.getZ() + (int) Math.round(Math.sin(angle) * dist);
            if (!level.hasChunkAt(new BlockPos(x, centre.getY(), z))) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos at = new BlockPos(x, y, z);
            TaintApi.spreadFibres(level, at, true);
            TaintApi.addEcologicalPressure(level, at, pressure);
        }
    }

    // ---------------------------------------------------------------- the end: the scar
    /** The last unit is drained: the Vessel is gone and a Taint Seed stands where it hung. */
    public static void finish(ServerLevel level, Vessel vessel) {
        int tier = vessel.recordCapitals(level); // before the shards crumble
        vessel.endRite(level);
        crumbleAnchors(level, vessel);
        // phase 5d: the seed, its scar, the extra motes and the warp, by the number of capitals
        VesselCrater crater = VesselEndings.craterOf(vessel);
        AbstractTaintSeed seed = VesselEndings.apply(level, vessel, tier, crater);
        for (ServerPlayer p : Vessel.players(level)) {
            if (p.distanceToSqr(vessel) <= 64.0 * 64.0) {
                p.displayClientMessage(Component.translatable("message.thaumicconcilium.rite_done"), true);
            }
        }
        // cycle 4e: the Vessel is not gone yet: it swells over the crater (blindness inside) while the painter works, then collapses
        vessel.beginDying(level, VesselDeath.swellRadius(GrovePainter.maxRim(crater)));
    }

    /** The last unit: each anchor shard crumbles. The block goes, leaving a burst of its own rubble particles and the stone's break sound. */
    public static void crumbleAnchors(ServerLevel level, Vessel vessel) {
        for (BlockPos pos : vessel.anchors()) {
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
            if (!state.is(dev.movingcastle.thaumicconcilium.registry.TCBlocks.ANCHOR.get())) {
                continue;
            }
            if (level.getBlockState(pos.above()).is(dev.movingcastle.thaumicconcilium.registry.TCBlocks.VERDANT_CAPITAL.get())) {
                level.destroyBlock(pos.above(), true); // the capital drops back as an item
            }
            level.removeBlock(pos, false);
            level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, state),
                    pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 24, 0.35, 0.25, 0.35, 0.05);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.STONE_BREAK, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.7F);
        }
    }

    /** Leaves the scar: a Taint Seed (Prime from the configured score), stronger and fuller the higher the score. Returns it. */
    public static AbstractTaintSeed spawnSeed(ServerLevel level, BlockPos hex, int missedClots, int spills) {
        int score = seedScore(missedClots, spills, TCConfig.RITE_SPILL_PER_POINT.get());
        boolean prime = isPrime(score, TCConfig.RITE_PRIME_SCORE.get());
        EntityType<? extends AbstractTaintSeed> type = prime ? TTEntities.TAINT_SEED_PRIME.get() : TTEntities.TAINT_SEED.get();
        AbstractTaintSeed seed = type.create(level);
        seed.moveTo(hex.getX() + 0.5, hex.getY() + 1.0, hex.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
        AttributeInstance health = seed.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(health.getBaseValue() * healthFactor(score, TCConfig.RITE_SEED_HEALTH_PER_POINT.get()));
        }
        seed.setHealth(seed.getMaxHealth());
        seed.setTaintReserve(reserveFraction(score, TCConfig.RITE_SEED_RESERVE_SCORE.get()) * TaintFeeding.reserveCap());
        seed.setPersistenceRequired();
        seed.getPersistentData().putInt(SCORE_KEY, score);
        level.addFreshEntity(seed);
        return seed;
    }
}
