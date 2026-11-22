package dev.movingcastle.thaumicconcilium.client;

import com.leclowndu93150.thaumaturge.client.effect.LateWorldRenderQueue;
import com.leclowndu93150.thaumaturge.client.render.TTRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.VesselClot;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * The thrown clot (phase 5c cycle 3): wind-up (it grows 0.3 to 1.0 with ease-out, a contracting additive ring, and a thin aim line to its
 * target's chest), flight (a billboard of half-size 0.4) and batted (pale cyan, half-size 0.5). The particles are {@link VesselFx}'s.
 */
public class VesselClotRenderer extends EntityRenderer<VesselClot> {
    private static final ResourceLocation CLOT = ThaumicConcilium.id("textures/misc/vessel_clot.png");
    private static final ResourceLocation HOT = ThaumicConcilium.id("textures/misc/vessel_clot_hot.png");
    private static final ResourceLocation RING = ThaumicConcilium.id("textures/misc/vessel_ring.png");
    private static final int MAGENTA = 0xFF4FD8;
    private static final int CYAN = 0xB0F0FF;

    public VesselClotRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(VesselClot entity, Frustum frustum, double x, double y, double z) {
        return entity.distanceToSqr(x, y, z) < 128.0 * 128.0;
    }

    @Override
    public ResourceLocation getTextureLocation(VesselClot entity) {
        return CLOT;
    }

    @Override
    public void render(VesselClot e, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        Vec3 origin = e.getPosition(pt);
        float now = e.level().getGameTime() + pt;
        LateWorldRenderQueue.enqueue(origin, (lp, lb) -> this.late(e, lp, lb, origin, now));
    }

    private void late(VesselClot e, PoseStack pose, MultiBufferSource b, Vec3 origin, float now) {
        float launch = e.launchTick();
        float windup = Math.max(1, TCConfig.CLOT_WINDUP.get());
        boolean winding = !e.batted() && now < launch;
        float a = winding ? Mth.clamp(1.0F - (launch - now) / windup, 0.0F, 1.0F) : 1.0F;
        float ease = 1.0F - (1.0F - a) * (1.0F - a);
        float half = e.batted() ? 0.5F : 0.4F * (winding ? 0.3F + 0.7F * ease : 1.0F);
        RenderType body = TTRenderTypes.fxTranslucent(e.batted() ? HOT : CLOT);
        VesselDraw.billboard(b, pose, body, half, 0.0F, e.batted() ? CYAN : 0xFFFFFF, 1.0F, 1.0F);
        VesselDraw.flush(b, body);
        if (winding) {
            RenderType ring = TTRenderTypes.fxAdditiveAlphaTest(RING);
            VesselDraw.billboard(b, pose, ring, Mth.lerp(a, 1.5F, 0.4F), 0.0F, MAGENTA, Mth.lerp(a, 0.4F, 1.0F), 1.0F);
            VesselDraw.flush(b, ring);
            Player target = e.syncedTarget().map(id -> e.level().getPlayerByUUID(id)).orElse(null);
            if (target != null) {
                RenderType line = TTRenderTypes.fxTranslucent(VesselRenderer.RIBBON_BODY);
                Vec3 chest = target.getPosition(0.0F).add(0.0, target.getBbHeight() * 0.6, 0.0).subtract(origin);
                VesselDraw.ribbon(b, pose, line, Vec3.ZERO, chest, 0.06F, 0.06F, MAGENTA, 1.0F, now, 0.05F * now, 0.0F, 178);
                VesselDraw.flush(b, line);
            }
        }
    }
}
