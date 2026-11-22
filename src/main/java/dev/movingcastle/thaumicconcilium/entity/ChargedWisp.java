package dev.movingcastle.thaumicconcilium.entity;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import dev.movingcastle.thaumicconcilium.entity.mob.BrightestOne;
import dev.movingcastle.thaumicconcilium.rift.ConciliumRifts;
import dev.movingcastle.thaumicconcilium.rift.Subspace;
import com.leclowndu93150.thaumaturge.api.capability.PlayerResearchAccess;
import com.leclowndu93150.thaumaturge.content.entity.EntityFluxRift;
import net.minecraft.world.entity.player.Player;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * A charged wisp (original: {@code ChargedWispEntity}): a wisp flung by the Vis Conductor's wisp launcher, or thrown at
 * creatures by a spatial rift. It flies straight, and every creature within 4 blocks of it takes 10 + power damage and
 * a long infectious vis exhaustion. It fizzles out after its lifetime.
 */
public class ChargedWisp extends Entity {
    private static final EntityDataAccessor<String> ASPECT = SynchedEntityData.defineId(ChargedWisp.class, EntityDataSerializers.STRING);
    private int age;
    private int lifetime = 40;
    private int power = 2;
    @Nullable
    private UUID caster;
    /** Cast by a player who has the Brightest One research (decided at the cast, so no lookup per tick). */
    private boolean summons;

    public ChargedWisp(EntityType<? extends ChargedWisp> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    /** Launched by a player (wisp launcher): along their look, lifetime 20-40 plus 20 per Extend, power = potency. */
    public static ChargedWisp launched(Level level, Vec3 at, LivingEntity caster, String aspect, int extend, int potency) {
        ChargedWisp wisp = new ChargedWisp(TCEntities.CHARGED_WISP.get(), level);
        wisp.setPos(at);
        wisp.caster = caster.getUUID();
        wisp.summons = caster instanceof Player player && PlayerResearchAccess.of(player).hasCompleted(Subspace.BOSS_RESEARCH);
        wisp.setDeltaMovement(caster.getLookAngle());
        wisp.lifetime = 20 + wisp.random.nextInt(20) + 20 * extend;
        wisp.power = potency;
        wisp.setAspect(aspect);
        return wisp;
    }

    /** Thrown by a rift: towards a target, lifetime 40-100, power 2. */
    public static ChargedWisp thrown(Level level, Vec3 at, Vec3 motion, String aspect) {
        ChargedWisp wisp = new ChargedWisp(TCEntities.CHARGED_WISP.get(), level);
        wisp.setPos(at);
        wisp.setDeltaMovement(motion);
        wisp.lifetime = 40 + wisp.random.nextInt(60);
        wisp.power = 2;
        wisp.setAspect(aspect);
        return wisp;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ASPECT, "thaumaturge:aer");
    }

    public String aspect() {
        return this.entityData.get(ASPECT);
    }

    public void setAspect(String aspect) {
        this.entityData.set(ASPECT, aspect);
    }

    /** The aspect's colour, for the renderer and particles. */
    public int color() {
        ResourceLocation id = ResourceLocation.tryParse(this.aspect());
        if (id == null) {
            return 0xFFFFFF;
        }
        return this.level().registryAccess().registry(IAspect.REGISTRY_KEY)
                .flatMap(r -> r.getOptional(ResourceKey.create(IAspect.REGISTRY_KEY, id))).map(IAspect::color).orElse(0xFFFFFF);
    }

    @Nullable
    public UUID caster() {
        return this.caster;
    }

    @Override
    public void tick() {
        super.tick();
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.age++;
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        int rgb = this.color();
        if (this.shortCircuitBoss(level) || this.openBossRift(level)) {
            return;
        }
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(4.0),
                e -> (this.caster == null || !e.getUUID().equals(this.caster)) && !(e instanceof BrightestOne));
        Entity owner = this.caster == null ? null : level.getEntity(this.caster);
        for (LivingEntity e : near) {
            e.hurt(level.damageSources().indirectMagic(this, owner), 10 + this.power);
            MobEffectInstance exhaust = new MobEffectInstance(TTMobEffects.INFECTIOUS_VIS_EXHAUST, 1000 * Math.max(1, this.power), 30);
            e.addEffect(exhaust, this);
            if (this.random.nextInt(50) == 0) {
                level.playSound(null, this, TTSounds.JACOBS.get(), SoundSource.HOSTILE, 0.5F,
                        1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
            }
            // Original: an arc of lightning in the wisp's colour from it to each victim.
            Vec3 from = this.position();
            Vec3 to = e.position().add(0.0, e.getBbHeight() / 2.0, 0.0);
            DustParticleOptions dust = new DustParticleOptions(new Vector3f((rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F), 0.8F);
            for (int i = 0; i <= 6; i++) {
                Vec3 p = from.lerp(to, i / 6.0);
                level.sendParticles(dust, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
            }
        }
        if (this.age > this.lifetime) {
            this.discard();
        }
    }

    /** Original: a charged wisp whose 4-block box meets a Brightest One older than 20 ticks short-circuits it and is spent. */
    private boolean shortCircuitBoss(ServerLevel level) {
        for (BrightestOne boss : level.getEntitiesOfClass(BrightestOne.class, this.getBoundingBox().inflate(4.0))) {
            if (boss.tickCount > 20 && boss.getHealth() > 0.0F) {
                boss.changeType();
                boss.shortCircuit();
                int rgb = this.color();
                DustParticleOptions dust = new DustParticleOptions(new Vector3f((rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F), 1.2F);
                Vec3 to = boss.position().add(0.0, 1.5, 0.0);
                for (int i = 0; i <= 8; i++) {
                    Vec3 p = this.position().lerp(to, i / 8.0);
                    level.sendParticles(dust, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
                }
                this.discard();
                return true;
            }
        }
        return false;
    }

    /** Original RiftEntity: a charged wisp cast by a player with the Brightest One research entering a chained rift. */
    private boolean openBossRift(ServerLevel level) {
        if (!this.summons) {
            return false;
        }
        for (EntityFluxRift rift : level.getEntitiesOfClass(EntityFluxRift.class, this.getBoundingBox().expandTowards(this.getDeltaMovement().scale(-1.0)).inflate(0.5), r -> !r.isRemoved() && !r.isCollapsing())) {
            if (ConciliumRifts.kind(rift) == ConciliumRifts.Kind.CHAINED) {
                Subspace.summonBoss(rift);
                this.discard();
                return true;
            }
        }
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getInt("Life");
        this.lifetime = tag.getInt("LifeTime");
        this.power = tag.getInt("power");
        this.summons = tag.getBoolean("summons");
        this.setAspect(tag.getString("type"));
        if (tag.hasUUID("caster")) {
            this.caster = tag.getUUID("caster");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Life", this.age);
        tag.putInt("LifeTime", this.lifetime);
        tag.putInt("power", this.power);
        tag.putBoolean("summons", this.summons);
        tag.putString("type", this.aspect());
        if (this.caster != null) {
            tag.putUUID("caster", this.caster);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0;
    }
}
