package dev.movingcastle.thaumicconcilium.aura;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.gadomancy.aura.effect.AuraEffect;
import dev.movingcastle.gadomancy.aura.effect.AuraEffectType;
import dev.movingcastle.thaumicconcilium.registry.TCAuraEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Original terminus effect ({@code GadomancyIntegration.java:280-299}, stone shower {@code :348-359}): every 600 ticks one random block
 * within 32 blocks is the target of 10 explosive orbs ({@code EntityExplosiveOrb}, strength 20, fire) dropped from the build limit.
 * Unbound (user decision g1-design 6); only the first of the original's meteor kinds exists, the others need mods we do not port.
 * The orb is a vanilla {@link LargeFireball} (no new entity type, no renderer): see {@link MeteorOrb}.
 */
public record MeteorShowerEffect(Timing timing) implements AuraEffect {
    public static final MapCodec<MeteorShowerEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Timing.CODEC.forGetter(MeteorShowerEffect::timing)).apply(i, MeteorShowerEffect::new));

    public static final int ORBS = 10;
    public static final int STRENGTH = 20;

    @Override
    public AuraEffectType<?> type() {
        return TCAuraEffects.METEOR_SHOWER.get();
    }

    @Override
    public boolean affectsEntities() {
        return false;
    }

    @Override
    public int blockCount(RandomSource random) {
        return 1;
    }

    @Override
    public void affectBlock(ServerLevel level, BlockPos origin, BlockPos block) {
        for (int i = 0; i < ORBS; i++) {
            MeteorOrb orb = new MeteorOrb(level, STRENGTH);
            orb.setPos(block.getX(), level.getMaxBuildHeight(), block.getZ());
            orb.setDeltaMovement((-0.5 + level.random.nextGaussian()) * 0.1, -0.1, (-0.5 + level.random.nextGaussian()) * 0.1);
            level.addFreshEntity(orb);
        }
    }

    /**
     * A vanilla fireball (type {@code minecraft:fireball}, so clients draw an ordinary fireball) flying like TC4's explosive orb: drag 0.99,
     * gravity 0.01, gone after 500 ticks. A plain fireball's 0.95 inertia would stop it two blocks from the sky. The subclass is server-side
     * only; a saved one reloads as a plain fireball.
     */
    public static final class MeteorOrb extends LargeFireball {
        public MeteorOrb(Level level, int strength) {
            super(EntityType.FIREBALL, level);
            this.accelerationPower = 0.0;
            CompoundTag tag = new CompoundTag();
            tag.putByte("ExplosionPower", (byte) strength);
            this.readAdditionalSaveData(tag);
        }

        @Override
        protected float getInertia() {
            return 0.99F;
        }

        @Override
        public void tick() {
            super.tick();
            if (!level().isClientSide) {
                setDeltaMovement(getDeltaMovement().add(0.0, -0.01, 0.0));
                if (tickCount > 500) {
                    discard();
                }
            }
        }

        /** Test hook: the explosion power set above. */
        public int strength() {
            CompoundTag tag = new CompoundTag();
            this.addAdditionalSaveData(tag);
            return tag.getByte("ExplosionPower");
        }
    }
}
