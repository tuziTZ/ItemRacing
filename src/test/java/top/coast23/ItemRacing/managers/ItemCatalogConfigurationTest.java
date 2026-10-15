package top.coast23.ItemRacing.managers;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ItemCatalogConfigurationTest {

    @Test
    public void bundledItemsConfigurationContainsRequiredSections() throws Exception {
        File file = resourceFile();
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);

        assertFalse(configuration.getStringList("mandatory-exclusions").isEmpty());
        assertFalse(configuration.getStringList("default-player-bans").isEmpty());
        for (int level = 1; level <= 5; level++) {
            assertFalse("难度 " + level + " 缺少物品规则",
                    configuration.getStringList("difficulty.levels." + level).isEmpty());
        }
        assertTrue(configuration.getStringList("mandatory-exclusions").contains("COMMAND_BLOCK"));
        assertTrue(configuration.getStringList("default-player-bans").contains("TRIDENT"));
    }

    @Test
    public void wildcardRulesMatchMaterialNames() {
        assertTrue(ItemManager.matchesRule(Material.ZOMBIE_SPAWN_EGG, "*_SPAWN_EGG"));
        assertTrue(ItemManager.matchesRule(Material.MUSIC_DISC_13, "MUSIC_DISC_*"));
        assertFalse(ItemManager.matchesRule(Material.EGG, "*_SPAWN_EGG"));
    }

    @Test
    public void everyBundledRuleMatchesAtLeastOneMaterial() throws Exception {
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(resourceFile());
        List<String> paths = List.of(
                "mandatory-exclusions",
                "default-player-bans",
                "difficulty.levels.1",
                "difficulty.levels.2",
                "difficulty.levels.3",
                "difficulty.levels.4",
                "difficulty.levels.5");

        for (String path : paths) {
            for (String rule : configuration.getStringList(path)) {
                assertTrue(path + " 包含不匹配任何 1.21.6 Material 的规则: " + rule,
                        Arrays.stream(Material.values())
                                .anyMatch(material -> ItemManager.matchesRule(material, rule)));
            }
        }
    }

    private File resourceFile() throws URISyntaxException {
        var resource = getClass().getClassLoader().getResource("items.yml");
        assertNotNull(resource);
        return new File(resource.toURI());
    }
}
