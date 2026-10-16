package com.pg85.otg.fabric.mixin;

import com.pg85.otg.OTG;
import com.pg85.otg.config.settings.preset.*;
import com.pg85.otg.constants.Constants;
import com.pg85.otg.fabric.biome.FabricBiomeLoader;
import com.pg85.otg.fabric.biome.LegacyFabricBiomeLoader;
import com.pg85.otg.fabric.biome.OTGFabricBiomeProvider;
import com.pg85.otg.fabric.gen.OTGFabricChunkGenerator;
import com.pg85.otg.fabric.materials.FabricMaterialData;
import com.pg85.otg.presets.Preset;
import com.pg85.otg.util.OTGLog;
import com.pg85.otg.util.minecraft.OTGDimensionType;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TimelineTags;
import net.minecraft.tags.WorldPresetTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.timeline.Timeline;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.*;

@Mixin(RegistryDataLoader.class)
@SuppressWarnings("unused") // Mixins are by nature unused
public class RegistryLoaderMixin {
    // Explainer: The injector below needs to match:
    // 1. The method signature of the target method
    // 2. The CallbackInfoReturnable parameter (not normal CallbackInfo)
    // 3. The local variables in the target method, in order
    // Only then do we get access to the list of registries
    @Inject(
            method = "load(Lnet/minecraft/resources/RegistryDataLoader$LoadingFunction;Ljava/util/List;" +
                    "Ljava/util/List;)Lnet/minecraft/core/RegistryAccess$Frozen;",
            at = @At(
                value = "INVOKE",
                target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V",
                ordinal = 1
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    @SuppressWarnings("rawtypes") // Raw types required for this to work, not used in the code
    private static void loadOTGPresets(RegistryDataLoader.LoadingFunction loadingFunction, List<HolderLookup.RegistryLookup<?>> contextRegistries,
                                       List<RegistryDataLoader.RegistryData<?>> list, CallbackInfoReturnable ci, Map errorMap,
                                       List<RegistryDataLoader.Loader<?>> registries) {
        if (getRegistry(registries, Registries.WORLD_PRESET) == null) {
            // Only the worldgen registries contain world presets. Vanilla auto-registers the level stems based on the
            // world preset, and clients load the registries synced from the server (which already contain our biomes)
            // through this same method, so we can ignore both
            return;
        }

        OTGLog.getLogger().info("Registering the following OTG presets: %s", OTG.getEngine().getPresetLoader().getAllPresets());

        //printAllRegistriesForDebug(registries);

        // register biomes
        registerBiomes(registries);

        // Register our dimension types in the format otg:preset
        HashMap<Preset, ResourceKey<DimensionType>> dimensionTypes = registerDimensionTypes(registries);

        // register noise generator settings for each preset
        for (Preset preset : dimensionTypes.keySet()) {
            registerNoiseGenSettings(preset, registries);
        }

        WritableRegistry<WorldPreset> worldPresets = getRegistry(registries, Registries.WORLD_PRESET);
        if (worldPresets == null) {
            OTGLog.getLogger().error("Could not find world preset registry");
            return;
        }

        for (Preset preset : dimensionTypes.keySet()) {
            if (!preset.getPresetConfig().getPresetInfo().isSelectableInWorldCreation()) {
                continue;
            }

            OTGLog.getLogger().info("Registering world preset: %s", dimensionTypes.get(preset).identifier());

            // create level stems, with at least one using the Overworld resource key
            Map<ResourceKey<LevelStem>, LevelStem> levelStems = createLevelStems(preset, registries);

            // register world presets, using the level stems
            registerWorldPresets(preset, worldPresets, levelStems);
        }
    }

    private static void registerBiomes(List<RegistryDataLoader.Loader<?>> registries) {
        if (OTG.getEngine().getPluginConfig().getDeveloperModeEnabled()) {
            // clear all the caches
            OTG.getEngine().getCustomObjectManager().reloadCustomObjectFiles();
            OTG.getEngine().getPresetLoader().loadPresetsFromDisk();
        }

        LegacyFabricBiomeLoader loader = (LegacyFabricBiomeLoader) OTG.getEngine().getPresetLoader();
        WritableRegistry<Biome> biomeWritableRegistry = getRegistry(registries, Registries.BIOME);
        if (biomeWritableRegistry == null) {
            OTGLog.getLogger().error("Could not find biome registry");
            return;
        }
        // Features and carvers are fully loaded at this point, so we can use them to build our biomes
        HolderGetter<PlacedFeature> placedFeatures = getRegistryOrThrow(registries, Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = getRegistryOrThrow(registries, Registries.CONFIGURED_CARVER);
        LegacyFabricBiomeLoader.PLACED_FEATURE_HOLDER = placedFeatures;
        LegacyFabricBiomeLoader.CONFIGURED_CARVER_HOLDER = carvers;
        LegacyFabricBiomeLoader.BIOME_DATA_INITIALIZED = true;

        FabricBiomeLoader.PLACED_FEATURE_HOLDER = placedFeatures;
        FabricBiomeLoader.CONFIGURED_CARVER_HOLDER = carvers;
        FabricBiomeLoader.BIOME_DATA_INITIALIZED = true;

        loader.registerBiomes(biomeWritableRegistry);
    }

    private static void printAllRegistriesForDebug(List<RegistryDataLoader.Loader<?>> registries) {
        System.out.println("--*--");
        for (RegistryDataLoader.Loader<?> registry : registries) {
            OTGLog.info("Registry: %s", registry.registry().key());
            //OTGLog.info("Object: %s", registry.getSecond());
        }
        System.out.println("--*--");
    }

    private static Map<ResourceKey<LevelStem>, LevelStem> createLevelStems(
            Preset preset,
            List<RegistryDataLoader.Loader<?>> registries
    ) {
        var dimensionNames = preset.getDimensionNames();
        int counter = 0;

        Map<ResourceKey<LevelStem>, LevelStem> levelStems = new HashMap<>();
        HolderGetter<DimensionType> dimensionHolders = getRegistryOrThrow(registries, Registries.DIMENSION_TYPE);
        HolderGetter<NoiseGeneratorSettings> noiseHolders = getRegistryOrThrow(registries, Registries.NOISE_SETTINGS);

        for (String dim : dimensionNames) {
            ResourceKey<LevelStem> key;
            LevelStem levelStem;
            if (counter == 0) {
                key = LevelStem.OVERWORLD;
            } else if (counter == 1) {
                key = LevelStem.NETHER;
            } else if (counter == 2) {
                key = LevelStem.END;
            } else {
                key = ResourceKey.create(Registries.LEVEL_STEM, Identifier.parse(dim));
            }

            ChunkGenerator chunkGenerator;

            if (dim.startsWith(Constants.MOD_ID_SHORT)) {
                Preset dimPreset = null;
                for (Preset p : OTG.getEngine().getPresetLoader().getAllPresets()) {
                    if (dim.equals(Constants.MOD_ID_SHORT + ":" + p.getPresetRegistryName())) {
                        dimPreset = p;
                        break;
                    }
                }
                if (dimPreset == null) {
                    OTGLog.getLogger().error("Could not find preset for dimension %s", dim);
                    continue;
                }

                ResourceKey<DimensionType> dimensionKey = switch (dimPreset.getPresetConfig().getDimensionSettings().getDimensionType()) {
                    // OTG dimension
                    case OTG -> ResourceKey.create(Registries.DIMENSION_TYPE, Identifier.fromNamespaceAndPath(Constants.MOD_ID_SHORT, dimPreset.getPresetRegistryName()));
                    case OVERWORLD -> BuiltinDimensionTypes.OVERWORLD;
                    case NETHER -> BuiltinDimensionTypes.NETHER;
                    case END -> BuiltinDimensionTypes.END;
                };

                ResourceKey<NoiseGeneratorSettings> noiseKey = switch (dimPreset.getPresetConfig().getDimensionSettings().getDimensionType()) {
                    case OVERWORLD -> NoiseGeneratorSettings.OVERWORLD;
                    case NETHER -> NoiseGeneratorSettings.NETHER;
                    case END -> NoiseGeneratorSettings.END;
                    case OTG -> ResourceKey.create(Registries.NOISE_SETTINGS, Identifier.fromNamespaceAndPath(Constants.MOD_ID_SHORT, dimPreset.getPresetRegistryName()));
                };

                Holder.Reference<DimensionType> dimensionReference = dimensionHolders.getOrThrow(dimensionKey);
                if (!dimensionReference.isBound()) {
                    OTGLog.getLogger().error("Dimension reference for dimension %s is not bound", dim);
                    //continue;
                }

                Holder.Reference<NoiseGeneratorSettings> noiseReference = noiseHolders.getOrThrow(noiseKey);
                if (!noiseReference.isBound()) {
                    OTGLog.getLogger().error("Noise reference for dimension %s is not bound", dim);
                    //continue;
                }

                Registry<Biome> biomeRegistry = getRegistryOrThrow(registries, Registries.BIOME);

                chunkGenerator = new OTGFabricChunkGenerator(
                        new OTGFabricBiomeProvider(preset.getFolderName(), 0L),
                        noiseReference,
                        biomeRegistry
                );
                levelStem = new LevelStem(
                        dimensionReference,
                        chunkGenerator
                );
            } else {
                ResourceKey<DimensionType> dimensionKey = ResourceKey.create(Registries.DIMENSION_TYPE, Identifier.parse(dim));
                Optional<Holder.Reference<DimensionType>> dimensionTypeHolder = dimensionHolders.get(dimensionKey);
                if (dimensionTypeHolder.isEmpty()) {
                    OTGLog.getLogger().error("Could not find dimension reference for dimension %s", dimensionKey.identifier());
                    continue;
                }
                if (!dimensionTypeHolder.get().isBound()) {
                    OTGLog.getLogger().error("Dimension reference for dimension %s is not bound", dimensionKey.identifier());
                    continue;
                }

                if (dimensionKey == BuiltinDimensionTypes.OVERWORLD) {
                    Holder<MultiNoiseBiomeSourceParameterList> overworldBiomeSource = getRegistryOrThrow(registries, Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                            .getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD);
                    if (!overworldBiomeSource.isBound()) {
                        OTGLog.getLogger().error("Overworld biome source is not bound");
                        //continue;
                    }
                    Holder<NoiseGeneratorSettings> overworldNoise = noiseHolders.getOrThrow(NoiseGeneratorSettings.OVERWORLD);
                    chunkGenerator = new NoiseBasedChunkGenerator(
                            MultiNoiseBiomeSource.createFromPreset(overworldBiomeSource),
                            overworldNoise
                    );
                } else if (dimensionKey == BuiltinDimensionTypes.NETHER) {
                    Holder<MultiNoiseBiomeSourceParameterList> netherBiomeSource = getRegistryOrThrow(registries, Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                            .getOrThrow(MultiNoiseBiomeSourceParameterLists.NETHER);
                    if (!netherBiomeSource.isBound()) {
                        OTGLog.getLogger().error("Nether biome source is not bound");
                        //continue;
                    }
                    Holder<NoiseGeneratorSettings> netherNoise = noiseHolders.getOrThrow(NoiseGeneratorSettings.NETHER);
                    chunkGenerator = new NoiseBasedChunkGenerator(
                            MultiNoiseBiomeSource.createFromPreset(netherBiomeSource),
                            netherNoise
                    );
                } else if (dimensionKey == BuiltinDimensionTypes.END) {
                    var biomes = getRegistryOrThrow(registries, Registries.BIOME);
                    Holder<NoiseGeneratorSettings> endNoise = noiseHolders.getOrThrow(NoiseGeneratorSettings.END);
                    chunkGenerator = new NoiseBasedChunkGenerator(
                            TheEndBiomeSource.create(biomes),
                            endNoise
                    );
                } else {
                    // how do we find an appropriate generator for a given non-otg dimension?
                    OTGLog.error("Non-OTG dimension %s not yet supported", dimensionKey.identifier());
                    continue;
                }

                levelStem = new LevelStem(
                        dimensionTypeHolder.get(),
                        chunkGenerator
                );
            }
            levelStems.put(key, levelStem);
            counter++;
        }

        return levelStems;
    }

    private static void registerWorldPresets(
            Preset preset,
            WritableRegistry<WorldPreset> worldPresets,
            Map<ResourceKey<LevelStem>, LevelStem> levelStems
    ) {
        OTGLog.info("%s", levelStems.keySet());
        OTGLog.info("%s", levelStems.values());
        // for this preset, we set up a WorldPreset with an overworld, nether and end according to its settings
        // potentially also with custom dimensions set up under OTG tags
        // or with all vanilla biomes, plus an OTG dimension
        // (latter will be good for testing a single preset, hopefully)

        WorldPreset worldPreset = new WorldPreset(levelStems);
        // create a world preset for thepreset
        Identifier id = Identifier.fromNamespaceAndPath(Constants.MOD_ID_SHORT, preset.getPresetRegistryName().toLowerCase(Locale.ROOT));
        ResourceKey<WorldPreset> key = ResourceKey.create(Registries.WORLD_PRESET, id);
        Holder.Reference<WorldPreset> worldPresetHolder = worldPresets.register(key, worldPreset, RegistrationInfo.BUILT_IN);
        OTGLog.getLogger().info("Registered world preset: " + key.identifier());

        // Add the preset to the normal world presets so it shows up in world creation. Tags are already bound
        // at this point, so we rebind the tag with our preset added
        List<Holder<WorldPreset>> normalPresets = new ArrayList<>(worldPresets.createRegistrationLookup().getOrThrow(WorldPresetTags.NORMAL).stream().toList());
        normalPresets.add(worldPresetHolder);
        worldPresets.bindTag(WorldPresetTags.NORMAL, normalPresets);
    }

    private static void registerNoiseGenSettings(
            Preset preset,
            List<RegistryDataLoader.Loader<?>> registries) {
        PresetSettings presetSettings = preset.getPresetConfig();
        DimensionSettings dimensionSettings = presetSettings.getDimensionSettings();
        BlockSettings blockSettings = presetSettings.getBlockSettings();
        ResourceSettings resourceSettings = presetSettings.getResourceSettings();
        var ngs = new NoiseGeneratorSettings(
                new NoiseSettings(
                        dimensionSettings.getMinY(),
                        dimensionSettings.getHeight(),
                        1,
                        2
                ),
                ((FabricMaterialData) blockSettings.getDefaultStoneBlock()).getState(),
                ((FabricMaterialData) blockSettings.getWaterBlock()).getState(),
                getZeroNoiseRouter(),
                SurfaceRuleData.overworld(),
                new OverworldBiomeBuilder().spawnTarget(),
                presetSettings.getTerrainSettings().getWaterLevelMax(),
                false, // disableMobGeneration
                false, // is aquifers enabled
                !resourceSettings.isDisableOreGen(), // is veins enabled
                false // use legacy random source
        );

        // register the noise settings
        WritableRegistry<NoiseGeneratorSettings> registry = getRegistry(registries, Registries.NOISE_SETTINGS);
        if (registry == null) {
            throw new RuntimeException("Could not find noise settings registry");
        }
        ResourceKey<NoiseGeneratorSettings> key = ResourceKey.create(Registries.NOISE_SETTINGS, Identifier.fromNamespaceAndPath(Constants.MOD_ID_SHORT, preset.getPresetRegistryName()));
        registry.register(key, ngs, RegistrationInfo.BUILT_IN);
    }

    private static @NotNull NoiseRouter getZeroNoiseRouter() {
        return new NoiseRouter(
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero()
        );
    }

    private static @NotNull HashMap<Preset, ResourceKey<DimensionType>> registerDimensionTypes(List<RegistryDataLoader.Loader<?>> list2) {
        var map = new HashMap<Preset, ResourceKey<DimensionType>>();
        for (Preset preset : OTG.getEngine().getPresetLoader().getAllPresets()) {
            OTGDimensionType otgDimensionType = preset.getPresetConfig().getDimensionSettings().getDimensionType();
            ResourceKey<DimensionType> dimensionKey = switch (otgDimensionType) {
                case OVERWORLD -> BuiltinDimensionTypes.OVERWORLD;
                case NETHER -> BuiltinDimensionTypes.NETHER;
                case END -> BuiltinDimensionTypes.END;
                case OTG -> {
                    Identifier id = Identifier.fromNamespaceAndPath(Constants.MOD_ID_SHORT, preset.getPresetRegistryName().toLowerCase(Locale.ROOT));
                    ResourceKey<DimensionType> dimensionTypeKey = ResourceKey.create(Registries.DIMENSION_TYPE, id);
                    // create settings for OTG dimension
                    WritableRegistry<DimensionType> dimensionTypes = getRegistryOrThrow(list2, Registries.DIMENSION_TYPE);
                    DimensionType dimensionType = getDimensionType(preset.getPresetConfig().getDimensionSettings(), dimensionTypes, getRegistryOrThrow(list2, Registries.TIMELINE).createRegistrationLookup());
                    // register the dimension
                    dimensionTypes.register(dimensionTypeKey, dimensionType, RegistrationInfo.BUILT_IN);
                    OTGLog.info("Registered dimension type: %s", dimensionTypeKey.identifier());
                    // return the key for use elsewhere
                    yield dimensionTypeKey;
                }
            };
            map.put(preset, dimensionKey);
        }
        return map;
    }

    private static @NotNull DimensionType getDimensionType(DimensionSettings settings, HolderGetter<DimensionType> dimensionTypes, HolderGetter<Timeline> timelines) {
        // Sky, fog, music, ambient sounds etc. are environment attributes stored on the dimension type now, so we
        // start from the vanilla dimension type matching the effects setting and apply our settings on top of it
        String effects = settings.getEffectsLocation().toLowerCase(Locale.ROOT);
        ResourceKey<DimensionType> vanillaType;
        DimensionType.Skybox skybox;
        DimensionType.CardinalLightType cardinalLightType;
        TagKey<Timeline> timelineTag;
        if (effects.equals("minecraft:the_nether")) {
            vanillaType = BuiltinDimensionTypes.NETHER;
            skybox = DimensionType.Skybox.NONE;
            cardinalLightType = DimensionType.CardinalLightType.NETHER;
            timelineTag = TimelineTags.IN_NETHER;
        } else if (effects.equals("minecraft:the_end")) {
            vanillaType = BuiltinDimensionTypes.END;
            skybox = DimensionType.Skybox.END;
            cardinalLightType = DimensionType.CardinalLightType.DEFAULT;
            timelineTag = TimelineTags.IN_END;
        } else {
            vanillaType = BuiltinDimensionTypes.OVERWORLD;
            skybox = DimensionType.Skybox.OVERWORLD;
            cardinalLightType = DimensionType.CardinalLightType.DEFAULT;
            timelineTag = TimelineTags.IN_OVERWORLD;
        }

        // ultraWarm, bedWorks, respawnAnchorWorks, piglinSafe and hasRaids are environment attributes now.
        // natural has no direct replacement, nether portals spawning piglins is the closest match
        EnvironmentAttributeMap attributes = EnvironmentAttributeMap.builder()
                .putAll(dimensionTypes.getOrThrow(vanillaType).value().attributes())
                .set(EnvironmentAttributes.WATER_EVAPORATES, settings.isUltraWarm())
                .set(EnvironmentAttributes.FAST_LAVA, settings.isUltraWarm())
                .set(EnvironmentAttributes.NETHER_PORTAL_SPAWNS_PIGLINS, settings.isNatural())
                .set(EnvironmentAttributes.BED_RULE, settings.isBedWorks() ? BedRule.CAN_SLEEP_WHEN_DARK : BedRule.EXPLODES)
                .set(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, settings.isRespawnAnchorWorks())
                .set(EnvironmentAttributes.PIGLINS_ZOMBIFY, !settings.isPiglinSafe())
                .set(EnvironmentAttributes.CAN_START_RAID, settings.isHasRaids())
                .build();

        return new DimensionType(
                // The fixed time itself is driven by timelines now, only whether the time is fixed can be set here
                settings.getFixedTime().isPresent(),
                settings.isHasSkyLight(),
                settings.isHasCeiling(),
                settings.getCoordinateScale(),
                settings.getMinY(),
                settings.getHeight(),
                settings.getLogicalHeight(),
                TagKey.create(Registries.BLOCK, Identifier.parse(settings.getInfiniburn())),
                (float) settings.getAmbientLight(),
                new DimensionType.MonsterSettings(
                        // monsterSpawnLightTest
                        UniformInt.of(
                                settings.getMonsterSpawnLightVariationMin(),
                                settings.getMonsterSpawnLightVariationMax()
                        ),
                        // monsterSpawnBlockLightLimit
                        settings.getMonsterSpawnLightLimit()
                ),
                skybox,
                cardinalLightType,
                attributes,
                timelines.getOrThrow(timelineTag)
        );
    }

    private static <T> WritableRegistry<T> getRegistryOrThrow(List<RegistryDataLoader.Loader<?>> registries, ResourceKey<? extends Registry<T>> key) {
        WritableRegistry<T> registry = getRegistry(registries, key);
        if (registry == null) {
            throw new RuntimeException("Could not find registry for key " + key.identifier());
        }
        return registry;
    }

    @SuppressWarnings("unchecked")
    private static <T> WritableRegistry<T> getRegistry(List<RegistryDataLoader.Loader<?>> registries, ResourceKey<? extends Registry<T>> key) {
        List<? extends WritableRegistry<?>> x = registries.stream()
                .map(RegistryDataLoader.Loader::registry)
                .filter(result -> result.key().equals(key))
                .toList();
        if (x.isEmpty()) {
            return null;
        }
        return (WritableRegistry<T>) x.get(0);
    }
}
