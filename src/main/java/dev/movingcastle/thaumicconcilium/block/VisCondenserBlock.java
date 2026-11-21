package dev.movingcastle.thaumicconcilium.block;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.content.aspect.EntityAspects;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/**
 * Vis Condenser (original: {@code VisCondenserBlock} / {@code VisCondenserTile}): a pedestal for a destabilized
 * crystal. A redstone signal from a side discharges up to 16 of the crystal's charge as a bolt the other way, reaching
 * as many blocks as the signal is strong and hitting harder the shorter it is; a creature it kills leaves an essentia
 * crystal of one of its aspects. A signal from below instead sends the whole charge into a Hex of Predictability
 * holding a rift within 8 blocks.
 */
public class VisCondenserBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public VisCondenserBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbour, BlockPos from, boolean moving) {
        super.neighborChanged(state, level, pos, neighbour, from, moving);
        if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos.above()) instanceof DestabilizedCrystalBlockEntity crystal)) {
            return;
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            int power = level.getSignal(pos.relative(side), side);
            if (power != 0) {
                shoot(server, pos, crystal, side.getOpposite(), power);
            }
        }
        if (level.getSignal(pos.below(), Direction.DOWN) != 0 && crystal.capacity() != 0) {
            send(server, pos, crystal);
        }
    }

    private static void shoot(ServerLevel level, BlockPos pos, DestabilizedCrystalBlockEntity crystal, Direction towards, int power) {
        if (crystal.capacity() == 0) {
            return;
        }
        int blast = Math.min(crystal.capacity(), 16);
        BlockPos top = pos.above();
        Vec3 start = Vec3.atCenterOf(top);
        Vec3 end = start.add(towards.getStepX() * power, 0.0, towards.getStepZ() * power);
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        if (hit.getType() == HitResult.Type.BLOCK && !hit.getBlockPos().equals(top)) {
            end = Vec3.atCenterOf(hit.getBlockPos());
        }
        crystal.setCapacity(crystal.capacity() - blast);
        AABB box = new AABB(start, end).inflate(0.6, 0.3, 0.6);
        float damage = Mth.clamp(blast + 1 - power, 0, 16);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
            e.hurt(level.damageSources().magic(), damage);
            if (e.isDeadOrDying()) {
                // Original: a wispy essence of one of the creature's aspects (here an essentia crystal).
                AspectList aspects = EntityAspects.of(e);
                List<AspectInstance> list = aspects.entries();
                if (!list.isEmpty()) {
                    ItemStack shard = new ItemStack(TTItems.ESSENTIA_CRYSTAL.get());
                    shard.set(TTDataComponents.CRYSTAL_ASPECT.get(), new AspectInstance(list.get(level.random.nextInt(list.size())).aspect(), 1));
                    e.spawnAtLocation(shard, 0.2F);
                }
            }
        }
        level.playSound(null, pos, TTSounds.SHOCK.get(), SoundSource.BLOCKS, 0.8F, level.random.nextFloat() * 0.1F + 0.9F);
        arc(level, start, end, crystal.color());
    }

    private static void send(ServerLevel level, BlockPos pos, DestabilizedCrystalBlockEntity crystal) {
        BlockPos c = pos.above();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-8, -8, -8), c.offset(8, 8, 8))) {
            if (level.getBlockEntity(p) instanceof HexBlockEntity hex && hex.isMaster() && hex.hasRift() && crystal.aspect() != null) {
                hex.addEssentia(crystal.aspect(), crystal.capacity());
                hex.setHeat(1200);
                crystal.setCapacity(0);
                level.playSound(null, pos, TTSounds.SHOCK.get(), SoundSource.BLOCKS, 0.8F, level.random.nextFloat() * 0.1F + 0.9F);
                arc(level, Vec3.atCenterOf(c), Vec3.atCenterOf(p).add(0.0, 1.0, 0.0), crystal.color());
                return;
            }
        }
    }

    static void arc(ServerLevel level, Vec3 from, Vec3 to, int rgb) {
        DustParticleOptions dust = new DustParticleOptions(new Vector3f((rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F), 1.0F);
        int steps = Math.max(4, (int) (from.distanceTo(to) * 4));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.lerp(to, i / (double) steps);
            level.sendParticles(dust, p.x, p.y, p.z, 1, 0.04, 0.04, 0.04, 0.0);
        }
    }
}
