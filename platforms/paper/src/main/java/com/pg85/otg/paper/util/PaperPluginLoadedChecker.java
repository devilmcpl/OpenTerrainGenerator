package com.pg85.otg.paper.util;

import com.pg85.otg.interfaces.IModLoadedChecker;
import com.pg85.otg.util.OTGModChecker;
import org.bukkit.Bukkit;

public class PaperPluginLoadedChecker implements IModLoadedChecker {
    @Override
    public boolean isModLoaded(String mod) {
        // OTG starts during bootstrap, before Bukkit and any other plugins are available
        return Bukkit.getServer() != null && Bukkit.getPluginManager().isPluginEnabled(mod);
    }

    public PaperPluginLoadedChecker() {
        OTGModChecker.set(this);
    }
}
