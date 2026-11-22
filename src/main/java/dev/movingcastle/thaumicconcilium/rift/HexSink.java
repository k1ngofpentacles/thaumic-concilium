package dev.movingcastle.thaumicconcilium.rift;

import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.api.warp.ItemWarp;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.leclowndu93150.thaumaturge.content.entity.EntityFluxRift;
import com.leclowndu93150.thaumaturge.content.taint.flux.PhysicalFlux;
import com.leclowndu93150.thaumaturge.registry.TTDataMaps;
import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.block.HexBlock;
import dev.movingcastle.thaumicconcilium.block.HexBlockEntity;
import dev.movingcastle.thaumicconcilium.entity.Vessel;
import dev.movingcastle.thaumicconcilium.registry.TCBlocks;
import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import dev.movingcastle.thaumicconcilium.world.VesselDebts;
import dev.movingcastle.thaumicconcilium.world.VesselResearch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;

/**
 * Phase 5c, cycle 1: the Hex of Predictability as a sink. It takes burden from five routes (aura flux, vitium essentia from the Vis
 * Condensers, flux goo and gas around it, a player's warp by sneak-use, warp-carrying items thrown into the rift), grows its rift with
 * it, cracks, strains past a hidden threshold (it still takes everything and transmutes), leaks flux back, and breaks: on the next load
 * of its chunk, or, if the chunk never unloads, when every player in the level sleeps through a night after enough has leaked.
 * Everything rides the master's existing 60-tick check, the rift's existing 5- and 20-tick checks, or events. The pure rules are static
 * and take plain values so the pack test checks them directly.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class HexSink {
    /** Ticks after a night's sleep during which a strained, leaking hex breaks (the master's check runs every 60). */
    private static final long SLEEP_WINDOW = 1200L;
    /** How far out from the hex the broken hex's contributors are landed. */
    public static final int ARENA_EDGE = 15;
    private static final Map<ResourceKey<Level>, Long> SLEPT = new HashMap<>();

    private HexSink() {
    }

    // ---------------------------------------------------------------- pure rules
    /** A hidden spread as a fraction in [-percent/100, +percent/100]. */
    public static double rollSpread(RandomSource random, int percent) {
        return (random.nextDouble() * 2.0 - 1.0) * percent / 100.0;
    }

    public static int threshold(int base, double spread) {
        return Math.max(1, (int) Math.round(base * (1.0 + spread)));
    }

    /** The rift size for a burden: {@code min} at none, {@code max} at the threshold and beyond. */
    public static int targetSize(int burden, int threshold, int min, int max) {
        double share = Math.min(1.0, Math.max(0.0, burden / (double) Math.max(1, threshold)));
        return (int) Math.round(min + (max - min) * share);
    }

    /** 0 whole, 1 to 3 cracked by the burden's share of the threshold; 3 once strained. */
    public static int crackStage(int burden, int threshold) {
        if (burden >= threshold) {
            return 3;
        }
        double share = burden / (double) Math.max(1, threshold);
        return share >= 2.0 / 3.0 ? 2 : share >= 1.0 / 3.0 ? 1 : 0;
    }

    /** L1: the flux a strained hex returns for {@code fed} burden. */
    public static float leakFlux(double fed, double factor, double fluxPerBurden) {
        return (float) (fed * factor * fluxPerBurden);
    }

    /** Where one contributor lands (x, z; the safe-landing search picks y). */
    public record Landing(UUID player, double x, double z) {
    }

    /** The landing spots for the online contributors: spread evenly round the arena's edge, starting at {@code startAngle} (radians). */
    public static List<Landing> teleportPlan(List<UUID> online, BlockPos centre, double radius, double startAngle) {
        List<Landing> plan = new ArrayList<>();
        for (int i = 0; i < online.size(); i++) {
            double angle = startAngle + i * (Math.PI * 2.0 / online.size());
            plan.add(new Landing(online.get(i), centre.getX() + 0.5 + Math.cos(angle) * radius, centre.getZ() + 0.5 + Math.sin(angle) * radius));
        }
        return plan;
    }

    /**
     * Temporary warp owed by contributors who are not online: their share of the burden times {@code scale}, at least 1.
     * Players who contributed nothing owe nothing.
     */
    public static Map<UUID, Integer> absentDebts(Map<UUID, Integer> contributions, Set<UUID> online, int burden, int scale) {
        Map<UUID, Integer> debts = new LinkedHashMap<>();
        for (Map.Entry<UUID, Integer> e : contributions.entrySet()) {
            if (online.contains(e.getKey()) || e.getValue() <= 0) {
                continue;
            }
            double share = Math.min(1.0, e.getValue() / (double) Math.max(1, burden));
            debts.put(e.getKey(), Math.max(1, (int) Math.round(share * scale)));
        }
        return debts;
    }

    /** Four anchor offsets (dx, dz), a quarter turn apart from a random start, each 8 to 12 blocks out. */
    public static List<int[]> anchorOffsets(RandomSource random) {
        List<int[]> out = new ArrayList<>();
        for (double[] s : anchorSpokes(random)) {
            out.add(new int[]{(int) Math.round(Math.cos(s[0]) * s[1]), (int) Math.round(Math.sin(s[0]) * s[1])});
        }
        return out;
    }

    /** The most a spoke strays from its quarter turn, degrees, and the most an anchor's distance strays from the Vessel's radius. */
    public static final double SPOKE_JITTER = 15.0;
    public static final double RADIUS_JITTER = 1.0;

    /**
     * The four spokes the anchors fly out along (cycle 3c): {angle in radians, distance}. A quarter turn apart from a random start,
     * each up to {@link #SPOKE_JITTER} degrees off; one radius per Vessel in 8 to 12, each anchor within {@link #RADIUS_JITTER} of
     * it and still inside 8 to 12, so the landing points form a rough circle round the hex.
     */
    public static List<double[]> anchorSpokes(RandomSource random) {
        double start = random.nextDouble() * Math.PI * 2.0;
        double radius = 8.0 + random.nextDouble() * 4.0;
        List<double[]> out = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            double angle = start + i * Math.PI / 2.0 + Math.toRadians((random.nextDouble() * 2.0 - 1.0) * SPOKE_JITTER);
            double dist = Math.max(8.0, Math.min(12.0, radius + (random.nextDouble() * 2.0 - 1.0) * RADIUS_JITTER));
            out.add(new double[]{angle, dist});
        }
        return out;
    }

    // ---------------------------------------------------------------- feeding
    /**
     * Adds {@code raw} burden to the master hex, credited to {@code who} (null for routes with no contributor). A strained hex also
     * leaks {@code leak.factor} times what it was fed back into its chunk as aura flux (L1). Returns the whole burden added.
     */
    public static int feed(ServerLevel level, BlockPos pos, HexBlockEntity hex, double raw, @Nullable UUID who) {
        if (raw <= 0.0 || !hex.sinkActive()) {
            return 0;
        }
        hex.rollSpreads(level.random);
        boolean wasStrained = hex.strained();
        int gained = hex.addBurden(raw, who);
        if (wasStrained) {
            float flux = leakFlux(raw, TCConfig.VESSEL_LEAK_FACTOR.get(), TCConfig.VESSEL_LEAK_FLUX_PER_BURDEN.get());
            if (flux > 0.0F) {
                AuraHelper.addFlux(level, pos, flux);
                hex.addLeaked(flux);
            }
        }
        refreshCrack(level, pos, hex);
        if (who != null && gained > 0 && hex.burden() * 4 >= hex.threshold()) {
            // R1 note 2: a hex of yours passed a quarter of its threshold (event-driven; one test once it is known)
            ServerPlayer p = Vessel.playerById(level, who);
            if (p != null) {
                VesselResearch.grantNote(p, VesselResearch.WHAT_IT_KEEPS, 0);
            }
        }
        return gained;
    }

    /** F2: vitium essentia pushed into the hex by a Vis Condenser (no owner is stored on a condenser, so no contributor). */
    public static void feedEssentia(ServerLevel level, BlockPos pos, HexBlockEntity hex, net.minecraft.resources.ResourceLocation aspect, int amount) {
        if (aspect.getNamespace().equals("thaumaturge") && aspect.getPath().equals("vitium") && hex.sinkActive()) {
            feed(level, pos, hex, amount * TCConfig.VESSEL_BURDEN_PER_VITIUM.get(), null);
        }
    }

    /** The master's 60-tick check (the existing one): rolls the spreads, F1 and F3 while it holds a rift, the cracks, the sleep break. */
    public static void check(ServerLevel level, BlockPos pos, HexBlockEntity hex) {
        if (!hex.sinkActive()) {
            return;
        }
        hex.rollSpreads(level.random);
        if (hex.sinkActive()) {
            float drained = AuraHelper.drainFlux(level, pos, TCConfig.VESSEL_FLUX_PER_CHECK.get().floatValue(), false);
            if (drained > 0.0F) {
                feed(level, pos, hex, drained * TCConfig.VESSEL_BURDEN_PER_FLUX.get(), null);
            }
            int quanta = drinkGoo(level, pos);
            if (quanta > 0) {
                feed(level, pos, hex, quanta * TCConfig.VESSEL_BURDEN_PER_GOO.get(), null);
            }
        }
        refreshCrack(level, pos, hex);
        if (hex.strained() && hex.leaked() >= hex.leakThreshold() && sleptRecently(level)) {
            breakHex(level, pos, hex);
        }
    }

    /** F3: the rift drinks flux goo and gas within 4 blocks of the hex (as Forbidden Magic's Tainthistle does). Returns the quanta drunk. */
    public static int drinkGoo(ServerLevel level, BlockPos pos) {
        int total = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-4, -4, -4), pos.offset(4, 4, 4))) {
            if (level.hasChunkAt(p) && PhysicalFlux.isPhysicalFlux(level.getBlockState(p))) {
                total += PhysicalFlux.reduce(level, p, PhysicalFlux.MAX_QUANTA);
            }
        }
        return total;
    }

    /** W1: one point of the player's temporary warp, or else normal warp (never permanent), given to the hex. */
    public static boolean sneakUse(ServerLevel level, BlockPos pos, Player player) {
        if (!(player instanceof ServerPlayer sp) || !(level.getBlockEntity(pos) instanceof HexBlockEntity hex) || !hex.isMaster() || !hex.sinkActive()) {
            return false;
        }
        WarpType type = WarpHelper.getWarp(player).get(WarpType.TEMPORARY) > 0 ? WarpType.TEMPORARY
                : WarpHelper.getWarp(player).get(WarpType.NORMAL) > 0 ? WarpType.NORMAL : null;
        if (type == null) {
            sp.displayClientMessage(Component.translatable("message.thaumicconcilium.hex_no_warp"), true);
            return true;
        }
        WarpHelper.addWarp(sp, -1, type);
        feed(level, pos, hex, TCConfig.VESSEL_BURDEN_PER_WARP.get(), sp.getUUID());
        sp.displayClientMessage(Component.translatable("message.thaumicconcilium.hex_warp_taken"), true);
        level.sendParticles(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 20, 0.3, 0.5, 0.3, 0.4);
        return true;
    }

    /** W2: items carrying warp that touch a hexed rift are destroyed and their warp becomes burden, credited to whoever threw them. */
    public static void offerWarpItems(ServerLevel level, EntityFluxRift rift) {
        BlockPos hexPos = ConciliumRifts.hex(rift);
        if (hexPos == null || !level.isLoaded(hexPos) || !(level.getBlockEntity(hexPos) instanceof HexBlockEntity hex) || !hex.isMaster()) {
            return;
        }
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, rift.getBoundingBox().inflate(0.5), e -> e.isAlive())) {
            ItemWarp warp = item.getItem().getItem().builtInRegistryHolder().getData(TTDataMaps.ITEM_WARP);
            if (warp == null) {
                continue;
            }
            UUID who = item.getOwner() instanceof Player thrower ? thrower.getUUID() : null;
            double raw = (double) warp.amount() * item.getItem().getCount() * TCConfig.VESSEL_BURDEN_PER_ITEM_WARP.get();
            item.discard();
            feed(level, hexPos, hex, raw, who);
        }
    }

    // ---------------------------------------------------------------- cracks
    /** Sets the crack state on all nine hex blocks to the burden's stage (only when it differs). */
    public static void refreshCrack(ServerLevel level, BlockPos master, HexBlockEntity hex) {
        int stage = crackStage(hex.burden(), hex.threshold());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = master.offset(dx, 0, dz);
                BlockState s = level.getBlockState(p);
                if (s.is(TCBlocks.HEX.get()) && s.getValue(HexBlock.CRACK) != stage) {
                    level.setBlock(p, s.setValue(HexBlock.CRACK, stage), 2);
                }
            }
        }
    }

    // ---------------------------------------------------------------- the break
    @SubscribeEvent
    public static void onSleepFinished(SleepFinishedTimeEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            noteSleep(level);
        }
    }

    /** All players in the level slept through the night (L2). */
    public static void noteSleep(ServerLevel level) {
        SLEPT.put(level.dimension(), level.getGameTime());
    }

    /** Forgets the level's last night slept through (a pack-test hook; tests share one level). */
    public static void forgetSleep(ServerLevel level) {
        SLEPT.remove(level.dimension());
    }

    private static boolean sleptRecently(ServerLevel level) {
        Long t = SLEPT.get(level.dimension());
        return t != null && level.getGameTime() - t <= SLEEP_WINDOW;
    }

    /**
     * The hex breaks: the rift is removed, the hex unmade with its four edge ring stones landing as anchors 8 to 12 blocks out, a
     * dormant Vessel left three blocks above it holding the burden and who gave it, every online contributor taken to the arena's
     * edge from any dimension, and offline contributors' temporary warp recorded for their next login.
     */
    public static void breakHex(ServerLevel level, BlockPos pos, HexBlockEntity hex) {
        int burden = hex.burden();
        Map<UUID, Integer> contributions = new LinkedHashMap<>(hex.contributions());
        EntityFluxRift rift = Hexes.held(level, pos, hex);
        if (rift != null) {
            rift.discard();
        }
        // The hex is broken for good: its nine blocks leave the ground at once and nothing is put back (with Sable they ride a
        // sub-level through the sequence and go with it). At the burst the crater is carved and the four shards land on its ground.
        List<BlockPos> parts = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = pos.offset(dx, 0, dz);
                if (level.getBlockEntity(p) instanceof HexBlockEntity part) {
                    part.setInert(true);
                    parts.add(p);
                }
            }
        }
        dev.movingcastle.thaumicconcilium.compat.SableBreak.Held[] held = {null};
        Hexes.quietly(() -> {
            if (!standInOnly && TCConfig.BREAK_SABLE.get() && net.neoforged.fml.ModList.get().isLoaded("sable")) {
                try {
                    held[0] = dev.movingcastle.thaumicconcilium.compat.SableBreak.assemble(level, pos);
                } catch (RuntimeException | LinkageError e) {
                    com.mojang.logging.LogUtils.getLogger().warn("The hex could not be made a Sable sub-level; the break plays with the stand-in", e);
                }
            }
            for (BlockPos p : parts) {
                level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
        });
        Vessel vessel = new Vessel(TCEntities.VESSEL.get(), level);
        vessel.moveTo(pos.getX() + 0.5, pos.getY() + 3.0, pos.getZ() + 0.5, 0.0F, 0.0F);
        vessel.setup(burden, contributions, List.of(), pos);
        vessel.startBreak(level, held[0] == null ? null : held[0].id(), held[0] == null ? null : held[0].base(),
                anchorSpokes(level.random).toArray(new double[0][]), crater);
        level.addFreshEntity(vessel);

        List<UUID> online = new ArrayList<>();
        List<ServerPlayer> players = new ArrayList<>();
        for (UUID id : contributions.keySet()) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
            if (p != null) {
                online.add(id);
                players.add(p);
            }
        }
        for (ServerPlayer p : players) {
            VesselResearch.appear(p, VesselResearch.THE_VESSEL); // R1 note 3 appears at the first break; the subdue completes it
        }
        List<Landing> plan = teleportPlan(online, pos, ARENA_EDGE, level.random.nextDouble() * Math.PI * 2.0);
        for (int i = 0; i < plan.size(); i++) {
            Subspace.cross(players.get(i), new Subspace.Crossing(level, plan.get(i).x(), plan.get(i).z()));
        }
        Map<UUID, Integer> debts = absentDebts(contributions, Set.copyOf(online), burden, TCConfig.VESSEL_ABSENT_WARP_SCALE.get());
        if (!debts.isEmpty()) {
            VesselDebts data = VesselDebts.get(level.getServer());
            debts.forEach((id, warp) -> data.owe(id, warp, contributions.get(id) / (float) Math.max(1, burden),
                    level.dimension().location().toString(), pos));
        }
    }

    /** QA scaffolding (the {@code qa vessel} command): a Vessel with four anchors at {@code pos}, dormant or quiescent (charge in full). */
    public static Vessel qaVessel(ServerLevel level, BlockPos pos, UUID contributor, boolean quiescent) {
        List<BlockPos> anchors = placeAnchors(level, pos);
        Vessel vessel = new Vessel(TCEntities.VESSEL.get(), level);
        vessel.moveTo(pos.getX() + 0.5, pos.getY() + 3.0, pos.getZ() + 0.5, 0.0F, 0.0F);
        Map<UUID, Integer> contributions = new LinkedHashMap<>();
        contributions.put(contributor, 400);
        vessel.setup(400, contributions, anchors, pos);
        if (quiescent) {
            vessel.setFightState(Vessel.SUBDUED, 32, 0);
            vessel.setCharge(TCConfig.RITE_CHARGE.get());
        }
        level.addFreshEntity(vessel);
        return vessel;
    }

    /**
     * QA scaffolding (the {@code qa vessel ... breaking} command): the break sequence at {@code pos}. A full hex standing there breaks
     * as in play; with none, a dormant Vessel plays the sequence with the stand-in.
     */
    public static void qaBreak(ServerLevel level, BlockPos pos, UUID contributor, boolean standIn) {
        boolean before = standInOnly;
        standInOnly = standIn;
        try {
            if (level.getBlockEntity(pos) instanceof HexBlockEntity hex && hex.isMaster()) {
                hex.setHasRift(true); // as in play: a hex that breaks holds a rift, so its runes are lit
                breakHex(level, pos, hex);
                return;
            }
            Vessel vessel = new Vessel(TCEntities.VESSEL.get(), level);
            vessel.moveTo(pos.getX() + 0.5, pos.getY() + 3.0, pos.getZ() + 0.5, 0.0F, 0.0F);
            Map<UUID, Integer> contributions = new LinkedHashMap<>();
            contributions.put(contributor, 400);
            vessel.setup(400, contributions, List.of(), pos);
            vessel.startBreak(level, null, null, anchorSpokes(level.random).toArray(new double[0][]), crater);
            level.addFreshEntity(vessel);
        } finally {
            standInOnly = before;
        }
    }

    /** Places four anchor blocks at once (QA scaffolding: no break sequence) on the radial landing spots. */
    private static List<BlockPos> placeAnchors(ServerLevel level, BlockPos pos) {
        List<BlockPos> placed = landingSpots(level, pos);
        for (BlockPos at : placed) {
            level.setBlock(at, TCBlocks.ANCHOR.get().defaultBlockState(), 3);
        }
        return placed;
    }

    /**
     * The four spots the anchors land on: solid ground along each of {@link #anchorSpokes}' spokes, so they stand in a rough circle
     * round the hex; where a spoke has no ground nearby, level with the hex. Nothing is placed.
     */
    public static List<BlockPos> landingSpots(ServerLevel level, BlockPos pos) {
        return landingSpots(level, pos, anchorSpokes(level.random).toArray(new double[0][]), null);
    }

    /**
     * The landing spots along given spokes ({angle, distance}). With a {@code crater}: on the ground it will leave once its ring has
     * passed (the Vessel asks at the burst, before anything is cut); with none, on the ground as it stands.
     */
    public static List<BlockPos> landingSpots(ServerLevel level, BlockPos pos, double[][] spokes,
            @Nullable dev.movingcastle.thaumicconcilium.entity.VesselCrater crater) {
        List<BlockPos> spots = new ArrayList<>();
        for (double[] spoke : spokes) {
            BlockPos at = groundAlong(level, pos, spoke[0], spoke[1], spots, crater);
            spots.add(at);
        }
        return spots;
    }

    /**
     * Steps along the spoke (the picked distance first, then up to 3 blocks nearer, never farther: every anchor stays inside the
     * crater's bowl, whose radius is the farthest spoke plus 2), then a little to either side of it.
     */
    private static final double[] SPOKE_DR = {0.0, -1.0, -2.0, -3.0};
    private static final double[] SPOKE_DA = {0.0, 4.0, -4.0, 8.0, -8.0};

    private static BlockPos groundAlong(ServerLevel level, BlockPos hex, double angle, double dist, List<BlockPos> taken,
            @Nullable dev.movingcastle.thaumicconcilium.entity.VesselCrater crater) {
        for (double da : SPOKE_DA) {
            for (double dr : SPOKE_DR) {
                double a = angle + Math.toRadians(da);
                int bx = hex.getX() + (int) Math.round(Math.cos(a) * (dist + dr));
                int bz = hex.getZ() + (int) Math.round(Math.sin(a) * (dist + dr));
                level.getChunk(bx >> 4, bz >> 4);
                int y = crater != null ? crater.surfaceAfter(level, bx, bz) : level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
                BlockPos at = new BlockPos(bx, y, bz);
                BlockPos floor = at.below();
                BlockState below = level.getBlockState(floor);
                // with a crater the spot itself may still be filled now (the ring will clear it); without, it must be free
                if (y > level.getMinBuildHeight() && below.isFaceSturdy(level, floor, Direction.UP) && below.getFluidState().isEmpty()
                        && (crater != null || level.getBlockState(at).canBeReplaced()) && !taken.contains(at)) {
                    return at;
                }
            }
        }
        return new BlockPos(hex.getX() + (int) Math.round(Math.cos(angle) * dist), hex.getY(), hex.getZ() + (int) Math.round(Math.sin(angle) * dist));
    }

    /** Test hook: the break plays with the stand-in even where Sable is loaded. */
    public static boolean standInOnly;
    /** Test hook: false leaves the crater out (GameTests that share an arena with other tests). Always true in play. */
    public static boolean crater = true;

    // ---------------------------------------------------------------- absent contributors
    /** An absent contributor's debt comes due on their next login: temporary warp, and a warp event at once. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }
        VesselDebts data = VesselDebts.get(player.getServer());
        VesselDebts.Debt debt = data.debts().get(player.getUUID());
        if (debt == null || debt.paid) {
            return;
        }
        pay(player, debt);
        data.setDirty();
    }

    /** Pays one debt: temporary warp, a warp event, and the entry marked paid (kept for the fight's rule on late joiners). */
    public static void pay(ServerPlayer player, VesselDebts.Debt debt) {
        debt.paid = true;
        VesselResearch.appear(player, VesselResearch.THE_VESSEL);
        WarpHelper.addWarp(player, debt.warp, WarpType.TEMPORARY);
        WarpKick.fire(player);
        player.displayClientMessage(Component.translatable("message.thaumicconcilium.vessel_debt"), false);
    }
}
