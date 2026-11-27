package dev.movingcastle.thaumicconcilium.entity.mob;

import dev.movingcastle.thaumicconcilium.compat.witchery.NetherPair;
import dev.movingcastle.thaumicconcilium.registry.TCSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * The Sloppy Alchemist (original {@code SloppyAlchemist}): spawned with a Chort on his back, whom he serves as a battering
 * ram; once the Chort is gone he can be freed with a right-click, and thanks the player with a reward. Exists only with Witchery.
 */
public class SloppyAlchemist extends Monster {
    public static final EntityDimensions ALONE = EntityDimensions.scalable(0.5F, 1.8F);
    public static final EntityDimensions RIDDEN = EntityDimensions.scalable(1.0F, 0.8F);
    /** Original {@code getMountedYOffset}. */
    public static final double RIDE_HEIGHT = 0.3;
    /** Witchery's ids for the original's infernal blood and warm blood (R8; Blood Arsenal's blood money is not in the pack). */
    public static final ResourceLocation DEMONS_BLOOD = ResourceLocation.fromNamespaceAndPath("witchery", "demons_blood");
    public static final ResourceLocation BOTTLE_OF_BLOOD = ResourceLocation.fromNamespaceAndPath("witchery", "bottle_of_blood");

    /** Set while a Chort rides him; not saved, as in the original. */
    public boolean wasFreed;

    public SloppyAlchemist(EntityType<? extends SloppyAlchemist> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.MAX_HEALTH, 25.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new SloppyAlchemistChargeGoal(this, 0.22, 1.1, 60, 16.0F));
        this.goalSelector.addGoal(4, new RandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new ImpDealTargetGoal(this));
    }

    public boolean ridden() {
        return this.getFirstPassenger() instanceof Chort;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return this.ridden() ? RIDDEN : ALONE;
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        this.refreshDimensions();
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        this.refreshDimensions();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0.0, RIDE_HEIGHT * scale, 0.0);
    }

    /** Original {@code onSpawnWithEgg}: a Chort appears on his back. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        Chort chort = NetherPair.CHORT.get().create(this.level());
        if (chort != null) {
            chort.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
            chort.finalizeSpawn(level, difficulty, MobSpawnType.MOB_SUMMONED, null);
            // Not added here: every spawn path (summon, egg, spawner, natural) adds the vehicle with its passengers, as
            // vanilla's spider jockey; adding it twice made /summon fail on a duplicate UUID.
            chort.startRiding(this, true);
        }
        return result;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.getFirstPassenger() instanceof Chort rider) {
            this.wasFreed = true;
            if (!this.level().isClientSide && rider.getTarget() != null) {
                this.setTarget(rider.getTarget());
            }
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.ridden()) {
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + this.getBbHeight() / 2.0, this.getZ(), 20,
                    this.getBbWidth() / 2.0, this.getBbHeight() / 2.0, this.getBbWidth() / 2.0, 0.02);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), TCSounds.BREATH.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
            if (!this.wasFreed) {
                player.sendSystemMessage(Component.translatable("thaumicconcilium.sloppy_alchemist.confused").withStyle(ChatFormatting.DARK_RED));
                this.discard();
                return InteractionResult.SUCCESS;
            }
            player.sendSystemMessage(Component.translatable("thaumicconcilium.sloppy_alchemist.thanks" + (1 + this.random.nextInt(3)))
                    .withStyle(ChatFormatting.DARK_RED));
            ItemStack reward = reward(this.random.nextInt(3), this.random.nextInt(3) + 1);
            if (!reward.isEmpty() && !player.getInventory().add(reward)) {
                ItemEntity drop = new ItemEntity(this.level(), this.getX(), this.getY() + 0.5, this.getZ(), reward);
                drop.setPickUpDelay(40);
                drop.setDeltaMovement(drop.getDeltaMovement().add(0.0, 0.3, 0.0));
                this.level().addFreshEntity(drop);
            }
            this.discard();
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    /** Original: 0 infernal blood, 1 warm blood, 2 blood money (Blood Arsenal; without it infernal blood again). */
    public static ItemStack reward(int roll, int count) {
        Item item = BuiltInRegistries.ITEM.get(roll == 1 ? BOTTLE_OF_BLOOD : DEMONS_BLOOD);
        return new ItemStack(item, count);
    }
}
