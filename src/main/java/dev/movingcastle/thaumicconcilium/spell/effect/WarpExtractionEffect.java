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
 * Warp Extraction: "share the pain". Channel to pull a {@link ConcentratedWarpCharge} out of your head (it needs
 * temporary warp, which Reflection provides); keep channelling to hold it in front of you, and release to hurl it
 * (see {@link dev.movingcastle.thaumicconcilium.spell.ChannelEvents}). Start the channel while sneaking to dismiss
 * your charge instead. Needs a Ring of Blustering Light.
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
        List<ConcentratedWarpCharge> mine = chargesOf(player, 32.0);
        if (player.isShiftKeyDown()) {
            if (!mine.isEmpty()) {
                mine.forEach(ConcentratedWarpCharge::discard);
                player.stopUsingItem();
            }
            return;
        }
        if (mine.isEmpty()) {
            if (WarpHelper.getWarp(player).get(WarpType.TEMPORARY) <= 0) {
                player.sendSystemMessage(Component.translatable("message.thaumicconcilium.no_warp").withStyle(ChatFormatting.DARK_PURPLE));
                player.stopUsingItem();
                return;
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), TCSounds.BREATH.get(), SoundSource.PLAYERS,
                    0.9F, 0.4F / (ctx.random().nextFloat() * 0.4F + 0.8F));
            ConcentratedWarpCharge.summon(level, player, 1 + ctx.setting("potency"), ctx.setting("reach"), ctx.setting("mode"));
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 point = new Vec3(player.getX() + look.x * 1.5, player.getY() + look.y * 1.5 + 2.0, player.getZ() + look.z * 1.5);
        for (ConcentratedWarpCharge charge : mine) {
            charge.steer(point);
        }
    }
}
