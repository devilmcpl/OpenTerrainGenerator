package com.pg85.otg.paper.api;

import com.pg85.otg.config.settings.biome.BiomeSettings;
import com.pg85.otg.paper.gen.OTGPaperChunkGenerator;
import com.pg85.otg.presets.Preset;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** API for other plugins. */
public class OTGPaperAPI {

    /** Gets the OTG biome settings at the given location, or null if the world isn't an OTG world. */
    public static @Nullable BiomeSettings getOTGBiome(Location location) {
        OTGPaperChunkGenerator generator = getGenerator(location.getWorld());
        if (generator == null) {
            return null;
        }
        return generator.getInternalGenerator().getCachedBiomeProvider().getBiomeConfig(location.getBlockX(), location.getBlockZ());
    }

    public static Optional<BiomeSettings> getOTGBiomeOptional(Location location) {
        return Optional.ofNullable(getOTGBiome(location));
    }

    public static boolean isOTGWorld(World world) {
        return getGenerator(world) != null;
    }

    /** Gets the preset used by the given world, or null if the world isn't an OTG world. */
    public static @Nullable Preset getPreset(World world) {
        OTGPaperChunkGenerator generator = getGenerator(world);
        return generator == null ? null : generator.getPreset();
    }

    private static @Nullable OTGPaperChunkGenerator getGenerator(@Nullable World world) {
        if (world == null) {
            return null;
        }
        return ((CraftWorld) world).getHandle().getChunkSource().getGenerator() instanceof OTGPaperChunkGenerator generator ? generator : null;
    }
}
