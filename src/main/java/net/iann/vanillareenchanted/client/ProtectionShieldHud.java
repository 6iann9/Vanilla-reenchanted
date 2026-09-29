package net.iann.vanillareenchanted.client;

import net.iann.vanillareenchanted.VanillaReenchanted;
import net.iann.vanillareenchanted.config.ShieldHudConfig;
import net.iann.vanillareenchanted.enchantment.ProtectionShieldData;
import net.iann.vanillareenchanted.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class ProtectionShieldHud {
    private static final ResourceLocation FULL = texture("full");
    private static final ResourceLocation HALF = texture("half");
    private static final ResourceLocation FULL_BLINKING = texture("full_blinking");
    private static final ResourceLocation HALF_BLINKING = texture("half_blinking");
    private static final ResourceLocation CONTAINER = texture("container");
    private static final ResourceLocation CONTAINER_BLINKING = texture("container_blinking");
    private static ProtectionShieldData lastData;
    private static float lastShield;
    private static float lastCapacity;
    private static float previousShield;
    private static int flashUntil;

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(VanillaReenchanted.MODID,
                "textures/gui/shield_heart/" + name + ".png");
    }

    public static void register(RegisterGuiLayersEvent event) {
        // Render after health and before armor, reserving rows through NeoForge's shared HUD height.
        event.registerBelow(VanillaGuiLayers.ARMOR_LEVEL,
                ResourceLocation.fromNamespaceAndPath(VanillaReenchanted.MODID, "protection_shield"),
                (graphics, deltaTracker) -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (!ShieldHudConfig.ENABLED.get() || mc.options.hideGui || mc.player == null
                            || mc.gameMode == null || !mc.gameMode.canHurtPlayer() || !mc.player.isAlive()) return;
                    ProtectionShieldData shield = mc.player.getData(ModAttachments.PROTECTION_SHIELD);
                    int tick = mc.player.tickCount;
                    if (shield != lastData || shield.capacity() != lastCapacity) {
                        lastData = shield;
                        lastShield = shield.current();
                        previousShield = shield.current();
                        flashUntil = 0;
                    } else if (shield.current() < lastShield) {
                        previousShield = lastShield;
                        flashUntil = tick + 20;
                    }
                    lastShield = shield.current();
                    lastCapacity = shield.capacity();
                    if (shield.capacity() <= 0) return;

                    boolean blink = tick < flashUntil && ((flashUntil - tick) / 3) % 2 == 1;
                    int hearts = Mth.ceil(shield.capacity() / 2.0F);
                    int rows = (hearts + 9) / 10;
                    int x = graphics.guiWidth() / 2 + ShieldHudConfig.X_OFFSET.get();
                    boolean autoStack = ShieldHudConfig.AUTO_STACK.get();
                    int y = graphics.guiHeight() - (autoStack ? mc.gui.leftHeight : 60)
                            + ShieldHudConfig.Y_OFFSET.get();
                    int visibleHp = Mth.ceil(Math.min(shield.capacity(), blink ? previousShield : shield.current()));
                    for (int heart = 0; heart < hearts; heart++) {
                        int heartX = x + (heart % 10) * 8;
                        int heartY = y - (heart / 10) * 10;
                        graphics.blit(blink ? CONTAINER_BLINKING : CONTAINER,
                                heartX, heartY, 0, 0, 9, 9, 9, 9);
                        int hp = visibleHp - heart * 2;
                        if (hp > 0) {
                            ResourceLocation fill = hp >= 2
                                    ? (blink ? FULL_BLINKING : FULL)
                                    : (blink ? HALF_BLINKING : HALF);
                            graphics.blit(fill, heartX, heartY, 0, 0, 9, 9, 9, 9);
                        }
                    }
                    if (autoStack) mc.gui.leftHeight += rows * 10;
                });
    }
}
