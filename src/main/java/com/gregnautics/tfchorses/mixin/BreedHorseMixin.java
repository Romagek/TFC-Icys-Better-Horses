package com.gregnautics.tfchorses.mixin;

import icy.betterhorses.net.IHorseData;
import icy.betterhorses.net.BhBreedData;
import icy.betterhorses.net.entity.BhBreedHorse;
import icy.betterhorses.net.registry.BhContent;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.entities.EntityHelpers;
import net.dries007.tfc.common.entities.TFCEntities;
import net.dries007.tfc.common.entities.ai.TFCAvoidEntityGoal;
import net.dries007.tfc.common.entities.ai.TFCGroundPathNavigation;
import net.dries007.tfc.common.entities.livestock.*;
import net.dries007.tfc.common.entities.livestock.horse.*;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.config.animals.AnimalConfig;
import net.dries007.tfc.config.animals.MammalConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Adds TFC's actual animal interfaces to the original Icy entity hierarchy. */
@Mixin(value = BhBreedHorse.class, remap = false)
public abstract class BreedHorseMixin extends Horse implements HorseProperties, com.gregnautics.tfchorses.HorseStamina {
    @Unique private static final EntityDataAccessor<Float> tfci$STAMINA = SynchedEntityData.defineId(BhBreedHorse.class, EntityDataSerializers.FLOAT);
    @Unique private static final EntityDataAccessor<Boolean> tfci$EXHAUSTED = SynchedEntityData.defineId(BhBreedHorse.class, EntityDataSerializers.BOOLEAN);
    @Unique private net.minecraft.world.phys.Vec3 tfci$staminaPosition;
    public float tfci$stamina() { return entityData.get(tfci$STAMINA); }
    public boolean tfci$exhausted() { return entityData.get(tfci$EXHAUSTED); }
    public void tfci$stamina(float amount, boolean exhausted) {
        entityData.set(tfci$STAMINA, Float.isFinite(amount) ? Math.clamp(amount,0,100) : 100);
        entityData.set(tfci$EXHAUSTED, exhausted);
    }
    @Unique private static final CommonAnimalData tfci$DATA = CommonAnimalData.create(BhBreedHorse.class);
    @Unique private static final EntityDataAccessor<Long> tfci$PREGNANT = SynchedEntityData.defineId(BhBreedHorse.class, EntityDataSerializers.LONG);
    @Unique private CompoundTag tfci$genes;
    protected BreedHorseMixin(EntityType<? extends Horse> type, Level level) { super(type, level); }
    public AnimalConfig animalConfig() { return TFCConfig.SERVER.horseConfig.inner(); }
    public MammalConfig getMammalConfig() { return TFCConfig.SERVER.horseConfig; }
    public CommonAnimalData animalData() { return tfci$DATA; }
    public TagKey<Item> getFoodTag() { return TFCTags.Items.HORSE_FOOD; }
    public long getPregnantTime() { return entityData.get(tfci$PREGNANT); }
    public void setPregnantTime(long value) { entityData.set(tfci$PREGNANT, value); }
    public CompoundTag getGenes() { return tfci$genes; }
    public void setGenes(CompoundTag value) { tfci$genes = value; }
    public boolean isBaby() { return getAgeType() == Age.CHILD; }
    public int getAge() { return isBaby() ? -24000 : 0; }
    public void setAge(int age) { super.setAge(0); }
    public boolean isTamed() { return getFamiliarity() > TAMED_FAMILIARITY; }
    public boolean isFood(ItemStack stack) { return HorseProperties.super.isFood(stack); }
    public InteractionResult fedFood(Player player, ItemStack stack) {
        return isFood(stack) ? HorseProperties.super.mobInteract(player, player.getUsedItemHand()) : InteractionResult.PASS;
    }
    public boolean canMate(Animal other) {
        return other != this && other instanceof TFCAnimalProperties p && getGender() != p.getGender()
            && isReadyToMate() && p.isReadyToMate() && checkExtraBreedConditions(p);
    }
    public boolean checkExtraBreedConditions(TFCAnimalProperties other) {
        return (other instanceof BhBreedHorse || other instanceof TFCDonkey)
            && TFCChestedHorse.vanillaParentingCheck(this)
            && TFCChestedHorse.vanillaParentingCheck((net.minecraft.world.entity.animal.horse.AbstractHorse)other);
    }
    public void createGenes(CompoundTag tag, TFCAnimalProperties male) {
        HorseProperties.super.createGenes(tag, male);
        tag.putBoolean("isMule", male instanceof TFCDonkey);
        if (male instanceof BhBreedHorse sire) {
            BhBreedHorse dam = (BhBreedHorse)(Object)this;
            BhBreedHorse source = random.nextBoolean() ? dam : sire;
            tag.putString("tfciBabyType", BuiltInRegistries.ENTITY_TYPE.getKey(source.getType()).toString());
            tag.putInt("tfciCoat", dam.getType() == sire.getType() && random.nextBoolean() ? sire.bhCoat() : source.bhCoat());
            tag.putBoolean("tfciMixed", dam.getType() != sire.getType());
        }
    }
    public EntityType<?> getEntityTypeForBaby() {
        if (tfci$genes != null) {
            if (tfci$genes.getBoolean("isMule")) return TFCEntities.MULE.get();
            var id = ResourceLocation.tryParse(tfci$genes.getString("tfciBabyType"));
            if (id != null) {
                var type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
                if (type != null && icy.betterhorses.net.BhHorseKind.managed(type)) return type;
            }
        }
        return getType();
    }
    public void applyGenes(CompoundTag tag, MammalProperties baby) {
        HorseProperties.super.applyGenes(tag, baby);
        if (baby instanceof BhBreedHorse foal) {
            var arch = BhBreedData.of(foal.bhFixedBreed()).archetype();
            // Keep TFC inheritance, using this breed's ranges instead of vanilla horse limits.
            EntityHelpers.setNullableAttribute(foal, Attributes.MAX_HEALTH, EntityHelpers.createOffspringAttribute(tag.getDouble("maxHealth1"),tag.getDouble("maxHealth2"),arch.lowHealth(),arch.highHealth(),random));
            EntityHelpers.setNullableAttribute(foal, Attributes.MOVEMENT_SPEED, EntityHelpers.createOffspringAttribute(tag.getDouble("movementSpeed1"),tag.getDouble("movementSpeed2"),arch.lowSpeed(),arch.highSpeed(),random));
            EntityHelpers.setNullableAttribute(foal, Attributes.JUMP_STRENGTH, EntityHelpers.createOffspringAttribute(tag.getDouble("jumpStrength1"),tag.getDouble("jumpStrength2"),arch.lowJump(),arch.highJump(),random));
            foal.bhSetCoat(tag.getInt("tfciCoat"));
            ((IHorseData)foal).bh_setMixedBreed(tag.getBoolean("tfciMixed"));
            foal.setHealth(foal.getMaxHealth());
        }
    }
    /** @author Gregnautics @reason TFC pregnancy replaces immediate vanilla foals. */
    @Overwrite public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        if (!isFemale()) return null;
        if (partner != (Object)this) {
            if (partner instanceof TFCAnimalProperties male && canMate((Animal)partner)) onFertilized(male);
            return null;
        }
        if (getEntityTypeForBaby().create(level) instanceof AgeableMob foal && foal instanceof TFCAnimalProperties properties) {
            setBabyTraits(properties);
            return foal;
        }
        return null;
    }
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult food = HorseProperties.super.mobInteract(player, hand);
        if (food != InteractionResult.PASS) return food;
        if (isBaby()) return InteractionResult.PASS;
        if (!isTamed()) {
            makeMad();
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (getOwnerUUID() == null && !level().isClientSide) tameWithName(player);
        return super.mobInteract(player, hand);
    }
    public PathNavigation createNavigation(Level level) { return new TFCGroundPathNavigation(this, level); }
    public void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(g -> g instanceof RunAroundLikeCrazyGoal || g instanceof TemptGoal);
        goalSelector.addGoal(3, new TemptGoal(this, 1.25, Ingredient.of(getFoodTag()), false));
        goalSelector.addGoal(5, new TFCAvoidEntityGoal(this, PathfinderMob.class, 8, 1.6, 1.4, TFCTags.Entities.LAND_PREDATORS));
    }
    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void tfci$define(SynchedEntityData.Builder builder, CallbackInfo ci) {
        tfci$DATA.define(builder); builder.define(tfci$PREGNANT, -1L);
        builder.define(tfci$STAMINA,100f); builder.define(tfci$EXHAUSTED,false);
    }
    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void tfci$spawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData group, CallbackInfoReturnable<SpawnGroupData> ci) {
        if (reason != MobSpawnType.BREEDING) initCommonAnimalData(level, difficulty, reason);
        setPregnantTime(-1); tfci$syncGender();
    }
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void tfci$save(CompoundTag tag, CallbackInfo ci) {
        saveCommonAnimalData(tag); tag.putInt("tfciVersion", 1);
        tag.putFloat("tfciStamina",tfci$stamina()); tag.putBoolean("tfciExhausted",tfci$exhausted());
    }
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void tfci$load(CompoundTag tag, CallbackInfo ci) {
        float stamina = tag.contains("tfciStamina") ? tag.getFloat("tfciStamina") : 100;
        tfci$stamina(stamina,stamina<=0 || tag.getBoolean("tfciExhausted") && stamina<30);
        tfci$staminaPosition = null;
        if (tag.contains("birthTick")) readCommonAnimalData(tag);
        else {
            setGender(((IHorseData)this).bh_getGender().equals(BhContent.MALE.getKey()) ? Gender.MALE : Gender.FEMALE);
            if (tag.getInt("Age") < 0) setBirthTickNow(); else setBirthTickToALongTimeAgo();
            setOldTick(-1); setPregnantTime(-1);
            setFamiliarity(tag.getBoolean("Tame") || getOwnerUUID() != null || ((IHorseData)this).bh_getOwner() != null ? 0.5F : 0);
        }
        tfci$syncGender();
    }
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount % 5 == 0) {
            var pos = position();
            boolean moving = tfci$staminaPosition != null && pos.subtract(tfci$staminaPosition).horizontalDistanceSqr() > .0001;
            tfci$staminaPosition = pos;
            int selected = ((IHorseData)this).bh_getGaitGear();
            var state = com.gregnautics.tfchorses.StaminaRules.step(tfci$stamina(),tfci$exhausted(),
                Math.max(1,selected),moving && getControllingPassenger() instanceof Player,.25f);
            tfci$stamina(state.amount(),state.exhausted());
        }
        if (level().getGameTime() % 20 == 0) {
            tickAnimalData();
            if (!level().isClientSide) {
                tfci$syncGender();
                if (isBaby() || !isTamed()) ejectPassengers();
                if (!isReadyToMate()) resetLove();
            }
        }
    }
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        super.onSyncedDataUpdated(data);
        if (tfci$DATA.birthTick().equals(data)) refreshDimensions();
    }
    @Unique private void tfci$syncGender() {
        var data = (IHorseData)this;
        var gender = isMale() ? BhContent.MALE.getKey() : BhContent.FEMALE.getKey();
        if (!gender.equals(data.bh_getGender())) data.bh_setGender(gender);
    }
}
