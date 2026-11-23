package dev.movingcastle.thaumicconcilium.entity;

import com.leclowndu93150.thaumaturge.api.damagesource.TTDamageSources;
import com.leclowndu93150.thaumaturge.api.taint.ITaintOrigin;
import com.leclowndu93150.thaumaturge.api.taint.TaintApi;
import com.leclowndu93150.thaumaturge.content.taint.flux.PhysicalFlux;
import dev.movingcastle.thaumicconcilium.world.VesselResearch;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.leclowndu93150.thaumaturge.content.entity.EntityTaintSwarm;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.block.AnchorBlock;
import dev.movingcastle.thaumicconcilium.block.VerdantCapitalBlockEntity;
import dev.movingcastle.thaumicconcilium.compat.SablePull;
import dev.movingcastle.thaumicconcilium.registry.TCBlocks;
import dev.movingcastle.thaumicconcilium.registry.TCEffects;
import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import dev.movingcastle.thaumicconcilium.rift.Hexes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * The Vessel (phase 5c). A hex that breaks leaves it dormant three blocks above the hex; a player within 16 blocks, or a wand used on
 * an anchor, wakes it. It then pulls everything toward its centre, is fed by what it eats, and is starved by players who draw from the
 * four anchors with a wand (memo {@code docs/design/5c-vessel-fight.md}). Players are pulled on their own client (a hook set by the
 * client; the server never sets a player's velocity for the pull); mobs, items and projectiles by the server.
 *
 * <p>Cost: dormant, one {@code hasNearbyAlivePlayer} a second; active, one entity query a tick, up to four tether checks, a disc test
 * every 10 ticks, and a block, a presence sample and a sub-level query a second.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public class Vessel extends Entity implements ITaintOrigin {
    public static final byte DORMANT = 0;
    public static final byte WAKING = 1;
    public static final byte ACTIVE = 2;
    public static final byte SUBDUED = 3;
    /** The finale (phase 5d cycle 4e): the last unit has drained; the black swells over the crater, then collapses. See {@link VesselDeath}. */
    public static final byte DYING = 4;

    /** Blocks it eats, one a second, within 3 of its centre (no drops). */
    public static final TagKey<Block> EDIBLE = TagKey.create(Registries.BLOCK, ThaumicConcilium.id("vessel_edible"));
    /** Entities the pull never touches (bosses, its own clots). */
    public static final TagKey<EntityType<?>> IMMUNE = TagKey.create(Registries.ENTITY_TYPE, ThaumicConcilium.id("vessel_pull_immune"));
    private static final String SWARM_KEY = "thaumicconcilium_vessel";
    private static final String SWARM_BORN = "thaumicconcilium_vessel_born";

    /** Set by the client (its mod setup): pulls the local player toward the hole. Null on a server. */
    @Nullable
    public static Consumer<Vessel> clientPull;

    /** Client hooks (set in {@code TCClient}; null on a server): a tick each client tick, and a synced field changing ("state", "swell", "pull", "mass", "tether"). */
    @Nullable
    public static Consumer<Vessel> clientTick;
    @Nullable
    public static java.util.function.BiConsumer<Vessel, String> clientEvent;
    /** Client-side look state (the renderer and its effects own these; plain fields so common code names no client class). */
    public long fxSwellStart = Long.MIN_VALUE / 2;
    public long fxStaggerStart = Long.MIN_VALUE / 2;
    public float fxShown = -1.0F;
    /** Eased centre height of each anchor's hovering shard above its block base (client only; negative: no shard). */
    public final float[] fxShard = {-1.0F, -1.0F, -1.0F, -1.0F};
    public float fxShownPrev = -1.0F;
    public int fxLastMass = -1;
    public float fxLastPull = -1.0F;
    public byte fxLastState = -1;
    public final UUID[] fxLastTether = new UUID[4];
    public Object fxHum;
    public long fxEatAt = Long.MIN_VALUE / 2;
    private boolean qaTether;

    private static final EntityDataAccessor<Integer> BURDEN =SynchedEntityData.defineId(Vessel.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MASS = SynchedEntityData.defineId(Vessel.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> START = SynchedEntityData.defineId(Vessel.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> STATE = SynchedEntityData.defineId(Vessel.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> PULL = SynchedEntityData.defineId(Vessel.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> SWELL = SynchedEntityData.defineId(Vessel.class, EntityDataSerializers.BOOLEAN);
    /** Units of charge left once it is subdued (cycle 4); 0 before. */
    private static final EntityDataAccessor<Integer> CHARGE = SynchedEntityData.defineId(Vessel.class, EntityDataSerializers.INT);
    /** Game time the break sequence began (cycle 3c), -1 when none runs; and whether a Sable sub-level is playing the hex. */
    private static final EntityDataAccessor<Long> BREAK_START = SynchedEntityData.defineId(Vessel.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> BREAK_SUB = SynchedEntityData.defineId(Vessel.class, EntityDataSerializers.BOOLEAN);
    /** The finale: game time it began (-1 none), the swell's radius, and the age (ticks) the collapse was decided at (-1 undecided). */
    private static final EntityDataAccessor<Long> DYING_START = SynchedEntityData.defineId(Vessel.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Float> DYING_RS = SynchedEntityData.defineId(Vessel.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DYING_C = SynchedEntityData.defineId(Vessel.class, EntityDataSerializers.INT);
    /** Tiers 3-4: the ages the light stage began and the light bursts (-1 none), and the pond spot the light rises from. The collapse (DYING_C) follows the stage. */
    private static final EntityDataAccessor<Integer> DYING_LIGHT = SynchedEntityData.defineId(Vessel.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DYING_BURST = SynchedEntityData.defineId(Vessel.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<BlockPos> DYING_POND = SynchedEntityData.defineId(Vessel.class, EntityDataSerializers.BLOCK_POS);
    /** Whether this ending paints a grove (tiers 3-4), known from the start: the client turns the horizon to the beam's cyan for those only. */
    private static final EntityDataAccessor<Boolean> DYING_GROVE = SynchedEntityData.defineId(Vessel.class, EntityDataSerializers.BOOLEAN);
    @SuppressWarnings("unchecked")
    private static final EntityDataAccessor<Optional<UUID>>[] TETHER = new EntityDataAccessor[4];
    @SuppressWarnings("unchecked")
    private static final EntityDataAccessor<Optional<BlockPos>>[] ANCHOR = new EntityDataAccessor[4];

    static {
        for (int i = 0; i < 4; i++) {
            TETHER[i] = SynchedEntityData.defineId(Vessel.class, EntityDataSerializers.OPTIONAL_UUID);
            ANCHOR[i] = SynchedEntityData.defineId(Vessel.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
        }
    }

    private final Map<UUID, Integer> contributors = new LinkedHashMap<>();
    private final List<BlockPos> anchors = new ArrayList<>();
    private BlockPos hex = BlockPos.ZERO;

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.thaumicconcilium.vessel"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
    private final Set<UUID> participants = new HashSet<>();
    private final Map<UUID, Integer> presence = new HashMap<>();
    private final UUID[] tetherOwner = new UUID[4];
    private final int[] tetherProgress = new int[4];
    private final int[] tetherAge = new int[4];
    private final int[] draws = new int[4];
    private final long[] cooldownUntil = new long[4];
    private int missedClots;
    private boolean frozen;
    private boolean clotsEnabled = true;
    private int dormantTest;
    private int gulpIntervalTest;
    private boolean anchorsDirty = true;
    private int wakeTimer;
    private int emptyTicks;
    private int clotTimer;
    private int pairGap;
    private int staggerLeft;
    private int gulpTimer;
    private int gulpPhase;
    private int gulpLeft;
    private int swarmTimer;
    private int lastPhase = -1;
    private long gainWindowStart = Long.MIN_VALUE / 2;
    private int gainedInWindow;
    private List<?> ships = Collections.emptyList();
    // the Quiescent Vessel and the thaumaturge's rite (cycle 4)
    private int drained;
    private int spilled;
    private boolean motesGiven;
    private final Map<UUID, Integer> subdueMotes = new java.util.LinkedHashMap<>();
    private boolean riteActive;
    /** A tether is held: Thaumaturge sees a living taint origin and the pulses run (a capital alone gives neither). */
    private boolean originOn;
    /** Consecutive ticks each capped anchor has been without the mana for a tick of its unit. */
    private final int[] starve = new int[4];
    /** Capitals standing on the anchors when the last unit drained (cycle 2's ending reads it). */
    private int capitalCount;
    private float taintReserve;
    /** The finale: the doomed seed's id (the painting job is found by it), whether this session started the finale, and who stood inside at the flash. */
    private boolean dyingLive;
    private final List<UUID> flashInside = new ArrayList<>();
    private final List<UUID> flashOutside = new ArrayList<>();

    public Vessel(EntityType<? extends Vessel> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.bossEvent.setVisible(false);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(BURDEN, 0).define(MASS, 0).define(START, 0).define(STATE, DORMANT).define(PULL, 0.0F).define(SWELL, false).define(CHARGE, 0)
                .define(BREAK_START, -1L).define(BREAK_SUB, false).define(DYING_START, -1L).define(DYING_RS, 0.0F).define(DYING_C, -1)
                .define(DYING_LIGHT, -1).define(DYING_BURST, -1).define(DYING_POND, BlockPos.ZERO).define(DYING_GROVE, false);
        for (int i = 0; i < 4; i++) {
            builder.define(TETHER[i], Optional.empty()).define(ANCHOR[i], Optional.empty());
        }
    }

    // ---------------------------------------------------------------- facts (also for the renderer and the tests)
    /** The burden the hex held when it broke (kept from cycle 1; the Vessel's Mass is not derived from it). */
    public int burdenAtBreak() {
        return this.entityData.get(BURDEN);
    }

    /** Contribution per player UUID at the break (present or absent). */
    public Map<UUID, Integer> contributors() {
        return this.contributors;
    }

    public List<BlockPos> anchors() {
        return this.anchors;
    }

    /** The master position of the hex that broke. */
    public BlockPos hexPos() {
        return this.hex;
    }

    /** The hole's centre: the entity's position (the 3 x 3 body stands on it). */
    public Vec3 holeCentre() {
        return this.position();
    }

    public byte state() {
        return this.entityData.get(STATE);
    }

    /** Mass in quarter points. */
    public int massQuarters() {
        return this.entityData.get(MASS);
    }

    public int startQuarters() {
        return this.entityData.get(START);
    }

    public double mass() {
        return this.massQuarters() / 4.0;
    }

    public float pullScale() {
        return this.entityData.get(PULL);
    }

    /** True during the wake-up swell and the gulp's 60-tick swell (the renderer's flag). */
    public boolean swelling() {
        return this.entityData.get(SWELL);
    }

    public boolean fightIsOn() {
        byte s = this.state();
        return s == WAKING || s == ACTIVE;
    }

    /** 0 Feeding, 1 Spin-up, 2 Collapse. */
    public int phase() {
        return VesselPull.phase(this.massQuarters(), TCConfig.SPINUP_MASS.get(), TCConfig.COLLAPSE_MASS.get());
    }

    public int missedClots() {
        return this.missedClots;
    }

    public void countMissedClot() {
        this.missedClots++;
    }

    public Set<UUID> participants() {
        return this.participants;
    }

    public Map<UUID, Integer> presence() {
        return this.presence;
    }

    public void addParticipant(UUID id) {
        this.participants.add(id);
    }

    @Nullable
    public UUID tetherOwner(int anchor) {
        return anchor >= 0 && anchor < 4 ? this.tetherOwner[anchor] : null;
    }

    /** Synced owner of a tether (the client's copy, for the renderer). */
    public Optional<UUID> syncedTether(int anchor) {
        return this.entityData.get(TETHER[anchor]);
    }

    /** Synced anchor position (the client's copy, for the renderer). */
    public Optional<BlockPos> syncedAnchor(int anchor) {
        return this.entityData.get(ANCHOR[anchor]);
    }

    public int drawsDone(int anchor) {
        return this.draws[anchor];
    }

    public long cooldownUntil(int anchor) {
        return this.cooldownUntil[anchor];
    }

    public int tetherProgress(int anchor) {
        return this.tetherProgress[anchor];
    }

    public int staggerLeft() {
        return this.staggerLeft;
    }

    public int gulpPhase() {
        return this.gulpPhase;
    }

    public boolean frozen() {
        return this.frozen;
    }

    // ---------------------------------------------------------------- the Quiescent Vessel (cycle 4)
    /** Units of charge left to drain (0 until it is subdued, and again once spent). */
    public int charge() {
        return this.entityData.get(CHARGE);
    }

    /** Test and QA hook: sets the charge directly. */
    public void setCharge(int charge) {
        this.entityData.set(CHARGE, Math.max(0, charge));
    }

    public int drained() {
        return this.drained;
    }

    /** Goo spills at the anchors in use so far (part of the Taint Seed's score). */
    public int spilled() {
        return this.spilled;
    }

    public boolean motesGiven() {
        return this.motesGiven;
    }

    /** Phase 5d: what each participant was given (or is owed) at the subdue, for the ending's top-up roll. */
    public Map<UUID, Integer> subdueMotes() {
        return this.subdueMotes;
    }

    /** Whether a drain is under way (somebody holds a tether on the subdued Vessel). */
    public boolean riteActive() {
        return this.riteActive;
    }

    /** Ticks of the unit one anchor's drawer has put in (kept across a pause). */
    public int riteProgress(int anchor) {
        return this.tetherProgress[anchor];
    }

    /** Ticks a capped anchor has been starved in a row. */
    public int starveTicks(int anchor) {
        return this.starve[anchor];
    }

    public void setStarveTicks(int anchor, int ticks) {
        this.starve[anchor] = ticks;
    }

    /** Capitals standing when the last unit drained (0 until then). */
    public int capitalCount() {
        return this.capitalCount;
    }

    /** Test hook: sets one anchor's progress toward the next unit. */
    public void setRiteProgress(int anchor, int ticks) {
        this.tetherProgress[anchor] = ticks;
    }

    /** A living taint origin only while a drain is under way: Thaumaturge sustains taint around it, and lets it wither once it stops. */
    @Override
    public boolean isLiveTaintOrigin() {
        return this.originOn && this.state() == SUBDUED && !this.isRemoved();
    }

    @Override
    public float getTaintReserve() {
        return this.taintReserve;
    }

    @Override
    public void setTaintReserve(float reserve) {
        this.taintReserve = Math.max(0.0F, reserve);
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    public void setup(int burden, Map<UUID, Integer> contributors, List<BlockPos> anchors, BlockPos hex) {
        this.entityData.set(BURDEN, burden);
        this.contributors.clear();
        this.contributors.putAll(contributors);
        this.anchors.clear();
        this.anchors.addAll(anchors);
        this.hex = hex;
        this.syncAnchors();
        if (this.level() instanceof ServerLevel level) { // four different shard looks, seeded by this Vessel's UUID
            int[] shapes = AnchorBlock.shapesFor(this.getUUID());
            for (int i = 0; i < this.anchors.size() && i < 4; i++) {
                BlockState now = level.getBlockState(this.anchors.get(i));
                if (now.is(TCBlocks.ANCHOR.get())) {
                    level.setBlock(this.anchors.get(i), now.setValue(AnchorBlock.SHAPE, shapes[i]), 3);
                }
            }
        }
    }

    private void syncAnchors() {
        for (int i = 0; i < 4; i++) {
            this.entityData.set(ANCHOR[i], i < this.anchors.size() ? Optional.of(this.anchors.get(i)) : Optional.empty());
        }
    }

    // ---------------------------------------------------------------- the pull (shared by the client hook and the tests)
    /** A player's personal factor (memo table); reads only what the client also knows. */
    public static double playerFactor(Player p) {
        boolean immune = p.isSpectator() || p.getAbilities().flying || p.isPassenger() || p.getType().is(IMMUNE);
        boolean flung = p.hasEffect(TCEffects.FLUNG);
        boolean aboard = aboardShip(p);
        boolean tethered = p.hasEffect(TCEffects.TETHERED);
        boolean sheltered = p.onGround() && p.level().getBlockState(BlockPos.containing(p.getX(), p.getY() - 0.2, p.getZ())).is(TCBlocks.ANCHOR.get());
        return VesselPull.playerFactor(immune, flung, aboard, tethered, sheltered, p.isShiftKeyDown(), TCConfig.PULL_SNEAK.get(), TCConfig.PULL_TETHERED.get());
    }

    /** The velocity to add to a player this tick. Called on the player's own client; the server calls it only for tests. */
    public Vec3 pullOn(Player p) {
        float scale = this.pullScale();
        if (scale <= 0.0F || !this.fightIsOn()) {
            return Vec3.ZERO;
        }
        Vec3 to = this.holeCentre().subtract(p.getBoundingBox().getCenter());
        double d = to.length();
        double a = VesselPull.accel(d, TCConfig.PULL_ACCEL.get(), TCConfig.PULL_RANGE.get());
        if (a <= 0.0 || d < 1.0e-6) {
            return Vec3.ZERO;
        }
        double f = playerFactor(p);
        return f <= 0.0 ? Vec3.ZERO : to.scale(1.0 / d).scale(a * scale * f);
    }

    private static boolean sableLoaded() {
        return Holder.SABLE;
    }

    private static final class Holder {
        static final boolean SABLE = ModList.get() != null && ModList.get().isLoaded("sable");
    }

    // ---------------------------------------------------------------- the break sequence (cycle 3c, timeline in VesselBreak)
    @Nullable
    private UUID breakSub;
    @Nullable
    private Vec3 breakBase;
    private boolean breakBurst;
    private boolean breakInterrupted;
    private int breakSubRetries;
    /** The four spokes the shards fly out along ({angle, distance}); the landing spots are found on them at the burst. */
    @Nullable
    private double[][] breakSpokes;
    /** Whether the burst carves a crater (always in play; the pack test turns it off for tests that share an arena). */
    private boolean breakCrater;
    /** The crater still being carved, if any (see {@link VesselCrater}); it outlives the sequence by a second or two. */
    @Nullable
    private VesselCrater crater;
    private boolean craterLoaded;

    /** Whether the break sequence is still playing: the anchors are not down yet and the Vessel cannot be woken. */
    public boolean breaking() {
        return this.entityData.get(BREAK_START) >= 0L;
    }

    /** Ticks since the break began (with the partial tick), or -1 when no sequence runs. */
    public float breakAge(float partialTick) {
        long start = this.entityData.get(BREAK_START);
        return start < 0L ? -1.0F : (float) (this.level().getGameTime() - start) + partialTick;
    }

    /** Whether a Sable sub-level is showing the hex (then the renderer draws no stand-in). */
    public boolean breakHasSubLevel() {
        return this.entityData.get(BREAK_SUB);
    }

    /** The sub-level still to be removed, if any (for the pack test). */
    @Nullable
    public UUID breakSubLevel() {
        return this.breakSub;
    }

    /** The crater still being carved, or null (for the pack test). */
    @Nullable
    public VesselCrater crater() {
        return this.crater;
    }

    /** The radius the burst's crater has for these spokes: two blocks past the farthest landing. */
    public static double craterRadius(double[][] spokes) {
        double far = 0.0;
        for (double[] s : spokes) {
            far = Math.max(far, s[1]);
        }
        return far + 2.0;
    }

    /**
     * Starts the sequence. {@code sub} and {@code base}: the hex's Sable sub-level and where its pose began, or null for the
     * stand-in. {@code spokes}: where the four shards will go. {@code crater}: whether the burst carves the crater.
     */
    public void startBreak(ServerLevel level, @Nullable UUID sub, @Nullable Vec3 base, double[][] spokes, boolean crater) {
        this.breakSub = sub;
        this.breakBase = base;
        this.breakSpokes = spokes;
        this.breakCrater = crater;
        this.breakBurst = false;
        this.breakInterrupted = false;
        this.entityData.set(BREAK_SUB, sub != null);
        this.entityData.set(BREAK_START, level.getGameTime());
    }

    private void tickBreak(ServerLevel level) {
        long age = level.getGameTime() - this.entityData.get(BREAK_START);
        if (this.breakInterrupted || age < 0L || age >= VesselBreak.END) {
            this.finishBreak(level, !this.breakInterrupted);
            return;
        }
        if (age < VesselBreak.BURST) {
            if (this.breakSub != null && this.breakBase != null) {
                Vec3 at = this.breakBase.add(0.0, VesselBreak.rise(age) * (this.getY() - this.breakBase.y), 0.0);
                dev.movingcastle.thaumicconcilium.compat.SableBreak.hold(level, this.breakSub, at, VesselBreak.yaw(age), VesselBreak.tilt(age));
            }
        } else if (!this.breakBurst) {
            this.breakBurst = true;
            this.removeBreakSubLevel(level);
            this.land(level, false);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
                    net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 0.6F);
        }
    }

    /**
     * The burst: the crater begins (a ring spreading from the centre; nothing is dug ahead of it) and the four landing spots are
     * found on the ground the ring will leave, so the shards fly to where they will stand and land after it has passed.
     * {@code instant}: the whole crater now (a break finished on load), no blast.
     */
    private void land(ServerLevel level, boolean instant) {
        if (this.breakSpokes == null || !this.anchors.isEmpty()) {
            return;
        }
        if (this.breakCrater && this.crater == null) {
            this.crater = new VesselCrater(this.hex, craterRadius(this.breakSpokes), this.getUUID().getLeastSignificantBits());
            if (instant) {
                this.crater.finish(level);
            } else {
                this.blast(level, this.crater.radius());
            }
        }
        this.anchors.addAll(dev.movingcastle.thaumicconcilium.rift.HexSink.landingSpots(level, this.hex, this.breakSpokes,
                instant ? null : this.crater));
        this.syncAnchors();
    }

    /**
     * The burst's blast: every living thing within the crater's radius of the hex (and 8 blocks above or below it) is hurt and
     * thrown outward and up, by {@link VesselBreak#blast} of its distance: explosion damage (armour and Blast Protection apply)
     * of {@code vessel.break.blastDamage} at the centre, a throw of {@code vessel.break.blastKnockback}. Nothing beyond the radius.
     */
    private void blast(ServerLevel level, double radius) {
        Vec3 centre = Vec3.atBottomCenterOf(this.hex);
        DamageSource source = level.damageSources().explosion(this, null);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(this.hex).inflate(radius, 8.0, radius), x -> x.isAlive() && !x.isSpectator())) {
            Vec3 off = e.position().subtract(centre);
            double k = VesselBreak.blast(Math.sqrt(off.x * off.x + off.z * off.z), radius);
            if (k <= 0.0) {
                continue;
            }
            e.hurt(source, (float) (TCConfig.BLAST_DAMAGE.get() * k));
            e.setDeltaMovement(e.getDeltaMovement().add(VesselBreak.blastPush(off, radius, TCConfig.BLAST_KNOCKBACK.get())));
            e.hurtMarked = true;
        }
    }

    /** Ends the sequence: no sub-level, the four anchors on their spots with their shapes. {@code impact}: with dust and sound. */
    private void finishBreak(ServerLevel level, boolean impact) {
        this.removeBreakSubLevel(level);
        this.land(level, !impact);
        if (!impact && this.crater != null) {
            this.crater.finish(level);
        }
        int[] shapes = AnchorBlock.shapesFor(this.getUUID());
        for (int i = 0; i < this.anchors.size() && i < 4; i++) {
            BlockPos pos = this.anchors.get(i);
            BlockState state = TCBlocks.ANCHOR.get().defaultBlockState().setValue(AnchorBlock.SHAPE, shapes[i]);
            if (!level.getBlockState(pos).is(TCBlocks.ANCHOR.get())) {
                level.setBlock(pos, state, 3);
            }
            if (impact) {
                level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, state),
                        pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 20, 0.4, 0.2, 0.4, 0.1);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 8, 0.4, 0.1, 0.4, 0.03);
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.DEEPSLATE_BREAK, net.minecraft.sounds.SoundSource.BLOCKS, 1.4F, 0.6F);
            }
        }
        this.breakInterrupted = false;
        this.breakBurst = false;
        this.breakSpokes = null;
        this.entityData.set(BREAK_START, -1L);
        this.anchorsDirty = true;
    }

    /** The crater's queue: a budget of columns a tick; all that is left at once after a load. */
    private void tickCrater(ServerLevel level) {
        if (this.craterLoaded) {
            this.craterLoaded = false;
            this.crater.finish(level);
        } else {
            this.crater.tick(level);
        }
        if (this.crater.done()) {
            this.crater = null;
        }
    }

    /** Removes the hex's sub-level if one is left. Not loaded yet (a break finished on load): it is tried again once a second. */
    private void removeBreakSubLevel(ServerLevel level) {
        this.entityData.set(BREAK_SUB, false);
        if (this.breakSub == null) {
            return;
        }
        if (!sableLoaded() || dev.movingcastle.thaumicconcilium.compat.SableBreak.remove(level, this.breakSub) || ++this.breakSubRetries > 60) {
            this.breakSub = null;
            this.breakSubRetries = 0;
        }
    }

    private static boolean aboardShip(Entity e) {
        return sableLoaded() && SablePull.aboard(e);
    }

    // ---------------------------------------------------------------- tick
    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            if (clientTick != null) {
                clientTick.accept(this);
            }
            if (clientPull != null && this.fightIsOn() && this.pullScale() > 0.0F) {
                clientPull.accept(this);
            }
            return;
        }
        if (this.frozen) {
            if (this.qaTether && this.tickCount % 20 == 1) { // QA only: the nearest player draws from anchor 0 (the tether's look)
                ServerPlayer near = null;
                for (ServerPlayer p : players(level)) {
                    if (near == null || p.distanceToSqr(this) < near.distanceToSqr(this)) {
                        near = p;
                    }
                }
                this.entityData.set(TETHER[0], near == null ? Optional.empty() : Optional.of(near.getUUID()));
            }
            return; // QaFrozen: no pull, no timers, state held
        }
        boolean wasBreaking = this.breaking();
        if (wasBreaking) {
            this.tickBreak(level);
        }
        if (this.crater != null) {
            this.tickCrater(level); // from the burst on, while the shards fly and for a second or two after
        }
        if (wasBreaking) {
            return; // dormant and not wakeable until the anchors are down
        }
        if (this.breakSub != null && this.tickCount % 20 == 0) {
            this.removeBreakSubLevel(level);
        }
        if (this.anchorsDirty) {
            this.anchorsDirty = false;
            for (int i = 0; i < this.anchors.size() && i < 4; i++) {
                this.syncAnchor(level, i);
            }
        }
        switch (this.state()) {
            case DORMANT -> {
                if (this.tickCount % 20 == 0 && anyNear(level, this, TCConfig.WAKE_RANGE.get())) {
                    this.wake(level);
                }
            }
            case WAKING -> {
                this.wakeTimer++;
                this.slowChecks(level);
                if (this.state() == WAKING && this.wakeTimer >= TCConfig.WAKE_SWELL.get()) {
                    this.entityData.set(STATE, ACTIVE);
                    this.entityData.set(SWELL, false);
                }
            }
            case ACTIVE -> this.activeTick(level);
            case SUBDUED -> this.riteTick(level);
            case DYING -> this.dyingTick(level);
            default -> { }
        }
    }

    /** Test and QA hook: sets the state and the Mass (quarters) directly, as the harness's NBT does. */
    public void setFightState(byte state, int startQuarters, int massQuarters) {
        this.entityData.set(START, startQuarters);
        this.entityData.set(MASS, massQuarters);
        this.entityData.set(STATE, state);
        this.entityData.set(SWELL, false);
        this.entityData.set(PULL, state == ACTIVE ? 1.0F : 0.0F);
        this.updateBar();
    }

    /** Test hook (not saved): shorter dormancy and gulp intervals, so a test need not wait 1200 or 400 ticks. 0 keeps the config. */
    public void setTestTimers(int dormantTicks, int gulpInterval) {
        this.dormantTest = dormantTicks;
        this.gulpIntervalTest = gulpInterval;
    }

    /** Test hook: an anchor's draw count and cooldown end (game time), as a save would hold them. */
    public void setAnchorState(int anchor, int draws, long cooldownUntil) {
        this.draws[anchor] = draws;
        this.cooldownUntil[anchor] = cooldownUntil;
    }

    /** Test hook (not saved): whether it throws clots at tethered players. */
    public void setClotsEnabled(boolean enabled) {
        this.clotsEnabled = enabled;
    }

    /** Marks an entity as this Vessel's swarm, born at a game time (also what a pack test uses). */
    public void tagSwarm(Entity swarm, long born) {
        swarm.getPersistentData().putUUID(SWARM_KEY, this.getUUID());
        swarm.getPersistentData().putLong(SWARM_BORN, born);
    }

    /** Wakes it (a player came near, or a wand touched an anchor). Fixes the start Mass the first time. */
    public void wake(ServerLevel level) {
        if (this.state() != DORMANT || this.breaking()) {
            return;
        }
        if (this.startQuarters() == 0) {
            int near = 0;
            double r = TCConfig.PLAYER_RANGE.get();
            for (ServerPlayer p : players(level)) {
                if (!p.isSpectator() && p.distanceToSqr(this) <= r * r) {
                    near++;
                }
            }
            int start = VesselPull.startMass(near, TCConfig.START_MASS.get(), TCConfig.START_MASS_PER_PLAYER.get(), TCConfig.START_MASS_CAP.get()) * 4;
            this.entityData.set(START, start);
            this.entityData.set(MASS, start);
        }
        this.wakeTimer = 0;
        this.emptyTicks = 0;
        this.entityData.set(STATE, WAKING);
        this.entityData.set(SWELL, true);
        this.updateBar();
    }

    private void activeTick(ServerLevel level) {
        long t = level.getGameTime();
        int phase = this.phase();
        this.runTimers(phase);
        this.entityData.set(PULL, this.computePull());
        this.tickTethers(level, t);
        this.pullEntities(level, t);
        if (this.state() != ACTIVE) {
            return;
        }
        this.spawnThings(level, t, phase);
        if (this.pullScale() > 0.0F && !this.ships.isEmpty() && sableLoaded()) {
            SablePull.pull(this.holeCentre(), this.pullScale(), this.ships);
        }
        this.slowChecks(level);
    }

    private float computePull() {
        if (this.staggerLeft > 0) {
            return 0.0F;
        }
        return this.gulpPhase == 2 ? (float) (double) TCConfig.GULP_SCALE.get() : 1.0F;
    }

    private void runTimers(int phase) {
        if (this.staggerLeft > 0) {
            this.staggerLeft--;
        }
        if (phase != 2) {
            this.resetGulp();
            return;
        }
        if (this.staggerLeft > 0) {
            return;
        }
        switch (this.gulpPhase) {
            case 0 -> {
                if (++this.gulpTimer >= (this.gulpIntervalTest > 0 ? this.gulpIntervalTest : TCConfig.GULP_INTERVAL.get())) {
                    this.gulpPhase = 1;
                    this.gulpLeft = TCConfig.GULP_SWELL.get();
                    this.entityData.set(SWELL, true);
                }
            }
            case 1 -> {
                if (--this.gulpLeft <= 0) {
                    this.gulpPhase = 2;
                    this.gulpLeft = TCConfig.GULP_TICKS.get();
                    this.entityData.set(SWELL, false);
                }
            }
            default -> {
                if (--this.gulpLeft <= 0) {
                    this.resetGulp();
                }
            }
        }
    }

    private void resetGulp() {
        this.gulpPhase = 0;
        this.gulpTimer = 0;
        this.gulpLeft = 0;
        if (this.state() == ACTIVE) {
            this.entityData.set(SWELL, false);
        }
    }

    /** A batted clot reached the horizon: no pull for 80 ticks, double draws, a pending gulp cancelled. Refreshes, never stacks. */
    public void stagger() {
        this.staggerLeft = TCConfig.STAGGER_TICKS.get();
        this.resetGulp();
        this.entityData.set(PULL, 0.0F);
    }

    // ---------------------------------------------------------------- the entity query: pull, horizon, eating, the disc
    private void pullEntities(ServerLevel level, long t) {
        double range = TCConfig.PULL_RANGE.get();
        Vec3 hole = this.holeCentre();
        double horizon = TCConfig.HORIZON.get();
        float scale = this.pullScale();
        boolean discTick = this.tickCount % TCConfig.DISC_INTERVAL.get() == 0;
        AABB box = new AABB(hole, hole).inflate(range);
        // players: only the horizon and the disc (the pull on them is the client's own)
        for (ServerPlayer p : players(level)) {
            if (p.isSpectator() || !p.isAlive() || p.level() != level || p.getType().is(IMMUNE)) {
                continue;
            }
            double d = p.getBoundingBox().getCenter().distanceTo(hole);
            if (d <= horizon && !p.hasEffect(TCEffects.FLUNG)) {
                this.horizon(level, p);
            }
            if (discTick && d <= TCConfig.DISC_OUTER.get() + 2.0 && this.inDisc(p, t)) {
                p.addEffect(new MobEffectInstance(TTMobEffects.FLUX_TAINT, TCConfig.DISC_TAINT_TICKS.get()));
            }
        }
        List<Entity> found = level.getEntities(this, box, e -> !e.isRemoved() && !(e instanceof Player) && (e instanceof ItemEntity
                || e instanceof Projectile || (e instanceof LivingEntity && !(e instanceof ArmorStand))) && !e.getType().is(IMMUNE));
        for (Entity e : found) {
            Vec3 centre = e.getBoundingBox().getCenter();
            double d = centre.distanceTo(hole);
            boolean own = this.ownSwarm(e);
            if (d <= horizon) {
                int units = (int) Math.round((own ? TCConfig.EAT_SWARM.get() : e instanceof LivingEntity ? TCConfig.EAT_MOB.get() : TCConfig.EAT_ITEM.get()) * 4.0);
                e.discard();
                this.eat(units, t);
                if (this.state() != ACTIVE) {
                    return;
                }
                continue;
            }
            if (scale <= 0.0F || e.isPassenger() || aboardShip(e)) {
                continue;
            }
            double a = VesselPull.accel(d, TCConfig.PULL_ACCEL.get(), range) * scale;
            if (own) {
                a *= VesselPull.swarmFactor(t - e.getPersistentData().getLong(SWARM_BORN), TCConfig.PULL_SWARM_AGE.get(),
                        TCConfig.PULL_SWARM.get(), TCConfig.PULL_SWARM_OLD.get());
            }
            if (a > 0.0 && d > 1.0e-6) {
                e.setDeltaMovement(e.getDeltaMovement().add(hole.subtract(centre).scale(a / d)));
                e.hasImpulse = true;
            }
        }
    }

    private boolean ownSwarm(Entity e) {
        return e instanceof EntityTaintSwarm && e.getPersistentData().hasUUID(SWARM_KEY) && e.getPersistentData().getUUID(SWARM_KEY).equals(this.getUUID());
    }

    private boolean inDisc(Player p, long t) {
        Vec3 rel = p.getBoundingBox().getCenter().subtract(this.holeCentre());
        return VesselPull.inDisc(rel.x, rel.y, rel.z, 0.3 + p.getBbHeight() / 2.0, t, TCConfig.DISC_TILT.get(), TCConfig.DISC_PRECESSION.get(),
                TCConfig.DISC_INNER.get(), TCConfig.DISC_OUTER.get());
    }

    /** The horizon throw: damage (never lethal), a warp, Flux Taint, thrown clear, no pull for a while, the tether broken. */
    private void horizon(ServerLevel level, ServerPlayer p) {
        float dmg = VesselPull.horizonDamage(p.getHealth(), (float) (double) TCConfig.HORIZON_MAX_DAMAGE.get());
        if (dmg > 0.0F) {
            p.hurt(TTDamageSources.taint(level), dmg);
        }
        if (TCConfig.HORIZON_WARP.get() > 0) {
            WarpHelper.addWarp(p, TCConfig.HORIZON_WARP.get(), WarpType.TEMPORARY);
        }
        p.addEffect(new MobEffectInstance(TTMobEffects.FLUX_TAINT, TCConfig.HORIZON_TAINT_TICKS.get()));
        p.addEffect(new MobEffectInstance(TCEffects.FLUNG, TCConfig.FLUNG_TICKS.get(), 0, false, false, true));
        Vec3 away = p.position().subtract(this.holeCentre());
        Vec3 flat = new Vec3(away.x, 0.0, away.z);
        flat = flat.lengthSqr() < 1.0e-6 ? new Vec3(1.0, 0.0, 0.0) : flat.normalize();
        p.setDeltaMovement(flat.x * TCConfig.HORIZON_THROW.get(), TCConfig.HORIZON_THROW_UP.get(), flat.z * TCConfig.HORIZON_THROW.get());
        p.hurtMarked = true;
        p.fallDistance = 0.0F;
        this.breakTether(p.getUUID());
        this.playVessel(level, "horizon_throw", p.position(), 1.5F);
        net.minecraft.sounds.SoundEvent zap = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT
                .get(net.minecraft.resources.ResourceLocation.parse("thaumaturge:zap"));
        if (zap != null) {
            level.playSound(null, p.getX(), p.getY(), p.getZ(), zap, net.minecraft.sounds.SoundSource.HOSTILE, 0.9F, 1.0F);
        }
    }

    /** Plays one of our {@code vessel.*} events from the server (for events no synced field shows). */
    private void playVessel(ServerLevel level, String name, Vec3 at, float volume) {
        var holder = dev.movingcastle.thaumicconcilium.registry.TCSounds.VESSEL.get(name);
        if (holder != null) {
            level.playSound(null, at.x, at.y, at.z, holder.get(), net.minecraft.sounds.SoundSource.HOSTILE, volume, 1.0F);
        }
    }

    // ---------------------------------------------------------------- mass
    private void eat(int units, long t) {
        if (units <= 0 || this.state() != ACTIVE) {
            return;
        }
        if (t - this.gainWindowStart >= TCConfig.GAIN_WINDOW.get()) {
            this.gainWindowStart = t;
            this.gainedInWindow = 0;
        }
        int gain = VesselPull.gain(this.massQuarters(), this.startQuarters(), units, this.gainedInWindow,
                (int) Math.round(TCConfig.GAIN_PER_WINDOW.get() * 4.0), TCConfig.MASS_OVERFLOW.get() * 4);
        this.gainedInWindow += gain;
        this.setMassQuarters(this.massQuarters() + gain);
    }

    /** Sets Mass in quarters (clamped); at 0 the Vessel is subdued. Used by eating, drawing and the tests. */
    public void setMassQuarters(int quarters) {
        int q = Math.max(0, Math.min(quarters, this.startQuarters() + TCConfig.MASS_OVERFLOW.get() * 4));
        this.entityData.set(MASS, q);
        this.updateBar();
        if (q == 0 && this.fightIsOn() && this.level() instanceof ServerLevel level) {
            this.subdue(level);
        }
    }

    private void updateBar() {
        int start = this.startQuarters();
        this.bossEvent.setProgress(start <= 0 ? 0.0F : Math.max(0.0F, Math.min(1.0F, this.massQuarters() / (float) start)));
        this.bossEvent.setVisible(this.fightIsOn());
    }

    /** Mass 0: everything it held in play is removed, participants are recorded (presence for contributors). */
    private void subdue(ServerLevel level) {
        this.releaseAllTethers(level);
        this.removeClots(level);
        for (EntityTaintSwarm swarm : level.getEntitiesOfClass(EntityTaintSwarm.class, this.bigBox())) {
            if (this.ownSwarm(swarm)) {
                swarm.discard();
            }
        }
        int need = TCConfig.PRESENCE_TICKS.get();
        for (UUID id : this.contributors.keySet()) {
            if (this.presence.getOrDefault(id, 0) >= need) {
                this.participants.add(id);
            }
        }
        this.entityData.set(STATE, SUBDUED);
        this.entityData.set(SWELL, false);
        this.entityData.set(PULL, 0.0F);
        this.staggerLeft = 0;
        this.resetGulp();
        this.updateBar();
        this.entityData.set(CHARGE, TCConfig.RITE_CHARGE.get());
        this.drained = 0;
        this.spilled = 0;
        java.util.Arrays.fill(this.tetherProgress, 0);
        java.util.Arrays.fill(this.starve, 0);
        this.capitalCount = 0;
        if (!this.motesGiven) {
            this.motesGiven = true;
            VesselRite.onSubdued(level, this);
        }
    }

    private AABB bigBox() {
        return new AABB(this.holeCentre(), this.holeCentre()).inflate(TCConfig.PLAYER_RANGE.get());
    }

    // ---------------------------------------------------------------- draw (tethers)
    private int anchorIndex(BlockPos pos) {
        return this.anchors.indexOf(pos);
    }

    private int tetherIndexOf(UUID id) {
        for (int i = 0; i < 4; i++) {
            if (id.equals(this.tetherOwner[i])) {
                return i;
            }
        }
        return -1;
    }

    public boolean isTethered(UUID id) {
        return this.tetherIndexOf(id) >= 0;
    }

    /** Whether an anchor is cooling at a game time. */
    public boolean cooling(int anchor, long gameTime) {
        return this.cooldownUntil[anchor] > gameTime;
    }

    /** A wand used on an anchor: wakes a dormant Vessel, otherwise forms a tether if every rule allows. Returns whether one formed. */
    public boolean draw(ServerPlayer player, BlockPos anchor) {
        ServerLevel level = player.serverLevel();
        int i = this.anchorIndex(anchor);
        if (i < 0 || i >= 4 || this.frozen) {
            return false;
        }
        if (this.state() == SUBDUED) {
            return this.drainDraw(player, i);
        }
        if (this.state() == DORMANT) {
            this.wake(level);
            return false;
        }
        if (this.state() != ACTIVE || this.tetherOwner[i] != null || this.tetherIndexOf(player.getUUID()) >= 0
                || player.hasEffect(TCEffects.FLUNG) || !player.isAlive()) {
            return false;
        }
        if (this.cooling(i, level.getGameTime())) {
            player.displayClientMessage(Component.translatable("message.thaumicconcilium.vessel_cooling"), true);
            return false;
        }
        this.tetherOwner[i] = player.getUUID();
        this.tetherProgress[i] = 0;
        this.tetherAge[i] = 0;
        this.entityData.set(TETHER[i], Optional.of(player.getUUID()));
        player.addEffect(new MobEffectInstance(TCEffects.TETHERED, 30, 0, false, false, true));
        this.syncAnchor(level, i);
        return true;
    }

    /** Breaks a player's tether (a clot hit, the horizon, a death); the progress is lost. */
    public void breakTether(UUID player) {
        int i = this.tetherIndexOf(player);
        if (i >= 0 && this.level() instanceof ServerLevel level) {
            this.releaseTether(level, i);
        }
    }

    private void releaseTether(ServerLevel level, int i) {
        UUID id = this.tetherOwner[i];
        this.tetherOwner[i] = null;
        if (this.state() != SUBDUED) {
            this.tetherProgress[i] = 0; // the rite keeps a unit's progress across a pause
        }
        this.tetherAge[i] = 0;
        this.entityData.set(TETHER[i], Optional.empty());
        if (id != null) {
            ServerPlayer p = playerById(level, id);
            if (p != null) {
                p.removeEffect(TCEffects.TETHERED);
            }
        }
        this.syncAnchor(level, i);
        if (this.originOn && this.tetherOwner[0] == null && this.tetherOwner[1] == null && this.tetherOwner[2] == null
                && this.tetherOwner[3] == null) {
            this.originOn = false; // no drawer: no origin, no pulses (a fed capital may still advance; riteTick decides riteActive)
            TaintApi.removeTaintSeed(level, this.blockPosition());
        }
    }

    // ---------------------------------------------------------------- the thaumaturge's rite (cycle 4)
    /** A wand on an anchor of the subdued Vessel: starts draining one unit at a time, if the player has the research. */
    private boolean drainDraw(ServerPlayer player, int i) {
        ServerLevel level = player.serverLevel();
        if (this.charge() <= 0 || this.tetherOwner[i] != null || this.tetherIndexOf(player.getUUID()) >= 0
                || player.hasEffect(TCEffects.FLUNG) || !player.isAlive()) {
            return false;
        }
        if (this.capital(level, i) != null) {
            player.displayClientMessage(Component.translatable("message.thaumicconcilium.capital_draws"), true);
            return false;
        }
        if (!VesselResearch.mayDrain(player)) {
            player.displayClientMessage(Component.translatable("message.thaumicconcilium.rite_locked"), true);
            return false;
        }
        this.tetherOwner[i] = player.getUUID();
        this.tetherAge[i] = 0;
        this.entityData.set(TETHER[i], Optional.of(player.getUUID()));
        player.addEffect(new MobEffectInstance(TCEffects.TETHERED, 30, 0, false, false, true));
        this.riteActive = true;
        if (!this.originOn) {
            this.originOn = true;
            TaintApi.addTaintSeed(level, this.blockPosition());
        }
        this.syncAnchor(level, i);
        return true;
    }

    /** Work only while somebody drains; a subdued Vessel nobody draws from costs a boolean test a tick. */
    private void riteTick(ServerLevel level) {
        if (this.frozen) {
            return;
        }
        if (!this.riteActive) {
            // idle: once a second, see whether a capital has mana or progress to resume (4 block entity lookups; nothing else)
            if (this.charge() > 0 && this.tickCount % 20 == 0 && this.cappedWake(level)) {
                this.riteActive = true;
            } else {
                return;
            }
        }
        int unit = TCConfig.RITE_UNIT_TICKS.get();
        int need = manaPerTick();
        int capped = 0;
        boolean busy = false;
        for (int i = 0; i < 4 && i < this.anchors.size(); i++) {
            VerdantCapitalBlockEntity cap = this.capital(level, i);
            if (cap != null) {
                capped++;
                if (this.tetherOwner[i] != null) {
                    this.releaseTether(level, i); // a capital drains this anchor by itself: no drawer
                }
                if (this.cappedTick(level, i, cap, need, unit)) {
                    return; // the last unit: finish() ran
                }
                busy |= cap.drive() == VerdantCapitalBlockEntity.Drive.FED || this.tetherProgress[i] > 0;
                continue;
            }
            this.starve[i] = 0;
            UUID id = this.tetherOwner[i];
            if (id == null) {
                continue;
            }
            busy = true;
            ServerPlayer p = playerById(level, id);
            if (p == null || !this.holds(level, p, i)) {
                this.releaseTether(level, i);
                continue;
            }
            this.tetherAge[i]++;
            if (this.tickCount % 10 == 0) {
                p.addEffect(new MobEffectInstance(TCEffects.TETHERED, 30, 0, false, false, true));
            }
            if (this.tetherAge[i] % TCConfig.RITE_SPILL_TICKS.get() == 0 && PhysicalFlux.spill(level, this.anchors.get(i), this.random)) {
                this.spilled++;
            }
            if (++this.tetherProgress[i] >= unit) {
                this.tetherProgress[i] = 0;
                this.unitDrained(p);
                if (this.charge() <= 0) {
                    VesselRite.finish(level, this);
                    return;
                }
            }
        }
        // cycle 4f: the taint runs in full at every tier, whatever the capitals: a rite run by capitals alone has a live origin too
        if (busy && !this.originOn) {
            this.originOn = true;
            TaintApi.addTaintSeed(level, this.blockPosition());
        } else if (!busy && this.originOn && this.tetherOwner[0] == null && this.tetherOwner[1] == null && this.tetherOwner[2] == null && this.tetherOwner[3] == null) {
            this.originOn = false;
            TaintApi.removeTaintSeed(level, this.blockPosition());
        }
        if (this.originOn && this.tickCount % TCConfig.RITE_PULSE_TICKS.get() == 0) {
            VesselRite.pulse(level, this);
        }
        if (!this.originOn && !busy) {
            this.riteActive = false; // a pause: no tether and no capital with mana or progress
        }
    }

    /** Mana a capped anchor spends each tick: its unit's mana over the unit's ticks, rounded up. */
    public static int manaPerTick() {
        return VesselRite.manaPerTick(TCConfig.RITE_MANA_PER_UNIT.get(), TCConfig.RITE_UNIT_TICKS.get());
    }

    /** The capital standing on anchor {@code i}, or null (no capital, or its chunk is not loaded). */
    @Nullable
    public VerdantCapitalBlockEntity capital(ServerLevel level, int i) {
        if (i < 0 || i >= this.anchors.size() || i >= 4) {
            return null;
        }
        BlockPos pos = this.anchors.get(i).above();
        if (!level.hasChunkAt(pos)) {
            return null;
        }
        return level.getBlockEntity(pos) instanceof VerdantCapitalBlockEntity cap ? cap : null;
    }

    /** How many anchors have a capital. */
    public int cappedCount(ServerLevel level) {
        int n = 0;
        for (int i = 0; i < 4 && i < this.anchors.size(); i++) {
            if (this.capital(level, i) != null) {
                n++;
            }
        }
        return n;
    }

    /** Idle check: does any capital have a tick of mana or progress in hand? Capitals that do not are shown idle. */
    private boolean cappedWake(ServerLevel level) {
        int need = manaPerTick();
        boolean wake = false;
        for (int i = 0; i < 4 && i < this.anchors.size(); i++) {
            VerdantCapitalBlockEntity cap = this.capital(level, i);
            if (cap == null) {
                continue;
            }
            if (cap.mana() >= need || this.tetherProgress[i] > 0) {
                wake = true;
            } else {
                cap.setDrive(VerdantCapitalBlockEntity.Drive.IDLE);
            }
        }
        return wake;
    }

    /**
     * One tick of a capped anchor. Fed (a tick's mana in the buffer): spends it and advances. Otherwise it pauses with its progress kept;
     * after the grace it unwinds one a tick, and a unit that unwinds to zero gives one unit of charge back (up to full), the anchor
     * carrying on from a full unit while it stays starved. Returns true if the last unit drained (the Vessel is finished).
     */
    private boolean cappedTick(ServerLevel level, int i, VerdantCapitalBlockEntity cap, int need, int unit) {
        if (cap.mana() >= need && this.charge() > 0 && cap.consume(need)) {
            this.starve[i] = 0;
            cap.setDrive(VerdantCapitalBlockEntity.Drive.FED);
            if (++this.tetherProgress[i] >= unit) {
                this.tetherProgress[i] = 0;
                this.cappedUnitDrained(level, cap);
                if (this.charge() <= 0) {
                    VesselRite.finish(level, this);
                    return true;
                }
            }
            return false;
        }
        this.starve[i]++;
        if (this.starve[i] > TCConfig.RITE_STARVE_GRACE.get() && this.tetherProgress[i] > 0) {
            cap.setDrive(VerdantCapitalBlockEntity.Drive.UNWINDING);
            if (--this.tetherProgress[i] <= 0) {
                if (this.charge() < TCConfig.RITE_CHARGE.get()) {
                    this.entityData.set(CHARGE, this.charge() + 1);
                    this.tetherProgress[i] = unit; // the next unit unwinds from full while the anchor stays starved
                } else {
                    this.tetherProgress[i] = 0;
                }
            }
        } else {
            cap.setDrive(VerdantCapitalBlockEntity.Drive.STARVED);
        }
        return false;
    }

    /** One unit drained by a capital: no warp, no spill. The placer, and their Heartstone partner, count as participants. */
    private void cappedUnitDrained(ServerLevel level, VerdantCapitalBlockEntity cap) {
        this.entityData.set(CHARGE, Math.max(0, this.charge() - 1));
        this.drained++;
        UUID placer = cap.placer();
        if (placer != null) {
            this.participants.add(placer);
            ServerPlayer p = playerById(level, placer);
            // online: Heartstone itself (then the registry); offline: the registry alone
            UUID partner = p != null ? dev.movingcastle.thaumicconcilium.compat.HeartstoneCompat.partnerOf(p)
                    : dev.movingcastle.thaumicconcilium.compat.HeartstoneCompat.partnerOfOffline(level.getServer(), placer);
            if (partner != null) {
                this.participants.add(partner);
            }
        }
    }

    /** Called by {@code VesselRite.finish}: counts the capitals standing as the last unit drains and keeps the number. */
    public int recordCapitals(ServerLevel level) {
        this.capitalCount = this.cappedCount(level);
        ThaumicConcilium.LOG.info("Vessel rite finished at {} with {} Verdant Capitals", this.hexPos(), this.capitalCount);
        return this.capitalCount;
    }

    /** One unit drawn through a player: 1 temporary warp, and 1 normal warp at every fifth unit. */
    public void unitDrained(ServerPlayer drawer) {
        this.entityData.set(CHARGE, Math.max(0, this.charge() - 1));
        this.drained++;
        WarpHelper.addWarp(drawer, 1, WarpType.TEMPORARY);
        if (VesselRite.normalWarpDue(this.drained, TCConfig.RITE_NORMAL_EVERY.get())) {
            WarpHelper.addWarp(drawer, 1, WarpType.NORMAL);
        }
        this.participants.add(drawer.getUUID());
    }

    /** Ends the rite: tethers off, the taint origin withdrawn. Called by {@code VesselRite.finish} before the Vessel is removed. */
    public void endRite(ServerLevel level) {
        this.releaseAllTethers(level);
        this.riteActive = false;
        this.originOn = false;
        TaintApi.removeTaintSeed(level, this.blockPosition());
    }

    // ---------------------------------------------------------------- the finale (phase 5d cycle 4e)
    /** Game time the finale began, or -1. */
    public long dyingStart() {
        return this.entityData.get(DYING_START);
    }

    /** The swell's radius in blocks (the crater's farthest rim plus one). */
    public float dyingRs() {
        return this.entityData.get(DYING_RS);
    }

    /** The age in ticks the collapse was decided at, or -1 while the swell holds. */
    public int dyingC() {
        return this.entityData.get(DYING_C);
    }

    /** Players who stood inside the swell at the flash, and those who did not (set at the collapse; the tests read them). */
    public List<UUID> flashInside() {
        return this.flashInside;
    }

    public List<UUID> flashOutside() {
        return this.flashOutside;
    }

    /**
     * The last unit has drained and the ending has run: the Vessel lives on as DYING, swelling to {@code rs}, until the crater is painted
     * (or the timeout); {@code seedId} is the doomed seed whose painting job the finale paces and gates. Called by {@code VesselRite.finish}.
     */
    public void beginDying(ServerLevel level, float rs) {
        long now = level.getGameTime();
        this.dyingLive = true;
        this.dyingChecked = -1;
        this.flashInside.clear();
        this.flashOutside.clear();
        this.entityData.set(STATE, DYING);
        this.entityData.set(SWELL, false);
        this.entityData.set(PULL, 0.0F);
        this.entityData.set(DYING_RS, rs);
        this.entityData.set(DYING_C, -1);
        this.entityData.set(DYING_LIGHT, -1);
        this.entityData.set(DYING_BURST, -1);
        this.flashed = false;
        this.entityData.set(DYING_START, now);
        dev.movingcastle.thaumicconcilium.world.GrovePaintJob job = this.dyingJob(level);
        this.entityData.set(DYING_GROVE, job != null);
        if (job != null) {
            job.darkUntil = now + TCConfig.FINALE_TIMEOUT.get();
            job.sightAt = now + TCConfig.FINALE_TIMEOUT.get() + VesselDeath.FADE; // an upper bound until the collapse is decided
            dev.movingcastle.thaumicconcilium.world.VesselAftermath.get(level).setDirty();
        }
    }

    private int dyingChecked = -1;

    @Nullable
    private dev.movingcastle.thaumicconcilium.world.GrovePaintJob dyingJob(ServerLevel level) {
        dev.movingcastle.thaumicconcilium.world.VesselAftermath data = dev.movingcastle.thaumicconcilium.world.VesselAftermath.peek(level);
        if (data == null) {
            return null;
        }
        dev.movingcastle.thaumicconcilium.world.GrovePaintJob found = null;
        for (dev.movingcastle.thaumicconcilium.world.GrovePaintJob j : data.jobs()) {
            if (j.centre.equals(this.hex)) {
                found = j; // the newest job at this hex (a seedless one: tiers 3 and 4 leave no seed to find it by)
            }
        }
        return found;
    }

    private void dyingTick(ServerLevel level) {
        if (!this.dyingLive) {
            this.endDying(level); // loaded in the middle of a finale: nothing is owed, sight is back
            return;
        }
        long now = level.getGameTime();
        int t = (int) (now - this.dyingStart());
        int c = this.dyingC();
        int tc = t - t % VesselDeath.CHECK;
        if (c >= 0) {
            // decided: tiers 3-4 play the light stage (shadow only) until c, then flash; tiers 0-2 simply fade
            if (t < c) {
                if (this.dyingLight() >= 0 && tc > this.dyingChecked) {
                    this.dyingChecked = tc;
                    if (TCConfig.FINALE_LIGHT_DARKNESS.get()) {
                        this.blind(level, VesselDeath.radius(tc, this.dyingRs(), -1), false);
                    }
                }
                return;
            }
            if (this.dyingLight() >= 0 && !this.flashed) {
                this.flashed = true;
                this.flash(level, now);
            }
            if (t >= c + VesselDeath.FADE) {
                this.endDying(level);
            }
            return;
        }
        if (tc <= this.dyingChecked) {
            return;
        }
        this.dyingChecked = tc;
        dev.movingcastle.thaumicconcilium.world.GrovePaintJob job = this.dyingJob(level);
        if (job == null) {
            // tiers 0-2 (or a hex in a plot): full blindness while the taint is laid, a fixed hold, no light and no flash
            if (VesselDeath.due(tc, true, TCConfig.FINALE_FIXED_HOLD.get(), TCConfig.FINALE_TIMEOUT.get())) {
                this.entityData.set(DYING_C, tc);
                return;
            }
            this.blind(level, VesselDeath.radius(tc, this.dyingRs(), -1), true);
            return;
        }
        boolean pondReady = job.pondPlaced || !dev.movingcastle.thaumicconcilium.world.GroveLife.botania(); // the pond stands before the light rises from it
        if (VesselDeath.due(tc, job.craterDoneAt >= 0L && pondReady, TCConfig.FINALE_MIN_HOLD.get(), TCConfig.FINALE_TIMEOUT.get())) {
            // the crater is painted: the blindness eases to shadow, the light rises from the pond, and the collapse follows
            int end = tc + TCConfig.FINALE_LIGHT_TICKS.get();
            this.entityData.set(DYING_LIGHT, tc);
            this.entityData.set(DYING_BURST, tc + Math.min(TCConfig.FINALE_LIGHT_BURST.get(), TCConfig.FINALE_LIGHT_TICKS.get()));
            this.entityData.set(DYING_POND, pondSpot(job));
            this.entityData.set(DYING_C, end);
            long finalSight = this.dyingStart() + end + VesselDeath.FADE;
            dev.movingcastle.thaumicconcilium.world.SeedRoots.retime(level, job, job.sightAt, finalSight);
            job.sightAt = finalSight;
            job.darkUntil = now;
            dev.movingcastle.thaumicconcilium.world.VesselAftermath.get(level).setDirty();
            Vec3 hole = this.holeCentre();
            for (ServerPlayer p : players(level)) { // blindness ends for those inside: they watch the light in the shadow
                if (p.level() == level && VesselDeath.inside(p.getEyePosition().distanceToSqr(hole), VesselDeath.radius(tc, this.dyingRs(), -1))) {
                    p.removeEffect(MobEffects.BLINDNESS);
                    if (!TCConfig.FINALE_LIGHT_DARKNESS.get()) {
                        p.removeEffect(MobEffects.DARKNESS); // the client's own shadow veil carries the stage
                    }
                }
            }
            if (TCConfig.FINALE_LIGHT_DARKNESS.get()) {
                this.blind(level, VesselDeath.radius(tc, this.dyingRs(), -1), false);
            }
            return;
        }
        this.blind(level, VesselDeath.radius(tc, this.dyingRs(), -1), true);
    }

    /** The pond's spot: the middle of the reserve's columns, at its water block. */
    public static BlockPos pondSpot(dev.movingcastle.thaumicconcilium.world.GrovePaintJob job) {
        if (job.pond.isEmpty()) {
            return job.centre;
        }
        long x = 0;
        long z = 0;
        int y = BlockPos.of(job.pond.get(0)).getY() + 1;
        for (long l : job.pond) {
            BlockPos b = BlockPos.of(l);
            x += b.getX();
            z += b.getZ();
        }
        return new BlockPos((int) Math.floorDiv(x, job.pond.size()), y, (int) Math.floorDiv(z, job.pond.size()));
    }

    /** Darkness (and, while {@code full}, Blindness) for every player whose eye is within {@code radius} of the hole. */
    public void blind(ServerLevel level, float radius, boolean full) {
        int ticks = TCConfig.FINALE_BLIND_TICKS.get();
        Vec3 hole = this.holeCentre();
        for (ServerPlayer p : players(level)) {
            if (p.level() != level || p.isSpectator()) {
                continue;
            }
            if (VesselDeath.inside(p.getEyePosition().distanceToSqr(hole), radius)) {
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, ticks, 0, false, false, true));
                if (full) {
                    p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks, 0, false, false, true));
                }
            }
        }
    }

    /** The age the light stage began (tiers 3-4), or -1 (none: tiers 0-2). */
    public int dyingLight() {
        return this.entityData.get(DYING_LIGHT);
    }

    /** Whether this ending paints a grove (the look of tiers 3-4: the cyan horizon, later the light). */
    public boolean dyingGrove() {
        return this.entityData.get(DYING_GROVE);
    }

    /** The age the light bursts. */
    public int dyingBurst() {
        return this.entityData.get(DYING_BURST);
    }

    /** Where the light rises from (the pond's spot). */
    public BlockPos dyingPond() {
        return this.entityData.get(DYING_POND);
    }

    private boolean flashed;

    /** The collapse: tells every player near enough when and where, and whether they stood inside (they are spared the white-out). */
    private void flash(ServerLevel level, long now) {
        this.flashInside.clear();
        this.flashOutside.clear();
        Vec3 hole = this.holeCentre();
        float rs = this.dyingRs();
        double range = TCConfig.FINALE_FLASH_RANGE.get();
        for (ServerPlayer p : players(level)) {
            if (p.level() != level || p.isSpectator()) {
                continue;
            }
            double d2 = p.getEyePosition().distanceToSqr(hole);
            boolean inside = VesselDeath.inside(d2, rs);
            (inside ? this.flashInside : this.flashOutside).add(p.getUUID());
            if (d2 <= range * range && !EXTRA_PLAYERS.contains(p)) {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p,
                        new dev.movingcastle.thaumicconcilium.network.VesselFlashPayload(now, hole.x, hole.y, hole.z, rs, inside));
            }
        }
    }

    /** The finale is over: sight is back, the gap before the seed's death counts from here at the latest, and the Vessel goes. */
    public void endDying(ServerLevel level) {
        dev.movingcastle.thaumicconcilium.world.GrovePaintJob job = this.dyingJob(level);
        if (job != null) {
            long now = level.getGameTime();
            job.sightAt = job.sightAt < 0L ? now : Math.min(job.sightAt, now);
            job.darkUntil = now;
            dev.movingcastle.thaumicconcilium.world.VesselAftermath.get(level).setDirty();
        }
        this.dyingLive = false;
        this.discard();
    }

    private void releaseAllTethers(ServerLevel level) {
        for (int i = 0; i < 4; i++) {
            if (this.tetherOwner[i] != null) {
                this.releaseTether(level, i);
            }
        }
    }

    private boolean holds(ServerLevel level, ServerPlayer p, int i) {
        if (!p.isAlive() || p.level() != level || p.hasEffect(TCEffects.FLUNG)
                || !(Hexes.isWand(p.getMainHandItem()) || Hexes.isWand(p.getOffhandItem()))) {
            return false;
        }
        BlockPos a = this.anchors.get(i);
        double dx = p.getX() - (a.getX() + 0.5);
        double dz = p.getZ() - (a.getZ() + 0.5);
        double r = TCConfig.TETHER_RANGE.get();
        return dx * dx + dz * dz <= r * r;
    }

    private void tickTethers(ServerLevel level, long t) {
        for (int i = 0; i < 4 && i < this.anchors.size(); i++) {
            UUID id = this.tetherOwner[i];
            if (id == null) {
                continue;
            }
            ServerPlayer p = playerById(level, id);
            if (p == null || !this.holds(level, p, i)) {
                this.releaseTether(level, i);
                continue;
            }
            this.tetherProgress[i] += this.staggerLeft > 0 ? 2 : 1;
            if (++this.tetherAge[i] == TCConfig.PARTICIPANT_TETHER_TICKS.get()) {
                this.participants.add(id);
            }
            if (this.tickCount % 10 == 0) {
                p.addEffect(new MobEffectInstance(TCEffects.TETHERED, 30, 0, false, false, true));
            }
            if (this.tetherProgress[i] >= TCConfig.DRAW_TICKS.get()) {
                this.tetherProgress[i] = 0;
                this.draws[i]++;
                boolean cools = this.draws[i] >= TCConfig.DRAWS_PER_COOLDOWN.get();
                if (cools) {
                    this.draws[i] = 0;
                    this.cooldownUntil[i] = t + TCConfig.ANCHOR_COOLDOWN.get();
                    this.releaseTether(level, i);
                } else {
                    this.syncAnchor(level, i);
                }
                this.setMassQuarters(this.massQuarters() - 4);
                if (this.state() != ACTIVE) {
                    return;
                }
            }
        }
    }

    /** Writes the anchor block's two display states from the Vessel's state, only when they differ. */
    private void syncAnchor(ServerLevel level, int i) {
        if (i >= this.anchors.size()) {
            return;
        }
        BlockPos pos = this.anchors.get(i);
        BlockState now = level.getBlockState(pos);
        if (!now.is(TCBlocks.ANCHOR.get())) {
            return;
        }
        int charges = this.cooling(i, level.getGameTime()) ? 0 : Math.max(0, Math.min(2, TCConfig.DRAWS_PER_COOLDOWN.get() - this.draws[i]));
        charges = Math.min(2, charges);
        BlockState want = now.setValue(AnchorBlock.CHARGES, charges).setValue(AnchorBlock.TETHERED, this.tetherOwner[i] != null);
        if (want != now) {
            int before = now.getValue(AnchorBlock.CHARGES);
            if (charges == 0 && before > 0) {
                this.playVessel(level, "anchor_cool", Vec3.atCenterOf(pos), 1.0F);
            } else if (before == 0 && charges > 0) {
                this.playVessel(level, "anchor_ready", Vec3.atCenterOf(pos), 1.0F);
            }
            level.setBlock(pos, want, 3);
        }
    }

    // ---------------------------------------------------------------- clots, swarms
    private void spawnThings(ServerLevel level, long t, int phase) {
        if (this.pairGap > 0 && --this.pairGap == 0) {
            this.throwClot(level, t);
        }
        int interval = phase == 0 ? TCConfig.CLOT_FEEDING.get() : phase == 1 ? TCConfig.CLOT_SPINUP.get() : TCConfig.CLOT_COLLAPSE.get();
        if (this.clotsEnabled && ++this.clotTimer >= interval) {
            if (this.throwClot(level, t)) {
                this.clotTimer = 0;
                if (phase == 1) {
                    this.pairGap = TCConfig.CLOT_SPINUP_GAP.get();
                }
            } else {
                this.clotTimer = interval;
            }
        }
        if (phase == 1) {
            if (this.lastPhase != 1 || ++this.swarmTimer >= TCConfig.SWARM_INTERVAL.get()) {
                this.swarmTimer = 0;
                this.spawnSwarms(level, t);
            }
        } else {
            this.swarmTimer = 0;
        }
        this.lastPhase = phase;
    }

    private boolean throwClot(ServerLevel level, long t) {
        List<ServerPlayer> targets = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (this.tetherOwner[i] != null) {
                ServerPlayer p = playerById(level, this.tetherOwner[i]);
                if (p != null && p.isAlive() && p.level() == level) {
                    targets.add(p);
                }
            }
        }
        if (targets.isEmpty()) {
            return false;
        }
        ServerPlayer target = targets.get(this.random.nextInt(targets.size()));
        Vec3 dir = new Vec3(target.getX() - this.getX(), 0.0, target.getZ() - this.getZ());
        dir = dir.lengthSqr() < 1.0e-6 ? new Vec3(1.0, 0.0, 0.0) : dir.normalize();
        VesselClot clot = TCEntities.VESSEL_CLOT.get().create(level);
        if (clot == null) {
            return false;
        }
        clot.setup(this, target, this.holeCentre().add(dir.scale(TCConfig.DISC_OUTER.get())), t + TCConfig.CLOT_WINDUP.get());
        level.addFreshEntity(clot);
        return true;
    }

    /** Spin-up: one Taint Swarm per player within range, at most {@code swarm.maxAlive} alive, at radius 6, tagged as this Vessel's. */
    private void spawnSwarms(ServerLevel level, long t) {
        int players = 0;
        double r = TCConfig.PLAYER_RANGE.get();
        for (ServerPlayer p : players(level)) {
            if (!p.isSpectator() && p.distanceToSqr(this) <= r * r) {
                players++;
            }
        }
        int alive = 0;
        for (EntityTaintSwarm s : level.getEntitiesOfClass(EntityTaintSwarm.class, this.bigBox())) {
            if (this.ownSwarm(s)) {
                alive++;
            }
        }
        int make = VesselPull.swarmsToSpawn(players, alive, TCConfig.SWARM_MAX.get());
        for (int i = 0; i < make; i++) {
            EntityTaintSwarm swarm = TTEntities.TAINT_SWARM.get().create(level);
            if (swarm == null) {
                continue;
            }
            double a = this.random.nextDouble() * Math.PI * 2.0;
            double rad = TCConfig.SWARM_RADIUS.get();
            swarm.moveTo(this.getX() + Math.cos(a) * rad, this.getY(), this.getZ() + Math.sin(a) * rad, (float) (a * 57.29578), 0.0F);
            this.tagSwarm(swarm, t);
            swarm.setPersistenceRequired();
            level.addFreshEntity(swarm);
        }
    }

    private void removeClots(ServerLevel level) {
        for (VesselClot c : level.getEntitiesOfClass(VesselClot.class, this.bigBox(), c -> this.getUUID().equals(c.vesselId()))) {
            c.discard();
        }
    }

    // ---------------------------------------------------------------- once a second
    private void slowChecks(ServerLevel level) {
        if (this.tickCount % 20 != 0) {
            return;
        }
        long t = level.getGameTime();
        // dormancy
        if (!anyNear(level, this, TCConfig.PLAYER_RANGE.get())) {
            this.emptyTicks += 20;
            if (this.emptyTicks >= (this.dormantTest > 0 ? this.dormantTest : TCConfig.DORMANT_TICKS.get())) {
                this.goDormant(level);
                return;
            }
        } else {
            this.emptyTicks = 0;
        }
        if (this.state() != ACTIVE) {
            return;
        }
        for (int i = 0; i < 4 && i < this.anchors.size(); i++) {
            if (this.cooldownUntil[i] != 0L && this.cooldownUntil[i] <= t) {
                this.cooldownUntil[i] = 0L;
                this.syncAnchor(level, i);
            }
        }
        this.applyDarkness(level);
        // presence: a burden contributor within range counts towards the mote at Mass 0
        double pr = TCConfig.PRESENCE_RANGE.get();
        for (UUID id : this.contributors.keySet()) {
            ServerPlayer p = playerById(level, id);
            if (p != null && p.level() == level && p.distanceToSqr(this) <= pr * pr) {
                this.presence.merge(id, 20, Integer::sum);
            }
        }
        // one random block within 3 of the centre: eaten if edible, no drops
        int r = TCConfig.EDIBLE_RADIUS.get();
        BlockPos at = BlockPos.containing(this.holeCentre()).offset(this.random.nextInt(2 * r + 1) - r, this.random.nextInt(2 * r + 1) - r,
                this.random.nextInt(2 * r + 1) - r);
        if (level.getBlockState(at).is(EDIBLE)) {
            level.removeBlock(at, false);
        }
        if (sableLoaded()) {
            this.ships = SablePull.query(level, new AABB(this.holeCentre(), this.holeCentre()).inflate(TCConfig.PULL_RANGE.get()));
        }
    }

    /**
     * Darkness inside {@code darkness.radius} of the hole, Blindness inside {@code darkness.blindnessRadius}; only while ACTIVE, short and
     * refreshed on the once-a-second sample. The anchors (8 to 12 out) stay readable. Public so a test can call it without waiting.
     */
    public void applyDarkness(ServerLevel level) {
        if (this.state() != ACTIVE) {
            return;
        }
        double dark = TCConfig.DARKNESS_RADIUS.get();
        double blind = TCConfig.BLINDNESS_RADIUS.get();
        int ticks = TCConfig.DARKNESS_TICKS.get();
        Vec3 hole = this.holeCentre();
        for (ServerPlayer p : players(level)) {
            if (p.level() != level || p.isSpectator()) {
                continue;
            }
            double d2 = p.getEyePosition().distanceToSqr(hole);
            if (d2 <= dark * dark && dark > 0.0) {
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, ticks, 0, false, false, true));
            }
            if (d2 <= blind * blind && blind > 0.0) {
                p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks, 0, false, false, true));
            }
        }
    }

    private void goDormant(ServerLevel level) {
        this.releaseAllTethers(level);
        this.removeClots(level);
        this.ships = Collections.emptyList();
        this.entityData.set(STATE, DORMANT);
        this.entityData.set(SWELL, false);
        this.entityData.set(PULL, 0.0F);
        this.staggerLeft = 0;
        this.pairGap = 0;
        this.clotTimer = 0;
        this.resetGulp();
        this.emptyTicks = 0;
        this.updateBar();
    }

    /**
     * Test hook: players a pack test adds because GameTest's mock players cannot join (their join hooks send client payloads) and a
     * fake player is not in the level's entity list. Always empty in play.
     */
    public static final java.util.List<ServerPlayer> EXTRA_PLAYERS = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** The players in a level (plus the test hook's). */
    static List<ServerPlayer> players(ServerLevel level) {
        if (EXTRA_PLAYERS.isEmpty()) {
            return level.players();
        }
        List<ServerPlayer> all = new ArrayList<>(level.players());
        for (ServerPlayer p : EXTRA_PLAYERS) {
            if (p.level() == level) {
                all.add(p);
            }
        }
        return all;
    }

    @Nullable
    public static ServerPlayer playerById(ServerLevel level, UUID id) {
        ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
        if (p == null) {
            for (ServerPlayer extra : EXTRA_PLAYERS) {
                if (extra.getUUID().equals(id)) {
                    return extra;
                }
            }
        }
        return p;
    }

    /** Whether a living, non-spectator player is within {@code range} of an entity. */
    static boolean anyNear(ServerLevel level, Entity at, double range) {
        for (ServerPlayer p : players(level)) {
            if (p.isAlive() && !p.isSpectator() && p.distanceToSqr(at) <= range * range) {
                return true;
            }
        }
        return false;
    }

    /** The Vessel an anchor belongs to, or null. */
    @Nullable
    public static Vessel forAnchor(ServerLevel level, BlockPos pos) {
        for (Vessel v : level.getEntitiesOfClass(Vessel.class, AnchorBlock.searchBox(pos), v -> !v.isRemoved() && v.anchors.contains(pos))) {
            return v;
        }
        return null;
    }

    /** Damage dealt to one of its swarms by a player makes them a participant. */
    @SubscribeEvent
    public static void onSwarmHurt(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        if (victim.level() instanceof ServerLevel level && event.getSource().getEntity() instanceof ServerPlayer player
                && victim.getPersistentData().hasUUID(SWARM_KEY) && level.getEntity(victim.getPersistentData().getUUID(SWARM_KEY)) instanceof Vessel v) {
            v.participants.add(player.getUUID());
        }
    }

    // ---------------------------------------------------------------- boss bar
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void remove(RemovalReason reason) {
        this.bossEvent.removeAllPlayers();
        if (this.level() instanceof ServerLevel level && !level.isClientSide) {
            for (int i = 0; i < 4; i++) {
                if (this.tetherOwner[i] != null) {
                    ServerPlayer p = playerById(level, this.tetherOwner[i]);
                    if (p != null) {
                        p.removeEffect(TCEffects.TETHERED);
                    }
                }
            }
        }
        super.remove(reason);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (clientEvent == null || !this.level().isClientSide) {
            return;
        }
        String what = key == STATE ? "state" : key == SWELL ? "swell" : key == PULL ? "pull" : key == MASS ? "mass" : null;
        for (int i = 0; i < 4 && what == null; i++) {
            if (key == TETHER[i]) {
                what = "tether";
            }
        }
        if (what != null) {
            clientEvent.accept(this, what);
        }
    }

    // ---------------------------------------------------------------- invulnerable, fixed
    @Override
    public boolean isPickable() {
        return false;
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
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 16384.0;
    }

    // ---------------------------------------------------------------- save and load
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(BURDEN, tag.getInt("burden"));
        this.hex = BlockPos.of(tag.getLong("hex"));
        this.contributors.clear();
        ListTag list = tag.getList("contributors", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            this.contributors.put(c.getUUID("id"), c.getInt("n"));
        }
        this.anchors.clear();
        for (long p : tag.getLongArray("anchors")) {
            this.anchors.add(BlockPos.of(p));
        }
        this.syncAnchors();
        this.entityData.set(START, tag.getInt("start"));
        this.entityData.set(MASS, tag.getInt("mass"));
        this.entityData.set(STATE, tag.getByte("state"));
        this.entityData.set(SWELL, tag.getBoolean("swell"));
        this.missedClots = tag.getInt("missedClots");
        if (tag.contains("breakStart")) { // a break was cut short (chunk unload, server stop): it finishes at once on the next tick
            this.entityData.set(BREAK_START, tag.getLong("breakStart"));
            this.breakInterrupted = true;
        }
        this.breakSub = tag.hasUUID("breakSub") ? tag.getUUID("breakSub") : null;
        this.breakCrater = tag.getBoolean("breakCrater");
        this.breakSpokes = null;
        if (tag.contains("breakSpokes")) {
            ListTag spokes = tag.getList("breakSpokes", 6);
            this.breakSpokes = new double[spokes.size() / 2][2];
            for (int i = 0; i + 1 < spokes.size(); i += 2) {
                this.breakSpokes[i / 2][0] = spokes.getDouble(i);
                this.breakSpokes[i / 2][1] = spokes.getDouble(i + 1);
            }
        }
        this.crater = tag.contains("crater") ? VesselCrater.load(tag.getCompound("crater")) : null;
        this.craterLoaded = this.crater != null; // a crater cut short finishes at once on the next tick
        this.frozen = tag.getBoolean("QaFrozen");
        this.qaTether = tag.getBoolean("QaTether");
        this.participants.clear();
        for (net.minecraft.nbt.Tag n : tag.getList("participants", 11)) {
            this.participants.add(net.minecraft.nbt.NbtUtils.loadUUID(n));
        }
        this.presence.clear();
        ListTag pres = tag.getList("presence", 10);
        for (int i = 0; i < pres.size(); i++) {
            CompoundTag c = pres.getCompound(i);
            this.presence.put(c.getUUID("id"), c.getInt("n"));
        }
        int[] d = tag.getIntArray("draws");
        long[] cd = tag.getLongArray("cooldowns");
        for (int i = 0; i < 4; i++) {
            this.draws[i] = i < d.length ? d[i] : 0;
            this.cooldownUntil[i] = i < cd.length ? cd[i] : 0L;
            this.tetherOwner[i] = null; // tethers are not kept: a loaded Vessel starts untethered
            this.tetherProgress[i] = 0;
            this.tetherAge[i] = 0;
            this.entityData.set(TETHER[i], Optional.empty());
        }
        this.wakeTimer = tag.getInt("wakeTimer");
        this.emptyTicks = tag.getInt("emptyTicks");
        this.clotTimer = tag.getInt("clotTimer");
        this.pairGap = tag.getInt("pairGap");
        this.staggerLeft = tag.getInt("staggerLeft");
        this.gulpTimer = tag.getInt("gulpTimer");
        this.gulpPhase = tag.getInt("gulpPhase");
        this.gulpLeft = tag.getInt("gulpLeft");
        this.swarmTimer = tag.getInt("swarmTimer");
        this.lastPhase = tag.contains("lastPhase") ? tag.getInt("lastPhase") : -1;
        this.gainWindowStart = tag.contains("gainWindowStart") ? tag.getLong("gainWindowStart") : Long.MIN_VALUE / 2;
        this.gainedInWindow = tag.getInt("gainedInWindow");
        this.entityData.set(PULL, this.fightIsOn() ? this.computePull() : 0.0F);
        this.entityData.set(CHARGE, tag.contains("charge") ? tag.getInt("charge") : (this.state() == SUBDUED ? TCConfig.RITE_CHARGE.get() : 0));
        this.drained = tag.getInt("drained");
        this.spilled = tag.getInt("spilled");
        this.motesGiven = tag.getBoolean("motesGiven");
        this.subdueMotes.clear();
        ListTag given = tag.getList("subdueMotes", 10);
        for (int i = 0; i < given.size(); i++) {
            CompoundTag c = given.getCompound(i);
            this.subdueMotes.put(c.getUUID("id"), c.getInt("n"));
        }
        this.riteActive = false; // tethers are not kept, so a loaded Vessel is paused (a capital with mana or progress re-arms it, see riteTick)
        this.originOn = false;
        this.capitalCount = tag.getInt("capitalCount");
        if (this.state() == SUBDUED) {
            int[] rp = tag.getIntArray("riteProgress");
            int[] sv = tag.getIntArray("starve");
            for (int i = 0; i < 4 && i < rp.length; i++) {
                this.tetherProgress[i] = rp[i];
            }
            for (int i = 0; i < 4 && i < sv.length; i++) {
                this.starve[i] = sv[i];
            }
        }
        this.dyingLive = false; // a Vessel loaded in DYING ends its finale at its first tick
        if (tag.contains("dyingStart")) {
            this.entityData.set(DYING_START, tag.getLong("dyingStart"));
            this.entityData.set(DYING_RS, tag.getFloat("dyingRs"));
            this.entityData.set(DYING_C, tag.getInt("dyingC"));
            this.entityData.set(DYING_GROVE, tag.getBoolean("dyingGrove"));
            if (tag.contains("dyingLight")) {
                this.entityData.set(DYING_LIGHT, tag.getInt("dyingLight"));
                this.entityData.set(DYING_BURST, tag.getInt("dyingBurst"));
                this.entityData.set(DYING_POND, BlockPos.of(tag.getLong("dyingPond")));
            }
        }
        this.anchorsDirty = true;
        this.updateBar();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("burden", this.burdenAtBreak());
        tag.putLong("hex", this.hex.asLong());
        ListTag list = new ListTag();
        this.contributors.forEach((id, n) -> {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", id);
            c.putInt("n", n);
            list.add(c);
        });
        tag.put("contributors", list);
        tag.putLongArray("anchors", this.anchors.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putInt("start", this.startQuarters());
        tag.putInt("mass", this.massQuarters());
        tag.putByte("state", this.state());
        tag.putBoolean("swell", this.swelling());
        tag.putInt("missedClots", this.missedClots);
        if (this.breaking()) {
            tag.putLong("breakStart", this.entityData.get(BREAK_START));
        }
        if (this.breakSub != null) {
            tag.putUUID("breakSub", this.breakSub);
        }
        if (this.breakSpokes != null) {
            ListTag spokes = new ListTag();
            for (double[] sp : this.breakSpokes) {
                spokes.add(net.minecraft.nbt.DoubleTag.valueOf(sp[0]));
                spokes.add(net.minecraft.nbt.DoubleTag.valueOf(sp[1]));
            }
            tag.put("breakSpokes", spokes);
            tag.putBoolean("breakCrater", this.breakCrater);
        }
        if (this.crater != null) {
            tag.put("crater", this.crater.save());
        }
        if (this.state() == DYING) {
            tag.putLong("dyingStart", this.dyingStart());
            tag.putFloat("dyingRs", this.dyingRs());
            tag.putInt("dyingC", this.dyingC());
            tag.putBoolean("dyingGrove", this.dyingGrove());
            if (this.dyingLight() >= 0) {
                tag.putInt("dyingLight", this.dyingLight());
                tag.putInt("dyingBurst", this.dyingBurst());
                tag.putLong("dyingPond", this.dyingPond().asLong());
            }
        }
        tag.putBoolean("QaFrozen", this.frozen);
        ListTag parts = new ListTag();
        this.participants.forEach(id -> parts.add(net.minecraft.nbt.NbtUtils.createUUID(id)));
        tag.put("participants", parts);
        ListTag pres = new ListTag();
        this.presence.forEach((id, n) -> {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", id);
            c.putInt("n", n);
            pres.add(c);
        });
        tag.put("presence", pres);
        tag.putIntArray("draws", this.draws);
        tag.putLongArray("cooldowns", this.cooldownUntil);
        tag.putInt("wakeTimer", this.wakeTimer);
        tag.putInt("emptyTicks", this.emptyTicks);
        tag.putInt("clotTimer", this.clotTimer);
        tag.putInt("pairGap", this.pairGap);
        tag.putInt("staggerLeft", this.staggerLeft);
        tag.putInt("gulpTimer", this.gulpTimer);
        tag.putInt("gulpPhase", this.gulpPhase);
        tag.putInt("gulpLeft", this.gulpLeft);
        tag.putInt("swarmTimer", this.swarmTimer);
        tag.putInt("lastPhase", this.lastPhase);
        tag.putLong("gainWindowStart", this.gainWindowStart);
        tag.putInt("gainedInWindow", this.gainedInWindow);
        tag.putInt("charge", this.charge());
        tag.putInt("drained", this.drained);
        tag.putInt("spilled", this.spilled);
        tag.putBoolean("motesGiven", this.motesGiven);
        ListTag given = new ListTag();
        this.subdueMotes.forEach((id, n) -> {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", id);
            c.putInt("n", n);
            given.add(c);
        });
        tag.put("subdueMotes", given);
        tag.putIntArray("riteProgress", this.tetherProgress);
        tag.putIntArray("starve", this.starve);
        tag.putInt("capitalCount", this.capitalCount);
    }
}
