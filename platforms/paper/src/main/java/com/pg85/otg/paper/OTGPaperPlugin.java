package com.pg85.otg.paper;

import com.pg85.otg.OTG;
import com.pg85.otg.paper.commands.OTGCommand;
import com.pg85.otg.paper.events.PaperWorldListener;
import com.pg85.otg.paper.events.SaplingHandler;
import com.pg85.otg.paper.gen.OTGPaperChunkGenerator;
import com.pg85.otg.paper.gen.PaperBukkitGeneratorStub;
import com.pg85.otg.paper.networking.BiomeSyncListener;
import com.pg85.otg.presets.Preset;
import com.pg85.otg.util.OTGLog;
import com.pg85.otg.util.logging.LogCategory;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"UnstableApiUsage", "unused"})
public class OTGPaperPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new PaperWorldListener(this), this);
        getServer().getPluginManager().registerEvents(new SaplingHandler(), this);
        getServer().getPluginManager().registerEvents(new BiomeSyncListener(this), this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, BiomeSyncListener.CHANNEL);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> OTGCommand.register(event.registrar()));
    }

    @Override
    public void onDisable() {
        // Worlds are saved after plugins are disabled, so save our structure caches here
        for (World world : getServer().getWorlds()) {
            ServerLevel serverLevel = ((CraftWorld) world).getHandle();
            if (serverLevel.getChunkSource().getGenerator() instanceof OTGPaperChunkGenerator paperChunkGenerator) {
                OTGLog.info(LogCategory.STRUCTURE_PLOTTING, "Saving structure cache for world " + paperChunkGenerator.getPreset().getFolderName());
                paperChunkGenerator.saveStructureCache();
                paperChunkGenerator.stopWorkerThreads();
            }
        }
    }

    // Used for worlds that set OpenTerrainGenerator:<preset> as their generator, for instance in bukkit.yml
    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(String worldName, @Nullable String id) {
        String presetName = id == null || id.isBlank() ? OTG.getEngine().getPresetLoader().getDefaultPresetFolderName() : id;
        Preset preset = OTG.getEngine().getPresetLoader().getPresetByShortNameOrFolderName(presetName);
        if (preset == null) {
            OTGLog.error("Could not find preset " + presetName + " for world " + worldName);
            return null;
        }
        return new PaperBukkitGeneratorStub(preset.getFolderName());
    }
}
