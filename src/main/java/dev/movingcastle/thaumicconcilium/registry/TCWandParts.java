package dev.movingcastle.thaumicconcilium.registry;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.wands.IWandRodOnUpdate;
import com.leclowndu93150.thaumaturge.api.wands.WandRod;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.leclowndu93150.thaumaturge.content.wands.WandVisHelper;
import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import com.leclowndu93150.thaumaturge.registry.TTWandParts;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.item.BraceletKind;
import dev.movingcastle.thaumicconcilium.integration.botania.BotaniaBracelets;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

/**
 * One Thaumaturge wand rod per bracelet. A bracelet's capacity and self-recharge come from its rod, as a wand's do; these
 * rods have no item, so they never show up in wand assembly. They are wand rods, not staffs, so a bracelet fits the
 * Arcane Workbench's wand slot (agreed with the user 2026-11-21; the original's bracelets counted as staffs).
 */
public final class TCWandParts {
    public static final DeferredRegister<WandRod> RODS = DeferredRegister.create(WandRod.REGISTRY_KEY, ThaumicConcilium.MODID);
    public static final Map<BraceletKind, DeferredHolder<WandRod, WandRod>> BRACELET_RODS = new EnumMap<>(BraceletKind.class);
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumaturge", "textures/model/wand_rod_greatwood.png");
    /** Tainted Magic's WARP_WAND_REFRESH_BASE. */
    private static final float WARP_REFRESH_BASE = 10000.0F;

    static {
        for (BraceletKind kind : BraceletKind.values()) {
            if (!kind.available()) {
                continue;
            }
            BRACELET_RODS.put(kind, RODS.register("bracelet_" + kind.id(),
                    () -> new WandRod(kind.capacity(), 1, TEXTURE, recharge(kind.recharge()), kind.glow(), false, false)));
        }
    }

    private TCWandParts() {
    }

    @Nullable
    private static IWandRodOnUpdate recharge(BraceletKind.Recharge recharge) {
        return switch (recharge) {
            case NONE -> null;
            case MANA -> (stack, player) -> BotaniaBracelets.refill(stack, player, false);
            case TERRASTEEL -> (stack, player) -> BotaniaBracelets.refill(stack, player, true);
            case FM_INFERNAL -> delegate("infernal");
            case FM_TAINTED -> delegate("tainted");
            case WARP -> TCWandParts::warpRecharge;
            case BLOOD -> (stack, player) -> dev.movingcastle.thaumicconcilium.integration.neovitae.NeoVitaeBracelets.refill(stack, player);
        };
    }

    /** Forbidden Magic's rod of that name, if it is installed. */
    private static IWandRodOnUpdate delegate(String rod) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("forbiddenmagic", rod);
        return (stack, player) -> {
            WandRod fm = TTWandParts.rods().get(id);
            if (fm != null && fm.onUpdate() != null) {
                fm.onUpdate().onUpdate(stack, player);
            }
        };
    }

    private static void warpRecharge(net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.player.Player player) {
        if (player.hasEffect(TTMobEffects.WARP_WARD)) {
            return;
        }
        int permanent = WarpHelper.getWarp(player).get(WarpType.PERMANENT);
        if (permanent <= 0) {
            return;
        }
        int period = Math.max(1, Math.round(WARP_REFRESH_BASE / permanent));
        if (player.tickCount % period == 0) {
            for (ResourceKey<IAspect> primal : TTAspects.PRIMALS) {
                WandVisHelper.addRealVis(stack, primal, 100, true);   // one vis, or what still fits
            }
        }
    }
}
