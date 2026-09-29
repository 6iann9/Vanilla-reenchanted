package net.iann.vanillareenchanted.enchantment;

import net.minecraft.util.Mth;

/** Transient straight-line charge; steering counts absolute turns, including zigzags. */
public final class MomentumState {
    public static final int ACTIVATION_TICKS = 40;
    public static final int STEERING_WINDOW_TICKS = 20;
    public static final float MAX_TURN_DEGREES = 30;
    private final float[] turns = new float[STEERING_WINDOW_TICKS];
    private int index;
    private int charge;
    private float totalTurn;
    private float lastYaw;
    private boolean initialized;

    public boolean update(float yaw, boolean eligible) {
        if (!eligible) { reset(); return false; }
        float turn = initialized ? Math.abs(Mth.wrapDegrees(yaw - lastYaw)) : 0;
        initialized = true;
        lastYaw = yaw;
        totalTurn += turn - turns[index];
        turns[index] = turn;
        index = (index + 1) % turns.length;
        if (totalTurn > MAX_TURN_DEGREES) {
            // The turn cancels this run; do not keep penalizing the fresh run for the same turn.
            reset();
            lastYaw = yaw;
            initialized = true;
            return false;
        }
        charge = Math.min(ACTIVATION_TICKS, charge + 1);
        return active();
    }

    public boolean active() { return charge >= ACTIVATION_TICKS; }
    public void reset() {
        java.util.Arrays.fill(turns, 0);
        index = charge = 0;
        totalTurn = 0;
        initialized = false;
    }

    public static double additionalMultiplier(int level, double potionBonus) {
        double bonus = 0.2 * Math.min(2, Math.max(0, level));
        return (1 + Math.max(bonus, potionBonus)) / (1 + potionBonus) - 1;
    }
}
