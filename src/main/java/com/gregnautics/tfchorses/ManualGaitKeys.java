package com.gregnautics.tfchorses;

import com.mojang.blaze3d.platform.InputConstants;
import icy.betterhorses.net.client.HorseAutodriveController;
import icy.betterhorses.net.client.HorseGearController;
import icy.betterhorses.net.entity.BhBreedHorse;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value="tfc_icy_horses", dist=Dist.CLIENT)
public final class ManualGaitKeys {
    public static final KeyMapping UP = new KeyMapping("key.tfc_icy_horses.gait_up",
        InputConstants.UNKNOWN.getValue(), "key.categories.tfc_icy_horses");
    public static final KeyMapping DOWN = new KeyMapping("key.tfc_icy_horses.gait_down",
        InputConstants.UNKNOWN.getValue(), "key.categories.tfc_icy_horses");
    public ManualGaitKeys(IEventBus bus) {
        bus.addListener((RegisterKeyMappingsEvent e) -> { e.register(UP); e.register(DOWN); });
        NeoForge.EVENT_BUS.addListener(ManualGaitKeys::tick);
    }
    private static void tick(ClientTickEvent.Pre event) {
        while (UP.consumeClick()) shift(1);
        while (DOWN.consumeClick()) shift(-1);
    }
    private static void shift(int direction) {
        var mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null
            || !(mc.player.getControlledVehicle() instanceof BhBreedHorse horse)
            || horse.getControllingPassenger() != mc.player) return;
        HorseAutodriveController.INSTANCE.reset();
        int gait = ((ManualGaitControl)(Object)HorseGearController.INSTANCE).tfci$shiftManual(horse, direction);
        mc.player.displayClientMessage(Component.translatable("message.tfc_icy_horses.manual_gait",
            Component.translatable("gait.tfc_icy_horses." + gait)), true);
    }
}
