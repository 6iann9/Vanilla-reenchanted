package net.iann.vanillareenchanted.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.iann.vanillareenchanted.enchantment.Updraft;
import net.iann.vanillareenchanted.network.UpdraftPayload;
import net.iann.vanillareenchanted.network.SyncUpdraftPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="iannvanillareenchanted", value=Dist.CLIENT)
public final class UpdraftClient {
    private static final ResourceLocation ARROW = ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted", "textures/gui/updraft/arrow_charge_indicator.png");
    private static LocalPlayer tracked;
    private static Vec3 previous;
    private static boolean wasCrouching, attempted;
    private static int stage, time, receivedAt;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        var p = mc.player;
        if (p != tracked) {
            tracked = p;
            previous = p == null ? null : p.position();
            wasCrouching = p != null && p.input.shiftKeyDown;
            attempted = false;
            stage = Updraft.IDLE;
            return;
        }
        if (p == null) return;
        boolean crouching = p.input.shiftKeyDown;
        // Read individual keys so opposing directions cannot cancel each other out.
        boolean movingInput = mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
                || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown() || p.input.jumping;
        boolean valid = !movingInput && Updraft.grounded(p) && mc.screen == null && mc.isWindowActive();
        boolean movedWhileCharging = stage == Updraft.CHARGING && !Updraft.stationary(p.position(), previous);
        if (attempted && (!valid || movedWhileCharging)) {
            send(Updraft.CANCEL);
            attempted = false;
            stage = Updraft.IDLE;
        }
        // Residual walking motion may settle during CHECKING; fresh movement input still cancels immediately.
        if (crouching && !wasCrouching && valid && stage == Updraft.IDLE) {
            send(Updraft.START);
            attempted = true;
        }
        if (!crouching && wasCrouching && attempted) {
            send(Updraft.RELEASE);
            attempted = false;
        }
        wasCrouching = crouching;
        previous = p.position();
    }

    private static void send(int action) { PacketDistributor.sendToServer(new UpdraftPayload(action)); }

    public static void receive(SyncUpdraftPayload payload) {
        var p = Minecraft.getInstance().player;
        if (p == null) return;
        if (p != tracked) {
            tracked = p;
            previous = p.position();
            wasCrouching = p.input.shiftKeyDown;
            attempted = false;
        }
        stage = payload.stage();
        time = payload.time();
        receivedAt = p.tickCount;
        if (stage == Updraft.IDLE || stage == Updraft.AIRBORNE || stage == Updraft.COOLDOWN) attempted = false;
    }

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, Updraft.ID, (graphics, delta) -> {
            var mc = Minecraft.getInstance();
            if (mc.player == null || mc.options.hideGui || !mc.options.getCameraType().isFirstPerson()
                    || mc.player.isSpectator() || Updraft.level(mc.player) == 0
                    || stage == Updraft.IDLE || stage == Updraft.AIRBORNE) return;
            float elapsed = mc.player.tickCount - receivedAt + delta.getGameTimeDeltaPartialTick(true);
            float progress = stage == Updraft.CHARGING ? (time + elapsed) / Updraft.CHARGE_TICKS
                    : stage == Updraft.COOLDOWN ? 1 - (time - elapsed) / Updraft.COOLDOWN_TICKS : 0;
            int width = 9, height = 11;
            int fill = (int)(height * Math.clamp(progress, 0F, 1F));
            int x = (graphics.guiWidth() - width) / 2, y = graphics.guiHeight() / 2 - 22;
            RenderSystem.enableBlend();
            // Match the vanilla crosshair attack indicator's background-sensitive blending.
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            try {
                // Under this blend mode RGB controls the strength of the see-through effect.
                // Draw each pixel once so the filled portion does not invert the background twice.
                graphics.setColor(0.25F, 0.25F, 0.25F, 0.8F);
                if (fill < height) graphics.blit(ARROW, x, y, 0, 0, width, height - fill, width, height);
                graphics.setColor(0.8F, 0.8F, 0.8F, 0.8F);
                if (fill > 0) graphics.blit(ARROW, x, y + height - fill, 0, height - fill, width, fill, width, height);
            } finally {
                graphics.setColor(1, 1, 1, 1);
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableBlend();
            }
        });
    }

    private UpdraftClient() {}
}
