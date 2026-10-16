package com.pg85.otg.paper.integration.worldedit;

import com.pg85.otg.paper.commands.region.Region;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import net.minecraft.core.BlockPos;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/** The only class that uses WorldEdit, see WorldEditBridge. */
public final class WorldEditSelectionSource {

    public static @Nullable Region getSelection(Player player) {
        LocalSession session = WorldEdit.getInstance().getSessionManager().getIfPresent(BukkitAdapter.adapt(player));
        if (session == null) {
            return null;
        }
        try {
            com.sk89q.worldedit.regions.Region selection = session.getSelection(BukkitAdapter.adapt(player.getWorld()));
            Region region = new Region();
            region.setPos1(getPosFromVector3(selection.getMinimumPoint()));
            region.setPos2(getPosFromVector3(selection.getMaximumPoint()));
            return region;
        } catch (IncompleteRegionException e) {
            return null;
        }
    }

    private static BlockPos getPosFromVector3(BlockVector3 vector) {
        return new BlockPos(vector.x(), vector.y(), vector.z());
    }

    private WorldEditSelectionSource() {}
}
