package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.item.AstralMonitor;
import dev.movingcastle.thaumicconcilium.item.ThaumaturgeDrum;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import dev.movingcastle.thaumicconcilium.item.CastingBracelet;
import net.neoforged.bus.api.EventPriority;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * First person, two-handed poses, as the original drew them (its renderers drew the player's arms themselves):
 * <ul>
 *   <li>the Astral Monitor, in the main hand with the off hand empty, is held up in both hands, flat to the camera
 *   (vanilla's two-handed map is the model for the arms);</li>
 *   <li>Thaumaturge's Drum and the Warped Drum, in the main hand with the off hand empty, sit on the left forearm
 *   with the skin up, and while the item is used the right arm beats down on it;</li>
 *   <li>the same picture for a drum in the OFF hand with the main hand empty or holding a casting bracelet (the drum
 *   plays from either hand; mirrored for left-handed players; the bracelet's band shows on the striking wrist). Held but
 *   not played, the off arm holds the drum and the main hand is drawn as usual.</li>
 * </ul>
 * Anything else keeps vanilla's one-handed display transforms. Nothing runs unless one of these items is held.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID, value = Dist.CLIENT)
public final class FirstPersonHands {
    static float ARM_SCALE = 1.15F;
    static float MONITOR_Y = -0.053F;
    static float MONITOR_SCALE = 0.304F;
    static float DRUM_X = -0.167F;
    static float DRUM_Y = -0.264F;
    static float DRUM_Z = 0.0F;
    static float DRUM_SCALE = 0.45F;
    static float DRUM_TILT = -2.0F;

    static float LEFT_ARM_X = -0.19F;
    static float LEFT_ARM_Y = 0.05F;
    /** Monitor grip: the forearms ride up and in so their ends overlap the frame's lower edges, behind its front face. */
    static float GRIP_UP = 0.11F;
    static float GRIP_IN = 0.03F;
    static float GRIP_BACK = 0.03F;
    /** Right arm while the drum is beaten (offsets from its resting place): on the skin, and hovering above it, in view. */
    static float STRIKE_X = -0.72F;
    static float STRIKE_Y = -0.55F;
    static float STRIKE_ROLL = 66.0F;
    static float HOVER_X = -0.27F;
    static float HOVER_Y = 0.03F;
    static float HOVER_ROLL = 52.0F;
    static float SLAP_DX = 0.1F;
    static float SLAP_DY = -0.07F;
    static float SLAP_ROLL = 6.0F;
    static float WRIST_SNAP = 8.0F;
    static float SQUEEZE_DIP = 0.015F;
    static float SQUEEZE_ROLL = 2.5F;
    static float IDLE_X = 0.0F;
    static float IDLE_Y = 0.0F;
    static float IDLE_ROLL = 0.0F;

    private static final int FULL_BRIGHT = 0xF000F0;
    /** The drum's beat length in ticks, and the tick of the beat on which the hand first lands on the skin (e reaches 1). */
    private static final int DRUM_BEAT_TICKS = 8;
    private static final int DRUM_CONTACT_TICK = 2;

    /** True on the use tick on which the striking hand lands on the skin (read-only; for test harnesses). */
    public static boolean drumContactAt(int useTicks) {
        return useTicks >= 0 && useTicks % DRUM_BEAT_TICKS == DRUM_CONTACT_TICK;
    }

    private FirstPersonHands() {
    }

    /** The off-hand drum stance: a drum in the off hand, the main hand empty or holding a casting bracelet. */
    private static boolean offHandDrum(LocalPlayer player) {
        return player.getOffhandItem().getItem() instanceof ThaumaturgeDrum
                && (player.getMainHandItem().isEmpty() || player.getMainHandItem().getItem() instanceof CastingBracelet);
    }

    // Highest, so the bracelet's own hand renderer (which would draw the main hand again) never sees these events.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || ShacklesFirstPerson.active()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        boolean stance = false;
        if (event.getHand() == InteractionHand.OFF_HAND) {
            stance = stack.getItem() instanceof ThaumaturgeDrum && offHandDrum(player);
            if (!stance) {
                return;
            }
        } else if (offHandDrum(player) && player.isUsingItem() && player.getUsedItemHand() == InteractionHand.OFF_HAND) {
            // The off-hand drum is being played: the off-hand event draws both hands, so draw nothing here.
            event.setCanceled(true);
            return;
        }
        boolean monitor = !stance && stack.getItem() instanceof AstralMonitor;
        boolean drum = stance || stack.getItem() instanceof ThaumaturgeDrum;
        if (event.getHand() == InteractionHand.MAIN_HAND && ((!monitor && !drum) || !player.getOffhandItem().isEmpty())) {
            return;
        }
        event.setCanceled(true);
        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        int light = event.getPackedLight();
        float swing = event.getSwingProgress();
        float equip = event.getEquipProgress();
        pose.pushPose();
        // Vanilla's two-handed map frame: bob with the swing, lower with the equip animation, then the plane the hands hold.
        pose.translate(0.0F, Mth.sin(swing * (float) Math.PI) * 0.1F, -0.4F * Mth.sin(Mth.sqrt(swing) * (float) Math.PI));
        pose.translate(0.0F, 0.04F - equip * 1.2F, -0.72F);
        if (monitor) {
            drawMonitor(player, stack, pose, buffers, FULL_BRIGHT, event);
        } else if (stance) {
            // The off arm grips the drum, the main arm strikes (picture drawn right-handed; left-handed mirrors it), wearing
            // the bracelet's band if the main hand holds one. Not played: the main hand is left to its own renderer.
            ItemStack main = player.getMainHandItem();
            boolean playing = player.isUsingItem() && player.getUsedItemHand() == InteractionHand.OFF_HAND;
            drawDrum(player, stack, pose, buffers, light, event, player.getMainArm() == HumanoidArm.LEFT ? -1.0F : 1.0F,
                    playing, main.getItem() instanceof CastingBracelet ? main : ItemStack.EMPTY, playing || main.isEmpty());
        } else {
            drawDrum(player, stack, pose, buffers, light, event, 1.0F,
                    player.isUsingItem() && player.getUseItem().getItem() instanceof ThaumaturgeDrum, ItemStack.EMPTY, true);
        }
        pose.popPose();
    }

    private static void drawMonitor(LocalPlayer player, ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, RenderHandEvent event) {
        // One transform for hands and frame: the swing tilt applies to both.
        pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(event.getSwingProgress() * (float) Math.PI) * 20.0F));
        if (!player.isInvisible()) {
            pose.pushPose();
            pose.translate(0.0F, GRIP_UP, -GRIP_BACK);
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            pose.pushPose();
            pose.translate(0.0F, 0.0F, -GRIP_IN);
            arm(player, pose, buffers, light, true, ItemStack.EMPTY);
            pose.popPose();
            pose.pushPose();
            pose.translate(0.0F, 0.0F, GRIP_IN);
            arm(player, pose, buffers, light, false, ItemStack.EMPTY);
            pose.popPose();
            pose.popPose();
        }
        pose.translate(0.0F, MONITOR_Y, 0.0F);
        pose.scale(MONITOR_SCALE, MONITOR_SCALE, MONITOR_SCALE);
        // The scanner mesh lies flat (screen up, hexagon corners along z): stand it up to face the camera, corners sideways.
        // The original's hexagon has flat edges top and bottom, corners left and right: a 30 degree roll about the view axis.
        pose.mulPose(Axis.ZP.rotationDegrees(30.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        AstralMonitorRenderer.forceFirstPerson = true;
        try {
            item(player, stack, pose, buffers, light);
        } finally {
            AstralMonitorRenderer.forceFirstPerson = false;
        }
    }

    /**
     * @param m 1 for the picture as drawn (drum on the left), -1 mirrored for a left-handed player: x offsets and the
     *          turns about y and z flip, and each arm is the other one
     * @param using the drum is being played (the use key held); the use ticks drive the beat
     * @param band the main-hand bracelet whose band shows on the striking forearm, or empty
     * @param strikingArm whether to draw the striking arm (not when the main hand is drawn by its own renderer)
     */
    private static void drawDrum(LocalPlayer player, ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light,
                                 RenderHandEvent event, float m, boolean using, ItemStack band, boolean strikingArm) {
        // Beat: 8 ticks (the item's own sound). Real technique: ~2 ticks fast accelerating downstroke, ~1 tick contact,
        // ~3 ticks decelerating lift, ~2 ticks hovering above the skin. e: 0 hovering, 1 on the skin.
        float e = 0.0F;
        float squeeze = 0.0F;
        float wrist = 0.0F;
        boolean slap = false;
        if (using) {
            float t = player.getTicksUsingItem() + event.getPartialTick();
            slap = ((int) Math.floor(t / DRUM_BEAT_TICKS) & 1) == 1;
            t = t % DRUM_BEAT_TICKS;
            if (t < DRUM_CONTACT_TICK) {
                float u = t / DRUM_CONTACT_TICK;
                e = u * u;
                wrist = u * u * u;
            } else if (t < 3.0F) {
                e = 1.0F;
                wrist = 1.0F;
            } else if (t < 6.0F) {
                float u = (t - 3.0F) / 3.0F;
                e = (1.0F - u) * (1.0F - u);
                // fingertips lift first: the wrist unbends ahead of the forearm
                wrist = Math.max(0.0F, 1.0F - u * 2.5F);
            }
            // the left arm squeezes the drum on the beat and recovers over the next ~2.5 ticks
            if (t >= 2.0F && t < 5.5F) {
                float u = Math.min(1.0F, (t - 2.0F) / 0.5F);
                squeeze = t < 3.0F ? u : 1.0F - (t - 3.0F) / 2.5F;
            } else if (t >= 1.5F && t < 2.0F) {
                squeeze = (t - 1.5F) * 2.0F * 0.3F;
            }
            squeeze = Mth.clamp(squeeze, 0.0F, 1.0F);
        }
        float dip = -SQUEEZE_DIP * squeeze;
        float tilt = SQUEEZE_ROLL * squeeze;
        pose.pushPose();
        pose.translate(0.0F, dip, 0.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(tilt * m));
        if (!player.isInvisible()) {
            pose.pushPose();
            // The left forearm comes in from the bottom left and wraps the drum's waist.
            pose.pushPose();
            pose.translate(LEFT_ARM_X * m, LEFT_ARM_Y, 0.0F);
            pose.mulPose(Axis.YP.rotationDegrees(90.0F * m));
            arm(player, pose, buffers, light, m < 0.0F, ItemStack.EMPTY);
            pose.popPose();
            pose.popPose();
            pose.pushPose();
            if (!strikingArm) {
                // the main hand is drawn by its own renderer
            } else if (using) {
                // Bass strokes land on the middle of the skin, flat; slaps nearer the rim on the player's side, more wrist.
                float sx = STRIKE_X + (slap ? SLAP_DX : 0.0F);
                float sy = STRIKE_Y + (slap ? SLAP_DY : 0.0F);
                float sroll = STRIKE_ROLL + (slap ? SLAP_ROLL : 0.0F) + WRIST_SNAP * wrist;
                pose.translate(Mth.lerp(e, HOVER_X, sx) * m, Mth.lerp(e, HOVER_Y, sy), 0.0F);
                pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(e, HOVER_ROLL, sroll) * m));
            } else {
                pose.translate(IDLE_X * m, IDLE_Y, 0.0F);
                pose.mulPose(Axis.ZP.rotationDegrees(IDLE_ROLL * m));
            }
            if (strikingArm) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F * m));
                arm(player, pose, buffers, light, m > 0.0F, band);
            }
            pose.popPose();
        }
        // The drum rests on the left forearm, centre left, skin up and a little toward the camera. The OBJ stands 2 blocks
        // tall (skin on top), so the last translate puts the middle of the skin at the origin.
        pose.translate(DRUM_X * m, DRUM_Y, DRUM_Z);
        pose.mulPose(Axis.XP.rotationDegrees(DRUM_TILT));
        pose.scale(DRUM_SCALE, DRUM_SCALE, DRUM_SCALE);
        pose.translate(0.0F, -1.5F, 0.0F);
        item(player, stack, pose, buffers, light);
        pose.popPose();
    }

    private static void item(LocalPlayer player, ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light) {
        Minecraft.getInstance().getItemRenderer().renderStatic(player, stack, ItemDisplayContext.NONE, false, pose, buffers,
                player.level(), light, OverlayTexture.NO_OVERLAY, player.getId());
    }

    /** Vanilla's ItemInHandRenderer.renderMapHand. */
    private static void arm(LocalPlayer player, PoseStack pose, MultiBufferSource buffers, int light, boolean right, ItemStack bracelet) {
        PlayerRenderer renderer = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        float side = right ? 1.0F : -1.0F;
        pose.pushPose();
        pose.scale(ARM_SCALE, ARM_SCALE, ARM_SCALE);
        pose.mulPose(Axis.YP.rotationDegrees(92.0F));
        pose.mulPose(Axis.XP.rotationDegrees(45.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(side * -41.0F));
        pose.translate(side * 0.3F, -1.1F, 0.45F);
        if (right) {
            renderer.renderRightHand(pose, buffers, light, player);
        } else {
            renderer.renderLeftHand(pose, buffers, light, player);
        }
        if (bracelet.getItem() instanceof CastingBracelet item) {
            // the band on the wrist, placed as BraceletFirstPerson does after drawing the bare arm
            pose.pushPose();
            (right ? renderer.getModel().rightArm : renderer.getModel().leftArm).translateAndRotate(pose);
            BraceletBandRenderer.draw(pose, buffers, item.kind(), right ? HumanoidArm.RIGHT : HumanoidArm.LEFT,
                    player.getSkin().model() == PlayerSkin.Model.SLIM, light, BraceletBandRenderer.gemColor(bracelet));
            pose.popPose();
        }
        pose.popPose();
    }
}
