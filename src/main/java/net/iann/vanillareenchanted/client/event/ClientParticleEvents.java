package net.iann.vanillareenchanted.client.event;

import net.iann.vanillareenchanted.client.particle.RestingMendingParticle;
import net.iann.vanillareenchanted.registry.ModParticles;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

public class ClientParticleEvents {
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SWEEP_SLASH.get(), net.iann.vanillareenchanted.client.particle.SweepSlashParticle.Provider::new);
        event.registerSpriteSet(ModParticles.ECHO_IMPACT_RING.get(), net.iann.vanillareenchanted.client.particle.EchoImpactRingParticle.Provider::new);
        event.registerSpriteSet(
                ModParticles.RESTING_MENDING.get(),
                RestingMendingParticle.Provider::new
        );
    }
}