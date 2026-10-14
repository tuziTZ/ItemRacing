package top.coast23.ItemRacing.commands;

import top.coast23.ItemRacing.managers.PlayerSettings;
import top.coast23.ItemRacing.managers.GameManager;
import top.coast23.ItemRacing.managers.ScoreboardManager;
import top.coast23.ItemRacing.managers.TeamManager;
import top.coast23.ItemRacing.models.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ScoreboardCommand implements CommandExecutor, TabCompleter {
    private final PlayerSettings playerSettings;
    private final TeamManager teamManager;
    private final GameManager gameManager;
    private final ScoreboardManager scoreboardManager;

    public ScoreboardCommand(PlayerSettings playerSettings, TeamManager teamManager,
                             GameManager gameManager, ScoreboardManager scoreboardManager) {
        this.playerSettings = playerSettings;
        this.teamManager = teamManager;
        this.gameManager = gameManager;
        this.scoreboardManager = scoreboardManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("这个指令只能由玩家执行。");
            return true;
        }
        if (args.length == 0) { sendHelp(player); return true; }
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "ranking":
                playerSettings.setRankingStyle(player);
                player.sendMessage(Component.text("计分板样式已切换为 [排行榜模式]。", NamedTextColor.GREEN));
                break;
            case "watch":
                if (args.length != 2) {
                    player.sendMessage(Component.text("用法: /sb watch <队伍ID>", NamedTextColor.RED));
                    return true;
                }
                String teamId = args[1].toLowerCase();
                Team targetTeam = teamManager.getTeamById(teamId).orElse(null);
                if (targetTeam == null || !getAvailableTeams().contains(targetTeam)) {
                    player.sendMessage(Component.text("该队伍不存在或未参加本局游戏。", NamedTextColor.RED));
                    return true;
                }
                if (targetTeam.isMember(player)) {
                    player.sendMessage(Component.text("你的队伍任务已显示在计分板上方，请选择其他队伍。", NamedTextColor.RED));
                    return true;
                }
                playerSettings.setWatchTeamStyle(player, teamId);
                player.sendMessage(Component.text("计分板样式已切换为 [监视 " + targetTeam.getDisplayName() + " 模式]。", NamedTextColor.GREEN));
                break;
            case "rotate":
                int seconds = 5;
                if (args.length > 2) {
                    player.sendMessage(Component.text("用法: /sb rotate [1-60秒]", NamedTextColor.RED));
                    return true;
                }
                if (args.length == 2) {
                    try {
                        seconds = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        player.sendMessage(Component.text("轮播间隔必须是 1 到 60 之间的整数。", NamedTextColor.RED));
                        return true;
                    }
                }
                if (seconds < 1 || seconds > 60) {
                    player.sendMessage(Component.text("轮播间隔必须是 1 到 60 秒。", NamedTextColor.RED));
                    return true;
                }
                if (getAvailableTeams().stream().noneMatch(team -> !team.isMember(player))) {
                    player.sendMessage(Component.text("当前没有其他可轮播的参赛队伍。", NamedTextColor.RED));
                    return true;
                }
                playerSettings.setRotatingStyle(player, seconds);
                player.sendMessage(Component.text("计分板样式已切换为 [滚动显示模式] (" + seconds + "秒/队)。", NamedTextColor.GREEN));
                break;
            default: sendHelp(player); break;
        }
        scoreboardManager.forceUpdateScoreboard(player);
        return true;
    }

    @Nullable @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) { return List.of("ranking", "watch", "rotate").stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList()); }
        if (args.length == 2 && args[0].equalsIgnoreCase("watch")) {
            if (!(sender instanceof Player player)) return List.of();
            return getAvailableTeams().stream()
                    .filter(team -> !team.isMember(player))
                    .map(Team::getId)
                    .filter(id -> id.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("--- 计分板样式帮助 ---", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/sb ranking", NamedTextColor.AQUA).append(Component.text(" - 显示所有队伍排名。", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/sb watch <队伍ID>", NamedTextColor.AQUA).append(Component.text(" - 持续关注一个队伍。", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/sb rotate [秒数]", NamedTextColor.AQUA).append(Component.text(" - 滚动显示其他队伍。", NamedTextColor.GRAY)));
    }

    private List<Team> getAvailableTeams() {
        return gameManager.getCurrentState() == GameManager.GameState.ACTIVE
                ? gameManager.getParticipatingTeams() : gameManager.getEnabledTeams();
    }
}
