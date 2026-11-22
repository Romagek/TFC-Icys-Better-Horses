package com.gregnautics.tfchorses.mixin;
import com.gregnautics.tfchorses.BreedRules;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.entity.Mob;
import net.minecraft.resources.ResourceLocation;
import icy.betterhorses.net.entity.BhBreedHorse;
@Mixin(targets="net.steampn.createhorsepower.blocks.horse_crank.HorseCrankBlock",remap=false)
public abstract class HorsePowerMixin {
    @Inject(method="getMobType",at=@At("HEAD"),cancellable=true)
    private void tfci$adult(Mob mob,CallbackInfoReturnable<ResourceLocation> cir) {
        if (mob instanceof BhBreedHorse horse && (horse.isBaby() || !horse.isTamed())) cir.setReturnValue(ResourceLocation.fromNamespaceAndPath("tfc_icy_horses","ineligible"));
    }
    @Redirect(method="verifyMobIsInConfig",at=@At(value="INVOKE",target="Ljava/util/List;contains(Ljava/lang/Object;)Z",ordinal=2))
    private boolean tfci$large(List<?> list,Object value) { return list.contains(value) || BreedRules.isBreedId(value); }
}
