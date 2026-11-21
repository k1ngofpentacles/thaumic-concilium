package dev.movingcastle.thaumicconcilium.entity;

import com.leclowndu93150.thaumaturge.api.entity.trait.MobTraits;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import dev.movingcastle.thaumicconcilium.item.ShardKind;
import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import org.joml.Vector3f;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A mote of ground shard sprayed by a Shard Mill. It falls, bounces and drifts for two seconds, then acts on the
 * spot where it settles, as in the original:
 * <ul>
 *   <li>air: 1 in 10 calls lightning; fire: 1 in 10 a small fiery blast that breaks no blocks; water: bone meal on and
 *       under the spot; earth: dirt in a free space; order: strikes the creature there with the caster's attack damage;
 *       entropy: Wither 51 for 4.5 seconds;</li>
 *   <li>wrath: sets the creature alight, with Weakness 6 and Infectious Vis Exhaustion; envy: destroys projectiles within
 *       10 blocks, each spawning another envy mote, up to five generations; taint: homes on the nearest other player
 *       within 8 blocks, giving players 20 temporary warp and anything else Flux Taint; pride: flings the creature away;
 *       lust: 1 in 10 raises a small taintacle that goes for the nearest untainted creature; sloth: Slowness 16 and
 *       Infectious Vis Exhaustion; greed: drags the creature onto the spot.</li>
 * </ul>
 * Port changes: powders that act on a creature also act the moment they touch one (the original waited until the mote
 * settled), and a mote never acts on the player who sprayed it. Each mote trails dust in its shard's colour.
 */
public class ShardPowder extends Entity {
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(ShardPowder.class, EntityDataSerializers.INT);
    private static final int LIFETIME = 40;
    private static final int ENVY_GENERATIONS = 5;
    /** Kinds that act on a creature, and so act on touching one. */
    private static final Set<ShardKind> ON_CONTACT = EnumSet.of(ShardKind.ORDO, ShardKind.PERDITIO, ShardKind.WRATH,
            ShardKind.TAINT, ShardKind.PRIDE, ShardKind.SLOTH, ShardKind.GREED);
    @Nullable
    private Vector3f dust;

    private int age;
    private int generation;
    @Nullable
    private UUID casterId;
    @Nullable
    private Player homing;

    public ShardPowder(EntityType<? extends ShardPowder> type, Level level) {
        super(type, level);
    }

    public static void spray(ServerLevel level, LivingEntity caster, Vec3 at, Vec3 motion, ShardKind kind, int generation) {
        ShardPowder mote = new ShardPowder(TCEntities.SHARD_POWDER.get(), level);
        mote.setPos(at);
        mote.setDeltaMovement(motion.scale(0.4));
        mote.entityData.set(KIND, kind.ordinal());
        mote.casterId = caster.getUUID();
        mote.generation = generation;
        mote.setYRot(level.random.nextFloat() * 360.0F);
        level.addFreshEntity(mote);
    }

    public ShardKind kind() {
        return ShardKind.byOrdinal(this.entityData.get(KIND));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, 0);
    }

    @Nullable
    private LivingEntity caster() {
        if (this.casterId != null && this.level() instanceof ServerLevel server && server.getEntity(this.casterId) instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = this.getDeltaMovement().add(0.0, -0.03, 0.0);
        if (this.level().getFluidState(this.blockPosition()).is(FluidTags.LAVA)) {
            v = new Vec3((this.random.nextFloat() - this.random.nextFloat()) * 0.2, 0.2, (this.random.nextFloat() - this.random.nextFloat()) * 0.2);
            this.playSound(SoundEvents.FIRE_EXTINGUISH, 0.4F, 2.0F + this.random.nextFloat() * 0.4F);
        }
        if (this.kind() == ShardKind.TAINT && !this.level().isClientSide()) {
            v = this.homeOnPlayer(v);
        }
        this.setDeltaMovement(v);
        this.move(MoverType.SELF, this.getDeltaMovement());
        float friction = 0.98F;
        if (this.onGround()) {
            BlockPos below = BlockPos.containing(this.getX(), this.getBoundingBox().minY - 1.0, this.getZ());
            friction = this.level().getBlockState(below).getFriction(this.level(), below, this) * 0.98F;
        }
        Vec3 m = this.getDeltaMovement();
        this.setDeltaMovement(m.x * friction, m.y * 0.98 * (this.onGround() ? -0.9 : 1.0), m.z * friction);
        if (this.level().isClientSide()) {
            if (this.dust == null) {
                int c = this.kind().color(this.level().registryAccess());
                this.dust = new Vector3f((c >> 16 & 255) / 255.0F, (c >> 8 & 255) / 255.0F, (c & 255) / 255.0F);
            }
            this.level().addParticle(new DustParticleOptions(this.dust, 0.9F), this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            return;
        }
        ServerLevel server = (ServerLevel) this.level();
        boolean touching = ON_CONTACT.contains(this.kind()) && this.creatureHere(server, 0.3) != null;
        if (++this.age >= LIFETIME || touching) {
            this.act(server);
            this.discard();
        }
    }

    private Vec3 homeOnPlayer(Vec3 v) {
        double range = 8.0;
        LivingEntity caster = this.caster();
        if (this.homing == null && this.tickCount % 5 == 0) {
            this.homing = this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), range,
                    p -> p != caster && !p.isSpectator());
        }
        if (this.homing != null) {
            double dx = (this.homing.getX() - this.getX()) / range;
            double dy = (this.homing.getEyeY() - this.getY()) / range;
            double dz = (this.homing.getZ() - this.getZ()) / range;
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double pull = 1.0 - d;
            if (pull > 0.0) {
                pull *= pull;
                v = v.add(dx / d * pull * 0.1, dy / d * pull * 0.1, dz / d * pull * 0.1);
            }
        }
        return v;
    }

    @Nullable
    private LivingEntity creatureHere(ServerLevel level) {
        return this.creatureHere(level, 1.5);
    }

    /** The first living creature within {@code reach}, never the player who sprayed the powder. */
    @Nullable
    private LivingEntity creatureHere(ServerLevel level, double reach) {
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(reach),
                e -> e.isAlive() && !e.getUUID().equals(this.casterId));
        return near.isEmpty() ? null : near.get(0);
    }

    /** The original's motion helper: aim the entity's motion at {@code target}, at most unit length, times {@code speed}. */
    private static void pushTowards(Entity e, Vec3 target, double speed) {
        Vec3 d = target.subtract(e.getBoundingBox().getCenter());
        if (d.length() > 1.0) {
            d = d.normalize();
        }
        e.setDeltaMovement(d.scale(speed));
        e.hurtMarked = true;
    }

    private void act(ServerLevel level) {
        BlockPos pos = this.blockPosition();
        LivingEntity caster = this.caster();
        switch (this.kind()) {
            case AER -> {
                if (this.random.nextInt(10) > 8) {
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                    if (bolt != null) {
                        bolt.moveTo(this.position());
                        level.addFreshEntity(bolt);
                    }
                }
            }
            case IGNIS -> {
                if (this.random.nextInt(10) > 8) {
                    level.explode(null, this.getX(), this.getY(), this.getZ(), 1.5F, true, Level.ExplosionInteraction.NONE);
                }
            }
            case AQUA -> {
                ItemStack meal = new ItemStack(Items.BONE_MEAL, 2);
                BoneMealItem.applyBonemeal(meal, level, pos.below(), caster instanceof Player p ? p : null);
                BoneMealItem.applyBonemeal(meal, level, pos, caster instanceof Player p ? p : null);
            }
            case TERRA -> {
                if (level.getBlockState(pos).canBeReplaced()) {
                    level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
                }
            }
            case ORDO -> {
                LivingEntity e = this.creatureHere(level);
                if (e != null && caster != null && caster.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                    DamageSource source = caster instanceof Player p ? level.damageSources().playerAttack(p) : level.damageSources().mobAttack(caster);
                    e.hurt(source, (float) caster.getAttributeValue(Attributes.ATTACK_DAMAGE));
                }
            }
            case PERDITIO -> {
                LivingEntity e = this.creatureHere(level);
                if (e != null) {
                    e.addEffect(new MobEffectInstance(MobEffects.WITHER, 90, 50));
                }
            }
            case WRATH -> {
                LivingEntity e = this.creatureHere(level);
                if (e != null) {
                    e.igniteForSeconds(50000);
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 5));
                    e.addEffect(new MobEffectInstance(TTMobEffects.INFECTIOUS_VIS_EXHAUST, 200, 10));
                }
            }
            case ENVY -> {
                for (Projectile p : level.getEntitiesOfClass(Projectile.class, this.getBoundingBox().inflate(10.0))) {
                    if (caster != null && this.generation < ENVY_GENERATIONS) {
                        spray(level, caster, p.position(), Vec3.ZERO, ShardKind.ENVY, this.generation + 1);
                    }
                    p.discard();
                }
            }
            case TAINT -> {
                LivingEntity e = this.creatureHere(level);
                if (e instanceof ServerPlayer player) {
                    WarpHelper.addWarp(player, 20, WarpType.TEMPORARY);
                } else if (e != null) {
                    e.addEffect(new MobEffectInstance(TTMobEffects.FLUX_TAINT, 80, 50));
                }
            }
            case PRIDE -> {
                LivingEntity e = this.creatureHere(level);
                if (e != null) {
                    // The original aimed at a point built from the offset alone (a bug that sent creatures towards
                    // the world origin); this keeps its intent, flinging them away from the mote.
                    Vec3 away = new Vec3(e.getX() - this.getX(), 0.0, e.getZ() - this.getZ()).scale(2.0);
                    pushTowards(e, e.getBoundingBox().getCenter().add(away).add(0.0, 0.5, 0.0), 3.0);
                }
            }
            case LUST -> {
                if (this.random.nextInt(10) > 8) {
                    Mob taintacle = TTEntities.TAINTACLE_SMALL.get().create(level);
                    if (taintacle != null) {
                        taintacle.moveTo(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);
                        level.addFreshEntity(taintacle);
                        level.getEntitiesOfClass(LivingEntity.class, taintacle.getBoundingBox().inflate(20.5, 2.5, 20.5),
                                        e -> e != taintacle && e.isAlive() && !MobTraits.isTainted(e))
                                .stream().findFirst().ifPresent(taintacle::setTarget);
                    }
                }
            }
            case SLOTH -> {
                LivingEntity e = this.creatureHere(level);
                if (e != null) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 15));
                    e.addEffect(new MobEffectInstance(TTMobEffects.INFECTIOUS_VIS_EXHAUST, 60, 20));
                }
            }
            case GREED -> {
                LivingEntity e = this.creatureHere(level);
                if (e != null) {
                    pushTowards(e, this.position(), 3.0);
                }
            }
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(KIND, tag.getInt("Kind"));
        this.age = tag.getInt("Age");
        this.generation = tag.getInt("Generation");
        if (tag.hasUUID("Caster")) {
            this.casterId = tag.getUUID("Caster");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Kind", this.entityData.get(KIND));
        tag.putInt("Age", this.age);
        tag.putInt("Generation", this.generation);
        if (this.casterId != null) {
            tag.putUUID("Caster", this.casterId);
        }
    }
}
