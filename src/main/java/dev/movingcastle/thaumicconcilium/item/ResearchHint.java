package dev.movingcastle.thaumicconcilium.item;

import com.leclowndu93150.thaumaturge.api.research.IResearchEntry;
import com.leclowndu93150.thaumaturge.api.capability.PlayerResearchAccess;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.registry.TCDataComponents;
import dev.movingcastle.thaumicconcilium.registry.TCItems;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * A research hint (original: a research note tagged {@code REFLECTION_HINT} and so on). Mad Thaumaturges drop the first
 * four, Thaumaturges trade the last three for completed research notes. Each is an {@code obtain} requirement on its
 * entry's first stage, so the entry can't be started without it.
 */
public final class ResearchHint extends Item {
    public static final ResourceLocation REFLECTION = ThaumicConcilium.id("reflection_focus");
    public static final ResourceLocation THICK_TAINT = ThaumicConcilium.id("bottle_of_thick_taint");
    public static final ResourceLocation WINDINGS = ThaumicConcilium.id("runic_windings");
    public static final ResourceLocation DEST_CRYSTAL = ThaumicConcilium.id("destabilized_crystal");
    public static final ResourceLocation VIS_CONDUCTOR = ThaumicConcilium.id("vis_conductor_focus");
    public static final ResourceLocation POSITIVE_BURST = ThaumicConcilium.id("positive_burst_focus");
    public static final ResourceLocation IMPULSE = ThaumicConcilium.id("impulse_focus");

    /** The four a Mad Thaumaturge carries; all four known lights up the original's "these lunatics" line. */
    public static final List<ResourceLocation> MAD = List.of(REFLECTION, THICK_TAINT, WINDINGS, DEST_CRYSTAL);
    /** The three a Thaumaturge trades; all three known, it says it has nothing left to surprise you with. */
    public static final List<ResourceLocation> TRADED = List.of(VIS_CONDUCTOR, POSITIVE_BURST, IMPULSE);

    private static final Map<ResourceLocation, String> TEXT = Map.of(
            REFLECTION, "reflection", THICK_TAINT, "thicktaint", WINDINGS, "windings", DEST_CRYSTAL, "destcrystal",
            VIS_CONDUCTOR, "visconductor", POSITIVE_BURST, "positiveburst", IMPULSE, "impulse");

    public ResearchHint(Properties properties) {
        super(properties);
    }

    public static ItemStack stack(ResourceLocation entry) {
        ItemStack stack = new ItemStack(TCItems.RESEARCH_HINT.get());
        stack.set(TCDataComponents.HINT.get(), entry);
        return stack;
    }

    public static boolean exists(HolderLookup.Provider registries, ResourceLocation entry) {
        return registries.lookup(IResearchEntry.REGISTRY_KEY)
                .flatMap(l -> l.get(ResourceKey.create(IResearchEntry.REGISTRY_KEY, entry))).isPresent();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation entry = stack.get(TCDataComponents.HINT.get());
        if (entry == null || !TEXT.containsKey(entry)) {
            return;
        }
        tooltip.add(Component.translatable("thaumicconcilium.tooltip.hint." + TEXT.get(entry)).withStyle(ChatFormatting.DARK_PURPLE));
        if (MAD.contains(entry) && FMLEnvironment.dist.isClient() && madComplete(ClientAccess.player(), context.registries())) {
            tooltip.add(Component.translatable("thaumicconcilium.tooltip.hint.mad_complete").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    /**
     * Original {@code Thaumonomicon.checkMadThaumaturgeComplete}: each of the Mad Thaumaturge's hints is either researched
     * or carried. A hint whose entry isn't in the pack yet counts as known.
     */
    public static boolean madComplete(Player player, HolderLookup.Provider registries) {
        return allKnown(player, registries, MAD);
    }

    /** Original {@code Thaumonomicon.checkThaumaturgeComplete}, the same check over the traded hints. */
    public static boolean allKnown(Player player, HolderLookup.Provider registries, List<ResourceLocation> hints) {
        if (player == null) {
            return false;
        }
        for (ResourceLocation entry : hints) {
            if (registries != null && !exists(registries, entry)) {
                continue;
            }
            boolean known = PlayerResearchAccess.of(player).hasCompleted(entry)
                    || player.getInventory().items.stream().anyMatch(s -> s.is(TCItems.RESEARCH_HINT.get())
                    && entry.equals(s.get(TCDataComponents.HINT.get())));
            if (!known) {
                return false;
            }
        }
        return true;
    }

    /** Keeps the client class out of the server's class loading. */
    private static final class ClientAccess {
        static Player player() {
            return Minecraft.getInstance().player;
        }
    }
}
