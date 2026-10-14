package top.coast23.ItemRacing.managers;

import top.coast23.ItemRacing.ItemRacing;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final ItemRacing plugin;
    private FileConfiguration config;

    public ConfigManager(ItemRacing plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public void loadTeams(TeamManager teamManager) {
        ConfigurationSection teamsSection = config.getConfigurationSection("teams");
        if (teamsSection == null) {
            plugin.getLogger().warning("在 config.yml 中找不到 'teams' 配置区域！将不会加载任何队伍。");
            return;
        }

        for (String teamId : teamsSection.getKeys(false)) {
            String displayName = teamsSection.getString(teamId + ".display-name", "Default Team Name");
            String hexColor = teamsSection.getString(teamId + ".color", "#FFFFFF");

            try {
                TextColor color = TextColor.fromHexString(hexColor);
                if (color == null) throw new IllegalArgumentException("无效的十六进制颜色码");

                teamManager.createTeam(teamId, displayName, color);
                plugin.getLogger().info("已加载队伍: " + displayName + " (ID: " + teamId + ")");

            } catch (Exception e) {
                plugin.getLogger().severe("加载队伍 '" + teamId + "' 时出错: " + e.getMessage());
            }
        }
    }

    // --- Getters for settings ---
    public int getTotalItemsToCollect() { return config.getInt("items-to-collect", 20); }
    public int getConfiguredTeamCount() {
        ConfigurationSection teamsSection = config.getConfigurationSection("teams");
        return teamsSection == null ? 0 : teamsSection.getKeys(false).size();
    }
    public int getEnabledTeamCount() {
        int configuredTeams = getConfiguredTeamCount();
        int requestedTeams = config.getInt("enabled-team-count", configuredTeams);
        return Math.max(1, Math.min(requestedTeams, Math.max(1, Math.min(configuredTeams, 54))));
    }
    public int getTeamChestAmount() { return Math.max(1, Math.min(config.getInt("team-chest-amount", 20), 54)); }
    public int getTeamWaypointAmount() { return Math.max(1, Math.min(config.getInt("team-waypoint-amount", 31), 54)); }
    // 侧边栏最多 15 行；队列大于 5 时会挤掉排行榜/监视区。
    public int getTaskQueueSize() { return Math.max(1, Math.min(config.getInt("task-queue-size", 5), 5)); }
    public int getInitialLocateUses() { return config.getInt("initial-locate-uses", 1); }
    public int getMaxRollsPerTeam() { return config.getInt("max-rolls-per-team", 5); }
    public int getLocateCommandCost() { return config.getInt("locate-cost", 3); }
    public int getRandomTeleportCost(){return config.getInt("tpr-cost", 2); }
    public int getInitialTprUses() { return config.getInt("initial-tpr-uses", 1); }
    public int getScorePerItem() {return config.getInt("score-per-item", 3); }
    public boolean isUnlockAllRecipes() { return config.getBoolean("unlock-all-recipes", true); }
}
