package me.siraphop.mineStats.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SetDisplay implements CommandExecutor, Listener {

    private enum DisplayMode {
        ACTION_BAR, TAB_LIST
    }

    private final Map<UUID, DisplayMode> displayModes = new HashMap<>();
    private BukkitRunnable task;
    private final File playerDataFile;
    private final FileConfiguration playerData;

    private final SystemInfo systemInfo = new SystemInfo();
    private final CentralProcessor processor = systemInfo.getHardware().getProcessor();
    private final GlobalMemory memory = systemInfo.getHardware().getMemory();

    public SetDisplay() {
        playerDataFile = new File(Bukkit.getPluginManager().getPlugin("MineStats").getDataFolder(), "player_data.yml");
        if (!playerDataFile.exists()) {
            try {
                playerDataFile.getParentFile().mkdirs();
                playerDataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        playerData = YamlConfiguration.loadConfiguration(playerDataFile);

        startDisplayTask();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("mdstats")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;

                if (args.length == 1) {
                    String mode = args[0].toLowerCase();

                    switch (mode) {
                        case "actionbar":
                            setDisplayMode(player, DisplayMode.ACTION_BAR);
                            player.sendMessage("§aStats will now display in the action bar.");
                            break;
                        case "tab":
                            setDisplayMode(player, DisplayMode.TAB_LIST);
                            player.sendMessage("§aStats will now display in the player list.");
                            break;
                        case "off":
                            removeDisplayMode(player);
                            player.sendMessage("§cStats display disabled.");
                            player.setPlayerListHeaderFooter("", "");
                            break;
                        default:
                            player.sendMessage("§cInvalid mode. Use /mdstats [actionbar/tab/off]");
                            break;
                    }
                } else {
                    player.sendMessage("§cUsage: /mdstats [actionbar/tab/off]");
                }

                return true;
            } else {
                sender.sendMessage("§cOnly players can use this command.");
                return true;
            }
        }

        return false;
    }

    private void setDisplayMode(Player player, DisplayMode mode) {
        displayModes.put(player.getUniqueId(), mode);
        playerData.set(player.getUniqueId().toString(), mode.name());
        savePlayerData();
        updateDisplayForPlayer(player, mode);

        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.5F);
    }

    private void removeDisplayMode(Player player) {
        displayModes.remove(player.getUniqueId());
        playerData.set(player.getUniqueId().toString(), null);
        savePlayerData();

        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 0.5F);

        player.setPlayerListHeaderFooter("", "");
    }

    private void savePlayerData() {
        try {
            playerData.save(playerDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startDisplayTask() {
        if (task != null) {
            task.cancel();
        }

        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (displayModes.isEmpty()) {
                    return;
                }

                double cpuLoad = processor.getSystemCpuLoad(500) * 100;

                double totalMemoryGB = memory.getTotal() / (1024.0 * 1024 * 1024);
                double freeMemoryGB = memory.getAvailable() / (1024.0 * 1024 * 1024);
                double usedMemoryGB = totalMemoryGB - freeMemoryGB;
                double memoryUsagePercent = (usedMemoryGB / totalMemoryGB) * 100;

                long totalDiskSpace = 0;
                long freeDiskSpace = 0;
                long usedDiskSpace = 0;
                double diskUsagePercent = 0;

                try {
                    for (FileStore store : FileSystems.getDefault().getFileStores()) {
                        totalDiskSpace += store.getTotalSpace();
                        freeDiskSpace += store.getUnallocatedSpace();
                    }
                    usedDiskSpace = totalDiskSpace - freeDiskSpace;
                    if (totalDiskSpace > 0) {
                        diskUsagePercent = (double) usedDiskSpace / totalDiskSpace * 100;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                String statsMessage = String.format(
                        "§aCPU: §f%.2f%% §aRAM: §f%.2f%% §aDISK: §f%.2f%%",
                        cpuLoad, memoryUsagePercent, diskUsagePercent
                );

                for (Map.Entry<UUID, DisplayMode> entry : displayModes.entrySet()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player == null || !player.isOnline()) {
                        continue;
                    }

                    DisplayMode mode = entry.getValue();

                    if (mode == DisplayMode.ACTION_BAR) {
                        player.sendActionBar(statsMessage);
                    } else if (mode == DisplayMode.TAB_LIST) {
                        player.setPlayerListHeaderFooter("", statsMessage);
                    }
                }
            }
        };

        task.runTaskTimer(Bukkit.getPluginManager().getPlugin("MineStats"), 0L, 40L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String mode = playerData.getString(player.getUniqueId().toString());

        if (mode != null) {
            DisplayMode displayMode = DisplayMode.valueOf(mode);
            displayModes.put(player.getUniqueId(), displayMode);
            player.sendMessage("§aYour stats display has been restored to " + mode.toLowerCase() + " mode.");

            Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("MineStats"), () -> {
                updateDisplayForPlayer(player, displayMode);
            }, 20L);
        }
    }

    private void updateDisplayForPlayer(Player player, DisplayMode mode) {
        if (displayModes.containsKey(player.getUniqueId())) {
            task.run();
        }
    }
}
