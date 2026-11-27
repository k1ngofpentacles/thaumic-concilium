package dev.movingcastle.thaumicconcilium.compat.witchery;

import net.neoforged.fml.ModList;

/**
 * Phase 10's one switch (user decision R1): everything Witchery-flavoured exists only when Just Another Witchery Remake
 * ({@code witchery}) is loaded, on both sides. This class names no Witchery class.
 */
public final class WitcheryGate {
    public static final String MODID = "witchery";

    private WitcheryGate() {
    }

    public static boolean loaded() {
        return ModList.get() != null && ModList.get().isLoaded(MODID);
    }
}
