package dev.movingcastle.thaumicconcilium.entity;

import com.leclowndu93150.thaumaturge.content.entity.AbstractTaintSeed;
import com.leclowndu93150.thaumaturge.content.taint.spread.TaintFeeding;
import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.compat.HeartstoneCompat;
import dev.movingcastle.thaumicconcilium.world.GrovePainter;
import dev.movingcastle.thaumicconcilium.world.VesselAftermath;
import dev.movingcastle.thaumicconcilium.world.VesselDebts;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.fml.ModList;

/**
 * Phase 5d cycle 2a: what the Vessel leaves when its last unit drains, by the number of Verdant Capitals (the tier, 0 to 4).
 * The pure rules are static and tested directly; {@link #apply} does the work in the order seed, scar record, motes, warp.
 *
 * <ul>
 * <li>0, accelerated: a full-reserve seed with a fibre storm and three child seeds (only with Botania loaded; otherwise the 5c seed exactly).
 * <li>1, bound: the seed's fibres spread slowly out to the crater rim and no further.
 * <li>2, starving: a seed with no reserve; Thaumaturge's own rules starve it.
 * <li>3 and 4, doomed: reserve 0 and a tag, and a grove painting job ({@link GrovePainter}); the seed dies when the job's last ring closes.
 * </ul>
 */
public final class VesselEndings {
    /** Persistent-data tag on a seed left by tiers 3 and 4: the grove kills it when the last ring closes (the painter skips its column until then). */
    public static final String DOOMED_KEY = "thaumicconcilium_grove_doomed";
    /** Persistent-data tag on every seed an ending leaves: the tier (number of capitals). */
    public static final String TIER_KEY = "thaumicconcilium_vessel_tier";
    public static final int MAX_TIER = 4;

    /** The kind of seed an ending leaves. {@code PLAIN} is the 5c seed untouched. */
    public enum Kind {
        PLAIN, ACCELERATED, BOUND, STARVING, DOOMED
    }

    private VesselEndings() {
    }

    // ---------------------------------------------------------------- pure rules
    public static int tier(int capitals) {
        return Mth.clamp(capitals, 0, MAX_TIER);
    }

    /** The seed kind for a tier; tier 0 is accelerated only with Botania loaded. */
    public static Kind kindFor(int tier, boolean botania) {
        return switch (tier(tier)) {
            case 0 -> botania ? Kind.ACCELERATED : Kind.PLAIN;
            case 1 -> Kind.BOUND;
            case 2 -> Kind.STARVING;
            default -> Kind.DOOMED;
        };
    }

    /** The tier's total chance of a second mote, from the list (the last entry stands in for a missing one). */
    public static double tierChance(List<? extends Double> chances, int tier) {
        if (chances.isEmpty()) {
            return 0.0;
        }
        return Mth.clamp(chances.get(Math.min(tier(tier), chances.size() - 1)), 0.0, 1.0);
    }

    public static double tierChance(int tier) {
        return tierChance(TCConfig.RITE_MOTE_TIER_CHANCES.get(), tier);
    }

    /** The chance of a top-up roll that, after the subdue's {@code base} roll, makes the total {@code target}: (target - base) / (1 - base). */
    public static double topUpChance(double target, double base) {
        if (base >= 1.0) {
            return 0.0;
        }
        return Mth.clamp((target - base) / (1.0 - base), 0.0, 1.0);
    }

    /**
     * The extra motes one participant gets at the ending. {@code got} is what the subdue gave them: 2 gets nothing more, 1 rolls the top-up,
     * and 0 (absent at the subdue) rolls the tier's chance for one mote.
     */
    public static int extraMotes(RandomSource random, int got, double target, double base) {
        if (got >= 2) {
            return 0;
        }
        double chance = got == 1 ? topUpChance(target, base) : Mth.clamp(target, 0.0, 1.0);
        return random.nextDouble() < chance ? 1 : 0;
    }

    /** Permanent warp to remove from one participant at tier 4: {@code pair} each for a Heartstone pair of participants, {@code solo} otherwise. */
    public static int warpToRemove(boolean partnerIsParticipant, int solo, int pair) {
        return partnerIsParticipant ? pair : solo;
    }

    // ---------------------------------------------------------------- apply
    /** The crater the Vessel's break carved, rebuilt (the Vessel's own is gone once carved): two blocks past the farthest anchor. */
    public static VesselCrater craterOf(Vessel vessel) {
        BlockPos hex = vessel.hexPos();
        double far = 0.0;
        for (BlockPos a : vessel.anchors()) {
            far = Math.max(far, Math.sqrt(Mth.square(a.getX() - hex.getX()) + Mth.square(a.getZ() - hex.getZ())));
        }
        return new VesselCrater(hex, far + 2.0, vessel.getUUID().getLeastSignificantBits());
    }

    /** The whole ending, with Botania as the mod list says. Returns the seed. */
    public static AbstractTaintSeed apply(ServerLevel level, Vessel vessel, int capitals, VesselCrater crater) {
        return apply(level, vessel, capitals, crater, ModList.get().isLoaded("botania"));
    }

    /** The whole ending, in order: seed, scar record, motes, warp. */
    public static AbstractTaintSeed apply(ServerLevel level, Vessel vessel, int capitals, VesselCrater crater, boolean botania) {
        int tier = tier(capitals);
        AbstractTaintSeed seed = seed(level, vessel, tier, crater, botania);
        endingMotes(level, vessel, tier, level.random);
        if (tier == MAX_TIER) {
            removeWarp(level, vessel);
        }
        return seed;
    }

    /** The seed and its scar record (no motes, no warp). */
    public static AbstractTaintSeed seed(ServerLevel level, Vessel vessel, int tier, VesselCrater crater, boolean botania) {
        AbstractTaintSeed seed = VesselRite.spawnSeed(level, vessel.hexPos(), vessel.missedClots(), vessel.spilled());
        seed.getPersistentData().putInt(TIER_KEY, tier);
        Kind kind = kindFor(tier, botania);
        switch (kind) {
            case ACCELERATED -> seed.setTaintReserve(TaintFeeding.reserveCap());
            case STARVING -> seed.setTaintReserve(0.0F);
            case DOOMED -> {
                seed.setTaintReserve(0.0F);
                seed.getPersistentData().putBoolean(DOOMED_KEY, true);
            }
            default -> {
            }
        }
        if (kind == Kind.ACCELERATED || kind == Kind.BOUND || kind == Kind.DOOMED) {
            VesselAftermath.get(level).add(new VesselAftermath.Scar(seed.getUUID(), kind, tier, vessel.hexPos(), crater.radius(),
                    vessel.getUUID().getLeastSignificantBits(), level.getGameTime()));
        }
        if (kind == Kind.DOOMED) {
            // the grove: painted outward from the hex; the seed dies when the last ring closes (a hex in a sub-level plot gets no job, and its seed starves)
            GrovePainter.open(level, vessel.hexPos(), crater, tier, vessel.burdenAtBreak(), seed.getUUID());
        }
        return seed;
    }

    /**
     * The ending's mote roll for every participant (see {@link #extraMotes}); offline ones are owed through {@link VesselDebts}. Returns what
     * each got extra (zeros left out).
     */
    public static Map<UUID, Integer> endingMotes(ServerLevel level, Vessel vessel, int tier, RandomSource random) {
        return endingMotesAt(level, vessel, tierChance(tier), random);
    }

    /** As {@link #endingMotes}, for an explicit total chance (the tests use 0 and 1). */
    public static Map<UUID, Integer> endingMotesAt(ServerLevel level, Vessel vessel, double target, RandomSource random) {
        double base = TCConfig.RITE_MOTE_SECOND.get();
        Map<UUID, Integer> extra = new LinkedHashMap<>();
        for (UUID id : vessel.participants()) {
            int n = extraMotes(random, vessel.subdueMotes().getOrDefault(id, 0), target, base);
            if (n <= 0) {
                continue;
            }
            extra.put(id, n);
            ServerPlayer p = Vessel.playerById(level, id);
            if (p != null) {
                VesselRite.giveMotes(p, n);
            } else {
                VesselDebts.get(level.getServer()).oweMotes(id, n);
            }
        }
        return extra;
    }

    /** Tier 4: removes permanent warp from every participant (3 solo, 1 each for a Heartstone pair). Returns what was asked, per participant. */
    public static Map<UUID, Integer> removeWarp(ServerLevel level, Vessel vessel) {
        Map<UUID, Integer> removed = new LinkedHashMap<>();
        for (UUID id : vessel.participants()) {
            ServerPlayer p = Vessel.playerById(level, id);
            UUID partner = p != null ? HeartstoneCompat.partnerOf(p) : HeartstoneCompat.partnerOfOffline(level.getServer(), id);
            int n = warpToRemove(partner != null && vessel.participants().contains(partner), TCConfig.RITE_WARP_SOLO.get(), TCConfig.RITE_WARP_PAIR.get());
            removed.put(id, n);
            VesselRite.removePermanentWarp(level, id, n);
        }
        return removed;
    }
}
