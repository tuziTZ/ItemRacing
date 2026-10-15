package top.coast23.ItemRacing.managers;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import top.coast23.ItemRacing.ItemRacing;
import top.coast23.ItemRacing.models.ItemDefinition;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * 加载 items.yml 中的任务物品目录，并委托 ItemSelectionPolicy 完成抽取。
 */
public class ItemManager {

    private static final String ITEMS_FILE_NAME = "items.yml";

    private final ItemRacing plugin;
    private final RandomGenerator random;
    private final Map<Material, ItemDefinition> definitions = new EnumMap<>(Material.class);
    private final List<Material> selectableItemPool = new ArrayList<>();
    private final Set<Material> mandatoryExclusions = EnumSet.noneOf(Material.class);
    private final Set<Material> defaultPlayerBans = EnumSet.noneOf(Material.class);
    private ItemSelectionPolicy selectionPolicy;

    public ItemManager(ItemRacing plugin) {
        // RandomGenerator.getDefault() 依赖服务提供者，在 Paper 隔离插件类加载器中可能不可见。
        this(plugin, new Random());
    }

    ItemManager(ItemRacing plugin, RandomGenerator random) {
        this.plugin = plugin;
        this.random = random;
        this.selectionPolicy = new ItemSelectionPolicy(random, 1.0, 4.5, 1.35);
    }

    /**
     * 从 items.yml 建立完整物品定义。仅技术上不可作为任务的物品从候选池中移除。
     */
    public void loadSurvivalItemPool() {
        if (plugin == null) {
            throw new IllegalStateException("测试用 ItemManager 不能加载插件资源");
        }

        File itemsFile = new File(plugin.getDataFolder(), ITEMS_FILE_NAME);
        if (!itemsFile.exists()) {
            plugin.saveResource(ITEMS_FILE_NAME, false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(itemsFile);
        List<String> mandatoryRules = normalizeRules(config.getStringList("mandatory-exclusions"));
        List<String> defaultBanRules = normalizeRules(config.getStringList("default-player-bans"));
        int defaultDifficulty = clampDifficulty(config.getInt("difficulty.default-level", 2));

        Map<Integer, List<String>> difficultyRules = new java.util.HashMap<>();
        for (int level = 1; level <= 5; level++) {
            difficultyRules.put(level,
                    normalizeRules(config.getStringList("difficulty.levels." + level)));
        }

        double minimum = config.getDouble("difficulty.curve.minimum", 1.0);
        double maximum = config.getDouble("difficulty.curve.maximum", 4.5);
        double exponent = config.getDouble("difficulty.curve.exponent", 1.35);
        this.selectionPolicy = new ItemSelectionPolicy(random, minimum, maximum, exponent);

        definitions.clear();
        selectableItemPool.clear();
        mandatoryExclusions.clear();
        defaultPlayerBans.clear();

        for (Material material : Material.values()) {
            boolean mandatory = material.isLegacy() || !material.isItem()
                    || matchesAny(material, mandatoryRules);
            boolean defaultBanned = !mandatory && matchesAny(material, defaultBanRules);
            int difficulty = resolveDifficulty(material, defaultDifficulty, difficultyRules);

            ItemDefinition definition = new ItemDefinition(material, difficulty, mandatory, defaultBanned);
            definitions.put(material, definition);
            if (mandatory) {
                mandatoryExclusions.add(material);
            } else {
                selectableItemPool.add(material);
                if (defaultBanned) defaultPlayerBans.add(material);
            }
        }

        validateRules("mandatory-exclusions", mandatoryRules);
        validateRules("default-player-bans", defaultBanRules);
        for (int level = 1; level <= 5; level++) {
            validateRules("difficulty.levels." + level, difficultyRules.get(level));
        }

        plugin.getLogger().info("任务物品目录加载成功：候选 " + selectableItemPool.size()
                + " 个，强制排除 " + mandatoryExclusions.size()
                + " 个，默认玩家封禁 " + defaultPlayerBans.size() + " 个。");
    }

    public List<Integer> createDifficultySequence(int totalCount, int startOrdinal,
                                                   int amount, ItemSelectionPolicy.Mode mode) {
        return selectionPolicy.createDifficultySequence(totalCount, startOrdinal, amount, mode);
    }

    public List<Material> generateRequiredItems(List<Integer> difficultySequence,
                                                Set<Material> excluded,
                                                ItemSelectionPolicy.Mode mode) {
        return selectionPolicy.select(selectableItemPool, excluded, difficultySequence,
                this::getDifficulty, mode);
    }

    /**
     * 兼容旧调用：使用默认封禁和渐进难度生成一整份列表。
     */
    public List<Material> generateRequiredItems(int amount) {
        List<Integer> sequence = createDifficultySequence(amount, 1, amount,
                ItemSelectionPolicy.Mode.PROGRESSIVE);
        return generateRequiredItems(sequence, defaultPlayerBans,
                ItemSelectionPolicy.Mode.PROGRESSIVE);
    }

    public int getDifficulty(Material material) {
        ItemDefinition definition = definitions.get(material);
        return definition == null ? 2 : definition.difficulty();
    }

    public boolean isSelectable(Material material) {
        ItemDefinition definition = definitions.get(material);
        return definition != null && !definition.mandatoryExcluded();
    }

    public int getAvailableItemCount(Collection<Material> playerBans) {
        if (playerBans == null || playerBans.isEmpty()) return selectableItemPool.size();
        Set<Material> bans = new LinkedHashSet<>(playerBans);
        return (int) selectableItemPool.stream().filter(item -> !bans.contains(item)).count();
    }

    public int getTotalItemAmount() {
        return selectableItemPool.size();
    }

    public List<Material> getSelectableItems() {
        return Collections.unmodifiableList(selectableItemPool);
    }

    public Set<Material> getMandatoryExclusions() {
        return Collections.unmodifiableSet(mandatoryExclusions);
    }

    public Set<Material> getDefaultPlayerBans() {
        return Collections.unmodifiableSet(defaultPlayerBans);
    }

    public Map<Material, ItemDefinition> getDefinitions() {
        return Collections.unmodifiableMap(definitions);
    }

    private int resolveDifficulty(Material material, int defaultDifficulty,
                                  Map<Integer, List<String>> rulesByLevel) {
        // 较高等级优先，避免 NETHERITE_BLOCK 同时被一般方块规则降级。
        for (int level = 5; level >= 1; level--) {
            if (matchesAny(material, rulesByLevel.getOrDefault(level, List.of()))) {
                return level;
            }
        }
        return defaultDifficulty;
    }

    private List<String> normalizeRules(List<String> rules) {
        return rules.stream()
                .filter(rule -> rule != null && !rule.isBlank())
                .map(rule -> rule.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    static boolean matchesRule(Material material, String rule) {
        String value = material.name();
        StringBuilder regex = new StringBuilder("^");
        for (char character : rule.toUpperCase(Locale.ROOT).toCharArray()) {
            switch (character) {
                case '*' -> regex.append(".*");
                case '?' -> regex.append('.');
                case '.', '\\', '+', '(', ')', '[', ']', '{', '}', '^', '$', '|' ->
                        regex.append('\\').append(character);
                default -> regex.append(character);
            }
        }
        return value.matches(regex.append('$').toString());
    }

    private boolean matchesAny(Material material, List<String> rules) {
        return rules.stream().anyMatch(rule -> matchesRule(material, rule));
    }

    private void validateRules(String path, List<String> rules) {
        for (String rule : rules) {
            boolean matched = java.util.Arrays.stream(Material.values())
                    .anyMatch(material -> matchesRule(material, rule));
            if (!matched) {
                plugin.getLogger().warning("items.yml 的 " + path + " 包含无效规则：" + rule);
            }
        }
    }

    private static int clampDifficulty(int difficulty) {
        return Math.max(1, Math.min(difficulty, 5));
    }
}
