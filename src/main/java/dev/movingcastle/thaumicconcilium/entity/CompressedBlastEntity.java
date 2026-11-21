package dev.movingcastle.thaumicconcilium.entity;

import com.leclowndu93150.thaumaturge.api.spell.Spells;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellContinuation;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import dev.movingcastle.thaumicconcilium.registry.TCSounds;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The "compressed blast": a straight-flying clot of energy that detonates in mid-air when its fuse runs out,
 * shared by the Impulse and Signal foci in the original. On Thaumaturge it carries the rest of the spell and
 * hands it the detonation point, so any effect can ride it.
 * <p>
 * Ported from {@code CompressedBlastEntity}: no gravity, no drag, fuse of {@code 5 + 10 * fuse + rand(20)} ticks,
 * a firework-spark trail, a "thump" every 5 ticks, and the optional vacuum that drags non-player mobs within 4 blocks.
 */
public class CompressedBlastEntity extends Entity {
    private int age;
    private int lifetime;
    private boolean vacuum;
    private Optional<SpellContinuation> payload = Optional.empty();

    public CompressedBlastEntity(EntityType<? extends CompressedBlastEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public static void launch(ServerLevel level, LivingEntity caster, SpellContinuation payload, SpellTarget origin,
                              float speed, int lifetime, boolean vacuum) {
        CompressedBlastEntity blast = new CompressedBlastEntity(TCEntities.COMPRESSED_BLAST.get(), level);
        Vec3 at = origin.position();
        blast.setPos(at.x, at.y, at.z);
        blast.setDeltaMovement(origin.direction().scale(speed));
        blast.lifetime = lifetime;
        blast.vacuum = vacuum;
        blast.payload = Optional.of(payload);
        level.addFreshEntity(blast);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = this.getDeltaMovement();
        this.move(MoverType.SELF, motion);
        double horizontal = motion.horizontalDistance();
        this.setYRot((float) (Math.atan2(motion.x, motion.z) * (180.0 / Math.PI)));
        this.setXRot((float) (Math.atan2(motion.y, horizontal) * (180.0 / Math.PI)));
        this.age++;

        Level level = this.level();
        if (level.isClientSide) {
            level.addParticle(ParticleTypes.FIREWORK, this.getX(), this.getY(), this.getZ(),
                    this.random.nextGaussian() * 0.05, -motion.y * 0.5, this.random.nextGaussian() * 0.05);
            if (this.age % 5 == 0) {
                level.addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
            return;
        }

        if (this.vacuum) {
            Vec3 centre = this.position();
            for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(4.0))) {
                if (!(near instanceof Player)) {
                    Motion.pullTowards(near, centre, 2.0F);
                }
            }
        }

        if (this.age % 5 == 0) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), TCSounds.THUMP.get(), SoundSource.PLAYERS,
                    0.9F, 2.6F + (this.random.nextFloat() - this.random.nextFloat()) * 0.8F);
        }

        if (this.age > this.lifetime) {
            this.detonate((ServerLevel) level);
        }
    }

    private void detonate(ServerLevel level) {
        Vec3 at = this.position();
        Vec3 direction = this.getDeltaMovement().lengthSqr() > 1.0E-6 ? this.getDeltaMovement().normalize() : new Vec3(0, 1, 0);
        this.payload.ifPresent(continuation -> {
            BlockHitResult point = BlockHitResult.miss(at, Direction.getNearest(direction.x, direction.y, direction.z), BlockPos.containing(at));
            Spells.resume(level, continuation, List.of(new SpellTarget(point, at, direction)));
        });
        this.discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getInt("Life");
        this.lifetime = tag.getInt("LifeTime");
        this.vacuum = tag.getBoolean("Vacuum");
        this.payload = tag.contains("Payload")
                ? SpellContinuation.CODEC.parse(NbtOps.INSTANCE, tag.get("Payload")).result()
                : Optional.empty();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Life", this.age);
        tag.putInt("LifeTime", this.lifetime);
        tag.putBoolean("Vacuum", this.vacuum);
        this.payload.flatMap(p -> SpellContinuation.CODEC.encodeStart(NbtOps.INSTANCE, p).result())
                .ifPresent(nbt -> tag.put("Payload", nbt));
    }
}
