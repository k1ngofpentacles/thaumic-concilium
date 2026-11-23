package dev.movingcastle.thaumicconcilium.world;

import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.entity.VesselCrater;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;

/**
 * 5d follow-up, the grove's own trees on a hill (user decisions 2026-11-23, {@code docs/design/5d-grove-trees.md} rounds 3-4). Pure rules plus
 * the job lookups they need; names no Dynamic Trees class.
 *
 * <p>The hill: 1 on the heroes' ring, falling (smoothstep) to 0 at the grove's edge and to {@code growth.hill.pondShare} at the pond's edge.
 * Right after the finale a finale tree's canopy is {@code edge + (top - edge) x hill}; afterwards it grows to its cap
 * {@code max(glade, glade + (top - glade) x hill)}, so the hill lasts and never drops below the regular grove trees' cap. Heroes: 30 after
 * the finale, 40 cap. Regular grove trees: 19. The grove reaches at least {@code growth.hill.pastLip} past the crater's widest lip.
 */
public final class GroveHill {
    /** The three tree classes (species prefixes {@code elder_}, {@code grove_}, {@code glade_}). */
    public enum Kind { HERO, FINALE, GLADE }

    /** Branch cells stop this many blocks under the canopy cap: a dark-oak leaf cluster stands 3 above its tip (measured: branch top 18, leaf top 21). */
    public static final int LEAF_ABOVE_TIP = 3;

    private GroveHill() {
    }

    // ---------------------------------------------------------------- pure
    public static double smooth(double x) {
        x = Mth.clamp(x, 0.0, 1.0);
        return x * x * (3.0 - 2.0 * x);
    }

    /** The hill at distance {@code d} from the hex: 1 on the ring, 0 at {@code radius}, {@code pondShare} at {@code pondR}. */
    public static double hill(double d, double ring, double radius, double pondR, double pondShare) {
        if (d >= ring) {
            return 1.0 - smooth((d - ring) / Math.max(1.0, radius - ring));
        }
        return pondShare + (1.0 - pondShare) * smooth((d - pondR) / Math.max(1.0, ring - pondR));
    }

    /** A finale tree's canopy right after the finale. */
    public static int finaleCanopy(double hill) {
        int edge = TCConfig.HILL_EDGE.get();
        return (int) Math.round(edge + (TCConfig.HILL_TOP.get() - edge) * hill);
    }

    /** A finale tree's cap: the hill between the glade cap and the top, never below the glade cap. */
    public static int finaleCap(double hill) {
        int glade = TCConfig.GLADE_CAP.get();
        return Math.max(glade, (int) Math.round(glade + (TCConfig.HILL_TOP.get() - glade) * hill));
    }

    /** The highest branch cell (above the soil) a tree with this canopy cap may grow. */
    public static int branchLimit(int canopyCap) {
        return Math.max(1, canopyCap - LEAF_ABOVE_TIP);
    }

    /** The grove's radius for a crater: at least {@code pastLip} past its widest lip, in 4-block rings. */
    public static int minRadius(double craterRadius) {
        double lip = craterRadius * (1.0 + VesselCrater.RAGGED);
        return 4 * (int) Math.ceil((lip + TCConfig.HILL_PAST_LIP.get()) / 4.0);
    }

    /** As {@link #minRadius} for a measured widest lip. */
    public static int minRadiusForRim(double maxRim) {
        return 4 * (int) Math.ceil((maxRim + TCConfig.HILL_PAST_LIP.get()) / 4.0);
    }

    // ---------------------------------------------------------------- per job (cached on the job)
    /** The hill's numbers for a job: the heroes' mean ring distance and the pond's reach. */
    public record Params(double ring, double pondR) {
    }

    public static Params params(GrovePaintJob job) {
        if (job.hill instanceof Params p) {
            return p;
        }
        double sum = 0;
        int n = 0;
        for (int[] spot : GroveLife.spots(job)) {
            if (spot[2] == GroveLife.TREE) {
                sum += Math.hypot(spot[0] + 0.5 - (job.centre.getX() + 0.5), spot[1] + 0.5 - (job.centre.getZ() + 0.5));
                n++;
            }
        }
        double ring = n > 0 ? sum / n : job.radius * 0.65;
        double pondR = 0;
        for (long col : GrovePond.moundColumns(job)) {
            pondR = Math.max(pondR, Math.hypot(net.minecraft.world.level.ChunkPos.getX(col) - job.centre.getX(), net.minecraft.world.level.ChunkPos.getZ(col) - job.centre.getZ()));
        }
        Params p = new Params(ring, Math.max(2.0, pondR));
        job.hill = p;
        return p;
    }

    public static double hillAt(GrovePaintJob job, int x, int z) {
        Params p = params(job);
        double d = Math.hypot(x + 0.5 - (job.centre.getX() + 0.5), z + 0.5 - (job.centre.getZ() + 0.5));
        return hill(d, p.ring(), job.radius, p.pondR(), TCConfig.HILL_POND_SHARE.get());
    }

    /** The canopy cap of a tree of this kind rooted at {@code pos}; a finale tree outside any grove job gets the glade cap. */
    public static int cap(Kind kind, @Nullable ServerLevel level, BlockPos pos) {
        return switch (kind) {
            case HERO -> TCConfig.HERO_CAP.get();
            case GLADE -> TCConfig.GLADE_CAP.get();
            case FINALE -> {
                GrovePaintJob job = level == null ? null : GroveDecay.jobOver(level, pos);
                yield job == null ? TCConfig.GLADE_CAP.get() : finaleCap(hillAt(job, pos.getX(), pos.getZ()));
            }
        };
    }
}
