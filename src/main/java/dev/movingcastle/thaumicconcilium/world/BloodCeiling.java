package dev.movingcastle.thaumicconcilium.world;

import com.mojang.serialization.Codec;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import java.util.function.Supplier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The best blood orb a player has ever had, as its life-point capacity (0 = never). Blood Magic kept this on the soul network
 * ({@code getCurrentMaxOrb}); Neo Vitae keeps none, so we do: raised whenever a bound orb of the player's is seen
 * ({@code integration.neovitae.NeoVitaeBlood}), saved, and kept through death. It is the ceiling for the party glyph and the
 * Drainage Syringe's life-point return. No Neo Vitae types here: the attachment exists with or without the mod.
 */
public final class BloodCeiling {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ThaumicConcilium.MODID);
    public static final Supplier<AttachmentType<Integer>> CAPACITY = ATTACHMENTS.register("blood_ceiling",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().build());

    private BloodCeiling() {
    }

    public static int get(Player player) {
        return player.getData(CAPACITY.get());
    }

    /** Raises the record to {@code capacity} when that is higher; never lowers it. */
    public static void raise(Player player, int capacity) {
        if (capacity > get(player)) {
            player.setData(CAPACITY.get(), capacity);
        }
    }
}
