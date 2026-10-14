package top.coast23.ItemRacing.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.coast23.ItemRacing.managers.GameManager;

import java.util.List;

/** 无需重启服务器即可清理上一局数据并返回大厅。 */
public class ResetGameCommand implements CommandExecutor, TabCompleter {
    private static final String PERMISSION = "itemracing.admin.reset";
    private final GameManager gameManager;

    public ResetGameCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Component.text("你没有权限重置游戏。", NamedTextColor.RED));
            return true;
        }
        if (gameManager.getCurrentState() != GameManager.GameState.ENDED) {
            sender.sendMessage(Component.text("只能在游戏结束后重置到大厅。", NamedTextColor.RED));
            return true;
        }
        if (args.length != 1 || !args[0].equalsIgnoreCase("confirm")) {
            sender.sendMessage(Component.text("请输入 /" + label + " confirm 确认重置本局数据。", NamedTextColor.YELLOW));
            return true;
        }

        gameManager.resetGame();
        sender.sendMessage(Component.text("游戏已重置并返回大厅。", NamedTextColor.GREEN));
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
