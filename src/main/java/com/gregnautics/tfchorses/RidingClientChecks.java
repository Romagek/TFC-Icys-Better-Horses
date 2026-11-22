package com.gregnautics.tfchorses;

import icy.betterhorses.net.IHorseData;
import icy.betterhorses.net.client.HorseAutodriveController;
import icy.betterhorses.net.client.HorseGearController;
import icy.betterhorses.net.entity.BhBreedHorse;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Real KeyboardInput/riding physics test, opt-in only in the disposable QA client. */
@EventBusSubscriber(modid="tfc_icy_horses",value=Dist.CLIENT)
public final class RidingClientChecks {
    private static final int[] GAITS={1,2,3,4,1,0};
    private static final double[] measured=new double[GAITS.length];
    private static final float[] caps=new float[GAITS.length];
    private static int phase,frame;
    private static Vec3 start;
    private static boolean finished;
    private static float staminaStart,staminaEnd,canterDrain;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("tfci.ridingQa") || Boolean.getBoolean("tfci.hudQa") || finished) return;
        var client=Minecraft.getInstance();
        if (client.player==null || client.screen!=null || !(client.player.getControlledVehicle() instanceof BhBreedHorse horse)) return;
        try {
            var data=IHorseData.of(horse);
            if (++frame==1) {
                HorseAutodriveController.INSTANCE.reset();
                HorseGearController.INSTANCE.reset();
                if (phase < 4) {
                    ManualGaitKeys.UP.setKey(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(313));
                    ManualGaitKeys.DOWN.setKey(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(314));
                    net.minecraft.client.KeyMapping.resetMapping();
                    if(phase==0) net.minecraft.client.KeyMapping.click(ManualGaitKeys.DOWN.getKey());
                    for(int i=1;i<GAITS[phase];i++) net.minecraft.client.KeyMapping.click(ManualGaitKeys.UP.getKey());
                } else HorseGearController.INSTANCE.shift(horse,GAITS[phase]);
            }
            // Test ordinary W input, through the actual modded KeyboardInput path.
            client.options.keyUp.setDown(phase!=4 && frame<=45);
            if(phase==0 && (frame==5 || frame==7)) client.options.keyUp.setDown(false);
            if(phase==3 && frame==48) net.minecraft.client.KeyMapping.click(ManualGaitKeys.DOWN.getKey());
            if(phase==3 && frame==54) {
                if(data.bh_getGaitGear()!=3) throw new AssertionError("Decrease key did not select canter");
                net.minecraft.client.KeyMapping.click(ManualGaitKeys.UP.getKey());
            }
            int expected=phase==3 && frame>=50 && frame<=54 ? 3 : GAITS[phase];
            if (phase<4 && frame>3 && frame!=49 && frame!=55 && (data.bh_getGear()!=0 || data.bh_getGaitGear()!=expected
                || HorseGearController.INSTANCE.isEngaged())) throw new AssertionError("Manual key enabled autorun or lost gait");
            if(frame==15) { start=horse.position(); staminaStart=((HorseStamina)horse).tfci$stamina(); }
            if(frame==45) {
                staminaEnd=((HorseStamina)horse).tfci$stamina();
                if(phase==2) {
                    canterDrain=staminaStart-staminaEnd;
                    if(canterDrain<=0) throw new AssertionError("Server stamina did not drain in canter");
                }
                if(phase==3 && staminaStart-staminaEnd<=canterDrain)
                    throw new AssertionError("Gallop stamina drain is not faster than canter");
                caps[phase]=horse.getSpeed();
                measured[phase]=horse.position().subtract(start).multiply(1,0,1).length()*20/30;
                if(measured[phase]<.1) throw new AssertionError("No movement in phase "+phase);
                com.mojang.logging.LogUtils.getLogger().info("TFCI RIDING QA phase {}: {} blocks/s at {} grounded {} speed {} gear {} gait {} input {}",phase,measured[phase],horse.position(),horse.onGround(),horse.getSpeed(),data.bh_getGear(),data.bh_getGaitGear(),client.player.input.forwardImpulse);
            }
            if(frame==60) start=horse.position();
            if(frame==70) {
                if((phase==2 || phase==3) && ((HorseStamina)horse).tfci$stamina()<=staminaEnd)
                    throw new AssertionError("Server stamina did not recover when W released");
                if(phase<4 && horse.position().subtract(start).multiply(1,0,1).length()>.15)
                    throw new AssertionError("Horse kept running after W released, gait "+GAITS[phase]);
                if(++phase==GAITS.length) {
                    for(int i=1;i<4;i++) if(measured[i]<=measured[i-1]+.1) throw new AssertionError("Manual gait did not increase: "+i);
                    if(Math.abs(caps[4]-caps[0])>.0001 || Math.abs(caps[5]-caps[0])>.0001)
                        throw new AssertionError("Walking speed cap differs between manual/auto/default");
                    if(Math.abs(measured[4]-measured[5])>.2) throw new AssertionError("Original auto/default walk differ");
                    finish(client,horse,"PASS: separate gait keys in 4 gaits, no autorun, stop on W release, auto walk, default walk. Measured blocks/s: "+java.util.Arrays.toString(measured));
                }
                frame=0;
            }
        } catch(Throwable e) { finish(client,horse,"FAIL: "+e); }
    }
    private static void finish(Minecraft client,BhBreedHorse horse,String result) {
        finished=true;
        client.options.keyUp.setDown(false);
        HorseGearController.INSTANCE.shift(horse,-4);
        com.mojang.logging.LogUtils.getLogger().info("TFCI RIDING QA {}",result);
        try { java.nio.file.Files.writeString(client.gameDirectory.toPath().resolve("riding-qa-result.txt"),result+"\n"); }
        catch(java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
    }
}
