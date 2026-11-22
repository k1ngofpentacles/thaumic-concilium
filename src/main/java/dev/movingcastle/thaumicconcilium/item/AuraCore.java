package dev.movingcastle.thaumicconcilium.item;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.mojang.serialization.Codec;
import dev.movingcastle.thaumicconcilium.registry.TCDataComponents;
import dev.movingcastle.thaumicconcilium.registry.TCItems;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Aura Core (adapted from Gadomancy 1.7.10's {@code ItemAuraCore}, LGPL, credit Makeo and the Gadomancy team). A blank core is
 * made by infusion, tuned to one primal aspect by a wand-struck primal crystal cluster ({@link dev.movingcastle.thaumicconcilium.aura.AuraCores}),
 * and washed back to blank in a crucible. The type is a data component (the original's damage value).
 * Unlike the original, the earth and entropy cores are not marked "unused": Concilium's Gem of Rifts and Terra-cast Gem use them.
 */
public class AuraCore extends Item {
    public AuraCore(Properties properties) {
        super(properties);
    }

    /** Original order and metadata: blank 0, air, fire, water, earth, order, entropy. */
    public enum Type implements StringRepresentable {
        BLANK("blank", -1, 0xFFFFFFFF),
        AIR("air", 0, 0xFFFFFF7E),
        FIRE("fire", 1, 0xFFFF5A01),
        WATER("water", 2, 0xFF3CD4FC),
        EARTH("earth", 3, 0xFF56C000),
        ORDER("order", 4, 0xFFD5D4EC),
        ENTROPY("entropy", 5, 0xFF404040);

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);
        public static final StreamCodec<RegistryFriendlyByteBuf, Type> STREAM_CODEC =
                ByteBufCodecs.idMapper(i -> values()[Math.floorMod(i, values().length)], Type::ordinal).cast();

        private final String name;
        private final int primal;
        private final int fallbackColor;

        Type(String name, int primal, int fallbackColor) {
            this.name = name;
            this.primal = primal;
            this.fallbackColor = fallbackColor;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public boolean isBlank() {
            return primal < 0;
        }

        /** The Thaumaturge primal this core is tuned to, or null for the blank core. */
        @Nullable
        public ResourceKey<IAspect> primal() {
            return primal < 0 ? null : TTAspects.PRIMALS.get(primal);
        }

        /** Index into {@link TTAspects#PRIMALS}, or -1. */
        public int primalIndex() {
            return primal;
        }

        public int fallbackColor() {
            return fallbackColor;
        }

        @Nullable
        public static Type ofPrimalIndex(int index) {
            for (Type t : values()) {
                if (t.primal == index) {
                    return t;
                }
            }
            return null;
        }
    }

    public static Type type(ItemStack stack) {
        return stack.getOrDefault(TCDataComponents.CORE_TYPE.get(), Type.BLANK);
    }

    public static ItemStack stack(Type type) {
        ItemStack stack = new ItemStack(TCItems.AURA_CORE.get());
        stack.set(TCDataComponents.CORE_TYPE.get(), type);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        Type type = type(stack);
        tooltip.add((type.isBlank() ? Component.translatable("thaumicconcilium.auracore.blank")
                : Component.translatable("aspect.thaumaturge." + type.primal().location().getPath())).withStyle(ChatFormatting.GRAY));
    }
}
