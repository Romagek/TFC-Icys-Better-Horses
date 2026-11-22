package com.gregnautics.tfchorses.mixin;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="icy.betterhorses.net.BhBiomeSpawns$SpawnModifier",remap=false)
public abstract class IcySpawnsMixin {
    @Inject(method="modify",at=@At("HEAD"),cancellable=true)
    private void tfci$noDuplicateSpawns(Holder<Biome> biome, BiomeModifier.Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder, CallbackInfo ci) {
        // Keep TFC's existing herds, weights and biome selection; only change their breed.
        if (biome.unwrapKey().map(k -> k.location().getNamespace().equals("tfc")).orElse(false)) ci.cancel();
    }
}
