package com.gregnautics.tfchorses;

/** Per loaded game second; never drains from a selected gait while stationary. */
public final class StaminaRules {
    public static final float MAX = 100, RECOVER_THRESHOLD = 30;
    public record State(float amount, boolean exhausted) {}
    public static State step(float amount, boolean exhausted, int selectedGait, boolean moving, float seconds) {
        if (!Float.isFinite(amount)) amount = MAX;
        int gait = exhausted ? Math.min(selectedGait, 2) : selectedGait;
        float rate = !moving ? 5 : gait >= 4 ? -4 : gait == 3 ? -1.5f : gait == 2 ? 1 : 3;
        amount = Math.clamp(amount + rate * seconds, 0, MAX);
        exhausted = amount <= 0 || exhausted && amount < RECOVER_THRESHOLD;
        return new State(amount, exhausted);
    }
}
