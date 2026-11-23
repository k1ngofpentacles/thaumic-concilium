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
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A small four-point star that swells, twinkles and fades as it rises (phase 5d cycle 4b): the mana pond's sparse sparkle, and the
 * green-gold motes that go up when the grove's leaves burst through the Taint Seed. Client only and never registered, like
 * {@link GlyphParticle}: it draws its own sheet ({@code textures/particle/mana_glint.png}, four star sizes) at full brightness with
 * the vanilla particle program.
 */
public final class GlintParticle extends Particle {
    private static final ResourceLocation TEXTURE = ThaumicConcilium.id("textures/particle/mana_glint.png");

    private static final ParticleRenderType SHEET = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textures) {
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, TEXTURE);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "thaumicconcilium:mana_glint";
        }
    };

    public static final int POND_FROM = 0xFFFFFF;
    public static final int POND_TO = 0xBFE9FF;

    private final float size;
    private final float rise;
    private final int from;
    private final int to;
    private final float roll;
    /** The brightest alpha it reaches. */
    private float peak = 1.0F;
    /** Whether it is one of the pond's (counted against {@link #POND_MAX_LIVE}). */
    private boolean pond;

    /** At most this many pond glints live at once on this client (the user: a subtle wonder, not a sensory overload). */
    public static final int POND_MAX_LIVE = 3;
    private static int pondLive;
    /** Cycle 4d: the pond sparkles in events. The pause between two, in ticks (6 to 14 seconds). */
    public static final int POND_PAUSE_MIN = 120;
    public static final int POND_PAUSE_MAX = 280;
    /** The game time of the next event on this client; a time far ahead (another world's clock) counts as due. */
    private static long pondNext = Long.MIN_VALUE;
    /** Events started on this client (for the harness and the notes). */
    public static int pondEvents;
    /** Ticks before it appears (an event's later glints). */
    private int delay;

    private GlintParticle(ClientLevel level, double x, double y, double z, int from, int to, float size, int life, float rise) {
        super(level, x, y, z);
        this.from = from;
        this.to = to;
        this.size = size;
        this.lifetime = life;
        this.rise = rise;
        this.roll = level.random.nextFloat() * Mth.HALF_PI;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.setSize(0.2F, 0.2F);
    }

    /** Adds one glint: colours from and to over its life, {@code size} its half width in blocks at full, {@code rise} blocks a tick. */
    public static void spawn(ClientLevel level, double x, double y, double z, int from, int to, float size, int life, float rise) {
        Minecraft.getInstance().particleEngine.add(new GlintParticle(level, x, y, z, from, to, size, life, rise));
    }

    /**
     * The mana pond's sparkle (the pond block's display-tick hook; numbers from docs/design/5d-pond-look.md). Not a rate but events: the
     * first call after the pause starts one, two or three glints rising from one spot over the block at {@code pos}, the later ones a
     * moment after the first; then nothing for 6 to 14 seconds. Vanilla's display ticks reach a pond a few times a second, so an event
     * starts close to its time without any ticking of ours.
     */
    public static void pond(Level level, BlockPos pos) {
        if (!(level instanceof ClientLevel client)) {
            return;
        }
        long now = level.getGameTime();
        if (now < pondNext && pondNext - now <= POND_PAUSE_MAX) {
            return;
        }
        RandomSource r = level.random;
        pondNext = now + POND_PAUSE_MIN + r.nextInt(POND_PAUSE_MAX - POND_PAUSE_MIN + 1);
        pondEvents++;
        double cx = pos.getX() + r.nextDouble();
        double cz = pos.getZ() + r.nextDouble();
        int n = 2 + r.nextInt(2);
        for (int k = 0; k < n && pondLive < POND_MAX_LIVE; k++) {
            double x = cx + (r.nextDouble() * 2.0 - 1.0) * 0.6;
            double z = cz + (r.nextDouble() * 2.0 - 1.0) * 0.6;
            if (!level.getFluidState(BlockPos.containing(x, pos.getY(), z)).is(level.getFluidState(pos).getType())) {
                x = cx; // off the water: it rises from the event's own spot
                z = cz;
            }
            GlintParticle p = new GlintParticle(client, x, pos.getY() + 0.9 + r.nextDouble() * 0.2, z,
                    POND_FROM, POND_TO, 0.04F + r.nextFloat() * 0.025F, 30 + r.nextInt(16), 0.006F);
            p.delay = k == 0 ? 0 : k == 1 ? 8 + r.nextInt(7) : 18 + r.nextInt(11);
            p.peak = 0.7F;
            p.pond = true;
            pondLive++;
            Minecraft.getInstance().particleEngine.add(p);
        }
    }

    @Override
    public void remove() {
        if (this.pond) {
            this.pond = false;
            pondLive = Math.max(0, pondLive - 1);
        }
        super.remove();
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.delay > 0) {
            this.delay--;
            return;
        }
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.y += this.rise;
    }

    @Override
    public void render(VertexConsumer vc, Camera camera, float pt) {
        float t = (this.age + pt) / this.lifetime;
        if (this.delay > 0 || t >= 1.0F) {
            return;
        }
        // swell in over the first fifth, hold, fade over the last third; a quick twinkle rides on top
        float env = Mth.clamp(t / 0.2F, 0.0F, 1.0F) * Mth.clamp((1.0F - t) / 0.33F, 0.0F, 1.0F);
        float twinkle = 0.75F + 0.25F * Mth.sin((this.age + pt) * 0.9F + this.roll * 4.0F);
        float a = env * twinkle * this.peak;
        if (a <= 0.1F) {
            return;
        }
        int frame = Mth.clamp((int) (env * twinkle * 4.0F), 0, 3); // the star's arms lengthen as it brightens
        float v0 = frame / 4.0F;
        float v1 = v0 + 0.25F;
        float half = this.size * (0.6F + 0.4F * env);
        int r = (int) Mth.lerp(t, (this.from >> 16) & 255, (this.to >> 16) & 255);
        int g = (int) Mth.lerp(t, (this.from >> 8) & 255, (this.to >> 8) & 255);
        int b = (int) Mth.lerp(t, this.from & 255, this.to & 255);
        int alpha = (int) (Mth.clamp(a, 0.0F, 1.0F) * 255.0F);
        var cam = camera.getPosition();
        float px = (float) (Mth.lerp(pt, this.xo, this.x) - cam.x);
        float py = (float) (Mth.lerp(pt, this.yo, this.y) - cam.y);
        float pz = (float) (Mth.lerp(pt, this.zo, this.z) - cam.z);
        Quaternionf q = new Quaternionf(camera.rotation()).rotateZ(this.roll);
        this.vertex(vc, q, px, py, pz, half, -half, 1.0F, v1, r, g, b, alpha);
        this.vertex(vc, q, px, py, pz, half, half, 1.0F, v0, r, g, b, alpha);
        this.vertex(vc, q, px, py, pz, -half, half, 0.0F, v0, r, g, b, alpha);
        this.vertex(vc, q, px, py, pz, -half, -half, 0.0F, v1, r, g, b, alpha);
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
