package dev.movingcastle.thaumicconcilium.entity.mob;

import com.leclowndu93150.thaumaturge.registry.TTItems;
import dev.movingcastle.thaumicconcilium.registry.TCSounds;
import dev.movingcastle.thaumicconcilium.world.Enslave;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Vengeful Golem (original: {@code mobs.VengefulGolem}). It never closes in to strike: from up to 24 blocks it rings its
 * bell and, four times in five when it can see the player (and they wear nothing of the {@code muffles_bells} tag on the head), enslaves them ({@link Enslave}: their
 * body walks toward it). Other mobs it only sends wandering.
 */
public class VengefulGolem extends Monster implements RangedAttackMob {
    /** Head items that muffle the bell. PROVISIONAL(R6): our own Earmuffs item waits on the user, so the tag is empty for now. */
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> MUFFLES_BELLS = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, ResourceLocation.fromNamespaceAndPath("thaumicconcilium", "muffles_bells"));

    public VengefulGolem(EntityType<? extends VengefulGolem> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0);
    }

    /** TC4's AILongRangeAttack: the arrow-attack goal that refuses while the target is inside the minimum distance. */
    private static final class LongRangeGoal extends RangedAttackGoal {
        private final Mob mob;
        private final double minSqr;

        LongRangeGoal(VengefulGolem golem, double min, double speed, int minInterval, int maxInterval, float range) {
            super(golem, speed, minInterval, maxInterval, range);
            this.mob = golem;
            this.minSqr = min * min;
        }

        @Override
        public boolean canUse() {
            if (!super.canUse()) {
                return false;
            }
            LivingEntity target = mob.getTarget();
            return target != null && mob.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ()) >= minSqr;
        }
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new LongRangeGoal(this, 1.0, 0.5, 20, 40, 24.0F));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.4));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, ThaumaturgeEntity.class, true));
    }

    /** Always holds a golem bell; never drops it. */
    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(TTItems.GOLEM_BELL.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @javax.annotation.Nullable
    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
            DifficultyInstance difficulty, net.minecraft.world.entity.MobSpawnType reason,
            @javax.annotation.Nullable net.minecraft.world.entity.SpawnGroupData data) {
        data = super.finalizeSpawn(level, difficulty, reason, data);
        populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        return data;
    }

    @Override
    public void aiStep() {
        if (getTarget() != null) {
            lookAt(getTarget(), 100.0F, 100.0F);
        }
        super.aiStep();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return TCSounds.GNAW.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return TCSounds.GNAW.get();
    }

    /** Whether the target wears an item of the {@code thaumicconcilium:muffles_bells} tag on the head (PROVISIONAL(R6): the tag is empty). */
    public static boolean muffed(LivingEntity target) {
        ItemStack head = target.getItemBySlot(EquipmentSlot.HEAD);
        return !head.isEmpty() && head.is(MUFFLES_BELLS);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (target instanceof Player player) {
            boolean seen = !muffed(player) && random.nextInt(10) >= 2 && hasLineOfSight(player);
            if (seen) {
                swing(InteractionHand.MAIN_HAND);
                level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.HOSTILE, 0.7F, 1.0F + level().random.nextFloat() * 0.1F);
                if (random.nextInt(10) > 5) {
                    player.sendSystemMessage(Component.translatable("thaumicconcilium.golem.taunt." + random.nextInt(4))
                            .withStyle(ChatFormatting.DARK_PURPLE));
                }
            }
            if (player instanceof ServerPlayer sp) {
                Enslave.set(sp, seen);
            }
        } else if (target instanceof Mob mob && level().random.nextFloat() > 0.1F) {
            double x = mob.getX() + (-0.5 + level().random.nextDouble()) * 20.0;
            double y = mob.getY() + (-0.5 + level().random.nextDouble()) * 2.0;
            double z = mob.getZ() + (-0.5 + level().random.nextDouble()) * 20.0;
            mob.getNavigation().moveTo(x, y, z, 0.7);
        }
    }
}
