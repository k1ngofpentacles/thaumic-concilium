package dev.movingcastle.thaumicconcilium.entity;

import dev.movingcastle.thaumicconcilium.entity.mob.CrimsonCultist;
import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Crimson Orb the Paladin fires (original: CrimsonOrbEntity): a small dark sphere flying at one block per tick that turns round
 * after 50 ticks, vanishes after 100, and hits everything it overlaps (but its caster and other cultists) for 12 magic damage,
 * stripping absorption. Port: the player's runic shield is not drained; the original's shader orb is a crimson sparkle.
 */
public class CrimsonOrb extends Entity {
    public static final int LIFETIME = 100;
    public static final int POWER = 2;

    private int age;
    @Nullable
    private UUID casterId;

    public CrimsonOrb(EntityType<? extends CrimsonOrb> type, Level level) {
        super(type, level);
        noPhysics = false;
    }

    /** Fires an orb from {@code at} along {@code direction} (one block per tick). */
    public static CrimsonOrb launch(ServerLevel level, LivingEntity caster, Vec3 at, Vec3 direction) {
        CrimsonOrb orb = new CrimsonOrb(TCEntities.CRIMSON_ORB.get(), level);
        orb.setPos(at);
        orb.setDeltaMovement(direction);
        orb.casterId = caster.getUUID();
        level.addFreshEntity(orb);
        return orb;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        move(MoverType.SELF, getDeltaMovement());
        age++;
        if (level() instanceof ServerLevel server) {
            Entity caster = casterId == null ? null : server.getEntity(casterId);
            if (caster instanceof LivingEntity owner) {
                for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox())) {
                    if (target == owner || target instanceof CrimsonCultist || target instanceof com.leclowndu93150.thaumaturge.content.entity.EntityCultist) {
                        continue;
                    }
                    target.hurt(damageSources().indirectMagic(this, owner), 10 + POWER);
                    target.setAbsorptionAmount(0.0F);
                }
            }
            if (age == 50) {
                setDeltaMovement(getDeltaMovement().scale(-1.0));
            }
            if (age > LIFETIME) {
                discard();
            }
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        age = tag.getInt("Life");
        if (tag.hasUUID("Caster")) {
            casterId = tag.getUUID("Caster");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Life", age);
        if (casterId != null) {
            tag.putUUID("Caster", casterId);
        }
    }
}
