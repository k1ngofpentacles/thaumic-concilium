package dev.movingcastle.thaumicconcilium.item;

import dev.movingcastle.thaumicconcilium.entity.ProtobodyItemEntity;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Protobody (original {@code Protobody}): stack 1, rare, no use. Dropped, it becomes a {@link ProtobodyItemEntity} (the same
 * position and motion, pickup delay 40) that shatters into Protolimbs below the void line.
 */
public class Protobody extends Item {
    public Protobody(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    @Nullable
    @Override
    public Entity createEntity(Level level, Entity location, ItemStack stack) {
        ProtobodyItemEntity item = new ProtobodyItemEntity(level, location.getX(), location.getY(), location.getZ(), stack);
        item.setDeltaMovement(location.getDeltaMovement());
        item.setPickUpDelay(40);
        if (location instanceof ItemEntity dropped && dropped.getOwner() != null) {
            item.setThrower(dropped.getOwner());
        }
        return item;
    }
}
