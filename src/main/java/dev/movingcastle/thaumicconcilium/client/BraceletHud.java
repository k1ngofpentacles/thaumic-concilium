package dev.movingcastle.thaumicconcilium.client;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.wands.WandVisHelper;
import dev.movingcastle.thaumicconcilium.item.CastingBracelet;
import dev.movingcastle.thaumicconcilium.spell.BraceletCasting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;

/**
 * While a bracelet is worn and no wand is held: its socketed focus beside the hotbar, darkening with the cooldown, and
 * one thin bar per primal showing its stored vis. (Thaumaturge's caster HUD only reads a held caster.)
 */
public final class BraceletHud implements LayeredDraw.Layer {
    private static final int BAR_HEIGHT = 16;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator() || !BraceletInput.braceletOwnsFocusKey(player)) {
            return;
        }
        ItemStack bracelet = BraceletCasting.worn(player);
        if (!(bracelet.getItem() instanceof CastingBracelet item)) {
            return;
        }
        // Right of the hotbar, clear of the offhand slot (which sits on the left for right-handed players).
        int x = graphics.guiWidth() / 2 + 91 + 8;
        int y = graphics.guiHeight() - 20;
        ItemStack focus = item.socketedFocus(bracelet);
        graphics.renderItem(focus.isEmpty() ? bracelet : focus, x, y);
        float cooldown = player.getCooldowns().getCooldownPercent(item, delta.getGameTimeDeltaPartialTick(false));
        if (cooldown > 0.0F) {
            int h = Math.round(16 * cooldown);
            graphics.fill(x, y + 16 - h, x + 16, y + 16, 0x80000000);
        }

        int max = Math.max(1, WandVisHelper.getMaxVis(bracelet));
        int bx = x + 19;
        int by = y;
        var aspects = player.level().registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY);
        for (ResourceKey<IAspect> primal : TTAspects.PRIMALS) {
            int fill = Math.round(BAR_HEIGHT * Math.min(1.0F, WandVisHelper.getVis(bracelet, primal) / (float) max));
            int color = aspects.get(primal).map(h -> h.value().color()).orElse(0xFFFFFF);
            graphics.fill(bx, by, bx + 2, by + BAR_HEIGHT, 0x60000000);
            graphics.fill(bx, by + BAR_HEIGHT - fill, bx + 2, by + BAR_HEIGHT, 0xFF000000 | color);
            bx += 3;
        }
    }
}
