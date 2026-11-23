package dev.movingcastle.thaumicconcilium.world;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;

/**
 * Phase 5d cycle 2b: one grove painting job (see {@link GrovePainter}). Plain data, saved with {@link VesselAftermath}; a finished job
 * stays as the grove's record. Rings are numbered outward from the hex in quart columns; {@code cursor} counts the chunk groups of the
 * current ring already done, so a reload resumes at the same place.
 */
public final class GrovePaintJob {
    public final BlockPos centre;
    public final double craterRadius;
    public final long craterSeed;
    public final int tier;
    /** The grove's radius in blocks. */
    public final int radius;
    @Nullable
    public final UUID seed;
    /** The 13 pond-reserve block columns, each at y = hex level minus the crater's cut there; left bare. */
    public final List<Long> pond = new ArrayList<>();
    /** Chunks that were not loaded when their ring passed (chunk positions as longs); painted when they load. */
    public final List<Long> waiting = new ArrayList<>();
    /**
     * Decoration items (cycle 2c) queued by the life hooks, one {@code long[]} each ({@code {kind, a, b, c}}; see {@link GroveLife}); processed at
     * least one per step and as many more as the step budget allows. An item that cannot run yet (chunks not loaded) goes to the back.
     */
    public final List<long[]> decor = new ArrayList<>();
    /** Per-job cache of the life hooks' plan (landmark and sapling spots); never saved, recomputed from the seed. */
    public transient Object plan;
    /** Phase 5d cycle 3, the mana pond: its blocks stand (set once the job finishes, only with Botania). */
    public boolean pondPlaced;
    /** The pond's ledger is set up ({@code residual} holds the starting mana). The residual never refills by itself, only from node vis. */
    public boolean ledgerReady;
    /** Mana left in the pond's residual. */
    public long residual;
    /** Mana drawn beyond what the nodes could give (every grove node empty) since the last node break. */
    public long overdrawn;
    /** Nodes broken into rifts by overdraw. */
    public int breaks;
    /** Mana drawn from the pond in all, and vis drained from nodes for it (statistics). */
    public long drawn;
    public long visDrained;
    /** Per-job cache of the grove's node positions (see {@link GrovePond}); never saved. */
    public transient GrovePond.NodeCache nodeCache;
    /** Per-job cache of the livingrock mound's block columns (see {@link GrovePond#moundColumns}); never saved. */
    public transient java.util.Set<Long> moundColumns;
    public int ring;
    public int cursor;
    /** Phase 5d cycle 4e (the finale): while the game time is before this, the painter takes {@code finale.paintPace} chunk groups a step; -1 = never. */
    public long darkUntil = -1L;
    /** Game time the crater's rings were done under the swell ({@link GrovePainter#craterDone}); -1 before. */
    public long craterDoneAt = -1L;
    /** Game time sight returns (the swell's collapse ends); the seed's death waits {@code finale.seedGap} after it. -1 = no gate. */
    public long sightAt = -1L;
    /**
     * Cycle 4e: the trees still growing on screen ({@link GroveGrowth}), one {@code long[]} each: {@code {kind, pos, done, target, packed, nextAt}}. A
     * landmark's {@code pos} is its soil block, a sapling's the sapling block; saved with the job.
     */
    public final List<long[]> growth = new ArrayList<>();
    /** Never saved: the earliest {@code nextAt} among the entries (0 = look), and where the round robin goes on. */
    public transient long growthWake;
    public transient int growthCursor;
    /** Cache of the crater's farthest rim (never saved); NaN before it is worked out. */
    public transient double craterRim = Double.NaN;
    /** Whether the doomed seed has been killed and its own column handled (the last thing the painter does). */
    public boolean seedDone;
    /** Game time at which the seed's death sequence began ({@link GroveSeedDeath}); -1 before it has. */
    public long seedDeathStart = -1L;
    /** Quart columns newly painted by this job. */
    public int painted;
    public int steps;
    public long totalNanos;
    public long maxStepNanos;
    public boolean done;

    public GrovePaintJob(BlockPos centre, double craterRadius, long craterSeed, int tier, int radius, @Nullable UUID seed) {
        this.centre = centre;
        this.craterRadius = craterRadius;
        this.craterSeed = craterSeed;
        this.tier = tier;
        this.radius = radius;
        this.seed = seed;
        this.seedDone = seed == null;
    }

    public int ringCount() {
        return (this.radius + 3) / 4;
    }

    public double meanStepNanos() {
        return this.steps == 0 ? 0.0 : this.totalNanos / (double) this.steps;
    }

    public CompoundTag save() {
        CompoundTag c = new CompoundTag();
        c.putLong("centre", this.centre.asLong());
        c.putDouble("craterRadius", this.craterRadius);
        c.putLong("craterSeed", this.craterSeed);
        c.putInt("tier", this.tier);
        c.putInt("radius", this.radius);
        if (this.seed != null) {
            c.putUUID("seed", this.seed);
        }
        c.put("pond", new LongArrayTag(this.pond.stream().mapToLong(Long::longValue).toArray()));
        c.put("waiting", new LongArrayTag(this.waiting.stream().mapToLong(Long::longValue).toArray()));
        ListTag decorList = new ListTag();
        for (long[] d : this.decor) {
            decorList.add(new LongArrayTag(d));
        }
        c.put("decor", decorList);
        c.putBoolean("pondPlaced", this.pondPlaced);
        c.putBoolean("ledgerReady", this.ledgerReady);
        c.putLong("residual", this.residual);
        c.putLong("overdrawn", this.overdrawn);
        c.putInt("breaks", this.breaks);
        c.putLong("drawn", this.drawn);
        c.putLong("visDrained", this.visDrained);
        ListTag growthList = new ListTag();
        for (long[] g : this.growth) {
            growthList.add(new LongArrayTag(g));
        }
        c.put("growth", growthList);
        c.putInt("ring", this.ring);
        c.putLong("darkUntil", this.darkUntil);
        c.putLong("craterDoneAt", this.craterDoneAt);
        c.putLong("sightAt", this.sightAt);
        c.putInt("cursor", this.cursor);
        c.putBoolean("seedDone", this.seedDone);
        c.putLong("seedDeathStart", this.seedDeathStart);
        c.putInt("painted", this.painted);
        c.putInt("steps", this.steps);
        c.putLong("totalNanos", this.totalNanos);
        c.putLong("maxStepNanos", this.maxStepNanos);
        c.putBoolean("done", this.done);
        return c;
    }

    public static GrovePaintJob load(CompoundTag c) {
        GrovePaintJob j = new GrovePaintJob(BlockPos.of(c.getLong("centre")), c.getDouble("craterRadius"), c.getLong("craterSeed"), c.getInt("tier"),
                c.getInt("radius"), c.hasUUID("seed") ? c.getUUID("seed") : null);
        for (long l : c.getLongArray("pond")) {
            j.pond.add(l);
        }
        for (long l : c.getLongArray("waiting")) {
            j.waiting.add(l);
        }
        ListTag decorList = c.getList("decor", net.minecraft.nbt.Tag.TAG_LONG_ARRAY);
        for (int i = 0; i < decorList.size(); i++) {
            j.decor.add(((LongArrayTag) decorList.get(i)).getAsLongArray());
        }
        j.pondPlaced = c.getBoolean("pondPlaced");
        j.ledgerReady = c.getBoolean("ledgerReady");
        j.residual = c.getLong("residual");
        j.overdrawn = c.getLong("overdrawn");
        j.breaks = c.getInt("breaks");
        j.drawn = c.getLong("drawn");
        j.visDrained = c.getLong("visDrained");
        ListTag growthList = c.getList("growth", net.minecraft.nbt.Tag.TAG_LONG_ARRAY);
        for (int i = 0; i < growthList.size(); i++) {
            j.growth.add(((LongArrayTag) growthList.get(i)).getAsLongArray());
        }
        j.ring = c.getInt("ring");
        j.darkUntil = c.contains("darkUntil") ? c.getLong("darkUntil") : -1L;
        j.craterDoneAt = c.contains("craterDoneAt") ? c.getLong("craterDoneAt") : -1L;
        j.sightAt = c.contains("sightAt") ? c.getLong("sightAt") : -1L;
        j.cursor = c.getInt("cursor");
        j.seedDone = c.getBoolean("seedDone");
        j.seedDeathStart = c.contains("seedDeathStart") ? c.getLong("seedDeathStart") : -1L;
        j.painted = c.getInt("painted");
        j.steps = c.getInt("steps");
        j.totalNanos = c.getLong("totalNanos");
        j.maxStepNanos = c.getLong("maxStepNanos");
        j.done = c.getBoolean("done");
        return j;
    }
}
