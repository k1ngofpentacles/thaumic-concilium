package dev.movingcastle.thaumicconcilium.menu;

import dev.movingcastle.thaumicconcilium.entity.mob.ThaumaturgeEntity;
import dev.movingcastle.thaumicconcilium.entity.mob.ThaumaturgeTrades;
import dev.movingcastle.thaumicconcilium.registry.TCMenus;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Thaumaturge's trade table (original: {@code ContainerThaumaturge}, the Pech layout): one offer slot, four payout
 * slots, and a button that lays out goods for one offered item while the payout slots are empty.
 */
public final class ThaumaturgeMenu extends AbstractContainerMenu {
    public static final int TRADE_BUTTON = 0;
    @Nullable
    private final ThaumaturgeEntity thaumaturge;
    private final SimpleContainer table = new SimpleContainer(5);

    public ThaumaturgeMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, inventory.player.level().getEntity(buf.readVarInt()) instanceof ThaumaturgeEntity t ? t : null);
    }

    public ThaumaturgeMenu(int id, Inventory inventory, @Nullable ThaumaturgeEntity thaumaturge) {
        super(TCMenus.THAUMATURGE.get(), id);
        this.thaumaturge = thaumaturge;
        this.addSlot(new Slot(this.table, 0, 36, 29));
        for (int i = 0; i < 4; i++) {
            this.addSlot(new Payout(this.table, 1 + i, 106 + 18 * (i % 2), 20 + 18 * (i / 2)));
        }
        for (int i = 9; i < 36; i++) {
            this.addSlot(new Slot(inventory, i, 8 + 18 * (i % 9), 84 + 18 * (i / 9 - 1)));
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inventory, i, 8 + 18 * i, 142));
        }
    }

    public boolean canTrade() {
        ItemStack offer = this.table.getItem(0);
        return this.thaumaturge != null && ThaumaturgeEntity.isValued(offer)
                && IntStream.rangeClosed(1, 4).allMatch(i -> this.table.getItem(i).isEmpty());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != TRADE_BUTTON) {
            return super.clickMenuButton(player, id);
        }
        if (!player.level().isClientSide() && this.canTrade()) {
            boolean[] hinted = {false};
            for (ItemStack payout : ThaumaturgeTrades.haggle(this.table.getItem(0), player.getRandom(),
                    player.level().registryAccess(), hinted)) {
                if (!this.shelve(payout)) {
                    player.getInventory().placeItemBackInInventory(payout);
                }
            }
            this.table.removeItem(0, 1);
            if (hinted[0]) {
                player.sendSystemMessage(Component.translatable("thaumicconcilium.thaumaturge.trade." + (player.getRandom().nextInt(3) + 1))
                        .withStyle(ChatFormatting.DARK_PURPLE));
            }
        }
        return true;
    }

    private boolean shelve(ItemStack stack) {
        for (int i = 1; i <= 4; i++) {
            ItemStack shelved = this.table.getItem(i);
            if (shelved.isEmpty()) {
                this.table.setItem(i, stack);
                return true;
            }
            if (ItemStack.isSameItemSameComponents(shelved, stack) && shelved.getCount() + stack.getCount() <= shelved.getMaxStackSize()) {
                shelved.grow(stack.getCount());
                return true;
            }
        }
        return false;
    }

    /** Original: open only while the Thaumaturge isn't angry. */
    @Override
    public boolean stillValid(Player player) {
        return this.thaumaturge != null && this.thaumaturge.isAlive() && this.thaumaturge.anger() == 0
                && player.canInteractWithEntity(this.thaumaturge, 4.0);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            this.clearContainer(player, this.table);
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
        boolean moved = index < 5 ? this.moveItemStackTo(moving, 5, 41, true) : this.moveItemStackTo(moving, 0, 1, true);
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

    /** Take-only payout slot. */
    private static final class Payout extends Slot {
        Payout(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
