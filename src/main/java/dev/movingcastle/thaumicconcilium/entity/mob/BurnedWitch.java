package dev.movingcastle.thaumicconcilium.entity.mob;

import dev.movingcastle.thaumicconcilium.compat.witchery.WitchBrews;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Burned Witch (original {@code BurnedWitch}, an {@code EntityFlying} that is an {@code IMob}): rides a broom in circles 4 to 8
 * blocks from the nearest player, 8 blocks above them, and throws a brew every 1.5 seconds while she can see them. Exists only with
 * Witchery. The AI is the original's, step for step (its comments are the original's line numbers in
 * docs/design/phase10-original-facts.md 4.3). Drops come from {@code loot_table/entities/burned_witch.json} (generator, R8); brews
 * and brew immunity are {@link WitchBrews}.
 */
public class BurnedWitch extends FlyingMob implements Enemy {
    /** Original: orbit height 8, speed 0.06 rad/tick, radius 4 to 8, bob sin(2 * angle) * 0.6. */
    private static final double ORBIT_HEIGHT = 8.0;
    private static final double ORBIT_SPEED = 0.06;
    private static final double ORBIT_RADIUS_MIN = 4.0;
    private static final double ORBIT_RADIUS_MAX = 8.0;
    private static final double ORBIT_BOB_AMPLITUDE = 0.6;
    private static final double ORBIT_BOB_SPEED = 2.0;
    /** Original: nearest player within 100 blocks, looked for again every 20 ticks. */
    private static final double TARGET_RANGE = 100.0;
    private static final int RETARGET_TICKS = 20;
    /** Ticks in sight before the brew leaves her hand, and the cool-down (negative counter) after it. */
    private static final int THROW_AT = 10;
    private static final int COOLDOWN = -20;
    /** She turns to face a target nearer than this (original: 64 squared). */
    private static final double SEE_RANGE_SQ = 64.0 * 64.0;

    public double orbitAngle;
    public double orbitRadius;
    private int courseChangeCooldown;
    private double waypointX;
    private double waypointY;
    private double waypointZ;
    private Player targeted;
    private int aggroCooldown;
    public int prevAttackCounter;
    public int attackCounter;
    /** Game time of the last brew splash that reached her, see {@link WitchBrews}; her stand-in for the original's gas mask. */
    public long brewShieldTime = Long.MIN_VALUE;

    public BurnedWitch(EntityType<? extends BurnedWitch> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0);
    }

    /** The target the orbit follows (null while she wanders); read by the tests. */
    public Player orbitTarget() {
        return this.targeted;
    }

    @Override
    protected void customServerAiStep() {
        if (this.level().getDifficulty() == Difficulty.PEACEFUL) {
            this.discard();
            return;
        }
        this.prevAttackCounter = this.attackCounter;
        if (this.targeted != null && !this.targeted.isAlive()) {
            this.targeted = null;
        }
        if (this.targeted == null || this.aggroCooldown-- <= 0) {
            this.targeted = this.level().getNearestPlayer(this, TARGET_RANGE);
            if (this.targeted != null) {
                this.aggroCooldown = RETARGET_TICKS;
                this.orbitRadius = ORBIT_RADIUS_MIN + this.random.nextDouble() * (ORBIT_RADIUS_MAX - ORBIT_RADIUS_MIN);
                this.orbitAngle = this.random.nextDouble() * Math.PI * 2.0;
            }
        }
        if (this.targeted != null) {
            this.orbitAngle += ORBIT_SPEED;
            this.waypointX = this.targeted.getX() + Math.cos(this.orbitAngle) * this.orbitRadius;
            this.waypointZ = this.targeted.getZ() + Math.sin(this.orbitAngle) * this.orbitRadius;
            this.waypointY = this.targeted.getY() + ORBIT_HEIGHT + Math.sin(this.orbitAngle * ORBIT_BOB_SPEED) * ORBIT_BOB_AMPLITUDE;
            if (this.courseChangeCooldown > 3) {
                this.courseChangeCooldown = 0;
            }
        } else {
            double dx = this.waypointX - this.getX();
            double dy = this.waypointY - this.getY();
            double dz = this.waypointZ - this.getZ();
            double d = dx * dx + dy * dy + dz * dz;
            if (d < 1.0 || d > 3600.0) {
                this.waypointX = this.getX() + (this.random.nextFloat() * 2.0F - 1.0F) * 16.0F;
                this.waypointY = this.getY() + (this.random.nextFloat() * 2.0F - 1.0F) * 16.0F;
                this.waypointZ = this.getZ() + (this.random.nextFloat() * 2.0F - 1.0F) * 16.0F;
            }
        }
        double dx = this.waypointX - this.getX();
        double dy = this.waypointY - this.getY();
        double dz = this.waypointZ - this.getZ();
        double distSq = dx * dx + dy * dy + dz * dz;
        if (this.courseChangeCooldown-- <= 0) {
            this.courseChangeCooldown += this.random.nextInt(5) + 2;
            double dist = Math.sqrt(distSq);
            if (dist > 0.0 && this.courseTraversable(dist)) {
                this.setDeltaMovement(this.getDeltaMovement().add(dx / dist * 0.1, dy / dist * 0.1, dz / dist * 0.1));
            } else {
                this.waypointX = this.getX();
                this.waypointY = this.getY();
                this.waypointZ = this.getZ();
            }
        }
        if (this.targeted != null && this.targeted.distanceToSqr(this) < SEE_RANGE_SQ) {
            this.faceYaw(-(float) Math.atan2(this.targeted.getX() - this.getX(), this.targeted.getZ() - this.getZ()) * 180.0F / (float) Math.PI);
            if (this.hasLineOfSight(this.targeted)) {
                // Original: the 1007 (ghast warn) level event was played here when the counter was 10 before it counted up. The
                // counter is reset to -20 the tick it reaches 10, so it never starts a tick at 10: unreachable, kept as written.
                this.attackCounter++;
                if (this.attackCounter == THROW_AT) {
                    this.throwBrew(this.targeted);
                    this.attackCounter = COOLDOWN;
                }
            } else if (this.attackCounter > 0) {
                this.attackCounter--;
            }
        } else {
            Vec3 v = this.getDeltaMovement();
            this.faceYaw(-(float) Math.atan2(v.x, v.z) * 180.0F / (float) Math.PI);
            if (this.attackCounter > 0) {
                this.attackCounter--;
            }
        }
    }

    private void faceYaw(float yaw) {
        this.setYRot(yaw);
        this.yBodyRot = yaw;
    }

    /** Original: only a player target gets a brew; the choice of brew is the original's roll (see {@link WitchBrews}). */
    private void throwBrew(LivingEntity target) {
        if (target instanceof Player player && !this.level().isClientSide) {
            WitchBrews.throwAt(this, player, this.random.nextInt(3));
        }
    }

    /** Original {@code isCourseTraversable}: the box swept one block at a time along the way to the waypoint. */
    private boolean courseTraversable(double dist) {
        double dx = (this.waypointX - this.getX()) / dist;
        double dy = (this.waypointY - this.getY()) / dist;
        double dz = (this.waypointZ - this.getZ()) / dist;
        AABB box = this.getBoundingBox();
        for (int i = 1; i < dist; i++) {
            box = box.move(dx, dy, dz);
            if (!this.level().noCollision(this, box)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.GHAST_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GHAST_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GHAST_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.5F;
    }

    /** Original: at most one per spawn group chunk ({@code getMaxSpawnedInChunk} 1). */
    @Override
    public int getMaxSpawnClusterSize() {
        return 1;
    }

    /** Original {@code doesEntityNotTriggerPressurePlate}: true. */
    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    /** True while a brew splash that reached her this tick is being applied (see {@link WitchBrews}). */
    public boolean brewShielded() {
        return this.level() instanceof ServerLevel level && this.brewShieldTime == level.getGameTime();
    }
}
