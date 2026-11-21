package dev.movingcastle.thaumicconcilium.client;

import com.leclowndu93150.thaumaturge.registry.TTSounds;
import dev.movingcastle.thaumicconcilium.menu.ThaumaturgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Original ThaumaturgeGUI: Thaumcraft's Pech trade screen, with the dice button lit while a valued item waits and the
 * payout slots are empty. The background is Thaumaturge's own Pech texture, used from its jar at runtime (not shipped).
 */
public final class ThaumaturgeScreen extends AbstractContainerScreen<ThaumaturgeMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumaturge", "textures/gui/gui_pech.png");

    public ThaumaturgeScreen(ThaumaturgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 175;
        this.imageHeight = 232;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        if (this.menu.canTrade()) {
            graphics.blit(TEXTURE, this.leftPos + 67, this.topPos + 24, 176, 0, 25, 25);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX - (this.leftPos + 67);
        int my = (int) mouseY - (this.topPos + 24);
        if (mx >= 0 && my >= 0 && mx < 25 && my < 25 && this.menu.canTrade()) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, ThaumaturgeMenu.TRADE_BUTTON);
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(TTSounds.PECH_DICE.get(),
                    0.95F + this.minecraft.player.getRandom().nextFloat() * 0.1F, 0.5F));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
