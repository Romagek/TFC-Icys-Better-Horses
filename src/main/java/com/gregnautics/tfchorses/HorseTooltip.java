package com.gregnautics.tfchorses;

import net.dries007.tfc.common.entities.livestock.TFCAnimalProperties;
import net.dries007.tfc.util.tooltip.EntityTooltip;
import net.dries007.tfc.util.tooltip.EntityTooltips;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.Locale;

/** Shared TFC tooltip callback, used by Jade and TFC's other tooltip integrations. */
public final class HorseTooltip {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("tfc_icy_horses", "husbandry");
    public static final EntityTooltip INSTANCE = (level, entity, output) -> {
        EntityTooltips.ANIMAL.display(level, entity, output);
        if (entity instanceof TFCAnimalProperties animal) {
            output.accept(Component.translatable("tfc_icy_horses.tooltip.age",
                Component.translatable("tfc_icy_horses.age." + animal.getAgeType().name().toLowerCase(Locale.ROOT))));
        }
    };
    private HorseTooltip() {}
}
