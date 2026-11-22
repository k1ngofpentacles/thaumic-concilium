package dev.movingcastle.thaumicconcilium.client;

import com.leclowndu93150.thaumaturge.client.effect.OccludingEffectRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.movingcastle.thaumicconcilium.entity.mob.LesserCrimsonPortal;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/**
 * The Lesser Crimson Portal: Thaumaturge's own animated portal swirl (its texture and its late, occluding effect queue, by reference),
 * 2.6 x 3 blocks, opening linearly over 50 ticks of age; each arrival shrinks then swells it, a hit wobbles it, damage makes it flicker.
 * Colour stays Thaumaturge's red (user decision, cycle 6b): the original also borrowed its host's portal; its violet is a recorded deviation.
 */
public final class LesserPortalRenderer extends EntityRenderer<LesserCrimsonPortal> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumaturge", "textures/misc/cultist_portal.png");
    private static final int FRAMES = 32;
    /** The original opens over 50 ticks. */
    private static final float OPEN_TICKS = 50.0F;
    /** 1.3 (the original's) x 77/63: Thaumaturge's texture fills less of its quad, so the visible oval matches the original's 2.0 blocks. */
    private static final float FULL_HALF_WIDTH = 1.6F;
    private static final float HALF_HEIGHT = 1.5F;
    private static final int LIGHT = 15728860;

    public LesserPortalRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(LesserCrimsonPortal portal, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(portal, yaw, partialTicks, pose, buffers, light);
        // The original's arithmetic (LesserPortalRenderer.renderPortal): half-width grows linearly over 50 ticks, no ease and no slit.
        float age = portal.tickCount + partialTicks;
        float e = Math.min(OPEN_TICKS, age);
        float halfH = HALF_HEIGHT;
        if (portal.hurtTime > 0) {                    // a hit wobbles it twice
            float d = Mth.sin(portal.hurtTime * 72.0F * Mth.DEG_TO_RAD);
            halfH -= d / 4.0F;
            e += 6.0F * d;
        }
        if (portal.pulse > 0) {                       // an arrival: shrinks first, then swells (a full sine over its 10 ticks)
            float d = Mth.sin(portal.pulse * 36.0F * Mth.DEG_TO_RAD);
            halfH += d / 4.0F;
            e += 12.0F * d;
        }
        float halfW = e / OPEN_TICKS * FULL_HALF_WIDTH;
        float m = (1.0F - portal.getHealth() / portal.getMaxHealth()) / 3.0F;   // a damaged portal flickers and shrinks
        float bob = Mth.sin(portal.tickCount / (5.0F - 12.0F * m)) * m + m;
        float bob2 = Mth.sin(portal.tickCount / (6.0F - 15.0F * m)) * m + m;
        float alpha = 1.0F - bob;
        halfH -= bob / 4.0F;
        halfW = Math.max(0.0F, halfW - bob2 / 3.0F);
        int frame = FRAMES - 1 - Math.floorMod((int) age, FRAMES);   // the frames run backwards
        float u0 = frame / (float) FRAMES;
        OccludingEffectRenderer.enqueuePortal(portal.getPosition(partialTicks).add(0.0, portal.getBbHeight() / 2.0F, 0.0),
                halfW, halfH, u0, u0 + 1.0F / FRAMES, FastColor.ARGB32.colorFromFloat(alpha, 1.0F, 1.0F, 1.0F), LIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(LesserCrimsonPortal portal) {
        return TEXTURE;
    }
}
