package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.movingcastle.thaumicconcilium.registry.TCItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * The Protobody (a small upright humanoid in the end portal starfield, original ProtobodyRenderer) and the Solid Void block item
 * (the same cube as the block). Both models are {@code builtin/entity}; their display transforms are in the generated model JSON.
 */
public final class VoidItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static VoidItemRenderer instance;
    private final VoidBody body;

    private VoidItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.body = new VoidBody(Minecraft.getInstance().getEntityModels(), false);
    }

    public static VoidItemRenderer get() {
        if (instance == null) {
            instance = new VoidItemRenderer();
        }
        return instance;
    }

    public static final class Extensions implements IClientItemExtensions {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return VoidItemRenderer.get();
        }
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F); // the item renderer centres the origin on the model's corner: undo it
        if (stack.is(TCItems.SOLID_VOID.get())) {
            pose.translate(-0.5F, -0.5F, -0.5F);
            VoidBody.cube(pose, buffers);
        } else {
            pose.scale(0.5F, 0.5F, 0.5F); // 2 model units of humanoid -> one block tall
            body.drawFigure(pose, buffers, light, overlay);
        }
        pose.popPose();
    }
}
