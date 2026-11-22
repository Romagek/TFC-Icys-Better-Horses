package com.gregnautics.tfchorses;

import icy.betterhorses.net.ModEntities;
import net.dries007.tfc.common.entities.livestock.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Disposable visual fixture. Never enabled by a normal launch. */
public final class TooltipClientFixture {
    private static boolean ready;
    private static Runnable pendingOpen;
    private static long openAt;
    public static void tick(ServerTickEvent.Post event) {
        if(pendingOpen!=null && event.getServer().overworld().getGameTime()>=openAt) { pendingOpen.run();pendingOpen=null; }
        if (ready || !Boolean.getBoolean("tfci.tooltipQa") || event.getServer().getPlayerList().getPlayers().isEmpty()) return;
        ready=true;
        var level=event.getServer().overworld();
        var player=event.getServer().getPlayerList().getPlayers().getFirst();
        var spawn=level.getSharedSpawnPos();
        int y=Math.min(240,Math.max(120,spawn.getY())+10);
        var center=new BlockPos(spawn.getX(),y,spawn.getZ());
        boolean riding=Boolean.getBoolean("tfci.ridingQa");
        for(int x=-4;x<=4;x++) for(int z=(riding?-160:-4);z<=(riding?160:6);z++) {
            level.setBlock(center.offset(x,0,z),Blocks.SMOOTH_STONE.defaultBlockState(),3);
            for(int h=1;h<=5;h++) level.setBlock(center.offset(x,h,z),Blocks.AIR.defaultBlockState(),3);
        }
        player.setGameMode(GameType.CREATIVE);
        player.teleportTo(level,center.getX()+.5,y+1,center.getZ()+.5,0,9);
        var horse=ModEntities.PERCHERON_HORSE.create(level);
        horse.moveTo(center.getX()+.5,y+1,center.getZ()+3.5,180,0);
        horse.finalizeSpawn(level,level.getCurrentDifficultyAt(horse.blockPosition()),MobSpawnType.COMMAND,null);
        var data=(TFCAnimalProperties)horse;
        data.setBirthTickToALongTimeAgo();data.setGender(Gender.FEMALE);data.setFamiliarity(.42F);data.setGeneticSize(16);
        horse.setNoAi(!riding);horse.setInvulnerable(true);horse.setNoGravity(!riding);
        level.addFreshEntity(horse);
        if(riding) {
            StaminaChecks.run(level,player);
            horse.tameWithName(player);
            horse.equipSaddle(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SADDLE),net.minecraft.sounds.SoundSource.NEUTRAL);
            player.startRiding(horse,true);
            if(Boolean.getBoolean("tfci.hudQa")) {
                player.setGameMode(GameType.SURVIVAL);
                level.setDayTime(6000);
                openAt=level.getGameTime()+40;
                pendingOpen=()->player.startRiding(horse,true);
            }
        }
        if(Boolean.getBoolean("tfci.shoesQa")) {
            HorseshoeChecks.run(level,player);
            horse.tameWithName(player);
            horse.equipSaddle(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SADDLE),null);
            player.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(HorseshoeChecks.item(),2));
            openAt=level.getGameTime()+40;
            pendingOpen=()->horse.openCustomInventoryScreen(player);
        }
        com.mojang.logging.LogUtils.getLogger().info("TFCI TOOLTIP FIXTURE READY: {}",horse.getUUID());
    }
}
