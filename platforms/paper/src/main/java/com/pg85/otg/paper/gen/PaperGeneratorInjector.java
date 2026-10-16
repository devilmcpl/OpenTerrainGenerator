package com.pg85.otg.paper.gen;

import com.pg85.otg.OTG;
import com.pg85.otg.constants.Constants;
import com.pg85.otg.paper.biome.OTGPaperBiomeProvider;
import com.pg85.otg.paper.util.PaperReflection;
import com.pg85.otg.presets.Preset;
import com.pg85.otg.util.OTGLog;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureCheck;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Replaces the chunk generator of a world that was created with a Bukkit generator (bukkit.yml, plugins) with the
 * OTG chunk generator. Worlds created from an OTG world preset already use the OTG chunk generator.
 * Everything that caches the chunk generator is rebuilt the same way ChunkMap and ServerLevel create them.
 */
public final class PaperGeneratorInjector {
    private static final Field CHUNK_MAP_WORLD_GEN_CONTEXT = PaperReflection.getField(ChunkMap.class, "worldGenContext");
    private static final Field CHUNK_MAP_RANDOM_STATE = PaperReflection.getField(ChunkMap.class, "randomState");
    private static final Field CHUNK_MAP_CHUNK_GENERATOR_STATE = PaperReflection.getField(ChunkMap.class, "chunkGeneratorState");
    private static final Field SERVER_LEVEL_STRUCTURE_CHECK = PaperReflection.getField(ServerLevel.class, "structureCheck");
    private static final Field STRUCTURE_MANAGER_STRUCTURE_CHECK = PaperReflection.getField(StructureManager.class, "structureCheck");

    private static final Set<ServerLevel> injectedLevels = Collections.newSetFromMap(new WeakHashMap<>());

    public static synchronized boolean inject(ServerLevel level, String presetFolderName) {
        if (injectedLevels.contains(level)) {
            return true;
        }

        Preset preset = OTG.getEngine().getPresetLoader().getPresetByFolderName(presetFolderName);
        if (preset == null) {
            OTGLog.log(LogLevel.FATAL, LogCategory.MAIN, "Could not find preset " + presetFolderName + " for world " + level.getWorld().getName());
            return false;
        }

        try {
            RegistryAccess registryAccess = level.registryAccess();
            ChunkMap chunkMap = level.getChunkSource().chunkMap;
            long seed = level.getSeed();

            ResourceKey<NoiseGeneratorSettings> noiseKey = ResourceKey.create(Registries.NOISE_SETTINGS, Identifier.fromNamespaceAndPath(Constants.MOD_ID_SHORT, preset.getPresetRegistryName()));
            Holder<NoiseGeneratorSettings> noiseSettings = registryAccess.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(noiseKey);
            OTGPaperBiomeProvider biomeProvider = new OTGPaperBiomeProvider(presetFolderName, 0L, registryAccess.lookupOrThrow(Registries.BIOME));
            OTGPaperChunkGenerator generator = new OTGPaperChunkGenerator(biomeProvider, noiseSettings, registryAccess.lookupOrThrow(Registries.BIOME));

            // Same as ChunkMap does for chunk generators that aren't NoiseBasedChunkGenerators
            RandomState randomState = RandomState.create(NoiseGeneratorSettings.dummy(), registryAccess.lookupOrThrow(Registries.NOISE), seed);
            ChunkGeneratorStructureState chunkGeneratorState = generator.createState(registryAccess.lookupOrThrow(Registries.STRUCTURE_SET), randomState, seed, level.spigotConfig);
            WorldGenContext oldWorldGenContext = chunkMap.worldGenContext;
            WorldGenContext worldGenContext = new WorldGenContext(
                    oldWorldGenContext.level(),
                    generator,
                    oldWorldGenContext.structureManager(),
                    oldWorldGenContext.lightEngine(),
                    oldWorldGenContext.mainThreadExecutor(),
                    oldWorldGenContext.unsavedListener()
            );
            StructureCheck structureCheck = new StructureCheck(
                    level.getChunkSource().chunkScanner(),
                    registryAccess,
                    level.getServer().getStructureManager(),
                    level.getTypeKey(),
                    generator,
                    randomState,
                    level,
                    biomeProvider,
                    seed,
                    level.getServer().getFixerUpper()
            );

            PaperReflection.setField(CHUNK_MAP_RANDOM_STATE, chunkMap, randomState);
            PaperReflection.setField(CHUNK_MAP_CHUNK_GENERATOR_STATE, chunkMap, chunkGeneratorState);
            PaperReflection.setField(CHUNK_MAP_WORLD_GEN_CONTEXT, chunkMap, worldGenContext);
            PaperReflection.setField(SERVER_LEVEL_STRUCTURE_CHECK, level, structureCheck);
            PaperReflection.setField(STRUCTURE_MANAGER_STRUCTURE_CHECK, level.structureManager(), structureCheck);

            chunkGeneratorState.ensureStructuresGenerated();
            generator.setServerLevel(level);
            injectedLevels.add(level);

            if (level.dimensionType().minY() != generator.getMinY() || level.dimensionType().height() != generator.getGenDepth()) {
                OTGLog.log(LogLevel.WARN, LogCategory.MAIN, "World " + level.getWorld().getName() + " uses a different world height than preset " + presetFolderName + ", use level-type=" + Constants.MOD_ID_SHORT + ":" + preset.getPresetRegistryName() + " in server.properties to use the preset's world height.");
            }
            OTGLog.log(LogLevel.INFO, LogCategory.MAIN, "Using preset " + presetFolderName + " for world " + level.getWorld().getName());
            return true;
        } catch (Exception e) {
            OTGLog.log(LogLevel.FATAL, LogCategory.MAIN, "Could not set up OTG for world " + level.getWorld().getName() + ": " + e);
            return false;
        }
    }

    private PaperGeneratorInjector() {}
}
