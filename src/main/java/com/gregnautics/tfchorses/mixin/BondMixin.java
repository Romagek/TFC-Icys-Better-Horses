package com.gregnautics.tfchorses.mixin;
import icy.betterhorses.net.BhHorseTraits;
import icy.betterhorses.net.entity.BhBreedHorse;
import net.dries007.tfc.common.entities.livestock.TFCAnimalProperties;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=BhHorseTraits.class, remap=false)
public abstract class BondMixin {
    @Inject(method="applyBondAttributes",at=@At("HEAD"),cancellable=true)
    private static void tfci$bond(AbstractHorse horse,int bond,CallbackInfo ci) {
        if (horse instanceof BhBreedHorse && horse instanceof TFCAnimalProperties p && (!horse.isTamed() || horse.isBaby())) {
            // Icy can call this during load; zero bonuses until TFC husbandry permits riding.
            if (bond != 0) { BhHorseTraits.applyBondAttributes(horse,0); ci.cancel(); }
        }
    }
}
