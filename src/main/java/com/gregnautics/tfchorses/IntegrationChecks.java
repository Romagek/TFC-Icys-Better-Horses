package com.gregnautics.tfchorses;

import com.mojang.logging.LogUtils;
import icy.betterhorses.net.IHorseData;
import icy.betterhorses.net.entity.BhBreedHorse;
import net.dries007.tfc.common.entities.TFCEntities;
import net.dries007.tfc.common.entities.livestock.*;
import net.dries007.tfc.util.calendar.Calendars;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;

/** Opt-in integration suite, only active with -Dtfci.qa=true in an isolated test world. */
public final class IntegrationChecks {
    private static int stage, passed;
    private static UUID migrated;
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        passed++; LogUtils.getLogger().info("TFCI QA PASS: {}", message);
    }
    public static void tick(ServerTickEvent.Post event) {
        if (!Boolean.getBoolean("tfci.qa")) return;
        var server = event.getServer();
        if (++stage < 40) return;
        var level = server.overworld();
        try {
            if (stage == 40) run(level);
            if (stage == 45) {
                Entity replacement = level.getEntity(migrated);
                require(replacement instanceof BhBreedHorse, "existing TFC horse converted with same UUID");
                require(replacement.getCustomName().getString().equals("Migration QA"), "migration preserves name");
                var data = (TFCAnimalProperties)replacement;
                require(Math.abs(data.getFamiliarity() - .75F) < .001, "migration preserves familiarity");
                require(data.getGender() == Gender.FEMALE, "migration preserves gender");
                require(Math.abs(((BhBreedHorse)replacement).getHealth()-8) < .01, "migration preserves health");
                java.nio.file.Files.writeString(server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("tfci-qa-result.txt"), "PASS " + passed + " integration assertions\n");
                LogUtils.getLogger().info("TFCI QA COMPLETE: {} assertions", passed);
                server.halt(false);
            }
        } catch (Throwable error) {
            LogUtils.getLogger().error("TFCI QA FAILED after {} assertions", passed, error);
            try { java.nio.file.Files.writeString(server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("tfci-qa-result.txt"), "FAIL " + error); } catch (Exception ignored) {}
            server.halt(false);
        }
    }
    private static void run(ServerLevel level) throws Exception {
        Calendars.SERVER.skipForwardBy(24000L * 100);
        var tooltipProviders = new ArrayList<net.dries007.tfc.util.tooltip.EntityTooltip>();
        net.dries007.tfc.util.tooltip.EntityTooltips.register((id, provider, target) -> {
            if (id.equals(HorseTooltip.ID) && target == BhBreedHorse.class) tooltipProviders.add(provider);
        });
        require(tooltipProviders.size() == 1,"TFC registers exactly one husbandry provider for Icy breed hierarchy");
        var tooltipProvider = tooltipProviders.getFirst();
        for (String recipe : List.of("canister","horse_hooves_gear","horse_medkit_gear","horse_stabilizer_gear","upgraded_saddle"))
            require(level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("icys-better-horses",recipe)).isPresent(),"TFC equipment recipe loaded: "+recipe);
        var player = FakePlayerFactory.getMinecraft(level);
        player.setGameMode(GameType.SURVIVAL);
        int index=0;
        for (var rule : BreedRules.rules()) {
            var type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(rule.entity()));
            Entity entity = type.create(level);
            require(entity instanceof BhBreedHorse && entity instanceof net.dries007.tfc.common.entities.livestock.horse.HorseProperties, rule.entity()+" implements TFC horse lifecycle");
            BhBreedHorse horse = (BhBreedHorse)entity;
            var p = (TFCAnimalProperties)horse;
            horse.moveTo(level.getSharedSpawnPos().getX() + (index++ % 4) * 3, level.getSharedSpawnPos().getY()+4, level.getSharedSpawnPos().getZ());
            horse.finalizeSpawn(level, level.getCurrentDifficultyAt(horse.blockPosition()), MobSpawnType.COMMAND, null);
            p.setBirthTickToALongTimeAgo(); p.setGender(Gender.FEMALE); p.setFamiliarity(0);
            tooltipChecks(level,horse,tooltipProvider);
            var table=level.getServer().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,ResourceLocation.parse(rule.entity()).withPrefix("entities/")));
            var loot=table.getRandomItems(new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,horse)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,horse.position())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE,level.damageSources().generic())
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY));
            require(loot.stream().anyMatch(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals("tfc:food/horse_meat")),"breed uses TFC horse meat loot");
            require(!horse.isTamed(), "wild breed cannot be ridden as tamed");
            require(!horse.tameWithName(player), "vanilla taming cannot bypass familiarity");
            ItemStack food = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("tfc:food/oat_grain")), 10);
            player.setItemInHand(InteractionHand.MAIN_HAND, food);
            require(horse.isFood(food), "TFC grain is horse food");
            require(!horse.isFood(new ItemStack(net.minecraft.world.item.Items.STONE)), "non-food is rejected by TFC diet");
            for(int feed=0;feed<6;feed++) {
                p.getEntityData().set(p.animalData().lastFedTick(), Long.MIN_VALUE);
                horse.mobInteract(player, InteractionHand.MAIN_HAND);
            }
            require(Math.abs(p.getFamiliarity()-.35F)<.02, "feeding uses TFC adult familiarity cap");
            require(food.getCount()==4, "feeding consumes one item per TFC feeding");
            horse.mobInteract(player, InteractionHand.MAIN_HAND);
            require(food.getCount()==4 && !horse.isInLove(), "feeding cooldown prevents instant vanilla breeding");
            require(horse.isTamed(), "familiarity enables taming");
            require(horse.tameWithName(player), "familiar horse can claim owner");
            require(player.getUUID().equals(IHorseData.of(horse).bh_getOwner()), "Icy ownership synchronized");
            gaitChecks(horse,player);
            CompoundTag tag = horse.saveWithoutId(new CompoundTag());
            BhBreedHorse restored = (BhBreedHorse)type.create(level);
            restored.load(tag);
            require(Math.abs(((TFCAnimalProperties)restored).getFamiliarity()-p.getFamiliarity())<.001,"TFC data survives entity save/load");
            require(IHorseData.of(restored).bh_getGender().location().getPath().equals("female"),"Icy gender matches TFC after load");
            var m = (MammalProperties)horse;
            var male = (BhBreedHorse)type.create(level);
            var mp = (TFCAnimalProperties)male;
            male.finalizeSpawn(level,level.getCurrentDifficultyAt(horse.blockPosition()),MobSpawnType.COMMAND,null);
            mp.setBirthTickToALongTimeAgo(); mp.setFamiliarity(.5F); mp.setGender(Gender.MALE); mp.setLastFedNow();
            require(horse.canMate(male),"opposite-sex adult TFC horses can mate");
            require(horse.getBreedOffspring(level,male)==null && m.isFertilized() && m.getPregnantTime()>0,"mating starts pregnancy instead of instant foal");
            CompoundTag pregnantTag = horse.saveWithoutId(new CompoundTag());
            restored.load(pregnantTag);
            require(((MammalProperties)restored).isFertilized() && ((MammalProperties)restored).getGenes()!=null,"pregnancy and genes survive reload");
            var foal = horse.getBreedOffspring(level,horse);
            require(foal instanceof BhBreedHorse && foal.isBaby(),"pregnancy produces TFC-aged breed foal");
            require(foal.getType()==horse.getType(),"same breed preserves breed");
            vehicleChecks(level, horse, (BhBreedHorse)foal, player);
            var donkey = TFCEntities.DONKEY.get().create(level);
            var dp = (TFCAnimalProperties)donkey;
            dp.setBirthTickToALongTimeAgo();dp.setFamiliarity(.5F);dp.setGender(Gender.MALE);dp.setLastFedNow();
            m.setFertilized(false);m.setPregnantTime(-1);p.setLastFedNow();
            require(horse.canMate(donkey),"breed can mate with TFC donkey");
            horse.getBreedOffspring(level,donkey);
            require(horse.getBreedOffspring(level,horse).getType()==TFCEntities.MULE.get(),"horse and donkey produce TFC mule");
            dp.setGender(Gender.FEMALE);mp.setGender(Gender.MALE);mp.setLastFedNow();dp.setLastFedNow();
            donkey.getBreedOffspring(level,male);
            require(donkey.getBreedOffspring(level,donkey).getType()==TFCEntities.MULE.get(),"female TFC donkey and male Icy horse produce mule");
            var otherRule=BreedRules.rules().get(index % BreedRules.rules().size());
            var other=(BhBreedHorse)BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(otherRule.entity())).create(level);
            var otherData=(TFCAnimalProperties)other;
            otherData.setBirthTickToALongTimeAgo();otherData.setGender(Gender.MALE);otherData.setFamiliarity(.5F);otherData.setLastFedNow();
            m.setFertilized(false);m.setPregnantTime(-1);p.setLastFedNow();
            horse.getBreedOffspring(level,other);
            var mixed=horse.getBreedOffspring(level,horse);
            require(mixed.getType()==horse.getType() || mixed.getType()==other.getType(),"mixed offspring inherits a parental breed");
            require(IHorseData.of((BhBreedHorse)mixed).bh_isMixedBreed(),"mixed ancestry preserved through TFC pregnancy");
            require(rule.climateMatches(rule.minTemperature(),rule.minRainfall()) && !rule.climateMatches(rule.maxTemperature()+1,rule.minRainfall()),"climate envelope boundaries");
        }
        Horse old = TFCEntities.HORSE.get().create(level);
        old.moveTo(level.getSharedSpawnPos().getCenter().add(0,4,0));old.setCustomName(Component.literal("Migration QA"));
        var data=(TFCAnimalProperties)old;data.setBirthTickToALongTimeAgo();data.setGender(Gender.FEMALE);data.setFamiliarity(.75F);
        old.setHealth(8);migrated=old.getUUID();level.addFreshEntity(old);
        require(level.getEntity(migrated)==old,"migration original successfully inserted");
        // Force optional integration classes to transform and validate their Mixin injection targets.
        Class.forName("com.jusipat.astikorcartsredux.entity.AbstractDrawnEntity");
        Class.forName("net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon");
        Class.forName("net.steampn.createhorsepower.blocks.horse_crank.HorseCrankBlock");
        require(true,"all optional integration targets transformed");
    }
    private static void gaitChecks(BhBreedHorse horse, net.minecraft.server.level.ServerPlayer rider) throws Exception {
        var riddenSpeed=net.minecraft.world.entity.animal.horse.AbstractHorse.class.getDeclaredMethod("getRiddenSpeed",net.minecraft.world.entity.player.Player.class);
        riddenSpeed.setAccessible(true);
        var data=IHorseData.of(horse);
        double base=horse.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
        data.bh_setGear(0);data.bh_setGaitGear(0);
        float initial=(Float)riddenSpeed.invoke(horse,rider);
        require(initial>0 && initial<=.04501F,"manual riding defaults to walk without an auto gait");
        float previous=0;
        for(int gait=1;gait<=4;gait++) {
            data.bh_setGaitGear(gait);data.bh_setGear(0);
            float manual=(Float)riddenSpeed.invoke(horse,rider);
            data.bh_setGear(gait);
            float automatic=(Float)riddenSpeed.invoke(horse,rider);
            require(Math.abs(manual-automatic)<.00001F,"W/S uses the same speed as selected automatic gait "+gait);
            require(manual>=previous,"manual gait speeds never decrease from walk to gallop");
            previous=manual;
        }
        require(Math.abs(previous-horse.getAttributeValue(Attributes.MOVEMENT_SPEED))<.00001,"manual gallop retains full horse attribute speed");
        require(horse.getAttributeBaseValue(Attributes.MOVEMENT_SPEED)==base,"changing manual gaits preserves breeding and breed attributes");
        var speedAttribute=horse.getAttribute(Attributes.MOVEMENT_SPEED);
        var slowId=ResourceLocation.fromNamespaceAndPath("tfc_icy_horses","qa_slow");
        speedAttribute.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(slowId,-.95,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        data.bh_setGear(0);data.bh_setGaitGear(1);
        require((Float)riddenSpeed.invoke(horse,rider)<=horse.getAttributeValue(Attributes.MOVEMENT_SPEED)+.00001,"manual walk respects existing slow/old-age modifiers");
        speedAttribute.removeModifier(slowId);
        data.bh_setGear(0);data.bh_setGaitGear(0);
    }
    private static void tooltipChecks(ServerLevel level, BhBreedHorse horse, net.dries007.tfc.util.tooltip.EntityTooltip provider) {
        var data=(MammalProperties)horse;
        var lines=new ArrayList<Component>();
        provider.display(level,horse,lines::add);
        require(hasKey(lines,"tfc.enum.gender.female"),"registered tooltip displays TFC gender and familiarity");
        require(hasKey(lines,"tfc.jade.animal_size"),"registered tooltip displays TFC genetic size");
        require(hasKey(lines,"tfc_icy_horses.tooltip.age") && hasKey(lines,"tfc.jade.animal_wear"),"registered tooltip displays adult age and TFC wear");
        data.setBirthTickNow();lines.clear();provider.display(level,horse,lines::add);
        require(hasKey(lines,"tfc.jade.adulthood_progress"),"registered tooltip displays foal growth time");
        data.setBirthTickToALongTimeAgo();data.setFertilized(true);data.setPregnantTime(Calendars.SERVER.getTotalCalendarDays());
        lines.clear();provider.display(level,horse,lines::add);
        require(hasKey(lines,"tfc.tooltip.animal.pregnant") && hasKey(lines,"tfc.jade.gestation_time_left"),"registered tooltip displays pregnancy and time left");
        data.setFertilized(false);data.setPregnantTime(-1);
    }
    private static boolean hasKey(List<Component> lines,String key) {
        return lines.stream().anyMatch(c -> c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tr && tr.getKey().equals(key));
    }
    private static void vehicleChecks(ServerLevel level, BhBreedHorse adult, BhBreedHorse foal, net.minecraft.server.level.ServerPlayer player) throws Exception {
        var cartClass = com.jusipat.astikorcartsredux.entity.AbstractDrawnEntity.class;
        var canPull=cartClass.getDeclaredMethod("canPull",Entity.class); canPull.setAccessible(true);
        for(String namespace : List.of("astikorcartsredux","tfcastikorcarts")) {
            var cart=(com.jusipat.astikorcartsredux.entity.AbstractDrawnEntity)BuiltInRegistries.ENTITY_TYPE.stream().filter(t -> BuiltInRegistries.ENTITY_TYPE.getKey(t).getNamespace().equals(namespace)).map(t -> t.create(level)).filter(cartClass::isInstance).findFirst().orElseThrow();
            require((Boolean)canPull.invoke(cart,adult),namespace+" accepts adult breed");
            require(!(Boolean)canPull.invoke(cart,foal),namespace+" rejects foal");
            cart.moveTo(adult.position());level.addFreshEntity(cart);
            cart.setPulling(adult);
            require(cart.getPulling()==adult,namespace+" attaches to breed");
            cart.setPulling(null);
            require(cart.getPulling()==null,namespace+" detaches from breed");
            cart.discard();
        }
        var wagonClass=Class.forName("net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon");
        var check=wagonClass.getDeclaredMethod("lambda$tryHitchHorse$0",net.minecraft.world.entity.player.Player.class,Mob.class);check.setAccessible(true);
        adult.setLeashedTo(player,false);foal.setLeashedTo(player,false);
        require((Boolean)check.invoke(null,player,adult),"Trotting Wagons accepts leashed adult breed");
        require(!(Boolean)check.invoke(null,player,foal),"Trotting Wagons rejects foal");
        adult.dropLeash(false,false);foal.dropLeash(false,false);
        var crank=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("createhorsepower:horse_crank"));
        var verify=crank.getClass().getDeclaredMethod("verifyMobIsInConfig",ResourceLocation.class,net.minecraft.world.level.Level.class,BlockPos.class,net.minecraft.world.level.block.state.BlockState.class);verify.setAccessible(true);
        var getMobType=crank.getClass().getDeclaredMethod("getMobType",Mob.class);getMobType.setAccessible(true);
        BlockPos pos=level.getSharedSpawnPos().above(20);
        level.setBlock(pos,crank.defaultBlockState(),3);
        require((Boolean)verify.invoke(crank,getMobType.invoke(crank,adult),level,pos,crank.defaultBlockState()),"Create Horse Power accepts breed as large worker");
        require(!(Boolean)verify.invoke(crank,getMobType.invoke(crank,foal),level,pos,crank.defaultBlockState()),"Create Horse Power rejects foal");
        level.removeBlock(pos,false);
    }
}
