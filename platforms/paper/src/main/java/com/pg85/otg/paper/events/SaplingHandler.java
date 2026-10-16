package com.pg85.otg.paper.events;

import com.pg85.otg.OTG;
import com.pg85.otg.config.settings.biome.BiomeSettings;
import com.pg85.otg.customobject.resource.SaplingResource;
import com.pg85.otg.interfaces.ISaplingSpawner;
import com.pg85.otg.interfaces.IWorldGenRegion;
import com.pg85.otg.paper.gen.OTGPaperChunkGenerator;
import com.pg85.otg.paper.gen.PaperLevelRegion;
import com.pg85.otg.paper.materials.PaperMaterialData;
import com.pg85.otg.presets.Preset;
import com.pg85.otg.util.materials.LocalMaterialData;
import com.pg85.otg.util.materials.LocalMaterials;
import com.pg85.otg.util.minecraft.SaplingType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.StructureGrowEvent;

import java.util.Random;

/** Grows the custom trees set with Sapling() in biome configs of OTG worlds. */
public class SaplingHandler implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        ServerLevel level = ((CraftWorld) event.getWorld()).getHandle();
        if (!(level.getChunkSource().getGenerator() instanceof OTGPaperChunkGenerator generator)) {
            return;
        }

        Preset preset = generator.getPreset();
        BlockPos blockPos = new BlockPos(event.getLocation().getBlockX(), event.getLocation().getBlockY(), event.getLocation().getBlockZ());
        PaperLevelRegion worldGenRegion = PaperLevelRegion.create(level, preset, blockPos);

        BiomeSettings biomeConfig = worldGenRegion.getCachedBiomeProvider().getBiomeConfig(blockPos.getX(), blockPos.getZ());
        PaperMaterialData material = (PaperMaterialData) worldGenRegion.getMaterial(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        LocalMaterialData saplingMaterial = PaperMaterialData.ofBlockState(material.getState().getBlock().defaultBlockState());

        BlockPos result = findFourSaplings(blockPos, material, worldGenRegion);
        boolean wideTrunk;
        if (result != null) {
            blockPos = result;
            wideTrunk = true;
        } else {
            // Not a wide trunk
            wideTrunk = false;
        }

        // Get the sapling generator
        ISaplingSpawner sapling = biomeConfig.getCustomSaplingGen(saplingMaterial, wideTrunk);
        if (sapling != null && sapling.hasWideTrunk() && !wideTrunk) {
            return;
        }

        if (sapling == null) {
            // Check whether block is a sapling. If not, assume custom sapling block.
            SaplingType saplingType = null;
            if (wideTrunk) {
                // Try to find big (2x2) sapling
                saplingType = getBigSaplingType(saplingMaterial);
            }

            // If not big sapling, try to find small sapling
            if (saplingType == null) {
                saplingType = getSmallSaplingType(saplingMaterial);
            }

            if (saplingType != null) {
                sapling = biomeConfig.getSaplingGen(saplingType);
            }
        }

        if (sapling == null) {
            // No sapling generator set for this sapling
            return;
        }

        // When we have reached this point, we know that we have to handle the event ourselves, so cancel it
        event.setCancelled(true);

        // Remove saplings
        setSaplings(worldGenRegion, blockPos, sapling.hasWideTrunk(), LocalMaterials.AIR);

        // Try ten times to grow sapling
        boolean saplingGrown = false;
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            if (((SaplingResource) sapling).growSapling(worldGenRegion, random, wideTrunk, blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                preset.getFolderName(), OTG.getEngine().getOTGRootFolder(), OTG.getEngine().getCustomObjectManager())) {
                saplingGrown = true;
                break;
            }
        }

        if (!saplingGrown) {
            // Restore sapling
            setSaplings(worldGenRegion, blockPos, sapling.hasWideTrunk(), material);
        }
    }

    private static void setSaplings(IWorldGenRegion worldGenRegion, BlockPos blockPos, boolean wideTrunk, LocalMaterialData material) {
        worldGenRegion.setBlock(blockPos.getX(), blockPos.getY(), blockPos.getZ(), material);
        if (wideTrunk) {
            worldGenRegion.setBlock(blockPos.getX() + 1, blockPos.getY(), blockPos.getZ(), material);
            worldGenRegion.setBlock(blockPos.getX(), blockPos.getY(), blockPos.getZ() + 1, material);
            worldGenRegion.setBlock(blockPos.getX() + 1, blockPos.getY(), blockPos.getZ() + 1, material);
        }
    }

    /**
     * Gets the sapling type, based on the assumption that the sapling is
     * not placed in a 2x2 pattern.
     *
     * @param saplingMaterial The sapling material
     * @return The sapling type, or null if not found.
     */
    private static SaplingType getSmallSaplingType(LocalMaterialData saplingMaterial) {
        if (saplingMaterial.isMaterial(LocalMaterials.OAK_SAPLING)) {
            return SaplingType.Oak;
        } else if (saplingMaterial.isMaterial(LocalMaterials.SPRUCE_SAPLING)) {
            return SaplingType.Redwood;
        } else if (saplingMaterial.isMaterial(LocalMaterials.BIRCH_SAPLING)) {
            return SaplingType.Birch;
        } else if (saplingMaterial.isMaterial(LocalMaterials.JUNGLE_SAPLING)) {
            return SaplingType.SmallJungle;
        } else if (saplingMaterial.isMaterial(LocalMaterials.ACACIA_SAPLING)) {
            return SaplingType.Acacia;
        } else if (saplingMaterial.isMaterial(LocalMaterials.BAMBOO_SAPLING)) {
            return SaplingType.Bamboo;
        }
        return null;
    }

    /**
     * Gets the sapling type, based on the assumption that the saplings must
     * be placed in a 2x2 pattern. Will never return one of the smaller
     * sapling types.
     *
     * @param saplingMaterial The material of the sapling.
     * @return The sapling type, or null if not found.
     */
    private static SaplingType getBigSaplingType(LocalMaterialData saplingMaterial) {
        if (saplingMaterial.isMaterial(LocalMaterials.DARK_OAK_SAPLING)) {
            return SaplingType.DarkOak;
        } else if (saplingMaterial.isMaterial(LocalMaterials.SPRUCE_SAPLING)) {
            return SaplingType.HugeRedwood;
        } else if (saplingMaterial.isMaterial(LocalMaterials.JUNGLE_SAPLING)) {
            return SaplingType.BigJungle;
        }
        return null;
    }

    /**
     * Gets whether the saplings are placed in a 2x2 pattern. If successful,
     * it returns a BlockPos that represents the top left
     * sapling (with the lowest x and z). If not, it returns null.
     *
     * @return BlockPos of sapling with lowest X and Z, or null if not four saplings
     */
    private static BlockPos findFourSaplings(BlockPos blockPos, LocalMaterialData material, IWorldGenRegion worldGenRegion) {
        int x = blockPos.getX();
        int y = blockPos.getY();
        int z = blockPos.getZ();
        for (int treeOffsetX = 0; treeOffsetX >= -1; --treeOffsetX) {
            for (int treeOffsetZ = 0; treeOffsetZ >= -1; --treeOffsetZ) {
                if (
                    material.isMaterial(worldGenRegion.getMaterial(x + treeOffsetX, y, z + treeOffsetZ))
                    && material.isMaterial(worldGenRegion.getMaterial(x + treeOffsetX + 1, y, z + treeOffsetZ))
                    && material.isMaterial(worldGenRegion.getMaterial(x + treeOffsetX, y, z + treeOffsetZ + 1))
                    && material.isMaterial(worldGenRegion.getMaterial(x + treeOffsetX + 1, y, z + treeOffsetZ + 1))
                ) {
                    // Found! Adjust internal position
                    return blockPos.offset(treeOffsetX, 0, treeOffsetZ);
                }
            }
        }
        return null;
    }
}
