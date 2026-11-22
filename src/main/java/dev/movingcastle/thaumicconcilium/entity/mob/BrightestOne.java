package dev.movingcastle.thaumicconcilium.entity.mob;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.entity.EntityGolemOrb;
import com.leclowndu93150.thaumaturge.content.entity.WispEntity;
import com.leclowndu93150.thaumaturge.content.entity.ai.GhastLikeFlight;
import com.leclowndu93150.thaumaturge.content.entity.ai.GhastLikeMoveControl;
import com.leclowndu93150.thaumaturge.content.spell.block.PortableHoles;
import com.leclowndu93150.thaumaturge.content.taint.item.EssentiaCrystalFactory;
import com.leclowndu93150.thaumaturge.network.ClientboundWispZapPayload;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import dev.movingcastle.thaumicconcilium.entity.BossEmber;
import dev.movingcastle.thaumicconcilium.entity.ShardPowder;
import dev.movingcastle.thaumicconcilium.entity.Shadowbeam;
import dev.movingcastle.thaumicconcilium.item.ShardKind;
import dev.movingcastle.thaumicconcilium.registry.TCItems;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Brightest One (original {@code BrightestOne}): a three-block wisp of living magic that drifts like a ghast and
 * cannot be hurt. A hit only makes the attacker its target (for 200 ticks). Its health, 15, falls by one each time a
 * charged wisp reaches it ({@link #shortCircuit()}), which also changes its aspect to one of ignis, aer, vacuos or
 * tenebrae; the aspect decides its attacks. Summoned by a charged wisp entering a chained rift (see
 * {@code Subspace}); never despawns, except on peaceful.
 */
public class BrightestOne extends Monster {
    public static final List<ResourceKey<IAspect>> TYPES = List.of(TTAspects.IGNIS, TTAspects.AER, TTAspects.VACUOS, TTAspects.TENEBRAE);
    private static final EntityDataAccessor<String> TYPE = SynchedEntityData.defineId(BrightestOne.class, EntityDataSerializers.STRING);
    private static final double ATTACK_RANGE = 16.0;

    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.PROGRESS);
    { this.bossEvent.setDarkenScreen(true); } // as the original's BossStatus.setBossStatus(entity, true): darkens the sky
    private double waypointX;
    private double waypointY;
    private double waypointZ;
    private boolean hasWaypoint;
    private int aggroCooldown;
    public int attackCounter;
    public boolean continuousAttack;

    public BrightestOne(EntityType<? extends BrightestOne> type, Level level) {
        super(type, level);
        this.xpReward = 5;
        this.moveControl = new GhastLikeMoveControl(this);
        this.setNoGravity(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 15.0)
                .add(Attributes.ATTACK_DAMAGE, 25.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.FLYING_SPEED, 0.1);
    }

    // ---------------------------------------------------------------- state
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TYPE, "");
    }

    /** The aspect id (e.g. {@code thaumaturge:ignis}), or empty before it is chosen. */
    public String aspectId() {
        return this.entityData.get(TYPE);
    }

    public void setAspectId(String type) {
        this.entityData.set(TYPE, type);
    }

    public void setAspectId(ResourceKey<IAspect> aspect) {
        this.setAspectId(aspect.location().toString());
    }

    /** The aspect's colour (white before it is chosen). */
    public int color() {
        ResourceLocation id = ResourceLocation.tryParse(this.aspectId());
        if (id == null) {
            return 0xFFFFFF;
        }
        return this.registryAccess().registry(IAspect.REGISTRY_KEY)
                .flatMap(r -> r.getOptional(ResourceKey.create(IAspect.REGISTRY_KEY, id))).map(IAspect::color).orElse(0xFFFFFF);
    }

    public void changeType() {
        this.setAspectId(TYPES.get(this.random.nextInt(TYPES.size())));
    }

    /** A charged wisp reached it: one health, and at 0 it dies. */
    public void shortCircuit() {
        float health = Mth.clamp(this.getHealth() - 1.0F, 0.0F, this.getMaxHealth());
        this.setHealth(health);
        this.level().broadcastEntityEvent(this, (byte) 2);
        if (health <= 0.0F) {
            this.die(this.damageSources().magic());
        }
    }

    // ---------------------------------------------------------------- damage: none, only a grudge
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof BrightestOne || source.getDirectEntity() instanceof BrightestOne) {
            return false;
        }
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            // /kill and the void still work (the original had no such case; it could be stuck for ever).
            return super.hurt(source, amount);
        }
        if (source.getDirectEntity() instanceof LivingEntity direct) {
            this.setTarget(direct);
            this.aggroCooldown = 200;
        }
        if (source.getEntity() instanceof LivingEntity attacker) {
            this.setTarget(attacker);
            this.aggroCooldown = 200;
        }
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void travel(Vec3 input) {
        GhastLikeFlight.travel(this, input, 0.02F);
    }

    @Override
    public boolean canChangeDimensions(Level from, Level to) {
        return false;
    }

    // ---------------------------------------------------------------- boss bar
    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void remove(RemovalReason reason) {
        this.bossEvent.removeAllPlayers();
        super.remove(reason);
    }

    // ---------------------------------------------------------------- client effects
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (this.tickCount <= 1) {
                this.level().addParticle(com.leclowndu93150.thaumaturge.content.effect.WispEffects.burst(1.0F), this.getX(), this.getY() + 0.45, this.getZ(), 0, 0, 0);
            }
            if (this.isDeadOrDying() && this.deathTime == 1) {
                this.level().addParticle(com.leclowndu93150.thaumaturge.content.effect.WispEffects.burst(4.0F), this.getX(), this.getY() + 0.45, this.getZ(), 0, 0, 0);
            }
            if (this.random.nextBoolean() && !this.aspectId().isEmpty()) {
                this.level().addParticle(com.leclowndu93150.thaumaturge.content.effect.WispEffects.mote(this.random, this.color()),
                        this.getX() + (this.random.nextFloat() - this.random.nextFloat()) * 0.7,
                        this.getY() + 0.45 + (this.random.nextFloat() - this.random.nextFloat()) * 0.7,
                        this.getZ() + (this.random.nextFloat() - this.random.nextFloat()) * 0.7, 0, 0, 0);
            }
        }
    }

    // ---------------------------------------------------------------- the brain (original updateEntityActionState)
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (this.aspectId().isEmpty()) {
            this.setAspectId(TTAspects.IGNIS);
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            this.discard();
            return;
        }
        this.wander(level);

        LivingEntity target = this.getTarget();
        if (target != null && !target.isAlive()) {
            this.setTarget(null);
            target = null;
        }
        this.aggroCooldown--;
        if (target == null || this.aggroCooldown-- <= 0) {
            Player nearest = level.getNearestPlayer(TargetingConditions.forCombat().range(64.0).ignoreLineOfSight(), this);
            this.setTarget(nearest);
            target = nearest;
            if (nearest != null) {
                this.aggroCooldown = 50;
            }
        }

        if (target != null && target.distanceToSqr(this) < ATTACK_RANGE * ATTACK_RANGE) {
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            float yaw = -((float) Math.atan2(dx, dz)) * 180.0F / (float) Math.PI;
            this.setYRot(yaw);
            this.yBodyRot = yaw;
            this.yHeadRot = yaw;
            if (this.hasLineOfSight(target)) {
                this.attackCounter++;
                if (this.continuousAttack && this.attackCounter < 10) {
                    this.continuousVolley(level, target);
                } else if (this.attackCounter > 10) {
                    this.continuousAttack = false;
                }
                if (this.attackCounter == 20) {
                    this.attackCycle(level, target);
                    this.summonWisps(level);
                    this.attackCounter = -20 + this.random.nextInt(20);
                }
            } else if (this.attackCounter > 0) {
                this.attackCounter--;
            }
        } else {
            Vec3 v = this.getDeltaMovement();
            float yaw = -((float) Math.atan2(v.x, v.z)) * 180.0F / (float) Math.PI;
            this.setYRot(yaw);
            this.yBodyRot = yaw;
            if (this.attackCounter > 0) {
                this.attackCounter--;
            }
        }
    }

    /** Waypoint within 16 on each axis; the ghast-like move control nudges 0.1 towards it every few ticks. */
    private void wander(ServerLevel level) {
        double dx = this.waypointX - this.getX();
        double dy = this.waypointY - this.getY();
        double dz = this.waypointZ - this.getZ();
        double d = dx * dx + dy * dy + dz * dz;
        if (!this.hasWaypoint || d < 1.0 || d > 3600.0 || !this.moveControl.hasWanted()) {
            for (int i = 0; i < 10; i++) {
                double x = this.getX() + (this.random.nextFloat() * 2.0F - 1.0F) * 16.0;
                double y = this.getY() + (this.random.nextFloat() * 2.0F - 1.0F) * 16.0;
                double z = this.getZ() + (this.random.nextFloat() * 2.0F - 1.0F) * 16.0;
                if (this.validWaypoint(level, x, y, z)) {
                    this.waypointX = x;
                    this.waypointY = y;
                    this.waypointZ = z;
                    this.hasWaypoint = true;
                    this.moveControl.setWantedPosition(x, y, z, 1.0);
                    return;
                }
            }
        }
    }

    /** Original: not a liquid, and ground within ten blocks below (so it never drifts off into the void). */
    private boolean validWaypoint(ServerLevel level, double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        if (!level.hasChunkAt(pos) || !level.getFluidState(pos).isEmpty()) {
            return false;
        }
        for (int a = 0; a < 11; a++) {
            if (!level.isEmptyBlock(pos.below(a))) {
                return true;
            }
        }
        return false;
    }

    /** Original continuousAttack branch: aer's orb volley every tick, ignis's ember burst, tenebrae's beam. */
    private void continuousVolley(ServerLevel level, LivingEntity target) {
        switch (this.aspectId()) {
            case "thaumaturge:aer" -> {
                Vec3 look = this.getLookAngle();
                EntityGolemOrb orb = new EntityGolemOrb(level, this, target, false);
                orb.setPos(orb.getX() + look.x + (-0.5 + this.random.nextFloat()) * 5.0, orb.getY() + 0.5,
                        orb.getZ() + look.z + (-0.5 + this.random.nextFloat()) * 5.0);
                double tx = target.getX() + target.getDeltaMovement().x - this.getX();
                double ty = target.getY() - this.getY() - target.getBbHeight() / 2.0;
                double tz = target.getZ() + target.getDeltaMovement().z - this.getZ();
                orb.shoot(tx, ty, tz, 0.66F, 5.0F);
                this.playSound(TTSounds.EGATTACK.get(), 1.0F, 1.0F + this.random.nextFloat() * 0.1F);
                level.addFreshEntity(orb);
            }
            case "thaumaturge:ignis" -> {
                Vec3 look = this.getLookAngle();
                for (int i = 0; i < 30; i++) {
                    double mx = (look.x + this.random.nextGaussian() * 0.11) * (-0.5 + Math.random());
                    double my = (look.y + this.random.nextGaussian() * 0.11) * Math.random();
                    double mz = (look.z + this.random.nextGaussian() * 0.11) * (-0.5 + Math.random());
                    Vec3 at = this.getEyePosition().add(mx * (8.0 + Math.random()), my * (8.0 + Math.random()), mz * (8.0 + Math.random()));
                    level.addFreshEntity(BossEmber.create(level, this, at.x, at.y, at.z, mx, my, mz, 25.0F));
                }
            }
            case "thaumaturge:tenebrae" -> {
                this.lookAt(target, 100.0F, 100.0F);
                Shadowbeam.fire(level, this, 20);
            }
            default -> {
            }
        }
    }

    private void zap(ServerLevel level, LivingEntity target) {
        level.playSound(null, this.getX(), this.getY(), this.getZ(), TTSounds.ZAP.get(), SoundSource.HOSTILE, 1.0F, 1.1F);
        PacketDistributor.sendToPlayersNear(level, null, this.getX(), this.getY(), this.getZ(), 32.0,
                new ClientboundWispZapPayload(this.getId(), target.getId()), new CustomPacketPayload[0]);
    }

    /** What happens when the counter reaches 20. */
    private void attackCycle(ServerLevel level, LivingEntity target) {
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        switch (this.aspectId()) {
            case "thaumaturge:aer" -> {
                if (this.random.nextInt(10) % 2 == 0) {
                    this.zap(level, target);
                    target.hurt(this.damageSources().mobAttack(this), damage);
                } else {
                    this.continuousAttack = true;
                }
            }
            case "thaumaturge:ignis" -> {
                if (this.random.nextInt(6) % 2 == 0) {
                    Vec3 look = this.getLookAngle();
                    for (int i = 0; i < 10; i++) {
                        Vec3 at = new Vec3(this.getX() + look.x - Mth.cos(this.getYRot() / 180.0F * (float) Math.PI) * 0.32,
                                this.getEyeY() - 0.3, this.getZ() + look.z - Mth.sin(this.getYRot() / 180.0F * (float) Math.PI) * 0.32);
                        ShardPowder.spray(level, this, at, new Vec3(look.x * Math.random(), look.y, look.z * Math.random()), ShardKind.ENVY, 0);
                    }
                } else {
                    this.continuousAttack = true;
                }
            }
            case "thaumaturge:vacuos" -> {
                if (this.random.nextInt(10) % 2 == 0) {
                    BlockPos under = BlockPos.containing(target.getX(), Math.floor(target.getY()) - 1.0, target.getZ());
                    PortableHoles.open(level, under, Direction.UP, 33, 120);
                } else {
                    this.zap(level, target);
                    target.hurt(this.damageSources().fellOutOfWorld(), damage / 4.0F);
                }
            }
            case "thaumaturge:tenebrae" -> {
                if (this.random.nextInt(10) % 2 == 0) {
                    this.continuousAttack = true;
                } else {
                    for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(32.0))) {
                        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 600, 30));
                    }
                }
            }
            default -> {
            }
        }
    }

    /** Fewer than eight wisps within 32: a third of the time, 3 to 5 new ones of the four aspects. */
    private void summonWisps(ServerLevel level) {
        List<WispEntity> wisps = level.getEntitiesOfClass(WispEntity.class, this.getBoundingBox().inflate(32.0));
        if (wisps.size() < 8 && this.random.nextInt(3) == 2) {
            int count = this.random.nextInt(3) + 3;
            for (int i = 0; i < count; i++) {
                WispEntity wisp = TTEntities.WISP.get().create(level);
                if (wisp != null) {
                    wisp.moveTo(this.getX(), this.getY(), this.getZ(), this.random.nextFloat(), this.random.nextFloat());
                    wisp.setAspect(TYPES.get(this.random.nextInt(TYPES.size())).location());
                    level.addFreshEntity(wisp);
                }
            }
        }
    }

    // ---------------------------------------------------------------- loot and sounds
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        ResourceLocation id = ResourceLocation.tryParse(this.aspectId());
        if (id != null) {
            level.registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY).get(ResourceKey.create(IAspect.REGISTRY_KEY, id))
                    .ifPresent(h -> this.spawnAtLocation(EssentiaCrystalFactory.of((Holder<IAspect>) h, 2)));
        }
        this.spawnAtLocation(new net.minecraft.world.item.ItemStack(TCItems.PRIMORDIAL_LIFE.get()), this.getBbHeight() / 2.0F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return TTSounds.WISPLIVE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FIRE_EXTINGUISH;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return TTSounds.WISPDEAD.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.25F;
    }

    // ---------------------------------------------------------------- save
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Type", this.aspectId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setAspectId(tag.getString("Type"));
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }
}
