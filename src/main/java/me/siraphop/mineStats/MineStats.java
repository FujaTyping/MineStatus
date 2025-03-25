package me.siraphop.mineStats;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

import me.siraphop.mineStats.commands.GetUsage;
import me.siraphop.mineStats.commands.SetDisplay;

public final class MineStats extends JavaPlugin {

    private Logger logger = getLogger();

    @Override
    public void onEnable() {
        logger.info("Plugin has been enable!");
        loadCommand();
    }

    @Override
    public void onDisable() {
        logger.info("Plugin has been disable!");
    }

    public void loadCommand() {
        getCommand("mstats").setExecutor(new GetUsage());
        getCommand("mdstats").setExecutor(new SetDisplay());
        logger.info("All commands has been loaded!");
    }
}
