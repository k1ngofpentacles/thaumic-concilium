package dev.movingcastle.thaumicconcilium.block;

import dev.movingcastle.thaumicconcilium.registry.TCBlocks;
import dev.movingcastle.thaumicconcilium.rift.Hexes;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One block of a Hex of Predictability (original: {@code HexOfPredictabilityTile}). The centre (master) holds the
 * essentia the Vis Condensers send, the heat that times a transmutation, and whether a rift is held; the eight around
 * it only remember the block they replaced. A lesser hex (port addition) is the same ring around any centre.
 */
public class HexBlockEntity extends BlockEntity {
    private boolean master;
    private boolean lesser;
    private boolean hasRift;
    private int heat;
    private final Map<ResourceLocation, Integer> essentia = new LinkedHashMap<>();
    /** The block this one replaced (bedrock or the lesser hex's centre, ancient stone for the ring), restored on break. */
    private BlockState replaced = Blocks.AIR.defaultBlockState();

    public HexBlockEntity(BlockPos pos, BlockState state) {
        super(TCBlocks.HEX_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HexBlockEntity hex) {
        if (hex.master && !hex.inert && level instanceof ServerLevel server) {
            Hexes.tickMaster(server, pos, hex);
        }
    }

    // ---------------------------------------------------------------- phase 5c: the sink (burden, strain, leak)
    private int burden;
    private float carry;
    /** Per-player contribution to the burden (players who gave it; routes with no contributor add to the burden only). */
    private final Map<java.util.UUID, Integer> contributions = new LinkedHashMap<>();
    private boolean spreadRolled;
    private float thresholdSpread;
    private float leakSpread;
    private float leaked;
    /** Not saved: set when the block entity loads already strained, acted on by the next master tick. */
    private boolean breakPending;
    /** Whether this hex holds a TAMED rift (set by the 60-tick check): the sink routes, growth, strain and break only apply then. */
    private boolean sinkActive;

    /**
     * Whether this hex is a sink. Only a lesser hex (a Barrier Stone at its centre) ever is (user, 2026-11-22): a full hex tames a
     * native rift as before phase 5c but never feeds, grows, strains, cracks or breaks.
     */
    public boolean sinkActive() {
        return this.sinkActive && this.lesser;
    }

    public void setSinkActive(boolean active) {
        if (this.sinkActive != active) {
            this.sinkActive = active;
            this.setChanged();
        }
    }

    public int burden() {
        return this.burden;
    }

    public Map<java.util.UUID, Integer> contributions() {
        return this.contributions;
    }

    public boolean spreadRolled() {
        return this.spreadRolled;
    }

    /** Rolls the hidden spreads (fractions, +/-) once; later calls do nothing. */
    public void rollSpreads(net.minecraft.util.RandomSource random) {
        if (!this.spreadRolled) {
            this.thresholdSpread = (float) dev.movingcastle.thaumicconcilium.rift.HexSink.rollSpread(random,
                    dev.movingcastle.thaumicconcilium.TCConfig.VESSEL_THRESHOLD_SPREAD.get());
            this.leakSpread = (float) dev.movingcastle.thaumicconcilium.rift.HexSink.rollSpread(random,
                    dev.movingcastle.thaumicconcilium.TCConfig.VESSEL_LEAK_SPREAD.get());
            this.spreadRolled = true;
            this.sync();
        }
    }

    /** The strain threshold of this hex (the config's T3 with this hex's spread). */
    public int threshold() {
        return dev.movingcastle.thaumicconcilium.rift.HexSink.threshold(dev.movingcastle.thaumicconcilium.TCConfig.VESSEL_THRESHOLD.get(),
                this.thresholdSpread);
    }

    public int leakThreshold() {
        return dev.movingcastle.thaumicconcilium.rift.HexSink.threshold(dev.movingcastle.thaumicconcilium.TCConfig.VESSEL_LEAK_THRESHOLD.get(),
                this.leakSpread);
    }

    public boolean strained() {
        return this.spreadRolled && this.burden >= this.threshold();
    }

    public float leaked() {
        return this.leaked;
    }

    public void addLeaked(float flux) {
        this.leaked += flux;
        this.setChanged();
    }

    /** Adds {@code raw} burden (fractions carry over); returns the whole burden added. */
    public int addBurden(double raw, @Nullable java.util.UUID who) {
        double total = this.carry + Math.max(0.0, raw);
        int whole = (int) Math.floor(total);
        this.carry = (float) (total - whole);
        if (whole > 0) {
            this.burden += whole;
            if (who != null) {
                this.contributions.merge(who, whole, Integer::sum);
            }
        }
        this.sync();
        return whole;
    }

    public boolean breakPending() {
        return this.breakPending;
    }

    public void setBreakPending(boolean pending) {
        this.breakPending = pending;
    }

    /**
     * Notes that this block entity was loaded from saved data (a chunk load or a server start). A strained master that loads breaks on its
     * next tick. Not {@code onLoad}: Minecraft calls that again whenever a block state change rebinds the ticker (the cracks do that).
     */
    public void noteLoaded() {
        this.breakPending = this.master && this.lesser && !this.inert && this.strained();
    }

    /**
     * Phase 5c cycle 3c: a hex that has broken and is only being shown (its blocks ride a Sable sub-level through the break
     * cinematic). An inert hex never ticks, never breaks again and is not unmade when its blocks go. Saved, so the copy Sable makes keeps it.
     */
    private boolean inert;

    public boolean inert() {
        return this.inert;
    }

    public void setInert(boolean inert) {
        this.inert = inert;
        this.setChanged();
    }

    public void setup(boolean master, boolean lesser, BlockState replaced) {
        this.master = master;
        this.lesser = lesser;
        this.replaced = replaced;
        this.sync();
    }

    public boolean isMaster() {
        return this.master;
    }

    public boolean isLesser() {
        return this.lesser;
    }

    public boolean hasRift() {
        return this.hasRift;
    }

    public void setHasRift(boolean hasRift) {
        if (this.hasRift != hasRift) {
            this.hasRift = hasRift;
            if (!hasRift) {
                this.essentia.clear();
                this.heat = 0;
            }
            this.sync();
        }
    }

    public int heat() {
        return this.heat;
    }

    public void setHeat(int heat) {
        this.heat = Math.max(0, heat);
        this.setChanged();
    }

    public Map<ResourceLocation, Integer> essentia() {
        return this.essentia;
    }

    public void addEssentia(ResourceLocation aspect, int amount) {
        this.essentia.merge(aspect, amount, Integer::sum);
        this.sync();
        // Phase 5c, route F2: vitium is also burden (no owner is stored on a condenser, so no contributor).
        if (this.master && this.level instanceof ServerLevel server) {
            dev.movingcastle.thaumicconcilium.rift.HexSink.feedEssentia(server, this.worldPosition, this, aspect, amount);
        }
    }

    public void clearEssentia() {
        this.essentia.clear();
        this.sync();
    }

    public BlockState replaced() {
        return this.replaced;
    }

    public void sync() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("master", this.master);
        tag.putBoolean("lesser", this.lesser);
        tag.putBoolean("hasRift", this.hasRift);
        tag.putInt("heat", this.heat);
        CompoundTag ess = new CompoundTag();
        this.essentia.forEach((k, v) -> ess.putInt(k.toString(), v));
        tag.put("essentia", ess);
        tag.put("replaced", NbtUtils.writeBlockState(this.replaced));
        tag.putInt("burden", this.burden);
        tag.putFloat("carry", this.carry);
        tag.putBoolean("spreadRolled", this.spreadRolled);
        tag.putFloat("thresholdSpread", this.thresholdSpread);
        tag.putFloat("leakSpread", this.leakSpread);
        tag.putFloat("leaked", this.leaked);
        tag.putBoolean("sinkActive", this.sinkActive);
        if (this.inert) {
            tag.putBoolean("inert", true);
        }
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        this.contributions.forEach((id, n) -> {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", id);
            c.putInt("n", n);
            list.add(c);
        });
        tag.put("contributions", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.master = tag.getBoolean("master");
        this.lesser = tag.getBoolean("lesser");
        this.hasRift = tag.getBoolean("hasRift");
        this.heat = tag.getInt("heat");
        this.essentia.clear();
        CompoundTag ess = tag.getCompound("essentia");
        for (String k : ess.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(k);
            if (id != null) {
                this.essentia.put(id, ess.getInt(k));
            }
        }
        this.replaced = NbtUtils.readBlockState(registries.lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK), tag.getCompound("replaced"));
        this.burden = tag.getInt("burden");
        this.inert = tag.getBoolean("inert");
        this.carry = tag.getFloat("carry");
        this.spreadRolled = tag.getBoolean("spreadRolled");
        this.thresholdSpread = tag.getFloat("thresholdSpread");
        this.leakSpread = tag.getFloat("leakSpread");
        this.leaked = tag.getFloat("leaked");
        this.sinkActive = tag.getBoolean("sinkActive");
        this.contributions.clear();
        net.minecraft.nbt.ListTag list = tag.getList("contributions", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            this.contributions.put(c.getUUID("id"), c.getInt("n"));
        }
        this.noteLoaded();
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
