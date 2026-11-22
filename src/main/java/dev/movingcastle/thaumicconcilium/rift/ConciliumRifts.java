package dev.movingcastle.thaumicconcilium.rift;

import com.leclowndu93150.thaumaturge.api.taint.TaintApi;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode;
import com.leclowndu93150.thaumaturge.content.entity.EntityFluxRift;
import com.leclowndu93150.thaumaturge.content.entity.EntityMindSpider;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.ChargedWisp;
import dev.movingcastle.thaumicconcilium.entity.PrimalOrb;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Concilium's spatial rifts, on Thaumaturge's flux rifts (agreed with the user 2026-11-21). A rift Concilium opens
 * carries a tag in its persistent data and gets the original's behaviour on top of Thaumaturge's own:
 * <ul>
 *   <li>{@link Kind#SPATIAL}: torn open by breaking an aura node (original {@code RiftEntity.createRift}). It grows
 *   to a size set by the node's vis, rolls the original's event table every 15 seconds, and spreads taint as it
 *   closes. Its Thaumaturge stability is held at 0, so Thaumaturge's own flux events stay quiet and these replace them.</li>
 *   <li>{@link Kind#CHAINED}: made on a Hex of Predictability from a jarred node: tame, full-efficiency transmutation.</li>
 *   <li>{@link Kind#INTERDIMENSIONAL}: opened by a Gem of Rifts, see {@link Subspace}.</li>
 *   <li>{@link Kind#TAMED}: a native flux rift held by a lesser hex: tame, half-efficiency transmutation.</li>
 * </ul>
 * Tame rifts are pinned to "very stable" through Thaumaturge's own stability: no events, no growth, a harmless collapse.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class ConciliumRifts {
    public static final String TAG = ThaumicConcilium.MODID + ":rift";
    private static final float PINNED_STABLE = 100.0F;

    public enum Kind {
        SPATIAL, CHAINED, TAMED, INTERDIMENSIONAL;

        public boolean tame() {
            return this == CHAINED || this == TAMED;
        }
    }

    private ConciliumRifts() {
    }

    // ---------------------------------------------------------------- state
    @Nullable
    public static Kind kind(EntityFluxRift rift) {
        CompoundTag tag = rift.getPersistentData().getCompound(TAG);
        if (!tag.contains("kind")) {
            return null;
        }
        try {
            return Kind.valueOf(tag.getString("kind"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static CompoundTag data(EntityFluxRift rift) {
        CompoundTag root = rift.getPersistentData();
        if (!root.contains(TAG)) {
            root.put(TAG, new CompoundTag());
        }
        return root.getCompound(TAG);
    }

    public static void mark(EntityFluxRift rift, Kind kind, @Nullable BlockPos hex) {
        CompoundTag tag = data(rift);
        tag.putString("kind", kind.name());
        if (hex != null) {
            tag.putLong("hex", hex.asLong());
        }
    }

    /** Releases a tamed native rift back to Thaumaturge (its lesser hex is gone). */
    public static void release(EntityFluxRift rift) {
        rift.getPersistentData().remove(TAG);
    }

    @Nullable
    public static BlockPos hex(EntityFluxRift rift) {
        CompoundTag tag = data(rift);
        return tag.contains("hex") ? BlockPos.of(tag.getLong("hex")) : null;
    }

    public static void growTo(EntityFluxRift rift, int size) {
        CompoundTag tag = data(rift);
        tag.putInt("max", size);
        tag.putBoolean("growing", true);
    }

    /**
     * Shrinks the rift towards {@code target} (the Vis Conductor's dematerialization). At or below nothing it simply
     * collapses, quietly: like the original, overshooting the rift's size spares the land its taint.
     */
    public static void dematerialize(EntityFluxRift rift, int target) {
        if (kind(rift) == null) {
            // A native flux rift: Thaumaturge's own, so shrink it at once (its collapse is Thaumaturge's).
            if (target <= 1) {
                rift.beginCollapse();
            } else if (target < rift.currentSize()) {
                rift.resize(target);
            }
            return;
        }
        CompoundTag tag = data(rift);
        if (target <= 1) {
            tag.putBoolean("quiet", true);
            rift.beginCollapse();
        } else if (target < rift.currentSize()) {
            tag.putInt("max", target);
            tag.putBoolean("growing", false);
            tag.putBoolean("shrinking", true);
        }
    }

    // ---------------------------------------------------------------- opening
    /** Original {@code createRift}: half the node's vis, clamped 5-80; under 5 nothing opens. */
    @Nullable
    public static EntityFluxRift openSpatial(ServerLevel level, Vec3 at, int nodeVis) {
        int amount = nodeVis / 2;
        if (amount < 5 || level.random.nextInt(100) >= TCConfig.RIFT_CHANCE.get()) {
            return null;
        }
        EntityFluxRift rift = TTEntities.FLUX_RIFT.get().create(level);
        if (rift == null) {
            return null;
        }
        rift.reseed(level.random.nextInt());
        rift.moveTo(at.x, at.y, at.z, level.random.nextInt(360), 0.0F);
        rift.resize(5);
        if (!level.addFreshEntity(rift)) {
            return null;
        }
        mark(rift, Kind.SPATIAL, null);
        growTo(rift, Mth.clamp(amount, 5, 80));
        return rift;
    }

    @SubscribeEvent
    public static void onNodeBroken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !event.getState().is(TTBlocks.NODE.get())
                || !(level.getBlockEntity(event.getPos()) instanceof BlockEntityNode node)) {
            return;
        }
        openSpatial(level, Vec3.atCenterOf(event.getPos()), node.getAspects().totalAmount());
    }

    // ---------------------------------------------------------------- ticking
    /**
     * Interdimensional rifts (the Gem of Rifts') run their own life ({@link Subspace}): Thaumaturge's tick is cancelled on the
     * server, so its block-eating sweep, damage, events and collapse loot never happen. The client keeps ticking (it only refreshes the outline).
     */
    @SubscribeEvent
    public static void onRiftTickPre(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof EntityFluxRift rift && rift.level() instanceof ServerLevel level && !rift.isRemoved()
                && kind(rift) == Kind.INTERDIMENSIONAL) {
            event.setCanceled(true);
            Subspace.tick(level, rift);
        }
    }

    @SubscribeEvent
    public static void onRiftTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof EntityFluxRift rift) || !(rift.level() instanceof ServerLevel level) || rift.isRemoved()) {
            return;
        }
        Kind kind = kind(rift);
        if (kind == null) {
            return;
        }
        if (kind == Kind.CHAINED && rift.tickCount % 5 == 0 && !crimsonOffering(level, rift)) {
            peelerOffering(level, rift);
        }
        if (kind.tame()) {
            if (rift.stabilityValue() < PINNED_STABLE) {
                rift.adjustStability(PINNED_STABLE - rift.stabilityValue());
            }
            if (rift.tickCount % 20 == 0) {
                Hexes.checkRift(level, rift, kind);
            }
            sizeStep(rift);
            return;
        }
        // Spatial: Thaumaturge's own events need negative stability; keep it at 0 and run the original's instead.
        if (rift.stabilityValue() != 0.0F) {
            rift.adjustStability(-rift.stabilityValue());
        }
        sizeStep(rift);
        if (rift.isCollapsing()) {
            if (!data(rift).getBoolean("quiet") && level.random.nextInt(3) == 0) {
                spreadTaint(level, rift);
            }
            return;
        }
        if (rift.tickCount > 0 && rift.tickCount % 300 == 0) {
            fireEvent(level, rift);
        }
        // Original: a rift that grew past 300 collapses to half.
        if (rift.currentSize() > 300) {
            dematerialize(rift, rift.currentSize() / 2);
        }
    }

    /**
     * Original RiftEntity:391 and :486: a Crimson Rites (Thaumaturge's {@code curio_rites}, for the original's eldritch object meta 1)
     * thrown into a chained rift by a player with the Crimson Pontifex research is eaten, and the rift opens the boss portal.
     * Runs every 5 ticks on chained rifts only.
     */
    public static boolean crimsonOffering(ServerLevel level, EntityFluxRift rift) {
        for (net.minecraft.world.entity.item.ItemEntity item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                rift.getBoundingBox().inflate(0.5), e -> e.isAlive() && e.getItem().is(com.leclowndu93150.thaumaturge.registry.TTItems.CURIO_RITES.get()))) {
            if (item.getOwner() instanceof net.minecraft.world.entity.player.Player thrower && com.leclowndu93150.thaumaturge.api.capability.PlayerResearchAccess
                    .of(thrower).hasCompleted(Subspace.PONTIFEX_RESEARCH)) {
                item.discard();
                Subspace.summonPontifexPortal(rift);
                return true;
            }
        }
        return false;
    }

    /**
     * Original RiftEntity:397 and :500: a Primordial Life thrown into a chained rift by a player with the Material Peeler research is
     * eaten, and the rift collapses at once and leaves a Peeler of Matter where it was. Runs every 5 ticks on chained rifts only.
     */
    public static boolean peelerOffering(ServerLevel level, EntityFluxRift rift) {
        for (net.minecraft.world.entity.item.ItemEntity item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                rift.getBoundingBox().inflate(0.5), e -> e.isAlive() && e.getItem().is(dev.movingcastle.thaumicconcilium.registry.TCItems.PRIMORDIAL_LIFE.get()))) {
            if (item.getOwner() instanceof net.minecraft.world.entity.player.Player thrower && com.leclowndu93150.thaumaturge.api.capability.PlayerResearchAccess
                    .of(thrower).hasCompleted(Subspace.PEELER_RESEARCH)) {
                item.getItem().shrink(1);
                if (item.getItem().isEmpty()) {
                    item.discard();
                }
                Subspace.summonPeeler(rift);
                return true;
            }
        }
        return false;
    }

    /** Original growth: one size step a third of the time towards the target; shrinking a step a tick. */
    private static void sizeStep(EntityFluxRift rift) {
        CompoundTag tag = data(rift);
        if (rift.isCollapsing()) {
            return;
        }
        int size = rift.currentSize();
        int max = tag.getInt("max");
        if (tag.getBoolean("growing")) {
            if (size < max) {
                if (rift.getRandom().nextInt(3) == 0) {
                    rift.resize(size + 1);
                }
            } else {
                tag.putBoolean("growing", false);
            }
        } else if (tag.getBoolean("shrinking")) {
            if (size > max) {
                rift.resize(size - 1);
            } else {
                tag.putBoolean("shrinking", false);
            }
        }
    }

    /** Original collapse: taint fibres, flux gas or goo around the rift (no taint biome here: ecological pressure). */
    private static void spreadTaint(ServerLevel level, EntityFluxRift rift) {
        spreadTaint(level, rift.position(), 1.5);
    }

    /** The same spread around any point (the Peeler of Matter's failing burst uses it too). */
    public static void spreadTaint(ServerLevel level, Vec3 at, double spread) {
        RandomSource random = level.random;
        int x = (int) (at.x + random.nextGaussian() * spread);
        int y = (int) at.y;
        int z = (int) (at.z + random.nextGaussian() * spread);
        for (int a = 0; a < 10; a++) {
            if (!random.nextBoolean()) {
                continue;
            }
            BlockPos pos = new BlockPos(x + (int) ((random.nextFloat() - random.nextFloat()) * 5.0F), y,
                    z + (int) ((random.nextFloat() - random.nextFloat()) * 5.0F));
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            TaintApi.addEcologicalPressure(level, pos, 0.04F);
            if (level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP) && level.getBlockState(pos).canBeReplaced()) {
                TaintApi.spreadFibres(level, pos, true);
            }
            if (level.getBlockState(pos.above()).isAir()) {
                level.setBlock(pos.above(), (random.nextBoolean() ? TTBlocks.FLUX_GAS.get() : TTBlocks.FLUX_GOO.get()).defaultBlockState(), 3);
            }
        }
    }

    // ---------------------------------------------------------------- the original's event table (fireEvent)
    private static void fireEvent(ServerLevel level, EntityFluxRift rift) {
        RandomSource random = level.random;
        AABB around = rift.getBoundingBox().inflate(7.0);
        switch (random.nextInt(10)) {
            case 0 -> {
                // Warp feeds it: grows by the warp of every player nearby.
                for (Player p : level.getEntitiesOfClass(Player.class, around)) {
                    CompoundTag tag = data(rift);
                    tag.putInt("max", Math.max(tag.getInt("max"), rift.currentSize()) + (int) Mth.clamp(WarpHelper.getActualWarp(p), 2, 10));
                    tag.putBoolean("growing", true);
                }
            }
            case 1 -> {
                // Primal orbs at whatever lives nearby.
                List<LivingEntity> list = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, around));
                if (!list.isEmpty()) {
                    int amount = Mth.clamp(rift.currentSize() % 11, 3, 10);
                    for (int i = 0; i < amount; i++) {
                        LivingEntity target = list.get(random.nextInt(list.size()));
                        Vec3 from = rift.position().add(random.nextInt(6) - 3, random.nextInt(6) - 3, random.nextInt(6) - 3);
                        level.addFreshEntity(PrimalOrb.aimed(level, from, target.position()));
                    }
                }
            }
            case 2 -> {
                // Lashes out: magic damage 4 + d(size, 5-30) to everything nearby.
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, around)) {
                    arc(level, rift.position(), e);
                    e.hurt(level.damageSources().magic(), 4 + random.nextInt(Mth.clamp(rift.currentSize(), 5, 30)));
                }
            }
            case 3 -> {
                // Feeds on a player's warp and lashes them.
                for (Player p : level.getEntitiesOfClass(Player.class, around)) {
                    rift.resize(rift.currentSize() + (int) Mth.clamp(WarpHelper.getActualWarp(p), 2, 10));
                    arc(level, rift.position(), p);
                    p.hurt(level.damageSources().magic(), 4 + random.nextInt(Mth.clamp(rift.currentSize(), 5, 30)));
                }
            }
            case 4 -> mindSpiders(level, rift, around);
            case 5 -> {
                // Charged wisps of random aspects at whatever lives nearby.
                List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, around);
                if (!list.isEmpty()) {
                    List<String> aspects = new ArrayList<>();
                    level.registryAccess().registry(com.leclowndu93150.thaumaturge.api.aspect.IAspect.REGISTRY_KEY)
                            .ifPresent(r -> r.keySet().forEach(k -> aspects.add(k.toString())));
                    int amount = Mth.clamp(rift.currentSize() % 11, 3, 10);
                    for (int i = 0; i < amount && !aspects.isEmpty(); i++) {
                        LivingEntity e = list.get(random.nextInt(list.size()));
                        Vec3 motion = new Vec3(e.getX() + e.getDeltaMovement().x - rift.getX(),
                                e.getY() - rift.getY() - e.getBbHeight() / 2.0, e.getZ() + e.getDeltaMovement().z - rift.getZ());
                        level.addFreshEntity(ChargedWisp.thrown(level, rift.position(), motion.normalize().scale(0.5),
                                aspects.get(random.nextInt(aspects.size()))));
                    }
                }
            }
            case 6 -> {
                if (random.nextInt(100) > 60) {
                    rift.beginCollapse();
                }
            }
            default -> {
            }
        }
    }

    /** Original case 4: up to 50 mind spiders (one per size) around the rift, set on a nearby player. */
    private static void mindSpiders(ServerLevel level, EntityFluxRift rift, AABB around) {
        RandomSource random = level.random;
        List<Player> players = level.getEntitiesOfClass(Player.class, around);
        int spawns = Math.min(50, rift.currentSize());
        BlockPos c = rift.blockPosition();
        for (int a = 0; a < spawns; a++) {
            EntityMindSpider spider = TTEntities.MIND_SPIDER.get().create(level);
            if (spider == null) {
                return;
            }
            boolean placed = false;
            for (int l = 0; l < 50 && !placed; l++) {
                BlockPos p = c.offset(Mth.nextInt(random, 7, 24) * Mth.nextInt(random, -1, 1),
                        Mth.nextInt(random, 7, 24) * Mth.nextInt(random, -1, 1), Mth.nextInt(random, 7, 24) * Mth.nextInt(random, -1, 1));
                if (level.hasChunkAt(p) && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) {
                    spider.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
                    placed = level.noCollision(spider) && !level.containsAnyLiquid(spider.getBoundingBox());
                }
            }
            if (placed) {
                if (!players.isEmpty()) {
                    spider.setTarget(players.get(random.nextInt(players.size())));
                }
                level.addFreshEntity(spider);
            }
        }
    }

    /** A purple arc from the rift to a victim (the original's lightning effect). */
    static void arc(ServerLevel level, Vec3 from, LivingEntity to) {
        Vec3 end = to.position().add(0.0, to.getBbHeight() / 2.0, 0.0);
        for (int i = 0; i <= 8; i++) {
            Vec3 p = from.lerp(end, i / 8.0);
            level.sendParticles(ParticleTypes.WITCH, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
        level.playSound(null, to.getX(), to.getY(), to.getZ(), TTSounds.SHOCK.get(), SoundSource.HOSTILE, 0.5F, 1.0F);
    }

    static List<EntityFluxRift> riftsIn(ServerLevel level, AABB box) {
        List<EntityFluxRift> list = new ArrayList<>(level.getEntitiesOfClass(EntityFluxRift.class, box, r -> !r.isRemoved()));
        Collections.shuffle(list);
        return list;
    }
}
