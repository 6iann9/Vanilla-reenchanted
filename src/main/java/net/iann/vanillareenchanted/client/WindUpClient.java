package net.iann.vanillareenchanted.client;

import net.iann.vanillareenchanted.enchantment.WindUp;
import net.iann.vanillareenchanted.network.WindUpPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="iannvanillareenchanted", value=Dist.CLIENT)
public final class WindUpClient {
    private static boolean charging;
    private static boolean held;
    private static int chargeStartedTick;
    private static ItemStack chargingStack;
    public static boolean charging() { return charging; }

    /** HUD-only progress; never changes attack damage or the hand animation. */
    public static float indicatorProgress(float partialTick) {
        var player = Minecraft.getInstance().player;
        if (player == null) return 1;
        if (charging) {
            float progress = Math.clamp((player.tickCount - chargeStartedTick + partialTick) / WindUp.CHARGE_TICKS, 0F, 1F);
            // Vanilla draws a complete 16px/18px bar just below 1, keeping it visible during the hold.
            return Math.min(progress, Math.nextDown(1F));
        }
        return WindUp.cooldownProgress(player, partialTick);
    }

    /** Stop a rejected local prediction without sending another cancellation packet. */
    public static void reject() {
        charging = false;
        chargingStack = null;
        var player = Minecraft.getInstance().player;
        if (player != null) player.stopUsingItem();
    }

    private static void begin(Minecraft mc) {
        if (mc.player != null) WindUp.checkEquipCooldown(mc.player);
        if (charging || mc.player == null || mc.gameMode == null || !mc.player.isAlive()
                || mc.player.isSpectator() || mc.player.isUsingItem()
                || WindUp.level(mc.player.getMainHandItem()) == 0
                || WindUp.isCoolingDown(mc.player) || mc.player.getCooldowns().isOnCooldown(mc.player.getMainHandItem().getItem())) return;
        charging = true;
        chargeStartedTick = mc.player.tickCount;
        chargingStack = mc.player.getMainHandItem();
        mc.gameMode.stopDestroyBlock();
        mc.player.connection.send(new ServerboundSetCarriedItemPacket(mc.player.getInventory().selected));
        PacketDistributor.sendToServer(new WindUpPayload(WindUpPayload.START));
        mc.player.startUsingItem(InteractionHand.MAIN_HAND);
        WindUpAnimations.play(mc.player, WindUpPayload.START, 0);
    }

    private static void finish(Minecraft mc, boolean release) {
        if (!charging) return;
        charging = false;
        if (mc.player != null) {
            PacketDistributor.sendToServer(new WindUpPayload(release ? WindUpPayload.RELEASE : WindUpPayload.CANCEL));
            WindUpAnimations.play(mc.player, release ? WindUpPayload.RELEASE : WindUpPayload.CANCEL, 0);
            mc.player.stopUsingItem();
            if (release && chargingStack != null) {
                mc.player.resetAttackStrengthTicker();
                WindUp.startCooldown(mc.player, WindUp.COOLDOWN_TICKS);
            }
        }
        chargingStack = null;
    }

    @SubscribeEvent
    public static void onInput(InputEvent.InteractionKeyMappingTriggered event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (event.isAttack() && WindUp.level(mc.player.getMainHandItem()) > 0) {
            event.setCanceled(true);
            event.setSwingHand(false);
            if (!held && mc.screen == null) begin(mc);
            held = true;
        } else if (charging && event.isUseItem()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        boolean down = mc.options.keyAttack.isDown();
        if (mc.player == null || mc.level == null) {
            charging = false; chargingStack = null; held = down;
            return;
        }
        WindUpAnimations.tick();
        WindUp.checkEquipCooldown(mc.player);
        if (mc.screen != null || !mc.isWindowActive() || !mc.player.isAlive()
                || charging && (mc.player.getMainHandItem() != chargingStack || WindUp.level(chargingStack) == 0)) {
            finish(mc, false);
        } else if (!down) {
            finish(mc, true);
        } else if (!charging) {
            // A held attack starts as soon as begin() accepts it, including after cooldown.
            begin(mc);
        }
        held = down;
    }

}
