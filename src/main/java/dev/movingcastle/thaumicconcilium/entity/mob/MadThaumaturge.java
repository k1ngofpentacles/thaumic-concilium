package dev.movingcastle.thaumicconcilium.entity.mob;

import com.leclowndu93150.thaumaturge.content.entity.EntityCultist;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import dev.movingcastle.thaumicconcilium.item.PontifexRobe;
import dev.movingcastle.thaumicconcilium.item.ResearchHint;
import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import dev.movingcastle.thaumicconcilium.registry.TCSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import org.joml.Vector3f;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * The Mad Thaumaturge (original: {@code mobs.MadThaumaturge}). Hostile, swings a void hoe or shovel, leaps onto its target
 * and claws at it, and sometimes drops the notes it made while still sane ({@link ResearchHint}).
 */
public class MadThaumaturge extends Monster {
    private int rideAttackTime;

    public MadThaumaturge(EntityType<? extends MadThaumaturge> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.4)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ARMOR, 2.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        this.goalSelector.addGoal(6, new MoveThroughVillageGoal(this, 1.0, false, 4, () -> false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, ThaumaturgeEntity.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, EntityCultist.class, true));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(random.nextBoolean() ? TTItems.VOID_HOE.get() : TTItems.VOID_SHOVEL.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.05F);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data) {
        data = super.finalizeSpawn(level, difficulty, reason, data);
        this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        return data;
    }

    /**
     * Crimson Initiation (original: right-click with the Crimson dagger in the full Pontifex Robe, no research needed). Any item in
     * the tag {@code thaumicconcilium:initiation_tools} is the tool (user decision: the tag is empty until our own Crimson Dagger
     * is designed and joins it). With a tool and the robe the server turns this creature into a Crimson Knight, Cleric or Paladin
     * (a third each); any other item in hand only shows the blood particles; an empty hand does nothing.
     */
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> INITIATION_TOOLS = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, dev.movingcastle.thaumicconcilium.ThaumicConcilium.id("initiation_tools"));

    public static boolean initiationTool(ItemStack stack) {
        return stack.is(INITIATION_TOOLS);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (PontifexRobe.isFullSet(player) && initiationTool(held)) {
            if (this.level() instanceof ServerLevel server) {
                this.initiate(server);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        if (this.level().isClientSide()) {
            for (int i = 0; i < 4; i++) {
                this.level().addParticle(new DustParticleOptions(new Vector3f(0.7F, 0.0F, 0.05F), 1.0F),
                        this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0.0, 0.05, 0.0);
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    /** Replaces this creature with a Crimson Knight, Cleric or Paladin at random, finalised so it is equipped. */
    public net.minecraft.world.entity.Mob initiate(ServerLevel server) {
        net.minecraft.world.entity.Mob cultist = switch (this.random.nextInt(3)) {
            case 0 -> TTEntities.CULTIST_KNIGHT.get().create(server);
            case 1 -> TTEntities.CULTIST_CLERIC.get().create(server);
            default -> TCEntities.CRIMSON_PALADIN.get().create(server);
        };
        cultist.getPersistentData().putBoolean(LesserCrimsonPortal.NO_REPLACE, true);
        cultist.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
        net.neoforged.neoforge.event.EventHooks.finalizeMobSpawn(cultist, server, server.getCurrentDifficultyAt(this.blockPosition()),
                MobSpawnType.CONVERSION, null);
        server.addFreshEntity(cultist);
        server.playSound(null, this.getX(), this.getY(), this.getZ(), TTSounds.CRAFTFAIL.get(), net.minecraft.sounds.SoundSource.HOSTILE,
                0.5F, 1.0F);
        this.discard();
        return cultist;
    }

    /** Equips a Mad Thaumaturge that replaced a Brainy Zombie (original: addEquipment). */
    public void addEquipment() {
        this.populateDefaultEquipmentSlots(this.random, this.level().getCurrentDifficultyAt(this.blockPosition()));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        // Original: 9% a tick within 2 blocks, it leaps onto the creature that last hurt it.
        LivingEntity attacker = this.getLastHurtByMob();
        if (this.getVehicle() == null && attacker != null && attacker.getPassengers().isEmpty() && attacker.isAlive()
                && this.distanceToSqr(attacker) < 4.0 && this.random.nextInt(100) > 90) {
            this.startRiding(attacker, true);
        }
        Entity vehicle = this.getVehicle();
        if (vehicle != null && !vehicle.isAlive()) {
            this.stopRiding();
            vehicle = null;
        }
        if (vehicle != null && --this.rideAttackTime <= 0) {
            this.rideAttackTime = 10 + this.random.nextInt(10);
            this.doHurtTarget(vehicle);
            if (this.random.nextFloat() < 0.2F) {
                this.stopRiding();
            }
        }
    }

    /** Original: rides 1.5 blocks lower, clinging to its victim's back. */
    @Override
    public Vec3 getVehicleAttachmentPoint(Entity vehicle) {
        return super.getVehicleAttachmentPoint(vehicle).add(0.0, 1.5, 0.0);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        // Original: 20% a hint note (reflection 4/10, thick taint 2/10, windings 2/10, destabilized crystal 2/10),
        // otherwise 0-1 knowledge fragments plus looting.
        if (this.random.nextInt(10) > 7) {
            int r = this.random.nextInt(10);
            ResourceLocation hint = r <= 3 ? ResearchHint.REFLECTION : r <= 5 ? ResearchHint.THICK_TAINT
                    : r <= 7 ? ResearchHint.WINDINGS : ResearchHint.DEST_CRYSTAL;
            // The Destabilized Crystal arrives in phase 6; until then that roll drops nothing.
            if (ResearchHint.exists(level.registryAccess(), hint)) {
                this.spawnAtLocation(ResearchHint.stack(hint), 1.5F);
            }
        } else {
            int count = this.random.nextInt(2) + ThaumaturgeTrades.looting(level, source);
            if (count > 0) {
                this.spawnAtLocation(new ItemStack(TTItems.CURIO_KNOWLEDGE.get(), count), 1.5F);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return TCSounds.SAD.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return TCSounds.SAD.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 100;
    }

    @Override
    protected float getSoundVolume() {
        return 0.3F;
    }
}
