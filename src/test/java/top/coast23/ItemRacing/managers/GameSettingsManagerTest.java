package top.coast23.ItemRacing.managers;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GameSettingsManagerTest {
    @Test
    public void teamCountCanChangeAndResetToConfiguredDefault() {
        GameSettingsManager settings = new GameSettingsManager(new TestConfigManager());

        assertEquals(4, settings.getEnabledTeamCount());
        settings.setEnabledTeamCount(2);
        assertEquals(2, settings.getEnabledTeamCount());
        settings.setEnabledTeamCount(0);
        assertEquals(1, settings.getEnabledTeamCount());

        settings.resetToDefaults();
        assertEquals(4, settings.getEnabledTeamCount());
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
