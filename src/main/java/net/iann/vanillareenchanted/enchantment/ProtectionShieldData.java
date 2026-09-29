package net.iann.vanillareenchanted.enchantment;

/** A separate, non-persistent pool: never converted to health or vanilla absorption. */
public final class ProtectionShieldData {
    private float current;
    private float capacity;
    private int rechargeDelay;
    private boolean dirty;

    public float current() { return current; }
    public float capacity() { return capacity; }

    public void updateCapacity(float newCapacity, int delayTicks) {
        newCapacity = Math.max(0, newCapacity);
        if (capacity == newCapacity) return;
        if (newCapacity > capacity) rechargeDelay = delayTicks;
        capacity = newCapacity;
        current = Math.min(current, capacity);
        if (capacity == 0) rechargeDelay = 0;
        dirty = true;
    }

    public void interruptRecharge(int delayTicks) {
        rechargeDelay = delayTicks;
    }

    /** Returns the damage left for vanilla absorption and health. */
    public float absorb(float damage) {
        if (damage <= 0) return damage;
        float absorbed = Math.min(current, damage);
        if (absorbed > 0) {
            current -= absorbed;
            dirty = true;
        }
        return damage - absorbed;
    }

    public void tick(float hpPerTick) {
        if (rechargeDelay > 0) {
            rechargeDelay--;
            return;
        }
        if (current < capacity && hpPerTick > 0) {
            current = Math.min(capacity, current + hpPerTick);
            dirty = true;
        }
    }

    public boolean isDirty() { return dirty; }
    public void markSynced() { dirty = false; }

    public void applySnapshot(float current, float capacity) {
        this.capacity = Math.max(0, capacity);
        this.current = Math.max(0, Math.min(current, this.capacity));
    }
}
