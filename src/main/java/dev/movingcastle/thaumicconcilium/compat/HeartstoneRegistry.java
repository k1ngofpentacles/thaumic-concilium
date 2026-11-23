package dev.movingcastle.thaumicconcilium.compat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Which players have carried which Heartstone (phase 5d): Heartstone id to the UUIDs seen with it, updated at login and when a capital is placed. It
 * lets a Heartstone partner be found while offline: the partner is the other UUID on a shared id. Saved with the overworld; no ticking. No
 * Heartstone type appears here (the ids are plain longs), so the class loads without the mod.
 */
public final class HeartstoneRegistry extends SavedData {
    public static final String NAME = "thaumicconcilium_heartstones";

    private final Map<Long, Set<UUID>> byId = new LinkedHashMap<>();

    public static SavedData.Factory<HeartstoneRegistry> factory() {
        return new SavedData.Factory<>(HeartstoneRegistry::new, HeartstoneRegistry::load, null);
    }

    public static HeartstoneRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), NAME);
    }

    /** Notes that {@code player} carries Heartstone {@code id}. */
    public void record(UUID player, long id) {
        if (this.byId.computeIfAbsent(id, k -> new LinkedHashSet<>()).add(player)) {
            this.setDirty();
        }
    }

    public Set<UUID> players(long id) {
        return this.byId.getOrDefault(id, Set.of());
    }

    /** Every Heartstone id this player has been seen with. */
    public List<Long> idsOf(UUID player) {
        List<Long> out = new ArrayList<>();
        this.byId.forEach((id, set) -> {
            if (set.contains(player)) {
                out.add(id);
            }
        });
        return out;
    }

    /** The other player on any Heartstone this player carries (the first found), or null. */
    @Nullable
    public UUID partnerOf(UUID player) {
        for (Map.Entry<Long, Set<UUID>> e : this.byId.entrySet()) {
            if (e.getValue().contains(player)) {
                for (UUID other : e.getValue()) {
                    if (!other.equals(player)) {
                        return other;
                    }
                }
            }
        }
        return null;
    }

    private static HeartstoneRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        HeartstoneRegistry data = new HeartstoneRegistry();
        ListTag list = tag.getList("stones", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            Set<UUID> set = new LinkedHashSet<>();
            for (net.minecraft.nbt.Tag n : c.getList("players", 11)) {
                set.add(net.minecraft.nbt.NbtUtils.loadUUID(n));
            }
            data.byId.put(c.getLong("id"), set);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        this.byId.forEach((id, set) -> {
            CompoundTag c = new CompoundTag();
            c.putLong("id", id);
            ListTag players = new ListTag();
            set.forEach(u -> players.add(net.minecraft.nbt.NbtUtils.createUUID(u)));
            c.put("players", players);
            list.add(c);
        });
        tag.put("stones", list);
        return tag;
    }
}
