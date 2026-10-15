package top.coast23.ItemRacing.models;

import org.bukkit.Material;

import java.util.Objects;

/**
 * 一种任务物品的静态定义。定义来自 items.yml，而不是散落在业务代码中的黑名单。
 */
public record ItemDefinition(
        Material material,
        int difficulty,
        boolean mandatoryExcluded,
        boolean defaultBanned
) {
    public ItemDefinition {
        Objects.requireNonNull(material, "material");
        difficulty = Math.max(1, Math.min(difficulty, 5));
    }
}
