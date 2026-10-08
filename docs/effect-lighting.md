# Minecraft lighting for AAA effects

`client.particle.EffectLighting` is a client-only helper shared by any AAA effect.
It samples Minecraft's lightmap at one block position per emitter. That includes
day/night, block light, weather, dimension lighting, brightness settings, and
night vision. It does not provide per-particle lighting, shader-pack shadows,
or automatic compatibility with dynamic-light mods.

## Material setup

Use `assets/iannvanillareenchanted/effeks/updraft/updraft_lighting.efkmat` as a
reusable material template. It multiplies texture RGB and particle RGB by
Custom Data 1 RGB; alpha remains texture alpha times particle alpha.

The Updraft material also snaps UVs to pixel centres to keep its pixel art sharp
in the custom-material rendering path. Its texture-size constant is currently
8 by 64 pixels (the whole dust sheet, not one frame). Change that constant when
reusing the material with a differently sized texture.

For each visible sprite node in Effekseer:

1. Select the custom material (Material = File), rather than built-in Lighting.
2. Assign its Dust texture parameter to the desired sprite sheet.
3. Set Custom Data 1 to Dynamic Input.
4. Reserve dynamic inputs 0, 1, and 2 for light RGB. Input 3 is available for other uses.
5. For editor previews, set those three inputs to 1 for full brightness.

Package the material and textures alongside the effect, retaining the relative
paths recorded by Effekseer. Replacing an effect with a new export must retain
these material and Custom Data settings.

## Code setup

After creating an emitter, call this on the Minecraft client thread:

```java
EffectLighting.track(emitter, clientLevel, BlockPos.containing(x, y + 0.8, z));
```

For a moving emitter, call `track` again when its sampling position changes.
For a stationary effect, one call is enough: lighting continues updating once
per client tick until the emitter ends. Stopped emitters are removed automatically;
world changes and disconnects clear all lighting registrations. `untrack(emitter)`
stops lighting updates without stopping the emitter.

The effect owner remains responsible for stopping its effects on cancellation or
world changes. The lighting helper never changes their position or lifetime.

## Updraft

Both `updraft_tornado` and `updraft_jump` use this helper and material.
The charging tornado samples near the player. After full charge its existing
particles remain at their final position and continue receiving lighting updates.
The jump effect samples at its launch position. Trigger 0 on every tornado node
stops spawning at full charge; preserve it when editing the tornado.
