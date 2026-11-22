package dev.movingcastle.thaumicconcilium.rift;

import com.leclowndu93150.thaumaturge.api.capability.PlayerResearchAccess;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.content.entity.EntityFluxRift;
import com.leclowndu93150.thaumaturge.content.wands.ItemWand;
import com.leclowndu93150.thaumaturge.content.wands.WandVisHelper;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.block.HexBlockEntity;
import dev.movingcastle.thaumicconcilium.item.CastingBracelet;
import dev.movingcastle.thaumicconcilium.recipe.RiftRecipe;
import dev.movingcastle.thaumicconcilium.registry.TCBlocks;
import dev.movingcastle.thaumicconcilium.registry.TCItems;
import dev.movingcastle.thaumicconcilium.registry.TCRecipes;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The Hex of Predictability (original: {@code HexOfPredictabilityBlock}, {@code HexOfPredictabilityTile},
 * {@code WandHandler}).
 * <ul>
 *   <li>Eight ancient stone around a bedrock block, struck with a wand (100 ordo and 100 terra vis, the Hex research):
 *   the <b>Hex</b>. A jarred node on its centre, struck with a wand, opens a <b>chained rift</b> above it.</li>
 *   <li>Port addition (agreed 2026-11-21): the same ring around a Barrier Stone makes a <b>lesser hex</b> (50 and 50),
 *   which tames a native flux rift above it: pinned very stable, reined in to size 25, transmuting at half efficiency.</li>
 * </ul>
 * Transmutation follows the original: Vis Condensers send a destabilized crystal's essentia into the hex (heat 1200).
 * With exactly one thrown item lying on it, the heat runs down 3 a tick, shedding void slag now and then; at zero the
 * item becomes the rift recipe's result, if the thrower knows the recipe's research and the essentia covers it (twice
 * over for a tame native rift), and the essentia is spent. Void slag lying nearby keeps the heat up; at 2400 the rift
 * collapses.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class Hexes {
    public static final ResourceLocation RESEARCH = ThaumicConcilium.id("hex_of_predictability");
    static final int CHAINED_SIZE = 25;
    private static final double CHAINED_HEIGHT = 3.5;
    private static boolean unmaking;

    private Hexes() {
    }

    // ---------------------------------------------------------------- forming
    private static boolean ring(Level level, BlockPos centre) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if ((dx != 0 || dz != 0) && !level.getBlockState(centre.offset(dx, 0, dz)).is(TTBlocks.STONE_ANCIENT.get())) {
                    return false;
                }
            }
        }
        // Original checkStructure: no other hex within two blocks.
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-2, 0, -2), centre.offset(2, 0, 2))) {
            if (level.getBlockState(p).is(TCBlocks.HEX.get())) {
                return false;
            }
        }
        return true;
    }

    /** A wand, not a casting bracelet (the Vessel's anchors take the same test). */
    public static boolean isWand(ItemStack stack) {
        return wand(stack);
    }

    private static boolean wand(ItemStack stack) {
        return stack.getItem() instanceof ItemWand && !(stack.getItem() instanceof CastingBracelet);
    }

    private static Map<ResourceKey<IAspect>, Integer> cost(int vis) {
        return Map.of(key("ordo"), vis * 100, key("terra"), vis * 100);
    }

    private static ResourceKey<IAspect> key(String aspect) {
        return ResourceKey.create(IAspect.REGISTRY_KEY, ResourceLocation.fromNamespaceAndPath("thaumaturge", aspect));
    }

    @SubscribeEvent
    public static void onWand(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!wand(stack) || !(event.getLevel() instanceof ServerLevel level) || !(player instanceof ServerPlayer sp)) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (state.is(TTBlocks.JAR_NODE.get()) && level.getBlockEntity(pos.below()) instanceof HexBlockEntity hex
                && hex.isMaster() && !hex.isLesser() && !hex.hasRift()) {
            // original WandHandler:63-77: a Peeler of Matter in the 3x4x3 box above the hex holds the rift down
            if (!dev.movingcastle.thaumicconcilium.entity.MaterialPeeler.peelerAbove(level, pos.below())) {
                chain(level, pos, pos.below());
            }
            done(event);
            return;
        }
        boolean full = state.is(Blocks.BEDROCK);
        // The lesser hex needs a Barrier Stone (Thaumaturge's warding paving stone) at its centre: bedrock for the full
        // hex, a ward for the lesser one.
        if ((full || state.is(TTBlocks.PAVING_STONE_BARRIER.get())) && ring(level, pos)) {
            if (!PlayerResearchAccess.of(sp).hasCompleted(RESEARCH)) {
                return;
            }
            if (!WandVisHelper.payCosts(stack, sp, cost(full ? 100 : 50), true, true)) {
                sp.displayClientMessage(Component.translatable("message.thaumicconcilium.hex_no_vis", full ? 100 : 50), true);
                done(event);
                return;
            }
            form(level, pos, !full);
            done(event);
        }
    }

    private static void done(PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /** Turns the ring and its centre into hex blocks, each remembering what it replaced. */
    public static void form(ServerLevel level, BlockPos centre, boolean lesser) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = centre.offset(dx, 0, dz);
                BlockState old = level.getBlockState(p);
                level.setBlock(p, TCBlocks.HEX.get().defaultBlockState(), 3);
                if (level.getBlockEntity(p) instanceof HexBlockEntity hex) {
                    hex.setup(dx == 0 && dz == 0, lesser, old);
                }
                level.sendParticles(ParticleTypes.ENCHANT, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, 12, 0.3, 0.1, 0.3, 0.5);
            }
        }
        level.playSound(null, centre, TTSounds.WAND.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    /** Original constructRift: the jarred node is unmade into a young rift (size 5, growing to 25) above the hex. */
    public static void chain(ServerLevel level, BlockPos jar, BlockPos hexPos) {
        level.removeBlock(jar, false);
        EntityFluxRift rift = TTEntities.FLUX_RIFT.get().create(level);
        if (rift == null) {
            return;
        }
        rift.reseed(level.random.nextInt());
        rift.moveTo(hexPos.getX() + 0.5, hexPos.getY() + CHAINED_HEIGHT, hexPos.getZ() + 0.5, level.random.nextInt(360), 0.0F);
        rift.resize(5);
        if (level.addFreshEntity(rift)) {
            ConciliumRifts.mark(rift, ConciliumRifts.Kind.CHAINED, hexPos);
            ConciliumRifts.growTo(rift, CHAINED_SIZE);
        }
        level.playSound(null, jar, TTSounds.CRAFTFAIL.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    /** Unmakes the hex around a broken block: every hex block goes back to what it replaced. */
    public static void unmake(Level level, BlockPos broken) {
        if (unmaking) {
            return;
        }
        BlockPos master = null;
        for (int dx = -1; dx <= 1 && master == null; dx++) {
            for (int dz = -1; dz <= 1 && master == null; dz++) {
                if (level.getBlockEntity(broken.offset(dx, 0, dz)) instanceof HexBlockEntity hex && hex.isMaster()) {
                    master = broken.offset(dx, 0, dz);
                }
            }
        }
        if (master == null) {
            return;
        }
        unmaking = true;
        try {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = master.offset(dx, 0, dz);
                    if (p.equals(broken) && !(level.getBlockEntity(p) instanceof HexBlockEntity)) {
                        continue;
                    }
                    if (level.getBlockEntity(p) instanceof HexBlockEntity hex) {
                        BlockState back = hex.replaced();
                        // The broken block itself is being replaced already; the rest return to what they were.
                        if (!p.equals(broken)) {
                            level.setBlock(p, back.isAir() ? TTBlocks.STONE_ANCIENT.get().defaultBlockState() : back, 3);
                        } else if (hex.isMaster() && back.is(Blocks.BEDROCK)) {
                            // Original: the centre goes back to bedrock even when it is the one broken.
                            BlockPos at = p;
                            ((ServerLevel) level).getServer().execute(() -> level.setBlock(at, Blocks.BEDROCK.defaultBlockState(), 3));
                        }
                    }
                }
            }
        } finally {
            unmaking = false;
        }
    }

    // ---------------------------------------------------------------- holding a rift
    private static AABB riftBox(BlockPos pos, boolean lesser) {
        return lesser ? new AABB(pos.getX() - 2, pos.getY() + 1, pos.getZ() - 2, pos.getX() + 3, pos.getY() + 9, pos.getZ() + 3)
                : new AABB(pos.getX() - 1, pos.getY(), pos.getZ() - 1, pos.getX() + 2, pos.getY() + 5, pos.getZ() + 2);
    }

    /** Called by a tame rift each second: is its hex still there? */
    static void checkRift(ServerLevel level, EntityFluxRift rift, ConciliumRifts.Kind kind) {
        BlockPos pos = ConciliumRifts.hex(rift);
        boolean held = pos != null && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof HexBlockEntity hex && hex.isMaster();
        if (!held && pos != null && !level.isLoaded(pos)) {
            return;
        }
        if (!held) {
            if (kind == ConciliumRifts.Kind.TAMED) {
                ConciliumRifts.release(rift);
            } else {
                rift.beginCollapse();
            }
        } else if (kind == ConciliumRifts.Kind.TAMED && level.getBlockEntity(pos) instanceof HexBlockEntity hex) {
            // Phase 5c: the size follows the hex's burden (25 empty, 75 at the threshold); it replaces the old clamp at 25.
            int target = HexSink.targetSize(hex.burden(), hex.threshold(), TCConfig.VESSEL_MIN_SIZE.get(), TCConfig.VESSEL_MAX_SIZE.get());
            if (rift.currentSize() < target) {
                ConciliumRifts.growTo(rift, target);
            } else if (rift.currentSize() > target) {
                ConciliumRifts.dematerialize(rift, target);
            }
        }
    }

    /** The rift this master hex holds, tamed if it was native (null if none). */
    @Nullable
    public static EntityFluxRift held(ServerLevel level, BlockPos pos, HexBlockEntity hex) {
        return heldRift(level, pos, hex);
    }

    /**
     * Phase 5c: the hex breaks. Every block goes back to what it replaced, except the four edge ring stones (north, south, east,
     * west of the centre), which are removed (they became the Vessel's anchors).
     */
    public static void dismantle(ServerLevel level, BlockPos master) {
        if (unmaking) {
            return;
        }
        unmaking = true;
        try {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = master.offset(dx, 0, dz);
                    if (!(level.getBlockEntity(p) instanceof HexBlockEntity hex)) {
                        continue;
                    }
                    BlockState back = hex.replaced();
                    boolean edge = (dx == 0) != (dz == 0);
                    if (edge) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    } else {
                        level.setBlock(p, back.isAir() ? TTBlocks.STONE_ANCIENT.get().defaultBlockState() : back, 3);
                    }
                }
            }
        } finally {
            unmaking = false;
        }
    }

    /** Runs block changes on a hex without any of them unmaking it (the break moves and replaces its blocks itself). */
    public static void quietly(Runnable changes) {
        boolean before = unmaking;
        unmaking = true;
        try {
            changes.run();
        } finally {
            unmaking = before;
        }
    }

    @Nullable
    private static EntityFluxRift heldRift(ServerLevel level, BlockPos pos, HexBlockEntity hex) {
        for (EntityFluxRift rift : ConciliumRifts.riftsIn(level, riftBox(pos, hex.isLesser()))) {
            ConciliumRifts.Kind kind = ConciliumRifts.kind(rift);
            if (kind == null && !rift.isCollapsing()) {
                // A native flux rift: tamed by any hex standing under it.
                ConciliumRifts.mark(rift, ConciliumRifts.Kind.TAMED, pos);
                return rift;
            }
            if (kind != null && kind.tame() && pos.equals(ConciliumRifts.hex(rift))) {
                return rift;
            }
        }
        return null;
    }

    public static void tickMaster(ServerLevel level, BlockPos pos, HexBlockEntity hex) {
        if (level.getGameTime() % 60L == 0L) {
            EntityFluxRift held = heldRift(level, pos, hex);
            hex.setHasRift(held != null);
            hex.setSinkActive(held != null && ConciliumRifts.kind(held) == ConciliumRifts.Kind.TAMED);
            HexSink.check(level, pos, hex);
        }
        // Phase 5c: a strained hex that loaded strained breaks on its first master tick.
        if (hex.breakPending() && hex.strained() && hex.isLesser()) {
            HexSink.breakHex(level, pos, hex);
            return;
        }
        if (!hex.hasRift()) {
            return;
        }
        if (hex.heat() >= 2400) {
            EntityFluxRift rift = heldRift(level, pos, hex);
            if (rift != null) {
                rift.beginCollapse();
            }
            hex.setHasRift(false);
            return;
        }
        if (hex.essentia().isEmpty()) {
            return;
        }
        AABB near = new AABB(pos).inflate(8.0, 3.0, 8.0);
        for (ItemEntity slag : level.getEntitiesOfClass(ItemEntity.class, near, e -> e.getItem().is(TCItems.VOID_SLAG.get()))) {
            hex.setHeat(hex.heat() + 2);
        }
        AABB on = new AABB(pos.getX() - 2, pos.getY(), pos.getZ() - 2, pos.getX() + 3, pos.getY() + 5, pos.getZ() + 3);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, on, e -> e.isAlive() && !e.getItem().is(TCItems.VOID_SLAG.get()));
        if (items.size() != 1) {
            return;
        }
        ItemEntity item = items.get(0);
        if (hex.heat() > 0) {
            if (level.getGameTime() % 60L == 0L && level.random.nextInt(10) >= 8) {
                int count = 3 + level.random.nextInt(3);
                for (int i = 0; i < count; i++) {
                    ItemEntity slag = new ItemEntity(level, pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 2.0, pos.getY() + 2,
                            pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 2.0, new ItemStack(TCItems.VOID_SLAG.get()));
                    slag.setDeltaMovement((level.random.nextDouble() - 0.5) * 0.4, 0.2, (level.random.nextDouble() - 0.5) * 0.4);
                    level.addFreshEntity(slag);
                }
            }
            hex.setHeat(hex.heat() - 3);
            if (level.getGameTime() % 10L == 0L) {
                Vec3 from = Vec3.atCenterOf(pos).add(0.0, CHAINED_HEIGHT, 0.0);
                Vec3 to = item.position().add(0.0, 0.2, 0.0);
                for (int i = 0; i <= 8; i++) {
                    Vec3 p = from.lerp(to, i / 8.0);
                    level.sendParticles(ParticleTypes.WITCH, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
                }
            }
            return;
        }
        transmute(level, pos, hex, item);
    }

    private static void transmute(ServerLevel level, BlockPos pos, HexBlockEntity hex, ItemEntity item) {
        Entity owner = item.getOwner();
        if (!(owner instanceof Player thrower)) {
            return;
        }
        int multiplier = hex.isLesser() || isTamed(level, pos, hex) ? 2 : 1;
        // Original findMatchingRiftRecipe: of the recipes the thrower knows, the matching one with the most aspects.
        Optional<RecipeHolder<RiftRecipe>> found = level.getRecipeManager().getAllRecipesFor(TCRecipes.RIFT_TYPE.get()).stream()
                .filter(r -> r.value().matches(item.getItem(), hex.essentia(), multiplier))
                .filter(r -> r.value().research().map(PlayerResearchAccess.of(thrower)::hasCompleted).orElse(true))
                .max(java.util.Comparator.comparingInt(r -> r.value().aspects().size()));
        if (found.isEmpty()) {
            return;
        }
        ItemStack result = found.get().value().result().copy();
        // The original took the whole stack for one result; here one item is used and the rest stay.
        ItemStack rest = item.getItem().copy();
        rest.shrink(1);
        if (rest.isEmpty()) {
            item.discard();
        } else {
            item.setItem(rest);
        }
        ItemEntity out = new ItemEntity(level, item.getX(), item.getY(), item.getZ(), result);
        // Original: flung towards a point a block up and up to 2.5 blocks aside, so it lands beside the hex rather than
        // rising into the rift (which would eat it).
        double angle = level.random.nextDouble() * Math.PI * 2.0;
        out.setDeltaMovement(Math.cos(angle) * 0.35, 0.15, Math.sin(angle) * 0.35);
        level.addFreshEntity(out);
        level.sendParticles(ParticleTypes.END_ROD, out.getX(), out.getY() + 0.3, out.getZ(), 20, 0.3, 0.3, 0.3, 0.05);
        level.playSound(null, pos, TTSounds.CRAFTFAIL.get(), SoundSource.BLOCKS, 0.6F, 1.4F);
        hex.clearEssentia();
    }

    private static boolean isTamed(ServerLevel level, BlockPos pos, HexBlockEntity hex) {
        EntityFluxRift rift = heldRift(level, pos, hex);
        return rift != null && ConciliumRifts.kind(rift) == ConciliumRifts.Kind.TAMED;
    }
}
