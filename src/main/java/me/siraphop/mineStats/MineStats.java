package me.siraphop.mineStats;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;
import me.siraphop.mineStats.commands.GetUsage;

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
    }
}
