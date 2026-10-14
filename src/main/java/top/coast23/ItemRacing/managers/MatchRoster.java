package top.coast23.ItemRacing.managers;

import top.coast23.ItemRacing.models.Team;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 不依赖 Bukkit 运行时的队伍筛选规则，便于独立测试。 */
final class MatchRoster {
    private MatchRoster() {}

    static List<Team> enabledTeams(Collection<Team> configuredTeams, int enabledCount) {
        return configuredTeams.stream()
                .limit(Math.max(0, Math.min(enabledCount, configuredTeams.size())))
                .toList();
    }

    static Set<String> participatingTeamIds(Collection<Team> enabledTeams) {
        Set<String> result = new LinkedHashSet<>();
        enabledTeams.stream()
                .filter(team -> team.getSize() > 0)
                .map(Team::getId)
                .forEach(result::add);
        return result;
    }
}
