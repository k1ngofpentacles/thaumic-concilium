package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Two render types on vanilla programs (no core shader of ours, so Iris maps both): the Peeler's additive halo shells (the end
 * portal program, blended additively, no depth write) and the chained hand's shackles (the unlit eyes program with the original's
 * {@code glBlendFunc(SRC_COLOR, ONE_MINUS_SRC_ALPHA)}). Spec: docs/design/5b-peeler-shackles-render.md.
 */
public final class TCRenderTypes extends RenderType {
    private TCRenderTypes() {
        super("tc_unused", DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS, 256, false, false, () -> {
        }, () -> {
        });
    }

    private static final RenderStateShard.TransparencyStateShard SRC_COLOR_BLEND = new RenderStateShard.TransparencyStateShard(
            "tc_src_color", () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                        GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            }, () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            });

    /** The end portal pattern added on top of what is behind: the original's three halo shells. Draw before the core. */
    public static final ResourceLocation PEELER_HALO_TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumicconcilium", "textures/misc/peeler_halo.png");

    /** D4-b: the vanilla position-tex-colour program, our own pale white-blue noise texture, blended (ONE, ONE). */
    public static final RenderType PEELER_HALO = RenderType.create("tc_peeler_halo", DefaultVertexFormat.POSITION_TEX_COLOR,
            VertexFormat.Mode.QUADS, 1536, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader))
                    .setTextureState(new RenderStateShard.TextureStateShard(PEELER_HALO_TEXTURE, false, false))
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private static final Function<ResourceLocation, RenderType> ETHEREAL_FN = Util.memoize(texture -> RenderType.create(
            "tc_ethereal", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_EYES_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                    .setTransparencyState(SRC_COLOR_BLEND)
                    .createCompositeState(false)));

    /** The chained hand's shackles and chain, textured with the per-tick baked dynamic texture. */
    public static RenderType ethereal(ResourceLocation texture) {
        return ETHEREAL_FN.apply(texture);
    }
}
