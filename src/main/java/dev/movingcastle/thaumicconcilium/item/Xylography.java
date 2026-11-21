package dev.movingcastle.thaumicconcilium.item;

import com.leclowndu93150.thaumaturge.registry.TTItemTags;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.registry.TCDataComponents;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Wand xylography (original: {@code WandXylography} plus the crafting and tooltip handlers in {@code TCEntityEventHandler}).
 * A wand alone in the crafting grid comes out signed with its crafter's name ({@link XylographyRecipe}); the original
 * stored the name only, the port stores the crafter's UUID as well so the Astral Monitor can find them again.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class Xylography {
    /** Who signed a wand. A recipe result carries {@link #PENDING} until the crafting event fills in the crafter. */
    public record Signer(UUID id, String name) {
        public static final Signer PENDING = new Signer(new UUID(0L, 0L), " ");
        public static final Codec<Signer> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Signer::id),
                Codec.STRING.fieldOf("name").forGetter(Signer::name)).apply(i, Signer::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Signer> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Signer::id, ByteBufCodecs.STRING_UTF8, Signer::name, Signer::new);

        public boolean pending() {
            return this.id.equals(PENDING.id);
        }
    }

    private Xylography() {
    }

    /** The signature on a wand, if a player has signed it (a pending, unsigned placeholder counts as none). */
    public static Optional<Signer> signer(ItemStack stack) {
        Signer signer = stack.get(TCDataComponents.XYLOGRAPHY.get());
        return signer == null || signer.pending() ? Optional.empty() : Optional.of(signer);
    }

    public static boolean signable(ItemStack stack) {
        return stack.is(TTItemTags.WANDS);
    }

    @SubscribeEvent
    public static void crafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack stack = event.getCrafting();
        if (event.getEntity() instanceof FakePlayer || !signable(stack)) {
            return;
        }
        Signer signer = stack.get(TCDataComponents.XYLOGRAPHY.get());
        if (signer != null && signer.pending()) {
            stack.set(TCDataComponents.XYLOGRAPHY.get(), new Signer(event.getEntity().getUUID(), event.getEntity().getGameProfile().getName()));
        }
    }

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        Signer signer = event.getItemStack().get(TCDataComponents.XYLOGRAPHY.get());
        if (signer != null && !signer.pending() && !signer.name().isBlank()) {
            event.getToolTip().add(Component.translatable("thaumicconcilium.tooltip.xylography").withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE)
                    .append(Component.literal(" " + signer.name()).withStyle(ChatFormatting.RESET)));
        }
    }
}
