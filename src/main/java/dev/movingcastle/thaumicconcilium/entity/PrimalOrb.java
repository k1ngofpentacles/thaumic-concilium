package dev.movingcastle.thaumicconcilium.entity;

import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import dev.movingcastle.thaumicconcilium.spell.Taint;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A primal orb thrown by a spatial rift (the original rift spawned Thaumcraft 4's primal orb, which Thaumaturge doesn't
 * have; this is the port's own version of it). It drifts after its first second and bursts on whatever it touches:
 * an explosion of strength 2 (breaking blocks only under mob griefing), and one time in a hundred a taint burst.
 */
public class PrimalOrb extends Entity {
    private int age;

    public PrimalOrb(EntityType<? extends PrimalOrb> type, Level level) {
        super(type, level);
    }

    public static PrimalOrb aimed(Level level, Vec3 at, Vec3 target) {
        PrimalOrb orb = new PrimalOrb(TCEntities.PRIMAL_ORB.get(), level);
        orb.setPos(at);
        // Original: throwable heading 0.66 with spread 0.1.
        Vec3 dir = target.subtract(at).normalize();
        orb.setDeltaMovement(dir.scale(0.66).add(orb.random.nextGaussian() * 0.0075 * 0.1, orb.random.nextGaussian() * 0.0075 * 0.1,
                orb.random.nextGaussian() * 0.0075 * 0.1));
        return orb;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        this.age++;
        Vec3 motion = this.getDeltaMovement();
        if (this.age > 20) {
            motion = motion.add((this.random.nextFloat() - this.random.nextFloat()) * 0.01, (this.random.nextFloat() - this.random.nextFloat()) * 0.01,
                    (this.random.nextFloat() - this.random.nextFloat()) * 0.01);
            this.setDeltaMovement(motion);
        }
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, e -> e.isAlive() && e.isPickable() && !(e instanceof PrimalOrb));
        if (hit.getType() != HitResult.Type.MISS && this.level() instanceof ServerLevel level) {
            this.burst(level, hit.getLocation());
            return;
        }
        this.setPos(this.position().add(motion));
        if (this.age > 200) {
            this.discard();
        }
    }

    private void burst(ServerLevel level, Vec3 at) {
        boolean griefing = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        level.explode(this, at.x, at.y, at.z, 2.0F, griefing ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE);
        if (this.random.nextInt(100) <= 1) {
            Taint.taintsplosion(level, at);
        }
        this.discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getInt("age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("age", this.age);
    }
}
