package dev.movingcastle.thaumicconcilium.entity.mob;

import com.leclowndu93150.thaumaturge.api.capability.PlayerResearchAccess;
import com.leclowndu93150.thaumaturge.api.spell.CastStyle;
import com.leclowndu93150.thaumaturge.api.spell.Spell;
import com.leclowndu93150.thaumaturge.api.spell.SpellNode;
import com.leclowndu93150.thaumaturge.api.spell.Spells;
import com.leclowndu93150.thaumaturge.api.spell.TTSpellParts;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.leclowndu93150.thaumaturge.content.entity.EntityCultist;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.menu.ThaumaturgeMenu;
import dev.movingcastle.thaumicconcilium.registry.TCSounds;
import dev.movingcastle.thaumicconcilium.spell.Taint;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.phys.Vec3;

/**
 * The friendly Thaumaturge (original: {@code mobs.thaumaturge.Thaumaturge}). Trades through {@link ThaumaturgeMenu}
 * once the player has the Thaumaturges research, taunts them until then. Hitting one angers every Thaumaturge within
 * 32 blocks for 20-40 seconds. Hunts cultists, zombies, skeletons, eldritch and tainted creatures with a fire wand.
 * <p>
 * Wanderers (spawned by {@link dev.movingcastle.thaumicconcilium.world.ThaumaturgeWanderers}) carry a despawn timer, like a
 * wandering trader; naturally spawned ones despawn like any monster, as in the original.
 */
public class ThaumaturgeEntity extends Monster implements RangedAttackMob {
    public static final ResourceLocation RESEARCH = ThaumicConcilium.id("thaumaturges");
    private static final EntityDataAccessor<Integer> ANGER = SynchedEntityData.defineId(ThaumaturgeEntity.class, EntityDataSerializers.INT);

    /** Ticks left before a wanderer leaves; 0 for a naturally spawned Thaumaturge. */
    private int wanderTicks;

    public ThaumaturgeEntity(EntityType<? extends ThaumaturgeEntity> type, Level level) {
        super(type, level);
        if (this.getNavigation() instanceof GroundPathNavigation ground) {
            ground.setCanOpenDoors(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.4)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Original: AILongRangeAttack (min. 3 blocks) and the arrow-style blast attack when holding the wand,
        // AIAttackOnCollide otherwise and up close.
        this.goalSelector.addGoal(1, new BlastGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, false));
        this.goalSelector.addGoal(5, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.5));
        this.goalSelector.addGoal(6, new MoveThroughVillageGoal(this, 1.0, false, 4, () -> false));
        this.goalSelector.addGoal(9, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
        this.goalSelector.addGoal(11, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, EntityCultist.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Zombie.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, AbstractSkeleton.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
                e -> !(e instanceof ThaumaturgeEntity) && Taint.isEldritch(e)));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
                e -> !(e instanceof ThaumaturgeEntity) && Taint.isTainted(e)));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANGER, 0);
    }

    public int anger() {
        return this.entityData.get(ANGER);
    }

    private void setAnger(int anger) {
        this.entityData.set(ANGER, anger);
    }

    /** Makes this one a wanderer that leaves after {@code ticks}. */
    public void setWanderer(int ticks) {
        this.wanderTicks = ticks;
        this.setPersistenceRequired();
    }

    @Override
    public void tick() {
        super.tick();
        Level level = this.level();
        int anger = this.anger();
        if (level.isClientSide()) {
            if (anger > 0 && this.random.nextInt(15) == 0) {
                double w = this.getBbWidth();
                level.addParticle(ParticleTypes.ANGRY_VILLAGER,
                        this.getX() + this.random.nextFloat() * w * 2.0F - w,
                        this.getY() + 0.5 + this.random.nextFloat() * this.getBbHeight(),
                        this.getZ() + this.random.nextFloat() * w * 2.0F - w,
                        this.random.nextGaussian() * 0.02, this.random.nextGaussian() * 0.02, this.random.nextGaussian() * 0.02);
            }
            return;
        }
        if (anger > 0) {
            this.setAnger(anger - 1);
            if (anger == 1 && this.getTarget() instanceof Player) {
                this.setTarget(null);
            }
        }
        if (this.wanderTicks > 0 && --this.wanderTicks == 0) {
            this.discard();
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        // Original: findPlayerToAttack only while angry, re-acquiring the nearest player.
        if (this.anger() > 0 && this.getTarget() == null) {
            Player nearest = this.level().getNearestPlayer(this, 16.0);
            if (nearest != null && !nearest.isCreative() && !nearest.isSpectator()) {
                this.setTarget(nearest);
            }
        }
        if (this.tickCount % 40 == 0) {
            this.heal(1.0F);
        }
    }

    private void becomeAngryAt(Player player) {
        this.setTarget(player);
        this.setAnger(400 + this.random.nextInt(400));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        if (!this.level().isClientSide() && source.getEntity() instanceof Player player && !player.isCreative()) {
            for (ThaumaturgeEntity other : this.level().getEntitiesOfClass(ThaumaturgeEntity.class,
                    this.getBoundingBox().inflate(32.0, 16.0, 32.0))) {
                other.becomeAngryAt(player);
            }
            this.becomeAngryAt(player);
        }
        return super.hurt(source, amount);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive() || player.getItemInHand(hand).is(Items.NAME_TAG)) {
            return InteractionResult.PASS;
        }
        if (this.anger() != 0) {
            return super.mobInteract(player, hand);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (PlayerResearchAccess.of(serverPlayer).hasCompleted(RESEARCH)) {
                if (dev.movingcastle.thaumicconcilium.item.ResearchHint.allKnown(serverPlayer, serverPlayer.registryAccess(),
                        dev.movingcastle.thaumicconcilium.item.ResearchHint.TRADED)) {
                    serverPlayer.sendSystemMessage(Component.translatable("thaumicconcilium.thaumaturge.no_more")
                            .withStyle(ChatFormatting.DARK_PURPLE));
                }
                serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> new ThaumaturgeMenu(id, inventory, this),
                        this.getDisplayName()), buf -> buf.writeVarInt(this.getId()));
            } else {
                serverPlayer.sendSystemMessage(Component.translatable("thaumicconcilium.thaumaturge.taunt." + this.random.nextInt(4))
                        .withStyle(ChatFormatting.DARK_PURPLE));
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    /** Whether the trade table values {@code stack} (original: {@code isValued}). */
    public static boolean isValued(ItemStack stack) {
        return ThaumaturgeTrades.value(stack) != null;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        // Original: a wand with a fire focus and a little vis; 10% drop chance.
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(TTItems.WAND.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.1F);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data) {
        data = super.finalizeSpawn(level, difficulty, reason, data);
        this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        // Found at a structure as its chunk generates: they live there (otherwise they would despawn at once, the
        // player being far away when chunks generate).
        if (reason == MobSpawnType.CHUNK_GENERATION) {
            this.setPersistenceRequired();
        }
        return data;
    }

    boolean holdsWand() {
        return this.getMainHandItem().is(TTItems.WAND.get());
    }

    /** Original: five embers (3 damage, 1.5 speed, 4.5 spread) from the fire focus; here five fire bolts. */
    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Spell spell = new Spell(CastStyle.INSTANT, SpellNode.of(Spell.ORIGIN).then(
                SpellNode.of(TTSpellParts.PROJECTILE).withSetting("speed", 2).then(SpellNode.of(TTSpellParts.FIRE))));
        Vec3 aim = target.getBoundingBox().getCenter().add(target.getDeltaMovement());
        double distance = this.distanceTo(target);
        for (int i = 0; i < 5; i++) {
            double spread = 0.0075 * 4.5 * distance;
            Vec3 point = aim.add(this.random.nextGaussian() * spread, this.random.nextGaussian() * spread, this.random.nextGaussian() * spread);
            Spells.cast(this, ItemStack.EMPTY, spell, List.of(SpellTarget.originTowards(this, point)), 1.0F);
        }
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FIRECHARGE_USE, this.getSoundSource(),
                0.4F, 1.0F + this.random.nextFloat() * 0.1F);
        this.swing(InteractionHand.MAIN_HAND);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        // Original: 0-1 knowledge fragments plus looting.
        int count = this.random.nextInt(2) + ThaumaturgeTrades.looting(level, source);
        if (count > 0) {
            this.spawnAtLocation(new ItemStack(TTItems.CURIO_KNOWLEDGE.get(), count), 1.5F);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return TCSounds.THINK.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 100;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return this.wanderTicks <= 0 && super.removeWhenFarAway(distance);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Anger", this.anger());
        tag.putInt("WanderTicks", this.wanderTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setAnger(tag.getInt("Anger"));
        this.wanderTicks = tag.getInt("WanderTicks");
    }

    /** The wand blast: only with the wand in hand, and only beyond 3 blocks (original AILongRangeAttack). */
    private static final class BlastGoal extends RangedAttackGoal {
        private final ThaumaturgeEntity mob;

        BlastGoal(ThaumaturgeEntity mob) {
            super(mob, 1.0, 20, 40, 15.0F);
            this.mob = mob;
        }

        private boolean usable() {
            LivingEntity target = this.mob.getTarget();
            return this.mob.holdsWand() && target != null && this.mob.distanceToSqr(target) > 9.0;
        }

        @Override
        public boolean canUse() {
            return this.usable() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return this.usable() && super.canContinueToUse();
        }
    }
}
