package dev.movingcastle.thaumicconcilium.entity;

import com.leclowndu93150.thaumaturge.api.spell.Spells;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellContinuation;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * One shot of the Cooling focus's flurry: a plain snowball (harmless, 3 damage to blazes, like vanilla) or,
 * with the old Refreeze upgrade, a fragile frost shard doing 1 damage that shatters on anything. If the spell has
 * further parts they are resumed at the impact point, so a flurry can carry other effects.
 */
public class FlurryShot extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Boolean> SHARD = SynchedEntityData.defineId(FlurryShot.class, EntityDataSerializers.BOOLEAN);

    private Optional<SpellContinuation> payload = Optional.empty();
    /** Damage override for shards re-forged by Alchemist's Spree; negative means the default. */
    private float damage = -1.0F;
    /** Slowness strength applied on hit (Alchemist's Spree with primal essence); 0 for none. */
    private int frost;

    public FlurryShot(EntityType<? extends FlurryShot> type, Level level) {
        super(type, level);
    }

    public static void throwFrom(ServerLevel level, LivingEntity caster, Vec3 at, Vec3 direction, boolean shard,
                                 float speed, Optional<SpellContinuation> payload) {
        FlurryShot shot = new FlurryShot(TCEntities.FLURRY_SHOT.get(), level);
        shot.setOwner(caster);
        shot.setPos(at.x, at.y, at.z);
        shot.entityData.set(SHARD, shard);
        shot.setItem(new ItemStack(shard ? Items.ICE : Items.SNOWBALL));
        shot.payload = payload;
        shot.shoot(direction.x, direction.y, direction.z, speed, shard ? 1.0F : 1.0F);
        level.addFreshEntity(shot);
        level.playSound(null, at.x, at.y, at.z, shard ? SoundEvents.GLASS_HIT : SoundEvents.ARROW_SHOOT,
                caster.getSoundSource(), shard ? 0.4F : 0.5F,
                shard ? 1.0F + level.random.nextFloat() * 0.1F : 0.4F / (level.random.nextFloat() * 0.4F + 0.8F));
    }

    /**
     * Alchemist's Spree: a caught snowball comes back out of the caster's hand as a frost shard with the given
     * damage, chilling whatever it hits when {@code frost > 0}.
     */
    public static void reforgeShard(ServerLevel level, LivingEntity caster, float damage, int frost) {
        FlurryShot shot = new FlurryShot(TCEntities.FLURRY_SHOT.get(), level);
        shot.setOwner(caster);
        shot.setPos(caster.getX(), caster.getEyeY() - 0.1, caster.getZ());
        shot.entityData.set(SHARD, true);
        shot.setItem(new ItemStack(Items.ICE));
        shot.damage = damage;
        shot.frost = frost;
        shot.shootFromRotation(caster, caster.getXRot(), caster.getYRot(), 0.0F, 1.5F, 1.0F);
        level.addFreshEntity(shot);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.GLASS_HIT, caster.getSoundSource(),
                0.4F, 1.0F + level.random.nextFloat() * 0.1F);
    }

    public boolean isShard() {
        return this.entityData.get(SHARD);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SHARD, false);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SNOWBALL;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            ItemParticleOption particle = new ItemParticleOption(ParticleTypes.ITEM, this.getItem());
            for (int i = 0; i < 8; i++) {
                this.level().addParticle(particle, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        float damage = this.isShard() ? (this.damage >= 0.0F ? this.damage : 1.0F) : (result.getEntity() instanceof Blaze ? 3.0F : 0.0F);
        result.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), damage);
        if (this.frost > 0 && result.getEntity() instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, this.frost - 1));
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level() instanceof ServerLevel server) {
            Vec3 at = result.getLocation();
            Vec3 dir = this.getDeltaMovement().lengthSqr() > 1.0E-6 ? this.getDeltaMovement().normalize() : new Vec3(0, -1, 0);
            this.payload.ifPresent(p -> {
                SpellTarget target = result instanceof BlockHitResult bhr
                        ? new SpellTarget(bhr, at, dir)
                        : new SpellTarget(result, at, dir);
                Spells.resume(server, p, List.of(target));
            });
            server.broadcastEntityEvent(this, (byte) 3);
            if (this.isShard()) {
                this.playSound(SoundEvents.GLASS_BREAK, 0.3F, 1.2F + this.random.nextFloat() * 0.2F);
            }
            this.discard();
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(SHARD, tag.getBoolean("Shard"));
        this.damage = tag.contains("Damage") ? tag.getFloat("Damage") : -1.0F;
        this.frost = tag.getInt("Frost");
        this.payload = tag.contains("Payload")
                ? SpellContinuation.CODEC.parse(NbtOps.INSTANCE, tag.get("Payload")).result()
                : Optional.empty();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Shard", this.isShard());
        tag.putFloat("Damage", this.damage);
        tag.putInt("Frost", this.frost);
        this.payload.flatMap(p -> SpellContinuation.CODEC.encodeStart(NbtOps.INSTANCE, p).result())
                .ifPresent(nbt -> tag.put("Payload", nbt));
    }
}
