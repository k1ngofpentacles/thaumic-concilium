package dev.movingcastle.thaumicconcilium.entity.mob;

import com.leclowndu93150.thaumaturge.content.entity.EntityCultist;
import com.leclowndu93150.thaumaturge.content.entity.portal.CultistPortals;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The Lesser Crimson Portal (original: LesserPortal): an immobile 500 health rift in the air that, while a player is within 48 blocks,
 * pushes Crimson cultists through on a staged timer. It regenerates a point a tick until stage 12, burns itself for 5 to 9 per
 * arrival past stage 12, and at stage 13 (only if {@link #boss}) spends itself to release the Crimson Pontifex. Players within
 * three blocks take 8. It explodes (no fire, no blocks) when it dies.
 */
public class LesserCrimsonPortal extends Monster {
    public static final byte PULSE_EVENT = 16;
    public static final int FIRST_COUNTER = 200;
    public static final int BOSS_STAGE = 13;

    /** The five minions, 20% each (original spawnMinions). */
    public enum Minion { KNIGHT, CLERIC, PALADIN, ARCHER, RANGER }

    private int stage;
    private int counter = FIRST_COUNTER;
    private boolean boss;
    /** Client: ticks of the pulse flash left. */
    public int pulse;

    public LesserCrimsonPortal(EntityType<? extends LesserCrimsonPortal> type, Level level) {
        super(type, level);
        this.xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 500.0).add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    // ---------------------------------------------------------------- the staged cycle, as pure functions

    /** Ticks until the next stage fires after {@code stage} did, given Thaumcraft-style timing (20 per cultist near). Stage 13 sets none. */
    public static int nextCounter(int stage, int timing, RandomSource random) {
        if (stage <= 4) {
            return 15 + random.nextInt(10 - stage) - stage;
        }
        if (stage == 12) {
            return 50 + 2 * timing + random.nextInt(50);
        }
        if (stage == BOSS_STAGE) {
            return 0;
        }
        return timing + random.nextInt(5 + timing / 3);
    }

    /** True for every stage but 13, which is the boss stage. */
    public static boolean spawnsMinions(int stage) {
        return stage != BOSS_STAGE;
    }

    public static Minion minionFor(int roll) {
        return Minion.values()[Math.floorMod(roll, Minion.values().length)];
    }

    // ---------------------------------------------------------------- state

    public int stage() {
        return stage;
    }

    public void setStage(int stage) {
        this.stage = stage;
    }

    public int counter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public boolean isBoss() {
        return boss;
    }

    public void setBoss(boolean boss) {
        this.boss = boss;
    }

    /** Opens a portal at {@code pos} (also used by the Pontifex and the QA command). */
    @Nullable
    public static LesserCrimsonPortal summon(ServerLevel level, Vec3 pos, boolean boss) {
        LesserCrimsonPortal portal = TCEntities.LESSER_CRIMSON_PORTAL.get().create(level);
        if (portal == null) {
            return null;
        }
        portal.moveTo(pos.x, pos.y, pos.z, level.random.nextFloat() * 360.0F, 0.0F);
        portal.boss = boss;
        portal.finalizeSpawn(level, level.getCurrentDifficultyAt(portal.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        level.addFreshEntity(portal);
        return portal;
    }

    // ---------------------------------------------------------------- behaviour

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !isAlive()) {
            return;
        }
        if (stage < 12 && getHealth() < getMaxHealth()) {
            heal(1.0F);
        }
        if (tickCount % 4 == 0) {
            burnNearby();
        }
        if (--counter <= 0) {
            ServerLevel server = (ServerLevel) level();
            fire(server, server.getNearestPlayer(this, 48.0) != null);
        }
    }

    /** One firing of the cycle (public for the pack test). */
    public void fire(ServerLevel server, boolean playerNear) {
        if (!playerNear) {
            counter = 30 + random.nextInt(30);
            return;
        }
        int timing = CultistPortals.cultistsNear(this, 32.0) * 20;
        if (spawnsMinions(stage)) {
            server.broadcastEntityEvent(this, PULSE_EVENT);
            spawnMinion(server, minionFor(random.nextInt(5)));
        } else if (boss) {
            spawnBoss(server);
        }
        int next = nextCounter(stage, timing, random);
        if (stage != BOSS_STAGE) {
            counter = next;
        }
        stage++;
    }

    /** Players within three blocks take 8 (original: every tick; here every four, inside the hurt cooldown anyway). */
    private void burnNearby() {
        for (Player player : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(3.0),
                p -> !p.isSpectator() && !p.isCreative() && p.distanceToSqr(this) <= 9.0)) {
            if (player.hurt(damageSources().mobAttack(this), 8.0F)) {
                playSound(TTSounds.ZAP.get(), 1.0F, 1.0F + (random.nextFloat() - random.nextFloat()) * 0.1F);
            }
        }
    }

    /** Past stage 12 each arrival costs the portal 5 to 9 health. */
    public EntityCultist spawnMinion(ServerLevel server, Minion kind) {
        EntityCultist minion = switch (kind) {
            case KNIGHT -> TTEntities.CULTIST_KNIGHT.get().create(server);
            case CLERIC -> TTEntities.CULTIST_CLERIC.get().create(server);
            case PALADIN -> TCEntities.CRIMSON_PALADIN.get().create(server);
            case ARCHER -> TCEntities.CRIMSON_ARCHER.get().create(server);
            case RANGER -> TCEntities.CRIMSON_RANGER.get().create(server);
        };
        // keeps the world's 20% knight-to-Paladin replacement from skewing the portal's own odds
        minion.getPersistentData().putBoolean(NO_REPLACE, true);
        CultistPortals.summon(this, server, minion);
        minion.restrictTo(blockPosition(), 32);
        if (stage > 12) {
            hurt(damageSources().fellOutOfWorld(), 5 + random.nextInt(5));
        }
        return minion;
    }

    public static final String NO_REPLACE = "thaumicconcilium_portal_minion";

    /** Releases the Pontifex beside the portal and the portal ends itself. */
    public CrimsonPontifex spawnBoss(ServerLevel server) {
        CrimsonPontifex pontifex = TCEntities.CRIMSON_PONTIFEX.get().create(server);
        pontifex.moveTo(getX() + random.nextFloat() - random.nextFloat(), getY() + 0.25, getZ() + random.nextFloat() - random.nextFloat(),
                random.nextFloat() * 360.0F, 0.0F);
        pontifex.finalizeSpawn(server, server.getCurrentDifficultyAt(pontifex.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        server.addFreshEntity(pontifex);
        hurt(damageSources().genericKill(), getHealth());
        return pontifex;
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel server && !this.dead) {
            server.explode(this, getX(), getY(), getZ(), 2.0F, Level.ExplosionInteraction.NONE);
        }
        super.die(source);
    }

    @Override
    public void aiStep() {
        if (pulse > 0) {
            pulse--;
        }
    }

    @Override
    public void handleEntityEvent(byte event) {
        if (event == PULSE_EVENT) {
            pulse = 10;
        } else {
            super.handleEntityEvent(event);
        }
    }

    @Override
    public void move(MoverType type, Vec3 movement) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean addEffect(MobEffectInstance effect, @Nullable Entity source) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 540;
    }

    @Override
    protected float getSoundVolume() {
        return 0.75F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return TTSounds.MONOLITH.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return TTSounds.ZAP.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return TTSounds.SHOCK.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Stage", stage);
        tag.putInt("Counter", counter);
        tag.putBoolean("Boss", boss);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        stage = tag.getInt("Stage");
        counter = tag.contains("Counter") ? tag.getInt("Counter") : FIRST_COUNTER;
        boss = tag.getBoolean("Boss");
    }
}
