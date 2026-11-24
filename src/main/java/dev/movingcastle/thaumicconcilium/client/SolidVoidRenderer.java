package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.movingcastle.thaumicconcilium.block.SolidVoidBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** The Solid Void's cube: the end portal starfield on every face no neighbour covers (original: SolidVoidTileRenderer). */
public class SolidVoidRenderer implements BlockEntityRenderer<SolidVoidBlockEntity> {
    public SolidVoidRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SolidVoidBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() != null) {
            VoidBody.block(be.getBlockState(), be.getLevel(), be.getBlockPos(), pose, buffers);
        }
    }
}
