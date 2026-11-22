package dev.movingcastle.thaumicconcilium.entity.mob;

import dev.movingcastle.thaumicconcilium.entity.ShardPowder;
import dev.movingcastle.thaumicconcilium.item.ShardKind;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Crimson Ranger (original: CrimsonRanger): 30 health, no weapon and no melee; every second it sprays four to nine motes of
 * wrath or envy shard powder (our {@link ShardPowder}) toward its target. Spawns naturally (biome modifier).
 */
public class CrimsonRanger extends CrimsonCultist {
    public CrimsonRanger(EntityType<? extends CrimsonRanger> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return crimsonAttributes(30.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new StrafeShootGoal(this, this::spray, 20, 20.0F));
        addIdleGoals();
        addTargetGoals();
        targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, ThaumaturgeEntity.class, false));
    }

    /** Sprays the powder (also called by the pack test): 4 to 9 motes, wrath or envy, along the look with a small spread. */
    public int spray(LivingEntity target) {
        if (!(level() instanceof ServerLevel server)) {
            return 0;
        }
        playSound(SoundEvents.GLASS_BREAK, 0.3F, 0.8F + random.nextFloat() * 0.1F);
        Vec3 look = target.getEyePosition().subtract(getEyePosition()).normalize();
        int count = 4 + random.nextInt(6);
        ShardKind kind = random.nextBoolean() ? ShardKind.WRATH : ShardKind.ENVY;
        for (int i = 0; i < count; i++) {
            Vec3 motion = look.add(random.nextGaussian() * 0.09, random.nextGaussian() * 0.09, random.nextGaussian() * 0.09);
            ShardPowder.spray(server, this, getEyePosition().add(look.scale(0.5)), motion, kind, 0);
        }
        return count;
    }
}
