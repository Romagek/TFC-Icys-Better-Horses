package com.gregnautics.tfchorses;

import com.mojang.logging.LogUtils;
import icy.betterhorses.net.IHorseData;
import icy.betterhorses.net.entity.BhBreedHorse;
import net.dries007.tfc.common.entities.livestock.horse.TFCHorse;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import java.nio.file.*;
import java.util.*;

@Mod("tfc_icy_horses")
public final class TfcIcyHorses {
    private final Map<Horse, Integer> pending = new LinkedHashMap<>();
    private final Set<Horse> natural = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    public TfcIcyHorses(IEventBus bus) throws java.io.IOException {
        BreedRules.load(FMLPaths.CONFIGDIR.get().resolve("tfc-icy-horses/breeds.json"));
        NeoForge.EVENT_BUS.addListener(this::join);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::placement);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent e) -> {
            if (e.getEntity() instanceof Horse horse && naturalReason(e.getSpawnType())) natural.add(horse);
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> pending.clear());
        if (Boolean.getBoolean("tfci.qa")) NeoForge.EVENT_BUS.addListener(IntegrationChecks::tick);
        if (Boolean.getBoolean("tfci.tooltipQa")) NeoForge.EVENT_BUS.addListener(TooltipClientFixture::tick);
    }
    private static boolean naturalReason(MobSpawnType reason) { return reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION; }
    private void placement(MobSpawnEvent.SpawnPlacementCheck event) {
        if ((event.getEntityType() == net.dries007.tfc.common.entities.TFCEntities.HORSE.get() || event.getEntityType() == EntityType.HORSE)
            && naturalReason(event.getSpawnType()) && !BreedRules.hasHabitat(event.getLevel(), event.getPos()))
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
    }
    private void join(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Horse horse
            && (horse.getClass() == Horse.class || horse.getClass() == TFCHorse.class)) pending.put(horse, 1);
    }
    private void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var work = pending.entrySet().stream().filter(e -> e.getKey().level() == level).toList();
        for (var entry : work) {
            Horse old = entry.getKey();
            if (old.isRemoved() || !old.isAlive()) { pending.remove(old); continue; }
            if (entry.getValue() > 0) { pending.put(old, entry.getValue() - 1); continue; }
            var speed = old.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            boolean pulling = speed != null && (speed.hasModifier(net.minecraft.resources.ResourceLocation.parse("astikorcartsredux:pull"))
                || speed.hasModifier(net.minecraft.resources.ResourceLocation.parse("astikorcartsredux:pull_slowly")));
            if (old.isVehicle() || old.isPassenger() || pulling || old.getTags().contains("icys-better-horses:wagon_hitched")) { pending.put(old, 20); continue; }
            pending.remove(old);
            try { convert(level, old); }
            catch (Exception e) { LogUtils.getLogger().error("TFC Icy migration failed; original horse retained {}", old.getUUID(), e); }
        }
    }
    private void convert(ServerLevel level, Horse old) throws java.io.IOException {
        var type = BreedRules.choose(level, old.blockPosition(), old.getRandom());
        // Existing domestic horses outside natural habitat still migrate to a generalist breed.
        if (type == null) type = icy.betterhorses.net.ModEntities.MUSTANG_HORSE;
        if (!(type.create(level) instanceof BhBreedHorse replacement)) throw new IllegalStateException("Rule must name an Icy breed: " + type);
        CompoundTag original = old.saveWithoutId(new CompoundTag());
        CompoundTag tag = original.copy();
        // Preserve the full entity, TFC data and UUID; only initialize Icy's visual breed.
        replacement.load(tag);
        replacement.bhSetCoat(replacement.bhCoatSet().roll(old.getRandom()));
        boolean fresh = natural.remove(old);
        if (fresh) ((com.gregnautics.tfchorses.mixin.BreedStatsAccess)replacement).tfci$rollStats();
        if (old.getOwnerUUID() != null) ((IHorseData)replacement).bh_setOwner(old.getOwnerUUID());
        var leash = old.getLeashHolder();
        Path backup = level.getServer().getWorldPath(LevelResource.ROOT).resolve("tfc-icy-horses-migration");
        Files.createDirectories(backup);
        Path file = backup.resolve(old.getUUID() + ".nbt");
        if (!fresh && !Files.exists(file)) {
            CompoundTag record = new CompoundTag();
            record.putString("id", net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(old.getType()).toString());
            record.put("entity", original);
            NbtIo.writeCompressed(record, file);
        }
        old.remove(Entity.RemovalReason.CHANGED_DIMENSION);
        if (!level.addFreshEntity(replacement)) {
            ((EntityRestore)old).tfci$restore();
            if (!level.addFreshEntity(old)) throw new IllegalStateException("Horse migration rollback failed: " + old.getUUID());
            return;
        }
        if (leash != null) replacement.setLeashedTo(leash, true);
        LogUtils.getLogger().info("TFC Icy migrated {} -> {}", old.getUUID(), type);
    }
}
