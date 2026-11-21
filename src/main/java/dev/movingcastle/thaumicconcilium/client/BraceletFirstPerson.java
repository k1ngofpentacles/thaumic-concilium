package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.item.CastingBracelet;
import dev.movingcastle.thaumicconcilium.spell.BraceletCasting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * First person: bracelets show on your own wrist instead of as an item (or, since a bracelet is a Thaumaturge wand
 * underneath, as Thaumaturge's wand). A held bracelet draws your bare arm with the band on it, the main arm for the
 * main hand and the off arm for the shield slot.
 * <p>
 * While you cast, the casting arm comes up from the lower corner of the view, forearm and hand only (the plasmid hand of
 * System Shock and Bioshock 2), band and glowing gem showing. Casting from a worn bracelet raises the off arm even when
 * the off hand is empty (the game wouldn't draw that arm at all); anything held in the off hand, a shield say, stays in
 * view, swung aside.
 * <p>
 * Runs before Thaumaturge's wand hand renderer, which would otherwise draw a held bracelet as a wand; cancelling the
 * event stops it.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID, value = Dist.CLIENT)
public final class BraceletFirstPerson {
    private BraceletFirstPerson() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isInvisible()) {
            return;
        }
        HumanoidArm mainArm = player.getMainArm();
        HumanoidArm offArm = mainArm.getOpposite();
        ItemStack stack = event.getItemStack();
        float raise = BraceletCastState.raise(player, event.getPartialTick());
        HumanoidArm casting = BraceletCastState.castingArm(player);
        float mainRaise = casting == mainArm ? raise : 0.0F;
        float offRaise = casting == offArm ? raise : 0.0F;
        ItemStack worn = BraceletCasting.worn(player);
        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        int light = event.getPackedLight();
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            if (stack.getItem() instanceof CastingBracelet) {
                event.setCanceled(true);
                drawArm(pose, buffers, light, player, mainArm, stack,
                        mainRaise > 0.0F ? 0.0F : event.getEquipProgress(), mainRaise > 0.0F ? 0.0F : event.getSwingProgress(), mainRaise,
                        ItemStack.EMPTY);
            }
            // With nothing in the off hand the game draws no off arm, so the raised casting arm is drawn from here.
            if (offRaise > 0.0F && player.getOffhandItem().isEmpty() && !worn.isEmpty()) {
                drawArm(pose, buffers, light, player, offArm, worn, 0.0F, 0.0F, offRaise, ItemStack.EMPTY);
            }
        } else {
            boolean held = stack.getItem() instanceof CastingBracelet;
            if (held || (offRaise > 0.0F && !worn.isEmpty())) {
                event.setCanceled(true);
                // A shield stays strapped along the raised forearm; anything else is swung out of the way.
                boolean shield = !held && stack.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.SHIELD_BLOCK);
                if (!held && !shield && offRaise > 0.0F) {
                    drawAside(pose, buffers, light, player, offArm, stack, offRaise);
                }
                ItemStack shown = worn.isEmpty() ? stack : worn;
                drawArm(pose, buffers, light, player, offArm, shown,
                        offRaise > 0.0F ? 0.0F : event.getEquipProgress(), offRaise > 0.0F ? 0.0F : event.getSwingProgress(), offRaise,
                        shield ? stack : ItemStack.EMPTY);
            }
        }
    }

    private static float ease(float t) {
        return 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
    }

    /** The off-hand item in its usual place, swung out and down, turned away. */
    private static void drawAside(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                  HumanoidArm arm, ItemStack item, float raise) {
        float f = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float e = ease(raise);
        pose.pushPose();
        pose.translate(f * 0.56F, -0.52F, -0.72F);   // the game's resting off-hand item position
        pose.translate(f * 0.14F * e, -0.10F * e, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(f * -28.0F * e));
        pose.mulPose(Axis.ZP.rotationDegrees(f * -10.0F * e));
        Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(player, item,
                arm == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                arm == HumanoidArm.LEFT, pose, buffers, light);
        pose.popPose();
    }

    /**
     * The game's first-person bare arm ({@code ItemInHandRenderer.renderPlayerArm}), raised by {@code raise} (0 to 1)
     * into the casting pose, then the band on its wrist.
     */
    private static void drawArm(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                HumanoidArm arm, ItemStack bracelet, float equip, float swing, float raise, ItemStack strapped) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)
                || !(bracelet.getItem() instanceof CastingBracelet item)) {
            return;
        }
        boolean right = arm == HumanoidArm.RIGHT;
        float f = right ? 1.0F : -1.0F;
        pose.pushPose();
        float root = Mth.sqrt(swing);
        float sx = -0.3F * Mth.sin(root * Mth.PI);
        float sy = 0.4F * Mth.sin(root * Mth.TWO_PI);
        float sz = -0.4F * Mth.sin(swing * Mth.PI);
        pose.translate(f * (sx + 0.64000005F), sy - 0.6F + equip * -0.6F, sz - 0.71999997F);
        if (raise > 0.0F) {
            // Up and in, pointing ahead towards the middle of the view, the back of the wrist (and the gem) towards
            // you; held back towards the camera so only the forearm and hand show, as a plasmid hand does.
            float e = ease(raise);
            float[] c = castPose();
            pose.translate(-f * c[0] * e, c[1] * e, c[2] * e);
            pose.mulPose(Axis.ZP.rotationDegrees(f * c[3] * e));
            pose.mulPose(Axis.XP.rotationDegrees(c[4] * e));
            pose.mulPose(Axis.YP.rotationDegrees(f * c[5] * e));
        }
        pose.mulPose(Axis.YP.rotationDegrees(f * 45.0F));
        float s1 = Mth.sin(swing * swing * Mth.PI);
        float s2 = Mth.sin(root * Mth.PI);
        pose.mulPose(Axis.YP.rotationDegrees(f * s2 * 70.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(f * s1 * -20.0F));
        pose.translate(f * -1.0F, 3.6F, 3.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(f * 120.0F));
        pose.mulPose(Axis.XP.rotationDegrees(200.0F));
        pose.mulPose(Axis.YP.rotationDegrees(f * -135.0F));
        pose.translate(f * 5.6F, 0.0F, 0.0F);
        if (right) {
            renderer.renderRightHand(pose, buffers, light, player);
        } else {
            renderer.renderLeftHand(pose, buffers, light, player);
        }
        pose.pushPose();
        (right ? renderer.getModel().rightArm : renderer.getModel().leftArm).translateAndRotate(pose);
        BraceletBandRenderer.draw(pose, buffers, item.kind(), arm, player.getSkin().model() == PlayerSkin.Model.SLIM, light,
                BraceletBandRenderer.gemColor(bracelet));
        if (!strapped.isEmpty()) {
            drawStrapped(pose, buffers, light, player, arm, strapped);
        }
        pose.popPose();
        pose.popPose();
    }

    /**
     * A shield strapped along the outside of the forearm (arm-model space: pixels / 16, Y down the arm to the hand at
     * 10, X out from the body): its face turned outwards, parallel to the arm, so the raised casting arm never passes
     * through it.
     */
    private static void drawStrapped(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                     HumanoidArm arm, ItemStack shield) {
        float out = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        pose.pushPose();
        float[] st = strap();
        pose.translate(out * st[0] / 16.0F, st[1] / 16.0F, st[3] / 16.0F);
        pose.mulPose(Axis.YP.rotationDegrees(out * 90.0F));   // face outwards
        pose.mulPose(Axis.ZP.rotationDegrees(180.0F));        // the shield's top towards the hand
        pose.scale(st[2], st[2], st[2]);
        Minecraft.getInstance().getItemRenderer().renderStatic(player, shield, ItemDisplayContext.NONE, arm == HumanoidArm.LEFT,
                pose, buffers, player.level(), light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    /** The cast pose: inward, up, towards the camera, then roll, pitch, yaw (degrees). Tuned in the render harness. */
    // "Moses stretching his hand towards the sea": reaching ahead and a little up, the hand towards the middle; chosen
    // by the user from the harness's candidates (pose_c_reach_up, 2026-11-21).
    private static final float[] CAST_POSE = {0.20F, 0.18F, -0.35F, -12.0F, -25.0F, 10.0F};

    /**
     * The cast pose, or a development override from the system property {@code thaumicconcilium.castpose}
     * ("x,y,z,roll,pitch,yaw"), which the render harness sets per scene to compare candidate poses in one run.
     */
    private static float[] castPose() {
        String dev = System.getProperty("thaumicconcilium.castpose");
        if (dev != null) {
            try {
                String[] parts = dev.split(",");
                float[] out = new float[6];
                for (int i = 0; i < 6; i++) {
                    out[i] = Float.parseFloat(parts[i].trim());
                }
                return out;
            } catch (RuntimeException ignored) {
                // fall back to the tuned pose
            }
        }
        return CAST_POSE;
    }

    /** Strapped shield: out from the arm, down the arm, scale, along z (pixels); override thaumicconcilium.strap. */
    private static float[] strap() {
        String dev = System.getProperty("thaumicconcilium.strap");
        if (dev != null) {
            try {
                String[] p = dev.split(",");
                return new float[]{Float.parseFloat(p[0]), Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])};
            } catch (RuntimeException ignored) {
                // fall back
            }
        }
        return new float[]{STRAP_OUT, STRAP_DOWN, STRAP_SCALE, STRAP_ALONG};
    }

    /** Strapped shield placement (enarmes: the forearm along the shield's back, the hand at its grip), picked in the render harness. */
    private static final float STRAP_OUT = 2.6F;
    private static final float STRAP_DOWN = -6.0F;
    private static final float STRAP_SCALE = 0.55F;
    private static final float STRAP_ALONG = -4.0F;
}
