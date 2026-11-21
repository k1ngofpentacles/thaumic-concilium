package dev.movingcastle.thaumicconcilium.client;

import com.leclowndu93150.thaumaturge.api.spell.Spell;
import com.leclowndu93150.thaumaturge.api.spell.Spells;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.movingcastle.thaumicconcilium.item.BraceletKind;
import dev.movingcastle.thaumicconcilium.item.CastingBracelet;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * The bracelet model, drawn round a player arm in arm-model space (pixels, the arm's pivot at the shoulder, Y down to
 * the hand at about 10): a band of the bracelet's material between two rims of its cap metal, and on the back of the
 * wrist a raised cap-metal setting holding a gem that glows in the socketed focus's colour (dull grey with no focus).
 * Skinned at run time from block-atlas sprites, so no other mod's art is shipped.
 * <p>
 * Used by {@link BraceletArmLayer} (third person) and {@link BraceletFirstPerson}.
 */
public final class BraceletBandRenderer {
    /** The wrist: just above the fist, which is the arm's last three pixels (the arm ends at 10). */
    private static final float BAND_TOP = 5.2F;
    private static final float BAND_BOTTOM = 7.6F;
    private static final float RIM = 0.45F;
    /** Over the sleeve (which is 0.25 px out) and under armour (1 px). */
    private static final float INFLATE = 0.35F;
    private static final ResourceLocation GEM = ResourceLocation.withDefaultNamespace("block/white_concrete");
    private static final Map<Spell, Integer> COLORS = new WeakHashMap<>();

    private BraceletBandRenderer() {
    }

    /** The colour of the bracelet's socketed focus, cached per spell. */
    public static int gemColor(ItemStack bracelet) {
        if (!(bracelet.getItem() instanceof CastingBracelet item) || Minecraft.getInstance().level == null) {
            return -1;
        }
        Spell spell = Spells.spellOf(item.socketedFocus(bracelet));
        if (spell == null) {
            return -1;
        }
        return COLORS.computeIfAbsent(spell, s -> Spells.color(Minecraft.getInstance().level.registryAccess(), s));
    }

    /**
     * Draws the bracelet on an arm whose model part transform is already applied to {@code pose}.
     *
     * @param gem the gem colour (RGB), or -1 for no focus
     */
    public static void draw(PoseStack pose, MultiBufferSource buffers, BraceletKind kind, HumanoidArm arm, boolean slim,
                            int light, int gem) {
        int width = slim ? 3 : 4;
        // Arm cubes: right arm x -3..1 (slim -2..1), left arm x -1..3 (slim -1..2); z -2..2.
        float x0 = arm == HumanoidArm.RIGHT ? 1 - width : -1;
        float x1 = x0 + width;
        int lit = kind.glow() ? LightTexture.FULL_BRIGHT : light;
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite band = atlas.apply(kind.band());
        TextureAtlasSprite rim = atlas.apply(kind.rim());
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        pose.pushPose();
        pose.scale(1 / 16.0F, 1 / 16.0F, 1 / 16.0F);
        Matrix4f m = pose.last().pose();
        float i = INFLATE;
        box(vc, pose, m, x0 - i, BAND_TOP + RIM, -2 - i, x1 + i, BAND_BOTTOM - RIM, 2 + i, band, lit, -1);
        float r = INFLATE + 0.18F;
        box(vc, pose, m, x0 - r, BAND_TOP, -2 - r, x1 + r, BAND_TOP + RIM, 2 + r, rim, lit, -1);
        box(vc, pose, m, x0 - r, BAND_BOTTOM - RIM, -2 - r, x1 + r, BAND_BOTTOM, 2 + r, rim, lit, -1);
        // The setting, on the outside of the arm (the back of the wrist), and its gem.
        float mid = (BAND_TOP + BAND_BOTTOM) / 2.0F;
        boolean right = arm == HumanoidArm.RIGHT;
        float face = right ? x0 - i : x1 + i;
        float out = right ? -1.0F : 1.0F;
        box(vc, pose, m, Math.min(face, face + out * 0.4F), mid - 0.9F, -0.9F, Math.max(face, face + out * 0.4F), mid + 0.9F, 0.9F,
                rim, lit, -1);
        float g0 = face + out * 0.4F;
        float g1 = face + out * 0.75F;
        int gemColor = gem < 0 ? 0x5A5A64 : gem;
        box(vc, pose, m, Math.min(g0, g1), mid - 0.5F, -0.5F, Math.max(g0, g1), mid + 0.5F, 0.5F, atlas.apply(GEM),
                gem < 0 ? lit : LightTexture.FULL_BRIGHT, gemColor);
        pose.popPose();
    }

    /** An axis-aligned box, each face mapped onto a sprite region proportional to its size. */
    private static void box(VertexConsumer vc, PoseStack pose, Matrix4f m, float x0, float y0, float z0, float x1, float y1,
                            float z1, TextureAtlasSprite s, int light, int rgb) {
        int color = rgb < 0 ? 0xFFFFFFFF : 0xFF000000 | rgb;
        // -Z, +Z, -X, +X, -Y (towards the shoulder), +Y
        quad(vc, pose, m, s, light, color, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1 - x0, y1 - y0);
        quad(vc, pose, m, s, light, color, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, x1 - x0, y1 - y0);
        quad(vc, pose, m, s, light, color, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, z1 - z0, y1 - y0);
        quad(vc, pose, m, s, light, color, 1, 0, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, z1 - z0, y1 - y0);
        quad(vc, pose, m, s, light, color, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, x1 - x0, z1 - z0);
        quad(vc, pose, m, s, light, color, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, x1 - x0, z1 - z0);
    }

    private static void quad(VertexConsumer vc, PoseStack pose, Matrix4f m, TextureAtlasSprite s, int light, int color,
                             float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, float w, float h) {
        float u0 = s.getU(0.0F);
        float v0 = s.getV(0.0F);
        float u1 = s.getU(Math.min(1.0F, w / 16.0F));
        float v1 = s.getV(Math.min(1.0F, h / 16.0F));
        vertex(vc, pose, m, ax, ay, az, u0, v0, light, color, nx, ny, nz);
        vertex(vc, pose, m, bx, by, bz, u1, v0, light, color, nx, ny, nz);
        vertex(vc, pose, m, cx, cy, cz, u1, v1, light, color, nx, ny, nz);
        vertex(vc, pose, m, dx, dy, dz, u0, v1, light, color, nx, ny, nz);
    }

    private static void vertex(VertexConsumer vc, PoseStack pose, Matrix4f m, float x, float y, float z, float u, float v,
                               int light, int color, float nx, float ny, float nz) {
        vc.addVertex(m, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose.last(), nx, ny, nz);
    }
}
