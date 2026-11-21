package dev.movingcastle.thaumicconcilium.item;

import com.leclowndu93150.thaumaturge.api.warp.IPlayerWarp;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * A curio that feeds on temporary warp, as the original Burdening Amulet and Tight Belt did. While the wearer has
 * temporary warp and no Warp Ward, it keeps an effect on them (level 1 to 4, rising every 15 warp) and, at the
 * original odds per tick, burns a point of temporary warp, calling {@link #onDrain} when it does.
 */
public abstract class WarpBauble extends Item implements ICurioItem {
    private final Holder<MobEffect> effect;
    private final int drainOdds;
    private final String description;

    protected WarpBauble(Properties properties, Holder<MobEffect> effect, int drainOdds, String description) {
        super(properties);
        this.effect = effect;
        this.drainOdds = drainOdds;
        this.description = description;
    }

    /** The warp the effect level is measured against. */
    protected abstract int levelWarp(IPlayerWarp warp);

    /** Called after a point of temporary warp is burned. */
    protected void onDrain(ServerPlayer player) {
    }

    /** Extra behaviour each tick, before the warp handling. */
    protected void wornTick(ServerPlayer player) {
    }

    @Override
    public void curioTick(SlotContext slot, ItemStack stack) {
        if (!(slot.entity() instanceof ServerPlayer player)) {
            return;
        }
        this.wornTick(player);
        IPlayerWarp warp = WarpHelper.getWarp(player);
        if (warp.get(WarpType.TEMPORARY) <= 0 || player.hasEffect(TTMobEffects.WARP_WARD)) {
            return;
        }
        int level = Math.min(3, this.levelWarp(warp) / 15);
        player.addEffect(new MobEffectInstance(this.effect, 60, level, true, true));
        // The original rolled nextInt() % odds == 0, which counts negative rolls too: about 1 in odds per tick.
        if (player.getRandom().nextInt(this.drainOdds) == 0) {
            WarpHelper.addWarp(player, -1, WarpType.TEMPORARY);
            this.onDrain(player);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(this.description).withStyle(ChatFormatting.GOLD));
    }
}
