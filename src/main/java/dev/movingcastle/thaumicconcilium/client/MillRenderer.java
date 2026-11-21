package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.item.ShardMill;
import dev.movingcastle.thaumicconcilium.registry.TCItems;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Draws a Shard Mill from two side-loaded OBJ models, the body (body and cap) and the crank handle, as the original's
 * renderer did. While its holder grinds, the mill is turned so its bottom faces where they look, like a pepper
 * grinder held out in front, and the handle spins about its shaft.
 */
public final class MillRenderer extends BlockEntityWithoutLevelRenderer {
    /** The crank shaft: the model's X axis through this point (scaled from the original renderer's pivot). */
    public static final float CRANK_PIVOT_Y = 0.6125F;
    public static final float CRANK_PIVOT_Z = 0.5F;
    /** Middle of the crank's grip bar (the original's handle grip, x -2.6..-1.4, y 0.81..0.96, scaled). */
    public static final float GRIP_X = 0.2615F;
    public static final float GRIP_Y = 0.4066F;
    public static final float GRIP_Z = 0.5F;
    /** Distance of the grip from the crank shaft, in model units. */
    public static final float CRANK_RADIUS = CRANK_PIVOT_Y - GRIP_Y;
    /** Degrees the crank turns per tick of grinding. */
    public static final float CRANK_SPEED = 18.0F;
    /** Where the hand sits in the arm model (pivot 5 px out, cube 12 px long): the middle of the fist. */
    private static final float HAND_X = 6.0F / 16.0F;
    private static final float HAND_Y = 10.5F / 16.0F;
    /** The arm is drawn inside the item's (shrunk) space; this brings it back to first-person size. */
    private static final float ARM_SCALE = 1.3F;

    public static final List<ModelResourceLocation> MODELS = List.of(
            part("greatwood", "body"), part("greatwood", "handle"), part("silverwood", "body"), part("silverwood", "handle"));

    private static MillRenderer instance;

    private MillRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static MillRenderer get() {
        if (instance == null) {
            instance = new MillRenderer();
        }
        return instance;
    }

    private static ModelResourceLocation part(String wood, String part) {
        return ModelResourceLocation.standalone(ThaumicConcilium.id("item/mill/" + wood + "_" + part));
    }

    /** True while {@code entity} is using {@code stack}, loading (lid open) or grinding (lid closed). */
    public static boolean isUsing(@Nullable LivingEntity entity, ItemStack stack) {
        return entity != null && entity.isUsingItem() && entity.getUseItem() == stack;
    }

    /** True while {@code entity} is grinding with {@code stack}: using it with the lid closed. */
    public static boolean isGrinding(@Nullable LivingEntity entity, ItemStack stack) {
        return entity != null && entity.isUsingItem() && entity.getUseItem() == stack && !ShardMill.state(stack).open();
    }

    /** The crank's angle in degrees for a grinding entity. */
    public static float crankDegrees(LivingEntity entity, float partialTick) {
        return (entity.getTicksUsingItem() + partialTick) * CRANK_SPEED;
    }

    public static float partialTick() {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
                             int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        String wood = stack.is(TCItems.SILVERWOOD_SHARD_MILL.get()) ? "silverwood" : "greatwood";
        BakedModel body = mc.getModelManager().getModel(part(wood, "body"));
        BakedModel handle = mc.getModelManager().getModel(part(wood, "handle"));
        VertexConsumer consumer = buffers.getBuffer(Sheets.cutoutBlockSheet());

        LivingEntity holder = context.firstPerson() ? mc.player : MillRenderContext.current();
        boolean inHand = context != ItemDisplayContext.GUI && context != ItemDisplayContext.GROUND
                && context != ItemDisplayContext.FIXED;
        boolean thirdPerson = context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        boolean grinding = inHand && isGrinding(holder, stack);

        pose.pushPose();
        if (thirdPerson && isUsing(holder, stack)) {
            // Third person, grinding or loading: MillArmPose moves the mill to the inside of the hand and turns it,
            // the same way it computes where the free hand goes.
            MillArmPose.transformMill(pose, holder, stack, context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, partialTick());
        } else if (grinding) {
            // First person: bottom (model -Y) forward, away from the holder.
            pose.translate(0.5F, 0.5F, 0.5F);
            pose.mulPose(Axis.XP.rotationDegrees(90.0F));
            pose.translate(-0.5F, -0.5F, -0.5F);
        }
        mc.getItemRenderer().renderModelLists(body, stack, light, overlay, pose, consumer);
        pose.pushPose();
        if (grinding) {
            pose.translate(0.0F, CRANK_PIVOT_Y, CRANK_PIVOT_Z);
            pose.mulPose(Axis.XP.rotationDegrees(crankDegrees(holder, partialTick())));
            pose.translate(0.0F, -CRANK_PIVOT_Y, -CRANK_PIVOT_Z);
        }
        mc.getItemRenderer().renderModelLists(handle, stack, light, overlay, pose, consumer);
        pose.popPose();
        pose.popPose();
        if (grinding && context.firstPerson() && holder instanceof AbstractClientPlayer player && !player.isInvisible()) {
            this.renderCrankingArm(pose, buffers, light, player);
        }
    }

    /**
     * First person: the free arm, its hand on the crank's grip and following it round, the forearm pointing back
     * towards the shoulder at the bottom corner of the view.
     */
    private void renderCrankingArm(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player) {
        HumanoidArm arm = player.getUsedItemHand() == InteractionHand.MAIN_HAND ? player.getMainArm().getOpposite() : player.getMainArm();
        float towardsMill = arm == HumanoidArm.LEFT ? 1.0F : -1.0F;
        float angle = crankDegrees(player, partialTick());
        pose.pushPose();
        // Follow the mill (bottom forward) and the crank to the grip...
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.translate(-0.5F, -0.5F, -0.5F);
        pose.translate(0.0F, CRANK_PIVOT_Y, CRANK_PIVOT_Z);
        pose.mulPose(Axis.XP.rotationDegrees(angle));
        pose.translate(GRIP_X, GRIP_Y - CRANK_PIVOT_Y, GRIP_Z - CRANK_PIVOT_Z);
        // ...then undo both turns, so the arm keeps its own orientation in the view while the hand travels round.
        pose.mulPose(Axis.XP.rotationDegrees(-angle));
        pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
        // Arm model: shoulder at the origin, the hand down its +Y axis. Aim shoulder-to-hand from the lower outer corner
        // of the view in towards the grip, then put the middle of the hand on the grip.
        Vector3f reach = new Vector3f(0.45F * towardsMill, 0.55F, -1.0F).normalize();
        pose.mulPose(new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, reach.x, reach.y, reach.z));
        pose.scale(ARM_SCALE, ARM_SCALE, ARM_SCALE);
        pose.translate(-HAND_X * towardsMill, -HAND_Y, 0.0F);
        PlayerRenderer renderer = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (arm == HumanoidArm.RIGHT) {
            renderer.renderRightHand(pose, buffers, light, player);
        } else {
            renderer.renderLeftHand(pose, buffers, light, player);
        }
        pose.popPose();
    }
}
