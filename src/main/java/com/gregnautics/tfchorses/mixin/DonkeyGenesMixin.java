package com.gregnautics.tfchorses.mixin;
import icy.betterhorses.net.entity.BhBreedHorse;
import net.dries007.tfc.common.entities.livestock.TFCAnimalProperties;
import net.dries007.tfc.common.entities.livestock.horse.TFCDonkey;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=TFCDonkey.class,remap=false)
public abstract class DonkeyGenesMixin {
    @Inject(method="createGenes",at=@At("TAIL"))
    private void tfci$hybrid(CompoundTag tag,TFCAnimalProperties male,CallbackInfo ci) {
        if (male instanceof BhBreedHorse) tag.putBoolean("isMule",true);
    }
}
