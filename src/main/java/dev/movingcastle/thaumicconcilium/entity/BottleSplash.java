package dev.movingcastle.thaumicconcilium.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** The burst a thrown bottle makes when it breaks: glass shards of the bottle, a cloud in its colour and a splash. */
final class BottleSplash {
    private BottleSplash() {
    }

    static void burst(ServerLevel level, Vec3 at, ItemStack bottle, int rgb, ParticleOptions extra, int extraCount) {
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, bottle), at.x, at.y + 0.1, at.z, 12, 0.1, 0.1, 0.1, 0.12);
        Vector3f colour = new Vector3f((rgb >> 16 & 255) / 255.0F, (rgb >> 8 & 255) / 255.0F, (rgb & 255) / 255.0F);
        level.sendParticles(new DustParticleOptions(colour, 1.6F), at.x, at.y + 0.4, at.z, 40, 1.4, 0.6, 1.4, 0.0);
        level.sendParticles(extra, at.x, at.y + 0.3, at.z, extraCount, 1.2, 0.4, 1.2, 0.05);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1.0F,
                level.random.nextFloat() * 0.1F + 0.9F);
    }
}
