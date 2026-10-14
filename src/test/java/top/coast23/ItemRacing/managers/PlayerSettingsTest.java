package top.coast23.ItemRacing.managers;

import org.bukkit.entity.Player;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class PlayerSettingsTest {
    @Test
    public void allThreeScoreboardModesCanBeSelected() {
        PlayerSettings settings = new PlayerSettings();
        Player player = fakePlayer(UUID.randomUUID());

        assertEquals(PlayerSettings.ScoreboardStyle.RANKING, settings.getSettingsFor(player).style());

        settings.setWatchTeamStyle(player, "blue");
        assertEquals(PlayerSettings.ScoreboardStyle.WATCH_TEAM, settings.getSettingsFor(player).style());
        assertEquals("blue", settings.getSettingsFor(player).watchedTeamId());

        settings.setRotatingStyle(player, 7);
        assertEquals(PlayerSettings.ScoreboardStyle.ROTATING, settings.getSettingsFor(player).style());
        assertEquals(7, settings.getSettingsFor(player).rotationSeconds());

        settings.setRankingStyle(player);
        assertEquals(PlayerSettings.ScoreboardStyle.RANKING, settings.getSettingsFor(player).style());
    }

    @Test
    public void rotatingSecondsAreClampedToSafeRange() {
        PlayerSettings settings = new PlayerSettings();
        Player player = fakePlayer(UUID.randomUUID());

        settings.setRotatingStyle(player, 0);
        assertEquals(1, settings.getSettingsFor(player).rotationSeconds());

        settings.setRotatingStyle(player, 1000);
        assertEquals(60, settings.getSettingsFor(player).rotationSeconds());
    }

    private Player fakePlayer(UUID uuid) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) return uuid;
                    if (method.getName().equals("hashCode")) return uuid.hashCode();
                    if (method.getName().equals("equals")) return proxy == args[0];
                    if (method.getReturnType().equals(boolean.class)) return false;
                    if (method.getReturnType().equals(int.class)) return 0;
                    if (method.getReturnType().equals(long.class)) return 0L;
                    return null;
                });
    }
}
