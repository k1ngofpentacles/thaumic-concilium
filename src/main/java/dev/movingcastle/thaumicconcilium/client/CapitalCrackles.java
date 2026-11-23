package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.block.VerdantCapitalBlockEntity;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The purple crackles a Verdant Capital throws while it unwinds (phase 5d cycle 4b; docs/design/5d-look.md, item 1 and addendum
 * A2). The list of unwinding capitals is kept from the block entity's own client-side load, change and removal ({@link #update}),
 * so the tick below returns at once unless one is unwinding near this client.
 *
 * <p>A crackle is one unregistered client particle: a jagged bolt from one of the star's bars, drawn three times along the same
 * points. First its <b>silhouette</b>: the bolt's own shape, wider and black (plain alpha blending of black darkens what is behind
 * it, so the bolt seems to drink the light round itself); then a violet fringe; then a white core. No shader of ours: under a
 * shader pack the core and fringe always read and the silhouette may soften.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID, value = Dist.CLIENT)
public final class CapitalCrackles {
    static final float CHANCE = 0.15F;
    static final float EXTRA_CHANCE = 0.20F;
    static final int MAX_LIVE = 12;
    static final double RANGE = 48.0;

    /** Unwinding capitals on this client, each with its count of live crackles. */
    private static final Map<VerdantCapitalBlockEntity, int[]> UNWINDING = new IdentityHashMap<>();

    private CapitalCrackles() {
    }

    /** The block entity's client hook: it loaded, its synced state changed, or it was removed. */
    public static void update(VerdantCapitalBlockEntity be) {
        if (be.isRemoved() || be.drive() != VerdantCapitalBlockEntity.Drive.UNWINDING) {
            UNWINDING.remove(be);
        } else {
            UNWINDING.computeIfAbsent(be, k -> new int[1]);
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (UNWINDING.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) {
            if (level == null) {
                UNWINDING.clear();
            }
            return;
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        RandomSource random = level.random;
        Iterator<Map.Entry<VerdantCapitalBlockEntity, int[]>> it = UNWINDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<VerdantCapitalBlockEntity, int[]> e = it.next();
            VerdantCapitalBlockEntity be = e.getKey();
            if (be.isRemoved() || be.getLevel() != level) {
                it.remove();
                continue;
            }
            BlockPos pos = be.getBlockPos();
            if (cam.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > RANGE * RANGE || random.nextFloat() >= CHANCE) {
                continue;
            }
            int n = random.nextFloat() < EXTRA_CHANCE ? 3 : 1;
            for (int i = 0; i < n && e.getValue()[0] < MAX_LIVE; i++) {
                spawn(level, be, random, e.getValue());
            }
        }
    }

    /** One crackle: from a random point on an edge of one of the star's small tetrahedra (as it stands now), away from the star's centre, within about 60 degrees. */
    private static void spawn(ClientLevel level, VerdantCapitalBlockEntity be, RandomSource random, int[] live) {
        BlockPos pos = be.getBlockPos();
        if (!(be.clientMotion instanceof CapitalMotion motion) || motion.radius <= 0.0F) {
            return; // never drawn yet: no pose to start from
        }
        Vec3 local = CapitalStar.pointOnPiece(random, motion);
        Vec3 out = local.subtract(0.5, CapitalStar.CENTRE_Y, 0.5);
        out = out.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 1.0, 0.0) : out.normalize();
        Vec3 dir = out.add((random.nextDouble() - 0.5) * 1.5, (random.nextDouble() - 0.5) * 1.5, (random.nextDouble() - 0.5) * 1.5);
        dir = dir.dot(out) < 0.5 * dir.length() ? out : dir.normalize();
        Vec3 start = new Vec3(pos.getX() + local.x, pos.getY() + local.y, pos.getZ() + local.z);
        float length = 0.5F + random.nextFloat() * 0.6F;
        live[0]++;
        Minecraft.getInstance().particleEngine.add(new Crackle(level, start, dir, length, 3 + random.nextInt(3), 4 + random.nextInt(4), random.nextInt(), live));
    }

    // ---------------------------------------------------------------- the particle
    private static final ResourceLocation STRIP_TEXTURE = ThaumicConcilium.id("textures/particle/capital_crackle.png");

    /** The crackle strips (left half: a soft falloff for the bolt; right half: an even shade for the silhouette), alpha blended, tested against depth but not written to it, so the silhouette never hides what is drawn after it. */
    private static final ParticleRenderType STRIP = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textures) {
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, STRIP_TEXTURE);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "thaumicconcilium:capital_crackle";
        }
    };

    static final int SILHOUETTE_RGB = 0x0A0612;
    static final float SILHOUETTE_ALPHA = 0.6F;
    static final float SILHOUETTE_HALF = 0.16F;
    /** The silhouette runs this far past both ends of the bolt and comes to a point there. */
    static final float SILHOUETTE_PAST = 0.1F;
    static final int FRINGE_RGB = 0xB040FF;
    static final float FRINGE_HALF = 0.07F;
    static final int CORE_RGB = 0xF4E8FF;
    static final float CORE_HALF = 0.025F;
    static final float JAG = 0.12F;
    /** A joint is widened to keep the ribbon's width through a bend, by at most this factor. */
    static final float MITRE_LIMIT = 1.5F;

    private static final class Crackle extends Particle {
        private final Vec3 start;
        private final Vec3 dir;
        private final Vec3 sideA;
        private final Vec3 sideB;
        private final float length;
        private final int segments;
        private final int seed;
        private final int[] live;
        private boolean counted = true;

        Crackle(ClientLevel level, Vec3 start, Vec3 dir, float length, int segments, int life, int seed, int[] live) {
            super(level, start.x, start.y, start.z);
            this.start = start;
            this.dir = dir;
            Vec3 up = Math.abs(dir.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
            this.sideA = dir.cross(up).normalize();
            this.sideB = dir.cross(this.sideA).normalize();
            this.length = length;
            this.segments = segments;
            this.lifetime = life;
            this.seed = seed;
            this.live = live;
            this.hasPhysics = false;
            this.gravity = 0.0F;
            this.setSize(2.0F, 2.0F);
        }

        @Override
        public void tick() {
            if (this.age++ >= this.lifetime) {
                this.remove();
            }
        }

        @Override
        public void remove() {
            if (this.counted) {
                this.counted = false;
                this.live[0] = Math.max(0, this.live[0] - 1);
            }
            super.remove();
        }

        /** Point {@code i} of the bolt (0 = on the bar); the points after it are knocked sideways, anew every two ticks. */
        private Vec3 point(int i) {
            Vec3 p = this.start.add(this.dir.scale(this.length * i / this.segments));
            if (i == 0) {
                return p;
            }
            int jag = this.seed + 131 * (this.age / 2) + 17 * i;
            float a = (VesselDraw.hash(jag) - 0.5F) * 2.0F * JAG;
            float b = (VesselDraw.hash(jag + 7919) - 0.5F) * 2.0F * JAG;
            return p.add(this.sideA.scale(a)).add(this.sideB.scale(b));
        }

        @Override
        public void render(VertexConsumer vc, Camera camera, float pt) {
            Vec3 cam = camera.getPosition();
            float t = this.age + pt;
            float bolt = Math.min(1.0F, Math.max(0.0F, (this.lifetime - t) / 2.0F));
            float shade = Math.min(1.0F, Math.max(0.0F, this.lifetime - t)); // the silhouette holds, then goes in the last tick
            if (shade <= 0.0F) {
                return;
            }
            int n = this.segments;
            Vec3[] pts = new Vec3[n + 1];
            for (int i = 0; i <= n; i++) {
                pts[i] = this.point(i).subtract(cam);
            }
            // 1. the silhouette: the bolt's own line, wide and dark, running a little past both ends to a point
            Vec3[] outline = new Vec3[n + 3];
            float[] wide = new float[n + 3];
            outline[0] = pts[0].subtract(pts[1].subtract(pts[0]).normalize().scale(SILHOUETTE_PAST));
            outline[n + 2] = pts[n].add(pts[n].subtract(pts[n - 1]).normalize().scale(SILHOUETTE_PAST));
            for (int i = 0; i <= n; i++) {
                outline[i + 1] = pts[i];
                wide[i + 1] = SILHOUETTE_HALF;
            }
            ribbon(vc, outline, wide, 0.5F, 1.0F, SILHOUETTE_RGB, (int) (SILHOUETTE_ALPHA * shade * 255.0F));
            if (bolt <= 0.0F) {
                return;
            }
            // 2. the fringe, 3. the core: the far end thins to a point
            float[] fringe = new float[n + 1];
            float[] core = new float[n + 1];
            for (int i = 0; i <= n; i++) {
                float taper = i == n ? 0.3F : 1.0F;
                fringe[i] = FRINGE_HALF * taper;
                core[i] = CORE_HALF * taper;
            }
            ribbon(vc, pts, fringe, 0.0F, 0.5F, FRINGE_RGB, (int) (0.8F * bolt * 255.0F));
            ribbon(vc, pts, core, 0.0F, 0.5F, CORE_RGB, (int) (bolt * 255.0F));
        }

        /**
         * One continuous ribbon through {@code pts} (camera at the origin), {@code half[i]} wide each way at point {@code i}, turned
         * to face the camera, textured from {@code u0} to {@code u1} across. Joints are mitred: each point is offset along the bisector of its two segments, so neighbouring quads
         * share an edge and no part of the ribbon is drawn twice (a doubled patch would be darker in the silhouette).
         */
        private static void ribbon(VertexConsumer vc, Vec3[] pts, float[] half, float u0, float u1, int rgb, int alpha) {
            if (alpha <= 0) {
                return;
            }
            int last = pts.length - 1;
            Vec3[] left = new Vec3[pts.length];
            Vec3[] right = new Vec3[pts.length];
            for (int i = 0; i <= last; i++) {
                Vec3 along = pts[Math.min(last, i + 1)].subtract(pts[Math.max(0, i - 1)]);
                Vec3 across = along.cross(pts[i]); // tangent x (camera to point): lies in the screen plane
                if (across.lengthSqr() < 1.0E-10) {
                    across = new Vec3(0.0, 1.0, 0.0);
                }
                across = across.normalize();
                float widen = 1.0F;
                if (i > 0 && i < last) {
                    double cos = Math.abs(along.normalize().dot(pts[i + 1].subtract(pts[i]).normalize()));
                    widen = (float) Math.min(MITRE_LIMIT, 1.0 / Math.max(0.01, cos));
                }
                Vec3 o = across.scale(half[i] * widen);
                left[i] = pts[i].subtract(o);
                right[i] = pts[i].add(o);
            }
            for (int i = 0; i < last; i++) {
                // both windings: the particle pass culls back faces, and which way a ribbon faces depends on where the camera stands
                put(vc, left[i], u0, 0.0F, rgb, alpha);
                put(vc, right[i], u1, 0.0F, rgb, alpha);
                put(vc, right[i + 1], u1, 1.0F, rgb, alpha);
                put(vc, left[i + 1], u0, 1.0F, rgb, alpha);
                put(vc, left[i + 1], u0, 1.0F, rgb, alpha);
                put(vc, right[i + 1], u1, 1.0F, rgb, alpha);
                put(vc, right[i], u1, 0.0F, rgb, alpha);
                put(vc, left[i], u0, 0.0F, rgb, alpha);
            }
        }

        private static void put(VertexConsumer vc, Vec3 p, float u, float v, int rgb, int alpha) {
            vc.addVertex((float) p.x, (float) p.y, (float) p.z).setUv(u, v).setColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, alpha).setLight(0xF000F0);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return STRIP;
        }
    }
}
