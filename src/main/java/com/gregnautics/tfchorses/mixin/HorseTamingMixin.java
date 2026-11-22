package com.gregnautics.tfchorses.mixin;
import icy.betterhorses.net.entity.BhBreedHorse;
import net.dries007.tfc.common.entities.livestock.TFCAnimalProperties;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(AbstractHorse.class)
public abstract class HorseTamingMixin {
    @Inject(method="tameWithName", at=@At("HEAD"), cancellable=true)
    private void tfci$tame(Player player, CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof BhBreedHorse && (Object)this instanceof TFCAnimalProperties p && (p.getFamiliarity() <= 0.15F || p.getEntity().isBaby())) cir.setReturnValue(false);
    }
}
