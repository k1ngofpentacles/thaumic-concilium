package dev.movingcastle.thaumicconcilium.entity.mob;

import com.leclowndu93150.thaumaturge.api.capability.PlayerResearchAccess;
import com.leclowndu93150.thaumaturge.content.crucible.BlockEntityCrucible;
import com.leclowndu93150.thaumaturge.content.entity.EntityCultist;
import com.leclowndu93150.thaumaturge.content.research.ResearchManager;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.block.QuicksilverReactorBlockEntity;
import dev.movingcastle.thaumicconcilium.mixin.CrucibleAccessor;
import dev.movingcastle.thaumicconcilium.registry.TCBlocks;
import dev.movingcastle.thaumicconcilium.registry.TCSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * The Quicksilver Elemental (original: {@code QuicksilverElemental}), a mercury man that only fire (or slowing it) can
 * hurt. Its size follows its health. Burned while standing in a Thaumaturge crucible, it fuses into the crucible's place
 * as a Quicksilver Reactor and shows every player within 16 blocks how.
 */
public class QuicksilverElemental extends Monster {
    public static final ResourceLocation RESEARCH = ThaumicConcilium.id("quicksilver_crucible");
    private static final float FULL_HEALTH = 30.0F;
    private float scaledFor = -1.0F;

    public QuicksilverElemental(EntityType<? extends QuicksilverElemental> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.MOVEMENT_SPEED, 0.6)
                .add(Attributes.MAX_HEALTH, FULL_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, 5.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.6, false));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.6));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, ThaumaturgeEntity.class, 10, false, false, null));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, EntityCultist.class, 10, false, false, null));
    }

    /** Original: hitbox and model scale with health / 30 (here the vanilla scale attribute does both). */
    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide() && this.getHealth() != this.scaledFor) {
            this.scaledFor = this.getHealth();
            AttributeInstance scale = this.getAttribute(Attributes.SCALE);
            if (scale != null) {
                scale.setBaseValue(Math.max(0.1, this.getHealth() / FULL_HEALTH));
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!source.is(DamageTypeTags.IS_FIRE) && !this.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            return false;
        }
        if (this.level() instanceof ServerLevel level && !this.isRemoved()) {
            BlockPos pos = this.blockPosition();
            if (level.getBlockEntity(pos) instanceof BlockEntityCrucible crucible) {
                this.fuseInto(level, pos, crucible);
                return true;
            }
        }
        return super.hurt(source, amount);
    }

    private void fuseInto(ServerLevel level, BlockPos pos, BlockEntityCrucible crucible) {
        // The original simply deleted the crucible and its contents; empty it first so Thaumaturge's block removal does
        // not spill the lot into the aura as flux.
        ((CrucibleAccessor) crucible).thaumicconcilium$setAspects(com.leclowndu93150.thaumaturge.api.aspect.AspectList.EMPTY);
        crucible.getTank().setFluid(net.neoforged.neoforge.fluids.FluidStack.EMPTY);
        level.setBlock(pos, TCBlocks.QUICKSILVER_REACTOR.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof QuicksilverReactorBlockEntity reactor) {
            reactor.setPermutatio(20);
        }
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(16.0))) {
            if (!PlayerResearchAccess.of(player).hasCompleted(RESEARCH) && ResearchManager.complete(player, RESEARCH)) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.HEARTBEAT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
        this.discard();
    }

    /** Original: deals the victim's own attack damage (its own 5 if the victim has none). */
    @Override
    public boolean doHurtTarget(Entity target) {
        this.swing(InteractionHand.MAIN_HAND);
        if (target instanceof LivingEntity living) {
            AttributeInstance theirs = living.getAttribute(Attributes.ATTACK_DAMAGE);
            float damage = theirs != null ? (float) theirs.getValue() : (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
            return target.hurt(this.damageSources().mobAttack(this), damage);
        }
        return super.doHurtTarget(target);
    }

    /** Original: every 2 seconds its target gets an uncurable Infectious Vis Exhaust and loses one good effect. */
    @Override
    public void tick() {
        super.tick();
        LivingEntity target = this.getTarget();
        if (!this.level().isClientSide() && this.tickCount % 40 == 0 && target != null) {
            MobEffectInstance exhaust = new MobEffectInstance(TTMobEffects.INFECTIOUS_VIS_EXHAUST, 1000, 20);
            exhaust.getCures().clear();
            target.addEffect(exhaust);
            for (MobEffectInstance effect : target.getActiveEffects()) {
                if (effect.getEffect().value().getCategory() != MobEffectCategory.HARMFUL) {
                    target.removeEffect(effect.getEffect());
                    break;
                }
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        int count = this.random.nextInt(8) + 4 + ThaumaturgeTrades.looting(level, source);
        this.spawnAtLocation(new ItemStack(TTItems.QUICKSILVER.get(), count), 1.5F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return TCSounds.MELTED.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return TCSounds.MELTED.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GLASS_BREAK;
    }

    @Override
    protected float getSoundVolume() {
        return 2.0F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 65;
    }
}
