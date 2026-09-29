package net.iann.vanillareenchanted.enchantment;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Owned by one player's ItemCooldowns, never shared between client and server. */
public final class RiptideCooldown {
    public static final int DURATION = 8 * 20;
    public int remaining;
    public boolean waiting, spinning, water;
    public AABB previousBox;
    public Vec3 direction = Vec3.ZERO;

    public void start() { remaining = DURATION; waiting = true; spinning = true; }
    public void tick(boolean grounded, boolean inWater) {
        if (remaining <= 0) return;
        if (waiting && !spinning && (grounded || inWater)) waiting = false;
        if (!waiting) remaining = Math.max(0, remaining - (inWater ? 2 : 1));
    }
    public float fraction(float partial) {
        return Math.clamp((remaining - (waiting ? 0 : partial * (water ? 2 : 1))) / DURATION, 0, 1);
    }
    public interface Access { RiptideCooldown vr$riptideCooldown(); }
}
