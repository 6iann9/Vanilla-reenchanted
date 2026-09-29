package net.iann.vanillareenchanted.event;

import net.iann.vanillareenchanted.enchantment.EchoingEdge;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public final class EchoingEdgeEvents {
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        var attack = EchoingEdge.current();
        if (attack != null && attack.activeTarget == event.getEntity()
                && event.getSource().is(DamageTypes.PLAYER_ATTACK) && event.getSource().getDirectEntity() == attack.player
                && (event.getNewDamage() > 0 || event.getReduction(DamageContainer.Reduction.ABSORPTION) > 0)) {
            attack.hits.add(event.getEntity());
        }
    }
}
