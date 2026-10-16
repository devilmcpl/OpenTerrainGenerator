package com.pg85.otg.paper.datapack;

import com.pg85.otg.config.settings.preset.*;
import com.pg85.otg.paper.materials.PaperMaterialData;
import com.pg85.otg.presets.Preset;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TimelineTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.timeline.Timeline;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Creates the dimension types and noise settings for OTG presets, the same way RegistryLoaderMixin does on Fabric.
 */
public final class OTGRegistryObjects {

    public static @NotNull NoiseGeneratorSettings createNoiseGeneratorSettings(Preset preset) {
        PresetSettings presetSettings = preset.getPresetConfig();
        DimensionSettings dimensionSettings = presetSettings.getDimensionSettings();
        BlockSettings blockSettings = presetSettings.getBlockSettings();
        ResourceSettings resourceSettings = presetSettings.getResourceSettings();
        return new NoiseGeneratorSettings(
                new NoiseSettings(
                        dimensionSettings.getMinY(),
                        dimensionSettings.getHeight(),
                        1,
                        2
                ),
                ((PaperMaterialData) blockSettings.getDefaultStoneBlock()).getState(),
                ((PaperMaterialData) blockSettings.getWaterBlock()).getState(),
                getZeroNoiseRouter(),
                SurfaceRuleData.overworld(),
                new OverworldBiomeBuilder().spawnTarget(),
                presetSettings.getTerrainSettings().getWaterLevelMax(),
                false, // disableMobGeneration
                false, // is aquifers enabled
                !resourceSettings.isDisableOreGen(), // is veins enabled
                false // use legacy random source
        );
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

    public static @NotNull DimensionType createDimensionType(DimensionSettings settings, HolderGetter<DimensionType> dimensionTypes, HolderGetter<Timeline> timelines) {
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

    private OTGRegistryObjects() {}
}
