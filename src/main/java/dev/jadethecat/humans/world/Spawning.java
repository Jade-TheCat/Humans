package dev.jadethecat.humans.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Stream;

import dev.jadethecat.humans.Humans;
import dev.jadethecat.humans.entity.HumanEntity;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.mixin.object.builder.SpawnRestrictionAccessor;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.world.Heightmap;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biome;

// Special thanks to Earth2Java https://github.com/Slexom/earth2java for parts of this code.
// Parts of this code adapted from Earth2Java used under the MIT License.

public class Spawning {
    private static Biome.Category[] humanCategories = new Biome.Category[]{
        Biome.Category.PLAINS,
        Biome.Category.EXTREME_HILLS,
        Biome.Category.FOREST,
        Biome.Category.TAIGA,
        Biome.Category.DESERT,
        Biome.Category.SAVANNA
    };
    public static void addHumanToBiomes() {
        String[] categories = Stream.of(humanCategories).flatMap(Stream::of).map(Biome.Category::getName).toArray(String[]::new);
        List<String> biomes = new ArrayList<>(Collections.EMPTY_LIST);
        for (String category : categories) {
            BuiltinRegistries.BIOME.forEach(biome -> {
                if (biome.getCategory().toString().equalsIgnoreCase(category)) {
                    biomes.add(BuiltinRegistries.BIOME.getId(biome).toString());
                }
            });
        }
        for (String id : biomes) {
            BuiltinRegistries.BIOME.stream()
                .filter(biome -> BuiltinRegistries.BIOME.getId(biome).toString().equals(id))
                .findFirst()
                .ifPresent(biome -> {
                    Predicate<BiomeSelectionContext> pred = BiomeSelectors.includeByKey(BuiltinRegistries.BIOME.getKey(biome).get());
                    BiomeModifications.addSpawn(pred, SpawnGroup.CREATURE, Humans.HUMAN, 8, 2, 4);
                });
            RegistryEntryAddedCallback.event(BuiltinRegistries.BIOME).register((i, registryName, biome) -> {
                if (registryName.toString().equals(id)) {
                    Predicate<BiomeSelectionContext> pred = BiomeSelectors.includeByKey(BuiltinRegistries.BIOME.getKey(biome).get());
                    BiomeModifications.addSpawn(pred, SpawnGroup.CREATURE, Humans.HUMAN, 8, 2, 4);
                }
            });
        }
        SpawnRestrictionAccessor.callRegister(Humans.HUMAN, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, Spawning::isValidNaturalSpawn);
    }
    public static boolean isValidNaturalSpawn(EntityType<? extends HumanEntity> type, WorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random) {
		return world.getBlockState(pos.down()).isIn(Humans.HUMAN_SPAWNABLE) && world.getBaseLightLevel(pos, 0) > 8;
	}
}
