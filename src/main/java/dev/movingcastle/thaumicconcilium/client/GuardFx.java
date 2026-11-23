package dev.movingcastle.thaumicconcilium.client;

import com.leclowndu93150.thaumaturge.client.effect.LateWorldRenderQueue;
import com.leclowndu93150.thaumaturge.client.render.TTRenderTypes;
import com.leclowndu93150.thaumaturge.content.particle.BoltParticleOptions;
import com.leclowndu93150.thaumaturge.content.particle.CrackShardParticleOptions;
import com.leclowndu93150.thaumaturge.content.particle.LightningFlashParticleOptions;
import com.leclowndu93150.thaumaturge.content.particle.ShieldSparkParticleOptions;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.client.GuardFxState.State;
import dev.movingcastle.thaumicconcilium.network.GuardFxPayload;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import org.joml.Matrix4f;

/**
 * The shield foci's look (docs/design/shield-foci-look.md, round 2: one family of shimmering wards). An armed Riposte is a ward
 * shell round the off-hand shield with a rim halo (third person: {@link Layer}; first person: {@link #onRenderHand}), more and
 * brighter shells for more charges, and it bursts with crackling bolts and shards on a release. The Aegis is a band of the same
 * ward round its holder with runes on it ({@link #onRenderLevel}), and crackles where a forced block lands. State and curves are
 * {@link GuardFxState}'s. Every hook returns at once while no entity has any state. Thaumaturge's particles ({@code bolt},
 * {@code crack_shard}, {@code lightning_flash}, {@code shield_spark}) and its additive particle render types are used by id; the
 * textures are ours (tools/gen_shield_foci_data.py).
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID, value = Dist.CLIENT)
public final class GuardFx {
    private static final ResourceLocation HALO = ThaumicConcilium.id("textures/misc/guard_halo.png");
    private static final ResourceLocation WARD = ThaumicConcilium.id("textures/misc/guard_ward.png");
    private static final ResourceLocation RUNES = ThaumicConcilium.id("textures/misc/aegis_runes.png");
    private static final int LIGHT = 0xF000F0;
    private static final float PX = 1.0F / 16.0F;
    // the shield plate, in the item's model space (after the display transform and the half-block shift): 12 x 22 px, faces at z 1 and 2 px
    private static final float HALO_HALF_W = 10.0F * PX;
    private static final float HALO_HALF_H = 15.0F * PX;
    private static final float PLATE_MID = 1.5F * PX;
    /** The viewer's own shield fills a third of the screen: its halo and shell are this much fainter. */
    private static final float OWN_GAIN = 0.4F;
    // the ward shell: a flattened ellipsoid about the plate's centre (half-axes in px), one more per layer, each a little larger
    private static final float SHELL_A = 8.5F * PX;
    private static final float SHELL_B = 13.5F * PX;
    private static final float SHELL_C = 3.2F * PX;
    private static final float SHELL_LAYER_STEP = 1.3F * PX;
    private static final int SHELL_LON = 14;
    private static final int SHELL_LAT = 8;
    private static final float[] LAYER_ALPHA = {1.0F, 0.55F, 0.3F};
    /** Outer layers are dimmed harder where they face the viewer: they add rims, not a film over the shield's face. */
    private static final float[] LAYER_FACE_DIM = {0.65F, 0.80F, 0.90F};
    private static final float WARD_SCROLL_U = 0.004F;
    private static final float WARD_SCROLL_V = 0.007F;
    // the Aegis band
    private static final int RUNE_COUNT = 8;
    private static final int RUNE_CELLS = 9;   // eight runes and a white cell
    private static final float RUNE_HALF = 0.14F;
    private static final float BAND_AT = 0.19F;
    private static final float BAND_HALF = 0.01F;
    private static final float BAND_ALPHA = 0.35F;
    private static final float WALL_ALPHA = 0.45F;
    private static final float WALL_TILES = 6.0F;
    private static final int BAND_SEGMENTS = 24;
    private static final float FLASH_HALF = 0.4F;
    private static final int FLASH_COLOR = GuardFxState.WARD_HOT;
    private static final float OWN_RING = 0.6F;

    private GuardFx() {
    }

    // ------------------------------------------------------------------------------ messages, sparks and crackle

    public static void receive(GuardFxPayload p) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        GuardFxState.apply(p, level.getGameTime());
        if (!(level.getEntity(p.entityId()) instanceof LivingEntity who)) {
            return;
        }
        RandomSource random = level.getRandom();
        boolean own = ownFirstPerson(who);
        switch (p.kind()) {
            case GuardFxPayload.ARMED -> {
                Vec3 at = shieldPoint(who);
                for (int i = 0; i < 6; i++) {
                    Vec3 j = jitter(random, 0.25);
                    level.addParticle(new ShieldSparkParticleOptions(GuardFxState.RIPOSTE_COLOR, 0.8F, 0.5F, 8, i, true),
                            at.x + j.x, at.y + j.y, at.z + j.z, 0.0, 0.0, 0.0);
                }
            }
            case GuardFxPayload.RELEASED -> {
                Vec3 at = shieldPoint(who);
                Vec3 dir = new Vec3(p.dx(), p.dy(), p.dz());
                level.addParticle(new LightningFlashParticleOptions(0xFF000000 | GuardFxState.WARD_HOT, 0.9F, own ? 3.0F : 6.0F), at.x, at.y, at.z, 0.0, 0.0, 0.0);
                shards(level, random, at, dir, own ? 6 : 12, GuardFxState.RIPOSTE_COLOR, 4, 4);
                bolts(level, random, at, dir, own ? 2 : 4, GuardFxState.RIPOSTE_COLOR);
            }
            case GuardFxPayload.AEGIS_BLOCK -> {
                Vec3 flat = flat(p.dx(), p.dz(), who);
                Vec3 at = ringPoint(who, flat);
                shards(level, random, at, flat, own ? 4 : 8, GuardFxState.AEGIS_COLOR, 4, 4);
                bolts(level, random, at, flat, 2, GuardFxState.AEGIS_COLOR);
            }
            default -> {
            }
        }
    }

    /**
     * The crackle after a burst: a bolt or two and a shard each tick for {@link GuardFxState#CRACKLE_TICKS} after a release or a
     * forced block (a bolt lives three ticks, so one volley alone reads as a single flash). Idle unless some entity has state.
     */
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (GuardFxState.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) {
            return;
        }
        long now = level.getGameTime();
        RandomSource random = level.getRandom();
        for (Map.Entry<Integer, State> e : GuardFxState.all().entrySet()) {
            State s = e.getValue();
            boolean release = now > s.releaseAt && now - s.releaseAt <= GuardFxState.CRACKLE_TICKS;
            boolean block = now > s.aegisFlashAt && now - s.aegisFlashAt <= GuardFxState.CRACKLE_TICKS;
            if (!release && !block || !(level.getEntity(e.getKey()) instanceof LivingEntity who)) {
                continue;
            }
            if (release) {
                Vec3 at = shieldPoint(who);
                Vec3 dir = new Vec3(s.rx, s.ry, s.rz);
                bolts(level, random, at, dir, ownFirstPerson(who) ? 1 : 2, GuardFxState.RIPOSTE_COLOR);
                shards(level, random, at, dir, 2, GuardFxState.RIPOSTE_COLOR, 3, 3);
            }
            if (block) {
                Vec3 flat = flat(s.ax, s.az, who);
                Vec3 at = ringPoint(who, flat);
                bolts(level, random, at, flat, 1, GuardFxState.AEGIS_COLOR);
                shards(level, random, at, flat, 1, GuardFxState.AEGIS_COLOR, 3, 3);
            }
        }
    }

    /** Shards of the broken ward thrown out along {@code dir} (they slow at once: Thaumaturge's shard has friction 0.8). */
    private static void shards(ClientLevel level, RandomSource random, Vec3 at, Vec3 dir, int count, int color, int minAge, int ageSpread) {
        for (int i = 0; i < count; i++) {
            Vec3 j = jitter(random, 0.2);
            Vec3 v = dir.scale(0.08 + random.nextDouble() * 0.14).add(jitter(random, 0.10));
            level.addParticle(new CrackShardParticleOptions(0xFF000000 | color, random.nextInt(4), 1.2F + random.nextFloat() * 1.2F, minAge + random.nextInt(ageSpread)),
                    at.x + j.x, at.y + j.y, at.z + j.z, v.x, v.y, v.z);
        }
    }

    /** Short jagged bolts from the burst point, leaning along {@code dir}. */
    private static void bolts(ClientLevel level, RandomSource random, Vec3 at, Vec3 dir, int count, int color) {
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        for (int i = 0; i < count; i++) {
            Vec3 way = dir.scale(0.5).add(jitter(random, 0.7));
            if (way.lengthSqr() < 1.0E-4) {
                continue;
            }
            Vec3 to = at.add(way.normalize().scale(0.6 + random.nextDouble() * 0.7));
            level.addParticle(new BoltParticleOptions(to.x, to.y, to.z, r, g, b, 0.45F), at.x, at.y, at.z, 0.0, 0.0, 0.0);
        }
    }

    private static Vec3 jitter(RandomSource random, double by) {
        return new Vec3((random.nextDouble() * 2.0 - 1.0) * by, (random.nextDouble() * 2.0 - 1.0) * by, (random.nextDouble() * 2.0 - 1.0) * by);
    }

    private static boolean ownFirstPerson(Entity e) {
        Minecraft mc = Minecraft.getInstance();
        return e == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
    }

    private static Vec3 flat(float dx, float dz, LivingEntity fallbackFacing) {
        Vec3 flat = new Vec3(dx, 0.0, dz);
        return flat.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, fallbackFacing.yBodyRot) : flat.normalize();
    }

    /** The point of the Aegis band in the unit direction {@code flat}. */
    private static Vec3 ringPoint(LivingEntity who, Vec3 flat) {
        float r = who.getBbWidth() * 0.5F + GuardFxState.RING_MARGIN;
        return who.position().add(0.0, who.getBbHeight() * GuardFxState.RING_HEIGHT, 0.0).add(flat.scale(r));
    }

    /** Where the off-hand shield is, near enough for a burst (the drawn cues use the real hand transform). */
    private static Vec3 shieldPoint(LivingEntity e) {
        boolean offLeft = e.getMainArm() == HumanoidArm.RIGHT;
        if (ownFirstPerson(e)) {
            Vec3 look = e.getLookAngle();
            Vec3 left = new Vec3(look.z, 0.0, -look.x);
            return e.getEyePosition().add(look.scale(0.9)).add(left.scale(offLeft ? 0.25 : -0.25)).add(0.0, -0.2, 0.0);
        }
        float yaw = e.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 facing = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
        Vec3 left = new Vec3(Mth.cos(yaw), 0.0, Mth.sin(yaw));
        return e.position().add(0.0, e.getBbHeight() * 0.62, 0.0).add(facing.scale(0.7)).add(left.scale(offLeft ? 0.15 : -0.15));
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        GuardFxState.clear();
    }

    // ------------------------------------------------------------------------------ Riposte: the warded shield

    private static boolean shows(State s, double t) {
        return s != null && (s.armed(t) || s.releaseEase(t) > 0.0F);
    }

    /** Moves the pose from the hand's item anchor into the item model's own space, as the item renderer does. */
    private static void itemSpace(PoseStack pose, ItemStack stack, LivingEntity holder, ItemDisplayContext context, boolean left) {
        BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(stack, holder.level(), holder, holder.getId() + context.ordinal());
        ClientHooks.handleCameraTransforms(pose, model, context, left);
        pose.translate(-0.5F, -0.5F, -0.5F);
    }

    /**
     * The rim halo and the ward shells, in the shield's model space. The shield must already be drawn (not merely batched): the
     * shells do not write depth, and an opaque item flushed after them would paint over the half that lies before its plate.
     */
    private static void drawShield(PoseStack pose, MultiBufferSource buffers, State s, double t, float gain) {
        Matrix4f m = pose.last().pose();
        int c = GuardFxState.RIPOSTE_COLOR;
        float alpha = s.haloAlpha(t) * gain;
        if (alpha > 0.0F) {
            float k = s.haloScale(t);
            float w = HALO_HALF_W * k;
            float h = HALO_HALF_H * k;
            int a = (int) (alpha * 255.0F);
            VertexConsumer vc = buffers.getBuffer(TTRenderTypes.fxAlphaAdditiveBlurred(HALO));
            vertex(vc, m, -w, -h, PLATE_MID, 0.0F, 1.0F, c, a);
            vertex(vc, m, -w, h, PLATE_MID, 0.0F, 0.0F, c, a);
            vertex(vc, m, w, h, PLATE_MID, 1.0F, 0.0F, c, a);
            vertex(vc, m, w, -h, PLATE_MID, 1.0F, 1.0F, c, a);
        }
        VertexConsumer vc = buffers.getBuffer(TTRenderTypes.fxAlphaAdditive(WARD));
        float shell = s.shellAlpha(t) * gain;
        if (shell > 0.0F) {
            float form = s.shellScale(t);
            for (int layer = 0, n = GuardFxState.shellLayers(s.charges); layer < n; layer++) {
                float grow = layer * SHELL_LAYER_STEP;
                float way = (layer & 1) == 0 ? 1.0F : -1.0F;   // neighbouring layers drift against each other: the shimmer
                shell(vc, m, (SHELL_A + grow) * form, (SHELL_B + grow) * form, (SHELL_C + grow) * form,
                        (float) (t * WARD_SCROLL_U * way) + layer * 0.37F, (float) (t * WARD_SCROLL_V) + layer * 0.19F, c, shell * LAYER_ALPHA[layer], LAYER_FACE_DIM[layer]);
            }
        }
        float burst = s.releaseEase(t);
        if (burst > 0.0F) {
            float k = s.burstScale(t);
            shell(vc, m, SHELL_A * k, SHELL_B * k, SHELL_C * k * 1.5F, (float) (t * 0.05), (float) (t * 0.03), GuardFxState.WARD_HOT, burst * gain, LAYER_FACE_DIM[0]);
        }
    }

    /** One ward shell: an ellipsoid about the plate's centre with half-axes (a, b, c), the tiling ward texture offset by (du, dv). */
    private static void shell(VertexConsumer vc, Matrix4f m, float a, float b, float c, float du, float dv, int color, float alpha, float faceDim) {
        for (int j = 0; j < SHELL_LAT; j++) {
            float p0 = (j / (float) SHELL_LAT - 0.5F) * Mth.PI;
            float p1 = ((j + 1) / (float) SHELL_LAT - 0.5F) * Mth.PI;
            for (int i = 0; i < SHELL_LON; i++) {
                float t0 = i * Mth.TWO_PI / SHELL_LON;
                float t1 = (i + 1) * Mth.TWO_PI / SHELL_LON;
                shellVertex(vc, m, a, b, c, t0, p0, i, j, du, dv, color, alpha, faceDim);
                shellVertex(vc, m, a, b, c, t0, p1, i, j + 1, du, dv, color, alpha, faceDim);
                shellVertex(vc, m, a, b, c, t1, p1, i + 1, j + 1, du, dv, color, alpha, faceDim);
                shellVertex(vc, m, a, b, c, t1, p0, i + 1, j, du, dv, color, alpha, faceDim);
            }
        }
    }

    private static void shellVertex(VertexConsumer vc, Matrix4f m, float a, float b, float c, float lon, float lat, int i, int j, float du, float dv,
                                    int color, float alpha, float faceDim) {
        float cl = Mth.cos(lat);
        float nz = cl * Mth.cos(lon);
        float k = 1.0F - faceDim * Math.abs(nz);
        vertex(vc, m, a * cl * Mth.sin(lon), b * Mth.sin(lat), PLATE_MID + c * nz, 2.0F * i / SHELL_LON + du, 3.0F * j / SHELL_LAT + dv, color,
                Mth.clamp((int) (alpha * k * 255.0F), 0, 255));
    }

    private static void flushBefore(MultiBufferSource buffers) {
        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch();
        }
    }

    /** Third person: round the off-hand shield of an armed player, in the hand transform of the game's own held-item layer. */
    public static final class Layer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        public Layer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                           float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (GuardFxState.isEmpty()) {
                return;
            }
            State s = GuardFxState.get(player.getId());
            double t = player.level().getGameTime() + partialTick;
            ItemStack stack = player.getOffhandItem();
            if (!shows(s, t) || player.isInvisible() || !stack.canPerformAction(ItemAbilities.SHIELD_BLOCK)) {
                return;
            }
            HumanoidArm arm = player.getMainArm().getOpposite();
            boolean left = arm == HumanoidArm.LEFT;
            flushBefore(buffers);   // the held-item layer ran before this one: its shield is batched, draw it now
            pose.pushPose();
            this.getParentModel().translateToHand(arm, pose);
            pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
            pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            pose.translate((left ? -1 : 1) / 16.0F, 0.125F, -0.625F);
            itemSpace(pose, stack, player, left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, left);
            drawShield(pose, buffers, s, t, 1.0F);
            pose.popPose();
        }
    }

    /**
     * First person: the same round the viewer's own off-hand shield. The shield has to be drawn before its ward, and the game
     * draws it after this event, so while a ward shows this takes the off hand over: it draws the shield exactly as
     * {@code ItemInHandRenderer.renderArmWithItem} would (lowered with the swing, or raised as a blocking item), then the ward.
     * Anything else (another use animation, a riptide spin, another handler's takeover such as a bracelet cast) is left alone.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderHand(RenderHandEvent event) {
        if (GuardFxState.isEmpty() || event.getHand() != InteractionHand.OFF_HAND) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isInvisible() || player.isScoping() || player.isAutoSpinAttack()) {
            return;
        }
        State s = GuardFxState.get(player.getId());
        double t = player.level().getGameTime() + event.getPartialTick();
        ItemStack stack = event.getItemStack();
        if (!shows(s, t) || !stack.canPerformAction(ItemAbilities.SHIELD_BLOCK)) {
            return;
        }
        boolean using = player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == InteractionHand.OFF_HAND;
        if (using && stack.getUseAnimation() != UseAnim.BLOCK && stack.getUseAnimation() != UseAnim.NONE) {
            return;
        }
        HumanoidArm arm = player.getMainArm().getOpposite();
        boolean right = arm == HumanoidArm.RIGHT;
        int side = right ? 1 : -1;
        float swing = event.getSwingProgress();
        float equip = event.getEquipProgress();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        ItemDisplayContext context = right ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        event.setCanceled(true);
        pose.pushPose();
        if (using) {
            pose.translate(side * 0.56F, -0.52F + equip * -0.6F, -0.72F);
        } else {
            pose.translate(side * -0.4F * Mth.sin(Mth.sqrt(swing) * Mth.PI), 0.2F * Mth.sin(Mth.sqrt(swing) * Mth.TWO_PI), -0.2F * Mth.sin(swing * Mth.PI));
            pose.translate(side * 0.56F, -0.52F + equip * -0.6F, -0.72F);
            float f = Mth.sin(swing * swing * Mth.PI);
            pose.mulPose(Axis.YP.rotationDegrees(side * (45.0F + f * -20.0F)));
            float f1 = Mth.sin(Mth.sqrt(swing) * Mth.PI);
            pose.mulPose(Axis.ZP.rotationDegrees(side * f1 * -20.0F));
            pose.mulPose(Axis.XP.rotationDegrees(f1 * -80.0F));
            pose.mulPose(Axis.YP.rotationDegrees(side * -45.0F));
        }
        Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(player, stack, context, !right, pose, buffers,
                event.getPackedLight());
        flushBefore(buffers);
        itemSpace(pose, stack, player, context, !right);
        drawShield(pose, buffers, s, t, OWN_GAIN);
        pose.popPose();
    }

    // ------------------------------------------------------------------------------ Aegis: the warded band

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || GuardFxState.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        GuardFxState.prune(now);
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double t = now + partial;
        for (Map.Entry<Integer, State> e : GuardFxState.all().entrySet()) {
            State s = e.getValue();
            float flash = s.flashEase(t);
            if (!s.aegis(t) && flash <= 0.0F) {
                continue;
            }
            if (!(level.getEntity(e.getKey()) instanceof LivingEntity who) || !who.isAlive() || who.isInvisible()) {
                continue;
            }
            boolean up = s.aegis(t) && who.isBlocking();
            if (!up && flash <= 0.0F) {
                continue;
            }
            float raised = up ? who.getTicksUsingItem() + partial : 0.0F;
            float alpha = s.ringAlpha(t, raised) * (ownFirstPerson(who) ? OWN_RING : 1.0F);
            float grown = up ? GuardFxState.ringIn(raised) : 1.0F;
            float radius = (who.getBbWidth() * 0.5F + GuardFxState.RING_MARGIN) * (0.7F + 0.3F * grown);
            float spin = (float) (t * GuardFxState.RING_SPIN % 360.0);
            float scroll = (float) (t * WARD_SCROLL_V % 1.0);
            int cell = e.getKey() & 7;
            float ax = s.ax;
            float az = s.az;
            Vec3 origin = who.getPosition(partial).add(0.0, who.getBbHeight() * GuardFxState.RING_HEIGHT, 0.0);
            LateWorldRenderQueue.enqueueEntity(origin, (pose, buffers) -> ring(pose, buffers, radius, alpha, spin, scroll, flash, ax, az, cell));
        }
    }

    private static void ring(PoseStack pose, MultiBufferSource buffers, float radius, float alpha, float spin, float scroll, float flash,
                             float ax, float az, int flashCell) {
        Matrix4f m = pose.last().pose();
        int c = GuardFxState.AEGIS_COLOR;
        int a = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        if (a > 0) {
            // the wall: the same ward as the Riposte shell, drifting round the band and upward
            VertexConsumer ward = buffers.getBuffer(TTRenderTypes.fxAlphaAdditive(WARD));
            int wall = (int) (a * WALL_ALPHA);
            float turn = spin / 360.0F;
            for (int i = 0; i < BAND_SEGMENTS; i++) {
                float a0 = i * Mth.TWO_PI / BAND_SEGMENTS;
                float a1 = (i + 1) * Mth.TWO_PI / BAND_SEGMENTS;
                float u0 = WALL_TILES * i / BAND_SEGMENTS + turn;
                float u1 = WALL_TILES * (i + 1) / BAND_SEGMENTS + turn;
                vertex(ward, m, Mth.sin(a0) * radius, -BAND_AT, Mth.cos(a0) * radius, u0, 0.45F + scroll, c, wall);
                vertex(ward, m, Mth.sin(a0) * radius, BAND_AT, Mth.cos(a0) * radius, u0, scroll, c, wall);
                vertex(ward, m, Mth.sin(a1) * radius, BAND_AT, Mth.cos(a1) * radius, u1, scroll, c, wall);
                vertex(ward, m, Mth.sin(a1) * radius, -BAND_AT, Mth.cos(a1) * radius, u1, 0.45F + scroll, c, wall);
            }
        }
        VertexConsumer vc = buffers.getBuffer(TTRenderTypes.fxAlphaAdditive(RUNES));
        if (a > 0) {
            for (int k = 0; k < RUNE_COUNT; k++) {
                float angle = (spin + k * 360.0F / RUNE_COUNT) * Mth.DEG_TO_RAD;
                rune(vc, m, Mth.sin(angle), Mth.cos(angle), radius, RUNE_HALF, k, c, a);
            }
            int band = (int) (a * BAND_ALPHA);
            float u = (RUNE_COUNT + 0.5F) / RUNE_CELLS;
            for (int i = 0; i < BAND_SEGMENTS; i++) {
                float a0 = i * Mth.TWO_PI / BAND_SEGMENTS;
                float a1 = (i + 1) * Mth.TWO_PI / BAND_SEGMENTS;
                float x0 = Mth.sin(a0) * radius;
                float z0 = Mth.cos(a0) * radius;
                float x1 = Mth.sin(a1) * radius;
                float z1 = Mth.cos(a1) * radius;
                for (int side = -1; side <= 1; side += 2) {
                    float y = side * BAND_AT;
                    vertex(vc, m, x0, y - BAND_HALF, z0, u, 0.5F, c, band);
                    vertex(vc, m, x0, y + BAND_HALF, z0, u, 0.5F, c, band);
                    vertex(vc, m, x1, y + BAND_HALF, z1, u, 0.5F, c, band);
                    vertex(vc, m, x1, y - BAND_HALF, z1, u, 0.5F, c, band);
                }
            }
        }
        if (flash > 0.0F) {
            float len = Mth.sqrt(ax * ax + az * az);
            float dx = len < 1.0E-3F ? 0.0F : ax / len;
            float dz = len < 1.0E-3F ? 1.0F : az / len;
            float grow = 0.7F + 0.45F * (1.0F - flash);
            rune(vc, m, dx, dz, radius + 0.02F, FLASH_HALF * grow, flashCell, FLASH_COLOR, Mth.clamp((int) (flash * 255.0F), 0, 255));
        }
    }

    /** A square rune standing on the band at unit direction (dx, dz), facing outward, seen from both sides. */
    private static void rune(VertexConsumer vc, Matrix4f m, float dx, float dz, float radius, float half, int cell, int c, int a) {
        float cx = dx * radius;
        float cz = dz * radius;
        float tx = dz * half;
        float tz = -dx * half;
        float u0 = (float) cell / RUNE_CELLS;
        float u1 = (float) (cell + 1) / RUNE_CELLS;
        vertex(vc, m, cx - tx, -half, cz - tz, u0, 1.0F, c, a);
        vertex(vc, m, cx - tx, half, cz - tz, u0, 0.0F, c, a);
        vertex(vc, m, cx + tx, half, cz + tz, u1, 0.0F, c, a);
        vertex(vc, m, cx + tx, -half, cz + tz, u1, 1.0F, c, a);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float v, int c, int a) {
        vc.addVertex(m, x, y, z).setUv(u, v).setColor(c >> 16 & 255, c >> 8 & 255, c & 255, a).setLight(LIGHT);
    }
}
