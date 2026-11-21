package dev.movingcastle.thaumicconcilium.spell;

import com.leclowndu93150.thaumaturge.api.spell.Spell;
import com.leclowndu93150.thaumaturge.api.spell.SpellNode;
import com.leclowndu93150.thaumaturge.api.spell.Spells;
import com.leclowndu93150.thaumaturge.api.spell.part.SpellPart;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import net.minecraft.core.RegistryAccess;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ItemAbilities;

/**
 * Shield casting is reserved for shield foci (agreed with the user 2026-11-21): a worn bracelet doesn't cast while the
 * off hand holds a shield, unless the focus carries a spell part tagged {@code thaumicconcilium:shield_foci}
 * ({@code data/thaumicconcilium/tags/thaumaturge/spell_part/shield_foci.json}). The tag is empty until the shield foci are
 * designed (after phase 4).
 */
public final class ShieldFoci {
    public static final TagKey<SpellPart> TAG = TagKey.create(SpellPart.REGISTRY_KEY, ThaumicConcilium.id("shield_foci"));

    private ShieldFoci() {
    }

    public static boolean holdsShield(LivingEntity entity) {
        return entity.getOffhandItem().canPerformAction(ItemAbilities.SHIELD_BLOCK);
    }

    /** Whether the focus's spell has a shield part. */
    public static boolean isShieldFocus(ItemStack focus, RegistryAccess registries) {
        Spell spell = Spells.spellOf(focus);
        if (spell == null) {
            return false;
        }
        var parts = registries.registryOrThrow(SpellPart.REGISTRY_KEY);
        for (SpellNode node : spell.nodes()) {
            if (parts.getHolder(node.part()).map(h -> h.is(TAG)).orElse(false)) {
                return true;
            }
        }
        return false;
    }
}
