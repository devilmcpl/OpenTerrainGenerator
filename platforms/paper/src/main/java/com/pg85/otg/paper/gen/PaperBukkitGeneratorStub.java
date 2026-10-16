package com.pg85.otg.paper.gen;

import lombok.Getter;
import org.bukkit.generator.ChunkGenerator;

/**
 * Bukkit generator for worlds that set OpenTerrainGenerator:<preset> as their generator. It doesn't generate
 * anything itself, the OTG chunk generator is injected into the world when it's initialised, see PaperGeneratorInjector.
 */
@Getter
public class PaperBukkitGeneratorStub extends ChunkGenerator {
    private final String presetFolderName;

    public PaperBukkitGeneratorStub(String presetFolderName) {
        this.presetFolderName = presetFolderName;
    }
}
