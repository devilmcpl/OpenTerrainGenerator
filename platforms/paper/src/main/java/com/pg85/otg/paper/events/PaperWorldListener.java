package com.pg85.otg.paper.events;

import com.pg85.otg.paper.gen.OTGPaperChunkGenerator;
import com.pg85.otg.paper.gen.PaperBukkitGeneratorStub;
import com.pg85.otg.paper.gen.PaperGeneratorInjector;
import com.pg85.otg.util.OTGLog;
import com.pg85.otg.util.logging.LogCategory;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class PaperWorldListener implements Listener {
    @EventHandler
    public void onWorldInit(WorldInitEvent event) {
        ServerLevel serverLevel = ((CraftWorld) event.getWorld()).getHandle();
        // Worlds using OpenTerrainGenerator:<preset> as their generator get the OTG chunk generator injected,
        // this happens before any chunks are generated
        if (event.getWorld().getGenerator() instanceof PaperBukkitGeneratorStub generatorStub) {
            PaperGeneratorInjector.inject(serverLevel, generatorStub.getPresetFolderName());
        } else if (serverLevel.getChunkSource().getGenerator() instanceof OTGPaperChunkGenerator paperChunkGenerator) {
            paperChunkGenerator.setServerLevel(serverLevel);
        }
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent event) {
        OTGPaperChunkGenerator paperChunkGenerator = getGenerator(event.getWorld());
        if (paperChunkGenerator != null) {
            OTGLog.info(LogCategory.STRUCTURE_PLOTTING, "Saving structure cache for world " + paperChunkGenerator.getPreset().getFolderName());
            paperChunkGenerator.saveStructureCache();
        }
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        OTGPaperChunkGenerator paperChunkGenerator = getGenerator(event.getWorld());
        if (paperChunkGenerator != null) {
            paperChunkGenerator.saveStructureCache();
            paperChunkGenerator.stopWorkerThreads();
        }
    }

    private static OTGPaperChunkGenerator getGenerator(World world) {
        ServerLevel serverLevel = ((CraftWorld) world).getHandle();
        return serverLevel.getChunkSource().getGenerator() instanceof OTGPaperChunkGenerator paperChunkGenerator ? paperChunkGenerator : null;
    }
}
