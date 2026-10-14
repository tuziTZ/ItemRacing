package top.coast23.ItemRacing.managers;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.junit.Test;
import top.coast23.ItemRacing.models.Team;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class MatchRosterTest {
    @Test
    public void enabledTeamsPreserveConfigurationOrderAndLimit() {
        TeamManager manager = createFourTeams();

        List<String> enabledIds = MatchRoster.enabledTeams(manager.getAllTeams(), 2).stream()
                .map(Team::getId)
                .toList();

        assertEquals(List.of("red", "blue"), enabledIds);
        assertThrows(UnsupportedOperationException.class, manager.getAllTeams()::clear);
    }

    @Test
    public void participatingTeamsExcludeEmptyTeams() {
        TeamManager manager = createFourTeams();
        manager.addPlayerToTeam(fakePlayer(UUID.randomUUID()), "red");
        manager.addPlayerToTeam(fakePlayer(UUID.randomUUID()), "blue");

        Set<String> participants = MatchRoster.participatingTeamIds(manager.getAllTeams());

        assertEquals(Set.of("red", "blue"), participants);
    }

    @Test
    public void disabledTeamsCannotLeakIntoParticipantSnapshot() {
        TeamManager manager = createFourTeams();
        manager.addPlayerToTeam(fakePlayer(UUID.randomUUID()), "red");
        manager.addPlayerToTeam(fakePlayer(UUID.randomUUID()), "green");

        List<Team> enabled = MatchRoster.enabledTeams(manager.getAllTeams(), 2);

        assertEquals(Set.of("red"), MatchRoster.participatingTeamIds(enabled));
    }

    private TeamManager createFourTeams() {
        TeamManager manager = new TeamManager();
        TextColor color = TextColor.color(0xFFFFFF);
        manager.createTeam("red", "红队", color);
        manager.createTeam("blue", "蓝队", color);
        manager.createTeam("green", "绿队", color);
        manager.createTeam("yellow", "黄队", color);
        return manager;
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
                    if (method.getReturnType().equals(double.class)) return 0D;
                    if (method.getReturnType().equals(float.class)) return 0F;
                    return null;
                });
    }
}
