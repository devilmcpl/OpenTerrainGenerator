package com.pg85.otg.paper.commands;

import com.pg85.otg.OTG;
import com.pg85.otg.constants.Constants;
import com.pg85.otg.customobject.CustomObject;
import com.pg85.otg.customobject.bofunctions.BlockFunction;
import com.pg85.otg.customobject.creator.ObjectCreator;
import com.pg85.otg.customobject.creator.ObjectType;
import com.pg85.otg.customobject.structures.StructuredCustomObject;
import com.pg85.otg.customobject.util.BoundingBox;
import com.pg85.otg.customobject.util.Corner;
import com.pg85.otg.paper.commands.region.Region;
import com.pg85.otg.paper.util.PaperNBTHelper;
import com.pg85.otg.presets.Preset;
import com.pg85.otg.util.OTGMaterialReader;
import com.pg85.otg.util.bo3.Rotation;
import com.pg85.otg.util.gen.LocalWorldGenRegion;
import com.pg85.otg.util.materials.LocalMaterials;
import net.minecraft.core.BlockPos;

import java.nio.file.Path;
import java.util.List;

final class ObjectCommandUtils {

    /** Get the path to the object folder, either Objects, WorldObjects, or GlobalObjects,
     * whichever is appropriate.
     *
     * @param presetFolder The path to the preset in question, or null if global
     * @return The path to the object folder
     */
    static Path getObjectFolderPath(Path presetFolder) {
        Path objectPath;
        if (presetFolder == null) {
            objectPath = OTG.getEngine().getOTGRootFolder().resolve(Constants.GLOBAL_OBJECTS_FOLDER);
        } else {
            objectPath = presetFolder.resolve(Constants.OBJECTS_FOLDER);
        }

        if (!objectPath.toFile().exists() && objectPath.resolveSibling(Constants.LEGACY_WORLD_OBJECTS_FOLDER).toFile().exists()) {
            objectPath = objectPath.resolveSibling(Constants.LEGACY_WORLD_OBJECTS_FOLDER);
        }
        return objectPath;
    }

    /** Fetches the preset from its name. If given a null string, returns the default preset. */
    static Preset getPresetOrDefault(String presetName) {
        if (presetName == null) {
            return OTG.getEngine().getPresetLoader().getPresetByShortNameOrFolderName(OTG.getEngine().getPresetLoader().getDefaultPresetFolderName());
        }
        return OTG.getEngine().getPresetLoader().getPresetByShortNameOrFolderName(presetName);
    }

    /** Gets what folder a given object is stored in, relative to the base object folder. */
    static String getFoldersFromObject(StructuredCustomObject object) {
        Path parent = object.getConfig().getFile().toPath().getParent();
        StringBuilder sb = new StringBuilder();
        // Search down to the Objects folder
        while (
            !parent.getFileName().toString().equalsIgnoreCase(Constants.LEGACY_WORLD_OBJECTS_FOLDER) &&
            !parent.getFileName().toString().equalsIgnoreCase(Constants.OBJECTS_FOLDER) &&
            !parent.getFileName().toString().equalsIgnoreCase(Constants.GLOBAL_OBJECTS_FOLDER)
        ) {
            sb.insert(0, "/");
            sb.insert(0, parent.getFileName());
            parent = parent.getParent();
        }
        return sb.toString();
    }

    /** Cleans an area to prepare it for spawning an object for editing, or cleans up after it is done editing.
     *
     * @param preparing When preparing, the area is filled with structure void. If not, fills with air.
     */
    static void cleanArea(LocalWorldGenRegion region, Corner min, Corner max, boolean preparing) {
        for (int x1 = min.x() - 1; x1 <= max.x() + 1; x1++) {
            for (int z1 = min.z() - 1; z1 <= max.z() + 1; z1++) {
                for (int y1 = min.y() - 1; y1 <= max.y() + 1; y1++) {
                    region.setBlock(x1, y1, z1, preparing ? LocalMaterials.STRUCTURE_VOID : LocalMaterials.AIR);
                }
            }
        }
    }

    /** Whether an object is larger than the bounds of its type. */
    static boolean isOutsideBounds(Region region, ObjectType type) {
        Corner min = region.getMin();
        Corner max = region.getMax();
        int xlen = Math.abs(max.x() - min.x());
        int zlen = Math.abs(max.z() - min.z());
        return switch (type) {
            case BO3 -> xlen > 31 || zlen > 31;
            case BO4 -> xlen > 15 || zlen > 15;
            default -> false;
        };
    }

    /** Gets a region large enough to fit an object, positioned away from the player. */
    static Region getRegionFromObject(BlockPos pos, StructuredCustomObject object, int worldMinY, int worldMaxY) {
        Region region = new Region();
        BoundingBox box = object.getBoundingBox(Rotation.NORTH);
        // Make the object not spawn on top of the player
        pos = pos.offset(3, 0, 3);

        int lowestElevation = pos.getY() + box.getMinY();
        int highestElevation = pos.getY() + box.getMinY() + box.getHeight();

        int yshift = 0;
        if (lowestElevation <= worldMinY + 1) {
            yshift = (worldMinY - lowestElevation) + 2;
        } else if (highestElevation >= worldMaxY) {
            yshift = worldMaxY - highestElevation;
        }

        Corner center = new Corner(pos.getX() + 2 + (box.getWidth() / 2), pos.getY() + yshift, pos.getZ() + 2 + (box.getDepth() / 2));

        region.setPos(new BlockPos(
            center.x() + box.getMinX(),
            lowestElevation + yshift,
            center.z() + box.getMinZ()));
        region.setPos(new BlockPos(
            center.x() + box.getMinX() + box.getWidth(),
            highestElevation + yshift,
            center.z() + box.getMinZ() + box.getDepth()));
        region.setCenter(center);
        return region;
    }

    /** Gets an object. If preset is null, uses the default preset instead. */
    static CustomObject getObject(String objectName, String presetFolderName) {
        if (presetFolderName == null) {
            presetFolderName = OTG.getEngine().getPresetLoader().getDefaultPresetFolderName();
        }
        return OTG.getEngine().getCustomObjectManager().getGlobalObjects().getObjectByName(
            objectName,
            presetFolderName,
            OTG.getEngine().getOTGRootFolder()
        );
    }

    /** Exports the area of an object that was spawned for editing, then cleans up the area. */
    static StructuredCustomObject exportEditedObject(ObjectType type, Region region, Corner center, StructuredCustomObject inputObject, Path exportPath,
                                                     List<BlockFunction<?>> extraBlocks, String presetFolderName, boolean leaveIllegalLeaves, LocalWorldGenRegion worldGenRegion) {
        StructuredCustomObject fixedObject = ObjectCreator.createObject(
            type,
            region.getMin(),
            region.getMax(),
            center,
            null,
            inputObject.getName(),
            false,
            leaveIllegalLeaves,
            exportPath,
            worldGenRegion,
            new PaperNBTHelper(),
            extraBlocks,
            inputObject.getConfig(),
            presetFolderName,
            OTG.getEngine().getOTGRootFolder(),
            OTG.getEngine().getCustomObjectManager(),
            OTGMaterialReader.get(),
            OTG.getEngine().getCustomObjectResourcesManager(),
            OTG.getEngine().getModLoadedChecker()
        );
        if (fixedObject != null) {
            OTG.getEngine().getCustomObjectManager().getGlobalObjects().addObjectToPreset(presetFolderName, fixedObject.getName(), fixedObject.getConfig().getFile(), fixedObject);
        }
        cleanArea(worldGenRegion, region.getMin(), region.getMax(), false);
        return fixedObject;
    }

    private ObjectCommandUtils() {}
}
