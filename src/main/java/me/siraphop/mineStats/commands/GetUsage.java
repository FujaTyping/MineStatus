package me.siraphop.mineStats.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.software.os.OperatingSystem;

import java.nio.file.FileStore;
import java.nio.file.FileSystems;

public class GetUsage implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (command.getName().equalsIgnoreCase("mstats")) {

            SystemInfo systemInfo = new SystemInfo();
            OperatingSystem os = systemInfo.getOperatingSystem();
            CentralProcessor processor = systemInfo.getHardware().getProcessor();
            GlobalMemory memory = systemInfo.getHardware().getMemory();

            String cpuName = processor.getProcessorIdentifier().getName();
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
                sender.sendMessage("§cFailed to get disk usage.");
                e.printStackTrace();
            }

            totalDiskSpace /= (1024 * 1024 * 1024);
            freeDiskSpace /= (1024 * 1024 * 1024);
            usedDiskSpace /= (1024 * 1024 * 1024);

            int onlinePlayers = Bukkit.getOnlinePlayers().size();
            int maxPlayers = Bukkit.getMaxPlayers();

            String message = String.format(
                    "§e===== Server Resource Usage =====\n" +
                    "§aCPU Name: §f%s\n" +
                    "§aCPU Load: §f%.2f%%\n" +
                    "§aMemory Usage: §f%.2f GB / %.2f GB (%.2f%%)\n" +
                    "§aDisk Usage: §f%d GB / %d GB (%.2f%%)\n" +
                    "§aPlayers: §f%d / %d",
                    cpuName, cpuLoad,
                    usedMemoryGB, totalMemoryGB, memoryUsagePercent,
                    usedDiskSpace, totalDiskSpace, diskUsagePercent,
                    onlinePlayers, maxPlayers
            );

            if (sender instanceof Player) {
                ((Player) sender).sendMessage(message);
            } else {
                sender.sendMessage(message);
            }

            return true;
        }

        return false;
    }
}
