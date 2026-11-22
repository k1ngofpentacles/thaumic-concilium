package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.item.CrimsonDagger;
import dev.movingcastle.thaumicconcilium.registry.TCItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * Client wiring of the Crimson Dagger's animation (mod bus): the item property {@code thaumicconcilium:grip} that picks the
 * item model (0 forward, 0.5 inline, 1 reverse; data only, see the generator), and the first-person hand transform.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DaggerClient {
    private DaggerClient() {
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(TCItems.CRIMSON_DAGGER.get(), ThaumicConcilium.id("grip"),
                (stack, level, entity, seed) -> {
                    if (entity == null) {
                        return 0.0F;
                    }
                    float pt = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
                    return DaggerPoses.ACTIVE.grip(DaggerAnim.frame(entity, pt));
                }));
    }

    @SubscribeEvent
    public static void extensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                                   float partialTick, float equipProcess, float swingProcess) {
                DaggerAnim.Frame frame = DaggerAnim.frame(player, partialTick);
                if (frame.mode() == DaggerAnim.Mode.REST || !(stack.getItem() instanceof CrimsonDagger)) {
                    return false;
                }
                float s = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
                // Vanilla's resting arm position (ItemInHandRenderer.applyItemArmTransform), then the option's offsets.
                pose.translate(s * 0.56F, -0.52F + equipProcess * -0.6F, -0.72F);
                return DaggerPoses.ACTIVE.firstPerson(pose, s, frame);
            }
        }, TCItems.CRIMSON_DAGGER.get());
    }
}
