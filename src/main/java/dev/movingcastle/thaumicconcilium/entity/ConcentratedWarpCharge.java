package dev.movingcastle.thaumicconcilium.entity;

import com.leclowndu93150.thaumaturge.api.warp.IPlayerWarp;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import dev.movingcastle.thaumicconcilium.registry.TCSounds;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Concentrated Warp Charge: a piece of the caster's warp, pulled out of their head and made solid. It trails its
 * owner, and everything it touches takes {@code nonTemporaryWarp / 10 * power} damage, each hit burning one point
 * of the owner's temporary warp. It falls apart when the owner runs out of temporary warp, dies or leaves, and does
 * no harm while the owner is warded against warp.
 * <ul>
 *   <li><b>mass hysteria</b>: every half second one of the owner's harmful effects jumps onto a random creature
 *   nearby;</li>
 *   <li><b>self-flagellation</b>: every half second the owner takes one harmful effect off each nearby player.</li>
 * </ul>
 */
public class ConcentratedWarpCharge extends Entity {
    public static final int MODE_NONE = 0;
    public static final int MODE_HYSTERIA = 1;
    public static final int MODE_FLAGELLATION = 2;

    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(ConcentratedWarpCharge.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(ConcentratedWarpCharge.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final double MAX_DIST_SQ = 49.0;
    private static final double TELEPORT_DIST_SQ = 225.0;

    private int power = 1;
    private int reach;

    public ConcentratedWarpCharge(EntityType<? extends ConcentratedWarpCharge> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public static ConcentratedWarpCharge summon(ServerLevel level, Player owner, int power, int reach, int mode) {
        ConcentratedWarpCharge charge = new ConcentratedWarpCharge(TCEntities.WARP_CHARGE.get(), level);
        charge.entityData.set(OWNER, Optional.of(owner.getUUID()));
        charge.power = power;
        charge.reach = reach;
        charge.entityData.set(MODE, mode);
        charge.setPos(owner.getX(), owner.getY(), owner.getZ());
        level.addFreshEntity(charge);
        return charge;
    }

    public int mode() {
        return this.entityData.get(MODE);
    }

    public boolean isOwnedBy(Entity entity) {
        return this.entityData.get(OWNER).map(entity.getUUID()::equals).orElse(false);
    }

    @Nullable
    public Player ownerPlayer() {
        return this.entityData.get(OWNER).map(this.level()::getPlayerByUUID).orElse(null);
    }

    /** Pulls the charge toward {@code point}, as the original did while the focus was held on it. */
    public void steer(Vec3 point) {
        Motion.pullTowards(this, point, 1.0F);
    }

    public void hurl(Vec3 direction) {
        this.setDeltaMovement(direction.scale(2.5));
        this.hasImpulse = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MODE, MODE_NONE);
        builder.define(OWNER, Optional.empty());
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0;
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(this.getDeltaMovement().scale(0.7));
        Player owner = this.ownerPlayer();

        if (this.level().isClientSide()) {
            if (owner != null && this.tickCount % 3 == 0) {
                Vec3 from = owner.getBoundingBox().getCenter();
                Vec3 to = this.position().add(0.0, 0.5, 0.0);
                Vec3 step = to.subtract(from);
                for (int k = 0; k < 6; k++) {
                    Vec3 p = from.add(step.scale(this.random.nextDouble()));
                    this.level().addParticle(ParticleTypes.PORTAL, p.x, p.y, p.z, 0.0, 0.0, 0.0);
                }
            }
            if (this.mode() == MODE_HYSTERIA && this.random.nextInt(8) == 0) {
                for (int k = 0; k < 10; k++) {
                    this.level().addParticle(ParticleTypes.WITCH, this.getX(), this.getY() + 0.5, this.getZ(),
                            this.random.nextGaussian() * 0.1, this.random.nextGaussian() * 0.1, this.random.nextGaussian() * 0.1);
                }
            }
            this.move(MoverType.SELF, this.getDeltaMovement());
            return;
        }

        if (!(owner instanceof ServerPlayer player) || !player.isAlive()) {
            this.discard();
            return;
        }
        if (this.tickCount % 120 == 1) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), TCSounds.WHISPERS.get(), SoundSource.PLAYERS, 0.1F, 1.0F);
        }
        double distSq = this.distanceToSqr(player);
        if (distSq > TELEPORT_DIST_SQ) {
            this.setPos(player.getX(), player.getY(), player.getZ());
        } else if (distSq > MAX_DIST_SQ) {
            Motion.pullTowards(this, player.position(), 1.0F);
        }
        if (!player.getAbilities().flying) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.1, 0.0));
        }
        this.move(MoverType.SELF, this.getDeltaMovement());

        if (this.tickCount % 10 == 0) {
            int d = 5 + 4 * this.reach;
            if (this.mode() == MODE_HYSTERIA) {
                this.spreadHysteria(player, d);
            } else if (this.mode() == MODE_FLAGELLATION) {
                this.flagellate(player, d);
            }
        }

        IPlayerWarp warp = WarpHelper.getWarp(player);
        if (warp.get(WarpType.TEMPORARY) <= 0) {
            player.sendSystemMessage(Component.translatable("message.thaumicconcilium.no_warp").withStyle(ChatFormatting.DARK_PURPLE));
            this.discard();
            return;
        }
        if (player.hasEffect(TTMobEffects.WARP_WARD)) {
            return;
        }
        // The original, every tick: everything inside the charge (not the owner) is struck for permanent warp / 10
        // times power, and each one touched burns a point of temporary warp, whether the blow lands or not.
        float damage = warp.get(WarpType.PERMANENT) / 10.0F * this.power;
        for (LivingEntity touched : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox(), e -> e != player && e.isAlive())) {
            if (damage > 0.0F) {
                touched.hurt(this.damageSources().indirectMagic(this, player), damage);
            }
            WarpHelper.addWarp(player, -1, WarpType.TEMPORARY);
        }
    }

    private void spreadHysteria(Player owner, int d) {
        for (MobEffectInstance effect : new ArrayList<>(owner.getActiveEffects())) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                List<LivingEntity> near = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(d),
                        e -> e != owner && e.isAlive());
                if (!near.isEmpty()) {
                    Collections.shuffle(near);
                    near.get(0).addEffect(new MobEffectInstance(effect));
                    owner.removeEffect(effect.getEffect());
                    break;
                }
            }
        }
    }

    private void flagellate(Player owner, int d) {
        // The original code, 1:1 (agreed with the user 2026-11-21; its research line is flavour): for each other player
        // in range, the first harmful effect the owner carries is stripped from that player; nothing moves to the owner.
        for (Player other : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(d), p -> p != owner && p.isAlive())) {
            for (MobEffectInstance effect : new ArrayList<>(owner.getActiveEffects())) {
                if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                    other.removeEffect(effect.getEffect());
                    break;
                }
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(OWNER, tag.hasUUID("Owner") ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
        this.power = tag.getInt("Power");
        this.reach = tag.getInt("Reach");
        this.entityData.set(MODE, tag.getInt("Mode"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        this.entityData.get(OWNER).ifPresent(id -> tag.putUUID("Owner", id));
        tag.putInt("Power", this.power);
        tag.putInt("Reach", this.reach);
        tag.putInt("Mode", this.mode());
    }
}
