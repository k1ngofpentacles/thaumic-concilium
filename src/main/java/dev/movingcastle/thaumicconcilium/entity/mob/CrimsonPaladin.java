package dev.movingcastle.thaumicconcilium.entity.mob;

import com.leclowndu93150.thaumaturge.api.wands.WandCap;
import com.leclowndu93150.thaumaturge.content.wands.WandParts;
import com.leclowndu93150.thaumaturge.api.wands.WandRod;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.leclowndu93150.thaumaturge.registry.TTWandParts;
import dev.movingcastle.thaumicconcilium.entity.CrimsonOrb;
import dev.movingcastle.thaumicconcilium.entity.EtherealShackles;
import dev.movingcastle.thaumicconcilium.registry.TCItems;
import dev.movingcastle.thaumicconcilium.registry.TCSounds;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * The Crimson Paladin (original: CrimsonPaladin): 60 health, melee 8, and from at least eight blocks away a spell every one to two
 * seconds: either Ethereal Shackles (only at players: chain them for five seconds) or a Crimson Orb. Holds a greatwood staff with an
 * alchemical cap and wears the cultist plate. Never spawns on its own: 20% of Thaumaturge's Crimson Knights become Paladins.
 */
public class CrimsonPaladin extends CrimsonCultist implements RangedAttackMob {
    /** Original: AILongRangeAttack min 8.0. */
    private static final double MIN_RANGE_SQR = 64.0;

    public CrimsonPaladin(EntityType<? extends CrimsonPaladin> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return crimsonAttributes(60.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 20, 40, 24.0F) {
            @Override
            public boolean canUse() {
                return farEnough() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return farEnough() && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        addIdleGoals();
        addTargetGoals();
        targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, ThaumaturgeEntity.class, false));
    }

    private boolean farEnough() {
        LivingEntity target = getTarget();
        return target != null && distanceToSqr(target) >= MIN_RANGE_SQR;
    }

    /** The staff: a wand with the greatwood staff rod and, with Forbidden Magic, its alchemical cap. */
    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData groupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        ItemStack staff = staff();
        if (!staff.isEmpty()) {
            setItemSlot(EquipmentSlot.MAINHAND, staff);
        }
        if (gilded(random)) {
            setItemSlot(EquipmentSlot.HEAD, new ItemStack(TCItems.GILDED_CRIMSON_HELM.get()));
        }
        return data;
    }

    /** Half of the Paladins wear the Gilded Crimson Helm in place of the hood (the head slot keeps the cultist's 5% drop chance). */
    public static boolean gilded(net.minecraft.util.RandomSource random) {
        return random.nextBoolean();
    }

    public static ItemStack staff() {
        WandRod rod = TTWandParts.rods().get(ResourceLocation.fromNamespaceAndPath("thaumaturge", "greatwood_staff"));
        WandCap cap = null;
        for (ResourceLocation key : TTWandParts.caps().keySet()) {
            if (key.getPath().equals("alchemical")) {
                cap = TTWandParts.caps().get(key);
                break;
            }
        }
        if (cap == null) {
            cap = TTWandParts.caps().get(ResourceLocation.fromNamespaceAndPath("thaumaturge", "iron"));
        }
        ItemStack stack = new ItemStack(TTItems.WAND.get());
        if (rod != null && cap != null) {
            stack.set(TTDataComponents.WAND_PARTS.get(), new WandParts(cap, rod, false));
        }
        return stack;
    }

    /** The original's ranged attack: the spell sound, then half the time shackles at a player, half the time an orb. */
    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        server.playSound(null, getX(), getY(), getZ(), TCSounds.SPELL.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        if (random.nextInt(2) == 0) {
            if (target instanceof Player) {
                EtherealShackles shackles = new EtherealShackles(server, this);
                double dx = target.getX() + target.getDeltaMovement().x - getX();
                double dy = target.getY() - getY();
                double dz = target.getZ() + target.getDeltaMovement().z - getZ();
                shackles.shoot(dx, dy, dz, 2.0F, 2.0F);
                server.addFreshEntity(shackles);
                server.playSound(null, shackles.getX(), shackles.getY(), shackles.getZ(), TCSounds.SHACKLES_THROW.get(),
                        SoundSource.HOSTILE, 0.5F, 1.0F);
                swing(InteractionHand.MAIN_HAND);
            }
        } else {
            Vec3 look = target.getEyePosition().subtract(getEyePosition()).normalize();
            lookAt(target, 100.0F, 100.0F);
            CrimsonOrb.launch(server, this, new Vec3(getX() + look.x, getY() + getEyeHeight(), getZ() + look.z), look);
            swing(InteractionHand.MAIN_HAND);
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
