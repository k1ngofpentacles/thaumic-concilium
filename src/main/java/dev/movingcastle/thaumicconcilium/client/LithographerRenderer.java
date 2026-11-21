package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.block.LithographerBlock;
import dev.movingcastle.thaumicconcilium.block.LithographerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

/**
 * The Lithographer (original: {@code LithographerTileRenderer}, WTFPL models): a base with a short first sector, and
 * three more sectors that slide in and out while it works. The animation reads the block entity's {@code state} and
 * {@code deploy} exactly as the original did, with the partial tick folded in so it runs smoothly.
 */
public class LithographerRenderer implements BlockEntityRenderer<LithographerBlockEntity> {
    private static final ResourceLocation TEXTURE = ThaumicConcilium.id("textures/block/lithographer.png");

    public LithographerRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static ObjMesh mesh(String name) {
        return ObjMesh.get(ThaumicConcilium.id("models/block/lithographer/" + name + ".obj"));
    }

    @Override
    public void render(LithographerBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        pose.pushPose();
        pose.translate(0.5, 0.0, 0.5);
        if (be.getBlockState().getValue(LithographerBlock.AXIS) == Direction.Axis.Z) {
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        mesh("base").render(pose, out, 0xFFFFFFFF, light);
        mesh("sector1").render(pose, out, 0xFFFFFFFF, light);
        int deploy = be.deploy();
        // The countdown in ticks, with the fraction of the current tick.
        float d = deploy > 0 ? deploy - partialTick : deploy < 0 ? deploy + partialTick : 0.0F;
        switch (be.state()) {
            case 1 -> {
                if (deploy != 0) {
                    sectors(pose, out, d / 150.0F, light);
                }
            }
            case 2 -> sectors(pose, out, 0.0F, light);
            case 3 -> {
                if (deploy != 0) {
                    sectors(pose, out, (20.0F + d) / 150.0F, light);
                }
            }
            default -> {
            }
        }
        pose.popPose();
    }

    /** Sectors 2 to 4 shifted up by 1, 3 and 5 times {@code lift} (original: deploy / 150 times 1, 3, 5). */
    private static void sectors(PoseStack pose, VertexConsumer out, float lift, int light) {
        int[] factors = {1, 3, 5};
        for (int i = 0; i < 3; i++) {
            pose.pushPose();
            pose.translate(0.0F, lift * factors[i], 0.0F);
            mesh("sector" + (i + 2)).render(pose, out, 0xFFFFFFFF, light);
            pose.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(LithographerBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(1.5, 1.5, 1.5);
    }
}
