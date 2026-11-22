package com.gregnautics.tfchorses;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
public interface ManualGaitControl {
    boolean tfci$isManual();
    int tfci$shiftManual(AbstractHorse horse, int direction);
}
