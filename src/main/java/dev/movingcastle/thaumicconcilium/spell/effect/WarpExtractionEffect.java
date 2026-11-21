package dev.movingcastle.thaumicconcilium.spell.effect;

import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.mojang.serialization.MapCodec;
import dev.movingcastle.thaumicconcilium.entity.ConcentratedWarpCharge;
import dev.movingcastle.thaumicconcilium.registry.TCSounds;
import dev.movingcastle.thaumicconcilium.registry.TCSpellBehaviors;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Warp Extraction: "share the pain", ported 1:1. Sneak + cast pulls a {@link ConcentratedWarpCharge} out of your head
 * (it lives on temporary warp, which Reflection provides), or dismisses yours if it is within 10 blocks. Cast and hold
 * to draw in the charge you are looking at; let go to hurl it (see
 * {@link dev.movingcastle.thaumicconcilium.spell.ChannelEvents}). Needs a Ring of Blustering Light.
 * <p>
 * Settings: {@code potency} (damage multiplier {@code 1 + potency}), {@code reach} (how far the charge's effect
 * transfers spread) and {@code mode}: none, mass hysteria or self-flagellation.
 */
public final class WarpExtractionEffect extends AbstractEffectBehavior {
    public static final WarpExtractionEffect INSTANCE = new WarpExtractionEffect();
    public static final MapCodec<WarpExtractionEffect> CODEC = MapCodec.unit(INSTANCE);

    private WarpExtractionEffect() {
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpellBehaviors.WARP_EXTRACTION.get();
    }

    @Override
    protected boolean widens() {
        return false;
    }

    public static List<ConcentratedWarpCharge> chargesOf(Player player, double range) {
        return player.level().getEntitiesOfClass(ConcentratedWarpCharge.class, player.getBoundingBox().inflate(range), c -> c.isOwnedBy(player));
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        if (!(ctx.caster() instanceof ServerPlayer player) || index > 0) {
            return;
        }
        ServerLevel level = ctx.level();
        if (player.isShiftKeyDown()) {
            // The original's Shift + right-click, a one-off: dismiss your charge if one is within 10 blocks, else pull
            // a new one out of your head ("Press Shift+RMB to extract the warp from your brain").
            List<ConcentratedWarpCharge> near = chargesOf(player, 10.0);
            if (!near.isEmpty()) {
                near.get(0).discard();
            } else {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), TCSounds.BREATH.get(), SoundSource.PLAYERS,
                        0.9F, 0.4F / (ctx.random().nextFloat() * 0.4F + 0.8F));
                ConcentratedWarpCharge.summon(level, player, 1 + ctx.setting("potency"), ctx.setting("reach"), ctx.setting("mode"));
            }
            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            player.stopUsingItem();
            dev.movingcastle.thaumicconcilium.spell.BraceletCasting.cancel(player);
            return;
        }
        // Held (the original's using tick): draw in the charge you're looking at, within 32 blocks; release throws it
        // ("Hold down RMB to throw it away", ChannelEvents).
        ConcentratedWarpCharge pointed = pointedCharge(player, 32.0, 2.0);
        if (pointed != null) {
            Vec3 look = player.getLookAngle();
            pointed.steer(new Vec3(player.getX() + look.x * 1.5, player.getY() + look.y * 1.5 + 2.0, player.getZ() + look.z * 1.5));
        }
    }

    /** The own charge nearest the line of sight (within {@code width} blocks of it), like TC4's getPointedEntity. */
    private static ConcentratedWarpCharge pointedCharge(ServerPlayer player, double range, double width) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        ConcentratedWarpCharge best = null;
        double bestAlong = Double.MAX_VALUE;
        for (ConcentratedWarpCharge charge : chargesOf(player, range)) {
            Vec3 to = charge.getBoundingBox().getCenter().subtract(eye);
            double along = to.dot(look);
            if (along <= 0.0 || along > range) {
                continue;
            }
            double off = to.subtract(look.scale(along)).length();
            if (off <= width + charge.getBbWidth() / 2.0 && along < bestAlong) {
                best = charge;
                bestAlong = along;
            }
        }
        return best;
    }
}
