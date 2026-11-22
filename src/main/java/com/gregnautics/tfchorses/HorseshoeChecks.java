package com.gregnautics.tfchorses;

import dev.atl4s.gregnauticscore.feature.exploration.HorseshoeInventory;
import icy.betterhorses.net.IHorseData;
import icy.betterhorses.net.ModItems;
import icy.betterhorses.net.entity.BhBreedHorse;
import icy.betterhorses.net.inventory.GearSlot;
import net.dries007.tfc.common.entities.livestock.TFCAnimalProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.item.*;

/** Opt-in tests against the original Core mod and production Minecraft inventory classes. */
public final class HorseshoeChecks {
    private static int passed;
    private static final ResourceLocation BONUS=ResourceLocation.parse("gregnautics_core:horseshoes");
    private static void require(boolean ok,String name) {
        if(!ok) throw new AssertionError(name);
        passed++;com.mojang.logging.LogUtils.getLogger().info("TFCI SHOES QA PASS: {}",name);
    }
    public static Item item() { return BuiltInRegistries.ITEM.get(ResourceLocation.parse("gregnautics_core:horseshoe_set")); }
    public static void run(ServerLevel level,ServerPlayer player) {
        try {
            require(item()!=Items.AIR,"original Core horseshoe set is registered");
            for(var rule:BreedRules.rules()) {
                var type=BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(rule.entity()));
                var horse=(BhBreedHorse)type.create(level);
                horse.moveTo(player.position());
                require(horse instanceof HorseshoeInventory,"breed inherits original Core horseshoe storage");
                var core=(HorseshoeInventory)horse;
                var animal=(TFCAnimalProperties)horse;
                animal.setBirthTickToALongTimeAgo();animal.setFamiliarity(.5F);horse.tameWithName(player);
                for(boolean upgraded:new boolean[]{false,true}) {
                    var saddle=new ItemStack(upgraded?ModItems.UPGRADED_SADDLE:Items.SADDLE);
                    horse.equipSaddle(saddle,null);
                    IHorseData.of(horse).bh_getGearContainer().setItem(GearSlot.CHEST.ordinal(),upgraded?new ItemStack(Items.CHEST):ItemStack.EMPTY);
                    var inventory=new SimpleContainer(2);inventory.setItem(0,saddle);
                    var menu=new HorseInventoryMenu(1,player.getInventory(),inventory,horse,0);
                    var candidates=menu.slots.stream().filter(s->s.container==core.gregnautics$horseshoes()).toList();
                    require(candidates.size()==1,"one Core slot with "+(upgraded?"upgraded saddle and chest":"ordinary saddle"));
                    var slot=candidates.getFirst();
                    require(slot.x==152 && slot.y==58,"Core slot is clear of Icy gear and statistics");
                    require(slot.mayPlace(new ItemStack(item())) && !slot.mayPlace(new ItemStack(Items.STONE)),"slot accepts only original Core horseshoe set");
                    player.getInventory().clearContent();player.getInventory().setItem(0,new ItemStack(item(),2));
                    var source=menu.slots.stream().filter(s->s.container==player.getInventory() && s.getContainerSlot()==0).findFirst().orElseThrow();
                    menu.quickMoveStack(player,source.index);
                    require(core.gregnautics$horseshoes().getItem(0).getCount()==1 && player.getInventory().getItem(0).getCount()==1,"shift equips exactly one set without duplication");
                    horse.tick();horse.tick();
                    var speed=horse.getAttribute(Attributes.MOVEMENT_SPEED);
                    require(speed.hasModifier(BONUS) && Math.abs(speed.getModifier(BONUS).amount()-.1)<.00001,"original Core +10% modifier is applied once");
                    CompoundTag tag=horse.saveWithoutId(new CompoundTag());
                    var loaded=(BhBreedHorse)type.create(level);loaded.load(tag);
                    require(((HorseshoeInventory)loaded).gregnautics$horseshoes().getItem(0).is(item()),"Core horseshoes survive save/load");
                    menu.quickMoveStack(player,slot.index);
                    require(core.gregnautics$horseshoes().isEmpty() && player.getInventory().countItem(item())==2,"shift returns shoes to actual player inventory");
                    horse.tick();
                    require(!speed.hasModifier(BONUS),"removing horseshoes removes original Core modifier");
                    core.gregnautics$horseshoes().setItem(0,new ItemStack(item()));
                    for(int i=0;i<36;i++) player.getInventory().setItem(i,new ItemStack(Items.STONE,64));
                    menu.quickMoveStack(player,slot.index);
                    require(core.gregnautics$horseshoes().getItem(0).is(item()),"full player inventory cannot lose equipped shoes");
                    core.gregnautics$horseshoes().clearContent();menu.removed(player);
                }
            }
            player.getInventory().clearContent();
            String result="PASS "+passed+" Core horseshoe assertions across 15 breeds and 2 saddle layouts";
            java.nio.file.Files.writeString(level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("horseshoe-qa-result.txt"),result+"\n");
            com.mojang.logging.LogUtils.getLogger().info("TFCI SHOES QA {}",result);
        } catch(Throwable failure) {
            com.mojang.logging.LogUtils.getLogger().error("TFCI SHOES QA FAIL after {} assertions",passed,failure);
            try { java.nio.file.Files.writeString(level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("horseshoe-qa-result.txt"),"FAIL "+failure); } catch(java.io.IOException ignored) {}
        }
    }
}
