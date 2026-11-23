package dev.movingcastle.thaumicconcilium.world;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Phase 5c: contributors who were offline when a hex broke (kept in the overworld's data so it is found whatever dimension they log into).
 * Each owes temporary warp in proportion to their share of the burden, paid on their next login (the warp event follows at once).
 * An entry stays after it is paid, with {@code paid} set: cycle 2's fight counts a participant who joins later as present, and reads
 * {@code hex}, {@code dimension} and {@code share} for that.
 */
public final class VesselDebts extends SavedData {
    public static final String NAME = "thaumicconcilium_vessel_debts";

    /** One absent contributor: warp owed, their share of the burden (0 to 1), whether it has been paid, and where the break happened. */
    public static final class Debt {
        public int warp;
        public float share;
        public boolean paid;
        public String dimension = "";
        public BlockPos hex = BlockPos.ZERO;
    }

    private final Map<UUID, Debt> debts = new LinkedHashMap<>();
    /** Phase 5c cycle 4: Primordial Motes owed to participants who were offline when the Vessel was subdued (paid on their next login). */
    private final Map<UUID, Integer> motes = new LinkedHashMap<>();
    private final Map<UUID, Integer> warpRemovals = new LinkedHashMap<>();

    public static SavedData.Factory<VesselDebts> factory() {
        return new SavedData.Factory<>(VesselDebts::new, VesselDebts::load, null);
    }

    public static VesselDebts get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), NAME);
    }

    public Map<UUID, Debt> debts() {
        return this.debts;
    }

    /** Records (or adds to) what an absent player owes. */
    public void owe(UUID id, int warp, float share, String dimension, BlockPos hex) {
        Debt d = this.debts.computeIfAbsent(id, k -> new Debt());
        d.warp += warp;
        d.share += share;
        d.paid = false;
        d.dimension = dimension;
        d.hex = hex;
        this.setDirty();
    }

    /** Motes owed per player (unpaid only; an entry is removed when paid). */
    public Map<UUID, Integer> motes() {
        return this.motes;
    }

    public void oweMotes(UUID id, int count) {
        this.motes.merge(id, count, Integer::sum);
        this.setDirty();
    }

    /** Takes and clears what a player is owed. */
    public int takeMotes(UUID id) {
        Integer n = this.motes.remove(id);
        if (n != null) {
            this.setDirty();
        }
        return n == null ? 0 : n;
    }

    /** Phase 5d: permanent warp to remove from participants who were offline at the ending (applied on their next login). */
    public Map<UUID, Integer> warpRemovals() {
        return this.warpRemovals;
    }

    public void queueWarpRemoval(UUID id, int amount) {
        if (amount > 0) {
            this.warpRemovals.merge(id, amount, Integer::sum);
            this.setDirty();
        }
    }

    /** Takes and clears the permanent warp still to be removed from a player. */
    public int takeWarpRemoval(UUID id) {
        Integer n = this.warpRemovals.remove(id);
        if (n != null) {
            this.setDirty();
        }
        return n == null ? 0 : n;
    }

    private static VesselDebts load(CompoundTag tag, HolderLookup.Provider registries) {
        VesselDebts data = new VesselDebts();
        ListTag list = tag.getList("debts", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            Debt d = new Debt();
            d.warp = c.getInt("warp");
            d.share = c.getFloat("share");
            d.paid = c.getBoolean("paid");
            d.dimension = c.getString("dimension");
            d.hex = BlockPos.of(c.getLong("hex"));
            data.debts.put(c.getUUID("id"), d);
        }
        ListTag owed = tag.getList("motes", 10);
        for (int i = 0; i < owed.size(); i++) {
            CompoundTag c = owed.getCompound(i);
            data.motes.put(c.getUUID("id"), c.getInt("n"));
        }
        ListTag removals = tag.getList("warpRemovals", 10);
        for (int i = 0; i < removals.size(); i++) {
            CompoundTag c = removals.getCompound(i);
            data.warpRemovals.put(c.getUUID("id"), c.getInt("n"));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        this.debts.forEach((id, d) -> {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", id);
            c.putInt("warp", d.warp);
            c.putFloat("share", d.share);
            c.putBoolean("paid", d.paid);
            c.putString("dimension", d.dimension);
            c.putLong("hex", d.hex.asLong());
            list.add(c);
        });
        tag.put("debts", list);
        ListTag owed = new ListTag();
        this.motes.forEach((id, n) -> {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", id);
            c.putInt("n", n);
            owed.add(c);
        });
        tag.put("motes", owed);
        ListTag removals = new ListTag();
        this.warpRemovals.forEach((id, n) -> {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", id);
            c.putInt("n", n);
            removals.add(c);
        });
        tag.put("warpRemovals", removals);
        return tag;
    }
}
