package com.gregnautics.tfchorses.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import icy.betterhorses.net.BhGears;
import icy.betterhorses.net.IHorseData;
import icy.betterhorses.net.entity.BhBreedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

/** Keep the selected gait when Icy suspends its automatic forward input for W/S. */
@Mixin(value = AbstractHorse.class, priority = 900)
public abstract class ManualGaitMixin {
    @WrapMethod(method = "getRiddenSpeed")
    private float tfci$manualGait(Player rider, Operation<Float> original) {
        float speed = original.call(rider);
        if ((Object)this instanceof BhBreedHorse horse) {
            int selected = IHorseData.of(horse).bh_getGaitGear();
            // Gear zero means no auto-forward, rather than unrestricted manual gallop.
            int gait = selected > 0 ? selected : BhGears.WALK_GEAR;
            if (((com.gregnautics.tfchorses.HorseStamina)horse).tfci$exhausted()) gait = Math.min(gait, BhGears.TROT_GEAR);
            // Icy's walk/trot/canter limits are absolute caps, so this is idempotent
            // when its auto-forward speed adjustment has already been applied.
            return BhGears.riddenSpeed(gait, speed);
        }
        return speed;
    }
}
