package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A rune that has left the stone (phase 5c cycle 3c): one of the hex's sixteen glyphs, rising from a Vessel anchor and dancing as it
 * goes. It sways (two slow sines across its path), flickers (its light steps every few ticks), turns (it swings about the vertical
 * like a hanging sign, and rocks a little in its own plane) and fades out as it floats away.
 *
 * <p>Client only and never registered: it is built here and handed to the particle engine, and it draws the hex's own ink sheet
 * ({@code textures/block/hex/runes_ink.png}, sixteen 16x16 glyphs) through its own render type, so there is no particle type to
 * sync and no atlas sprite. Nothing spawns it unless a Vessel is loaded and its anchor is near the camera ({@link VesselFx}).
 */
public final class GlyphParticle extends Particle {
    private static final ResourceLocation INK = ThaumicConcilium.id("textures/block/hex/runes_ink.png");

    /** The ink sheet, translucent, full bright, written to depth (the particle shader drops the empty texels). */
    private static final ParticleRenderType SHEET = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textures) {
            RenderSystem.depthMask(true);
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, INK);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "thaumicconcilium:glyph";
        }
    };

    private double baseX;
    private double baseZ;
    /** A flung glyph's speed (the burst), blocks per tick, slowing; zero for one that only rises. */
    private double flingX;
    private double flingY;
    private double flingZ;
    private final float size;
    private final float peak;
    private final float u0;
    private final float v0;
    private final float swayA;
    private final float swayB;
    private final float swayPhaseA;
    private final float swayPhaseB;
    private final float swayRadius;
    private final float turnSpeed;
    private final float turnPhase;
    private final float rockPhase;
    private final int seed;
    private float rise;
    private final float riseTop;
    private float turn;
    private float turnO;
    private float flicker = 1.0F;
    private float flickerO = 1.0F;

    private GlyphParticle(ClientLevel level, double x, double y, double z, int rgb, float size, int life, float riseTop, float peak, int seed) {
        super(level, x, y, z);
        this.baseX = x;
        this.baseZ = z;
        this.seed = seed;
        this.size = size;
        this.peak = peak;
        this.lifetime = life;
        this.riseTop = riseTop;
        this.rise = riseTop * 0.3F;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.rCol = ((rgb >> 16) & 255) / 255.0F;
        this.gCol = ((rgb >> 8) & 255) / 255.0F;
        this.bCol = (rgb & 255) / 255.0F;
        this.alpha = 0.0F;
        int glyph = Math.floorMod(seed, 16);
        this.u0 = (glyph % 4) / 4.0F;
        this.v0 = (glyph / 4) / 4.0F;
        this.swayA = 0.07F + 0.06F * VesselDraw.hash(seed + 1);
        this.swayB = 0.05F + 0.07F * VesselDraw.hash(seed + 2);
        this.swayPhaseA = Mth.TWO_PI * VesselDraw.hash(seed + 3);
        this.swayPhaseB = Mth.TWO_PI * VesselDraw.hash(seed + 4);
        this.swayRadius = 0.10F + 0.10F * VesselDraw.hash(seed + 5);
        this.turnSpeed = (VesselDraw.hash(seed + 6) < 0.5F ? -1.0F : 1.0F) * (0.05F + 0.07F * VesselDraw.hash(seed + 7));
        this.turnPhase = Mth.TWO_PI * VesselDraw.hash(seed + 8);
        this.rockPhase = Mth.TWO_PI * VesselDraw.hash(seed + 9);
        this.turn = this.turnPhase;
        this.turnO = this.turn;
        this.setSize(0.3F, 0.3F);
    }

    /**
     * Adds one glyph. {@code rgb} its colour, {@code size} its half height in blocks, {@code life} ticks, {@code rise} its top
     * rising speed in blocks per tick, {@code peak} its brightest alpha.
     */
    public static void spawn(ClientLevel level, Vec3 at, int rgb, float size, int life, float rise, float peak) {
        int seed = level.random.nextInt();
        Minecraft.getInstance().particleEngine.add(new GlyphParticle(level, at.x, at.y, at.z, rgb, size, life, rise, peak, seed));
    }

    /** Adds one glyph thrown from {@code at} at {@code speed} (blocks per tick): it slows, then drifts and dances like the others. */
    public static void fling(ClientLevel level, Vec3 at, Vec3 speed, int rgb, float size, int life, float peak) {
        GlyphParticle p = new GlyphParticle(level, at.x, at.y, at.z, rgb, size, life, 0.01F, peak, level.random.nextInt());
        p.flingX = speed.x;
        p.flingY = speed.y;
        p.flingZ = speed.z;
        Minecraft.getInstance().particleEngine.add(p);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.turnO = this.turn;
        this.flickerO = this.flicker;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.rise += (this.riseTop - this.rise) * 0.04F; // it lifts off slowly and gathers way
        float t = this.age;
        float grow = Math.min(1.0F, t / 20.0F); // the sway widens as it leaves the stone
        double sx = this.swayRadius * grow * Mth.sin(t * this.swayA + this.swayPhaseA);
        double sz = this.swayRadius * grow * Mth.sin(t * this.swayB + this.swayPhaseB);
        this.baseX += this.flingX;
        this.baseZ += this.flingZ;
        this.setPos(this.baseX + sx, this.y + this.rise + this.flingY, this.baseZ + sz);
        this.flingX *= 0.88;
        this.flingY *= 0.88;
        this.flingZ *= 0.88;
        this.turn += this.turnSpeed * (0.6F + 0.4F * Mth.sin(t * 0.11F + this.turnPhase));
        if (this.age % 3 == 0) { // the flicker: a new light level every three ticks, mostly bright, now and then a dip
            float h = VesselDraw.hash(this.seed + 31 * this.age);
            this.flicker = h < 0.15F ? 0.35F : 0.75F + 0.25F * h;
        }
    }

    @Override
    public void render(VertexConsumer vc, Camera camera, float pt) {
        Vec3 cam = camera.getPosition();
        float px = (float) (Mth.lerp(pt, this.xo, this.x) - cam.x);
        float py = (float) (Mth.lerp(pt, this.yo, this.y) - cam.y);
        float pz = (float) (Mth.lerp(pt, this.zo, this.z) - cam.z);
        float t = this.age + pt;
        float life = t / this.lifetime;
        float in = Mth.clamp(t / 8.0F, 0.0F, 1.0F);
        float out = Mth.clamp((1.0F - life) / 0.45F, 0.0F, 1.0F);
        float a = this.peak * in * out * out * Mth.lerp(pt, this.flickerO, this.flicker);
        if (a <= 0.01F) {
            return;
        }
        float half = this.size * (0.75F + 0.25F * in) * (0.8F + 0.2F * out);
        // the turn: the glyph swings about the vertical; seen from behind it is mirrored, and it never goes fully edge-on
        float c = Mth.cos(Mth.lerp(pt, this.turnO, this.turn));
        boolean back = c < 0.0F;
        float halfW = half * Math.max(0.12F, Math.abs(c));
        float rock = 0.30F * Mth.sin(t * 0.09F + this.rockPhase);
        Quaternionf q = new Quaternionf(camera.rotation()).rotateZ(rock);
        float ua = back ? this.u0 + 0.25F : this.u0;
        float ub = back ? this.u0 : this.u0 + 0.25F;
        float va = this.v0;
        float vb = this.v0 + 0.25F;
        int r = (int) (this.rCol * 255.0F);
        int g = (int) (this.gCol * 255.0F);
        int b = (int) (this.bCol * 255.0F);
        int alpha = (int) (Mth.clamp(a, 0.0F, 1.0F) * 255.0F);
        this.vertex(vc, q, px, py, pz, halfW, -half, ub, vb, r, g, b, alpha);
        this.vertex(vc, q, px, py, pz, halfW, half, ub, va, r, g, b, alpha);
        this.vertex(vc, q, px, py, pz, -halfW, half, ua, va, r, g, b, alpha);
        this.vertex(vc, q, px, py, pz, -halfW, -half, ua, vb, r, g, b, alpha);
    }

    private void vertex(VertexConsumer vc, Quaternionf q, float px, float py, float pz, float x, float y, float u, float v, int r, int g, int b, int a) {
        Vector3f p = new Vector3f(x, y, 0.0F).rotate(q).add(px, py, pz);
        vc.addVertex(p.x(), p.y(), p.z()).setUv(u, v).setColor(r, g, b, a).setLight(0xF000F0);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return SHEET;
    }
}
