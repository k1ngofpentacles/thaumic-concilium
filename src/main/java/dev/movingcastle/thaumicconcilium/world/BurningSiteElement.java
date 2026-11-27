package dev.movingcastle.thaumicconcilium.world;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.thaumicconcilium.compat.witchery.WitcheryGate;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * Phase 10-4: the Burning Site (original BurningSite.java, village piece weight 10, limit 1). The template holds the floor, the wall ring
 * and the stake; this element then rolls the pyre per tile (original: {@code rand.nextInt(3)} = coal block, hay bale, vertical log) and
 * spawns the site's Witch Hunter (only with the remake). Registered always (names no remake class), so saves keep loading if the remake
 * leaves; the pool injection that puts it into villages is {@code compat.witchery.BurningSites}.
 *
 * <p>Local frame (template coordinates): floor y 1, pyre x and z 3..5, the original's hunter spot (2, floor + 1, 2) = (2, 2, 2), the
 * jigsaw/gate cell (0, 2, 4).
 */
public class BurningSiteElement extends SinglePoolElement {
    public static final MapCodec<BurningSiteElement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            templateCodec(), processorsCodec(), projectionCodec(), overrideLiquidSettingsCodec()).apply(i, BurningSiteElement::new));

    /** The entity tag that marks the site's own hunter (its goals are swapped by compat.witchery.SiteHunter). */
    public static final String HUNTER_TAG = "thaumicconcilium.site_hunter";
    public static final ResourceLocation HUNTER_TYPE = ResourceLocation.fromNamespaceAndPath("witchery", "hunter");
    public static final BlockPos HUNTER_SPOT = new BlockPos(2, 2, 2);
    public static final BlockPos GATE = new BlockPos(0, 2, 4);

    public BurningSiteElement(Either<ResourceLocation, StructureTemplate> template, Holder<StructureProcessorList> processors,
                              StructureTemplatePool.Projection projection, Optional<LiquidSettings> liquid) {
        super(template, processors, projection, liquid);
    }

    /** An element on a template id, rigid, no processors (tests and the pool injection). */
    public static BurningSiteElement of(ResourceLocation template) {
        return new BurningSiteElement(Either.left(template), Holder.direct(new StructureProcessorList(List.of())),
                StructureTemplatePool.Projection.RIGID, Optional.empty());
    }

    /** Desert sites swap the pyre's log for sandstone (1.7.10's desert village swap: gravel and log to sandstone; the walls, hay and coal stay). */
    public boolean isDesert() {
        return this.template.left().map(rl -> rl.getPath().endsWith("_desert")).orElse(false);
    }

    @Override
    public StructurePoolElementType<?> getType() {
        return TCStructures.BURNING_SITE.get();
    }

    /** Original: one per village (the piece limit). Only the first site piece of a village, in piece order, places; no village = places. */
    public static boolean isFirstSite(List<StructurePiece> pieces, BlockPos pos) {
        for (StructurePiece piece : pieces) {
            if (piece instanceof PoolElementStructurePiece p && p.getElement() instanceof BurningSiteElement) {
                return p.getPosition().equals(pos);
            }
        }
        return true;
    }

    @Override
    public boolean place(StructureTemplateManager manager, WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                         BlockPos pos, BlockPos pivot, Rotation rotation, BoundingBox box, RandomSource random, LiquidSettings liquid,
                         boolean keepJigsaws) {
        StructureStart start = structures.getStructureWithPieceAt(pos, StructureTags.VILLAGE);
        if (start.isValid() && !isFirstSite(start.getPieces(), pos)) {
            return true;
        }
        if (!super.place(manager, level, structures, generator, pos, pivot, rotation, box, random, liquid, keepJigsaws)) {
            return false;
        }
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
        boolean desert = isDesert();
        // the pyre: nine rolls per pass (as the original), set only inside this chunk's box
        for (int x = 3; x <= 5; x++) {
            for (int z = 3; z <= 5; z++) {
                int r = random.nextInt(3);
                BlockPos at = StructureTemplate.calculateRelativePosition(settings, new BlockPos(x, 1, z)).offset(pos);
                if (box.isInside(at)) {
                    BlockState state = r == 0 ? Blocks.COAL_BLOCK.defaultBlockState()
                            : r == 1 ? Blocks.HAY_BLOCK.defaultBlockState()
                            : (desert ? Blocks.SANDSTONE : Blocks.OAK_LOG).defaultBlockState();
                    level.setBlock(at, state, 2);
                }
            }
        }
        // the jigsaw's final state is not rotated by the game, so the gate cell's wall is set here with its true connections
        BlockPos gate = StructureTemplate.calculateRelativePosition(settings, GATE).offset(pos);
        if (box.isInside(gate)) {
            level.setBlock(gate, Blocks.COBBLESTONE_WALL.defaultBlockState().setValue(WallBlock.UP, false)
                    .setValue(WallBlock.NORTH_WALL, WallSide.LOW).setValue(WallBlock.SOUTH_WALL, WallSide.LOW).rotate(rotation), 2);
        }
        // the hunter, in the chunk that holds its spot (the original spawned one per chunk pass; see the executive call)
        BlockPos spot = StructureTemplate.calculateRelativePosition(settings, HUNTER_SPOT).offset(pos);
        if (WitcheryGate.loaded() && box.isInside(spot)) {
            spawnHunter(level, spot);
        }
        return true;
    }

    /** Original: persistent, {@code onSpawnWithEgg(null)}, at the spot centred, yaw 0. Returns the hunter or null. */
    public static Mob spawnHunter(WorldGenLevel level, BlockPos spot) {
        Optional<net.minecraft.world.entity.EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(HUNTER_TYPE);
        if (type.isEmpty()) {
            return null;
        }
        Entity e = type.get().create(level.getLevel());
        if (!(e instanceof Mob mob)) {
            return null;
        }
        mob.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0.0F, 0.0F);
        mob.setPersistenceRequired();
        // through the event: the remake hands its hunter the crossbow and hunter armour in a FinalizeSpawnEvent listener
        net.neoforged.neoforge.event.EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(spot), MobSpawnType.STRUCTURE, null);
        mob.addTag(HUNTER_TAG);
        level.addFreshEntityWithPassengers(mob);
        return mob;
    }
}
