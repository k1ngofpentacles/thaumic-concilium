package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.item.RiftGem;
import dev.movingcastle.thaumicconcilium.item.TerraCastGem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * The Gem of Rifts (a smoked-glass frustum) and the Terra-cast Gem (a green pyramid): the original's two OBJ meshes, in
 * every view but first person's hover pose ({@link GemFirstPerson}, which calls {@link #drawHeld}). Spec:
 * docs/design/6c-gem-held-render.md. The glass is alpha 84, drawn back-face culled as the original did; the meshes are lit
 * by flat face normals.
 */
public final class GemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation BASE_OBJ = ThaumicConcilium.id("models/item/gem_base.obj");
    private static final ResourceLocation TOP_OBJ = ThaumicConcilium.id("models/item/gem_top.obj");
    private static final ResourceLocation BASE_TEX = ThaumicConcilium.id("textures/entity/gem/gem_base.png");
    private static final ResourceLocation TOP_TEX = ThaumicConcilium.id("textures/entity/gem/gem_top.png");

    private static GemRenderer instance;

    private GemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static GemRenderer get() {
        if (instance == null) {
            instance = new GemRenderer();
        }
        return instance;
    }

    /** Client hook that gives the four gem items this renderer. */
    public static final class Extensions implements IClientItemExtensions {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return GemRenderer.get();
        }
    }

    private static boolean isRift(ItemStack stack) {
        return RiftGem.isGem(stack);
    }

    private static boolean isActive(ItemStack stack) {
        return isRift(stack) ? RiftGem.active(stack) : TerraCastGem.active(stack);
    }

    /** True while the local player holds the use key on one of the two active gems: the fast spin. */
    static boolean usingGem() {
        var player = Minecraft.getInstance().player;
        if (player == null || !player.isUsingItem()) {
            return false;
        }
        ItemStack use = player.getUseItem();
        return RiftGem.active(use) || TerraCastGem.active(use);
    }

    /** Degrees of spin: a degree a tick, nine while the player uses a gem (first and third person only). */
    static float spin(float partial, boolean allowFast) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera == null) {
            return 0.0F;
        }
        return (camera.tickCount + partial) * (allowFast && usingGem() ? 9.0F : 1.0F);
    }

    private static float partial() {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
    }

    private static void mesh(boolean base, PoseStack pose, MultiBufferSource buffers, int light) {
        ObjMesh.getFlat(base ? BASE_OBJ : TOP_OBJ).render(pose,
                buffers.getBuffer(RenderType.entityTranslucentCull(base ? BASE_TEX : TOP_TEX)), 0xFFFFFFFF, light);
    }

    /** The small pyramid a rift gem that holds casts wears as its tip, at the given placement. */
    private static void cap(PoseStack pose, MultiBufferSource buffers, int light, float y, float scale) {
        pose.pushPose();
        pose.translate(0.0F, y, 0.0F);
        pose.scale(scale, scale, scale);
        mesh(false, pose, buffers, light);
        pose.popPose();
    }

    private static void castBlock(ItemStack block, PoseStack pose, MultiBufferSource buffers, int light) {
        if (block.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        pose.pushPose();
        pose.translate(0.0F, -0.09F, 0.0F);
        pose.scale(0.275F, 0.275F, 0.275F);
        mc.getItemRenderer().renderStatic(mc.player, block, ItemDisplayContext.NONE, false, pose, buffers, mc.level, light,
                OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    /**
     * First person, in the gem frame (the caller has placed the pivot and scaled by 0.4). The cast block is a fixed-buffer
     * draw that vanilla flushes after our translucent glass, which writes depth: flush it first or the glass hides it.
     */
    public static void drawHeld(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, float spinDegrees) {
        boolean rift = isRift(stack);
        pose.pushPose();
        if (isActive(stack)) {
            pose.mulPose(Axis.YP.rotationDegrees(spinDegrees));
        }
        ItemStack block = rift ? RiftGem.currentCast(stack) : TerraCastGem.castStack(stack);
        boolean cast = !block.isEmpty();
        if (rift && RiftGem.castCount(stack) > 0 && cast) {
            cap(pose, buffers, light, 0.373F, 0.1075F);
        }
        if (cast) {
            castBlock(block, pose, buffers, light);
        }
        if (cast && buffers instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch();
        }
        pose.pushPose();
        if (rift) {
            pose.scale(0.4F, 0.4F, 0.4F);
            mesh(true, pose, buffers, light);
        } else if (cast) {
            pose.translate(0.0F, 0.02F, 0.0F);
            pose.scale(0.396F, 0.396F, 0.396F);
            mesh(false, pose, buffers, light);
        } else {
            pose.scale(0.4F, 0.4F, 0.4F);
            mesh(false, pose, buffers, light);
        }
        pose.popPose();
        pose.popPose();
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        boolean rift = isRift(stack);
        boolean active = isActive(stack);
        boolean capped = rift && RiftGem.castCount(stack) > 0;
        pose.pushPose();
        // The item renderer has centred the origin on the model's corner: undo it.
        pose.translate(0.5F, 0.5F, 0.5F);
        switch (context) {
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                float m = context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND ? -1.0F : 1.0F;
                // Undo the 1.21 hand prefix, then replay the 1.7.10 chain (RenderPlayer.renderEquippedItems + renderer).
                pose.translate(m * -0.0625F, -0.125F, 0.625F);
                pose.mulPose(Axis.YP.rotationDegrees(-180.0F));
                pose.mulPose(Axis.XP.rotationDegrees(90.0F));
                pose.translate(m * -0.0625F, 0.4375F, 0.0625F);
                pose.translate(m * 0.25F, 0.1875F, -0.1875F);
                pose.scale(0.375F, 0.375F, 0.375F);
                pose.mulPose(Axis.ZP.rotationDegrees(m * 60.0F));
                pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
                pose.mulPose(Axis.ZP.rotationDegrees(m * 20.0F));
                pose.translate(m * -0.5F, -0.5F, -0.5F);
                pose.scale(0.5F, 0.5F, 0.5F);
                pose.translate(0.0F, 2.5F, 1.4F);
                pose.scale(0.8F, 0.8F, 0.8F);
                pose.mulPose(Axis.ZP.rotationDegrees(m * -30.0F));
                if (capped) {
                    cap(pose, buffers, light, 0.9F, 0.25F);
                }
                if (active) {
                    pose.mulPose(Axis.YP.rotationDegrees(spin(partial(), true)));
                }
                mesh(rift, pose, buffers, light);
            }
            case GUI -> {
                pose.scale(0.5F, 0.5F, 0.5F);
                if (active) {
                    pose.mulPose(Axis.YP.rotationDegrees(spin(partial(), false)));
                }
                if (capped) {
                    cap(pose, buffers, light, 0.9F, 0.25F);
                }
                mesh(rift, pose, buffers, light);
            }
            default -> {
                if (context == ItemDisplayContext.FIXED) {
                    pose.translate(0.0F, -0.16F, 0.0F);
                }
                pose.scale(0.5F, 0.5F, 0.5F);
                if (capped) {
                    cap(pose, buffers, light, 0.9F, 0.25F);
                }
                mesh(rift, pose, buffers, light);
            }
        }
        pose.popPose();
    }
}
