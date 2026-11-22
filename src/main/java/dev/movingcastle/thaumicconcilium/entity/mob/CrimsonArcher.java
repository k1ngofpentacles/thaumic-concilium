package dev.movingcastle.thaumicconcilium.entity.mob;

import dev.movingcastle.thaumicconcilium.entity.PrimalArrow;
import dev.movingcastle.thaumicconcilium.entity.PrimalType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.providers.VanillaEnchantmentProviders;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * The Crimson Archer (original: CrimsonArcher): 25 health, a bow in hand that shoots a primal arrow of a random kind every second
 * (2.5 velocity, 1.5 inaccuracy, damage 2 to 9 before speed), regenerating one health every two seconds. Never spawns on its own.
 */
public class CrimsonArcher extends CrimsonCultist {
    public CrimsonArcher(EntityType<? extends CrimsonArcher> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return crimsonAttributes(25.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new StrafeShootGoal(this, this::shoot, 20, 20.0F));
        addIdleGoals();
        addTargetGoals();
        targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, ThaumaturgeEntity.class, true));
    }

    /** Original: enchants the bow at 0.25 times the difficulty (Power, Punch and Flame then ride on its arrows). */
    @Override
    protected void populateDefaultEquipmentEnchantments(ServerLevelAccessor level, RandomSource random, DifficultyInstance difficulty) {
        ItemStack bow = getMainHandItem();
        if (!bow.isEmpty() && random.nextFloat() < 0.25F * difficulty.getSpecialMultiplier()) {
            EnchantmentHelper.enchantItemFromProvider(bow, level.registryAccess(), VanillaEnchantmentProviders.MOB_SPAWN_EQUIPMENT,
                    difficulty, random);
        }
    }

    /** Fires one primal arrow at the target (also called by the pack test). */
    public void shoot(LivingEntity target) {
        double dx = target.getX() - getX();
        double dy = target.getY(0.3333333333333333) - (getEyeY() - 0.1F);
        double dz = target.getZ() - getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        ItemStack bow = getMainHandItem();
        // the original asks for velocity 2.5 flat; the arrow's own 1.5 factor is divided out
        PrimalArrow arrow = PrimalArrow.shoot(level(), this, PrimalType.byIndex(random.nextInt(6)), dx, dy + flat * 0.2, dz,
                2.5F / PrimalArrow.SPEED_FACTOR, 1.5F, bow.isEmpty() ? null : bow);
        arrow.setBaseDamage(scaledDamage(2 + random.nextInt(8), dev.movingcastle.thaumicconcilium.TCConfig.ARCHER_DAMAGE_MULTIPLIER.get()));
        playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (random.nextFloat() * 0.4F + 0.8F));
    }

    /** The archer's base arrow damage after the server's multiplier (config {@code mobs.crimsonArcherDamageMultiplier}). */
    public static double scaledDamage(int base, double multiplier) {
        return base * multiplier;
    }

    /** Original: +1 health every 40 ticks. */
    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && tickCount % 40 == 0 && getHealth() < getMaxHealth() && isAlive()) {
            heal(1.0F);
        }
    }
}
