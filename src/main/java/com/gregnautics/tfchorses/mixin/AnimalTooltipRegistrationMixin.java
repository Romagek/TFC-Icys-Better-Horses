package com.gregnautics.tfchorses.mixin;

import com.gregnautics.tfchorses.HorseTooltip;
import icy.betterhorses.net.entity.BhBreedHorse;
import net.dries007.tfc.util.tooltip.EntityTooltip;
import net.dries007.tfc.util.tooltip.EntityTooltips;
import net.dries007.tfc.util.tooltip.RegisterCallback;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityTooltips.class, remap = false)
public abstract class AnimalTooltipRegistrationMixin {
    @Inject(method = "register", at = @At("TAIL"))
    private static void tfci$register(RegisterCallback<EntityTooltip, Entity> registry, CallbackInfo ci) {
        registry.register(HorseTooltip.ID, HorseTooltip.INSTANCE, BhBreedHorse.class);
    }
}
