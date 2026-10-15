package top.coast23.ItemRacing.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import top.coast23.ItemRacing.managers.GameManager;
import top.coast23.ItemRacing.models.Team;
import top.coast23.ItemRacing.ItemRacing;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import top.coast23.ItemRacing.managers.GameSettingsManager;
import top.coast23.ItemRacing.managers.ScoreboardManager;
import top.coast23.ItemRacing.managers.BroadcastManager;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [完全重写] 统一处理所有与聊天相关的逻辑。
 * 使用现代的 AsyncChatEvent，并处理游戏设置的数值输入。
 */
public class ChatListener implements Listener {

    private final GameManager gameManager;
    private final GameSettingsManager gameSettings;
    private final ScoreboardManager scoreboardManager;
    private final BroadcastManager broadcastManager;
    private static final Set<UUID> playersAwaitingInput = ConcurrentHashMap.newKeySet();

    public ChatListener(GameManager gameManager, GameSettingsManager gameSettings, ScoreboardManager scoreboardManager, BroadcastManager broadcastManager) {
        this.gameManager = gameManager;
        this.gameSettings = gameSettings;
        this.scoreboardManager = scoreboardManager;
        this.broadcastManager = broadcastManager;
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (playersAwaitingInput.remove(playerId)) {
            event.setCancelled(true);
            String message = PlainTextComponentSerializer.plainText().serialize(event.originalMessage());
            Bukkit.getScheduler().runTask(ItemRacing.getInstance(), () -> applyItemAmountInput(player, message));
            return;
        }

        // 如果玩家不是在设置数值，则进行聊天格式化
        Optional<Team> playerTeamOpt = gameManager.getTeamManager().getPlayerTeam(player);

        if (playerTeamOpt.isPresent()) {
            Team team = playerTeamOpt.get();

            // 定义一个新的渲染器来覆盖默认的聊天格式
            event.renderer((source, sourceDisplayName, message, viewer) ->
                    Component.text()
                            // [改造] 前缀和玩家名都使用队伍颜色
                            .append(Component.text("[", team.getColor()))
                            .append(Component.text(team.getDisplayName(), team.getColor()))
                            .append(Component.text("] ", team.getColor()))
                            .append(sourceDisplayName.color(team.getColor()))
                            .append(Component.text(": ", NamedTextColor.GRAY))
                            .append(message.color(NamedTextColor.WHITE))
                            .build()
            );
        }
        // 如果玩家不在任何队伍中，则不调用 event.renderer()，使用服务器的默认聊天格式
        /* TODO */
    }

    public static void requestItemAmountInput(Player player) {
        playersAwaitingInput.add(player.getUniqueId());
        player.sendMessage(Component.text("---------------------------------", NamedTextColor.GOLD));
        player.sendMessage(Component.text("请在聊天框中输入新的物品数量。", NamedTextColor.AQUA));
        player.sendMessage(Component.text("输入 cancel 或 取消 可退出设置。", NamedTextColor.GRAY));
        player.sendMessage(Component.text("---------------------------------", NamedTextColor.GOLD));
    }

    private void applyItemAmountInput(Player player, String message) {
        if (!player.isOnline()) return;
        if (gameManager.getCurrentState() != GameManager.GameState.LOBBY) {
            player.sendMessage(Component.text("游戏已经开始，本次设置输入已取消。", NamedTextColor.RED));
            return;
        }
        if (message.equalsIgnoreCase("cancel") || message.equals("取消")) {
            player.sendMessage(Component.text("已取消修改目标物品数量。", NamedTextColor.YELLOW));
            return;
        }

        try {
            int previous = gameSettings.getItemsToCollect();
            gameSettings.setItemsToCollect(Integer.parseInt(message));
            int actual = gameSettings.getItemsToCollect();
            if (previous != actual) {
                gameManager.invalidateLobbyReadyStates();
                broadcastManager.settingChanged(player, "目标物品数量", String.valueOf(actual));
                scoreboardManager.forceUpdateAllScoreboards();
            }
            if (!String.valueOf(actual).equals(message.trim())) {
                player.sendMessage(Component.text("输入已按可用物品数量修正为 " + actual + "。", NamedTextColor.YELLOW));
            }
        } catch (NumberFormatException exception) {
            player.sendMessage(Component.text("输入非法，取消设置。", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playersAwaitingInput.remove(event.getPlayer().getUniqueId());
    }

    public static void cancelAllPendingInputs() {
        playersAwaitingInput.clear();
    }
}
