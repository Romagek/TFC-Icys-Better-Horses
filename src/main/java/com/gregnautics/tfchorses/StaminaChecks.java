package com.gregnautics.tfchorses;

import icy.betterhorses.net.IHorseData;
import icy.betterhorses.net.entity.BhBreedHorse;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public final class StaminaChecks {
    private static int passed;
    private static void check(boolean ok,String message) {
        if(!ok) throw new AssertionError(message);
        passed++;
    }
    public static void run(ServerLevel level, Player player) {
        try {
            var gallop=StaminaRules.step(100,false,4,true,10);
            var canter=StaminaRules.step(100,false,3,true,10);
            check(gallop.amount()==60 && canter.amount()==85,"different drain rates");
            check(StaminaRules.step(20,false,4,false,1).amount()==25,"stationary gallop recovers");
            check(StaminaRules.step(20,false,1,true,1).amount()==23,"walk recovers");
            check(StaminaRules.step(20,false,2,true,1).amount()==21,"trot recovers");
            check(StaminaRules.step(1,false,4,true,1).exhausted(),"empty locks sprint");
            check(StaminaRules.step(28,true,4,true,1).exhausted(),"cannot bypass exhaustion by selecting gallop");
            check(!StaminaRules.step(29,true,1,false,1).exhausted(),"rest unlocks above threshold");
            check(StaminaRules.step(99,false,1,false,10).amount()==100,"recovery capped");
            var speed=net.minecraft.world.entity.animal.horse.AbstractHorse.class.getDeclaredMethod("getRiddenSpeed",Player.class);
            speed.setAccessible(true);
            for(var rule:BreedRules.rules()) {
                var type=BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(rule.entity()));
                var horse=(BhBreedHorse)type.create(level);
                var state=(HorseStamina)horse;
                check(state.tfci$stamina()==100 && !state.tfci$exhausted(),"new horse full");
                state.tfci$stamina(0,true);
                var data=IHorseData.of(horse);
                for(int active:new int[]{0,3,4}) {
                    data.bh_setGear(active); data.bh_setGaitGear(4);
                    check((Float)speed.invoke(horse,player)<=.101251,"exhaustion speed limit manual and auto");
                }
                var tag=horse.saveWithoutId(new CompoundTag());
                var copy=(BhBreedHorse)type.create(level); copy.load(tag);
                check(((HorseStamina)copy).tfci$stamina()==0 && ((HorseStamina)copy).tfci$exhausted(),"exhaustion persists");
                state.tfci$stamina(57.5f,false);
                copy.load(horse.saveWithoutId(new CompoundTag()));
                check(((HorseStamina)copy).tfci$stamina()==57.5f,"partial stamina persists");
                tag.remove("tfciStamina"); tag.remove("tfciExhausted"); copy.load(tag);
                check(((HorseStamina)copy).tfci$stamina()==100 && !((HorseStamina)copy).tfci$exhausted(),"old saves default full");
            }
            String result="PASS "+passed+" stamina assertions across 15 breeds";
            java.nio.file.Files.writeString(level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("stamina-qa-result.txt"),result+"\n");
            com.mojang.logging.LogUtils.getLogger().info("TFCI {}",result);
        } catch(Exception e) { throw new RuntimeException(e); }
    }
}
