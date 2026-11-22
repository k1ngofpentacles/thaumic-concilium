package dev.movingcastle.thaumicconcilium.world;

import com.leclowndu93150.thaumaturge.api.infusion.InfusionCraftedEvent;
import com.leclowndu93150.thaumaturge.api.recipe.InfusionCraftingTransaction;
import com.leclowndu93150.thaumaturge.api.recipe.InfusionMatrixContext;
import com.leclowndu93150.thaumaturge.content.infusion.BlockEntityInfusionMatrix;
import com.leclowndu93150.thaumaturge.content.infusion.BlockEntityPedestal;
import com.leclowndu93150.thaumaturge.content.infusion.MatrixEnvironment;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.network.BindingPayload;
import dev.movingcastle.thaumicconcilium.registry.TCItems;
import dev.movingcastle.thaumicconcilium.registry.TCSounds;
import dev.movingcastle.thaumicconcilium.world.BindingData.Mode;
import dev.movingcastle.thaumicconcilium.world.BindingData.Session;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Crimson Dagger's binding minigame, D1 option C "The Offertory" (docs/design/5b-infusion-minigame-options.md).
 *
 * <p>While the dagger recipe is infusing, the player who started it is chained to the matrix. The chain drinks 1 HP every
 * {@link #DRAIN_EVERY} ticks unless it is fed: from tick {@link #FIRST_DEMAND}, then every {@link #DEMAND_EVERY} ticks, it
 * demands one item of the tag {@code thaumicconcilium:binding_offerings}; a matching stack on a spare pedestal within
 * {@link #DEMAND_WINDOW} ticks is eaten and pauses the drain until the next demand. A lapsed demand bites {@link #BITE} HP and
 * doubles the drain until one is met. The chain never takes the player to or below {@link #HP_FLOOR} HP: it snaps instead
 * (Rites dropped, native fail, Weakness).
 *
 * <p>Server-authoritative. Idle cost: the tick handler returns at once while no session and no pending open exists; a session
 * costs one counter, one distance check, and every {@link #SCAN_EVERY} ticks inside a demand window up to ten pedestal lookups.
 * No mixin: the session is opened from the right-click on the matrix and closed by the matrix's own state.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class BindingSessions {
    // ------------------------------------------------------------------ the numbers (memo option C)
    public static final int FIRST_DEMAND = 40;
    public static final int DEMAND_EVERY = 80;
    public static final int DEMAND_WINDOW = 60;
    public static final int SCAN_EVERY = 5;
    public static final int DRAIN_EVERY = 20;
    public static final int STARVED_DRAIN_EVERY = 10;
    public static final float DRAIN = 1.0F;
    public static final float BITE = 5.0F;
    /** The chain never takes the player to or below this: a drain that would reach it snaps the chain instead. */
    public static final float HP_FLOOR = 4.0F;
    public static final double TETHER = 6.0;
    public static final double SNAP_DISTANCE = 10.0;
    public static final double PULL = 0.3;
    public static final int WEAKNESS_TICKS = 600;
    /** Ticks after a snap in which ritual damage cannot kill (the matrix's native failure outcome rolls then). */
    public static final int SNAP_PROTECT_TICKS = 100;
    /** Ticks a bound player may be offline (a restart) before the chain snaps. */
    public static final int OFFLINE_GRACE = 1200;

    // ------------------------------------------------------------------ the Yank (D1-c2: a native instability trigger of this rite)
    /** Ticks between the trigger (the chain glows) and the pull. */
    public static final int YANK_TELEGRAPH = 20;
    /** Ticks the pulled binder is fully Chained (no item use, the first-person shackles show) before the leash holds them again. */
    public static final int YANK_CHAINED = 60;
    /** Ticks a snapped matrix's own failing cycle may swallow its native outcome (the rite already completed). */
    public static final int COMPLETING_TTL = 200;

    // ------------------------------------------------------------------ sub-decisions (one-line switches)
    /** Sub-decision 3: false = helpers may heal and restock but cannot take over (default); true = the chain passes to the nearest player on a snap. */
    public static final boolean SNAP_PASSES_CHAIN = false;
    /** Sub-decision 4: true = a dagger craft finishing with no live binding returns the Rites (default); false = allowed. */
    public static final boolean HARD_GATE = true;

    public static final ResourceLocation RECIPE = ThaumicConcilium.id("infusion/crimson_dagger");
    public static final TagKey<Item> OFFERINGS = TagKey.create(Registries.ITEM, ThaumicConcilium.id("binding_offerings"));
    public static final ResourceKey<DamageType> BINDING_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, ThaumicConcilium.id("binding"));

    private static final int PENDING_TTL = 3;

    private record Pending(UUID player, int[] age) {
    }

    /** Levels that hold at least one session (the idle tick returns at once when this and PENDING are empty). */
    private static final java.util.Set<ResourceKey<Level>> ACTIVE = new java.util.HashSet<>();
    private static final Map<GlobalPos, Pending> PENDING = new HashMap<>();
    /** Cached ring pedestals per matrix (refreshed at each demand). */
    private static final Map<GlobalPos, List<BlockPos>> PEDESTALS = new HashMap<>();
    /** Fake players are not in the player list: GameTests register theirs here. */
    private static final Map<UUID, ServerPlayer> TRACKED = new HashMap<>();
    private static boolean stopping;
    /** Matrices whose rite a snap completed: their next (failing) cycle's native outcome is swallowed, until this game time. */
    private static final Map<GlobalPos, Long> COMPLETING = new HashMap<>();
    /** Player to game time until which ritual damage cannot kill (after a snap). */
    private static final Map<UUID, Long> PROTECT = new HashMap<>();
    /** The ritual guard leaves at least this much health. */
    public static final float SURVIVE_HEALTH = 1.0F;

    /** Thaumaturge's instability outcomes: a zap is plain magic damage, an explosion has no source entity. Mobs, lava etc. are not ritual. */
    public static boolean isRitualDamage(DamageSource source) {
        return source.getEntity() == null && source.getDirectEntity() == null
                && (source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC) || source.is(net.minecraft.world.damagesource.DamageTypes.EXPLOSION));
    }

    private static boolean isBound(ServerPlayer player) {
        for (ResourceKey<Level> key : ACTIVE) {
            ServerLevel level = player.server.getLevel(key);
            if (level != null) {
                for (Session s : data(level).sessions().values()) {
                    if (s.player.equals(player.getUUID())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** While bound, and for {@link #SNAP_PROTECT_TICKS} after a snap, ritual damage cannot take the player below {@link #SURVIVE_HEALTH}. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || (ACTIVE.isEmpty() && PROTECT.isEmpty())
                || !isRitualDamage(event.getSource())) {
            return;
        }
        Long until = PROTECT.get(player.getUUID());
        boolean guarded = until != null && player.level().getGameTime() <= until;
        if (until != null && !guarded) {
            PROTECT.remove(player.getUUID());
        }
        if ((guarded || isBound(player)) && player.getHealth() - event.getAmount() < SURVIVE_HEALTH) {
            event.setAmount(Math.max(0.0F, player.getHealth() - SURVIVE_HEALTH));
        }
    }

    private BindingSessions() {
    }

    // ------------------------------------------------------------------ access
    public static BindingData data(ServerLevel level) {
        BindingData data = level.getDataStorage().computeIfAbsent(BindingData.factory(), BindingData.NAME);
        if (data.sessions().isEmpty()) {
            ACTIVE.remove(level.dimension());
        } else {
            ACTIVE.add(level.dimension());
        }
        return data;
    }

    /** The live session at a matrix, or null. */
    public static Session session(ServerLevel level, BlockPos matrix) {
        return data(level).get(matrix);
    }

    /** GameTests: make a fake player findable by the session (the player list does not hold fake players). */
    public static void track(ServerPlayer player) {
        TRACKED.put(player.getUUID(), player);
    }

    public static void untrack(ServerPlayer player) {
        TRACKED.remove(player.getUUID());
    }

    private static ServerPlayer findPlayer(MinecraftServer server, UUID id) {
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        return p != null ? p : TRACKED.get(id);
    }

    // ------------------------------------------------------------------ opening
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (level.getBlockEntity(event.getPos()) instanceof BlockEntityInfusionMatrix) {
            tryOpen(level, event.getPos(), player);
        }
    }

    /**
     * Queues a binding when the matrix at {@code pos} is active, idle, and its staged ring is the dagger recipe; the session opens on
     * the next server tick if the craft has started by then. Returns whether it was queued.
     */
    public static boolean tryOpen(ServerLevel level, BlockPos pos, ServerPlayer player) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityInfusionMatrix matrix) || !matrix.isActive() || matrix.isCrafting()
                || data(level).get(pos) != null) {
            return false;
        }
        ItemStack catalyst = pedestalItem(level, pos.below(2));
        if (catalyst.isEmpty()) {
            return false;
        }
        List<ItemStack> ring = new ArrayList<>();
        for (BlockPos p : MatrixEnvironment.survey(level, pos).pedestals()) {
            ItemStack stack = pedestalItem(level, p);
            if (!stack.isEmpty()) {
                ring.add(stack.copyWithCount(1));
            }
        }
        InfusionCraftingTransaction.Inspection inspection = InfusionCraftingTransaction.inspect(
                InfusionMatrixContext.of(player, pos), player, catalyst.copyWithCount(1), ring);
        if (!inspection.successful() || inspection.recipeId() == null || !inspection.recipeId().location().equals(RECIPE)) {
            return false;
        }
        PENDING.put(GlobalPos.of(level.dimension(), pos.immutable()), new Pending(player.getUUID(), new int[1]));
        return true;
    }

    private static ItemStack pedestalItem(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof BlockEntityPedestal ped ? ped.getItem() : ItemStack.EMPTY;
    }

    private static void open(ServerLevel level, BlockPos pos, ServerPlayer player) {
        BindingData data = data(level);
        Session s = new Session(pos.immutable(), player.getUUID(), level.getGameTime());
        for (BlockPos p : MatrixEnvironment.survey(level, pos).pedestals()) {
            ItemStack held = pedestalItem(level, p);
            if (!held.isEmpty()) {
                s.ring.put(p.immutable(), BuiltInRegistries.ITEM.getKey(held.getItem()).toString());
            }
        }
        data.put(s);
        data(level);
        PEDESTALS.remove(GlobalPos.of(level.dimension(), s.matrix));
        send(level, s, BindingPayload.OPEN, "", BlockPos.ZERO);
        level.playSound(null, pos, TCSounds.SHACKLES.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message." + ThaumicConcilium.MODID + ".binding_open"), true);
    }

    // ------------------------------------------------------------------ the tick
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty() && ACTIVE.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        if (!PENDING.isEmpty()) {
            tickPending(server);
        }
        for (ResourceKey<Level> key : new ArrayList<>(ACTIVE)) {
            ServerLevel level = server.getLevel(key);
            if (level == null) {
                continue;
            }
            BindingData data = data(level);
            for (Session s : new ArrayList<>(data.sessions().values())) {
                tickSession(server, level, data, s);
            }
        }
    }

    private static void tickPending(MinecraftServer server) {
        for (var it = new ArrayList<>(PENDING.entrySet()).iterator(); it.hasNext(); ) {
            var entry = it.next();
            GlobalPos at = entry.getKey();
            Pending pending = entry.getValue();
            ServerLevel level = server.getLevel(at.dimension());
            ServerPlayer player = findPlayer(server, pending.player());
            boolean done = true;
            if (level != null && player != null && level.isLoaded(at.pos())) {
                if (level.getBlockEntity(at.pos()) instanceof BlockEntityInfusionMatrix m && m.isCrafting()) {
                    if (data(level).get(at.pos()) == null) {
                        open(level, at.pos(), player);
                    }
                } else {
                    done = ++pending.age()[0] > PENDING_TTL;
                }
            }
            if (done) {
                PENDING.remove(at);
            }
        }
    }

    private static void tickSession(MinecraftServer server, ServerLevel level, BindingData data, Session s) {
        if (!level.isLoaded(s.matrix)) {
            return;
        }
        if (!(level.getBlockEntity(s.matrix) instanceof BlockEntityInfusionMatrix matrix) || !matrix.isActive() || !matrix.isCrafting()) {
            close(level, s);
            return;
        }
        ServerPlayer player = findPlayer(server, s.player);
        if (player == null) {
            if (++s.away > OFFLINE_GRACE) {
                snap(level, s, "other");
            } else {
                data.setDirty();
            }
            return;
        }
        s.away = 0;
        if (!player.isAlive() || player.level() != level) {
            snap(level, s, "other");
            return;
        }
        Vec3 centre = Vec3.atCenterOf(s.matrix);
        double d2 = player.position().distanceToSqr(centre);
        if (d2 > SNAP_DISTANCE * SNAP_DISTANCE) {
            snap(level, s, "away");
            return;
        }
        if (d2 > TETHER * TETHER) {
            player.setDeltaMovement(centre.subtract(player.position()).normalize().scale(PULL));
            player.hurtMarked = true;
        }
        s.elapsed++;
        data.setDirty();
        // a Yank in the making: the chain glows, then pulls the binder to the altar and shackles them
        if (s.telegraph > 0 && --s.telegraph == 0) {
            yank(level, s, player);
        }

        // a new demand
        if (s.elapsed == FIRST_DEMAND + DEMAND_EVERY * s.demands) {
            beginDemand(level, s, player);
        }
        // the window: feed it, or let it lapse
        if (!s.demand.isEmpty()) {
            if (s.elapsed % SCAN_EVERY == 0 && feed(level, s)) {
                s.demand = "";
                s.mode = Mode.PAUSED;
            } else if (s.elapsed >= s.windowEnd) {
                s.demand = "";
                s.mode = Mode.STARVED;
                s.drainTimer = 0;
                send(level, s, BindingPayload.MISSED, "", BlockPos.ZERO);
                player.displayClientMessage(Component.translatable("message." + ThaumicConcilium.MODID + ".binding_miss"), true);
                if (!deal(level, s, player, BITE)) {
                    return;
                }
            }
        }
        // the drain
        if (s.mode != Mode.PAUSED && ++s.drainTimer >= s.drainInterval()) {
            s.drainTimer = 0;
            deal(level, s, player, DRAIN);
        }
    }

    private static void beginDemand(ServerLevel level, Session s, ServerPlayer player) {
        s.demand = pickDemand(s);
        s.demands++;
        s.windowEnd = s.elapsed + DEMAND_WINDOW;
        if (s.mode == Mode.PAUSED) {
            s.mode = Mode.NORMAL;
            s.drainTimer = 0;
        }
        GlobalPos key = GlobalPos.of(level.dimension(), s.matrix);
        PEDESTALS.put(key, new ArrayList<>(MatrixEnvironment.survey(level, s.matrix).pedestals()));
        send(level, s, BindingPayload.DEMAND, s.demand, BlockPos.ZERO);
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(s.demand));
        player.displayClientMessage(Component.translatable("message." + ThaumicConcilium.MODID + ".binding_demand", item.getDescription()), true);
    }

    /** The demanded item, seeded by the matrix position and the start time (deterministic per craft and per index). */
    private static String pickDemand(Session s) {
        List<String> ids = new ArrayList<>();
        BuiltInRegistries.ITEM.getTag(OFFERINGS).ifPresent(set -> set.forEach(h -> ids.add(BuiltInRegistries.ITEM.getKey(h.value()).toString())));
        if (ids.isEmpty()) {
            ids.add("minecraft:rotten_flesh");
        }
        Collections.sort(ids);
        long seed = s.matrix.asLong() * 31L + s.started * 17L + s.demands * 7919L;
        return ids.get(new Random(seed ^ 0x5DEECE66DL).nextInt(ids.size()));
    }

    /** Eats one demanded item from a ring pedestal that holds it. Ring items are never offerings, so they are never touched. */
    private static boolean feed(ServerLevel level, Session s) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(s.demand));
        List<BlockPos> pedestals = PEDESTALS.computeIfAbsent(GlobalPos.of(level.dimension(), s.matrix),
                k -> new ArrayList<>(MatrixEnvironment.survey(level, s.matrix).pedestals()));
        for (BlockPos p : pedestals) {
            if (level.isLoaded(p) && level.getBlockEntity(p) instanceof BlockEntityPedestal ped) {
                ItemStack stack = ped.getItem();
                // a ring ingredient the matrix has not pulled yet (the brain is also an offering) is never eaten
                boolean ringItem = !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(s.ring.get(p));
                if (!stack.isEmpty() && stack.is(item) && !ringItem) {
                    ped.setItem(stack.getCount() > 1 ? stack.copyWithCount(stack.getCount() - 1) : ItemStack.EMPTY);
                    send(level, s, BindingPayload.FED, "", p);
                    ServerPlayer player = findPlayer(level.getServer(), s.player);
                    if (player != null) {
                        player.displayClientMessage(Component.translatable("message." + ThaumicConcilium.MODID + ".binding_fed"), true);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /** Drains {@code amount} HP; a drain that would take the player to the floor snaps the chain instead. Returns whether the session lives. */
    private static boolean deal(ServerLevel level, Session s, ServerPlayer player, float amount) {
        if (player.getHealth() - amount <= HP_FLOOR) {
            snap(level, s, "health");
            return false;
        }
        player.invulnerableTime = 0;
        player.hurt(new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(BINDING_DAMAGE)), amount);
        return true;
    }

    // ------------------------------------------------------------------ ending
    /** Ends the binding because the craft stopped (success is flagged by the crafted event; failures by their own cause). */
    private static void close(ServerLevel level, Session s) {
        ServerPlayer player = findPlayer(level.getServer(), s.player);
        end(level, s);
        if (player != null) {
            freeFromYank(level, s, player);
        }
    }

    /** A craft that ends while the binder is still shackled by a Yank sets them free at once. */
    private static void freeFromYank(ServerLevel level, Session s, ServerPlayer player) {
        if (level.getGameTime() < s.yankedUntil) {
            Chained.free(player);
            s.yankedUntil = 0L;
        }
    }

    // ------------------------------------------------------------------ the Yank
    /**
     * The hook of the matrix mixin: the matrix is about to roll a native instability outcome. Returns whether the original should run.
     * A rite a snap completed swallows it (once); a bound rite turns it into a Yank (a trigger during a Yank is swallowed, not stacked);
     * every other matrix keeps its native outcomes untouched.
     */
    public static boolean onInstability(ServerLevel level, BlockPos matrix) {
        GlobalPos key = GlobalPos.of(level.dimension(), matrix.immutable());
        Long until = COMPLETING.remove(key);
        if (until != null && level.getGameTime() <= until) {
            grantInstabilityResearch(level, matrix);
            return false;
        }
        if (ACTIVE.isEmpty()) {
            return true;
        }
        Session s = data(level).get(matrix);
        ServerPlayer player = s == null ? null : findPlayer(level.getServer(), s.player);
        if (s == null || player == null || !player.isAlive() || pedestalItem(level, matrix.below(2)).isEmpty()) {
            return true;   // no live binding, or the catalyst is gone (the native failure runs)
        }
        grantInstabilityResearch(level, matrix);
        if (s.telegraph == 0 && !Chained.isChained(player)) {
            s.telegraph = YANK_TELEGRAPH;
            data(level).setDirty();
            send(level, s, BindingPayload.YANK, "", BlockPos.ZERO);
            level.playSound(null, matrix, TCSounds.SHACKLES.get(), SoundSource.BLOCKS, 1.0F, 0.5F);
            player.displayClientMessage(Component.translatable("message." + ThaumicConcilium.MODID + ".binding_yank"), true);
        }
        return false;
    }

    /** What Thaumaturge's own trigger does before it rolls an outcome (its method is private): players within ten blocks discover Instability. */
    private static void grantInstabilityResearch(ServerLevel level, BlockPos matrix) {
        ResourceLocation research = com.leclowndu93150.thaumaturge.TTIds.rl("instability");
        for (ServerPlayer near : level.getEntitiesOfClass(ServerPlayer.class, new net.minecraft.world.phys.AABB(matrix).inflate(10.0))) {
            if (!com.leclowndu93150.thaumaturge.api.capability.PlayerResearchAccess.of(near).knows(research)
                    && com.leclowndu93150.thaumaturge.content.research.ResearchManager.complete(near, research)) {
                near.displayClientMessage(Component.translatable("message.thaumaturge.discovery.instability")
                        .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE), true);
            }
        }
    }

    /** The pull: the binder lands beside the altar and is fully Chained for {@link #YANK_CHAINED} ticks. */
    private static void yank(ServerLevel level, Session s, ServerPlayer player) {
        Vec3 spot = landing(level, s.matrix, player);
        if (spot != null) {
            player.moveTo(spot.x, spot.y, spot.z, player.getYRot(), player.getXRot());
            player.connection.teleport(spot.x, spot.y, spot.z, player.getYRot(), player.getXRot());
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        Chained.chain(player, YANK_CHAINED);
        s.yankedUntil = level.getGameTime() + YANK_CHAINED;
        level.playSound(null, s.matrix, TCSounds.SHACKLES.get(), SoundSource.BLOCKS, 1.0F, 1.2F);
    }

    /** A free standing spot about two blocks from the matrix on the binder's side, or null when none is free (the binder stays put). */
    private static Vec3 landing(ServerLevel level, BlockPos matrix, ServerPlayer player) {
        Vec3 centre = Vec3.atCenterOf(matrix);
        double bearing = Math.atan2(player.getZ() - centre.z, player.getX() - centre.x);
        for (int turn = 0; turn < 12; turn++) {
            double angle = bearing + (turn % 2 == 0 ? 1 : -1) * (turn / 2) * (Math.PI / 6.0);
            double x = centre.x + Math.cos(angle) * 2.0;
            double z = centre.z + Math.sin(angle) * 2.0;
            for (double dy : new double[] {0.0, 1.0, -1.0, 2.0}) {
                net.minecraft.world.phys.AABB box = player.getBoundingBox().move(x - player.getX(), dy, z - player.getZ());
                if (level.noCollision(player, box) && !level.noCollision(player, box.move(0.0, -0.5, 0.0))) {
                    return new Vec3(x, player.getY() + dy, z);
                }
            }
        }
        return null;
    }

    private static void end(ServerLevel level, Session s) {
        BindingData data = data(level);
        data.remove(s.matrix);
        data(level);
        PEDESTALS.remove(GlobalPos.of(level.dimension(), s.matrix));
        send(level, s, BindingPayload.CLOSE, "", BlockPos.ZERO);
    }

    /**
     * The chain snaps and the rite completes anyway (D1-c2): the Rites on the centre pedestal becomes the Crimson Dagger, as on a
     * success, and a hostile Crimson Raid opens beside the altar ({@link BindingRaid}). The matrix's own next cycle sees the catalyst
     * gone and ends the craft; its native outcome is swallowed ({@link #COMPLETING}). The player gets Weakness I and a line saying why.
     */
    public static void snap(ServerLevel level, Session s, String reason) {
        ServerPlayer player = findPlayer(level.getServer(), s.player);
        if (SNAP_PASSES_CHAIN) {
            ServerPlayer next = nearestOther(level, s);
            if (next != null) {
                if (player != null) {
                    freeFromYank(level, s, player);
                }
                s.player = next.getUUID();
                s.drainTimer = 0;
                data(level).setDirty();
                send(level, s, BindingPayload.OPEN, "", BlockPos.ZERO);
                level.playSound(null, s.matrix, TCSounds.SHACKLES.get(), SoundSource.BLOCKS, 1.0F, 0.8F);
                return;
            }
        }
        BlockPos centre = s.matrix.below(2);
        completeAnyway(level, s, centre);
        send(level, s, BindingPayload.SNAP, "", BlockPos.ZERO);
        level.playSound(null, s.matrix, TCSounds.SHACKLES.get(), SoundSource.BLOCKS, 1.0F, 0.6F);
        if (player != null) {
            freeFromYank(level, s, player);
            if (player.isAlive()) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_TICKS, 0));
                // The raid's first 100 ticks: ritual damage (a zap, an explosion) cannot kill; the raid's own attacks always land.
                PROTECT.put(player.getUUID(), level.getGameTime() + SNAP_PROTECT_TICKS);
            }
            String key = reason.equals("health") || reason.equals("away") ? reason : "other";
            player.displayClientMessage(Component.translatable("message." + ThaumicConcilium.MODID + ".binding_snap." + key), true);
        }
        end(level, s);
        BindingRaid.open(level, s.matrix, s.player, centre);
    }

    /**
     * The snap completes the rite: the Crimson Dagger stands on the centre pedestal where the Rites was (the Rites is consumed), the ring's
     * unpulled ingredients vanish and the essentia still owed is drained from the jars (as much as they hold); the matrix ends the craft
     * on its next cycle.
     */
    private static void completeAnyway(ServerLevel level, Session s, BlockPos centre) {
        for (Map.Entry<BlockPos, String> e : s.ring.entrySet()) {
            if (pedestalItem(level, e.getKey()).isEmpty() || !(level.getBlockEntity(e.getKey()) instanceof BlockEntityPedestal ring)) {
                continue;
            }
            if (BuiltInRegistries.ITEM.getKey(ring.getItem().getItem()).toString().equals(e.getValue())) {
                ring.setItem(ItemStack.EMPTY);
            }
        }
        if (level.getBlockEntity(s.matrix) instanceof BlockEntityInfusionMatrix matrix) {
            com.leclowndu93150.thaumaturge.content.infusion.EssentiaSources sources =
                    new com.leclowndu93150.thaumaturge.content.infusion.EssentiaSources(s.matrix);
            for (com.leclowndu93150.thaumaturge.api.aspect.AspectInstance owed : matrix.remainingEssentia().entries()) {
                for (int i = 0; i < owed.amount(); i++) {
                    if (!sources.drain(level, owed.aspect(), 0)) {
                        break;
                    }
                }
            }
        }
        ItemStack dagger = new ItemStack(TCItems.CRIMSON_DAGGER.get());
        if (level.isLoaded(centre) && level.getBlockEntity(centre) instanceof BlockEntityPedestal ped) {
            ItemStack there = ped.getItem();
            ped.setItem(dagger);
            if (!there.isEmpty() && !there.is(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumaturge:curio_rites")))) {
                drop(level, centre, there);   // somebody swapped something else in: it is not the Rites, so it is not consumed
            }
            COMPLETING.put(GlobalPos.of(level.dimension(), s.matrix), level.getGameTime() + COMPLETING_TTL);
        } else {
            drop(level, centre, dagger);
        }
    }

    private static void drop(ServerLevel level, BlockPos centre, ItemStack stack) {
        ItemEntity drop = new ItemEntity(level, centre.getX() + 0.5, centre.getY() + 1.1, centre.getZ() + 0.5, stack);
        drop.setDeltaMovement(0.0, 0.2, 0.0);
        level.addFreshEntity(drop);
    }

    private static ServerPlayer nearestOther(ServerLevel level, Session s) {
        ServerPlayer best = null;
        double bestD = TETHER * TETHER;
        Vec3 centre = Vec3.atCenterOf(s.matrix);
        for (ServerPlayer p : level.players()) {
            double d = p.position().distanceToSqr(centre);
            if (!p.getUUID().equals(s.player) && p.isAlive() && d < bestD) {
                best = p;
                bestD = d;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ the crafted event: the hard gate and the success flag
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onCrafted(InfusionCraftedEvent event) {
        ServerLevel level = event.getLevel();
        Session s = data(level).get(event.getMatrixPos());
        boolean bound = s != null && s.away == 0;
        if (event.getResult().is(TCItems.CRIMSON_DAGGER.get()) && !bound && HARD_GATE) {
            event.setResult(event.getCatalyst());
            if (event.getPlayer() != null) {
                event.getPlayer().displayClientMessage(Component.translatable("message." + ThaumicConcilium.MODID + ".binding_unbound"), true);
            }
        }
        if (s != null) {
            close(level, s);
        }
    }

    // ------------------------------------------------------------------ the bound player leaves
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!stopping && event.getEntity() instanceof ServerPlayer player) {
            snapBound(player);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            snapBound(player);
        }
    }

    private static void snapBound(ServerPlayer player) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        for (ResourceKey<Level> key : new ArrayList<>(ACTIVE)) {
            ServerLevel level = player.server.getLevel(key);
            if (level == null) {
                continue;
            }
            for (Session s : new ArrayList<>(data(level).sessions().values())) {
                if (s.player.equals(player.getUUID())) {
                    snap(level, s, "other");
                }
            }
        }
    }

    // ------------------------------------------------------------------ lifecycle and sync
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            data(level);
        }
    }

    @SubscribeEvent
    public static void onStarting(ServerStartingEvent event) {
        stopping = false;
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        stopping = true;
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        ACTIVE.clear();
        PENDING.clear();
        PEDESTALS.clear();
        TRACKED.clear();
        PROTECT.clear();
        COMPLETING.clear();
        stopping = false;
    }

    /** A player who starts watching the chunk learns of a binding already running there. */
    @SubscribeEvent
    public static void onChunkSent(ChunkWatchEvent.Sent event) {
        if (ACTIVE.isEmpty() || !ACTIVE.contains(event.getLevel().dimension())) {
            return;
        }
        for (Session s : data(event.getLevel()).sessions().values()) {
            if (new ChunkPos(s.matrix).equals(event.getPos())) {
                PacketDistributor.sendToPlayer(event.getPlayer(), new BindingPayload(s.matrix, s.player, BindingPayload.OPEN, "", BlockPos.ZERO));
                if (!s.demand.isEmpty()) {
                    PacketDistributor.sendToPlayer(event.getPlayer(), new BindingPayload(s.matrix, s.player, BindingPayload.DEMAND, s.demand, BlockPos.ZERO));
                }
            }
        }
    }

    private static void send(ServerLevel level, Session s, byte state, String item, BlockPos target) {
        PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(s.matrix), new BindingPayload(s.matrix, s.player, state, item, target));
    }
}
