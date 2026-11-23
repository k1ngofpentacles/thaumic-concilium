package dev.movingcastle.thaumicconcilium.world;

import dev.movingcastle.thaumicconcilium.TCConfig;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;

/**
 * 5d follow-up (user, 2026-11-23; {@code docs/design/5d-grove-trees.md}): no decay in a living grove. Inside a Concilium Grove whose job is
 * still running (the finale, before the pond's ledger exists) or whose mana pond still gives mana, Dynamic Trees' leaves never die and its
 * branches never rot or catch disease, so the trees grown on screen only ever gain leaves ("it's not going through seasons"). Read by the
 * optional mixins {@code GroveLeavesNoDecayMixin} and {@code GroveSpeciesNoDecayMixin}; names no Dynamic Trees class itself.
 *
 * <p>"Gives mana" is what {@link GrovePond#offer} would answer above 0, without its side effects: residual mana left, or grove nodes left to
 * refill it or to overdraw. Cheap: one chunk-attachment read for any position outside a grove, then a walk over the few jobs.
 */
public final class GroveDecay {
    /** Tests: decays prevented since the game started. */
    public static long kept;

    private GroveDecay() {
    }

    /** Whether decay is held off at {@code pos}. */
    public static boolean protects(LevelAccessor accessor, BlockPos pos) {
        if (!(accessor instanceof ServerLevel level) || !TCConfig.GROVE_NO_DECAY.get() || !GroveColumns.isGrove(level, pos)) {
            return false;
        }
        GrovePaintJob job = jobOver(level, pos);
        return job != null && active(level, job);
    }

    /** The job whose disc (its radius, plus a canopy's overhang) holds this column, or null. */
    @Nullable
    static GrovePaintJob jobOver(ServerLevel level, BlockPos pos) {
        VesselAftermath data = VesselAftermath.peek(level);
        if (data == null) {
            return null;
        }
        for (GrovePaintJob j : data.jobs()) {
            double dx = pos.getX() - j.centre.getX();
            double dz = pos.getZ() - j.centre.getZ();
            double reach = j.radius + 4.0;
            if (dx * dx + dz * dz <= reach * reach) {
                return j;
            }
        }
        return null;
    }

    /** A running job, or a finished one whose pond still gives mana. A finished grove without a pond (no Botania) decays as usual. */
    public static boolean active(ServerLevel level, GrovePaintJob job) {
        if (!job.done) {
            return true;
        }
        return job.ledgerReady && (job.residual > 0 || !GrovePond.nodes(level, job, false).isEmpty());
    }
}
