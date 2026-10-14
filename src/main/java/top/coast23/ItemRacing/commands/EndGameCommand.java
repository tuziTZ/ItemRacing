package top.coast23.ItemRacing.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.coast23.ItemRacing.managers.GameManager;

import java.util.List;

/** 管理员安全中止当前对局，不声明胜者。 */
public class EndGameCommand implements CommandExecutor, TabCompleter {
    private static final String PERMISSION = "itemracing.admin.end";
    private final GameManager gameManager;

    public EndGameCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Component.text("你没有权限结束游戏。", NamedTextColor.RED));
            return true;
        }
        if (gameManager.getCurrentState() != GameManager.GameState.ACTIVE) {
            sender.sendMessage(Component.text("当前没有正在进行的游戏。", NamedTextColor.RED));
            return true;
        }
        if (args.length != 1 || !args[0].equalsIgnoreCase("confirm")) {
            sender.sendMessage(Component.text("请输入 /" + label + " confirm 确认中止本局游戏。", NamedTextColor.YELLOW));
            return true;
        }

        Bukkit.broadcast(Component.text(sender.getName() + " 手动中止了本局游戏。", NamedTextColor.YELLOW));
        gameManager.endGame(null);
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        return args.length == 1 && "confirm".startsWith(args[0].toLowerCase())
                ? List.of("confirm") : List.of();
    }
}
