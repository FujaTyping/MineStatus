package me.siraphop.mineStats.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;

import java.nio.file.FileStore;
import java.nio.file.FileSystems;

import java.util.HashMap;
import java.util.Map;

public class SetDisplay implements CommandExecutor {

    private enum DisplayMode {
        ACTION_BAR, TAB_LIST
    }

    private final Map<Player, DisplayMode> displayModes = new HashMap<>();
    private BukkitRunnable task;

    public SetDisplay() {
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
                            displayModes.put(player, DisplayMode.ACTION_BAR);
                            player.sendMessage("§aStats will now display in the action bar.");
                            break;
                        case "tab":
                            displayModes.put(player, DisplayMode.TAB_LIST);
                            player.sendMessage("§aStats will now display in the player list.");
                            break;
                        case "off":
                            displayModes.remove(player);
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

    private void startDisplayTask() {
        if (task != null) {
            task.cancel();
        }

        task = new BukkitRunnable() {
            private final SystemInfo systemInfo = new SystemInfo();
            private final CentralProcessor processor = systemInfo.getHardware().getProcessor();
            private final GlobalMemory memory = systemInfo.getHardware().getMemory();

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

                for (Map.Entry<Player, DisplayMode> entry : displayModes.entrySet()) {
                    Player player = entry.getKey();
                    DisplayMode mode = entry.getValue();

                    if (!player.isOnline()) {
                        displayModes.remove(player);
                        continue;
                    }

                    if (mode == DisplayMode.ACTION_BAR) {
                        player.sendActionBar(statsMessage);
                    } else if (mode == DisplayMode.TAB_LIST) {
                        player.setPlayerListHeaderFooter(
                                "",
                                String.format("§aCPU: §f%.2f%% §aRAM: §f%.2f%% §aDISK: §f%.2f%%",
                                        cpuLoad, memoryUsagePercent, diskUsagePercent)
                        );
                    }
                }
            }
        };

        task.runTaskTimer(Bukkit.getPluginManager().getPlugin("MineStats"), 0L, 40L);
    }
}
