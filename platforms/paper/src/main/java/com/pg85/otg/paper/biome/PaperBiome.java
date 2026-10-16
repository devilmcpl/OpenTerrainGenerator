package com.pg85.otg.paper.biome;

import com.pg85.otg.config.settings.biome.BiomeSettings;
import com.pg85.otg.interfaces.IBiome;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

@Getter
public class PaperBiome implements IBiome {
    private final BiomeSettings biomeSettings;
    private final ResourceKey<Biome> biomeKey;
    // Biomes are registered through the OTG datapack, so the holder is bound once the datapack has been loaded
    @Setter
    private volatile Holder.Reference<Biome> biomeHolder;

    public PaperBiome(BiomeSettings biomeSettings, ResourceKey<Biome> biomeKey) {
        this.biomeSettings = biomeSettings;
        this.biomeKey = biomeKey;
    }

    public Biome getBiome() {
        return this.biomeHolder.value();
    }

    @Override
    public float getTemperatureAt(int x, int y, int z) {
        return getBiome().getBaseTemperature();
    }
}
