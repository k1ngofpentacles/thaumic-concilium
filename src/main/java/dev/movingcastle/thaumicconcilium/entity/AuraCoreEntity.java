package dev.movingcastle.thaumicconcilium.entity;

import com.leclowndu93150.thaumaturge.api.aspect.AspectIndexAccess;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.Aspects;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.wands.WandChargingEvents;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import dev.movingcastle.thaumicconcilium.item.AuraCore;
import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A blank Aura Core being attuned (adapted from Gadomancy 1.7.10's {@code EntityAuraCore}, LGPL).
 * <p>
 * Ticks 0-50 are the pre-gather; ticks 50-500 scan the 9x9x9 cube around the core in a seeded shuffled order, spread evenly over
 * the 450 ticks, summing each non-air block's primal aspects (Thaumaturge's aspect index, reduced to primals). At tick 500 the
 * core becomes the primal that a weighted draw picks among those at or above the average total, provided at least
 * {@link #REQUIRED_BLOCKS} blocks had aspects; otherwise it comes back blank. The cluster's own primal starts with 60.
 * Nothing runs after the core is finished (a fresh item entity replaces this one) and the cluster-free blank item entity never ticks this.
 */
public class AuraCoreEntity extends ItemEntity {
    public static final int PRE_GATHER = 50;
    public static final int GATHER = 500;
    private static final int RADIUS = 4;
    private static final int CELLS = 9 * 9 * 9;
    public static final int REQUIRED_BLOCKS = (int) Math.round(Math.pow(9.0, 3.0) * 0.15);
    private static final int CLUSTER_START = 60;

    private final int[] totals = new int[6];
    private int gather;
    private int scanned;
    private int blockCount;
    private BlockPos origin = BlockPos.ZERO;
    private List<Integer> order;

    public AuraCoreEntity(EntityType<? extends ItemEntity> type, Level level) {
        super(type, level);
    }

    /** {@code cluster} is the primal of the crystal cluster that was energised. */
    public AuraCoreEntity(Level level, double x, double y, double z, ItemStack stack, BlockPos origin, ResourceKey<IAspect> cluster) {
        super(TCEntities.AURA_CORE.get(), level);
        setPos(x, y, z);
        setItem(stack);
        setNeverPickUp();
        this.origin = origin;
        int i = TTAspects.PRIMALS.indexOf(cluster);
        if (i >= 0) {
            totals[i] = CLUSTER_START;
        }
    }

    public int gatherTicks() {
        return gather;
    }

    public int blocksFound() {
        return blockCount;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server) || isRemoved()) {
            return;
        }
        gather++;
        setNeverPickUp();
        if (gather > GATHER) {
            finish(server);
            return;
        }
        if (gather > PRE_GATHER) {
            int target = Math.min(CELLS, (int) (CELLS * (gather - PRE_GATHER) / (double) (GATHER - PRE_GATHER)));
            if (scanned < target) {
                if (order == null) {
                    order = shuffledCells(origin);
                }
                while (scanned < target) {
                    scan(server, order.get(scanned++));
                }
            }
        }
        if ((gather & 1) == 0) {
            effects(server);
        }
    }

    /** The 729 offsets of the cube, packed, in the order seeded by the activation position (as the original). */
    public static List<Integer> shuffledCells(BlockPos seed) {
        List<Integer> cells = new ArrayList<>(CELLS);
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int y = -RADIUS; y <= RADIUS; y++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    cells.add(((x + RADIUS) << 8) | ((y + RADIUS) << 4) | (z + RADIUS));
                }
            }
        }
        Collections.shuffle(cells, new Random(seed.hashCode()));
        return cells;
    }

    private void scan(ServerLevel level, int cell) {
        BlockPos pos = BlockPos.containing(getX(), getY(), getZ()).offset(
                ((cell >> 8) & 15) - RADIUS, ((cell >> 4) & 15) - RADIUS, (cell & 15) - RADIUS);
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        ItemStack stack;
        if (state.getBlock() instanceof LiquidBlock) {
            stack = new ItemStack(state.getFluidState().getType().getBucket());
        } else {
            stack = state.getBlock().getCloneItemStack(level, pos, state);
            if (stack.isEmpty()) {
                stack = new ItemStack(state.getBlock().asItem());
            }
        }
        if (stack.isEmpty()) {
            return;
        }
        AspectList list = AspectIndexAccess.of(stack);
        if (list.isEmpty()) {
            return;
        }
        Map<ResourceKey<IAspect>, Integer> primals = WandChargingEvents.reduceToPrimals(list);
        if (primals.isEmpty()) {
            return;
        }
        primals.forEach((key, amount) -> {
            int i = TTAspects.PRIMALS.indexOf(key);
            if (i >= 0) {
                totals[i] += amount;
            }
        });
        blockCount++;
    }

    /** Weighted draw among the primals at or above the average of the aspects found; null when the scan failed. */
    private AuraCore.Type decide() {
        if (blockCount < REQUIRED_BLOCKS) {
            return null;
        }
        int sum = 0;
        int kinds = 0;
        for (int t : totals) {
            sum += t;
            if (t > 0) {
                kinds++;
            }
        }
        if (kinds == 0) {
            return null;
        }
        double avg = (double) sum / kinds;
        int weight = 0;
        for (int t : totals) {
            if (t > 0 && t >= avg) {
                weight += t;
            }
        }
        int roll = random.nextInt(weight);
        for (int i = 0; i < totals.length; i++) {
            if (totals[i] > 0 && totals[i] >= avg) {
                roll -= totals[i];
                if (roll < 0) {
                    return AuraCore.Type.ofPrimalIndex(i);
                }
            }
        }
        return null;
    }

    private void finish(ServerLevel level) {
        AuraCore.Type type = decide();
        ItemStack result = AuraCore.stack(type == null ? AuraCore.Type.BLANK : type);
        double x = getX();
        double y = getY();
        double z = getZ();
        if (type != null) {
            level.sendParticles(ParticleTypes.END_ROD, x, y + 0.2, z, 40, 0.3, 0.3, 0.3, 0.1);
            level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.5F, 1.2F);
        } else {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.2, z, 20, 0.3, 0.3, 0.3, 0.02);
            level.playSound(null, x, y, z, TTSounds.CRAFTFAIL.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        ItemEntity out = new ItemEntity(level, x, y, z, result);
        out.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(out);
        discard();
    }

    private int colorOf(ServerLevel level, int primal) {
        Holder<IAspect> holder = Aspects.resolve(level, TTAspects.PRIMALS.get(primal));
        return holder == null ? 0xFFFFFF : holder.value().color();
    }

    /** A colour drawn by weight from what has been found so far (the original showed six seats by share). */
    private int pickColor(ServerLevel level) {
        int sum = 0;
        for (int t : totals) {
            sum += t;
        }
        if (sum <= 0) {
            return 0xFFFFFF;
        }
        int roll = random.nextInt(sum);
        for (int i = 0; i < totals.length; i++) {
            roll -= totals[i];
            if (roll < 0) {
                return colorOf(level, i);
            }
        }
        return 0xFFFFFF;
    }

    private static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(Vec3.fromRGB24(rgb).toVector3f(), size);
    }

    /** Pre-gather: a stream from the cluster to the core. Gather: an orbit around the core shrinking with the time left. */
    private void effects(ServerLevel level) {
        if (gather <= PRE_GATHER) {
            double p = gather / (double) PRE_GATHER;
            Vec3 from = Vec3.atCenterOf(origin);
            Vec3 at = from.lerp(position().add(0, 0.2, 0), p);
            for (int i = 0; i < 2; i++) {
                level.sendParticles(dust(pickColor(level), 1.0F), at.x, at.y, at.z, 1, 0.1, 0.1, 0.1, 0);
            }
        } else {
            double shrink = 1.0 - (gather - PRE_GATHER) / (double) (GATHER - PRE_GATHER);
            double r = 0.15 + 1.1 * shrink;
            for (int i = 0; i < 3; i++) {
                double a = gather * 0.25 + i * (Math.PI * 2 / 3);
                double tilt = i * 0.9;
                level.sendParticles(dust(pickColor(level), 0.8F), getX() + Math.cos(a) * r,
                        getY() + 0.2 + Math.sin(a) * Math.sin(tilt + 0.5) * r, getZ() + Math.sin(a) * Math.cos(tilt) * r, 1, 0, 0, 0, 0);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("AuraGather", gather);
        tag.putInt("AuraScanned", scanned);
        tag.putInt("AuraBlocks", blockCount);
        tag.putIntArray("AuraTotals", totals);
        tag.putIntArray("AuraOrigin", new int[]{origin.getX(), origin.getY(), origin.getZ()});
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        gather = tag.getInt("AuraGather");
        scanned = tag.getInt("AuraScanned");
        blockCount = tag.getInt("AuraBlocks");
        int[] saved = tag.getIntArray("AuraTotals");
        System.arraycopy(saved, 0, totals, 0, Math.min(saved.length, totals.length));
        int[] o = tag.getIntArray("AuraOrigin");
        origin = o.length == 3 ? new BlockPos(o[0], o[1], o[2]) : blockPosition();
        order = null;
        setNeverPickUp();
    }
}
