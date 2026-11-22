package com.gregnautics.tfchorses.mixin;
import icy.betterhorses.net.IHorseData;
import icy.betterhorses.net.entity.BhBreedHorse;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(value=AbstractWagon.class, remap=false, priority=900)
public abstract class TrottingMixin {
    @WrapMethod(method="lambda$tryHitchHorse$0")
    private static boolean tfci$allow(Player player, Mob mob, Operation<Boolean> original) {
        if (mob instanceof BhBreedHorse horse) return horse.getLeashHolder() == player && horse.isTamed() && !horse.isBaby()
            && IHorseData.of(horse).bh_mayHandle(player.getUUID()) && !IHorseData.of(horse).bh_hasCartGear();
        return original.call(player,mob);
    }
}
