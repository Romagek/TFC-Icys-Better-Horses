package com.gregnautics.tfchorses;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class OptionalMixins implements IMixinConfigPlugin {
    public void onLoad(String p) {}
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String target, String name) {
        // Do not initialize optional mods during the Mixin discovery phase.
        if(name.contains("CoreHorseshoe") && OptionalMixins.class.getClassLoader().getResource("dev/atl4s/gregnauticscore/feature/exploration/HorseshoeInventory.class")==null) return false;
        return OptionalMixins.class.getClassLoader().getResource(target.replace('.', '/') + ".class") != null;
    }
    public void acceptTargets(Set<String> own, Set<String> other) {}
    public List<String> getMixins() { return null; }
    public void preApply(String t, ClassNode c, String m, IMixinInfo i) {}
    public void postApply(String t, ClassNode c, String m, IMixinInfo i) {}
}
