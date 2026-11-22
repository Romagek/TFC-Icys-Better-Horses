package com.gregnautics.tfchorses.mixin;
import icy.betterhorses.net.entity.BhBreedHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value=BhBreedHorse.class,remap=false)
public interface BreedStatsAccess {
    @Invoker("bhRollStats") void tfci$rollStats();
}
