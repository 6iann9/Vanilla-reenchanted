package net.iann.vanillareenchanted.client;

import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.animation.PlayerAnimResources;
import com.zigythebird.playeranim.animation.PlayerRawAnimationBuilder;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractFadeModifier;
import com.zigythebird.playeranimcore.animation.layered.modifier.MirrorModifier;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.easing.EasingType;
import com.zigythebird.playeranimcore.enums.PlayState;
import net.iann.vanillareenchanted.enchantment.WindUp;
import net.iann.vanillareenchanted.network.SyncWindUpPayload;
import net.iann.vanillareenchanted.network.WindUpPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;

/** A dedicated PAL layer: only the weapon arm and its item are animated. */
public final class WindUpAnimations {
    private static final ResourceLocation LAYER = id("wind_up");
    private static final ResourceLocation CHARGE = id("mace_charge");
    private static final ResourceLocation RELEASE = id("mace_hit");
    private static final ResourceLocation HOLD = id("mace_hold");
    private static final int RELEASE_TICKS = 12;

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted", path);
    }

    public static void register() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 1500, Controller::new);
    }

    private static final class Controller extends PlayerAnimationController {
        private final MirrorModifier mirror = new MirrorModifier();
        private int phase = WindUpPayload.CANCEL;
        private long releaseUntil;

        private Controller(AbstractClientPlayer player) {
            super(player, (controller, state, setter) -> PlayState.STOP);
            setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
            setFirstPersonFollowsCamera(true);
            setFirstPersonTransitionLength(3);
            setFirstPersonConfiguration(new FirstPersonConfiguration()
                    .setShowRightArm(true).setShowLeftArm(true)
                    .setShowRightItem(true).setShowLeftItem(true));
            addModifierLast(mirror);
        }

        private void play(int action, int elapsed) {
            mirror.enabled = getPlayer().getMainArm() == HumanoidArm.LEFT;
            removeModifierIf(modifier -> modifier instanceof AbstractFadeModifier);
            if (action == WindUpPayload.START && PlayerAnimResources.hasAnimation(CHARGE) && PlayerAnimResources.hasAnimation(HOLD)) {
                phase = action;
                if (elapsed >= WindUp.CHARGE_TICKS) {
                    triggerAnimation(HOLD);
                } else {
                    triggerAnimation(PlayerRawAnimationBuilder.begin().thenPlay(CHARGE).thenLoop(HOLD).build(),
                            Math.max(elapsed, 0));
                }
                if (elapsed == 0) addModifierLast(AbstractFadeModifier.standardFadeIn(3, EasingType.LINEAR));
            } else if (action == WindUpPayload.RELEASE && PlayerAnimResources.hasAnimation(RELEASE)) {
                phase = action;
                releaseUntil = getPlayer().level().getGameTime() + RELEASE_TICKS;
                // Blend from the actual pose, including a partially charged early release.
                replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(2, EasingType.LINEAR), RELEASE);
                addModifierLast(AbstractFadeModifier.standardFadeOut(4, EasingType.LINEAR));
            } else {
                phase = WindUpPayload.CANCEL;
                stopTriggeredAnimation();
                stop();
            }
        }
    }

    public static void play(AbstractClientPlayer player, int action, int elapsed) {
        if (PlayerAnimationAccess.getPlayerAnimationLayer(player, LAYER) instanceof Controller controller) {
            controller.play(action, elapsed);
        }
    }

    public static void receive(SyncWindUpPayload payload) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (mc.level.getEntity(payload.playerId()) instanceof AbstractClientPlayer player) {
            if (player == mc.player && payload.action() == WindUpPayload.CANCEL) WindUpClient.reject();
            play(player, payload.action(), payload.elapsed());
        }
    }

    public static void tick() {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        for (var player : level.players()) {
            if (PlayerAnimationAccess.getPlayerAnimationLayer(player, LAYER) instanceof Controller controller) {
                controller.mirror.enabled = player.getMainArm() == HumanoidArm.LEFT;
                if (controller.phase != WindUpPayload.CANCEL && (!player.isAlive()
                        || WindUp.level(player.getMainHandItem()) == 0
                        || controller.phase == WindUpPayload.RELEASE && level.getGameTime() >= controller.releaseUntil)) {
                    controller.play(WindUpPayload.CANCEL, 0);
                }
            }
        }
    }
}
