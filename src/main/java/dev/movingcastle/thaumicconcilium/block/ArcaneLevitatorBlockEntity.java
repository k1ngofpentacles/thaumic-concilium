package dev.movingcastle.thaumicconcilium.block;

import dev.movingcastle.thaumicconcilium.registry.TCBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The Arcane Levitator's state (Thaumcraft 4's lifter, mechanics only). The column height is computed on the server only when something
 * next to the block changes (placement, a neighbour update, a redstone change, the stack below changing) and synced in the update tag;
 * the ticker does nothing when the lifter is off or its column is empty, and otherwise one AABB query over the cached column, never a scan
 * of the blocks. Players move client side, so the client lifts only the local player and the server everything else.
 * <p>
 * Column: 10 blocks per lifter in the unpowered stack directly below the top one (itself included), stopping at the first full block.
 * Off while the lifter or the block above it is powered. Lift: vertical speed +0.1 a tick up to 0.35; a sneaking rider is not lifted
 * and falls at 0.9 of its speed instead; fall distance is reset.
 */
public class ArcaneLevitatorBlockEntity extends BlockEntity {
    public static final int PER_LIFTER = 10;
    public static final int MAX_STACK = 8;
    public static final double LIFT_STEP = 0.1;
    public static final double LIFT_MAX = 0.35;
    public static final double SNEAK_DAMP = 0.9;
    private static final DustParticleOptions AIR = new DustParticleOptions(new Vector3f(0.85F, 0.95F, 1.0F), 0.8F);

    private int height;
    private boolean off;
    /** The column ends at a full block (riders hover under it instead of being pushed into it). */
    private boolean capped;
    private boolean dirty = true;
    private AABB column;

    public ArcaneLevitatorBlockEntity(BlockPos pos, BlockState state) {
        super(TCBlocks.ARCANE_LEVITATOR_BLOCK_ENTITY.get(), pos, state);
    }

    public int height() {
        return height;
    }

    public boolean isOff() {
        return off;
    }

    /** Called when a neighbour changed or the block was placed. Server side. */
    public void refresh() {
        if (level == null || level.isClientSide()) {
            return;
        }
        dirty = false;
        boolean nowOff = level.hasNeighborSignal(worldPosition) || level.hasNeighborSignal(worldPosition.above());
        int stack = 1;
        BlockPos below = worldPosition.below();
        while (stack < MAX_STACK && level.getBlockState(below).is(TCBlocks.ARCANE_LEVITATOR.get()) && !level.hasNeighborSignal(below)) {
            stack++;
            below = below.below();
        }
        int h = 0;
        boolean stopped = false;
        for (int i = 1; i <= PER_LIFTER * stack; i++) {
            BlockPos p = worldPosition.above(i);
            if (level.getBlockState(p).isCollisionShapeFullBlock(level, p)) {
                stopped = true;
                break;
            }
            h = i;
        }
        boolean changed = h != height || nowOff != off || stopped != capped;
        height = h;
        capped = stopped;
        off = nowOff;
        column = null;
        if (changed) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            // the lifter above counts this one in its stack
            if (level.getBlockEntity(worldPosition.above()) instanceof ArcaneLevitatorBlockEntity above) {
                above.refresh();
            }
        }
    }

    private AABB column() {
        if (column == null) {
            column = new AABB(worldPosition.getX(), worldPosition.getY() + 1.0, worldPosition.getZ(),
                    worldPosition.getX() + 1.0, worldPosition.getY() + 1.0 + height, worldPosition.getZ() + 1.0);
        }
        return column;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ArcaneLevitatorBlockEntity be) {
        boolean client = level.isClientSide();
        if (be.dirty && !client) {
            be.refresh();
        }
        if (be.off || be.height <= 0) {
            return;
        }
        List<Entity> riders = level.getEntitiesOfClass(Entity.class, be.column(), e -> liftable(e, client));
        if (client && (level.getGameTime() + pos.asLong()) % 4 == 0 && level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 16.0, false) != null) {
            level.addParticle(AIR, pos.getX() + 0.2 + level.random.nextDouble() * 0.6, pos.getY() + 1.05,
                    pos.getZ() + 0.2 + level.random.nextDouble() * 0.6, 0.0, 0.12, 0.0);
        }
        if (riders.isEmpty()) {
            return;
        }
        double ceiling = be.capped ? pos.getY() + 1.0 + be.height : Double.POSITIVE_INFINITY;
        for (Entity e : riders) {
            lift(e, ceiling);
            // reached the top of a column that may have grown since: look again (event-driven, not periodic)
            if (!client && !be.capped && e.getY() >= pos.getY() + 1.0 + be.height) {
                be.refresh();
            }
        }
    }

    /** What the lifter moves: items and creatures; players only on their own client, everything else on the server. */
    private static boolean liftable(Entity e, boolean client) {
        if (e.isSpectator() || e.isPassenger()) {
            return false;
        }
        if (e instanceof Player player) {
            return client ? player.isLocalPlayer() && !player.getAbilities().flying : false;
        }
        return !client && (e instanceof ItemEntity || e instanceof LivingEntity);
    }

    /** One tick of lift on one entity (shared with the tests). */
    public static void lift(Entity e) {
        lift(e, Double.POSITIVE_INFINITY);
    }

    /** As {@link #lift(Entity)}, but never carrying the entity's top above {@code ceiling} (the underside of the full block that ends the column). */
    public static void lift(Entity e, double ceiling) {
        Vec3 m = e.getDeltaMovement();
        double y = m.y;
        if (e.isShiftKeyDown()) {
            if (y < 0.0) {
                y *= SNEAK_DAMP;
            }
        } else if (y < LIFT_MAX) {
            y = Math.min(LIFT_MAX, y + LIFT_STEP);
        }
        double room = ceiling - e.getBoundingBox().maxY;
        if (y > room) {
            y = Math.max(0.0, room);
        }
        e.setDeltaMovement(m.x, y, m.z);
        e.fallDistance = 0.0F;
        if (!e.level().isClientSide()) {
            e.hasImpulse = true;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("height", height);
        tag.putBoolean("off", off);
        tag.putBoolean("capped", capped);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        height = tag.getInt("height");
        off = tag.getBoolean("off");
        capped = tag.getBoolean("capped");
        column = null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
