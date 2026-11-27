package dev.movingcastle.thaumicconcilium.compat.witchery;

import com.mojang.datafixers.util.Pair;
import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.world.BurningSiteElement;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/**
 * Phase 10-4: puts the Burning Site into every village type's houses pool and its zombie twin (the original reached every 1.7.10 village;
 * 1.21 has five types). Weight per pool = the original's share (10 of 1.7.10's 91 house weight: pool total x chance / 91), never below 1;
 * "one per village" is {@code BurningSiteElement.isFirstSite}. Desert pools get the sandstone template. The two pool fields are made
 * accessible by META-INF/accesstransformer.cfg. Runs once per pool object (a restarted integrated server loads fresh pools).
 * Only with the remake and {@code witchery.generateBurningSites}.
 */
public final class BurningSites {
    public static final String[] TYPES = {"plains", "desert", "savanna", "snowy", "taiga"};
    /** 1.7.10's village house weights (the original's chance of 10 is out of this). */
    public static final int ORIGINAL_HOUSE_WEIGHT = 91;

    /** Pool total before the injection and the weight given, per pool (read by the pack test: other mods may add houses after us). */
    public static final java.util.Map<ResourceLocation, int[]> LAST = new java.util.concurrent.ConcurrentHashMap<>();

    private BurningSites() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(BurningSites::onAboutToStart);
    }

    private static void onAboutToStart(ServerAboutToStartEvent event) {
        if (TCConfig.generateBurningSites()) {
            inject(event.getServer(), TCConfig.burningSitesGenChance());
        }
    }

    public static ResourceLocation poolId(String type, boolean zombie) {
        return ResourceLocation.withDefaultNamespace("village/" + type + (zombie ? "/zombie" : "") + "/houses");
    }

    public static int weightFor(int poolTotal, int chance) {
        return Math.max(1, Math.round(poolTotal * (float) chance / ORIGINAL_HOUSE_WEIGHT));
    }

    /** Returns the number of pools changed (0 when chance is 0 or already injected). */
    public static int inject(MinecraftServer server, int chance) {
        if (chance <= 0) {
            return 0;
        }
        Registry<StructureTemplatePool> pools = server.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        int changed = 0;
        for (String type : TYPES) {
            for (boolean zombie : new boolean[]{false, true}) {
                StructureTemplatePool pool = pools.get(poolId(type, zombie));
                if (pool == null || contains(pool)) {
                    continue;
                }
                BurningSiteElement element = BurningSiteElement.of(ThaumicConcilium.id(
                        type.equals("desert") ? "village/burning_site_desert" : "village/burning_site"));
                int weight = weightFor(pool.size(), chance);
                LAST.put(poolId(type, zombie), new int[]{pool.size(), weight});
                List<Pair<StructurePoolElement, Integer>> raw = new ArrayList<>(pool.rawTemplates);
                raw.add(Pair.of(element, weight));
                pool.rawTemplates = raw;
                for (int i = 0; i < weight; i++) {
                    pool.templates.add(element);
                }
                changed++;
            }
        }
        return changed;
    }

    public static boolean contains(StructureTemplatePool pool) {
        for (Pair<StructurePoolElement, Integer> p : pool.rawTemplates) {
            if (p.getFirst() instanceof BurningSiteElement) {
                return true;
            }
        }
        return false;
    }
}
