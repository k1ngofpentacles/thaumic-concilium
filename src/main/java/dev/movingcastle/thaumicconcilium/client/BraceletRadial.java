package dev.movingcastle.thaumicconcilium.client;

import com.leclowndu93150.thaumaturge.client.screen.TTScreenTextures;
import com.leclowndu93150.thaumaturge.compat.curio.ThaumaturgeCuriosCompat;
import com.leclowndu93150.thaumaturge.content.casters.CasterManager;
import com.leclowndu93150.thaumaturge.content.casters.FocusPouchItem;
import com.leclowndu93150.thaumaturge.content.spell.item.FocusItems;
import com.mojang.math.Axis;
import dev.movingcastle.thaumicconcilium.item.CastingBracelet;
import dev.movingcastle.thaumicconcilium.network.BraceletFocusPayload;
import dev.movingcastle.thaumicconcilium.spell.BraceletCasting;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The worn bracelet's focus radial, modelled on Thaumaturge's own (which only reads a held caster): hold the
 * change-focus key, point at a focus from the inventory or a focus pouch, and let go (or click) to socket it; the
 * centre takes the current focus out. Sneak + the key removes it directly.
 */
public final class BraceletRadial implements LayeredDraw.Layer {
    private static final float BASE_RADIUS = 26.0F;
    private static final float RADIUS_PER_FOCUS = 3.0F;
    private static final int RING_TINT = 0xA0C0C0C8;
    private static final int HOVER = 10;
    private static boolean open;
    /** Sort key to focus stack, in Thaumaturge's order. */
    private static final TreeMap<String, ItemStack> FOCI = new TreeMap<>();
    @Nullable
    private static String hovered;
    private static boolean centreHovered;

    public static boolean isOpen() {
        return open;
    }

    static void open(Minecraft mc) {
        FOCI.clear();
        hovered = null;
        centreHovered = false;
        collect(mc);
        ItemStack bracelet = BraceletCasting.worn(mc.player);
        boolean socketed = bracelet.getItem() instanceof CastingBracelet b && FocusItems.isFocus(b.socketedFocus(bracelet));
        if (FOCI.isEmpty() && !socketed) {
            return;
        }
        open = true;
        mc.mouseHandler.releaseMouse();
    }

    /** Closes the radial; {@code select} sends whatever is under the mouse. */
    static void close(Minecraft mc, boolean select) {
        if (!open) {
            return;
        }
        open = false;
        if (select) {
            if (hovered != null) {
                PacketDistributor.sendToServer(new BraceletFocusPayload(hovered));
            } else if (centreHovered) {
                remove();
            }
        }
        if (mc.screen == null && mc.isWindowActive()) {
            mc.mouseHandler.grabMouse();
        }
    }

    static void remove() {
        PacketDistributor.sendToServer(new BraceletFocusPayload(CasterManager.REMOVE_FOCUS));
    }

    private static void collect(Minecraft mc) {
        for (ThaumaturgeCuriosCompat.CurioPouchRef ref
                : ThaumaturgeCuriosCompat.equippedPouches(mc.player, stack -> stack.getItem() instanceof FocusPouchItem)) {
            addPouch(ref.stack());
        }
        NonNullList<ItemStack> main = mc.player.getInventory().items;
        for (int slot = 0; slot < Math.min(36, main.size()); slot++) {
            ItemStack stack = main.get(slot);
            add(stack);
            if (stack.getItem() instanceof FocusPouchItem) {
                addPouch(stack);
            }
        }
    }

    private static void addPouch(ItemStack pouch) {
        for (ItemStack stack : FocusPouchItem.getInventory(pouch)) {
            add(stack);
        }
    }

    private static void add(ItemStack stack) {
        if (FocusItems.isFocus(stack)) {
            String key = FocusItems.sortKey(stack);
            if (key != null) {
                FOCI.putIfAbsent(key, stack.copy());
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!open || mc.player == null) {
            return;
        }
        if (mc.screen != null) {
            close(mc, false);
            return;
        }
        int cx = graphics.guiWidth() / 2;
        int cy = graphics.guiHeight() / 2;
        int mouseX = (int) (mc.mouseHandler.xpos() * graphics.guiWidth() / mc.getWindow().getScreenWidth());
        int mouseY = (int) (mc.mouseHandler.ypos() * graphics.guiHeight() / mc.getWindow().getScreenHeight());
        float radius = BASE_RADIUS + FOCI.size() * RADIUS_PER_FOCUS;
        float time = mc.player.tickCount + delta.getGameTimeDeltaPartialTick(false);
        ring(graphics, TTScreenTextures.RADIAL_INNER, cx, cy, time / 640.0F * 360.0F, radius * 4.2F);
        ring(graphics, TTScreenTextures.RADIAL_OUTER, cx, cy, -time / 900.0F * 360.0F, radius * 4.4F);

        ItemStack tooltip = ItemStack.EMPTY;
        ItemStack bracelet = BraceletCasting.worn(mc.player);
        ItemStack socketed = bracelet.getItem() instanceof CastingBracelet b ? b.socketedFocus(bracelet) : ItemStack.EMPTY;
        centreHovered = false;
        if (FocusItems.isFocus(socketed)) {
            graphics.renderItem(socketed, cx - 8, cy - 8);
            if (Math.abs(mouseX - cx) <= HOVER && Math.abs(mouseY - cy) <= HOVER) {
                centreHovered = true;
                tooltip = socketed;
            }
        }

        hovered = null;
        List<String> keys = new ArrayList<>(FOCI.keySet());
        float slice = keys.isEmpty() ? 0.0F : 360.0F / keys.size();
        for (int i = 0; i < keys.size(); i++) {
            float angle = (-90.0F + i * slice) * Mth.DEG_TO_RAD;
            int x = Mth.floor(cx + Mth.cos(angle) * radius);
            int y = Mth.floor(cy + Mth.sin(angle) * radius);
            ItemStack stack = FOCI.get(keys.get(i));
            boolean over = Math.abs(mouseX - x) <= HOVER && Math.abs(mouseY - y) <= HOVER;
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0.0F);
            if (over) {
                graphics.pose().scale(1.3F, 1.3F, 1.0F);
                hovered = keys.get(i);
                tooltip = stack;
            }
            graphics.renderItem(stack, -8, -8);
            graphics.pose().popPose();
        }
        if (!tooltip.isEmpty()) {
            graphics.renderTooltip(mc.font, tooltip, cx - 4, cy + 20);
        }
    }

    private static void ring(GuiGraphics graphics, ResourceLocation texture, int cx, int cy, float degrees, float size) {
        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(degrees - 90.0F));
        graphics.pose().translate(-size / 2.0F, -size / 2.0F, 0.0F);
        graphics.pose().scale(size / 256.0F, size / 256.0F, 1.0F);
        graphics.setColor((RING_TINT >> 16 & 0xFF) / 255.0F, (RING_TINT >> 8 & 0xFF) / 255.0F, (RING_TINT & 0xFF) / 255.0F,
                (RING_TINT >>> 24) / 255.0F);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        graphics.blit(texture, 0, 0, 0.0F, 0.0F, 256, 256, 256, 256);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.pose().popPose();
    }
}
