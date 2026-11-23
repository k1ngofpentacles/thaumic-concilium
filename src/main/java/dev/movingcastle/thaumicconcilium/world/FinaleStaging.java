package dev.movingcastle.thaumicconcilium.world;

import com.leclowndu93150.thaumaturge.content.world.space.LevelSpaces;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import dev.movingcastle.thaumicconcilium.entity.Vessel;
import dev.movingcastle.thaumicconcilium.entity.VesselCrater;
import dev.movingcastle.thaumicconcilium.entity.VesselEndings;
import dev.movingcastle.thaumicconcilium.entity.VesselRite;
import dev.movingcastle.thaumicconcilium.registry.TCBlocks;
import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import dev.movingcastle.thaumicconcilium.rift.HexSink;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * QA staging of the tier 3-4 ending (phase 5d, the teaser; {@code /thaumicconcilium qa finale <tier>}): a crater carved where the
 * player stands, four anchors on it, a Verdant Capital on {@code tier} of them, a subdued Vessel on its last unit, and then the rite is finished
 * the way a drained last unit finishes it ({@link VesselRite#finish}), so the real finale, painter, growth and roots run. A few wild
 * Taint Seeds (no score, as the fight's pulses leave them) are seeded inside the grove's reach just after the finish; the Vessel is already
 * dying, so the fight's join roll leaves them, and the painter's reach rolls them at the capital count and the real root rule eats them.
 * The player is moved to the crater's edge (the crater is carved under their feet). Runs only when the command does.
 */
public final class FinaleStaging {
    private FinaleStaging() {
    }

    /** Stages the rite where {@code player} stands; returns the capitals placed, or -1 when it cannot be staged. */
    public static int stage(ServerLevel level, ServerPlayer player, int tier) {
        BlockPos hex = BlockPos.containing(player.position()).below();
        if (LevelSpaces.isPlot(level, hex)) {
            return -1;
        }
        // the anchors' columns first (the crater's size comes from the farthest), then the crater, then the anchors on its floor
        List<double[]> spokes = HexSink.anchorSpokes(level.random);
        List<BlockPos> columns = new ArrayList<>();
        for (double[] s : spokes) {
            columns.add(new BlockPos(hex.getX() + (int) Math.round(Math.cos(s[0]) * s[1]), hex.getY(), hex.getZ() + (int) Math.round(Math.sin(s[0]) * s[1])));
        }
        Vessel vessel = TCEntities.VESSEL.get().create(level);
        vessel.moveTo(hex.getX() + 0.5, hex.getY() + 3.0, hex.getZ() + 0.5, 0.0F, 0.0F);
        Map<UUID, Integer> contributions = new LinkedHashMap<>();
        contributions.put(player.getUUID(), 400);
        vessel.setup(400, contributions, columns, hex);
        VesselCrater crater = VesselEndings.craterOf(vessel);
        crater.finish(level);
        List<BlockPos> anchors = new ArrayList<>();
        for (BlockPos c : columns) {
            level.getChunk(c.getX() >> 4, c.getZ() >> 4);
            BlockPos at = new BlockPos(c.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c.getX(), c.getZ()), c.getZ());
            level.setBlock(at, TCBlocks.ANCHOR.get().defaultBlockState(), 3);
            anchors.add(at);
        }
        int capitals = Math.max(0, Math.min(4, tier));
        for (int i = 0; i < capitals; i++) {
            level.setBlock(anchors.get(i).above(), TCBlocks.VERDANT_CAPITAL.get().defaultBlockState(), 3);
        }
        vessel.setup(400, contributions, anchors, hex);
        vessel.setFightState(Vessel.SUBDUED, 32, 0);
        vessel.setCharge(20);
        vessel.setClotsEnabled(false);
        level.addFreshEntity(vessel);
        // the player stands at the crater's edge (the hole was cut under their feet)
        int px = hex.getX() + (int) Math.ceil(crater.radius()) + 8;
        int pz = hex.getZ();
        level.getChunk(px >> 4, pz >> 4);
        double py = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, px, pz);
        player.teleportTo(level, px + 0.5, py, pz + 0.5, 90.0F, 0.0F);
        if (Math.abs(player.getX() - (px + 0.5)) > 0.5) { // a player with no connection (the tests' fake one) is not moved by the teleport packet
            player.moveTo(px + 0.5, py, pz + 0.5, 90.0F, 0.0F);
        }
        finishNow(level, vessel);
        return capitals;
    }

    /** The last unit is drained, and the wild seeds are seeded in the grove's reach. */
    public static void finishNow(ServerLevel level, Vessel vessel) {
        BlockPos hex = vessel.hexPos();
        VesselRite.finish(level, vessel);
        GrovePaintJob job = VesselAftermath.get(level).jobs().stream().filter(j -> j.centre.equals(hex)).reduce((a, b) -> b).orElse(null);
        if (job != null) {
            seedWildSeeds(level, hex, job);
        }
    }

    /** 3 to 5 wild Taint Seeds between just past the crater's rim and just inside the grove's radius (so the painter's rings reach every one). */
    public static int seedWildSeeds(ServerLevel level, BlockPos hex, GrovePaintJob job) {
        int n = 3 + level.random.nextInt(3);
        double lo = Math.min(job.craterRadius + 1.5, job.radius - 2.5);
        double hi = Math.max(lo + 1.0, job.radius - 1.5);
        int made = 0;
        double phase = level.random.nextDouble() * Math.PI * 2.0;
        for (int i = 0; i < n; i++) {
            double a = phase + i * (Math.PI * 2.0 / n) + (level.random.nextDouble() - 0.5) * 0.6;
            double d = lo + level.random.nextDouble() * (hi - lo);
            int x = hex.getX() + (int) Math.round(Math.cos(a) * d);
            int z = hex.getZ() + (int) Math.round(Math.sin(a) * d);
            if (!level.hasChunkAt(new BlockPos(x, hex.getY(), z))) {
                continue;
            }
            var seed = TTEntities.TAINT_SEED.get().create(level);
            seed.moveTo(x + 0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
            seed.setPersistenceRequired();
            level.addFreshEntity(seed);
            made++;
        }
        return made;
    }
}
