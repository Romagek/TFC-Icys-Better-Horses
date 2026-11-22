package com.gregnautics.tfchorses.mixin;

import com.gregnautics.tfchorses.ManualGaitControl;
import icy.betterhorses.net.client.HorseAutodriveController;
import icy.betterhorses.net.client.HorseGearController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HorseAutodriveController.class)
public abstract class ManualAutodriveMixin {
    @Shadow public abstract void reset();
    @Inject(method="tick", at=@At("HEAD"), cancellable=true)
    private void tfci$manual(long tick, boolean eligible, int horseId, boolean forward,
            boolean back, boolean left, boolean right, float forwardImpulse, float leftImpulse,
            CallbackInfoReturnable<HorseAutodriveController.Output> ci) {
        if (((ManualGaitControl)(Object)HorseGearController.INSTANCE).tfci$isManual()) {
            reset();
            ci.setReturnValue(new HorseAutodriveController.Output(false, forward, back, left, right,
                forwardImpulse, leftImpulse));
        }
    }
}
