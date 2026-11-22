package com.gregnautics.tfchorses;

import icy.betterhorses.net.entity.BhBreedHorse;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid="tfc_icy_horses", value=Dist.CLIENT)
public final class StaminaHud {
    @SubscribeEvent public static void render(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen != null || mc.player == null
            || !(mc.player.getControlledVehicle() instanceof BhBreedHorse horse)) return;
        var data = (HorseStamina)horse;
        float value = data.tfci$stamina();
        var g = event.getGuiGraphics();
        // Match TFC's 81x5 food/water bars and use the next free right HUD row.
        int x = g.guiWidth()/2+10, y = g.guiHeight()-mc.gui.rightHeight+4;
        int fill = Math.clamp(Math.round(79*value/StaminaRules.MAX),0,79);
        boolean tired = data.tfci$exhausted();
        int light = tired ? 0xffff9b74 : 0xffffee9a;
        int middle = tired ? 0xffd44b36 : value<30 ? 0xffe0932f : 0xffd9b44a;
        int dark = tired ? 0xff853025 : 0xff89702b;
        // Stepped dark border, recessed empty track and one-pixel bevel.
        g.fill(x+1,y,x+80,y+5,0xff17170f);
        g.fill(x,y+1,x+81,y+4,0xff17170f);
        g.fill(x+1,y+1,x+80,y+4,0xff403b24);
        g.fill(x+1,y+1,x+80,y+2,0xff272518);
        if(fill>0) {
            g.fill(x+1,y+1,x+1+fill,y+2,light);
            g.fill(x+1,y+2,x+1+fill,y+3,middle);
            g.fill(x+1,y+3,x+1+fill,y+4,dark);
        }
        for(int i=8;i<79;i+=8) g.fill(x+i,y+2,x+i+1,y+4,0x55332912);
    }
}
