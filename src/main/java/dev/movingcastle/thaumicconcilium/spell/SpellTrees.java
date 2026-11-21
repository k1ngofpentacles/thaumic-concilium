package dev.movingcastle.thaumicconcilium.spell;

import com.leclowndu93150.thaumaturge.api.spell.Spell;
import com.leclowndu93150.thaumaturge.api.spell.SpellNode;
import com.leclowndu93150.thaumaturge.api.spell.part.SpellPart;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import java.util.Set;
import net.minecraft.resources.ResourceKey;

/** Helpers for inspecting inscribed spell trees. */
public final class SpellTrees {
    private SpellTrees() {
    }

    public static ResourceKey<SpellPart> part(String path) {
        return ResourceKey.create(SpellPart.REGISTRY_KEY, ThaumicConcilium.id(path));
    }

    public static boolean contains(Spell spell, ResourceKey<SpellPart> part) {
        return spell != null && contains(spell.root(), Set.of(part));
    }

    public static boolean containsAny(Spell spell, Set<ResourceKey<SpellPart>> parts) {
        return spell != null && contains(spell.root(), parts);
    }

    private static boolean contains(SpellNode node, Set<ResourceKey<SpellPart>> parts) {
        if (parts.contains(node.part())) {
            return true;
        }
        for (SpellNode child : node.children()) {
            if (contains(child, parts)) {
                return true;
            }
        }
        return false;
    }

}
