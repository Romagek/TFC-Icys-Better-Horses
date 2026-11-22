package com.gregnautics.tfchorses.mixin;

import com.gregnautics.tfchorses.ManualGaitControl;
import icy.betterhorses.net.BhGears;
import icy.betterhorses.net.IHorseData;
import icy.betterhorses.net.client.HorseGearController;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HorseGearController.class)
public abstract class ManualGaitControllerMixin implements ManualGaitControl {
    @Shadow private int selectedGear;
    @Shadow private int appliedGear;
    @Shadow private int gearedHorseId;
    @Shadow private void apply(AbstractHorse horse, int gear) { throw new AssertionError(); }
    @Unique private boolean tfci$manual;
    @Override public boolean tfci$isManual() { return tfci$manual; }

    @Override public int tfci$shiftManual(AbstractHorse horse, int direction) {
        if (horse.getId() != gearedHorseId) {
            gearedHorseId = horse.getId();
            selectedGear = BhGears.WALK_GEAR;
        }
        tfci$manual = true;
        selectedGear = Math.clamp(Math.max(BhGears.WALK_GEAR, selectedGear) + direction,
            BhGears.WALK_GEAR, BhGears.TOP_GEAR);
        apply(horse, 0);
        return selectedGear;
    }
    @Inject(method="shift", at=@At("HEAD"))
    private void tfci$auto(AbstractHorse horse, int by, CallbackInfoReturnable<Integer> ci) {
        tfci$manual = false;
    }
    @Inject(method="reset", at=@At("HEAD"))
    private void tfci$reset(CallbackInfo ci) { tfci$manual = false; }
    @Inject(method="isEngaged", at=@At("HEAD"), cancellable=true)
    private void tfci$engaged(CallbackInfoReturnable<Boolean> ci) {
        if (tfci$manual) ci.setReturnValue(false);
    }
    @Inject(method="tick", at=@At("HEAD"), cancellable=true)
    private void tfci$tick(boolean eligible, AbstractHorse horse, boolean forward, boolean back,
            CallbackInfoReturnable<HorseGearController.Output> ci) {
        if (!tfci$manual || !eligible || horse == null || horse.getId() != gearedHorseId) return;
        var data = IHorseData.of(horse);
        if (appliedGear != 0 || data.bh_getGear() != 0 || data.bh_getGaitGear() != selectedGear)
            apply(horse, 0);
        ci.setReturnValue(HorseGearController.Output.PASS_THROUGH);
    }
}
