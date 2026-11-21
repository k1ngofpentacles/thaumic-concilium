package dev.movingcastle.thaumicconcilium.menu;

import dev.movingcastle.thaumicconcilium.block.LithographerBlockEntity;
import dev.movingcastle.thaumicconcilium.item.AstralMonitor;
import dev.movingcastle.thaumicconcilium.registry.TCMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Lithographer's screen (original: {@code LithographerContainer}): four slots in a cross, each taking one Astral
 * Monitor that is watching somebody, over the player's inventory.
 */
public final class LithographerMenu extends AbstractContainerMenu {
    private static final int[][] SLOT_POSITIONS = {{52, 30}, {80, 2}, {108, 30}, {80, 58}};
    private final Container lithographer;

    public LithographerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, inventory.player.level().getBlockEntity(buf.readBlockPos()) instanceof LithographerBlockEntity be
                ? be : new SimpleContainer(LithographerBlockEntity.SLOTS));
    }

    public LithographerMenu(int id, Inventory inventory, Container lithographer) {
        super(TCMenus.LITHOGRAPHER.get(), id);
        this.lithographer = lithographer;
        checkContainerSize(lithographer, LithographerBlockEntity.SLOTS);
        lithographer.startOpen(inventory.player);
        for (int i = 0; i < LithographerBlockEntity.SLOTS; i++) {
            this.addSlot(new Slot(lithographer, i, SLOT_POSITIONS[i][0], SLOT_POSITIONS[i][1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return AstralMonitor.watching(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack moving = slot.getItem();
        ItemStack original = moving.copy();
        boolean moved = index < LithographerBlockEntity.SLOTS
                ? this.moveItemStackTo(moving, LithographerBlockEntity.SLOTS, this.slots.size(), true)
                : this.moveItemStackTo(moving, 0, LithographerBlockEntity.SLOTS, false);
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
        return this.lithographer.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.lithographer.stopOpen(player);
    }
}
