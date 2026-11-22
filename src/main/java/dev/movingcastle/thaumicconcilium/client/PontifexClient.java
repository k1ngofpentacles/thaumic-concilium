package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.item.PontifexRobe;
import dev.movingcastle.thaumicconcilium.network.PontifexTogglePayload;
import dev.movingcastle.thaumicconcilium.registry.TCItems;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Client side of the Pontifex's robe: the model hook, the dye tint and the toggle key (L, rebindable). */
@EventBusSubscriber(modid = ThaumicConcilium.MODID, value = Dist.CLIENT)
public final class PontifexClient {
    public static final KeyMapping TOGGLE = new KeyMapping("key.thaumicconcilium.pontifex_toggle", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_L, "key.categories.thaumicconcilium");

    private PontifexClient() {
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        while (TOGGLE.consumeClick()) {
            if (mc.screen == null && PontifexRobe.isFullSet(mc.player)) {
                PacketDistributor.sendToServer(new PontifexTogglePayload());
            }
        }
    }

    /** Item colour for the icon's base layer (the overlay layer is not tinted). */
    public static int iconTint(ItemStack stack, int tintIndex) {
        return tintIndex == 0 ? 0xFF000000 | DyedItemColor.getOrDefault(stack, PontifexRobe.DEFAULT_COLOR) : 0xFFFFFFFF;
    }

    /** Registered for the four pieces in {@link TCClient#itemExtensions}. */
    public static final class Extensions implements IClientItemExtensions {
        private final HumanoidModel<?>[] models = new HumanoidModel<?>[4];

        @Override
        public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
            int i = slot.getIndex();
            if (models[i] == null) {
                models[i] = new PontifexRobeModel(Minecraft.getInstance().getEntityModels().bakeLayer(PontifexRobeModel.LAYER), slot);
            }
            return models[i];
        }

        @Override
        public int getDefaultDyeColor(ItemStack stack) {
            return 0xFF000000 | PontifexRobe.DEFAULT_COLOR;
        }
    }

    static net.minecraft.world.item.Item[] pieces() {
        return new net.minecraft.world.item.Item[]{TCItems.PONTIFEX_HELM.get(), TCItems.PONTIFEX_CHEST.get(), TCItems.PONTIFEX_LEGS.get(),
                TCItems.PONTIFEX_BOOTS.get()};
    }
}
