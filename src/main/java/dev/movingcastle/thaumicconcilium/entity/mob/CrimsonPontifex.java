package dev.movingcastle.thaumicconcilium.entity.mob;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import com.leclowndu93150.thaumaturge.content.entity.EntityCultist;
import dev.movingcastle.thaumicconcilium.entity.ConcentratedWarpCharge;
import dev.movingcastle.thaumicconcilium.registry.TCSounds;
import dev.movingcastle.thaumicconcilium.world.Enslave;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * The Crimson Pontifex (original: CrimsonPontifex): 150 health, melee 25, a boss with a purple bar that darkens the sky. Only a Lesser
 * Crimson Portal at stage 13 releases it. Every 20 to 40 ticks of fighting a target within 16 blocks it rolls one of: a concentrated
 * warp charge on a player, enslaving a player (they walk blindly toward it), opening another portal, or draining the life of everything
 * non-cultist within six blocks. Cultists near it are regenerated and split the damage it takes (damage divided by cultists + 3).
 * Never hurt for more than 35 in a blow, and a blow that big (after the split) enrages it.
 */
public class CrimsonPontifex extends CrimsonCultist {
    public static final String[] TITLES = {"Ivius", "Ufarihm", "Ihith", "Pemonar", "Shagron", "Ugimaex", "Qroleus", "Oxon", "Rheforn", "Zubras"};
    public static final int ATTACK_ROLL = 20;

    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
    { this.bossEvent.setDarkenScreen(true); } // original: BossStatus.setBossStatus(entity, true), which darkens the sky

    private int attackCounter;
    private int anger;

    public CrimsonPontifex(EntityType<? extends CrimsonPontifex> type, Level level) {
        super(type, level);
        this.xpReward = 40;
    }

    /** Original: speed 0.32, health 150, attack 25 (follow range 32 from the cultist base). */
    public static AttributeSupplier.Builder createAttributes() {
        return crimsonAttributes(150.0).add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.ATTACK_DAMAGE, 25.0);
    }

    public ServerBossEvent bossEvent() {
        return bossEvent;
    }

    public int attackCounter() {
        return attackCounter;
    }

    public void setAttackCounter(int counter) {
        this.attackCounter = counter;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 0.7, false));
        addIdleGoals();
        addTargetGoals();
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData groupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        setPersistenceRequired();
        setCustomName(Component.literal("Pontifex " + TITLES[random.nextInt(TITLES.length)]));
        return data;
    }

    // ---------------------------------------------------------------- boss bar

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        bossEvent.setName(getDisplayName());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void remove(RemovalReason reason) {
        bossEvent.removeAllPlayers();
        super.remove(reason);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // ---------------------------------------------------------------- the fight

    public List<EntityCultist> cultistsNear() {
        return level().getEntitiesOfClass(EntityCultist.class, getBoundingBox().inflate(8.0), e -> e != this && e.isAlive());
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (anger > 0) {
            anger--;
        }
        if (tickCount % 10 == 0) {
            for (EntityCultist cultist : cultistsNear()) {
                if (!cultist.hasEffect(MobEffects.REGENERATION)) {
                    cultist.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1));
                }
            }
        }
        if (random.nextInt(10) > 7) {
            Player nearest = level().getNearestPlayer(this, 32.0);
            if (nearest != null) {
                setTarget(nearest);
            }
        }
        LivingEntity target = getTarget();
        if (target != null && distanceToSqr(target) <= 256.0 && hasLineOfSight(target)) {
            attackCounter++;
            if (attackCounter > 10 && target instanceof ServerPlayer player) {
                Enslave.set(player, false);
            }
            if (attackCounter == ATTACK_ROLL) {
                attackRoll(random.nextInt(4), target);
                attackCounter = -20 + random.nextInt(20);
            }
        }
    }

    /** The four attacks (public for the pack test): 0 warp charge, 1 enslave, 2 portal, 3 life drain. */
    public void attackRoll(int roll, LivingEntity target) {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        switch (roll) {
            case 0 -> {
                if (target instanceof ServerPlayer player && server.getEntitiesOfClass(ConcentratedWarpCharge.class,
                        player.getBoundingBox().inflate(7.0), c -> c.isOwnedBy(player)).isEmpty()) {
                    ConcentratedWarpCharge charge = ConcentratedWarpCharge.summon(server, player, 1, 0, ConcentratedWarpCharge.MODE_NONE);
                    charge.setPos(player.getX() + random.nextInt(5) - 2, player.getY(), player.getZ() + random.nextInt(5) - 2);
                }
            }
            case 1 -> {
                if (target instanceof ServerPlayer player) {
                    Enslave.set(player, true);
                }
            }
            case 2 -> {
                if (server.getEntitiesOfClass(LesserCrimsonPortal.class, getBoundingBox().inflate(64.0)).size() < 3) {
                    BlockPos at = BlockPos.containing(getX() + random.nextInt(9) - 4, getY(), getZ() + random.nextInt(9) - 4);
                    if (server.getBlockState(at).isAir() && server.getBlockState(at.above()).isAir()) {
                        LesserCrimsonPortal.summon(server, at.getBottomCenter(), false);
                    }
                }
            }
            default -> {
                if (!cultistsNear().isEmpty()) {
                    for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(6.0),
                            e -> e != this && e.isAlive() && !(e instanceof EntityCultist))) {
                        if (victim.hurt(damageSources().mobAttack(this), 25.0F)) {
                            heal(10.0F);
                        }
                    }
                }
            }
        }
    }

    /** A hurt by a creature turns the Pontifex on it; the blow is shared out among cultists near (cultists + 3). */
    @Override
    public boolean hurt(DamageSource source, float damage) {
        if (!level().isClientSide()) {
            if (source.getEntity() instanceof LivingEntity attacker && attacker != this && !(attacker instanceof EntityCultist)) {
                setTarget(attacker);
            }
            damage /= cultistsNear().size() + 3;
            if (damage > 35.0F) {
                if (anger == 0) {
                    addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, (int) (damage / 15.0F)));
                    addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, (int) (damage / 40.0F)));
                    addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, (int) (damage / 40.0F)));
                    anger = 200;
                    if (source.getEntity() instanceof Player player) {
                        player.sendSystemMessage(Component.translatable("message.thaumicconcilium.pontifex_enrage", getDisplayName()));
                    }
                }
                damage = 35.0F;
            }
        }
        return super.hurt(source, damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        bossEvent.setName(getDisplayName());
    }

    /**
     * Original dropFewItems: one research note by nextInt(10): 0-3 the Witchery spells note (or the Peeler's without Witchery), 4-5 the
     * hammer, 6-8 the Peeler, 9 the robe. A note whose research entry is not in the pack yet is not dropped.
     */
    public static net.minecraft.resources.ResourceLocation noteFor(int roll, boolean witcheryLoaded) {
        if (roll <= 3) {
            return witcheryLoaded ? ThaumicConcilium.id("crimson_spells") : dev.movingcastle.thaumicconcilium.item.ResearchHint.MATERIAL_PEELER;
        }
        if (roll <= 5) {
            return dev.movingcastle.thaumicconcilium.item.ResearchHint.PONTIFEX_HAMMER;
        }
        return roll <= 8 ? dev.movingcastle.thaumicconcilium.item.ResearchHint.MATERIAL_PEELER
                : dev.movingcastle.thaumicconcilium.item.ResearchHint.PONTIFEX_ROBE;
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        net.minecraft.resources.ResourceLocation note = noteFor(random.nextInt(10), net.neoforged.fml.ModList.get().isLoaded("witchery"));
        if (dev.movingcastle.thaumicconcilium.item.ResearchHint.exists(level.registryAccess(), note)) {
            spawnAtLocation(dev.movingcastle.thaumicconcilium.item.ResearchHint.stack(note), getBbHeight() / 2.0F);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return TCSounds.CHANT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return TCSounds.GROWL.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 450;
    }
}
