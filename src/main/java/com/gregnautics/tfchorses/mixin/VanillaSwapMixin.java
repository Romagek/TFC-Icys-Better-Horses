package com.gregnautics.tfchorses.mixin;
import icy.betterhorses.net.BhVanillaHorseSwap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.Horse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=BhVanillaHorseSwap.class, remap=false)
public abstract class VanillaSwapMixin {
    @Inject(method="trySwap",at=@At("HEAD"),cancellable=true)
    private static void tfci$climateSwap(Entity entity,CallbackInfoReturnable<Boolean> cir) {
        if (entity.getClass() == Horse.class) cir.setReturnValue(false);
    }
}
