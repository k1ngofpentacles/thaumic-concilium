package dev.movingcastle.thaumicconcilium.client;

import com.leclowndu93150.thaumaturge.client.effect.LateWorldRenderQueue;
import com.leclowndu93150.thaumaturge.client.render.TTRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.Vessel;
import dev.movingcastle.thaumicconcilium.entity.VesselBreak;
import dev.movingcastle.thaumicconcilium.entity.VesselPull;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The Vessel's look (phase 5c cycle 3, memo docs/design/5c-vessel-render.md): a black core that writes depth (entity pass), the dark halo,
 * the accretion disc haze, clots and the horizon ring (late queue, in that order), the tether ribbons and the subdued pins, and the
 * disc's rubble (entity pass). Quads only, on programs already in use; nothing per tick but the eased radius ({@link VesselFx}).
 */
public class VesselRenderer extends EntityRenderer<Vessel> {
    private static final ResourceLocation DARK = ThaumicConcilium.id("textures/misc/vessel_dark.png");
    private static final ResourceLocation RING = ThaumicConcilium.id("textures/misc/vessel_ring.png");
    private static final ResourceLocation DISC = ThaumicConcilium.id("textures/misc/vessel_disc.png");
    private static final ResourceLocation CLOT = ThaumicConcilium.id("textures/misc/vessel_clot.png");
    private static final ResourceLocation BLACK = ThaumicConcilium.id("textures/misc/vessel_black.png");
    static final ResourceLocation RIBBON = ThaumicConcilium.id("textures/misc/vessel_ribbon.png");
    static final ResourceLocation RIBBON_BODY = ThaumicConcilium.id("textures/misc/vessel_ribbon_body.png");
    private static final ResourceLocation RUBBLE = ThaumicConcilium.id("textures/block/vessel_anchor.png");

    private static final int VIOLET = 0x9B5CFF;
    private static final int PALE = 0xE8D8FF;
    private static final int GREY = 0x5C6A80;
    private static final int DEEP = 0x6A4BB0;
    private static final int FLASH = 0xB0F0FF;
    private static final float HORIZON = 2.5F;
    /** The dark halo quad: the memo says 8, run 1 measured too little dimming at 150 px (0.90 against 0.75), so 10 (gulp 13). */
    private static final float HALO_HALF = 10.0F;

    public VesselRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(Vessel entity, Frustum frustum, double x, double y, double z) {
        double dx = entity.getX() - x;
        double dy = entity.getY() - y;
        double dz = entity.getZ() - z;
        return dx * dx + dy * dy + dz * dz <= 96.0 * 96.0; // the halo is larger than the bounding box
    }

    @Override
    public ResourceLocation getTextureLocation(Vessel entity) {
        return BLACK;
    }

    /** The black's radius before easing: 0.6 + 1.4 * mass / start, between 0.6 and the horizon (2.5); 0.6 when subdued. */
    static float baseRadius(Vessel e) {
        if (e.state() == Vessel.SUBDUED) {
            return 0.6F;
        }
        double start = e.startQuarters() / 4.0;
        double r = start <= 0.0 ? 2.0 : 0.6 + 1.4 * e.mass() / start;
        return (float) Mth.clamp(r, 0.6, HORIZON);
    }

    /** Everything one frame needs, worked out once from the synced state (memo section 1's table). */
    private static final class Look {
        float core;
        float jx;
        float jy;
        int ringColor = VIOLET;
        float ringInt;
        float ringHalf = 4.0F;
        float rotRate = 1.5F;
        float haloA;
        float haloHalf = HALO_HALF;
        float flashScale;
        float flashInt;
        boolean disc;
        boolean stagger;
        float hazeSpeed = 1.0F;
        float hazeAlpha = 1.0F;
        float time;
    }

    private static Look look(Vessel e, float pt) {
        Look l = new Look();
        byte st = e.state();
        long now = e.level().getGameTime();
        l.time = now + pt;
        float shown = e.fxShown < 0.0F ? baseRadius(e) : Mth.lerp(pt, e.fxShownPrev < 0.0F ? e.fxShown : e.fxShownPrev, e.fxShown);
        boolean active = st == Vessel.ACTIVE;
        l.stagger = active && e.pullScale() == 0.0F;
        boolean gulp = active && e.pullScale() >= 1.5F;
        boolean swell = e.swelling();
        float s = swell ? Mth.clamp((l.time - e.fxSwellStart) / 60.0F, 0.0F, 1.0F) : 0.0F;
        l.core = shown;
        l.disc = active;
        if (st == Vessel.SUBDUED) {
            l.core = 0.6F;
            l.ringColor = DEEP;
            l.ringInt = 0.45F;
            l.ringHalf = 1.45F;
            l.rotRate = 0.5F;
            l.haloA = 0.20F;
            l.haloHalf = 3.0F;
        } else if (st == Vessel.DORMANT) {
            l.ringInt = 0.25F;
            l.rotRate = 0.0F;
            l.haloA = 0.30F;
        } else if (l.stagger) {
            l.core = shown * 0.85F;
            l.jx = (VesselDraw.hash((int) now) - 0.5F) * 0.10F;
            l.jy = (VesselDraw.hash((int) now * 31 + 7) - 0.5F) * 0.10F;
            l.ringColor = GREY;
            l.ringInt = 0.20F;
            l.rotRate = 0.0F;
            l.haloA = 0.25F;
            float dt = l.time - e.fxStaggerStart;
            if (dt >= 0.0F && dt <= 10.0F) {
                l.flashScale = 1.0F + dt / 10.0F;
                l.flashInt = 1.0F - dt / 10.0F;
            }
        } else if (gulp) {
            l.core = Math.min(shown * 1.08F, HORIZON);
            l.ringColor = PALE;
            l.ringInt = 1.0F;
            l.haloA = 0.80F;
            l.haloHalf = 13.0F;
        } else {
            l.ringInt = 0.60F + 0.08F * Mth.sin(l.time / 8.0F);
            l.haloA = 0.68F;
            if (swell) {
                l.core = Math.min(shown * (1.0F + 0.08F * s), HORIZON);
                l.ringColor = lerpColor(VIOLET, PALE, s);
                float period = Mth.lerp(s, 20.0F, 6.0F);
                l.ringInt = Mth.lerp(s, 0.6F, 1.0F) * (0.88F + 0.12F * Mth.sin(l.time * Mth.TWO_PI / period));
                l.haloA = Mth.lerp(s, 0.68F, 0.78F);
                l.ringHalf = Mth.lerp(s, 4.0F, 3.6F);
            }
        }
        float breakAge = e.breakAge(pt);
        if (breakAge >= 0.0F) { // the break: only the ring and a thin haze, so the hex shows; at the burst the black swells shut over it
            float k = Mth.clamp((breakAge - VesselBreak.BURST) / 8.0F, 0.0F, 1.0F);
            l.core *= k * k * (3.0F - 2.0F * k);
            l.haloA *= 0.5F + 0.5F * k;
        }
        if (active) {
            int phase = e.phase();
            l.hazeSpeed = phase == 2 ? 2.0F : phase == 1 ? 1.5F : 1.0F;
            l.hazeAlpha = (phase == 2 ? 1.3F : phase == 1 ? 1.15F : 1.0F) * (l.stagger ? 0.5F : 1.0F);
        }
        return l;
    }

    private static int lerpColor(int a, int b, float t) {
        int r = (int) Mth.lerp(t, a >> 16 & 255, b >> 16 & 255);
        int g = (int) Mth.lerp(t, a >> 8 & 255, b >> 8 & 255);
        int bl = (int) Mth.lerp(t, a & 255, b & 255);
        return r << 16 | g << 8 | bl;
    }

    @Override
    public void render(Vessel e, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        Look l = look(e, pt);
        // the core: a black 48-gon facing the camera, depth-writing, so the far half of the disc is hidden by it
        pose.pushPose();
        pose.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        pose.translate(l.jx, l.jy, 0.0F);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(BLACK));
        Matrix4f m = pose.last().pose();
        int n = l.core > 0.001F ? 48 : 0;
        for (int k = 0; k < n; k += 2) {
            float a0 = Mth.TWO_PI * k / n;
            float a1 = Mth.TWO_PI * (k + 1) / n;
            float a2 = Mth.TWO_PI * (k + 2) / n;
            coreVertex(vc, pose, m, 0.0F, 0.0F, light);
            coreVertex(vc, pose, m, Mth.cos(a0) * l.core, Mth.sin(a0) * l.core, light);
            coreVertex(vc, pose, m, Mth.cos(a1) * l.core, Mth.sin(a1) * l.core, light);
            coreVertex(vc, pose, m, Mth.cos(a2) * l.core, Mth.sin(a2) * l.core, light);
        }
        pose.popPose();
        if (l.disc) {
            this.rubble(e, pt, pose, buffers, light);
        }
        Vec3 origin = e.getPosition(pt);
        this.shards(e, pt, pose, buffers, origin);
        float breakAge = e.breakAge(pt);
        if (breakAge >= 0.0F) {
            this.breaking(e, breakAge, pose, buffers, light, origin);
        }
        LateWorldRenderQueue.enqueue(origin, (lp, lb) -> this.late(e, l, lp, lb, origin, pt));
    }

    private static void coreVertex(VertexConsumer vc, PoseStack pose, Matrix4f m, float x, float y, int light) {
        vc.addVertex(m, x, y, 0.0F).setColor(255, 255, 255, 255).setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose.last(), 0.0F, 0.0F, 1.0F);
    }

    // ---------------------------------------------------------------- the disc's plane (cycle 2's pure function, interpolated)
    private static float discTime(Vessel e, float pt) {
        return VesselTime.pinned >= 0L ? (float) VesselTime.pinned : e.level().getGameTime() + pt;
    }

    private static Vec3 discNormal(float time) {
        long t0 = (long) Math.floor(time);
        float f = time - t0;
        double[] a = VesselPull.discNormal(t0, TCConfig.DISC_TILT.get(), TCConfig.DISC_PRECESSION.get());
        double[] b = VesselPull.discNormal(t0 + 1, TCConfig.DISC_TILT.get(), TCConfig.DISC_PRECESSION.get());
        return new Vec3(Mth.lerp(f, a[0], b[0]), Mth.lerp(f, a[1], b[1]), Mth.lerp(f, a[2], b[2])).normalize();
    }

    /** The two in-plane axes of a normal. */
    private static Vec3[] axes(Vec3 normal) {
        Vec3 u = normal.cross(new Vec3(1, 0, 0)).normalize();
        Vec3 v = normal.cross(u).normalize();
        return new Vec3[] {u, v};
    }

    private static Vec3 onPlane(Vec3[] ax, double radius, double angle) {
        return ax[0].scale(radius * Math.cos(angle)).add(ax[1].scale(radius * Math.sin(angle)));
    }

    // ---------------------------------------------------------------- rubble (entity pass)
    private void rubble(Vessel e, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        float tt = discTime(e, pt);
        Vec3[] ax = axes(discNormal(tt));
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(RUBBLE));
        for (int i = 0; i < 6; i++) {
            int seed = e.getId() * 131 + i * 17;
            double radius = 4.3 + 1.4 * ((i * 5) % 6) / 5.0;
            double angle = VesselDraw.hash(seed) * Mth.TWO_PI + tt * 0.06 * Math.pow(4.0 / radius, 1.5);
            float half = 0.125F + 0.075F * VesselDraw.hash(seed + 3);
            Vec3 at = onPlane(ax, radius, angle);
            Vector3f axis = new Vector3f(VesselDraw.hash(seed + 5) - 0.5F, VesselDraw.hash(seed + 6) - 0.5F, VesselDraw.hash(seed + 7) - 0.5F + 0.2F).normalize();
            pose.pushPose();
            pose.translate(at.x, at.y, at.z);
            pose.mulPose(new Quaternionf().rotateAxis((float) Math.toRadians(4.0 * tt), axis.x, axis.y, axis.z));
            cube(vc, pose, half, light);
            pose.popPose();
        }
    }

    private static void cube(VertexConsumer vc, PoseStack pose, float h, int light) {
        cube(vc, pose, h, light, 0.0F, 0.0F, 1.0F, 1.0F);
    }

    /** A cube whose every face shows the texture's (u0, v0)-(u1, v1) rectangle. */
    private static void cube(VertexConsumer vc, PoseStack pose, float h, int light, float u0, float v0, float u1, float v1) {
        float[][] faces = {
                {0, 0, 1, -h, -h, h, h, -h, h, h, h, h, -h, h, h}, {0, 0, -1, h, -h, -h, -h, -h, -h, -h, h, -h, h, h, -h},
                {1, 0, 0, h, -h, h, h, -h, -h, h, h, -h, h, h, h}, {-1, 0, 0, -h, -h, -h, -h, -h, h, -h, h, h, -h, h, -h},
                {0, 1, 0, -h, h, h, h, h, h, h, h, -h, -h, h, -h}, {0, -1, 0, -h, -h, -h, h, -h, -h, h, -h, h, -h, -h, h}};
        float[] us = {u0, u1, u1, u0};
        float[] vs = {v1, v1, v0, v0};
        Matrix4f m = pose.last().pose();
        for (float[] f : faces) {
            for (int k = 0; k < 4; k++) {
                vc.addVertex(m, f[3 + k * 3], f[4 + k * 3], f[5 + k * 3]).setColor(255, 255, 255, 255).setUv(us[k], vs[k])
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(), f[0], f[1], f[2]);
            }
        }
    }

    // ---------------------------------------------------------------- the hovering anchor shards (cycle 3b, memo 5c-anchor-shard.md section 4)
    /** The block state at anchor {@code i}, or null when no anchor block is there (then there is no shard and no pin). */
    static net.minecraft.world.level.block.state.BlockState anchorState(Vessel e, int i) {
        Optional<BlockPos> pos = e.syncedAnchor(i);
        if (pos.isEmpty()) {
            return null;
        }
        net.minecraft.world.level.block.state.BlockState s = e.level().getBlockState(pos.get());
        return s.is(dev.movingcastle.thaumicconcilium.registry.TCBlocks.ANCHOR.get()) ? s : null;
    }

    /** The shard's centre above the block's base, in blocks, for a state (the eased value in {@code fxShard} follows it). */
    static float shardTarget(Vessel e, BlockPos pos, net.minecraft.world.level.block.state.BlockState s) {
        for (Player p : e.level().players()) {
            double dx = p.getX() - (pos.getX() + 0.5);
            double dz = p.getZ() - (pos.getZ() + 0.5);
            if (dx * dx + dz * dz <= 0.49 && p.getY() >= pos.getY() + 0.4 && p.getY() <= pos.getY() + 1.6) {
                return 2.75F;
            }
        }
        float vary = (VesselDraw.hash(pos.hashCode() + 11) - 0.5F) * 0.24F; // each anchor's own hover height
        if (s.getValue(dev.movingcastle.thaumicconcilium.block.AnchorBlock.TETHERED)) {
            return 1.55F + vary;
        }
        return switch (s.getValue(dev.movingcastle.thaumicconcilium.block.AnchorBlock.CHARGES)) {
            case 2 -> 1.30F + vary;
            case 1 -> 1.20F + vary;
            default -> 0.70F;
        };
    }

    /** Bob periods in ticks (each divides the 7200-tick wrap of {@link #shardTime}, so the wrap is never seen). */
    private static final float[] BOB_PERIODS = {60.0F, 72.0F, 80.0F, 90.0F};

    /** Game time wrapped to six minutes: a float keeps its precision, and every spin and bob below repeats exactly across the wrap. */
    private static float shardTime(Vessel e, float pt) {
        return (float) (e.level().getGameTime() % 7200L) + pt;
    }

    /**
     * The hovering shards (cycle 3c): each bobs on a sine and turns at a steady rate, as an item does on an infusion pedestal
     * (one degree a tick there), with its own phase, bob period and height (the height variance is in {@link #shardTarget}).
     * Tethered, it turns three times as fast on a tighter bob; cooling, it lies on the stone. Plain hex stone: the glyphs are particles.
     */
    private void shards(Vessel e, float pt, PoseStack pose, MultiBufferSource buffers, Vec3 origin) {
        float time = shardTime(e, pt);
        for (int i = 0; i < 4; i++) {
            net.minecraft.world.level.block.state.BlockState s = anchorState(e, i);
            if (s == null || e.fxShard[i] < 0.0F) {
                continue;
            }
            BlockPos pos = e.syncedAnchor(i).get();
            boolean tethered = s.getValue(dev.movingcastle.thaumicconcilium.block.AnchorBlock.TETHERED);
            int charges = s.getValue(dev.movingcastle.thaumicconcilium.block.AnchorBlock.CHARGES);
            int h = pos.hashCode();
            float phase = VesselDraw.hash(h);
            float bob = 0.0F;
            float spin = phase * 360.0F;
            float roll = 20.0F;
            if (!tethered && charges == 0) {
                roll = 70.0F;
            } else {
                float period = tethered ? 30.0F : BOB_PERIODS[(int) (VesselDraw.hash(h + 3) * 3.999F)];
                bob = (tethered ? 0.03F : 0.055F) * Mth.sin(Mth.TWO_PI * (time / period + phase));
                spin += (tethered ? 3.0F : 1.0F) * time;
            }
            int light = net.minecraft.client.renderer.LevelRenderer.getLightColor(e.level(), pos.above());
            pose.pushPose();
            pose.translate(pos.getX() + 0.5 - origin.x, pos.getY() + e.fxShard[i] + bob - origin.y, pos.getZ() + 0.5 - origin.z);
            pose.mulPose(new Quaternionf().rotateY((float) Math.toRadians(spin)));
            pose.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(roll)));
            pose.scale(0.10F, 0.18F, 0.04F);
            cube(buffers.getBuffer(RenderType.entityCutoutNoCull(RUBBLE)), pose, 1.0F, light, 5.0F / 16.0F, 3.0F / 16.0F, 11.0F / 16.0F, 13.0F / 16.0F);
            pose.popPose();
        }
    }

    // ---------------------------------------------------------------- the break sequence (cycle 3c; timeline in VesselBreak)
    /**
     * Draws the break from its age alone: the cracks of light and the burst's flash at the hex's pose, the four shard models on
     * their arcs to the synced landing spots, and (when no Sable sub-level is playing the hex) the hex itself as a stand-in.
     */
    private void breaking(Vessel e, float age, PoseStack pose, MultiBufferSource buffers, int light, Vec3 origin) {
        if (age < VesselBreak.BURST) {
            float crack = VesselBreak.crack(age);
            HexRenderer.breakCrack = crack; // the sub-level's own hex whitens its runes from this (read by its block entity renderer)
            HexRenderer.breakCrackAt = e.level().getGameTime();
            pose.pushPose();
            pose.translate(0.0, -2.5 * (1.0 - VesselBreak.rise(age)), 0.0); // the hex's middle plane, 2.5 below the Vessel at the start
            pose.mulPose(new Quaternionf().rotateY((float) VesselBreak.yaw(age)).rotateX((float) VesselBreak.tilt(age)));
            if (!e.breakHasSubLevel()) {
                pose.pushPose();
                pose.translate(0.0, -0.5, 0.0);
                HexRenderer.drawBreaking(pose, buffers, light, crack, (float) (e.level().getGameTime() % 360L) + (age - (float) Math.floor(age)));
                pose.popPose();
            }
            if (crack > 0.0F) {
                VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
                Matrix4f m = pose.last().pose();
                for (int k = 0; k < 9; k++) {
                    int seed = e.getId() * 53 + k * 29;
                    float born = 0.08F * k; // the cracks open one after another
                    float grown = Mth.clamp((crack - born) / (1.0F - born), 0.0F, 1.0F);
                    if (grown <= 0.0F) {
                        continue;
                    }
                    float az = Mth.TWO_PI * (k / 9.0F + 0.2F * VesselDraw.hash(seed));
                    float el = (VesselDraw.hash(seed + 1) - 0.35F) * 2.2F;
                    float len = grown * (2.0F + 3.0F * VesselDraw.hash(seed + 2));
                    float flick = 0.7F + 0.3F * VesselDraw.hash(seed + (int) (age * 2.0F));
                    Vector3f dir = new Vector3f(Mth.cos(az) * Mth.cos(el), Mth.sin(el), Mth.sin(az) * Mth.cos(el));
                    beam(vc, m, dir, 0.4F, len, 0.05F + 0.10F * grown, (int) (230.0F * flick * Math.min(1.0F, grown * 3.0F)));
                }
            }
            pose.popPose();
            return;
        }
        float since = age - VesselBreak.BURST;
        if (since < 10.0F) { // the burst: shards of light thrown outward, thinning and fading as they go
            VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
            Matrix4f m = pose.last().pose();
            float k = since / 10.0F;
            for (int j = 0; j < 22; j++) {
                int seed = e.getId() * 71 + j * 13;
                float az = Mth.TWO_PI * VesselDraw.hash(seed);
                float el = (VesselDraw.hash(seed + 1) - 0.5F) * 3.0F;
                Vector3f dir = new Vector3f(Mth.cos(az) * Mth.cos(el), Mth.sin(el), Mth.sin(az) * Mth.cos(el));
                float reach = 5.0F + 6.0F * VesselDraw.hash(seed + 2);
                float head = 0.6F + reach * (1.0F - (1.0F - k) * (1.0F - k)); // fast out, slowing
                beam(vc, m, dir, Math.max(0.2F, head - 1.2F - 1.5F * k), head, 0.16F * (1.0F - k), (int) (255.0F * (1.0F - k)));
            }
        }
        double t = VesselBreak.flight(age);
        int[] shapes = dev.movingcastle.thaumicconcilium.block.AnchorBlock.shapesFor(e.getUUID());
        for (int i = 0; i < 4; i++) {
            Optional<BlockPos> anchor = e.syncedAnchor(i);
            if (anchor.isEmpty() || anchorState(e, i) != null) {
                continue; // nothing to throw, or it has landed
            }
            Vec3 to = Vec3.atBottomCenterOf(anchor.get()).subtract(origin);
            Vec3 at = VesselBreak.shardAt(Vec3.ZERO, to, t);
            int seed = e.getId() * 97 + i * 31;
            Vector3f axis = new Vector3f(VesselDraw.hash(seed) - 0.5F, 0.4F * (VesselDraw.hash(seed + 1) - 0.5F), VesselDraw.hash(seed + 2) - 0.5F).normalize();
            float left = (float) (1.0 - t);
            float tumble = (float) Math.toRadians(900.0) * left * left; // it comes to rest square on its spot
            net.minecraft.world.level.block.state.BlockState state = dev.movingcastle.thaumicconcilium.registry.TCBlocks.ANCHOR.get().defaultBlockState()
                    .setValue(dev.movingcastle.thaumicconcilium.block.AnchorBlock.SHAPE, shapes[i]);
            int lit = net.minecraft.client.renderer.LevelRenderer.getLightColor(e.level(), BlockPos.containing(origin.add(at)));
            pose.pushPose();
            pose.translate(at.x, at.y + 0.25, at.z);
            pose.mulPose(new Quaternionf().rotateAxis(tumble, axis.x, axis.y, axis.z));
            pose.translate(-0.5, -0.25, -0.5);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, pose, buffers, lit, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }

    /** A beam of light from {@code start} to {@code end} along a direction: two crossed quads, bright at the root, nothing at the tip. */
    private static void beam(VertexConsumer vc, Matrix4f m, Vector3f dir, float start, float end, float width, int alpha) {
        Vector3f side = new Vector3f(dir).cross(0.0F, 1.0F, 0.0F);
        if (side.lengthSquared() < 1.0e-4F) {
            side.set(1.0F, 0.0F, 0.0F);
        }
        side.normalize().mul(width);
        Vector3f up = new Vector3f(dir).cross(side).normalize().mul(width);
        Vector3f a = new Vector3f(dir).mul(start);
        Vector3f b = new Vector3f(dir).mul(end);
        for (Vector3f w : new Vector3f[] {side, up}) {
            for (int pass = 0; pass < 2; pass++) { // both windings: seen from either side
                float sgn = pass == 0 ? 1.0F : -1.0F;
                vc.addVertex(m, a.x - w.x * sgn, a.y - w.y * sgn, a.z - w.z * sgn).setColor(236, 220, 255, alpha);
                vc.addVertex(m, a.x + w.x * sgn, a.y + w.y * sgn, a.z + w.z * sgn).setColor(236, 220, 255, alpha);
                vc.addVertex(m, b.x + w.x * sgn * 0.2F, b.y + w.y * sgn * 0.2F, b.z + w.z * sgn * 0.2F).setColor(160, 102, 255, 0);
                vc.addVertex(m, b.x - w.x * sgn * 0.2F, b.y - w.y * sgn * 0.2F, b.z - w.z * sgn * 0.2F).setColor(160, 102, 255, 0);
            }
        }
    }

    // ---------------------------------------------------------------- the late layers
    private void late(Vessel e, Look l, PoseStack pose, MultiBufferSource b, Vec3 origin, float pt) {
        RenderType halo = TTRenderTypes.fxTranslucent(DARK);
        VesselDraw.billboard(b, pose, halo, l.haloHalf, 0.0F, 0xFFFFFF, 1.0F, l.haloA);
        VesselDraw.flush(b, halo);
        if (l.disc) {
            this.haze(e, l, pose, b, pt);
            this.clots(e, l, pose, b, pt);
        }
        RenderType ring = TTRenderTypes.fxAdditiveAlphaTest(RING);
        VesselDraw.billboard(b, pose, ring, l.ringHalf, l.rotRate * l.time, l.ringColor, l.ringInt, 1.0F);
        if (l.flashInt > 0.0F) {
            VesselDraw.billboard(b, pose, ring, l.ringHalf * l.flashScale, 0.0F, FLASH, l.flashInt, 1.0F);
        }
        VesselDraw.flush(b, ring);
        RenderType body = TTRenderTypes.fxTranslucent(RIBBON_BODY);
        this.tethers(e, l, pose, b, body, origin, pt, true);
        VesselDraw.flush(b, body);
        RenderType ribbon = TTRenderTypes.fxAdditiveAlphaTest(RIBBON);
        this.tethers(e, l, pose, b, ribbon, origin, pt, false);
        VesselDraw.flush(b, ribbon);
    }

    private void haze(Vessel e, Look l, PoseStack pose, MultiBufferSource b, float pt) {
        float tt = discTime(e, pt);
        Vec3[] ax = axes(discNormal(tt));
        RenderType type = TTRenderTypes.fxTranslucent(DISC);
        double inner = TCConfig.DISC_INNER.get();
        double outer = TCConfig.DISC_OUTER.get();
        float[][] copies = {{0.9F, 0.38F}, {-0.5F, 0.22F}};
        VertexConsumer vc = b.getBuffer(type);
        Matrix4f m = pose.last().pose();
        int segs = 48;
        for (float[] c : copies) {
            double off = Math.toRadians(c[0] * tt * l.hazeSpeed);
            int alpha = Mth.clamp((int) (c[1] * l.hazeAlpha * 255.0F), 0, 255);
            for (int j = 0; j < segs; j++) {
                double a0 = Mth.TWO_PI * j / segs + off;
                double a1 = Mth.TWO_PI * (j + 1) / segs + off;
                float u0 = j / (float) segs;
                float u1 = (j + 1) / (float) segs;
                hazeVertex(vc, m, onPlane(ax, inner, a0), u0, 0.0F, alpha);
                hazeVertex(vc, m, onPlane(ax, outer, a0), u0, 1.0F, alpha);
                hazeVertex(vc, m, onPlane(ax, outer, a1), u1, 1.0F, alpha);
                hazeVertex(vc, m, onPlane(ax, inner, a1), u1, 0.0F, alpha);
            }
        }
        VesselDraw.flush(b, type);
    }

    private static void hazeVertex(VertexConsumer vc, Matrix4f m, Vec3 p, float u, float v, int alpha) {
        vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setUv(u, v).setColor(0x6B, 0x2E, 0x8F, alpha).setLight(VesselDraw.LIGHT);
    }

    private void clots(Vessel e, Look l, PoseStack pose, MultiBufferSource b, float pt) {
        float tt = discTime(e, pt);
        Vec3[] ax = axes(discNormal(tt));
        RenderType type = TTRenderTypes.fxTranslucent(CLOT);
        for (int i = 0; i < 12; i++) {
            int seed = e.getId() * 977 + i * 13;
            double radius = 4.2 + 1.6 * ((i * 5) % 12) / 11.0;
            double angle = VesselDraw.hash(seed) * Mth.TWO_PI + tt * 0.06 * Math.pow(4.0 / radius, 1.5);
            float half = 0.22F + 0.16F * VesselDraw.hash(seed + 1);
            Vec3 at = onPlane(ax, radius, angle);
            pose.pushPose();
            pose.translate(at.x, at.y, at.z);
            VesselDraw.billboard(b, pose, type, half, 0.0F, 0xFFFFFF, 1.0F, 1.0F);
            pose.popPose();
        }
        VesselDraw.flush(b, type);
    }

    // ---------------------------------------------------------------- tethers and pins (light ribbons)
    private void tethers(Vessel e, Look l, PoseStack pose, MultiBufferSource b, RenderType type, Vec3 origin, float pt, boolean bodyPass) {
        Minecraft mc = Minecraft.getInstance();
        float scroll = 0.05F * l.time * (l.stagger ? 2.0F : 1.0F);
        for (int i = 0; i < 4; i++) {
            Optional<UUID> owner = e.syncedTether(i);
            if (owner.isEmpty()) {
                continue;
            }
            Player p = e.level().getPlayerByUUID(owner.get());
            if (p == null) {
                continue; // not loaded on this client
            }
            Vec3 hand = handPoint(mc, p, pt);
            if (bodyPass) {
                VesselDraw.ribbon(b, pose, type, hand.subtract(origin), Vec3.ZERO, 0.10F, 0.30F, 0x7A3DF0, 1.0F, l.time, scroll, 0.08F, 217);
            } else {
                VesselDraw.ribbon(b, pose, type, hand.subtract(origin), Vec3.ZERO, 0.035F, 0.105F, 0x5A2ACF, 0.50F, l.time, scroll, 0.08F, 255);
            }
        }
        if (e.state() == Vessel.SUBDUED) {
            for (int i = 0; i < 4; i++) {
                Optional<BlockPos> anchor = e.syncedAnchor(i);
                if (anchor.isEmpty()) {
                    continue;
                }
                if (anchorState(e, i) == null || e.fxShard[i] < 0.0F) {
                    continue; // no anchor block there: no shard and no pin
                }
                Vec3 start = Vec3.atCenterOf(anchor.get()).add(0.0, e.fxShard[i] - 0.5, 0.0).subtract(origin);
                Vec3 end = start.normalize().scale(0.6);
                if (bodyPass) {
                    VesselDraw.ribbon(b, pose, type, start, end, 0.12F, 0.12F, 0x7A3DF0, 1.0F, l.time, 0.02F * l.time, 0.0F, 217);
                } else {
                    VesselDraw.ribbon(b, pose, type, start, end, 0.042F, 0.042F, 0x5A2ACF, 0.40F, l.time, 0.02F * l.time, 0.0F, 255);
                }
            }
        }
    }

    /** The drawer's hand: camera-relative for the local player in first person, otherwise the body plus a hand-side offset. */
    private static Vec3 handPoint(Minecraft mc, Player p, float pt) {
        float side = mc.options.mainHand().get() == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        if (p == mc.player && mc.options.getCameraType().isFirstPerson()) {
            Camera cam = mc.gameRenderer.getMainCamera();
            Vector3f look = cam.getLookVector();
            Vector3f up = cam.getUpVector();
            Vector3f left = cam.getLeftVector();
            return cam.getPosition().add(look.x * 0.5, look.y * 0.5, look.z * 0.5).add(-left.x * 0.35 * side, -left.y * 0.35 * side, -left.z * 0.35 * side)
                    .add(-up.x * 0.30, -up.y * 0.30, -up.z * 0.30);
        }
        double yaw = Math.toRadians(Mth.rotLerp(pt, p.yBodyRotO, p.yBodyRot));
        return p.getPosition(pt).add(-Math.cos(yaw) * 0.35 * side, 1.1, -Math.sin(yaw) * 0.35 * side);
    }
}
