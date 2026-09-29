package net.iann.vanillareenchanted.client;

import net.iann.vanillareenchanted.enchantment.EchoingEdge;
import net.iann.vanillareenchanted.enchantment.LoyaltyReturn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class EchoingEdgeHud {
    private static final ResourceLocation PRESENT = texture("charge_present");
    private static final ResourceLocation MISSING = texture("charge_missing");

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted",
                "textures/gui/echoing_edge/" + name + ".png");
    }

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, ResourceLocation.fromNamespaceAndPath("iannvanillareenchanted","echoing_edge"),
                (graphics, delta) -> {
                    var mc=Minecraft.getInstance();
                    if (mc.player==null || mc.level==null || mc.options.hideGui || mc.player.isSpectator()) return;
                    var stack=mc.player.getMainHandItem();
                    boolean echo=EchoingEdge.level(stack)>0;
                    if (!echo && LoyaltyReturn.level(stack)==0) return;
                    var mode=mc.options.attackIndicator().get();
                    if (mode==AttackIndicatorStatus.OFF) return;
                    int count=echo ? EchoingEdge.charges(stack,mc.level.getGameTime())
                            : LoyaltyReturn.charges(stack,mc.level.getGameTime());
                    int x=graphics.guiWidth()/2-8;
                    int y=mode==AttackIndicatorStatus.HOTBAR ? graphics.guiHeight()-28 : graphics.guiHeight()/2+24;
                    for(int i=0;i<3;i++) {
                        graphics.blit(i < count ? PRESENT : MISSING,
                                x + i * 6, y, 0, 0, 4, 4, 4, 4);
                    }
                });
    }
}
