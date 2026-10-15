package top.coast23.ItemRacing.managers;

import org.bukkit.Material;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * 管理一局游戏在大厅阶段可修改的设置。
 */
public class GameSettingsManager {

    public enum GameMode {
        CLASSIC,
        RACING
    }

    public enum ToolType {
        NONE,
        STONE,
        IRON
    }

    public enum ElytraType {
        NONE,
        BROKEN,
        NORMAL
    }

    public enum FoodType {
        NONE,
        GOLDEN_CARROT,
        SATURATION
    }

    public enum ToolDropEnchantment {
        NONE("无", 0),
        SILK_TOUCH("精准采集", 1),
        FORTUNE_I("时运 I", 1),
        FORTUNE_II("时运 II", 2),
        FORTUNE_III("时运 III", 3);

        private final String displayName;
        private final int level;

        ToolDropEnchantment(String displayName, int level) {
            this.displayName = displayName;
            this.level = level;
        }

        public String displayName() { return displayName; }
        public int level() { return level; }

        public ToolDropEnchantment next() {
            ToolDropEnchantment[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    public enum BanUpdateResult {
        BANNED,
        UNBANNED,
        NOT_SELECTABLE,
        WOULD_LEAVE_TOO_FEW_ITEMS
    }

    private final ConfigManager configManager;
    private final ItemManager itemManager;

    private int itemsToCollect;
    private int teamChestAmount;
    private int teamWaypointAmount;
    private int maxRollsPerTeam;
    private int enabledTeamCount;

    private int speedLevel;
    private int hasteLevel;
    private int resistLevel;

    private GameMode gameMode;
    private ToolType toolType;
    private ElytraType elytraType;
    private FoodType foodType;

    private int toolEfficiencyLevel;
    private int toolUnbreakingLevel;
    private boolean toolMending;
    private ToolDropEnchantment toolDropEnchantment;
    private ItemSelectionPolicy.Mode itemSelectionMode;
    private final Set<Material> bannedItems = EnumSet.noneOf(Material.class);

    public GameSettingsManager(ConfigManager configManager, ItemManager itemManager) {
        this.configManager = configManager;
        this.itemManager = itemManager;
        resetToDefaults();
    }

    public void resetToDefaults() {
        bannedItems.clear();
        bannedItems.addAll(itemManager.getDefaultPlayerBans());

        int availableItems = Math.max(1, itemManager.getAvailableItemCount(bannedItems));
        this.itemsToCollect = Math.min(Math.max(1, configManager.getTotalItemsToCollect()), availableItems);
        this.teamChestAmount = configManager.getTeamChestAmount();
        this.teamWaypointAmount = configManager.getTeamWaypointAmount();
        this.maxRollsPerTeam = configManager.getMaxRollsPerTeam();
        this.enabledTeamCount = configManager.getEnabledTeamCount();

        this.speedLevel = 2;
        this.hasteLevel = 2;
        this.resistLevel = 2;
        this.gameMode = GameMode.CLASSIC;
        this.elytraType = ElytraType.NORMAL;
        this.toolType = ToolType.IRON;
        this.foodType = FoodType.GOLDEN_CARROT;

        // 原本默认发放经验修补、精准采集附魔书；现在直接附到初始工具上。
        this.toolEfficiencyLevel = 0;
        this.toolUnbreakingLevel = 0;
        this.toolMending = true;
        this.toolDropEnchantment = ToolDropEnchantment.SILK_TOUCH;
        this.itemSelectionMode = ItemSelectionPolicy.Mode.PROGRESSIVE;
    }

    public int getItemsToCollect() { return itemsToCollect; }

    public void setItemsToCollect(int itemsToCollect) {
        int totalItems = Math.max(1, itemManager.getAvailableItemCount(bannedItems));
        this.itemsToCollect = Math.min(Math.max(1, itemsToCollect), totalItems);
    }

    public GameMode getGameMode() { return gameMode; }
    public void toggleGameMode() {
        this.gameMode = (this.gameMode == GameMode.CLASSIC) ? GameMode.RACING : GameMode.CLASSIC;
    }

    public int getSpeedLevel() { return speedLevel; }
    public void setSpeedLevel(int level) { this.speedLevel = Math.min(Math.max(0, level), 5); }
    public int getHasteLevel() { return hasteLevel; }
    public void setHasteLevel(int level) { this.hasteLevel = Math.min(Math.max(0, level), 5); }
    public int getResistLevel() { return resistLevel; }
    public void setResistLevel(int level) { this.resistLevel = Math.min(Math.max(0, level), 5); }

    public ToolType getToolType() { return toolType; }
    public void cycleToolType() {
        ToolType[] types = ToolType.values();
        this.toolType = types[(this.toolType.ordinal() + 1) % types.length];
    }

    public ElytraType getElytraType() { return elytraType; }
    public void cycleElytraType() {
        ElytraType[] types = ElytraType.values();
        this.elytraType = types[(this.elytraType.ordinal() + 1) % types.length];
    }

    public FoodType getFoodType() { return foodType; }
    public void cycleFoodType() {
        FoodType[] types = FoodType.values();
        this.foodType = types[(this.foodType.ordinal() + 1) % types.length];
    }

    public int getTeamChestAmount() { return teamChestAmount; }
    public int getMaxRollsPerTeam() { return maxRollsPerTeam; }
    public int getEnabledTeamCount() { return enabledTeamCount; }
    public void setEnabledTeamCount(int enabledTeamCount) {
        this.enabledTeamCount = Math.max(1, enabledTeamCount);
    }
    public int getTeamWaypointAmount() { return teamWaypointAmount; }

    public int getToolEfficiencyLevel() { return toolEfficiencyLevel; }
    public void setToolEfficiencyLevel(int level) {
        this.toolEfficiencyLevel = Math.max(0, Math.min(level, 5));
    }

    public int getToolUnbreakingLevel() { return toolUnbreakingLevel; }
    public void setToolUnbreakingLevel(int level) {
        this.toolUnbreakingLevel = Math.max(0, Math.min(level, 3));
    }

    public boolean isToolMending() { return toolMending; }
    public void toggleToolMending() { this.toolMending = !this.toolMending; }

    public ToolDropEnchantment getToolDropEnchantment() { return toolDropEnchantment; }
    public void cycleToolDropEnchantment() {
        this.toolDropEnchantment = this.toolDropEnchantment.next();
    }

    public ItemSelectionPolicy.Mode getItemSelectionMode() { return itemSelectionMode; }
    public void cycleItemSelectionMode() { this.itemSelectionMode = this.itemSelectionMode.next(); }

    public Set<Material> getBannedItems() {
        if (bannedItems.isEmpty()) return Collections.emptySet();
        return Collections.unmodifiableSet(EnumSet.copyOf(bannedItems));
    }

    public boolean isItemBanned(Material material) {
        return bannedItems.contains(material);
    }

    public BanUpdateResult toggleBannedItem(Material material) {
        if (!itemManager.isSelectable(material)) return BanUpdateResult.NOT_SELECTABLE;
        if (bannedItems.remove(material)) return BanUpdateResult.UNBANNED;

        int availableAfterBan = itemManager.getAvailableItemCount(bannedItems) - 1;
        if (availableAfterBan < itemsToCollect) {
            return BanUpdateResult.WOULD_LEAVE_TOO_FEW_ITEMS;
        }
        bannedItems.add(material);
        return BanUpdateResult.BANNED;
    }

    public void resetBannedItems() {
        bannedItems.clear();
        bannedItems.addAll(itemManager.getDefaultPlayerBans());
        setItemsToCollect(itemsToCollect);
    }

    public int getAvailableItemCount() {
        return itemManager.getAvailableItemCount(bannedItems);
    }
}
