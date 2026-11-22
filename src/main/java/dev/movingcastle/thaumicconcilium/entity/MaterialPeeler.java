package dev.movingcastle.thaumicconcilium.entity;

import com.leclowndu93150.thaumaturge.registry.TTSounds;
import dev.movingcastle.thaumicconcilium.block.HexBlockEntity;
import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import dev.movingcastle.thaumicconcilium.rift.ConciliumRifts;
import dev.movingcastle.thaumicconcilium.world.CrimsonState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Peeler of Matter (original {@code MaterialPeeler}): an entity that never moves and that nothing can hurt. It must hang two
 * blocks above a Hex of Predictability's master: every 5 ticks it checks, and with no hex below it bursts, taints the ground around
 * it and is gone. A player touching it (every 20 ticks) loses their grip on matter: their ethereal state flips and they are sick for ten
 * seconds, unless the Pontifex's robe toggle is what holds them ethereal. It holds the nearby hex's rift down: a wand will not tear
 * a rift from a jarred node while one hangs in the 3x4x3 box above that hex.
 */
public class MaterialPeeler extends Entity {
    private static final EntityDataAccessor<Integer> ROTATION = SynchedEntityData.defineId(MaterialPeeler.class, EntityDataSerializers.INT);
    public static final int NAUSEA_TICKS = 200;
    public static final int NAUSEA_AMPLIFIER = 10;

    public MaterialPeeler(EntityType<? extends MaterialPeeler> type, Level level) {
        super(type, level);
        this.entityData.set(ROTATION, level.random.nextInt(360));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ROTATION, 0);
    }

    /** The random inner rotation seed (original {@code innerRotation}) the renderer spins from. */
    public int innerRotation() {
        return this.entityData.get(ROTATION);
    }

    /** Places a Peeler (at a hex's master, two blocks above it, when {@code hex} is known). */
    public static MaterialPeeler summon(ServerLevel level, Vec3 at, BlockPos hex) {
        MaterialPeeler peeler = new MaterialPeeler(TCEntities.MATERIAL_PEELER.get(), level);
        Vec3 pos = hex != null ? new Vec3(hex.getX() + 0.5, hex.getY() + 2.0, hex.getZ() + 0.5) : at;
        peeler.moveTo(pos.x, pos.y, pos.z, level.random.nextFloat() * 360.0F, 0.0F);
        level.addFreshEntity(peeler);
        level.sendParticles(ParticleTypes.PORTAL, pos.x, pos.y + 0.5, pos.z, 30, 0.5, 0.5, 0.5, 0.3);
        level.playSound(null, pos.x, pos.y, pos.z, TTSounds.CRAFTFAIL.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        return peeler;
    }

    /** Whether a master hex lies two blocks below this block position (the Peeler's anchor). */
    public static boolean hexBelow(Level level, BlockPos pos) {
        return level.getBlockEntity(pos.below(2)) instanceof HexBlockEntity hex && hex.isMaster();
    }

    /** The original's 3x4x3 box above a hex master, where a Peeler holds the rift down. */
    public static boolean peelerAbove(Level level, BlockPos hex) {
        return !level.getEntitiesOfClass(MaterialPeeler.class,
                new AABB(hex.getX() - 1, hex.getY(), hex.getZ() - 1, hex.getX() + 1, hex.getY() + 4, hex.getZ() + 1)).isEmpty();
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.tickCount % 300 == 0) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), TTSounds.EVILPORTAL.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        }
        if (this.tickCount % 5 == 0 && !hexBelow(level, this.blockPosition())) {
            collapse(level);
        }
    }

    /** No hex below: burst, a failing note, taint around, gone. */
    public void collapse(ServerLevel level) {
        level.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 0.5, this.getZ(), 40, 0.5, 0.5, 0.5, 0.4);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), TTSounds.CRAFTFAIL.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        ConciliumRifts.spreadTaint(level, this.position(), 1.5);
        this.discard();
    }

    @Override
    public void playerTouch(Player player) {
        if (this.tickCount % 20 == 0 && player instanceof ServerPlayer server) {
            touch(server);
        }
    }

    /** What touching does: flips the ethereal state and sickens, unless the robe's toggle holds the wearer. Returns whether it acted. */
    public boolean touch(ServerPlayer player) {
        CrimsonState.State s = CrimsonState.get(player);
        if (s.pontifexRobeToggle()) {
            return false;
        }
        CrimsonState.set(player, s.withEthereal(!s.ethereal()));
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NAUSEA_TICKS, NAUSEA_AMPLIFIER));
        return true;
    }

    @Override
    public void move(MoverType type, Vec3 movement) {
        // never moves
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("rotation")) {
            this.entityData.set(ROTATION, tag.getInt("rotation"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("rotation", this.innerRotation());
    }
}
