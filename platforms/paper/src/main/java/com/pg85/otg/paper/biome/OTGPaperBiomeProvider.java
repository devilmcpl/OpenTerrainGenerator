package com.pg85.otg.paper.biome;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pg85.otg.OTG;
import com.pg85.otg.gen.biome.layers.BiomeLayers;
import com.pg85.otg.gen.biome.layers.util.CachingLayerSampler;
import com.pg85.otg.interfaces.ILayerSampler;
import com.pg85.otg.interfaces.ILayerSource;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Getter
public class
OTGPaperBiomeProvider extends BiomeSource implements ILayerSource, BiomeManager.NoiseBiomeSource {
    public static final MapCodec<OTGPaperBiomeProvider> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.STRING.fieldOf("preset_name").stable().forGetter(OTGPaperBiomeProvider::getPresetFolderName),
                    Codec.LONG.fieldOf("seed").stable().forGetter(OTGPaperBiomeProvider::getSeed),
                    RegistryOps.retrieveGetter(Registries.BIOME)
            ).apply(instance, instance.stable(OTGPaperBiomeProvider::new)));
    private final String presetFolderName;
    //this.layer = ThreadLocal.withInitial(() -> BiomeLayers.create(seed, OTG.getEngine().getPresetLoader().getPresetGenerationData().get(presetFolderName), OTG.getEngine().getLogger()));
    private final CountDownLatch latch = new CountDownLatch(1);
    private long seed;
    private ThreadLocal<CachingLayerSampler> layer;
    private final Int2ObjectOpenHashMap<Holder<Biome>> keyLookup = new Int2ObjectOpenHashMap<>();

    public OTGPaperBiomeProvider(String presetFolderName, long seed, HolderGetter<Biome> biomeRegistry) {
        this.presetFolderName = presetFolderName;
        this.seed = seed;
        // OTG biomes are loaded from the OTG datapack, bind them before anything asks for the possible biomes
        ((PaperPresetLoader) OTG.getEngine().getPresetLoader()).bindHolders(presetFolderName, biomeRegistry);
    }

    @Override
    public ILayerSampler getSampler() {
        return layer.get();
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected @NotNull Stream<Holder<Biome>> collectPossibleBiomes() {
        var iBiomes = OTG.getEngine().getPresetLoader().getGlobalIdMapping(presetFolderName);
        if (iBiomes == null) {
            OTG.getEngine().getLogger().log(LogLevel.ERROR, LogCategory.BIOME_REGISTRY,
                    "Biome mapping for preset " + presetFolderName + " is null.");
            return Stream.empty();
        }
        for (int otgBiomeID = 0; otgBiomeID < iBiomes.length; otgBiomeID++) {
            keyLookup.put(otgBiomeID, ((PaperBiome) iBiomes[otgBiomeID]).getBiomeHolder());
        }
        return Stream.of(iBiomes).map(iBiome -> ((PaperBiome) iBiome).getBiomeHolder());
    }

    @Override
    public @Nullable Pair<BlockPos, Holder<Biome>> findBiomeHorizontal(int i, int j, int k, int l, int m, Predicate<Holder<Biome>> predicate, RandomSource randomSource, boolean bl, Climate.Sampler sampler) {
        if (this.getLayer() == null) {
            throw new IllegalStateException("Layer is null. Seed was not set properly.");
        }
        return super.findBiomeHorizontal(i, j, k, l, m, predicate, randomSource, bl, sampler);
    }

    @Override
    public @Nullable Pair<BlockPos, Holder<Biome>> findBiomeHorizontal(int i, int j, int k, int l, Predicate<Holder<Biome>> predicate, RandomSource randomSource, Climate.Sampler sampler) {
        if (this.getLayer() == null) {
            throw new IllegalStateException("Layer is null. Seed was not set properly.");
        }
        return super.findBiomeHorizontal(i, j, k, l, predicate, randomSource, sampler);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int i, int j, int k, Climate.Sampler sampler) {
        return keyLookup.get(this.getLayer().get().sample(i, k));
    }

    @Override
    public Holder<Biome> getNoiseBiome(int i, int j, int k) {
        return keyLookup.get(this.getLayer().get().sample(i, k));
    }

    public void setSeed(long seed) {
        synchronized (this) {
            if (this.seed == seed && this.layer != null) {
                return;
            }
            if (this.seed != seed && this.layer != null) {
                OTG.getEngine().getLogger().log(LogLevel.ERROR, LogCategory.MAIN,
                        "Biome provider seed changed from " + this.seed + " to " + seed +
                                ". This is unexpected and may lead to inconsistent biome generation.");
            }
            this.seed = seed;
            layer = ThreadLocal.withInitial(() -> BiomeLayers.create(seed, OTG.getEngine().getPresetLoader().getPresetGenerationData().get(presetFolderName), OTG.getEngine().getLogger()));
            latch.countDown();
        }
    }
}
