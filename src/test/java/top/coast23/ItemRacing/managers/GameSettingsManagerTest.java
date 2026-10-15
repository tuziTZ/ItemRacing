package top.coast23.ItemRacing.managers;

import org.bukkit.Material;
import org.junit.Test;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GameSettingsManagerTest {

    @Test
    public void teamCountCanChangeAndResetToConfiguredDefault() {
        GameSettingsManager settings = createSettings();

        assertEquals(4, settings.getEnabledTeamCount());
        settings.setEnabledTeamCount(2);
        assertEquals(2, settings.getEnabledTeamCount());
        settings.setEnabledTeamCount(0);
        assertEquals(1, settings.getEnabledTeamCount());

        settings.resetToDefaults();
        assertEquals(4, settings.getEnabledTeamCount());
    }

    @Test
    public void toolEnchantmentsAreAppliedAsSettingsInsteadOfBookFlags() {
        GameSettingsManager settings = createSettings();

        assertTrue(settings.isToolMending());
        assertEquals(GameSettingsManager.ToolDropEnchantment.SILK_TOUCH,
                settings.getToolDropEnchantment());
        assertEquals(0, settings.getToolEfficiencyLevel());
        assertEquals(0, settings.getToolUnbreakingLevel());

        settings.setToolEfficiencyLevel(99);
        settings.setToolUnbreakingLevel(99);
        assertEquals(5, settings.getToolEfficiencyLevel());
        assertEquals(3, settings.getToolUnbreakingLevel());
    }

    @Test
    public void playerBansCannotMakePoolSmallerThanTaskCount() {
        GameSettingsManager settings = createSettings();
        settings.setItemsToCollect(98);

        assertEquals(GameSettingsManager.BanUpdateResult.WOULD_LEAVE_TOO_FEW_ITEMS,
                settings.toggleBannedItem(Material.DIAMOND));
        assertFalse(settings.isItemBanned(Material.DIAMOND));

        settings.setItemsToCollect(20);
        assertEquals(GameSettingsManager.BanUpdateResult.BANNED,
                settings.toggleBannedItem(Material.DIAMOND));
        assertTrue(settings.isItemBanned(Material.DIAMOND));
        assertEquals(GameSettingsManager.BanUpdateResult.UNBANNED,
                settings.toggleBannedItem(Material.DIAMOND));
    }

    @Test
    public void resetRestoresDefaultPlayerBansAndProgressiveMode() {
        GameSettingsManager settings = createSettings();
        assertTrue(settings.isItemBanned(Material.TRIDENT));
        settings.toggleBannedItem(Material.TRIDENT);
        settings.cycleItemSelectionMode();

        settings.resetToDefaults();

        assertTrue(settings.isItemBanned(Material.TRIDENT));
        assertEquals(ItemSelectionPolicy.Mode.PROGRESSIVE, settings.getItemSelectionMode());
    }

    private static GameSettingsManager createSettings() {
        return new GameSettingsManager(new TestConfigManager(), new TestItemManager());
    }

    private static class TestItemManager extends ItemManager {
        private final Set<Material> defaults = EnumSet.of(Material.TRIDENT, Material.ELYTRA);

        TestItemManager() { super(null); }

        @Override public Set<Material> getDefaultPlayerBans() { return defaults; }
        @Override public boolean isSelectable(Material material) { return material != Material.BARRIER; }
        @Override public int getAvailableItemCount(Collection<Material> playerBans) {
            return 100 - (int) playerBans.stream().filter(this::isSelectable).distinct().count();
        }
    }

    private static class TestConfigManager extends ConfigManager {
        TestConfigManager() { super(null); }

        @Override public int getTotalItemsToCollect() { return 20; }
        @Override public int getTeamChestAmount() { return 40; }
        @Override public int getTeamWaypointAmount() { return 40; }
        @Override public int getMaxRollsPerTeam() { return 5; }
        @Override public int getEnabledTeamCount() { return 4; }
    }
}
