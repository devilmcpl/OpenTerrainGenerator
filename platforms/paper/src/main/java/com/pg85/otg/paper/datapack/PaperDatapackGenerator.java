package com.pg85.otg.paper.datapack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.pg85.otg.OTG;
import com.pg85.otg.constants.Constants;
import com.pg85.otg.paper.biome.PaperPresetLoader;
import com.pg85.otg.presets.Preset;
import com.pg85.otg.util.OTGLog;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;
import com.pg85.otg.util.minecraft.OTGDimensionType;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;

/**
 * Paper has no way to add biomes and world presets to the registries directly, so OTG writes them to a datapack
 * that is loaded through the normal datapack loading. This replaces RegistryLoaderMixin, BiomeDataMixin and
 * WorldPresetTagsMixin on Fabric. The datapack is regenerated on every startup.
 */
public final class PaperDatapackGenerator {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    // OTG block tags, shipped in the plugin jar under otg-datapack
    private static final String[] BLOCK_TAGS = {
            "air", "concrete", "dirt", "leaves", "log", "long_grass", "prismarine", "quartz_block", "red_flower",
            "red_sandstone", "sand", "sandstone", "stained_clay", "stone"
    };

    public static Path generate(Path datapackDir) throws IOException {
        Path tempDir = datapackDir.resolveSibling(datapackDir.getFileName() + "_tmp");
        deleteRecursively(tempDir);
        Files.createDirectories(tempDir);

        HolderLookup.Provider vanillaRegistries = VanillaRegistries.createLookup();
        // The serialization context knows which holders belong to which registry, both for the vanilla worldgen
        // registries and the built-in registries (sound events etc.)
        RegistryOps<JsonElement> ops = vanillaRegistries.createSerializationContext(JsonOps.INSTANCE);
        PaperPresetLoader presetLoader = (PaperPresetLoader) OTG.getEngine().getPresetLoader();

        // Biomes
        Map<ResourceKey<Biome>, Biome> biomes = presetLoader.createBiomes(
                vanillaRegistries.lookupOrThrow(Registries.PLACED_FEATURE),
                vanillaRegistries.lookupOrThrow(Registries.CONFIGURED_CARVER),
                vanillaRegistries.lookupOrThrow(Registries.BIOME)
        );
        for (Map.Entry<ResourceKey<Biome>, Biome> entry : biomes.entrySet()) {
            writeJson(tempDir, "worldgen/biome", entry.getKey().identifier(), Biome.DIRECT_CODEC.encodeStart(ops, entry.getValue()).getOrThrow());
        }

        // Dimension types and noise settings
        Map<String, JsonElement> noiseSettingsById = new HashMap<>();
        for (Preset preset : presetLoader.getAllPresets()) {
            if (preset.getPresetConfig().getDimensionSettings().getDimensionType() == OTGDimensionType.OTG) {
                Identifier id = Identifier.fromNamespaceAndPath(Constants.MOD_ID_SHORT, preset.getPresetRegistryName().toLowerCase(Locale.ROOT));
                DimensionType dimensionType = OTGRegistryObjects.createDimensionType(
                        preset.getPresetConfig().getDimensionSettings(),
                        vanillaRegistries.lookupOrThrow(Registries.DIMENSION_TYPE),
                        vanillaRegistries.lookupOrThrow(Registries.TIMELINE)
                );
                writeJson(tempDir, "dimension_type", id, DimensionType.DIRECT_CODEC.encodeStart(ops, dimensionType).getOrThrow());
            }

            Identifier noiseId = Identifier.fromNamespaceAndPath(Constants.MOD_ID_SHORT, preset.getPresetRegistryName());
            NoiseGeneratorSettings noiseGeneratorSettings = OTGRegistryObjects.createNoiseGeneratorSettings(preset);
            JsonElement noiseSettingsJson = NoiseGeneratorSettings.DIRECT_CODEC.encodeStart(ops, noiseGeneratorSettings).getOrThrow();
            writeJson(tempDir, "worldgen/noise_settings", noiseId, noiseSettingsJson);
            noiseSettingsById.put(noiseId.toString(), noiseSettingsJson);
        }
        for (ResourceKey<NoiseGeneratorSettings> vanillaNoiseSettings : List.of(NoiseGeneratorSettings.OVERWORLD, NoiseGeneratorSettings.NETHER, NoiseGeneratorSettings.END)) {
            NoiseGeneratorSettings noiseGeneratorSettings = vanillaRegistries.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(vanillaNoiseSettings).value();
            noiseSettingsById.put(vanillaNoiseSettings.identifier().toString(), NoiseGeneratorSettings.DIRECT_CODEC.encodeStart(ops, noiseGeneratorSettings).getOrThrow());
        }

        // World presets
        JsonArray worldPresetTag = new JsonArray();
        for (Preset preset : presetLoader.getAllPresets()) {
            if (preset.getPresetConfig().getPresetInfo().isSelectableInWorldCreation()) {
                Identifier presetId = Identifier.fromNamespaceAndPath(Constants.MOD_ID_SHORT, preset.getPresetRegistryName().toLowerCase(Locale.ROOT));
                writeJson(tempDir, "worldgen/world_preset", presetId, createWorldPreset(preset, presetLoader, noiseSettingsById));
                worldPresetTag.add(presetId.toString());
                OTGLog.getLogger().info("Registered world preset: " + presetId);
            }
        }

        // Add the presets to the normal world presets
        JsonObject normalTag = new JsonObject();
        normalTag.addProperty("replace", false);
        normalTag.add("values", worldPresetTag);
        writeJson(tempDir, "tags/worldgen/world_preset", Identifier.withDefaultNamespace("normal"), normalTag);

        for (String blockTag : BLOCK_TAGS) {
            try (InputStream inputStream = PaperDatapackGenerator.class.getResourceAsStream("/otg-datapack/data/otg/tags/block/" + blockTag + ".json")) {
                if (inputStream == null) {
                    OTGLog.log(LogLevel.ERROR, LogCategory.MAIN, "Could not find block tag " + blockTag + " in the plugin jar");
                    continue;
                }
                Path target = tempDir.resolve("data/otg/tags/block/" + blockTag + ".json");
                Files.createDirectories(target.getParent());
                Files.copy(inputStream, target);
            }
        }

        PackFormat packFormat = SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA);
        Files.writeString(tempDir.resolve("pack.mcmeta"), """
                {
                    "pack": {
                        "description": "Biomes and world presets for OpenTerrainGenerator",
                        "min_format": [%d, %d],
                        "max_format": [%d, %d]
                    }
                }
                """.formatted(packFormat.major(), packFormat.minor(), packFormat.major(), packFormat.minor()), StandardCharsets.UTF_8);

        deleteRecursively(datapackDir);
        Files.move(tempDir, datapackDir, StandardCopyOption.ATOMIC_MOVE);
        return datapackDir;
    }

    // Creates the world preset for an OTG preset, the same way RegistryLoaderMixin.createLevelStems does on Fabric
    private static JsonObject createWorldPreset(Preset preset, PaperPresetLoader presetLoader, Map<String, JsonElement> noiseSettingsById) {
        JsonObject dimensions = new JsonObject();
        int counter = 0;
        for (String dim : preset.getDimensionNames()) {
            String levelStem;
            if (counter == 0) {
                levelStem = "minecraft:overworld";
            } else if (counter == 1) {
                levelStem = "minecraft:the_nether";
            } else if (counter == 2) {
                levelStem = "minecraft:the_end";
            } else {
                levelStem = Identifier.parse(dim).toString();
            }

            JsonObject stem = new JsonObject();
            if (dim.startsWith(Constants.MOD_ID_SHORT)) {
                Preset dimPreset = null;
                for (Preset p : presetLoader.getAllPresets()) {
                    if (dim.equals(Constants.MOD_ID_SHORT + ":" + p.getPresetRegistryName())) {
                        dimPreset = p;
                        break;
                    }
                }
                if (dimPreset == null) {
                    OTGLog.getLogger().error("Could not find preset for dimension %s", dim);
                    continue;
                }

                String otgId = Identifier.fromNamespaceAndPath(Constants.MOD_ID_SHORT, dimPreset.getPresetRegistryName()).toString();
                String dimensionType = switch (dimPreset.getPresetConfig().getDimensionSettings().getDimensionType()) {
                    case OTG -> otgId;
                    case OVERWORLD -> "minecraft:overworld";
                    case NETHER -> "minecraft:the_nether";
                    case END -> "minecraft:the_end";
                };
                String noiseSettings = switch (dimPreset.getPresetConfig().getDimensionSettings().getDimensionType()) {
                    case OTG -> otgId;
                    case OVERWORLD -> "minecraft:overworld";
                    case NETHER -> "minecraft:nether";
                    case END -> "minecraft:end";
                };

                JsonObject biomeSource = new JsonObject();
                biomeSource.addProperty("type", Constants.MOD_ID_SHORT + ":" + Constants.MOD_ID_SHORT);
                biomeSource.addProperty("preset_name", preset.getFolderName());
                biomeSource.addProperty("seed", 0L);
                JsonObject generator = new JsonObject();
                generator.addProperty("type", Constants.MOD_ID_SHORT + ":" + Constants.MOD_ID_SHORT);
                generator.add("biome_source", biomeSource);
                // World presets are loaded before registry values are bound, but the OTG chunk generator needs its noise
                // settings right away, so they're included in the preset instead of referenced by id
                generator.add("settings", noiseSettingsById.get(noiseSettings).deepCopy());

                stem.addProperty("type", dimensionType);
                stem.add("generator", generator);
            } else {
                String dimensionType = Identifier.parse(dim).toString();
                JsonObject biomeSource = new JsonObject();
                String noiseSettings;
                switch (dimensionType) {
                    case "minecraft:overworld" -> {
                        biomeSource.addProperty("type", "minecraft:multi_noise");
                        biomeSource.addProperty("preset", "minecraft:overworld");
                        noiseSettings = "minecraft:overworld";
                    }
                    case "minecraft:the_nether" -> {
                        biomeSource.addProperty("type", "minecraft:multi_noise");
                        biomeSource.addProperty("preset", "minecraft:nether");
                        noiseSettings = "minecraft:nether";
                    }
                    case "minecraft:the_end" -> {
                        biomeSource.addProperty("type", "minecraft:the_end");
                        noiseSettings = "minecraft:end";
                    }
                    default -> {
                        // how do we find an appropriate generator for a given non-otg dimension?
                        OTGLog.error("Non-OTG dimension %s not yet supported", dimensionType);
                        continue;
                    }
                }
                JsonObject generator = new JsonObject();
                generator.addProperty("type", "minecraft:noise");
                generator.add("biome_source", biomeSource);
                generator.addProperty("settings", noiseSettings);

                stem.addProperty("type", dimensionType);
                stem.add("generator", generator);
            }
            dimensions.add(levelStem, stem);
            counter++;
        }

        JsonObject worldPreset = new JsonObject();
        worldPreset.add("dimensions", dimensions);
        return worldPreset;
    }

    private static void writeJson(Path root, String registryPath, Identifier id, JsonElement json) throws IOException {
        Path path = root.resolve("data/" + id.getNamespace() + "/" + registryPath + "/" + id.getPath() + ".json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(json), StandardCharsets.UTF_8);
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(path)) {
            for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }

    private PaperDatapackGenerator() {}
}
