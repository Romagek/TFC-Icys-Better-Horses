package com.gregnautics.tfchorses;

public interface HorseStamina {
    float tfci$stamina();
    boolean tfci$exhausted();
    void tfci$stamina(float amount, boolean exhausted);
}
