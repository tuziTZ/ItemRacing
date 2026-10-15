package top.coast23.ItemRacing.managers;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.random.RandomGenerator;

/**
 * 可测试、可注入随机数的任务物品选择策略。
 */
public final class ItemSelectionPolicy {

    public enum Mode {
        RANDOM("完全随机"),
        PROGRESSIVE("渐进难度"),
        STRICT("严格递增");

        private final String displayName;

        Mode(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }

        public Mode next() {
            Mode[] modes = values();
            return modes[(ordinal() + 1) % modes.length];
        }
    }

    private final RandomGenerator random;
    private final double minimumDifficulty;
    private final double maximumDifficulty;
    private final double curveExponent;

    public ItemSelectionPolicy(RandomGenerator random, double minimumDifficulty,
                               double maximumDifficulty, double curveExponent) {
        this.random = Objects.requireNonNull(random, "random");
        this.minimumDifficulty = clamp(minimumDifficulty, 1.0, 5.0);
        this.maximumDifficulty = clamp(maximumDifficulty, this.minimumDifficulty, 5.0);
        this.curveExponent = Math.max(0.05, curveExponent);
    }

    /**
     * 为一段任务生成目标难度。渐进模式使用相邻等级随机取整，因此其期望等于曲线目标值。
     */
    public List<Integer> createDifficultySequence(int totalCount, int startOrdinal,
                                                   int amount, Mode mode) {
        if (totalCount < 1 || amount < 1) return List.of();

        int safeStart = Math.max(1, startOrdinal);
        int safeAmount = Math.min(amount, Math.max(0, totalCount - safeStart + 1));
        List<Integer> result = new ArrayList<>(safeAmount);

        for (int offset = 0; offset < safeAmount; offset++) {
            int ordinal = safeStart + offset;
            if (mode == Mode.RANDOM) {
                result.add(0); // 0 表示不限制难度。
                continue;
            }

            double expected = expectedDifficulty(ordinal, totalCount);
            if (mode == Mode.STRICT) {
                result.add(clampDifficulty((int) Math.round(expected)));
                continue;
            }

            int lower = clampDifficulty((int) Math.floor(expected));
            int upper = clampDifficulty((int) Math.ceil(expected));
            double upperProbability = expected - Math.floor(expected);
            result.add(lower == upper || random.nextDouble() >= upperProbability ? lower : upper);
        }
        return result;
    }

    public double expectedDifficulty(int ordinal, int totalCount) {
        if (totalCount <= 1) return minimumDifficulty;
        double progress = clamp((ordinal - 1.0) / (totalCount - 1.0), 0.0, 1.0);
        return minimumDifficulty
                + (maximumDifficulty - minimumDifficulty) * Math.pow(progress, curveExponent);
    }

    /**
     * 按给定难度序列选择不重复物品。目标等级为空时，自动回退到最近的非空等级。
     */
    public List<Material> select(Collection<Material> candidates,
                                 Set<Material> excluded,
                                 List<Integer> difficultySequence,
                                 ToIntFunction<Material> difficultyResolver,
                                 Mode mode) {
        Objects.requireNonNull(candidates, "candidates");
        Objects.requireNonNull(excluded, "excluded");
        Objects.requireNonNull(difficultySequence, "difficultySequence");
        Objects.requireNonNull(difficultyResolver, "difficultyResolver");

        Set<Material> excludedCopy = new HashSet<>(excluded);
        List<Material> remaining = candidates.stream()
                .filter(material -> !excludedCopy.contains(material))
                .distinct()
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        List<Material> selected = new ArrayList<>(Math.min(remaining.size(), difficultySequence.size()));
        int strictMinimum = 1;

        for (int desiredDifficulty : difficultySequence) {
            if (remaining.isEmpty()) break;

            List<Material> choices;
            if (desiredDifficulty == 0) {
                choices = remaining;
            } else {
                int lowerBound = mode == Mode.STRICT ? strictMinimum : 1;
                int nearestDistance = remaining.stream()
                        .mapToInt(difficultyResolver)
                        .filter(level -> level >= lowerBound)
                        .map(level -> Math.abs(level - desiredDifficulty))
                        .min()
                        .orElse(Integer.MAX_VALUE);

                choices = remaining.stream()
                        .filter(material -> difficultyResolver.applyAsInt(material) >= lowerBound)
                        .filter(material -> Math.abs(difficultyResolver.applyAsInt(material) - desiredDifficulty) == nearestDistance)
                        .toList();

                // 极端自定义配置可能耗尽所有不低于上一等级的候选物，保证仍能生成完整列表。
                if (choices.isEmpty()) {
                    int fallbackDistance = remaining.stream()
                            .mapToInt(difficultyResolver)
                            .map(level -> Math.abs(level - desiredDifficulty))
                            .min()
                            .orElse(0);
                    choices = remaining.stream()
                            .filter(material -> Math.abs(difficultyResolver.applyAsInt(material) - desiredDifficulty) == fallbackDistance)
                            .toList();
                }
            }

            Material chosen = choices.get(random.nextInt(choices.size()));
            selected.add(chosen);
            remaining.remove(chosen);
            if (mode == Mode.STRICT) {
                strictMinimum = Math.max(strictMinimum, difficultyResolver.applyAsInt(chosen));
            }
        }

        if (mode == Mode.STRICT) {
            selected.sort(Comparator.comparingInt(difficultyResolver));
        }
        return selected;
    }

    private static int clampDifficulty(int difficulty) {
        return Math.max(1, Math.min(difficulty, 5));
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }
}
