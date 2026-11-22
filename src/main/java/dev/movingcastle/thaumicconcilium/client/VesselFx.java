package dev.movingcastle.thaumicconcilium.client;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.Vessel;
import dev.movingcastle.thaumicconcilium.entity.VesselClot;
import dev.movingcastle.thaumicconcilium.registry.TCSounds;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Client effects of the Vessel (phase 5c cycle 3), all driven from the synced data: the eased radius, the hum loop (only while the fight
 * is on and within 48 blocks), the one-shot sounds (when a synced field changes), the gulp's inward streaks and the clots' trails.
 * Nothing runs for a dormant Vessel except one eased float.
 */
public final class VesselFx {
    /** Nearest active Vessel to the local player this client tick (for the vignette). */
    static volatile float nearest = Float.MAX_VALUE;
    static volatile boolean nearestGulp;
    static volatile long nearestAt = Long.MIN_VALUE;

    private static final DustParticleOptions TAINT = new DustParticleOptions(new Vector3f(0.76F, 0.23F, 0.84F), 0.9F);
    private static final DustParticleOptions PALE = new DustParticleOptions(new Vector3f(0.69F, 0.94F, 1.0F), 0.9F);

    private VesselFx() {
    }

    // ---------------------------------------------------------------- each client tick
    public static void tick(Vessel v) {
        Level level = v.level();
        float target = VesselRenderer.baseRadius(v);
        v.fxShownPrev = v.fxShown < 0.0F ? target : v.fxShown;
        v.fxShown = v.fxShown < 0.0F ? target : v.fxShown + (target - v.fxShown) * 0.15F;
        for (int i = 0; i < 4; i++) { // the anchor shards' heights, eased (no per-tick work when the Vessel is not loaded)
            var state = VesselRenderer.anchorState(v, i);
            if (state == null) {
                v.fxShard[i] = -1.0F;
                continue;
            }
            float want = VesselRenderer.shardTarget(v, v.syncedAnchor(i).get(), state);
            v.fxShard[i] = v.fxShard[i] < 0.0F ? want : v.fxShard[i] + (want - v.fxShard[i]) * 0.15F;
            glyphs(v, i, v.syncedAnchor(i).get(), state);
        }
        breakGlyphs(v);
        if (!v.fightIsOn()) {
            return;
        }
        var player = Minecraft.getInstance().player;
        if (player != null) {
            long now = level.getGameTime();
            float d = (float) player.getEyePosition().distanceTo(v.position());
            if (nearestAt != now) {
                nearestAt = now;
                nearest = Float.MAX_VALUE;
                nearestGulp = false;
            }
            if (d < nearest) {
                nearest = d;
                nearestGulp = v.pullScale() >= 1.5F;
            }
            if (d <= 48.0F && !(v.fxHum instanceof Hum[] hums && !hums[0].isStopped())) {
                Hum beacon = new Hum(TCSounds.VESSEL.get("hum").get(), v, 1.0F, true);
                Hum portal = new Hum(BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.withDefaultNamespace("block.portal.ambient")), v, 0.25F, false);
                v.fxHum = new Hum[] {beacon, portal};
                Minecraft.getInstance().getSoundManager().play(beacon);
                Minecraft.getInstance().getSoundManager().play(portal);
            }
        }
        if (v.pullScale() >= 1.5F) { // the gulp: about 24 inward streaks a second from radius 10
            RandomSource r = level.random;
            int n = 1 + (r.nextFloat() < 0.2F ? 1 : 0);
            for (int i = 0; i < n; i++) {
                Vec3 dir = new Vec3(r.nextGaussian(), r.nextGaussian(), r.nextGaussian()).normalize().scale(10.0);
                level.addParticle(ParticleTypes.ENCHANT, v.getX(), v.getY(), v.getZ(), dir.x, dir.y, dir.z);
            }
        }
    }

    // ---------------------------------------------------------------- the anchors' glyphs (cycle 3c)
    /** Glyph colours: the Vessel's violet while charged, white while someone draws, the hex caps' ember orange while cooling. */
    static final int GLYPH_VIOLET = 0xA066FF;
    static final int GLYPH_WHITE = 0xF6F0FF;
    static final int GLYPH_EMBER = 0xE0661C;
    /** No glyph is spawned for an anchor farther than this from the camera. */
    private static final double GLYPH_RANGE = 48.0;

    /**
     * The runes leave the stone: the anchor's state is read through how many glyphs rise from it. Two charges, a steady stream (one
     * every 3 ticks); one charge, sparse (every 12); cooling, a small ember now and then (every 40); tethered, white and quick
     * (every 2). Spawned here, on the client, only while this Vessel is loaded and the anchor is within 48 blocks of the camera.
     */
    private static void glyphs(Vessel v, int i, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (!(v.level() instanceof net.minecraft.client.multiplayer.ClientLevel level)) {
            return;
        }
        boolean tethered = state.getValue(dev.movingcastle.thaumicconcilium.block.AnchorBlock.TETHERED);
        int charges = state.getValue(dev.movingcastle.thaumicconcilium.block.AnchorBlock.CHARGES);
        int every = tethered ? 2 : charges == 2 ? 3 : charges == 1 ? 12 : 40;
        if ((level.getGameTime() + i * 7L) % every != 0L) {
            return;
        }
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        if (cam.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > GLYPH_RANGE * GLYPH_RANGE) {
            return;
        }
        RandomSource r = level.random;
        Vec3 at = new Vec3(pos.getX() + 0.22 + r.nextDouble() * 0.56, pos.getY() + 0.42 + r.nextDouble() * 0.12, pos.getZ() + 0.22 + r.nextDouble() * 0.56);
        if (tethered) {
            GlyphParticle.spawn(level, at, GLYPH_WHITE, 0.13F, 34 + r.nextInt(14), 0.050F, 1.0F);
        } else if (charges > 0) {
            GlyphParticle.spawn(level, at, GLYPH_VIOLET, 0.12F, 56 + r.nextInt(24), 0.028F, 0.95F);
        } else {
            GlyphParticle.spawn(level, at, GLYPH_EMBER, 0.07F, 30 + r.nextInt(16), 0.012F, 0.8F);
        }
    }

    /** The break sequence: glyphs boil off the cracking hex, and each flying shard sheds a few on its way out. */
    private static void breakGlyphs(Vessel v) {
        float age = v.breakAge(0.0F);
        if (age < 0.0F || !(v.level() instanceof net.minecraft.client.multiplayer.ClientLevel level)) {
            return;
        }
        RandomSource r = level.random;
        if (age >= dev.movingcastle.thaumicconcilium.entity.VesselBreak.CRACK_START && age < dev.movingcastle.thaumicconcilium.entity.VesselBreak.BURST) {
            Vec3 at = v.position().add((r.nextDouble() - 0.5) * 2.4, (r.nextDouble() - 0.5) * 0.6, (r.nextDouble() - 0.5) * 2.4);
            GlyphParticle.spawn(level, at, GLYPH_WHITE, 0.13F, 20 + r.nextInt(10), 0.06F, 1.0F);
        }
        if ((int) age == dev.movingcastle.thaumicconcilium.entity.VesselBreak.BURST) { // the burst: the hex's glyphs are flung out in every direction
            for (int n = 0; n < 40; n++) {
                Vec3 dir = new Vec3(r.nextGaussian(), r.nextGaussian() * 0.7 + 0.15, r.nextGaussian()).normalize();
                GlyphParticle.fling(level, v.position().add(dir.scale(0.6)), dir.scale(0.35 + r.nextDouble() * 0.45),
                        n % 3 == 0 ? GLYPH_VIOLET : GLYPH_WHITE, 0.15F, 30 + r.nextInt(25), 1.0F);
            }
        }
        if (age >= dev.movingcastle.thaumicconcilium.entity.VesselBreak.BURST && (int) age % 2 == 0) {
            double t = dev.movingcastle.thaumicconcilium.entity.VesselBreak.flight(age);
            for (int i = 0; i < 4; i++) {
                Optional<net.minecraft.core.BlockPos> anchor = v.syncedAnchor(i);
                if (anchor.isPresent()) {
                    Vec3 at = dev.movingcastle.thaumicconcilium.entity.VesselBreak.shardAt(v.position(), Vec3.atBottomCenterOf(anchor.get()).add(0.0, 0.25, 0.0), t);
                    GlyphParticle.spawn(level, at, GLYPH_VIOLET, 0.11F, 26 + r.nextInt(12), 0.02F, 0.95F);
                }
            }
        }
    }

    // ---------------------------------------------------------------- a synced field changed
    public static void event(Vessel v, String what) {
        Level level = v.level();
        boolean live = v.tickCount >= 3; // not for the first sync at spawn
        long now = level.getGameTime();
        switch (what) {
            case "state" -> {
                byte s = v.state();
                if (live && v.fxLastState == Vessel.DORMANT && s == Vessel.WAKING) {
                    VesselSounds.play(v, "wake", 4.0F);
                }
                if (live && s == Vessel.SUBDUED && v.fxLastState != Vessel.SUBDUED) {
                    VesselSounds.play(v, "subdued", 4.0F);
                }
                v.fxLastState = s;
            }
            case "swell" -> {
                if (v.swelling()) {
                    v.fxSwellStart = now;
                    if (live && v.state() == Vessel.ACTIVE) {
                        VesselSounds.play(v, "swell", 4.0F);
                    }
                }
            }
            case "pull" -> {
                float p = v.pullScale();
                float old = v.fxLastPull;
                if (live && v.state() == Vessel.ACTIVE) {
                    if (p >= 1.5F && old < 1.5F) {
                        VesselSounds.play(v, "gulp", 4.0F);
                    } else if (p == 0.0F && old > 0.0F) {
                        v.fxStaggerStart = now;
                        VesselSounds.play(v, "stagger", 4.0F);
                    } else if (p > 0.0F && old == 0.0F) {
                        VesselSounds.play(v, "recover", 3.0F);
                    }
                } else if (p == 0.0F && old > 0.0F) {
                    v.fxStaggerStart = now;
                }
                v.fxLastPull = p;
            }
            case "mass" -> {
                int m = v.massQuarters();
                if (live && v.fxLastMass >= 0) {
                    if (m <= v.fxLastMass - 4) {
                        VesselSounds.play(v, "mass_drop", 3.0F);
                    } else if (m > v.fxLastMass && now - v.fxEatAt >= 4) {
                        v.fxEatAt = now;
                        VesselSounds.play(v, "eat", 1.0F);
                    }
                }
                v.fxLastMass = m;
            }
            case "tether" -> {
                for (int i = 0; i < 4; i++) {
                    Optional<UUID> now0 = v.syncedTether(i);
                    UUID was = v.fxLastTether[i];
                    UUID is = now0.orElse(null);
                    if (live && was == null && is != null) {
                        VesselSounds.play(v, "tether_form", 2.0F);
                    } else if (live && was != null && is == null) {
                        VesselSounds.play(v, "tether_break", 2.0F);
                    }
                    v.fxLastTether[i] = is;
                }
            }
            default -> { }
        }
    }

    // ---------------------------------------------------------------- the clot
    public static void clotTick(VesselClot c) {
        Level level = c.level();
        long now = level.getGameTime();
        if (c.tickCount == 1 && now < c.launchTick() && !c.batted()) {
            VesselSounds.playAt(level, c.position(), "clot_windup", 2.0F);
        }
        if (!c.fxLaunched && now >= c.launchTick() && !c.batted()) {
            c.fxLaunched = true;
            if (c.tickCount > 1) {
                VesselSounds.playAt(level, c.position(), "clot_launch", 2.0F);
            }
        }
        if (c.batted() && !c.fxBatted) {
            c.fxBatted = true;
            VesselSounds.playAt(level, c.position(), "bat", 2.0F);
        }
        if (c.batted()) {
            level.addParticle(PALE, c.getX(), c.getY(), c.getZ(), 0.0, 0.0, 0.0);
        } else if (now >= c.launchTick() && c.tickCount % 2 == 0) {
            level.addParticle(TAINT, c.getX(), c.getY(), c.getZ(), 0.0, 0.0, 0.0);
        }
    }

    // ---------------------------------------------------------------- the hum loop
    /** One looping layer of the hum; ends with the fight, the entity or 48 blocks of distance. */
    private static final class Hum extends AbstractTickableSoundInstance {
        private final Vessel vessel;
        private final float base;
        private final boolean pitched;

        Hum(SoundEvent event, Vessel vessel, float base, boolean pitched) {
            super(event, SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
            this.vessel = vessel;
            this.base = base;
            this.pitched = pitched;
            this.looping = true;
            this.delay = 0;
            this.volume = base * 0.8F;
            this.x = vessel.getX();
            this.y = vessel.getY();
            this.z = vessel.getZ();
        }

        @Override
        public void tick() {
            var player = Minecraft.getInstance().player;
            if (this.vessel.isRemoved() || !this.vessel.fightIsOn() || player == null || player.distanceToSqr(this.vessel) > 48.0 * 48.0) {
                this.stop();
                return;
            }
            double start = this.vessel.startQuarters() / 4.0;
            this.pitch = this.pitched ? (float) (1.1 - 0.5 * this.vessel.mass() / (start + 4.0)) : 1.0F;
            this.volume = this.base * 0.8F * (this.vessel.pullScale() == 0.0F ? 0.25F : 1.0F);
        }
    }

    static ResourceLocation id(String path) {
        return ThaumicConcilium.id(path);
    }

    static float clamp01(float f) {
        return Mth.clamp(f, 0.0F, 1.0F);
    }
}
