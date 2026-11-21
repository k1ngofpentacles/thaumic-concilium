package dev.movingcastle.thaumicconcilium.spell;

import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.spell.Spell;
import com.leclowndu93150.thaumaturge.api.spell.SpellNode;
import com.leclowndu93150.thaumaturge.api.spell.Spells;
import com.leclowndu93150.thaumaturge.api.spell.event.SpellCastEvent;
import com.leclowndu93150.thaumaturge.api.spell.part.SpellPart;
import com.leclowndu93150.thaumaturge.api.wands.WandVis;
import com.leclowndu93150.thaumaturge.content.spell.casting.CasterHands;
import com.leclowndu93150.thaumaturge.content.wands.WandVisHelper;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * The original Reflection focus cost no vis (only experience or research points), but Thaumaturge refuses a cast that
 * costs nothing. So Reflection charges its nominal cost and, once the cast has run, gives back exactly what was paid
 * for it: its share of the spell's base vis, so a spell that also carries other parts still pays for those (suggested
 * by the user, 2026-11-21).
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class ReflectionRefund {
    private static final Map<UUID, Pending> PENDING = new java.util.HashMap<>();

    private record Pending(ItemStack caster, WandVis before, float share) {
    }

    private ReflectionRefund() {
    }

    /** Reflection's share of the spell's base vis (part vis + its settings' vis), 0 if absent. */
    private static float share(LivingEntity caster, Spell spell) {
        var registries = caster.level().registryAccess();
        ResourceKey<SpellPart> reflection = ResourceKey.create(SpellPart.REGISTRY_KEY, ThaumicConcilium.id("reflection"));
        float total = 0.0F;
        float ours = 0.0F;
        for (SpellNode node : spell.nodes()) {
            float vis = Spells.part(registries, node.part()).map(SpellPart::vis).orElse(0.0F);
            total += vis;
            if (node.part().equals(reflection)) {
                ours += vis;
            }
        }
        return ours <= 0.0F ? 0.0F : total <= 0.0F ? 1.0F : Math.min(1.0F, ours / total);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void before(SpellCastEvent.Pre event) {
        if (event.isCanceled() || !(event.caster() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        float share = share(player, event.spell());
        if (share <= 0.0F) {
            return;
        }
        ItemStack wand = BraceletCasting.casting();
        if (wand.isEmpty()) {
            wand = CasterHands.held(player);
        }
        if (!wand.isEmpty()) {
            PENDING.put(player.getUUID(), new Pending(wand, WandVisHelper.getAllVis(wand), share));
        }
    }

    @SubscribeEvent
    public static void after(SpellCastEvent.Post event) {
        Pending pending = PENDING.remove(event.caster().getUUID());
        if (pending == null) {
            return;
        }
        WandVis now = WandVisHelper.getAllVis(pending.caster());
        WandVis refunded = now;
        for (var primal : TTAspects.PRIMALS) {
            int paid = pending.before().amount(primal) - now.amount(primal);
            if (paid > 0) {
                refunded = refunded.with(primal, now.amount(primal) + Math.round(paid * pending.share()));
            }
        }
        WandVisHelper.setAllVis(pending.caster(), refunded);
    }
}
