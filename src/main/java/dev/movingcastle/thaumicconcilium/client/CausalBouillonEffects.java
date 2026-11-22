package dev.movingcastle.thaumicconcilium.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Sky and fog of the Causal Bouillon (original {@code WorldProviderCausalBouillonDimension}): no sky, no sunrise, a fixed fog of
 * 0xA080A0 at 15% brightness, fog everywhere. The fixed noon, no weather and the void particles are in the dimension type and biome data.
 */
public final class CausalBouillonEffects extends DimensionSpecialEffects {
    private static final Vec3 FOG = new Vec3(0xA0 / 255.0 * 0.15, 0x80 / 255.0 * 0.15, 0xA0 / 255.0 * 0.15);

    public CausalBouillonEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return FOG;
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return true;
    }
}
