package dev.movingcastle.thaumicconcilium.client;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/**
 * Phase 5d cycle 3: how the mana pond is drawn. Only Minecraft's standard fluid path (a still texture from the block atlas, translucent
 * render type set in {@link TCClient}), so it renders under Iris/Sodium shader packs like any modded fluid: no custom shader, no
 * block-entity renderer, no custom GL. The still texture is an animation strip (its .mcmeta comes from the generator).
 */
public final class ManaPondClient implements IClientFluidTypeExtensions {
    public static final ResourceLocation STILL = ThaumicConcilium.id("block/mana_pond_still");

    @Override
    public ResourceLocation getStillTexture() {
        return STILL;
    }

    @Override
    public ResourceLocation getFlowingTexture() {
        return STILL; // never flows; the same strip, so the atlas holds one sprite
    }

    @Override
    public int getTintColor() {
        return 0xFFFFFFFF;
    }
}
