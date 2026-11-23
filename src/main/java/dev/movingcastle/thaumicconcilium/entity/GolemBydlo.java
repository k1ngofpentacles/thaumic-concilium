package dev.movingcastle.thaumicconcilium.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The Golem Bydlo (original: {@code entities.other.GolemBydlo}, WTFPL), the enlarged thaumium golem a Mad Thaumaturge becomes.
 * <p>
 * It has no goals and no targets: it stands where it is until a player rides it. Any player can (no owner, as the original);
 * the controlling rider steers (yaw from the rider, pitch halved, strafe halved, no jump). Its arms are the rider's wand
 * commands, handled by {@link dev.movingcastle.thaumicconcilium.golem.BydloCommands} on events, so an idle or ridden Bydlo costs
 * no more than any mob. The original's home-scan "leash" (a per-tick search around the world origin, a bug) is dropped.
 * <p>
 * An {@link AbstractGolem}, so the Horizons vat keeps refusing to infuse it.
 */
public class GolemBydlo extends AbstractGolem {
    public static final double MAX_HEALTH = 90.0;
    public static final double SPEED = 0.2;
    public static final float WIDTH = 2.0F;
    public static final float HEIGHT = 3.5F;
    public static final double STEP_HEIGHT = 0.5;
    public static final double STEP_HEIGHT_RIDDEN = 1.0;
    /** Original {@code attackEntityAsMob}: {@code 10 + rand(15)}. */
    public static final int PUNCH_MIN = 10;
    public static final int PUNCH_SPREAD = 15;
    /** Ticks the arm-raise animation lasts (the original's {@code action = 6}). */
    public static final int ACTION_TICKS = 6;
    public static final byte EVENT_ACTION = 4;

    /** Client-side countdown of the raised-arms animation. */
    public int actionAnim;
    /** Game time of the last wand command (server): the 6-tick cooldown is a stamp, nothing ticks. */
    public long lastCommand = -1000L;

    public GolemBydlo(EntityType<? extends GolemBydlo> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractGolem.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.STEP_HEIGHT, STEP_HEIGHT);
    }

    /** The punch: 10 to 24 damage. */
    public static float punchDamage(RandomSource random) {
        return PUNCH_MIN + random.nextInt(PUNCH_SPREAD);
    }

    @Override
    protected void registerGoals() {
        // none: it stands still unless ridden or commanded (original: the AI task list is cleared)
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // ---------------------------------------------------------------- riding
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!player.isSecondaryUseActive() && !this.isVehicle() && !player.getItemInHand(hand).is(Items.NAME_TAG)) {
            if (!this.level().isClientSide) {
                player.startRiding(this);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return !this.isVehicle();
    }

    /** The seat: behind the shoulders, 0.75 of the height up and a little more (original {@code updateRiderPosition}). */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0.0, dimensions.height() * 0.75 + 0.15, -0.5);
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        this.setStepHeight(STEP_HEIGHT_RIDDEN);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!this.isVehicle()) {
            this.setStepHeight(STEP_HEIGHT);
        }
    }

    private void setStepHeight(double value) {
        AttributeInstance step = this.getAttribute(Attributes.STEP_HEIGHT);
        if (step != null && step.getBaseValue() != value) {
            step.setBaseValue(value);
        }
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        this.setRot(player.getYRot(), player.getXRot() * 0.5F);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        return new Vec3(player.xxa * 0.5F, 0.0, player.zza);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    // ---------------------------------------------------------------- arms
    /** Raises the arms for {@link #ACTION_TICKS} ticks (the original's wand-command animation); server side, tells the clients. */
    public void raiseArms() {
        this.actionAnim = ACTION_TICKS;
        if (!this.level().isClientSide) {
            this.level().broadcastEntityEvent(this, EVENT_ACTION);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_ACTION) {
            this.actionAnim = ACTION_TICKS;
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.actionAnim > 0) {
            this.actionAnim--;
        }
    }

    // ---------------------------------------------------------------- sounds (the iron golem's)
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.IRON_GOLEM_STEP, 1.0F, 1.0F);
    }
}
