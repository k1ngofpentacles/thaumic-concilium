package dev.movingcastle.thaumicconcilium.registry;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Every sound the original shipped, under the same names. */
public final class TCSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ThaumicConcilium.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> THUMP = register("thump");
    public static final DeferredHolder<SoundEvent, SoundEvent> STAB = register("stab");
    public static final DeferredHolder<SoundEvent, SoundEvent> PULL = register("pull");
    public static final DeferredHolder<SoundEvent, SoundEvent> IRA = register("ira");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHISPERS = register("whispers");
    public static final DeferredHolder<SoundEvent, SoundEvent> HUM = register("hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUBBLES = register("bubbles");
    public static final DeferredHolder<SoundEvent, SoundEvent> MILL = register("mill");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHARD_DROP = register("shard_drop");
    public static final DeferredHolder<SoundEvent, SoundEvent> GNAW = register("gnaw");
    public static final DeferredHolder<SoundEvent, SoundEvent> MELTED = register("melted");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAD = register("sad");
    public static final DeferredHolder<SoundEvent, SoundEvent> CLACK = register("clack");
    public static final DeferredHolder<SoundEvent, SoundEvent> UAGH = register("uagh");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPELL = register("spell");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPEECH = register("speech");
    public static final DeferredHolder<SoundEvent, SoundEvent> THINK = register("think");
    public static final DeferredHolder<SoundEvent, SoundEvent> GROWL = register("growl");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHANT = register("chant");
    public static final DeferredHolder<SoundEvent, SoundEvent> GARGLE_MANY = register("gargle_many");
    public static final DeferredHolder<SoundEvent, SoundEvent> GARGLE_ONE = register("gargle_one");
    public static final DeferredHolder<SoundEvent, SoundEvent> GARGLE_PAIN = register("gargle_pain");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHACKLES = register("shackles");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHACKLES_THROW = register("shackles_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> BREATH = register("breath");

    /** Phase 5c: the Vessel's own events (vessel.hum ...), each pointing at a vanilla or Thaumaturge event in sounds.json; nothing is shipped. */
    public static final java.util.Map<String, DeferredHolder<SoundEvent, SoundEvent>> VESSEL = new java.util.LinkedHashMap<>();

    static {
        for (String n : new String[] {"hum", "wake", "swell", "gulp", "clot_windup", "clot_launch", "bat", "stagger", "recover", "tether_form",
                "tether_break", "mass_drop", "horizon_throw", "eat", "anchor_cool", "anchor_ready", "subdued"}) {
            VESSEL.put(n, register("vessel." + n));
        }
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ThaumicConcilium.id(name)));
    }

    private TCSounds() {
    }
}
