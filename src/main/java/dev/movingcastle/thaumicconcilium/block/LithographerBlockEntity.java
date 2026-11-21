package dev.movingcastle.thaumicconcilium.block;

import com.leclowndu93150.thaumaturge.registry.TTSounds;
import dev.movingcastle.thaumicconcilium.item.AstralMonitor;
import dev.movingcastle.thaumicconcilium.lithograph.Lithograph;
import dev.movingcastle.thaumicconcilium.network.LithographerZapPayload;
import dev.movingcastle.thaumicconcilium.registry.TCBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Lithographer's state (original: {@code LithographerTile}): four monitor slots and the projection animation.
 * <ul>
 *   <li>{@code state} 0 at rest; 1 deploying (20 ticks); 2 projecting (20 ticks); 3 retracting (20 ticks);</li>
 *   <li>{@code deploy} the countdown within a state (negative while retracting), which the renderer reads.</li>
 * </ul>
 * At the end of state 1 the machine operates on whoever sleeps in the bed below it ({@link #operate}). It only ticks
 * while it is moving.
 */
public class LithographerBlockEntity extends BlockEntity implements Container {
    public static final int SLOTS = 4;
    public static final int CRYSTAL_COST = 32;
    private static final ResourceLocation SENSUS = ResourceLocation.fromNamespaceAndPath("thaumaturge", "sensus");

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private int deploy;
    private int state;

    public LithographerBlockEntity(BlockPos pos, BlockState blockState) {
        super(TCBlocks.LITHOGRAPHER_BLOCK_ENTITY.get(), pos, blockState);
    }

    public int deploy() {
        return this.deploy;
    }

    public int state() {
        return this.state;
    }

    public boolean idle() {
        return this.deploy == 0 && this.state == 0;
    }

    /** Original {@code project()}: start deploying. */
    public void project() {
        if (this.level != null && !this.level.isClientSide()) {
            this.deploy = 20;
            this.state = 1;
            this.sync();
        }
    }

    private void sync() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState blockState, LithographerBlockEntity be) {
        if (be.state == 0 && be.deploy == 0) {
            return;
        }
        if (be.deploy > 0) {
            be.deploy--;
        } else if (be.deploy < 0) {
            be.deploy++;
        }
        if (be.state == 1 && be.deploy == 0) {
            be.deploy = 20;
            be.state = 2;
            if (!level.isClientSide()) {
                be.sync();
                be.operate((ServerLevel) level);
            }
        } else if (be.state == 2 && be.deploy == 0) {
            be.deploy = -20;
            be.state = 3;
            if (!level.isClientSide()) {
                be.sync();
            }
        } else if (be.state == 3 && be.deploy == 0) {
            be.state = 0;
            if (!level.isClientSide()) {
                be.sync();
            }
        }
    }

    /**
     * Original operation, at the end of the deploy: a Vis Condenser directly above, a destabilized crystal of sensus
     * (32 or more) above that, the head of a bed below, and a sleeping player within two blocks of it. The player is
     * lithographed with the four monitored targets and the crystal pays 32.
     */
    private void operate(ServerLevel level) {
        BlockPos pos = this.worldPosition;
        if (!level.getBlockState(pos.above()).is(TCBlocks.VIS_CONDENSER.get())
                || !(level.getBlockEntity(pos.above(2)) instanceof DestabilizedCrystalBlockEntity crystal)
                || !SENSUS.equals(crystal.aspect()) || crystal.capacity() < CRYSTAL_COST) {
            return;
        }
        BlockPos bedPos = pos.below();
        BlockState bed = level.getBlockState(bedPos);
        if (!(bed.getBlock() instanceof BedBlock) || bed.getValue(BedBlock.PART) != BedPart.HEAD) {
            return;
        }
        ServerPlayer sleeper = null;
        double best = 4.0;
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(bedPos).inflate(2.0), ServerPlayer::isSleeping)) {
            double d = p.distanceToSqr(bedPos.getX() + 0.5, bedPos.getY() + 0.5, bedPos.getZ() + 0.5);
            if (d <= best) {
                best = d;
                sleeper = p;
            }
        }
        if (sleeper == null) {
            return;
        }
        List<String> targets = new ArrayList<>(SLOTS);
        for (ItemStack monitor : this.items) {
            // Empty slots watch nobody (original: a placeholder string).
            Optional<UUID> id = monitor.getItem() instanceof AstralMonitor ? AstralMonitor.target(monitor) : Optional.empty();
            targets.add(id.map(Lithograph::playerTarget).orElse(""));
        }
        Lithograph.State old = Lithograph.get(sleeper);
        Lithograph.set(sleeper, new Lithograph.State(true, old.relieved(), targets));
        crystal.setCapacity(crystal.capacity() - CRYSTAL_COST);
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, TTSounds.ZAP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        PacketDistributor.sendToPlayersNear(level, null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 32.0, new LithographerZapPayload(pos));
    }

    // ---------------------------------------------------------------- inventory
    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack out = ContainerHelper.removeItem(this.items, slot, amount);
        if (!out.isEmpty()) {
            this.setChanged();
        }
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
        this.setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return AstralMonitor.watching(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    // ---------------------------------------------------------------- saving
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("deploy", this.deploy);
        tag.putInt("state", this.state);
        ContainerHelper.saveAllItems(tag, this.items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.deploy = tag.getInt("deploy");
        this.state = tag.getInt("state");
        this.items.clear();
        ContainerHelper.loadAllItems(tag, this.items, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
