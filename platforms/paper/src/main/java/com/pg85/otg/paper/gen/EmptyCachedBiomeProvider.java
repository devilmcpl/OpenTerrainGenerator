package com.pg85.otg.paper.gen;

import com.pg85.otg.config.settings.biome.BiomeSettings;
import com.pg85.otg.interfaces.IBiome;
import com.pg85.otg.interfaces.ICachedBiomeProvider;
import com.pg85.otg.util.ChunkCoordinate;

/**
 * Biome provider for non-OTG worlds, which have no OTG biomes. Used so PaperLevelRegion can be
 * created for those worlds, OTG features that need biomes aren't available there.
 */
public class EmptyCachedBiomeProvider implements ICachedBiomeProvider {
    @Override
    public BiomeSettings[] getBiomeConfigsForChunk(ChunkCoordinate chunkCoordinate) {
        return new BiomeSettings[0];
    }

    @Override
    public IBiome[] getBiomesForChunk(ChunkCoordinate chunkCoordinate) {
        return new IBiome[0];
    }

    @Override
    public IBiome[] getBiomesForChunks(ChunkCoordinate chunkCoord, int widthHeightInChunks) {
        return new IBiome[0];
    }

    @Override
    public BiomeSettings getBiomeConfig(int x, int z, boolean cacheChunk) {
        return null;
    }

    @Override
    public BiomeSettings getBiomeConfig(int x, int z) {
        return null;
    }

    @Override
    public IBiome getBiome(int x, int z) {
        return null;
    }

    @Override
    public BiomeSettings[] getNoiseBiomeConfigsForRegion(int noiseStartX, int noiseStartZ, int widthHeight) {
        return new BiomeSettings[0];
    }

    @Override
    public BiomeSettings getNoiseBiomeConfig(int x, int z, boolean cacheChunk) {
        return null;
    }

    @Override
    public IBiome getNoiseBiome(int x, int z) {
        return null;
    }
}
