package com.pg85.otg.config.settings.preset;

import com.pg85.otg.config.io.SettingsMap;
import com.pg85.otg.config.settingtype.Setting;
import com.pg85.otg.config.settingtype.Settings;
import com.pg85.otg.config.settings.ConfigSection;
import com.pg85.otg.constants.Constants;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CarverSettings extends ConfigSection {
    private final boolean cavesEnabled;
    private final int caveFrequency;
    private final int caveRarity;
    private final boolean evenCaveDistribution;
    private final int caveMinAltitude;
    private final int caveMaxAltitude;
    private final int caveSystemFrequency;
    private final int individualCaveRarity;
    private final int caveSystemPocketMinSize;
    private final int caveSystemPocketChance;
    private final int caveSystemPocketMaxSize;
    private final boolean vanillaCavesEnabled;
    private final double vanillaCaveDensityScale;
    private final double vanillaCaveDepthGradient;
    private final boolean ravinesEnabled;
    private final int ravineRarity;
    private final int ravineMinLength;
    private final int ravineMaxLength;
    private final double ravineDepth;
    private final int ravineMinAltitude;
    private final int ravineMaxAltitude;

    public static final Setting<Integer> CAVE_RARITY = Settings.intSetting(
            "CaveRarity", 14, 0, 100,
            t -> ((CarverSettings) t).getCaveRarity(),
            "This controls the odds that a given chunk will host a single cave and/or the start of a cave system."
    );
    public static final Setting<Integer> CAVE_FREQUENCY = Settings.intSetting(
            "CaveFrequency", 15, 0, 200,
            t -> ((CarverSettings) t).getCaveFrequency(),
            "The number of times the cave generation algorithm will attempt to create single caves and cave",
            "systems in the given chunk. This value is larger because the likelihood for the cave generation",
            "algorithm to bailout is fairly high and it is used in a randomizer that trends towards lower",
            "random numbers. With an input of 40 (default) the randomizer will result in an average random",
            "result of 5 to 6. This can be turned off by setting evenCaveDistribution (below) to true."
    );
    public static final Setting<Integer> CAVE_MIN_ALTITUDE = Settings.intSetting(
            "CaveMinAltitude", 8, Constants.WORLD_START_MIN_Y, Constants.WORLD_HEIGHT - 1,
            t -> ((CarverSettings) t).getCaveMinAltitude(),
            "Sets the minimum and maximum altitudes at which caves will be generated. These values are",
            "used in a randomizer that trends towards lower numbers so that caves become more frequent",
            "the closer you get to the bottom of the map. Setting even cave distribution (above) to true",
            "will turn off this randomizer and use a flat random number generator that will create an even",
            "density of caves at all altitudes."
    );
    public static final Setting<Integer> CAVE_MAX_ALTITUDE = Settings.intSetting(
            "CaveMaxAltitude", 128, Constants.WORLD_START_MIN_Y, Constants.WORLD_HEIGHT - 1,
            t -> ((CarverSettings) t).getCaveMaxAltitude(),
            "See " + CAVE_MIN_ALTITUDE
    );
    public static final Setting<Integer> INDIVIDUAL_CAVE_RARITY = Settings.intSetting(
            "IndividualCaveRarity", 25, 0, 100,
            t -> ((CarverSettings) t).getIndividualCaveRarity(),
            "The odds that the cave generation algorithm will generate a single cavern without an accompanying",
            "cave system. Note that whenever the algorithm generates an individual cave it will also attempt to",
            "generate a pocket of cave systems in the vicinity (no guarantee of connection or that the cave system",
            "will actually be created)."
    );
    public static final Setting<Integer> CAVE_SYSTEM_FREQUENCY = Settings.intSetting(
            "CaveSystemFrequency", 1, 0, 200,
            t -> ((CarverSettings) t).getCaveSystemFrequency(),
            "The number of times the algorithm will attempt to start a cave system in a given chunk per cycle of",
            "the cave generation algorithm (see cave frequency setting above). Note that setting this value too",
            "high with an accompanying high cave frequency value can cause extremely long world generation time."
    );
    public static final Setting<Integer> CAVE_SYSTEM_POCKET_CHANCE = Settings.intSetting(
            "CaveSystemPocketChance", 0, 0, 100,
            t -> ((CarverSettings) t).getCaveSystemPocketChance(),
            "This can be set to create an additional chance that a cave system pocket (a higher than normal",
            "density of cave systems) being started in a given chunk. Normally, a cave pocket will only be",
            "attempted if an individual cave is generated, but this will allow more cave pockets to be generated",
            "in addition to the individual cave trigger."
    );
    public static final Setting<Integer> CAVE_SYSTEM_POCKET_MIN_SIZE = Settings.intSetting(
            "CaveSystemPocketMinSize", 0, 0, 100,
            t -> ((CarverSettings) t).getCaveSystemPocketMinSize(),
            "The minimum and maximum size that a cave system pocket can be. This modifies/overrides the",
            "cave system frequency setting (above) when triggered."
    );
    public static final Setting<Integer> CAVE_SYSTEM_POCKET_MAX_SIZE = Settings.intSetting(
            "CaveSystemPocketMaxSize", 3, 0, 100,
            t -> ((CarverSettings) t).getCaveSystemPocketMaxSize(),
            "See " + CAVE_SYSTEM_POCKET_MIN_SIZE
    );
    public static final Setting<Integer> RAVINE_RARITY = Settings.intSetting(
            "RavineRarity", 2, 0, 100,
            t -> ((CarverSettings) t).getRavineRarity()
    );
    public static final Setting<Integer> RAVINE_MIN_ALTITUDE = Settings.intSetting(
            "RavineMinAltitude", 20, Constants.WORLD_START_MIN_Y, Constants.WORLD_HEIGHT - 1,
            t -> ((CarverSettings) t).getRavineMinAltitude()
    );
    public static final Setting<Integer> RAVINE_MAX_ALTITUDE = Settings.intSetting(
            "RavineMaxAltitude", 68, Constants.WORLD_START_MIN_Y, Constants.WORLD_HEIGHT - 1,
            t -> ((CarverSettings) t).getRavineMaxAltitude()
    );
    public static final Setting<Integer> RAVINE_MIN_LENGTH = Settings.intSetting(
            "RavineMinLength", 84, 1, 500,
            t -> ((CarverSettings) t).getRavineMinLength()
    );
    public static final Setting<Integer> RAVINE_MAX_LENGTH = Settings.intSetting(
            "RavineMaxLength", 112, 1, 500,
            t -> ((CarverSettings) t).getRavineMaxLength()
    );
    public static final Setting<Boolean> CAVES_ENABLED = Settings.booleanSetting(
            "CavesEnabled", true,
            t -> ((CarverSettings) t).isCavesEnabled(),
            "Enables/disables OTG caves. OTG should automatically disable caves/carvers for biomes when modded carvers are detected."
    );
    public static final Setting<Boolean> EVEN_CAVE_DISTRIBUTION = Settings.booleanSetting(
            "EvenCaveDistribution", false,
            t -> ((CarverSettings) t).isEvenCaveDistribution(),
            "Setting this to true will turn off the randomizer for cave frequency (above). Do note that",
            "if you turn this on you will probably want to adjust the cave frequency down to avoid long",
            "load times at world creation."
    );
    public static final Setting<Boolean> RAVINES_ENABLED = Settings.booleanSetting(
            "RavinesEnabled", true,
            t -> ((CarverSettings) t).isRavinesEnabled(),
            "Enables/disables OTG ravines. OTG should automatically disable ravines/carvers for biomes when modded carvers are detected."
    );
    public static final Setting<Double> RAVINE_DEPTH = Settings.doubleSetting(
            "RavineDepth", 3, 0.1, 15,
            t -> ((CarverSettings) t).getRavineDepth()
    );
    public static final Setting<Boolean> VANILLA_CAVES_ENABLED = Settings.booleanSetting(
            "VanillaCavesEnabled", true,
            t -> ((CarverSettings) t).isVanillaCavesEnabled(),
            "Enables vanilla 1.18+ noise caves (cheese/spaghetti/noodle/pillar caves), carved into OTG's",
            "base terrain via vanilla's density function system. Independent of CavesEnabled/RavinesEnabled,",
            "which control OTG's legacy worm carvers; both can be enabled at once.",
            "Note: with this enabled, stone block replacement comes from surface/ground control layers only,",
            "and caves below a biome's WaterLevelMax will flood (aquifers are not yet supported)."
    );
    public static final Setting<Double> VANILLA_CAVE_DENSITY_SCALE = Settings.doubleSetting(
            "VanillaCaveDensityScale", 128.0, 1.0, 10000.0,
            t -> ((CarverSettings) t).getVanillaCaveDensityScale(),
            "Divisor mapping OTG's raw terrain noise onto vanilla's density scale for noise caves.",
            "This controls how strongly terrain density resists carving near the surface (cave",
            "entrances and cave/terrain blending); the depth at which caves open up is controlled",
            "by VanillaCaveDepthGradient instead."
    );
    public static final Setting<Double> VANILLA_CAVE_DEPTH_GRADIENT = Settings.doubleSetting(
            "VanillaCaveDepthGradient", 0.05, 0.001, 1.0,
            t -> ((CarverSettings) t).getVanillaCaveDepthGradient(),
            "Density-per-block depth gradient for vanilla noise caves, measured from the blended",
            "terrain center height of each column. This makes cave depth independent of biome",
            "volatility, so caves open up at the same depth in flat and mountainous biomes alike.",
            "Full-size caves start at roughly 1.56/value blocks below the terrain center height",
            "(default 0.05 -> ~31 blocks); cheese caves are fully open from ~2.34/value blocks",
            "(default -> ~47 blocks). Higher values move caves closer to the surface."
    );


    public static CarverSettings getCarverSettings(SettingsMap reader, TerrainSettings terrainSettings) {
        CarverSettingsBuilder carverSettingsBuilder = builder();
        // Altitudes can only go below y 0 when the world does, MinY >= 0 keeps the old floor of 0.
        int altitudeFloorY = Math.min(0, terrainSettings.getMinY());

        carverSettingsBuilder.cavesEnabled(reader.getSetting(CAVES_ENABLED));
        carverSettingsBuilder.caveFrequency(reader.getSetting(CAVE_FREQUENCY));
        carverSettingsBuilder.caveRarity(reader.getSetting(CAVE_RARITY));
        carverSettingsBuilder.evenCaveDistribution(reader.getSetting(EVEN_CAVE_DISTRIBUTION));
        carverSettingsBuilder.caveMinAltitude(Math.max(reader.getSetting(CAVE_MIN_ALTITUDE), altitudeFloorY));
        carverSettingsBuilder.caveMaxAltitude(Math.max(reader.getSetting(CAVE_MAX_ALTITUDE), altitudeFloorY));
        carverSettingsBuilder.caveSystemFrequency(reader.getSetting(CAVE_SYSTEM_FREQUENCY));
        carverSettingsBuilder.individualCaveRarity(reader.getSetting(INDIVIDUAL_CAVE_RARITY));
        carverSettingsBuilder.caveSystemPocketChance(reader.getSetting(CAVE_SYSTEM_POCKET_CHANCE));
        carverSettingsBuilder.caveSystemPocketMinSize(reader.getSetting(CAVE_SYSTEM_POCKET_MIN_SIZE));
        carverSettingsBuilder.caveSystemPocketMaxSize(reader.getSetting(CAVE_SYSTEM_POCKET_MAX_SIZE));

        carverSettingsBuilder.vanillaCavesEnabled(reader.getSetting(VANILLA_CAVES_ENABLED));
        carverSettingsBuilder.vanillaCaveDensityScale(reader.getSetting(VANILLA_CAVE_DENSITY_SCALE));
        carverSettingsBuilder.vanillaCaveDepthGradient(reader.getSetting(VANILLA_CAVE_DEPTH_GRADIENT));

        carverSettingsBuilder.ravinesEnabled(reader.getSetting(RAVINES_ENABLED));
        carverSettingsBuilder.ravineRarity(reader.getSetting(RAVINE_RARITY));
        carverSettingsBuilder.ravineMinLength(reader.getSetting(RAVINE_MIN_LENGTH));
        carverSettingsBuilder.ravineMaxLength(reader.getSetting(RAVINE_MAX_LENGTH));
        carverSettingsBuilder.ravineDepth(reader.getSetting(RAVINE_DEPTH));
        carverSettingsBuilder.ravineMinAltitude(Math.max(reader.getSetting(RAVINE_MIN_ALTITUDE), altitudeFloorY));
        carverSettingsBuilder.ravineMaxAltitude(Math.max(reader.getSetting(RAVINE_MAX_ALTITUDE), altitudeFloorY));
        return carverSettingsBuilder.fixSettings().build();
    }

    @Override
    public String getSectionName() {
        return "Carver Settings";
    }

    public static class CarverSettingsBuilder {
        public CarverSettingsBuilder fixSettings() {
            checkCaveMaxAltitude();
            checkCaveSystemPocketMaxSize();
            checkRavineMaxAltitude();
            checkRavineMaxLength();
            return this;
        }
        private void checkCaveMaxAltitude() {
            caveMaxAltitude = Math.max(caveMaxAltitude, caveMinAltitude);
        }
        private void checkCaveSystemPocketMaxSize() {
            caveSystemPocketMaxSize = Math.max(caveSystemPocketMaxSize, caveSystemPocketMinSize);
        }
        private void checkRavineMaxAltitude() {
            ravineMaxAltitude = Math.max(ravineMaxAltitude, ravineMinAltitude);
        }
        private void checkRavineMaxLength() {
            ravineMaxLength = Math.max(ravineMaxLength, ravineMinLength);
        }
    }
}