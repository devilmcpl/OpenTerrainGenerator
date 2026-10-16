package com.pg85.otg.paper.commands;

import com.pg85.otg.OTG;
import com.pg85.otg.customobject.creator.ObjectType;
import com.pg85.otg.util.materials.LocalMaterialData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Per player settings for /otg export, set with /otg configexport. */
final class ExportOptions {
    private static final Map<UUID, ExportOptions> options = new ConcurrentHashMap<>();

    String preset = OTG.getEngine().getPresetLoader().getDefaultPresetFolderName();
    String template = "default";
    ObjectType objectType = ObjectType.BO3;
    LocalMaterialData centerBlock = null;
    boolean overwrite = false;
    boolean exportToGlobal = false;
    boolean includeAir = false;
    final List<LocalMaterialData> filter = new ArrayList<>();

    static ExportOptions get(UUID player) {
        return options.computeIfAbsent(player, uuid -> new ExportOptions());
    }

    static void remove(UUID player) {
        options.remove(player);
    }
}
