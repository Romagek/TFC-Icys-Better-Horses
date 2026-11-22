package com.gregnautics.tfchorses;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import net.dries007.tfc.util.climate.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;

public final class BreedRules {
    public record Rule(String entity, float minTemperature, float maxTemperature,
                       float minRainfall, float maxRainfall, int weight, List<String> biomes) {
        public boolean climateMatches(float temperature, float rainfall) {
            return temperature >= minTemperature && temperature <= maxTemperature && rainfall >= minRainfall && rainfall <= maxRainfall;
        }
        boolean matchesBiome(Holder<Biome> biome) {
            return biomes.isEmpty() || biomes.stream().anyMatch(s -> s.startsWith("#")
                ? biome.is(TagKey.create(Registries.BIOME, ResourceLocation.parse(s.substring(1))))
                : biome.is(ResourceLocation.parse(s)));
        }
    }
    private static List<Rule> rules = List.of();
    public static List<Rule> rules() { return rules; }
    public static void load(Path config) throws IOException {
        if (!Files.exists(config)) {
            Files.createDirectories(config.getParent());
            try (var in = Objects.requireNonNull(BreedRules.class.getResourceAsStream("/breed-rules.json"))) { Files.copy(in, config); }
        }
        try (var reader = Files.newBufferedReader(config)) {
            rules = validate(Arrays.asList(new Gson().fromJson(reader, Rule[].class)));
        }
    }
    public static List<Rule> validate(List<Rule> input) {
        if (input == null || input.isEmpty()) throw new IllegalArgumentException("Breed rules must not be empty");
        Set<String> ids = new HashSet<>();
        for (Rule rule : input) {
            if (rule == null || !ids.add(rule.entity) || ResourceLocation.tryParse(rule.entity) == null || rule.weight < 1 || rule.weight > 10000
                || !Float.isFinite(rule.minTemperature) || !Float.isFinite(rule.maxTemperature)
                || !Float.isFinite(rule.minRainfall) || !Float.isFinite(rule.maxRainfall)
                || rule.minTemperature > rule.maxTemperature || rule.minRainfall > rule.maxRainfall || rule.biomes == null)
                throw new IllegalArgumentException("Invalid or duplicate breed rule: " + rule);
            for (String biome : rule.biomes) ResourceLocation.parse(biome.startsWith("#") ? biome.substring(1) : biome);
        }
        return List.copyOf(input);
    }
    public static EntityType<?> choose(Level level, BlockPos pos, RandomSource random) {
        float temperature = Climate.getAverageTemperature(level, pos);
        float rainfall = Climate.getAverageRainfall(level, pos);
        var biome = level.getBiome(pos);
        var eligible = rules.stream().filter(r -> r.climateMatches(temperature, rainfall) && r.matchesBiome(biome)).toList();
        int total = eligible.stream().mapToInt(Rule::weight).sum();
        if (total == 0) return null;
        int roll = random.nextInt(total);
        for (Rule rule : eligible) {
            roll -= rule.weight;
            if (roll < 0) return BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(rule.entity)).orElseThrow();
        }
        throw new IllegalStateException("Unreachable weighted selection");
    }
    public static boolean hasHabitat(ServerLevelAccessor world, BlockPos pos) {
        var model = Climate.get(world.getLevel());
        float temperature = model.getAverageTemperature(world, pos);
        float rainfall = model.getAverageRainfall(world, pos);
        return rules.stream().anyMatch(r -> r.climateMatches(temperature, rainfall) && r.matchesBiome(world.getBiome(pos)));
    }
    public static boolean isBreedId(Object id) { return rules.stream().anyMatch(r -> r.entity.equals(String.valueOf(id))); }
}
