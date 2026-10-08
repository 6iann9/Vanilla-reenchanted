package net.iann.vanillareenchanted.client;

import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.animation.PlayerAnimResources;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractFadeModifier;
import com.zigythebird.playeranimcore.animation.layered.modifier.MirrorModifier;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.enums.FadeType;
import com.zigythebird.playeranimcore.enums.PlayState;
import net.iann.vanillareenchanted.enchantment.Bash;
import net.iann.vanillareenchanted.network.BashPayload;
import net.iann.vanillareenchanted.network.SyncBashPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid="iannvanillareenchanted", value=Dist.CLIENT)
public final class BashAnimations {
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted", "shield_bash");
    public static void register() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(Bash.ID, 1600, Controller::new);
    }

    private static final class Controller extends PlayerAnimationController {
        private final MirrorModifier mirror = new MirrorModifier();
        private int busyUntil;
        private boolean playing;
        private InteractionHand shieldHand;
        private Controller(AbstractClientPlayer player) {
            super(player, (controller, state, setter) -> PlayState.STOP);
            setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
            setFirstPersonFollowsCamera(true);
            setFirstPersonTransitionLength(2);
            setFirstPersonConfiguration(new FirstPersonConfiguration().setShowRightArm(false).setShowLeftArm(true)
                    .setShowRightItem(false).setShowLeftItem(true));
            addModifierLast(mirror);
        }
        private void play(boolean offhand) {
            shieldHand = offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            HumanoidArm arm = offhand ? getPlayer().getMainArm().getOpposite() : getPlayer().getMainArm();
            mirror.enabled = arm == HumanoidArm.RIGHT;
            busyUntil = getPlayer().tickCount + Bash.ANIMATION_TICKS;
            if (!PlayerAnimResources.hasAnimation(ANIMATION)) return;
            removeModifierIf(modifier -> modifier instanceof AbstractFadeModifier);
            triggerAnimation(ANIMATION);
            playing = true;
            // The exported clip holds its last frame. Fade after that hold, not during the strike.
            addModifierLast(new AbstractFadeModifier(2) {
                @Override protected float getAlpha(String boneName, float progress) { return progress; }
                @Override protected FadeType getFadeType() { return FadeType.FADE_OUT; }
                @Override protected float getEndTime(String boneName) { return Bash.ANIMATION_TICKS; }
            });
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        for (var player : level.players()) {
            if (PlayerAnimationAccess.getPlayerAnimationLayer(player, Bash.ID) instanceof Controller controller
                    && controller.playing && (!player.isAlive() || player.tickCount >= controller.busyUntil)) {
                controller.stopTriggeredAnimation();
                controller.stop();
                controller.playing = false;
            }
        }
    }

    public static boolean busy(AbstractClientPlayer player) {
        return player != null && PlayerAnimationAccess.getPlayerAnimationLayer(player, Bash.ID) instanceof Controller controller
                && player.tickCount < controller.busyUntil;
    }

    public static boolean animatingShield(LivingEntity entity, ItemStack stack) {
        return entity instanceof AbstractClientPlayer player
                && PlayerAnimationAccess.getPlayerAnimationLayer(player, Bash.ID) instanceof Controller controller
                && controller.playing && player.isAlive() && player.tickCount < controller.busyUntil
                && player.getItemInHand(controller.shieldHand) == stack && Bash.enchanted(stack);
    }

    public static void input() {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.screen != null || !mc.isWindowActive()) return;
        if (busy(player)) {
            while (mc.options.keyAttack.consumeClick()) { }
            return;
        }
        if (!player.isBlocking() || !Bash.enchanted(player.getUseItem())) return;
        boolean clicked = false;
        while (mc.options.keyAttack.consumeClick()) clicked = true;
        if (!clicked || player.getCooldowns().isOnCooldown(player.getUseItem().getItem())) return;
        if (mc.gameMode != null) mc.gameMode.stopDestroyBlock();
        if (PlayerAnimationAccess.getPlayerAnimationLayer(player, Bash.ID) instanceof Controller controller)
            controller.busyUntil = player.tickCount + Bash.ANIMATION_TICKS;
        PacketDistributor.sendToServer(BashPayload.INSTANCE);
    }

    public static void receive(SyncBashPayload payload) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getEntity(payload.playerId()) instanceof AbstractClientPlayer player)) return;
        if (PlayerAnimationAccess.getPlayerAnimationLayer(player, Bash.ID) instanceof Controller controller) controller.play(payload.offhand());
    }

    private BashAnimations() {}
}
