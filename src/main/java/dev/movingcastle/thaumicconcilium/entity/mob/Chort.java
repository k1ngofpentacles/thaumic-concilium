package dev.movingcastle.thaumicconcilium.entity.mob;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The Chort (original {@code Chort}): a pig-voiced imp that spits three globs at a time and rides a Sloppy Alchemist it has
 * enslaved. Exists only with Witchery. Drops come from {@code loot_table/entities/chort.json} (generator, R8).
 */
public class Chort extends Monster {
    /** Ticks the pact line stays quiet after it is said (original: 1200). */
    public static final int PACT_LINE_COOLDOWN = 1200;
    /** Ticks until the next pact line may be said; counted down every tick. */
    public int chatCooldown;

    public Chort(EntityType<? extends Chort> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.MAX_HEALTH, 25.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new ChortAttackGoal(this));
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new ImpDealTargetGoal(this));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PIG_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PIG_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PIG_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.5F;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.chatCooldown > 0) {
            this.chatCooldown--;
        }
        // Original: riding a Sloppy Alchemist, it takes the mount's target (the mount takes the rider's in turn).
        if (!this.level().isClientSide && this.getVehicle() instanceof SloppyAlchemist mount) {
            LivingEntity mountTarget = mount.getTarget();
            if (mountTarget != null) {
                this.setTarget(mountTarget);
            }
        }
    }
}
