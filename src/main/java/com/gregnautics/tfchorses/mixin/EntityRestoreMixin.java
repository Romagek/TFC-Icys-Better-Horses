package com.gregnautics.tfchorses.mixin;
import com.gregnautics.tfchorses.EntityRestore;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
@Mixin(Entity.class)
public abstract class EntityRestoreMixin implements EntityRestore {
    @Shadow protected abstract void unsetRemoved();
    public void tfci$restore() { unsetRemoved(); }
}
