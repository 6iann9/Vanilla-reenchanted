# Photon effect migration

The development project includes Photon 2.2.9, LDLib2 2.2.43 (all), and KilaGraph
21.1.0.15 from the official FirstDark Maven repository. Versions are pinned in
`gradle.properties`. LDLib2 bundles its Kotlin, Yoga, and Taffy support libraries.

Reload the Gradle project in the IDE, restart the development client, enter a
creative world, and run `/photon_editor` to start creating effects.

Updraft charging now uses `vr_effects:updraft_tornado`, imported from
`run/photon/fxpacks/VR_effects.fxpack` with its material and dust texture. The
effect follows the player, stops emitting at full charge and lets live particles
finish. Cancellation removes the charging effect immediately.

Updraft's jump burst uses `vr_effects:updraft_jump` at the takeoff position, including its timeline audio. Horsekeeper still uses AAA Particles. Both Photon and AAA
are required client dependencies; neither is bundled into the output mod jar.

When migrating an effect, add its Photon assets and replace its client playback
code while retaining the server's ability timing and tracking behavior.
Remove AAA only after all its playback code, imports, and assets have migrated.

## Required cleanup for current and future Photon effects

Use the client-only `client.particle.PhotonEffects` helper instead of calling
`FXRuntime.destroy(...)` directly:

- `PhotonEffects.finish(runtime)`: stop audio and emission, letting existing
  particles finish. Keep tracking the runtime until `isFinished()` if needed.
- `PhotonEffects.stop(runtime)`: stop audio and remove particles immediately.
  Use for cancellation, owner removal, playback errors, and world/disconnect cleanup.

Both methods accept null. They disable timeline audio dispatch before destroying
the runtime. This prevents cleanup ticks from restarting looping audio in Photon
2.2.8. The workaround was confirmed in-game for Updraft cancellation and full
charge. All future Photon integrations should use these helpers for termination.
Create a fresh runtime for each new playback; a terminated runtime has audio
dispatch disabled and should not be reused.

Keep this workaround until an upstream fix has been verified for both cancellation
and graceful completion with an audio clip still active.

Documentation: https://low-drag-mc.github.io/LowDragMC-Doc/en/photon2/
Java integration: https://low-drag-mc.github.io/LowDragMC-Doc/en/photon2/java-api/

Dependency requirements were inspected from the published jars. No Gradle build
or game launch was run for this dependency addition.
