package dev.movingcastle.thaumicconcilium.client;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.menu.AstralMonitorMenu;
import dev.movingcastle.thaumicconcilium.menu.LithographerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** The Astral Monitor and Lithographer screens (original: AstralMonitorGUI, LithographerGUI): a texture and no labels. */
public final class SlotScreens {
    private SlotScreens() {
    }

    private abstract static class Plain<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
        private final ResourceLocation texture;

        Plain(T menu, Inventory inventory, Component title, String texture) {
            super(menu, inventory, title);
            this.texture = ThaumicConcilium.id("textures/gui/" + texture + ".png");
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.render(graphics, mouseX, mouseY, partialTick);
            this.renderTooltip(graphics, mouseX, mouseY);
        }

        @Override
        protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
            graphics.blit(this.texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        }

        @Override
        protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        }
    }

    public static final class Monitor extends Plain<AstralMonitorMenu> {
        public Monitor(AstralMonitorMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title, "astral_monitor");
        }
    }

    public static final class Lithographer extends Plain<LithographerMenu> {
        public Lithographer(LithographerMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title, "lithographer");
        }
    }
}
