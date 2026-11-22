package com.gregnautics.tfchorses.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.atl4s.gregnauticscore.feature.exploration.HorseshoeInventory;
import icy.betterhorses.net.entity.BhBreedHorse;
import icy.betterhorses.net.mixin.SlotAccessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=HorseInventoryMenu.class,priority=2000)
public abstract class CoreHorseshoeMenuMixin extends AbstractContainerMenu {
    @Unique private Slot tfci$coreShoes;
    protected CoreHorseshoeMenuMixin(MenuType<?> type,int id) { super(type,id); }
    @Inject(method="<init>",at=@At("TAIL"))
    private void tfci$slot(int id,Inventory player,Container inventory,AbstractHorse horse,int columns,CallbackInfo ci) {
        if(horse instanceof BhBreedHorse && horse instanceof HorseshoeInventory core) {
            for(Slot slot:slots) if(slot.container==core.gregnautics$horseshoes()) {
                tfci$coreShoes=slot;
                ((SlotAccessor)slot).bh_setX(152);
                ((SlotAccessor)slot).bh_setY(58);
            }
        }
    }
    @WrapMethod(method="quickMoveStack")
    private ItemStack tfci$transfer(Player player,int index,Operation<ItemStack> original) {
        if(tfci$coreShoes==null || index<0 || index>=slots.size()) return original.call(player,index);
        Slot source=slots.get(index);
        ItemStack stack=source.getItem();
        boolean fromShoes=source==tfci$coreShoes;
        boolean fromPlayer=source.container==player.getInventory() && stack.is(BuiltInRegistries.ITEM.get(ResourceLocation.parse("gregnautics_core:horseshoe_set")));
        if((!fromShoes && !fromPlayer) || stack.isEmpty() || !source.mayPickup(player)) return original.call(player,index);
        ItemStack copy=stack.copy();
        boolean moved=false;
        if(fromShoes) {
            // Find the actual player inventory. Core and Icy both append slots, so
            // neither 'last slot - 36' nor a fixed gear index is reliable here.
            for(int i=slots.size()-1;i>=0 && !stack.isEmpty();i--) if(slots.get(i).container==player.getInventory())
                moved=moveItemStackTo(stack,i,i+1,false) || moved;
        } else if(tfci$coreShoes.mayPlace(stack)) {
            int target=slots.indexOf(tfci$coreShoes);
            moved=moveItemStackTo(stack,target,target+1,false);
        }
        if(!moved) return ItemStack.EMPTY;
        if(stack.isEmpty()) source.setByPlayer(ItemStack.EMPTY); else source.setChanged();
        source.onTake(player,stack);
        return copy;
    }
}
