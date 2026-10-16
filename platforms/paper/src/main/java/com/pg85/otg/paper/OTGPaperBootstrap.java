package com.pg85.otg.paper;

import com.pg85.otg.OTG;
import com.pg85.otg.constants.Constants;
import com.pg85.otg.paper.biome.OTGPaperBiomeProvider;
import com.pg85.otg.paper.biome.PaperPresetLoader;
import com.pg85.otg.paper.datapack.PaperDatapackGenerator;
import com.pg85.otg.paper.gen.OTGPaperChunkGenerator;
import com.pg85.otg.paper.materials.PaperMaterialReader;
import com.pg85.otg.paper.util.PaperLogger;
import com.pg85.otg.util.OTGLog;
import com.pg85.otg.util.OTGMaterialReader;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.file.Path;

@SuppressWarnings({"UnstableApiUsage", "unused"})
public class OTGPaperBootstrap implements PluginBootstrap {
    private Path datapackPath = null;

    @Override
    public void bootstrap(BootstrapContext context) {
        // Bootstrappers run before the built-in registries are frozen, so we can register our generator and biome source here
        Registry.register(BuiltInRegistries.BIOME_SOURCE, Identifier.fromNamespaceAndPath(Constants.MOD_ID_SHORT, Constants.MOD_ID_SHORT), OTGPaperBiomeProvider.CODEC);
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, Identifier.fromNamespaceAndPath(Constants.MOD_ID_SHORT, Constants.MOD_ID_SHORT), OTGPaperChunkGenerator.CODEC);

        // Datapacks are discovered after Minecraft has bootstrapped, but before the worldgen registries are loaded,
        // which is when OTG starts and writes its biomes and world presets to its datapack.
        context.getLifecycleManager().registerEventHandler(LifecycleEvents.DATAPACK_DISCOVERY, event -> {
            if (this.datapackPath == null) {
                this.datapackPath = startEngine(context);
            }
            if (this.datapackPath == null) {
                return;
            }
            try {
                event.registrar().discoverPack(this.datapackPath, Constants.MOD_ID_SHORT, configurer -> configurer.autoEnableOnServerStart(true));
            } catch (IOException e) {
                OTGLog.log(LogLevel.FATAL, LogCategory.MAIN, "Could not register the OTG datapack: " + e.getMessage());
            }
        });
    }

    private Path startEngine(BootstrapContext context) {
        OTGLog.setLogger(new PaperLogger());
        OTGLog.getLogger().log(LogLevel.INFO, LogCategory.MAIN, "OTG Engine starting");
        OTGMaterialReader.set(new PaperMaterialReader());
        OTG.startEngine(new PaperEngine(context.getDataDirectory(), context.getPluginSource()));
        ((PaperPresetLoader) OTG.getEngine().getPresetLoader()).buildLayouts();
        OTG.log("OTG Engine started, presets loaded");

        try {
            return PaperDatapackGenerator.generate(context.getDataDirectory().resolve("GeneratedDatapack"));
        } catch (IOException e) {
            OTGLog.log(LogLevel.FATAL, LogCategory.MAIN, "Could not write the OTG datapack, OTG worlds will not work: " + e.getMessage());
            return null;
        }
    }
}
