package dev.movingcastle.thaumicconcilium.client;

import net.minecraft.world.level.Level;

/**
 * The time the Vessel's disc is drawn at (phase 5c, for cycle 3's renderer). Normally the level's game time; the render harness pins it
 * ({@code vessel_time} action) so stills repeat. The server's contact test uses the real game time with the same pure function
 * ({@code VesselPull.inDisc}).
 */
public final class VesselTime {
    /** Pinned disc time in ticks, or -1 for the level's game time. Set only by the render harness. */
    public static volatile long pinned = -1L;

    private VesselTime() {
    }

    public static long now(Level level) {
        return pinned >= 0L ? pinned : level.getGameTime();
    }
}
