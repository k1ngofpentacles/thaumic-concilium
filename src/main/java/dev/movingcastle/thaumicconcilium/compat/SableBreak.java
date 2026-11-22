package dev.movingcastle.thaumicconcilium.compat;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/**
 * The break cinematic's Sable half (phase 5c cycle 3c): the hex's nine blocks become a sub-level that the Vessel holds to an exact
 * pose every tick (a teleport plus a velocity reset: Sable has no kinematic flag for a sub-level), then removes. Like
 * {@link SablePull}, this class names Sable (PolyForm Shield: compiled against, never shipped) and is reached only when it is loaded.
 */
public final class SableBreak {
    private SableBreak() {
    }

    /** What an assembly gave: the sub-level's id and the world position its pose started at (its centre of mass). */
    public record Held(UUID id, Vec3 base) {
    }

    /** Moves the 3x3 hex at {@code master} into a new sub-level. Null when Sable has no container here or the assembly failed. */
    @Nullable
    public static Held assemble(ServerLevel level, BlockPos master) {
        if (SubLevelContainer.getContainer(level) == null) {
            return null;
        }
        List<BlockPos> blocks = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = master.offset(dx, 0, dz);
                if (!level.getBlockState(p).isAir()) {
                    blocks.add(p);
                }
            }
        }
        if (blocks.isEmpty()) {
            return null;
        }
        BoundingBox3i bounds = new BoundingBox3i(master.getX() - 1, master.getY(), master.getZ() - 1, master.getX() + 1, master.getY(), master.getZ() + 1);
        ServerSubLevel sub = SubLevelAssemblyHelper.assembleBlocks(level, master, blocks, bounds);
        if (sub == null || sub.isRemoved()) {
            return null;
        }
        var p = sub.logicalPose().position();
        return new Held(sub.getUniqueId(), new Vec3(p.x(), p.y(), p.z()));
    }

    /** Holds the sub-level at {@code at}, turned {@code yaw} about the vertical and tilted {@code tilt}. False when it is not loaded. */
    public static boolean hold(ServerLevel level, UUID id, Vec3 at, double yaw, double tilt) {
        ServerSubLevel sub = find(level, id);
        if (sub == null) {
            return false;
        }
        RigidBodyHandle handle = RigidBodyHandle.of(sub);
        if (handle == null || !handle.isValid()) {
            return false;
        }
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        container.physicsSystem().getPipeline().resetVelocity(sub);
        handle.teleport(new Vector3d(at.x, at.y, at.z), new Quaterniond().rotateY(yaw).rotateX(tilt));
        return true;
    }

    /** Removes the sub-level and everything in it. True when it is gone (removed now, or already absent from a loaded container). */
    public static boolean remove(ServerLevel level, UUID id) {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return true;
        }
        SubLevel sub = container.getSubLevel(id);
        if (sub == null) {
            return false;
        }
        if (!sub.isRemoved()) {
            container.removeSubLevel(sub, SubLevelRemovalReason.REMOVED);
        }
        return true;
    }

    /** Whether a sub-level with that id is loaded (for the pack test). */
    public static boolean exists(ServerLevel level, UUID id) {
        return find(level, id) != null;
    }

    @Nullable
    private static ServerSubLevel find(ServerLevel level, UUID id) {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return null;
        }
        return container.getSubLevel(id) instanceof ServerSubLevel s && !s.isRemoved() ? s : null;
    }
}
