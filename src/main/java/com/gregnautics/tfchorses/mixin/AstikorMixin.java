package com.gregnautics.tfchorses.mixin;
import com.jusipat.astikorcartsredux.entity.AbstractDrawnEntity;
import icy.betterhorses.net.entity.BhBreedHorse;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=AbstractDrawnEntity.class,remap=false)
public abstract class AstikorMixin {
    @Inject(method="canPull",at=@At("HEAD"),cancellable=true)
    private void tfci$pull(Entity entity,CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof BhBreedHorse horse) cir.setReturnValue(horse.isTamed() && !horse.isBaby() && !icy.betterhorses.net.IHorseData.of(horse).bh_hasCartGear());
    }
}
