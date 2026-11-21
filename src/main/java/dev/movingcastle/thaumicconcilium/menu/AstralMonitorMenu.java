package dev.movingcastle.thaumicconcilium.menu;

import dev.movingcastle.thaumicconcilium.item.AstralMonitor;
import dev.movingcastle.thaumicconcilium.registry.TCDataComponents;
import dev.movingcastle.thaumicconcilium.registry.TCMenus;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Astral Monitor's screen (original: {@code AstralMonitorContainer}): one slot that takes a xylographed wand, over
 * the player's inventory. The monitor itself, in the selected hotbar slot, can't be moved while the screen is open.
 * Unlike the original, which wrote the wand back when the screen closed, the wand is stored in the monitor the moment
 * the slot changes, so nothing is lost if the screen is closed by a disconnect.
 */
public final class AstralMonitorMenu extends AbstractContainerMenu {
    private final Inventory inventory;
    private final int monitorHotbar;
    /** Menu index of the locked hotbar slot (original: blockSlot). */
    private final int lockedIndex;
    private final SimpleContainer input = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            AstralMonitorMenu.this.store();
        }
    };
    private boolean loading = true;

    public AstralMonitorMenu(int id, Inventory inventory) {
        super(TCMenus.ASTRAL_MONITOR.get(), id);
        this.inventory = inventory;
        this.monitorHotbar = inventory.selected;
        this.lockedIndex = 28 + this.monitorHotbar;
        this.addSlot(new Slot(this.input, 0, 78, 30) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AstralMonitor.fits(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            if (col == this.monitorHotbar) {
                this.addSlot(new Slot(inventory, col, 8 + col * 18, 142) {
                    @Override
                    public boolean mayPickup(Player player) {
                        return false;
                    }

                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            } else {
                this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
            }
        }
        ItemStack wand = AstralMonitor.wand(this.monitor());
        if (!wand.isEmpty()) {
            this.input.setItem(0, wand.copy());
        }
        this.loading = false;
    }

    private ItemStack monitor() {
        return this.inventory.getItem(this.monitorHotbar);
    }

    /** Writes the slot into the monitor's component (server only; the client gets the stack through slot sync). */
    private void store() {
        if (this.loading || this.inventory.player.level().isClientSide()) {
            return;
        }
        ItemStack monitor = this.monitor();
        if (!(monitor.getItem() instanceof AstralMonitor)) {
            return;
        }
        ItemStack wand = this.input.getItem(0);
        if (wand.isEmpty()) {
            monitor.remove(TCDataComponents.MONITOR_WAND.get());
        } else {
            monitor.set(TCDataComponents.MONITOR_WAND.get(), wand.copy());
        }
        // The reading belongs to whoever the old wand named; the next pass writes a fresh one.
        monitor.remove(TCDataComponents.MONITOR_STATS.get());
        this.inventory.setChanged();
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (slotId == this.lockedIndex || (type == ClickType.SWAP && button == this.monitorHotbar)) {
            return;
        }
        super.clicked(slotId, button, type, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index == this.lockedIndex) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack moving = slot.getItem();
        ItemStack original = moving.copy();
        boolean moved = index == 0 ? this.moveItemStackTo(moving, 1, this.slots.size(), true) : this.moveItemStackTo(moving, 0, 1, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (moving.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (moving.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, moving);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.monitor().getItem() instanceof AstralMonitor && player.getInventory().selected == this.monitorHotbar;
    }
}
