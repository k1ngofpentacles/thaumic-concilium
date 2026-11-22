package dev.movingcastle.thaumicconcilium.entity;

import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import dev.movingcastle.thaumicconcilium.registry.TCItems;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Thaumcraft's primal arrow: a vanilla arrow with a primal kind (see {@link PrimalType}). Flies 1.5 times as fast as a normal arrow
 * at the same draw, hits for the kind's multiplier of the base damage and carries the kind's effect:
 * aer and ordo are magic damage that ignores armour, ignis adds five seconds of fire, aqua slows (V, 10 s), ordo weakens (V, 10 s),
 * perditio withers (5 s), terra knocks back one Punch level more when a player shot it.
 * Built on {@link AbstractArrow}, so bows, crossbows, Power, Punch, Flame, piercing and dispensers all work unchanged.
 */
public class PrimalArrow extends AbstractArrow {
    private static final EntityDataAccessor<Byte> TYPE = SynchedEntityData.defineId(PrimalArrow.class, EntityDataSerializers.BYTE);
    /** The original's shot velocity factor ({@code power * 1.5}). */
    public static final float SPEED_FACTOR = 1.5F;

    @Nullable
    private DamageSources kindSources;

    public PrimalArrow(EntityType<? extends PrimalArrow> type, Level level) {
        super(type, level);
    }

    /** Loose arrow at a position (dispensers). The stack is the pickup stack and decides the kind. */
    public PrimalArrow(Level level, double x, double y, double z, ItemStack stack) {
        super(TCEntities.PRIMAL_ARROW.get(), x, y, z, level, stack, null);
        setKindFrom(stack);
    }

    /** Arrow shot by {@code owner} from a bow or crossbow ({@code weapon}); the stack is the pickup stack and decides the kind. */
    public PrimalArrow(Level level, LivingEntity owner, ItemStack stack, @Nullable ItemStack weapon) {
        super(TCEntities.PRIMAL_ARROW.get(), owner, level, stack, weapon);
        setKindFrom(stack);
    }

    /**
     * Mob-facing factory (the Crimson Archer): spawns an arrow of {@code type} at the shooter's eyes flying along (dx, dy, dz) and
     * returns it. {@code velocity} is the vanilla arrow velocity (1.6 for a skeleton): the 1.5 factor is applied here. Not
     * pick-up-able (the owner is not a player). Server side only.
     */
    public static PrimalArrow shoot(Level level, LivingEntity shooter, PrimalType type, double dx, double dy, double dz,
                                    float velocity, float inaccuracy) {
        return shoot(level, shooter, type, dx, dy, dz, velocity, inaccuracy, null);
    }

    /** As above, with the bow the shooter fires from (its enchantments, Power, Punch, Flame, apply to the arrow). */
    public static PrimalArrow shoot(Level level, LivingEntity shooter, PrimalType type, double dx, double dy, double dz,
                                    float velocity, float inaccuracy, @Nullable ItemStack weapon) {
        PrimalArrow arrow = new PrimalArrow(level, shooter, new ItemStack(TCItems.PRIMAL_ARROWS.get(type).get()), weapon);
        arrow.shoot(dx, dy, dz, velocity, inaccuracy);
        level.addFreshEntity(arrow);
        return arrow;
    }

    private void setKindFrom(ItemStack stack) {
        if (stack.getItem() instanceof dev.movingcastle.thaumicconcilium.item.PrimalArrowItem item) {
            setPrimalType(item.kind());
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TYPE, (byte) 0);
    }

    public PrimalType primalType() {
        return PrimalType.byIndex(entityData.get(TYPE));
    }

    public void setPrimalType(PrimalType type) {
        entityData.set(TYPE, (byte) type.ordinal());
        kindSources = null;
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(TCItems.PRIMAL_ARROWS.get(primalType()).get());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("type", (byte) primalType().ordinal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setPrimalType(PrimalType.byIndex(tag.getByte("type")));
        if (!tag.contains("item", 10)) {
            setPickupItemStack(getDefaultPickupItem());
        }
    }

    /** One and a half times the vanilla velocity, for every launcher (bows, crossbows, dispensers, mobs). */
    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        super.shoot(x, y, z, velocity * SPEED_FACTOR, inaccuracy);
    }

    /** Aqua arrows put the target out instead of lighting it: a flaming aqua arrow does not ignite. */
    @Override
    public boolean isOnFire() {
        return primalType() != PrimalType.AQUA && super.isOnFire();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        double base = getBaseDamage();
        double factor = primalType().damageMultiplier();
        if (factor != 1.0) {
            setBaseDamage(base * factor);
        }
        try {
            super.onHitEntity(hit);
        } finally {
            if (factor != 1.0) {
                setBaseDamage(base);
            }
        }
    }

    /** Vanilla builds the arrow damage source from {@code damageSources().arrow}; the kinds with their own damage type swap it here. */
    @Override
    public DamageSources damageSources() {
        if (primalType().damageType() == null) {
            return super.damageSources();
        }
        if (kindSources == null) {
            kindSources = new KindSources(this);
        }
        return kindSources;
    }

    @Override
    protected void doKnockback(LivingEntity target, DamageSource source) {
        super.doKnockback(target, source);
        if (primalType() == PrimalType.TERRA && getOwner() instanceof Player) {
            // one extra Punch level: 0.6 blocks of push along the flight, reduced by knockback resistance
            double resist = Math.max(0.0, 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            Vec3 push = getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize().scale(0.6 * resist);
            if (push.lengthSqr() > 0.0) {
                target.push(push.x, 0.1, push.z);
            }
        }
    }

    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        Entity source = getEffectSource();
        switch (primalType()) {
            case IGNIS -> {
                if (target.getType() != EntityType.ENDERMAN) {
                    target.igniteForTicks(target.getRemainingFireTicks() + 100);
                }
            }
            case AQUA -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 4), source);
            case ORDO -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 4), source);
            case PERDITIO -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0), source);
            default -> {
            }
        }
    }

    /** Damage sources whose arrow hits use the primal kind's damage type. */
    private static final class KindSources extends DamageSources {
        private final PrimalArrow arrow;

        KindSources(PrimalArrow arrow) {
            super(arrow.level().registryAccess());
            this.arrow = arrow;
        }

        @Override
        public DamageSource arrow(AbstractArrow projectile, @Nullable Entity owner) {
            Holder<DamageType> type = arrow.level().registryAccess()
                    .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(arrow.primalType().damageType());
            return new DamageSource(type, projectile, owner);
        }
    }
}
