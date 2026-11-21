package dev.movingcastle.thaumicconcilium;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config. Values mirror the original TCConfig where a setting still makes sense on Thaumaturge.
 * Focus upgrade IDs are gone: upgrades are now spell-part settings.
 */
public final class TCConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue IMPULSE_BREAKS_BLOCKS = B
            .comment("Whether the Impulse spell's explosion breaks blocks (it still obeys the mobGriefing game rule).")
            .define("foci.impulseBreaksBlocks", true);

    public static final ModConfigSpec.IntValue POSITIVE_ORB_LIFETIME = B
            .comment("Ticks a Positive Burst orb lives before fading (original: 150).")
            .defineInRange("foci.positiveOrbLifetime", 150, 20, 1200);

    public static final ModConfigSpec SPEC = B.build();

    private TCConfig() {
    }
}
