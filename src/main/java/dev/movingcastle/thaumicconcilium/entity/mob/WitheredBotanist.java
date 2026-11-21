package dev.movingcastle.thaumicconcilium.entity.mob;

import com.leclowndu93150.thaumaturge.registry.TTSounds;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/**
 * The Withered Botanist (original: {@code corpse.WitheredBotanist}, only with Botania): a dead botanist, who gives back
 * to the soil. Hit or touched, three or four Botania mystical flowers of random colours bloom around them and the body
 * is gone with the wind.
 */
public class WitheredBotanist extends Corpse {
    public WitheredBotanist(EntityType<? extends WitheredBotanist> type, Level level) {
        super(type, level);
    }

    @Override
    protected void disturb(@Nullable Player player) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        List<BlockPos> valid = new ArrayList<>();
        int bx = Mth.floor(this.getX());
        int by = Mth.floor(this.getY());
        int bz = Mth.floor(this.getZ());
        for (int i = -4; i < 3; i++) {
            for (int j = -4; j < 3; j++) {
                for (int k = 2; k >= -2; k--) {
                    BlockPos pos = new BlockPos(bx + i + 1, by + k + 1, bz + j + 1);
                    if (level.isEmptyBlock(pos) && level.isInWorldBounds(pos) && flower(DyeColor.WHITE).canSurvive(level, pos)) {
                        valid.add(pos);
                    }
                }
            }
        }
        int count = Math.min(valid.size(), this.random.nextBoolean() ? 3 : 4);
        for (int i = 0; i < count; i++) {
            BlockPos pos = valid.remove(this.random.nextInt(valid.size()));
            level.setBlock(pos, flower(DyeColor.byId(this.random.nextInt(16))), 3);
        }
        level.playSound(null, this.getX(), this.getY(), this.getZ(), TTSounds.WIND.get(), SoundSource.NEUTRAL, 0.5F,
                0.9F + this.random.nextFloat() * 0.2F);
        // Original (client): falling leaves and fifteen Botania wisps of random colours.
        level.sendParticles(ParticleTypes.CHERRY_LEAVES, this.getX(), this.getY() + 0.3, this.getZ(), 20, 1.0, 0.2, 0.6, 0.0);
        for (int i = 0; i < 15; i++) {
            double x = bx - 3 + this.random.nextInt(7) + this.random.nextDouble();
            double z = bz - 3 + this.random.nextInt(7) + this.random.nextDouble();
            level.sendParticles(new DustParticleOptions(new Vector3f(this.random.nextFloat(), this.random.nextFloat(), this.random.nextFloat()),
                    1.0F + this.random.nextFloat()), x, by + 1, z, 1, 0.0, 0.1, 0.0, 0.0);
        }
        this.discard();
    }

    /** Botania's mystical flower of that colour (falls back to air, so nothing is placed without Botania). */
    private static BlockState flower(DyeColor color) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("botania", color.getName() + "_mystical_flower"));
        return block == Blocks.AIR ? Blocks.AIR.defaultBlockState() : block.defaultBlockState();
    }
}
