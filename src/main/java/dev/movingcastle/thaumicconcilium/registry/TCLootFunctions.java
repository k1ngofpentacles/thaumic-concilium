package dev.movingcastle.thaumicconcilium.registry;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Loot functions the port's tables need and vanilla has no exact equivalent for. */
public final class TCLootFunctions {
    private TCLootFunctions() {}

    public static final DeferredRegister<LootItemFunctionType<?>> FUNCTIONS =
            DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, ThaumicConcilium.MODID);

    public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<UniformEnchantBonus>> UNIFORM_ENCHANT_BONUS =
            FUNCTIONS.register("uniform_enchant_bonus", () -> new LootItemFunctionType<>(UniformEnchantBonus.CODEC));

    /**
     * Adds {@code nextInt(level + 1)} to the stack, level being the attacker's level of the enchantment (looked up as
     * {@code minecraft:enchanted_count_increase} does, NeoForge's loot-level event included). This is the 1.7.10 mob-drop
     * idiom ({@code l += rand.nextInt(looting + 1)}, the Burned Witch's {@code func_70628_a}), which vanilla's
     * {@code round(L * uniform(0, 1))} matches only at levels 0 and I.
     */
    public static final class UniformEnchantBonus extends LootItemConditionalFunction {
        public static final MapCodec<UniformEnchantBonus> CODEC = RecordCodecBuilder.mapCodec(i -> commonFields(i)
                .and(Enchantment.CODEC.fieldOf("enchantment").forGetter(f -> f.enchantment))
                .apply(i, UniformEnchantBonus::new));

        private final Holder<Enchantment> enchantment;

        UniformEnchantBonus(List<LootItemCondition> conditions, Holder<Enchantment> enchantment) {
            super(conditions);
            this.enchantment = enchantment;
        }

        /** The level this function reads from a loot context (0 without a living attacker). */
        public static int level(Holder<Enchantment> enchantment, LootContext ctx) {
            Entity attacker = ctx.getParamOrNull(LootContextParams.ATTACKING_ENTITY);
            int lvl = attacker instanceof LivingEntity living ? EnchantmentHelper.getEnchantmentLevel(enchantment, living) : 0;
            return net.neoforged.neoforge.event.EventHooks.getEntityLootEnchantmentLevel(enchantment, lvl, ctx);
        }

        @Override
        public LootItemFunctionType<UniformEnchantBonus> getType() {
            return UNIFORM_ENCHANT_BONUS.get();
        }

        @Override
        public Set<LootContextParam<?>> getReferencedContextParams() {
            return Set.of(LootContextParams.ATTACKING_ENTITY);
        }

        @Override
        protected ItemStack run(ItemStack stack, LootContext ctx) {
            int lvl = level(enchantment, ctx);
            if (lvl > 0) stack.grow(ctx.getRandom().nextInt(lvl + 1));
            return stack;
        }
    }
}
