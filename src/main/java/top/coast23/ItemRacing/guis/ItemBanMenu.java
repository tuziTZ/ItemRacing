package top.coast23.ItemRacing.guis;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import top.coast23.ItemRacing.ItemRacing;
import top.coast23.ItemRacing.managers.GameManager;
import top.coast23.ItemRacing.managers.GameSettingsManager;
import top.coast23.ItemRacing.managers.ItemManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 大厅内可分页、按难度筛选的玩家黑名单菜单。 */
public class ItemBanMenu {

    public static final Component MENU_TITLE = Component.text("物品封禁", NamedTextColor.DARK_RED);
    private static final int PAGE_SIZE = 45;
    private static final Map<UUID, ViewState> VIEW_STATES = new HashMap<>();

    private final GameManager gameManager;
    private final GameSettingsManager settings;
    private final ItemManager itemManager;
    private final NamespacedKey materialKey;
    private final NamespacedKey actionKey;

    public ItemBanMenu(GameManager gameManager) {
        this.gameManager = gameManager;
        this.settings = gameManager.getGameSettings();
        this.itemManager = gameManager.getItemManager();
        this.materialKey = new NamespacedKey(ItemRacing.getInstance(), "ban_material");
        this.actionKey = new NamespacedKey(ItemRacing.getInstance(), "ban_action");
    }

    public void open(Player player) {
        ViewState state = VIEW_STATES.computeIfAbsent(player.getUniqueId(), ignored -> new ViewState());
        open(player, state);
    }

    private void open(Player player, ViewState state) {
        List<Material> visibleItems = itemManager.getSelectableItems().stream()
                .filter(material -> state.difficulty == 0 || itemManager.getDifficulty(material) == state.difficulty)
                .sorted(Comparator.comparingInt(itemManager::getDifficulty).reversed()
                        .thenComparing(Material::name))
                .toList();

        int pageCount = Math.max(1, (visibleItems.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        state.page = Math.max(0, Math.min(state.page, pageCount - 1));
        int start = state.page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, visibleItems.size());

        Inventory menu = Bukkit.createInventory(null, 54, MENU_TITLE);
        for (int index = start; index < end; index++) {
            Material material = visibleItems.get(index);
            menu.setItem(index - start, createMaterialButton(material));
        }

        if (state.page > 0) menu.setItem(45, createActionButton(Material.ARROW, "上一页", "previous"));
        menu.setItem(47, createActionButton(Material.HOPPER,
                state.difficulty == 0 ? "难度筛选: 全部" : "难度筛选: " + state.difficulty,
                "filter"));
        menu.setItem(48, createActionButton(Material.OAK_DOOR, "返回游戏设置", "back"));
        menu.setItem(49, createInfoButton(state.page + 1, pageCount));
        menu.setItem(50, createActionButton(Material.LAVA_BUCKET, "恢复默认封禁", "reset"));
        if (state.page + 1 < pageCount) menu.setItem(53, createActionButton(Material.ARROW, "下一页", "next"));
        player.openInventory(menu);
    }

    public void handleClick(Player player, ItemStack clickedItem, ClickType clickType) {
        if (gameManager.getCurrentState() != GameManager.GameState.LOBBY) {
            player.closeInventory();
            player.sendMessage(Component.text("游戏开始后不能修改物品封禁。", NamedTextColor.RED));
            return;
        }
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        String materialName = meta.getPersistentDataContainer().get(materialKey, PersistentDataType.STRING);
        if (materialName != null) {
            Material material = Material.matchMaterial(materialName);
            if (material == null) return;
            GameSettingsManager.BanUpdateResult result = settings.toggleBannedItem(material);
            switch (result) {
                case BANNED, UNBANNED -> gameManager.invalidateLobbyReadyStates();
                case WOULD_LEAVE_TOO_FEW_ITEMS -> player.sendMessage(Component.text(
                        "不能继续封禁：可用物品数量必须不少于目标物品数量。", NamedTextColor.RED));
                case NOT_SELECTABLE -> player.sendMessage(Component.text("该物品不能由玩家修改。", NamedTextColor.RED));
            }
            open(player);
            return;
        }

        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action == null) return;
        ViewState state = VIEW_STATES.computeIfAbsent(player.getUniqueId(), ignored -> new ViewState());
        switch (action) {
            case "previous" -> state.page--;
            case "next" -> state.page++;
            case "filter" -> {
                int delta = clickType.isRightClick() ? -1 : 1;
                state.difficulty = Math.floorMod(state.difficulty + delta, 6);
                state.page = 0;
            }
            case "reset" -> {
                settings.resetBannedItems();
                gameManager.invalidateLobbyReadyStates();
                player.sendMessage(Component.text("已恢复 items.yml 中的默认玩家封禁。", NamedTextColor.GREEN));
            }
            case "back" -> {
                new GameSettingsMenu(settings).open(player);
                return;
            }
            default -> { return; }
        }
        open(player, state);
    }

    private ItemStack createMaterialButton(Material material) {
        boolean banned = settings.isItemBanned(material);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Component.translatable(material)
                .color(banned ? NamedTextColor.RED : NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("难度: " + itemManager.getDifficulty(material) + "/5", NamedTextColor.GOLD));
        lore.add(Component.text(banned ? "当前: 已封禁" : "当前: 可抽取",
                banned ? NamedTextColor.RED : NamedTextColor.GREEN));
        lore.add(Component.empty());
        lore.add(Component.text("» 点击切换", NamedTextColor.YELLOW));
        meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        meta.getPersistentDataContainer().set(materialKey, PersistentDataType.STRING, material.name());
        markGuiItem(meta);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInfoButton(int page, int pageCount) {
        ItemStack item = createActionButton(Material.BOOK,
                "第 " + page + " / " + pageCount + " 页", "info");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.lore(List.of(
                    Component.text("已封禁: " + settings.getBannedItems().size(), NamedTextColor.RED),
                    Component.text("可抽取: " + settings.getAvailableItemCount(), NamedTextColor.GREEN),
                    Component.text("目标数量: " + settings.getItemsToCollect(), NamedTextColor.AQUA)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createActionButton(Material icon, String name, String action) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
            markGuiItem(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void markGuiItem(ItemMeta meta) {
        meta.getPersistentDataContainer().set(ItemRacing.GUI_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
    }

    private static final class ViewState {
        private int page;
        private int difficulty;
    }
}
