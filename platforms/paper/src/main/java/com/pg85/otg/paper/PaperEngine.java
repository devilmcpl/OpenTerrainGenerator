package com.pg85.otg.paper;

import com.pg85.otg.OTGEngine;
import com.pg85.otg.paper.biome.PaperPresetLoader;
import com.pg85.otg.paper.materials.PaperMaterials;
import com.pg85.otg.paper.util.PaperPluginLoadedChecker;
import com.pg85.otg.util.OTGLog;

import java.io.File;
import java.nio.file.Path;

public class PaperEngine extends OTGEngine {
    private final Path pluginSource;

    protected PaperEngine(Path dataDirectory, Path pluginSource) {
        super(
                OTGLog.getLogger(),
                dataDirectory,
                new PaperPluginLoadedChecker(),
                new PaperPresetLoader(dataDirectory)
        );
        this.pluginSource = pluginSource;
    }

    @Override
    public void onStart() {
        PaperMaterials.init();
        super.onStart();
    }

    @Override
    public File getJarFile() {
        return this.pluginSource.toFile();
    }
}
