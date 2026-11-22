package dev.movingcastle.thaumicconcilium.block;

import dev.movingcastle.thaumicconcilium.entity.Vessel;
import dev.movingcastle.thaumicconcilium.rift.Hexes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Vessel anchor (phase 5c): one of the four ring stones of a broken hex, thrown out 8 to 12 blocks onto solid ground. Using a wand
 * on it draws from the Vessel (a tether). The two block states are the server's display of the fight (for cycle 3's models):
 * {@code charges} is the draws left before it cools (2 ready, 1, 0 cooling) and {@code tethered} whether someone draws from it.
 * No block entity: the Vessel holds all the state.
 */
public class AnchorBlock extends Block {
    public static final IntegerProperty CHARGES = IntegerProperty.create("charges", 0, 2);
    public static final BooleanProperty TETHERED = BooleanProperty.create("tethered");

    /** Which of the eight broken-shard looks this stone has (cycle 3b); each Vessel hands four different ones to its anchors. */
    public static final IntegerProperty SHAPE = IntegerProperty.create("shape", 0, 7);
    /** The walkable top is half a block: stepped onto without a jump. */
    private static final VoxelShape BOX = Block.box(1.0, 0.0, 1.0, 15.0, 8.0, 15.0);

    public AnchorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(CHARGES, 2).setValue(TETHERED, false).setValue(SHAPE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGES, TETHERED, SHAPE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BOX;
    }

    /** A shuffle of 0..7 seeded by the Vessel's UUID; the first four are its anchors' shapes (pure, so a test can read it). */
    public static int[] shapesFor(java.util.UUID vessel) {
        int[] order = {0, 1, 2, 3, 4, 5, 6, 7};
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(vessel.getLeastSignificantBits());
        for (int i = order.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int t = order[i];
            order[i] = order[j];
            order[j] = t;
        }
        return order;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (!Hexes.isWand(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
            Vessel vessel = Vessel.forAnchor(server, pos);
            if (vessel != null) {
                vessel.draw(sp, pos);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Cooling, a smoke puff at 1 in 6. The glyphs that rise from a Vessel's anchor are spawned by the Vessel's client effects. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (state.getValue(CHARGES) == 0 && !state.getValue(TETHERED) && random.nextInt(6) == 0) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6, pos.getY() + 0.55,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6, 0.0, 0.02, 0.0);
        }
    }

    /** The box searched for the Vessel an anchor belongs to (anchors stand 8 to 12 blocks out, sometimes level with the hex). */
    public static AABB searchBox(BlockPos pos) {
        return new AABB(pos).inflate(24.0);
    }
}
