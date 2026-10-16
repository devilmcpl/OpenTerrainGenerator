package com.pg85.otg.paper.commands.region;

import com.pg85.otg.paper.integration.WorldEditBridge;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Region selections made with /otg region, per player. */
public final class RegionSelections {
    private static final Map<UUID, Region> selections = new ConcurrentHashMap<>();

    public static Region get(UUID player) {
        return selections.computeIfAbsent(player, uuid -> new Region());
    }

    public static void set(UUID player, Region region) {
        selections.put(player, region);
    }

    public static void remove(UUID player) {
        selections.remove(player);
    }

    /** Gets the player's WorldEdit selection if WorldEdit is installed and the player has one, else their /otg region selection. */
    public static @Nullable Region getSelection(Player player) {
        Region region = WorldEditBridge.getSelection(player);
        if (region == null) {
            region = selections.get(player.getUniqueId());
        }
        if (region == null || region.getMin() == null || region.getMax() == null) {
            return null;
        }
        return region;
    }

    private RegionSelections() {}
}
