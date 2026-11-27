package dev.movingcastle.thaumicconcilium.lithograph;

import com.leclowndu93150.thaumaturge.content.equipment.runic.RunicShieldState;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.thaumicconcilium.integration.botania.BotaniaMana;
import dev.movingcastle.thaumicconcilium.integration.neovitae.NeoVitaeBlood;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

/**
 * What the Astral Monitor and the lithograph HUD show about one watched player (original: the {@code vals} and
 * {@code party*} arrays in {@code TCEntityEventHandler}). Each field is a frame of the constellation sprite sheet:
 * <ul>
 *   <li>{@code health}: 0 to 10 tenths of full health;</li>
 *   <li>{@code blood}: -1 (hidden: no Neo Vitae, or no blood orb carried) or 0 to 9, Neo Vitae's soul network over the
 *   ceiling of the orbs carried in tenths minus one; an empty network is frame 0, as in the original;</li>
 *   <li>{@code mana}: -1 (hidden) or 0 to 9, the tenths of the mana held in Botania mana items minus one, as the
 *   original did (Botania only);</li>
 *   <li>{@code runes}: -1 (hidden) or 0 to 9, the same for Thaumaturge's runic shielding (charge over its maximum).</li>
 * </ul>
 * {@link #NONE} (health -1) is an empty slot or a player who is not in this dimension.
 */
public record PartyStats(int health, int blood, int mana, int runes) {
    public static final PartyStats NONE = new PartyStats(-1, -1, -1, -1);
    public static final Codec<PartyStats> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("health").forGetter(PartyStats::health),
            Codec.INT.fieldOf("blood").forGetter(PartyStats::blood),
            Codec.INT.fieldOf("mana").forGetter(PartyStats::mana),
            Codec.INT.fieldOf("runes").forGetter(PartyStats::runes)).apply(i, PartyStats::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, PartyStats> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PartyStats::health, ByteBufCodecs.VAR_INT, PartyStats::blood,
            ByteBufCodecs.VAR_INT, PartyStats::mana, ByteBufCodecs.VAR_INT, PartyStats::runes, PartyStats::new);

    public boolean present() {
        return this.health >= 0;
    }

    /** The stats of {@code target}, or {@link #NONE} if they are not in {@code level} (original: same world, by name). */
    public static PartyStats of(ServerLevel level, @Nullable UUID target) {
        if (target == null || !(level.getPlayerByUUID(target) instanceof ServerPlayer player)) {
            return NONE;
        }
        return of(player);
    }

    public static PartyStats of(ServerPlayer player) {
        int health = Math.round(player.getHealth() / player.getMaxHealth() * 10.0F);
        return new PartyStats(Mth.clamp(health, 0, 9), blood(player), mana(player), runes(player));
    }

    /** Neo Vitae's soul network over the ceiling of the orbs the player carries; see {@link NeoVitaeBlood#tenths}. */
    private static int blood(Player player) {
        return ModList.get().isLoaded("neovitae") ? NeoVitaeBlood.tenths(player) : -1;
    }

    private static int mana(Player player) {
        return ModList.get().isLoaded("botania") ? BotaniaMana.tenths(player) : -1;
    }

    private static int runes(ServerPlayer player) {
        RunicShieldState state = player.getData(TTAttachments.RUNIC_SHIELD.get());
        if (state.maxCharge <= 0) {
            return -1;
        }
        float held = Math.min(player.getAbsorptionAmount(), state.maxCharge);
        return Mth.clamp(Math.round(held / state.maxCharge * 10.0F) - 1, -1, 9);
    }
}
