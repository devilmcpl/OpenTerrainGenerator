package com.pg85.otg.paper.integration;

import com.pg85.otg.paper.commands.region.Region;
import com.pg85.otg.paper.integration.worldedit.WorldEditSelectionSource;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * WorldEdit is optional, so WorldEdit classes are only referenced from WorldEditSelectionSource,
 * which is only loaded when WorldEdit is installed.
 */
public final class WorldEditBridge {

    public static boolean isWorldEditEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("WorldEdit");
    }

    public static @Nullable Region getSelection(Player player) {
        if (!isWorldEditEnabled()) {
            return null;
        }
        try {
            return WorldEditSelectionSource.getSelection(player);
        } catch (LinkageError e) {
            // WorldEdit is installed but isn't a version we can use
            return null;
        }
    }

    private WorldEditBridge() {}
}
