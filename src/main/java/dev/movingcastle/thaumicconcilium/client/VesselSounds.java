package dev.movingcastle.thaumicconcilium.client;

import dev.movingcastle.thaumicconcilium.entity.Vessel;
import dev.movingcastle.thaumicconcilium.registry.TCSounds;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Vessel's one-shot sounds (memo section 5). Each is our own event ({@code thaumicconcilium:vessel.*}, sounds.json, pointing at a
 * vanilla or Thaumaturge event, so nothing is shipped); the layered ones add a second event here. Played on the client from synced
 * changes, so every player in range hears them.
 */
final class VesselSounds {
    private record Extra(String event, float volume, float pitch) {
    }

    private static final Map<String, List<Extra>> EXTRAS = Map.of(
            "wake", List.of(new Extra("thaumaturge:rumble", 0.7F, 1.0F)),
            "gulp", List.of(new Extra("thaumaturge:wind", 1.0F, 1.0F)),
            "stagger", List.of(new Extra("minecraft:block.glass.break", 0.5F, 0.5F)),
            "horizon_throw", List.of(new Extra("thaumaturge:zap", 0.6F, 1.0F)));

    private VesselSounds() {
    }

    static void play(Vessel v, String name, float range) {
        playAt(v.level(), v.position(), name, range);
    }

    /** {@code range} scales the audible distance (the variable-range event is 16 blocks at 1.0). */
    static void playAt(Level level, Vec3 at, String name, float range) {
        var holder = TCSounds.VESSEL.get(name);
        if (holder != null) {
            level.playLocalSound(at.x, at.y, at.z, holder.get(), SoundSource.HOSTILE, range, 1.0F, false);
        }
        for (Extra x : EXTRAS.getOrDefault(name, List.of())) {
            SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(x.event()));
            if (event != null) {
                level.playLocalSound(at.x, at.y, at.z, event, SoundSource.HOSTILE, x.volume() * range, x.pitch(), false);
            }
        }
    }
}
