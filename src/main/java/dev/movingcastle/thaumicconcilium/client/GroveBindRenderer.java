package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.GroveBind;
import dev.movingcastle.thaumicconcilium.world.GroveSeedDeath;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/**
 * Draws the grove killing a Taint Seed (phase 5d cycle 4b, variant C of docs/design/5d-look.md): eight roots that erupt in a ring and
 * arch over the seed, tighten as it struggles, leaf clusters that burst through, and the whole mound sinking at the end. All of it is a
 * function of the bind's age (game time since its synced start), so there is no animation state to keep. The seed itself is
 * Thaumaturge's entity with its own renderer; nothing of it is touched or copied.
 *
 * <p>One entity cutout render type on our own sheet ({@code textures/entity/grove_bind.png}: bark and a leaf cluster), so it is lit,
 * casts shadows under shader packs and needs no shader of ours. {@link #fx} adds the particles.
 */
public final class GroveBindRenderer extends EntityRenderer<GroveBind> {
    private static final ResourceLocation TEXTURE = ThaumicConcilium.id("textures/entity/grove_bind.png");

    static final int ROOTS = 8;
    static final int SEGMENTS = 12;
    /** Where a root leaves the ground (blocks from the seed's axis, for a plain seed). */
    static final float RING = 1.6F;
    static final float PEAK = 2.5F;
    static final float TIGHTEN = 0.15F;
    static final float ROOT_GROW_TICKS = 40.0F;
    static final float ROOT_STAGGER_TICKS = 18.0F;
    static final float SINK = 1.25F;

    public GroveBindRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(GroveBind entity) {
        return TEXTURE;
    }

    // ---------------------------------------------------------------- the shape, as functions of age
    /** How far root {@code i} has grown (0 to 1): each starts within the first 18 ticks and takes 40, so all are locked by tick 60. */
    static float grown(int i, float age) {
        float t = Mth.clamp((age - VesselDraw.hash(i * 31 + 7) * ROOT_STAGGER_TICKS) / ROOT_GROW_TICKS, 0.0F, 1.0F);
        float inv = 1.0F - t;
        return 1.0F - inv * inv * inv;
    }

    /** How tight the roots hold (0 loose, 1 fully tightened): a steady squeeze through the struggle, with a jolt at every lash. */
    static float tighten(float age) {
        float g = Mth.clamp((age - GroveSeedDeath.LOCKED) / (float) (GroveSeedDeath.BURST_START - GroveSeedDeath.LOCKED), 0.0F, 1.0F);
        for (int lash : GroveSeedDeath.LASHES) {
            if (age >= lash) {
                g -= 0.45F * (float) Math.exp(-(age - lash) / 2.5F) - 0.3F * (float) Math.exp(-(age - lash) / 7.0F); // forced open, then pulled tighter
            }
        }
        return Mth.clamp(g, -0.4F, 1.3F);
    }

    /** How far the mound has collapsed (0 to 1) after the seed's death. */
    static float collapse(float age) {
        float e = Mth.clamp((age - GroveSeedDeath.KILL) / (float) (GroveSeedDeath.END - GroveSeedDeath.KILL), 0.0F, 1.0F);
        return e * e;
    }

    /** The point at {@code u} (0 the ground, 1 the tip on the seed) along root {@code i}, in blocks from the bind's feet. */
    static Vector3f root(int i, float u, float tighten, float k) {
        float squeeze = 1.0F - TIGHTEN * tighten;
        float r0 = RING;
        float y0 = -0.25F;
        float r1 = 1.95F * squeeze;
        float y1 = 1.5F;
        float r2 = 1.0F * squeeze;
        float y2 = (PEAK + 0.3F * VesselDraw.hash(i * 17 + 3)) * squeeze;
        float r3 = 0.3F;
        float y3 = 1.05F;
        float v = 1.0F - u;
        float b0 = v * v * v;
        float b1 = 3.0F * v * v * u;
        float b2 = 3.0F * v * u * u;
        float b3 = u * u * u;
        float r = (b0 * r0 + b1 * r1 + b2 * r2 + b3 * r3) * k;
        float y = (b0 * y0 + b1 * y1 + b2 * y2 + b3 * y3) * k;
        float angle = i * Mth.TWO_PI / ROOTS + (VesselDraw.hash(i * 13 + 5) - 0.5F) * 0.28F + (VesselDraw.hash(i * 29 + 3) - 0.5F) * 1.2F * u;
        return new Vector3f(r * Mth.cos(angle), y, r * Mth.sin(angle));
    }

    /** The age at which leaf cluster {@code j} bursts through. */
    static int burstAt(int j) {
        return GroveSeedDeath.BURST_START + j * GroveSeedDeath.BURST_EVERY;
    }

    /** Where leaf cluster {@code j} sits on the seed's shell. */
    static Vector3f cluster(int j, float k) {
        float angle = j * Mth.TWO_PI / GroveSeedDeath.BURSTS + 0.44F;
        return new Vector3f(0.7F * k * Mth.cos(angle), k * (0.45F + 0.3F * (j % 3)), 0.7F * k * Mth.sin(angle));
    }

    /** A burst's size over time: it pops out past full size and settles. */
    static float pop(float since) {
        if (since <= 0.0F) {
            return 0.0F;
        }
        float t = Math.min(1.0F, since / 5.0F);
        return t + 0.3F * Mth.sin(Math.min(since, 10.0F) / 10.0F * Mth.PI) * (since < 10.0F ? 1.0F : 0.0F);
    }

    // ---------------------------------------------------------------- drawing
    @Override
    public void render(GroveBind bind, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = bind.age(partialTick);
        if (age < 0.0F || age > GroveSeedDeath.END + 2.0F) {
            return;
        }
        float k = bind.scale();
        float tighten = tighten(age);
        float fall = collapse(age);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        pose.pushPose();
        pose.translate(0.0F, -SINK * k * fall, 0.0F);
        pose.scale(1.0F, 1.0F - 0.5F * fall, 1.0F);
        PoseStack.Pose p = pose.last();
        for (int i = 0; i < ROOTS; i++) {
            float g = grown(i, age);
            if (g > 0.01F) {
                this.root(vc, p, i, g, tighten, k, light);
            }
        }
        for (int j = 0; j < GroveSeedDeath.BURSTS; j++) {
            float since = age - burstAt(j);
            if (since <= 0.0F) {
                continue;
            }
            float s = pop(since);
            Vector3f c = cluster(j, k);
            leaves(vc, p, c.x, c.y, c.z, 0.5F * k * s, j * 0.7F, light);
            // two smaller tufts on the roots with every burst
            Vector3f a = root(j % ROOTS, 0.42F, tighten, k);
            Vector3f b = root((j + 4) % ROOTS, 0.76F, tighten, k);
            leaves(vc, p, a.x, a.y + 0.1F * k, a.z, 0.3F * k * s, j * 1.3F + 0.5F, light);
            leaves(vc, p, b.x, b.y + 0.1F * k, b.z, 0.3F * k * s, j * 1.9F + 1.1F, light);
        }
        pose.popPose();
        super.render(bind, yaw, partialTick, pose, buffers, light);
    }

    /** One root as a tapering square tube, drawn as far as it has grown. */
    private void root(VertexConsumer vc, PoseStack.Pose p, int i, float grown, float tighten, float k, int light) {
        Vector3f[] centre = new Vector3f[SEGMENTS + 1];
        Vector3f[] side = new Vector3f[SEGMENTS + 1];
        Vector3f[] over = new Vector3f[SEGMENTS + 1];
        float[] radius = new float[SEGMENTS + 1];
        for (int s = 0; s <= SEGMENTS; s++) {
            float rel = s / (float) SEGMENTS;
            float u = rel * grown;
            centre[s] = root(i, u, tighten, k);
            Vector3f tangent = root(i, Math.min(1.0F, u + 0.02F), tighten, k).sub(root(i, Math.max(0.0F, u - 0.02F), tighten, k)).normalize();
            float angle = (float) Math.atan2(centre[s].z, centre[s].x);
            Vector3f around = new Vector3f(-Mth.sin(angle), 0.0F, Mth.cos(angle)); // horizontal, across the root's own plane
            around.sub(new Vector3f(tangent).mul(around.dot(tangent))).normalize();
            side[s] = around;
            over[s] = new Vector3f(tangent).cross(around).normalize();
            radius[s] = k * Mth.lerp(rel, 0.17F, 0.04F) * (0.55F + 0.45F * grown);
        }
        float[][] corners = {{1.0F, 1.0F}, {-1.0F, 1.0F}, {-1.0F, -1.0F}, {1.0F, -1.0F}};
        for (int s = 0; s < SEGMENTS; s++) {
            float va = (s % 2) * 0.5F;
            float vb = 0.5F - va;
            for (int f = 0; f < 4; f++) {
                float[] c0 = corners[f];
                float[] c1 = corners[(f + 1) % 4];
                Vector3f a0 = corner(centre[s], side[s], over[s], radius[s], c0);
                Vector3f a1 = corner(centre[s], side[s], over[s], radius[s], c1);
                Vector3f b1 = corner(centre[s + 1], side[s + 1], over[s + 1], radius[s + 1], c1);
                Vector3f b0 = corner(centre[s + 1], side[s + 1], over[s + 1], radius[s + 1], c0);
                Vector3f normal = new Vector3f(side[s]).mul(c0[0] + c1[0]).add(new Vector3f(over[s]).mul(c0[1] + c1[1])).normalize();
                vertex(vc, p, a0, 0.0F, va, normal, light);
                vertex(vc, p, a1, 0.5F, va, normal, light);
                vertex(vc, p, b1, 0.5F, vb, normal, light);
                vertex(vc, p, b0, 0.0F, vb, normal, light);
            }
        }
    }

    private static Vector3f corner(Vector3f centre, Vector3f side, Vector3f over, float radius, float[] c) {
        return new Vector3f(centre).add(new Vector3f(side).mul(radius * c[0])).add(new Vector3f(over).mul(radius * c[1]));
    }

    /** A leaf cluster: three upright crossed planes of the leaf texture, {@code half} blocks each way from its middle. */
    private static void leaves(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float half, float turn, int light) {
        if (half <= 0.01F) {
            return;
        }
        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
        for (int n = 0; n < 3; n++) {
            float a = turn + n * Mth.PI / 3.0F;
            float dx = half * Mth.cos(a);
            float dz = half * Mth.sin(a);
            vertex(vc, p, new Vector3f(x - dx, y - half, z - dz), 0.5F, 0.5F, up, light);
            vertex(vc, p, new Vector3f(x + dx, y - half, z + dz), 1.0F, 0.5F, up, light);
            vertex(vc, p, new Vector3f(x + dx, y + half, z + dz), 1.0F, 0.0F, up, light);
            vertex(vc, p, new Vector3f(x - dx, y + half, z - dz), 0.5F, 0.0F, up, light);
        }
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose p, Vector3f at, float u, float v, Vector3f normal, int light) {
        vc.addVertex(p, at.x, at.y, at.z).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(p, normal.x, normal.y, normal.z);
    }

    // ---------------------------------------------------------------- particles (the bind's client tick)
    static final int MOTES = 30;
    static final int MOTE_FROM = 0xA8E060;
    static final int MOTE_TO = 0xFFE070;

    /** The particles of the ages just passed ({@code after}, {@code upTo}]: soil at the roots, the lashes' dust, leaves at each burst, the motes, the collapse. */
    public static void fx(GroveBind bind, long after, long upTo) {
        if (!(bind.level() instanceof ClientLevel level) || upTo - after > 20L) {
            return;
        }
        RandomSource r = level.random;
        float k = bind.scale();
        double x = bind.getX();
        double y = bind.getY();
        double z = bind.getZ();
        BlockState ground = level.getBlockState(BlockPos.containing(x, y - 0.5, z));
        BlockParticleOption soil = new BlockParticleOption(ParticleTypes.BLOCK, ground.isAir() ? Blocks.DIRT.defaultBlockState() : ground);
        BlockParticleOption leaf = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState());
        for (long age = after + 1L; age <= upTo; age++) {
            if (age <= GroveSeedDeath.LOCKED) { // the roots break the ground
                for (int n = 0; n < 3; n++) {
                    Vector3f base = root(r.nextInt(ROOTS), 0.08F, 0.0F, k);
                    level.addParticle(soil, x + base.x, y + 0.1, z + base.z, (r.nextDouble() - 0.5) * 0.1, 0.12, (r.nextDouble() - 0.5) * 0.1);
                }
            }
            for (int i = 0; i < GroveSeedDeath.LASHES.length; i++) {
                if (age == GroveSeedDeath.LASHES[i]) { // each lash shakes dust off the seed, paler every time
                    float pale = i / (float) (GroveSeedDeath.LASHES.length - 1);
                    DustParticleOptions dust = new DustParticleOptions(
                            new Vector3f(Mth.lerp(pale, 0.55F, 0.80F), Mth.lerp(pale, 0.20F, 0.78F), Mth.lerp(pale, 0.70F, 0.72F)), 1.3F);
                    for (int n = 0; n < 10; n++) {
                        level.addParticle(dust, x + (r.nextDouble() - 0.5) * 1.6 * k, y + r.nextDouble() * 1.3 * k, z + (r.nextDouble() - 0.5) * 1.6 * k,
                                0.0, 0.02, 0.0);
                    }
                }
            }
            for (int j = 0; j < GroveSeedDeath.BURSTS; j++) {
                if (age == burstAt(j)) {
                    Vector3f c = cluster(j, k);
                    for (int n = 0; n < 12; n++) {
                        level.addParticle(leaf, x + c.x, y + c.y, z + c.z, (r.nextDouble() - 0.5) * 0.5, r.nextDouble() * 0.4, (r.nextDouble() - 0.5) * 0.5);
                    }
                }
            }
            if (age == GroveSeedDeath.KILL) { // a column of green-gold motes, to be seen from far off
                for (int n = 0; n < MOTES; n++) {
                    double a = r.nextDouble() * Math.PI * 2.0;
                    double d = r.nextDouble() * 0.8 * k;
                    GlintParticle.spawn(level, x + Math.cos(a) * d, y + r.nextDouble() * 2.0 * k, z + Math.sin(a) * d, MOTE_FROM, MOTE_TO,
                            0.08F + r.nextFloat() * 0.06F, 50 + r.nextInt(21), 0.1F);
                }
            }
            if (age > GroveSeedDeath.KILL && age <= GroveSeedDeath.END) { // dust as the mound goes down
                for (int n = 0; n < 2; n++) {
                    double a = r.nextDouble() * Math.PI * 2.0;
                    double d = r.nextDouble() * 1.3 * k;
                    level.addParticle(soil, x + Math.cos(a) * d, y + 0.1, z + Math.sin(a) * d, 0.0, 0.08, 0.0);
                }
            }
        }
    }
}
