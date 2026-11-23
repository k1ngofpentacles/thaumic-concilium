package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.block.VerdantCapitalBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.joml.Matrix4f;

/**
 * The Verdant Capital (phase 5d cycle 4b; the user's redesign, docs/design/5d-look.md addendum A1): the star of two 3D
 * triforces ({@link CapitalStar}), and the ray of mana that drives it. The pose is what the capital is doing for its Vessel:
 * idle, slightly open and at rest; fed, spread wider with its two triforces counter-rotating; starved, back at the idle opening
 * and crawling; unwinding, drawn in as far as the pieces can go without touching, and spinning backwards. One translucent
 * cyan sphere at the centre passes through the middle of every piece (its radius is the spread); it darkens, with a violet rim,
 * when the capital starves or leaks. The renderer integrates the angle and eases the separation frame by frame, so the block entity never
 * ticks and nothing runs for a capital that is off screen.
 *
 * <p>The beam of mana passes through the exact centre of the sphere from the side the mana last came from. It does not push the
 * pieces; its pulses flow inward while fed and back out while unwinding. Only vanilla programs: the entity cutout type for the
 * star, the translucent emissive entity type for the sphere, the beacon beam's type for the beam.
 */
public final class VerdantCapitalRenderer implements BlockEntityRenderer<VerdantCapitalBlockEntity> {
    private static final ResourceLocation RAY = ThaumicConcilium.id("textures/misc/capital_ray.png");

    // The pose per drive, the spreads, the twist, the breath and the crossing live in CapitalMotion (round 5), which uses no client
    // class, so the pack test can step it; its bounds are proved by tools/capital_clearance.py.
    static final float TAU_RAY = CapitalMotion.TAU_RAY;

    // ---- the ray
    /** One tile of the beam's texture (a pulse and a faint pip) along the beam, and how many pass a point per turn of the star. */
    static final float PULSE_PITCH = 0.75F;
    static final float PULSES_PER_TURN = 4.0F;
    static final float UPSTREAM = 2.5F;
    static final float DOWNSTREAM = 0.75F;
    static final float FADE = 0.5F;
    static final float HALF_WIDTH = 1.5F / 16.0F;
    static final float CORE_HALF_WIDTH = 0.6F / 16.0F;
    static final int FED_CORE = 0xE8FFF4;
    static final int FED_EDGE = 0x4FE0B0;
    static final int UNWIND_CORE = 0xE6DCFF;
    static final int UNWIND_EDGE = 0x9B6CFF;

    public VerdantCapitalRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(VerdantCapitalBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        CapitalMotion motion = CapitalMotion.advance(be, level.getGameTime() + (double) partialTick);
        // the jiggle moves the whole capital, beam and all, so the beam still passes through the sphere's centre
        pose.pushPose();
        pose.translate(motion.offsetX, motion.offsetY, motion.offsetZ);
        CapitalStar.draw(pose, buffers, light, motion.radius, motion.lon, motion.lat, motion.turn);
        if (be.clientRay > 0.01F) {
            this.ray(be, motion, pose, buffers);
        }
        CapitalStar.sphere(pose, buffers, motion.spread, be.clientDark, false);
        pose.popPose();
    }

    private static int mix(int a, int b, float t) {
        int r = (int) Mth.lerp(t, (a >> 16) & 255, (b >> 16) & 255);
        int g = (int) Mth.lerp(t, (a >> 8) & 255, (b >> 8) & 255);
        int bl = (int) Mth.lerp(t, a & 255, b & 255);
        return (r << 16) | (g << 8) | bl;
    }

    /**
     * The beam: a straight line of light from the side the mana last came from, through the exact centre of the sphere and a little
     * way out the other side. With no side on record it comes straight down (a spark sits above). It does not touch or drive the
     * pieces: its pulses only flow, inward while the capital is fed and back out while it unwinds.
     */
    private void ray(VerdantCapitalBlockEntity be, CapitalMotion motion, PoseStack pose, MultiBufferSource buffers) {
        Direction fed = be.lastFeedSide();
        Direction side = fed != null ? fed : Direction.UP;
        Vec3 d = new Vec3(-side.getStepX(), -side.getStepY(), -side.getStepZ()); // the way the mana travels: in from that side
        Vec3 c = new Vec3(0.5, CapitalStar.CENTRE_Y, 0.5);
        Vec3 w1 = side.getAxis().isVertical() ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 w2 = d.cross(w1).normalize();
        // the pulses move with the star's own turning (so they slow, stop and reverse with it; the poles' while the rings stand
        // still on the crossing loop); a whole turn is a whole number of tiles
        float scroll = motion.rayAngle / 360.0F * PULSES_PER_TURN * PULSE_PITCH;
        float[] at = {-UPSTREAM, -UPSTREAM + FADE, 0.0F, DOWNSTREAM};
        float[] alpha = {0.0F, 1.0F, 1.0F, 0.0F};
        int edge = mix(FED_EDGE, UNWIND_EDGE, be.clientUnwind);
        int core = mix(FED_CORE, UNWIND_CORE, be.clientUnwind);
        VertexConsumer vc = buffers.getBuffer(RenderType.beaconBeam(RAY, true));
        Matrix4f m = pose.last().pose();
        for (int i = 0; i < 3; i++) {
            float sa = at[i];
            float sb = at[i + 1];
            float va = (sa - scroll) / PULSE_PITCH;
            float vb = (sb - scroll) / PULSE_PITCH;
            int aa = (int) (255.0F * alpha[i] * be.clientRay);
            int ab = (int) (255.0F * alpha[i + 1] * be.clientRay);
            Vec3 pa = c.add(d.scale(sa));
            Vec3 pb = c.add(d.scale(sb));
            strip(vc, m, pa, pb, w1, HALF_WIDTH, va, vb, edge, aa, ab);
            strip(vc, m, pa, pb, w2, HALF_WIDTH, va, vb, edge, aa, ab);
            strip(vc, m, pa, pb, w1, CORE_HALF_WIDTH, va, vb, core, aa, ab);
            strip(vc, m, pa, pb, w2, CORE_HALF_WIDTH, va, vb, core, aa, ab);
        }
    }

    /** One ribbon of the ray between two points, {@code half} wide each way along {@code across}, drawn on both faces (the beam type culls). */
    private static void strip(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 across, float half, float va, float vb, int rgb, int alphaA, int alphaB) {
        Vec3 o = across.scale(half);
        vertex(vc, m, a.subtract(o), 0.0F, va, rgb, alphaA);
        vertex(vc, m, a.add(o), 1.0F, va, rgb, alphaA);
        vertex(vc, m, b.add(o), 1.0F, vb, rgb, alphaB);
        vertex(vc, m, b.subtract(o), 0.0F, vb, rgb, alphaB);
        vertex(vc, m, b.subtract(o), 0.0F, vb, rgb, alphaB);
        vertex(vc, m, b.add(o), 1.0F, vb, rgb, alphaB);
        vertex(vc, m, a.add(o), 1.0F, va, rgb, alphaA);
        vertex(vc, m, a.subtract(o), 0.0F, va, rgb, alphaA);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Vec3 p, float u, float v, int rgb, int alpha) {
        vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, alpha).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0.0F, 1.0F, 0.0F);
    }

    @Override
    public AABB getRenderBoundingBox(VerdantCapitalBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(3.0);
    }

    // ---------------------------------------------------------------- the item: the closed star
    /** Draws the capital's item as the closed star at rest (the item model is {@code builtin/entity}). */
    public static final class Item extends BlockEntityWithoutLevelRenderer {
        static final float ITEM_SCALE = 1.6F;

        private static Item instance;

        private Item() {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        }

        static Item get() {
            if (instance == null) {
                instance = new Item();
            }
            return instance;
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            // the closed star, untwisted (nothing turns in an item, so its pieces may touch), enlarged so it fills the slot,
            // centred in the item's cube, with a faint cyan sphere through the middle of its pieces
            pose.pushPose();
            pose.translate(0.5F, 0.5F, 0.5F);
            pose.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            pose.translate(-0.5F, -CapitalStar.CENTRE_Y, -0.5F);
            CapitalStar.drawClosed(pose, buffers, light);
            CapitalStar.sphere(pose, buffers, 0.0F, 0.0F, true);
            pose.popPose();
        }
    }

    /** Client hook that gives the capital's item {@link Item}. */
    public static final class Extensions implements IClientItemExtensions {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return Item.get();
        }
    }
}
