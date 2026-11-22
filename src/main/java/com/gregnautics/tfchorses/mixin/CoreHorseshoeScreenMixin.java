package com.gregnautics.tfchorses.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.atl4s.gregnauticscore.feature.exploration.HorseshoeInventory;
import icy.betterhorses.net.entity.BhBreedHorse;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;

@Mixin(value=HorseInventoryScreen.class,priority=900)
public abstract class CoreHorseshoeScreenMixin extends AbstractContainerScreen<HorseInventoryMenu> {
    @Shadow @Final private AbstractHorse horse;
    protected CoreHorseshoeScreenMixin(HorseInventoryMenu menu,Inventory inventory,Component title) { super(menu,inventory,title); }
    @WrapMethod(method="renderBg")
    private void tfci$shoes(GuiGraphics graphics,float partial,int mouseX,int mouseY,Operation<Void> original) {
        original.call(graphics,partial,mouseX,mouseY);
        if(horse instanceof BhBreedHorse && horse instanceof HorseshoeInventory core) {
            for(var slot:menu.slots) if(slot.container==core.gregnautics$horseshoes()) {
                graphics.blitSprite(ResourceLocation.withDefaultNamespace("container/slot"),leftPos+slot.x-1,topPos+slot.y-1,18,18);
                if(!slot.hasItem()) graphics.renderFakeItem(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("gregnautics_core:wrought_iron_horseshoe"))),leftPos+slot.x,topPos+slot.y);
            }
        }
    }
}
