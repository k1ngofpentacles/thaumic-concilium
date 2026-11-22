package dev.movingcastle.thaumicconcilium.world;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * The Crimson Dagger's binding sessions of one level (D1 option C, The Offertory), saved with the level so a craft survives a
 * restart. One {@link Session} per infusion matrix. Logic is in {@link BindingSessions}; this class only holds and saves state.
 */
public final class BindingData extends SavedData {
    public static final String NAME = "thaumicconcilium_binding";

    /** Drain state: {@code NORMAL} 1 HP every 20 ticks, {@code PAUSED} none (a demand was met), {@code STARVED} every 10 (a demand lapsed). */
    public enum Mode { NORMAL, PAUSED, STARVED }

    /** One binding: the matrix, the bound player and the offertory's state. */
    public static final class Session {
        public final BlockPos matrix;
        public UUID player;
        /** Ticks run while the bound player was online and the matrix chunk loaded. */
        public int elapsed;
        /** Game time at opening (seeds the demands). */
        public final long started;
        /** Demands begun so far. */
        public int demands;
        /** Registry id of the open demand's item, or "" when no demand is open. */
        public String demand = "";
        /** {@code elapsed} at which the open demand lapses. */
        public int windowEnd;
        public Mode mode = Mode.NORMAL;
        public int drainTimer;
        /** Ticks the bound player has been offline (grace after a restart). */
        public int away;
        /** Ticks left of a Yank's telegraph (the chain glows); 0 when none is coming. */
        public int telegraph;
        /** Game time until which a Yank's shackles hold the binder (not saved: the shackles themselves are). */
        public long yankedUntil;
        /** The pedestals that held an item when the binding opened (position to item id): the ring, never eaten as offerings. */
        public final Map<BlockPos, String> ring = new LinkedHashMap<>();

        public Session(BlockPos matrix, UUID player, long started) {
            this.matrix = matrix;
            this.player = player;
            this.started = started;
        }

        /** Ticks between 1 HP drains in the current mode; 0 while paused. */
        public int drainInterval() {
            return switch (this.mode) {
                case NORMAL -> BindingSessions.DRAIN_EVERY;
                case STARVED -> BindingSessions.STARVED_DRAIN_EVERY;
                case PAUSED -> 0;
            };
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.put("Pos", NbtUtils.writeBlockPos(this.matrix));
            t.putUUID("Player", this.player);
            t.putInt("Elapsed", this.elapsed);
            t.putLong("Started", this.started);
            t.putInt("Demands", this.demands);
            t.putString("Demand", this.demand);
            t.putInt("WindowEnd", this.windowEnd);
            t.putInt("Mode", this.mode.ordinal());
            t.putInt("DrainTimer", this.drainTimer);
            t.putInt("Away", this.away);
            t.putInt("Telegraph", this.telegraph);
            ListTag rings = new ListTag();
            for (Map.Entry<BlockPos, String> e : this.ring.entrySet()) {
                CompoundTag r = new CompoundTag();
                r.put("Pos", NbtUtils.writeBlockPos(e.getKey()));
                r.putString("Item", e.getValue());
                rings.add(r);
            }
            t.put("Ring", rings);
            return t;
        }

        static Session load(CompoundTag t) {
            BlockPos pos = NbtUtils.readBlockPos(t, "Pos").orElse(BlockPos.ZERO);
            Session s = new Session(pos, t.getUUID("Player"), t.getLong("Started"));
            s.elapsed = t.getInt("Elapsed");
            s.demands = t.getInt("Demands");
            s.demand = t.getString("Demand");
            s.windowEnd = t.getInt("WindowEnd");
            s.mode = Mode.values()[Math.floorMod(t.getInt("Mode"), Mode.values().length)];
            s.drainTimer = t.getInt("DrainTimer");
            s.away = t.getInt("Away");
            s.telegraph = t.getInt("Telegraph");
            ListTag rings = t.getList("Ring", Tag.TAG_COMPOUND);
            for (int i = 0; i < rings.size(); i++) {
                CompoundTag r = rings.getCompound(i);
                NbtUtils.readBlockPos(r, "Pos").ifPresent(p -> s.ring.put(p, r.getString("Item")));
            }
            return s;
        }
    }

    /** A hostile raid a snap opened (D1-c2): who bound it, who champions it, and the pedestal the dagger waits on. */
    public record Raid(UUID binder, UUID champion, BlockPos pedestal) {
        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putUUID("Binder", this.binder);
            t.putUUID("Champion", this.champion);
            t.put("Pos", NbtUtils.writeBlockPos(this.pedestal));
            return t;
        }

        static Raid load(CompoundTag t) {
            return new Raid(t.getUUID("Binder"), t.getUUID("Champion"), NbtUtils.readBlockPos(t, "Pos").orElse(BlockPos.ZERO));
        }
    }

    private final Map<BlockPos, Session> sessions = new LinkedHashMap<>();
    private final Map<UUID, Raid> raids = new LinkedHashMap<>();   // by champion

    public static SavedData.Factory<BindingData> factory() {
        return new SavedData.Factory<>(BindingData::new, BindingData::load, null);
    }

    private static BindingData load(CompoundTag tag, HolderLookup.Provider registries) {
        BindingData data = new BindingData();
        ListTag list = tag.getList("Sessions", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Session s = Session.load(list.getCompound(i));
            data.sessions.put(s.matrix, s);
        }
        ListTag raids = tag.getList("Raids", Tag.TAG_COMPOUND);
        for (int i = 0; i < raids.size(); i++) {
            Raid r = Raid.load(raids.getCompound(i));
            data.raids.put(r.champion(), r);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Session s : this.sessions.values()) {
            list.add(s.save());
        }
        tag.put("Sessions", list);
        ListTag raidList = new ListTag();
        for (Raid r : this.raids.values()) {
            raidList.add(r.save());
        }
        tag.put("Raids", raidList);
        return tag;
    }

    public Map<UUID, Raid> raids() {
        return this.raids;
    }

    public void putRaid(Raid raid) {
        this.raids.put(raid.champion(), raid);
        this.setDirty();
    }

    public void removeRaid(UUID champion) {
        if (this.raids.remove(champion) != null) {
            this.setDirty();
        }
    }

    public Map<BlockPos, Session> sessions() {
        return this.sessions;
    }

    public Session get(BlockPos pos) {
        return this.sessions.get(pos);
    }

    public void put(Session s) {
        this.sessions.put(s.matrix, s);
        this.setDirty();
    }

    public void remove(BlockPos pos) {
        if (this.sessions.remove(pos) != null) {
            this.setDirty();
        }
    }
}
