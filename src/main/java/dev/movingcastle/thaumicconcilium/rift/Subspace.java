package dev.movingcastle.thaumicconcilium.rift;

import com.leclowndu93150.thaumaturge.content.entity.EntityFluxRift;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.item.GemCast;
import dev.movingcastle.thaumicconcilium.item.RiftGem;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Gem of Rifts' subspace: interdimensional rifts and the Causal Bouillon crossing. The rift rules are the original's
 * ({@code RiftEntity} 296-370, 416, 463, 517): it grows from 5 towards 50, throws arc lightning, never eats blocks, hurts nobody, fires
 * no events, drops nothing, and collapses when it reaches 50 or is older than 200 ticks. A player touching it with a Gem of Rifts in the
 * main hand (not riding, not ridden) crosses. Thaumaturge's own rift tick is cancelled for these rifts ({@link ConciliumRifts}), which is what
 * keeps its block-eating sweep, damage and collapse loot away; this class does the ticking, and only while the rift lives.
 */
public final class Subspace {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, ThaumicConcilium.id("causal_bouillon"));
    public static final net.minecraft.resources.ResourceLocation BOSS_RESEARCH = ThaumicConcilium.id("brightest_one");
    public static final int MAX_SIZE = 50;
    public static final int START_SIZE = 5;
    public static final int MAX_AGE = 200;
    /** Original range for "another rift is near". */
    public static final double NEAR = 16.0;
    private static final int SEARCH_RADIUS = 8;

    private Subspace() {
    }

    // ---------------------------------------------------------------- pure maths
    /** Exit scaling: inflates space (x10) by default, deflates (/10) when REVERSE. Entering keeps x and z. */
    public static double scale(double coordinate, boolean reverse) {
        return reverse ? coordinate / 10.0 : coordinate * 10.0;
    }

    public static double clampToBorder(double v, double min, double max) {
        return Mth.clamp(v, min + 1.0, max - 1.0);
    }

    // ---------------------------------------------------------------- opening
    public static boolean noRiftNear(ServerLevel level, Player player) {
        return level.getEntitiesOfClass(EntityFluxRift.class, player.getBoundingBox().inflate(NEAR), r -> !r.isRemoved()).isEmpty();
    }

    /** Original {@code summonRift(interdimensional)}: a growing rift of up to 50. */
    @Nullable
    public static EntityFluxRift summon(ServerLevel level, Vec3 at) {
        EntityFluxRift rift = TTEntities.FLUX_RIFT.get().create(level);
        if (rift == null) {
            return null;
        }
        rift.reseed(level.random.nextInt());
        rift.moveTo(at.x, at.y, at.z, level.random.nextInt(360), 0.0F);
        rift.resize(START_SIZE);
        if (!level.addFreshEntity(rift)) {
            return null;
        }
        convert(rift);
        return rift;
    }

    /** Makes a rift interdimensional (also what a charged wisp does to a chained rift in cycle 3). */
    public static void convert(EntityFluxRift rift) {
        ConciliumRifts.mark(rift, ConciliumRifts.Kind.INTERDIMENSIONAL, null);
        ConciliumRifts.growTo(rift, MAX_SIZE);
        ConciliumRifts.data(rift).putInt("age", 0);
    }

    /** A chained rift struck by an authorised charged wisp: it goes interdimensional, collapses at once, and the Brightest One comes out of it. */
    public static void summonBoss(EntityFluxRift rift) {
        convert(rift);
        ConciliumRifts.data(rift).putBoolean("boss", true);
        rift.beginCollapse();
    }

    private static void spawnBoss(ServerLevel level, EntityFluxRift rift) {
        dev.movingcastle.thaumicconcilium.entity.mob.BrightestOne boss =
                dev.movingcastle.thaumicconcilium.registry.TCEntities.BRIGHTEST_ONE.get().create(level);
        if (boss != null) {
            boss.moveTo(rift.getX(), rift.getY(), rift.getZ(), level.random.nextFloat() * 360.0F, 0.0F);
            level.addFreshEntity(boss);
        }
    }

    // ---------------------------------------------------------------- the rift's life
    /** One server tick of an interdimensional rift (Thaumaturge's own tick is cancelled). */
    static void tick(ServerLevel level, EntityFluxRift rift) {
        CompoundTag tag = ConciliumRifts.data(rift);
        int age = tag.getInt("age") + 1;
        tag.putInt("age", age);
        if (level.random.nextInt(3) == 0) {
            sparks(level, rift);
        }
        crossings(level, rift);
        if (rift.isRemoved()) {
            return;
        }
        if (rift.isCollapsing()) {
            if (rift.currentSize() <= 1) {
                level.sendParticles(ParticleTypes.PORTAL, rift.getX(), rift.getY(), rift.getZ(), 20, 0.5, 0.5, 0.5, 0.2);
                if (tag.getBoolean("boss")) {
                    spawnBoss(level, rift);
                }
                rift.discard();
            } else {
                rift.resize(rift.currentSize() - 1);
            }
            return;
        }
        int size = rift.currentSize();
        if (size < tag.getInt("max") && level.random.nextInt(3) == 0) {
            rift.resize(size + 1);
        }
        if (rift.currentSize() >= tag.getInt("max") || age > MAX_AGE) {
            rift.beginCollapse();
        }
    }

    /** The original's arc lightning, purple, within a few blocks of the rift. */
    private static void sparks(ServerLevel level, EntityFluxRift rift) {
        Vec3 from = rift.position().add((level.random.nextFloat() - 0.5) * 5.0, (level.random.nextFloat() - 0.5) * 5.0,
                (level.random.nextFloat() - 0.5) * 5.0);
        Vec3 to = rift.position();
        for (int i = 0; i <= 6; i++) {
            Vec3 p = from.lerp(to, i / 6.0);
            level.sendParticles(ParticleTypes.WITCH, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
        }
    }

    /** Players near the rift only: a Gem of Rifts in the main hand and a point of the rift within half a block of the body. */
    private static void crossings(ServerLevel level, EntityFluxRift rift) {
        if (level.players().isEmpty()) {
            return;
        }
        AABB box = rift.getBoundingBox().inflate(1.0);
        List<ServerPlayer> near = null;
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && p.getBoundingBox().intersects(box)) {
                if (near == null) {
                    near = new ArrayList<>();
                }
                near.add(p);
            }
        }
        if (near == null) {
            return;
        }
        for (ServerPlayer p : near) {
            ItemStack gem = p.getMainHandItem();
            if (!RiftGem.isGem(gem) || p.isPassenger() || p.isVehicle() || !touches(rift, p)) {
                continue;
            }
            travel(p, gem);
        }
    }

    public static boolean touches(EntityFluxRift rift, Player player) {
        AABB body = player.getBoundingBox().inflate(0.5);
        Vec3 origin = rift.position();
        for (Vec3 point : rift.outline) {
            if (body.contains(origin.add(point))) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- crossing
    /** Where a crossing goes: the level and the x, z (before the safe-landing search). */
    public record Crossing(ServerLevel dest, double x, double z) {
    }

    /**
     * Enters the Bouillon (same x, z) or, from inside it, leaves to the selected cast's dimension (or the overworld) at x and z scaled.
     * {@code bouillon} is the Bouillon's level (a test may pass another level to stand in for it); null if it is not loaded.
     */
    @Nullable
    public static Crossing plan(ServerPlayer player, ItemStack gem, @Nullable ServerLevel bouillon) {
        ServerLevel from = player.serverLevel();
        double x = player.getX();
        double z = player.getZ();
        ServerLevel dest;
        if (bouillon == null || from.dimension() != bouillon.dimension()) {
            dest = bouillon;
        } else {
            RiftGem.State state = RiftGem.state(gem);
            dest = exitLevel(player.server, state.selected());
            x = scale(x, state.reverse());
            z = scale(z, state.reverse());
        }
        return dest == null ? null : new Crossing(dest, x, z);
    }

    public static boolean travel(ServerPlayer player, ItemStack gem) {
        Crossing crossing = plan(player, gem, player.server.getLevel(DIMENSION));
        return crossing != null && cross(player, crossing);
    }

    /** The vanilla dimension transition to a safe spot at the crossing's x, z, clamped to the destination's border. */
    public static boolean cross(ServerPlayer player, Crossing crossing) {
        ServerLevel dest = crossing.dest();
        WorldBorder border = dest.getWorldBorder();
        double x = clampToBorder(crossing.x(), border.getMinX(), border.getMaxX());
        double z = clampToBorder(crossing.z(), border.getMinZ(), border.getMaxZ());
        Vec3 target = landing(dest, x, z);
        player.changeDimension(new DimensionTransition(dest, target, Vec3.ZERO, player.getYRot(), player.getXRot(),
                e -> e.resetFallDistance()));
        return true;
    }

    /** The selected cast's dimension, the overworld when there is no cast or its dimension is gone. */
    public static ServerLevel exitLevel(MinecraftServer server, @Nullable GemCast cast) {
        if (cast != null) {
            ServerLevel level = server.getLevel(cast.dimension());
            if (level != null && cast.dimension() != DIMENSION) {
                return level;
            }
        }
        return server.overworld();
    }

    // ---------------------------------------------------------------- safe landing
    /**
     * A safe place to stand at (x, z): the surface (Nether and other roofed dimensions: an air pocket below the roof), searched in
     * growing squares up to {@value #SEARCH_RADIUS} blocks away; failing that the dimension's spawn point surface.
     */
    public static Vec3 landing(ServerLevel level, double x, double z) {
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        for (int r = 0; r <= SEARCH_RADIUS; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    Integer y = safeY(level, bx + dx, bz + dz);
                    if (y != null) {
                        return r == 0 ? new Vec3(x, y, z) : new Vec3(bx + dx + 0.5, y, bz + dz + 0.5);
                    }
                }
            }
        }
        BlockPos spawn = level.getSharedSpawnPos();
        Integer y = safeY(level, spawn.getX(), spawn.getZ());
        if (y != null) {
            return new Vec3(spawn.getX() + 0.5, y, spawn.getZ() + 0.5);
        }
        level.getChunk(bx >> 4, bz >> 4);
        return new Vec3(x, Math.max(level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz), level.getMinBuildHeight() + 1), z);
    }

    @Nullable
    static Integer safeY(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        int min = level.getMinBuildHeight();
        if (level.dimensionType().hasCeiling()) {
            int top = min + level.dimensionType().logicalHeight() - 2;
            for (int y = top; y > min + 1; y--) {
                if (standable(level, new BlockPos(x, y, z))) {
                    return y;
                }
            }
            return null;
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        return y > min && standable(level, new BlockPos(x, y, z)) ? y : null;
    }

    /** Feet and head free of collision and fluid and fire, a sturdy harmless floor below. */
    public static boolean standable(ServerLevel level, BlockPos feet) {
        BlockPos below = feet.below();
        BlockState floor = level.getBlockState(below);
        if (!floor.isFaceSturdy(level, below, Direction.UP) || floor.is(Blocks.MAGMA_BLOCK) || floor.is(Blocks.CACTUS)
                || floor.is(BlockTags.FIRE) || floor.is(BlockTags.CAMPFIRES) || !floor.getFluidState().isEmpty()) {
            return false;
        }
        for (BlockPos p : new BlockPos[]{feet, feet.above()}) {
            BlockState s = level.getBlockState(p);
            if (!s.getCollisionShape(level, p).isEmpty() || !s.getFluidState().isEmpty() || s.is(BlockTags.FIRE)) {
                return false;
            }
        }
        return true;
    }
}
