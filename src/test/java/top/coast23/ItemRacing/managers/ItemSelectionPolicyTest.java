package top.coast23.ItemRacing.managers;

import org.bukkit.Material;
import org.junit.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ItemSelectionPolicyTest {

    @Test
    public void expectedDifficultyFollowsConfiguredCurve() {
        ItemSelectionPolicy policy = new ItemSelectionPolicy(new Random(1), 1.0, 4.5, 1.35);

        assertEquals(1.0, policy.expectedDifficulty(1, 20), 0.0001);
        assertEquals(1.43, policy.expectedDifficulty(5, 20), 0.02);
        assertEquals(2.28, policy.expectedDifficulty(10, 20), 0.02);
        assertEquals(3.32, policy.expectedDifficulty(15, 20), 0.02);
        assertEquals(4.5, policy.expectedDifficulty(20, 20), 0.0001);
    }

    @Test
    public void progressiveSequenceGetsHarderInExpectation() {
        double[] sums = new double[20];
        int samples = 20_000;
        ItemSelectionPolicy policy = new ItemSelectionPolicy(new Random(20261015), 1.0, 4.5, 1.35);

        for (int sample = 0; sample < samples; sample++) {
            List<Integer> sequence = policy.createDifficultySequence(
                    20, 1, 20, ItemSelectionPolicy.Mode.PROGRESSIVE);
            for (int index = 0; index < sequence.size(); index++) sums[index] += sequence.get(index);
        }

        for (int index = 1; index < sums.length; index++) {
            assertTrue("位置 " + index + " 的平均难度应不低于前一位置",
                    sums[index] / samples >= sums[index - 1] / samples - 0.03);
        }
        assertTrue(sums[19] / samples > sums[0] / samples + 3.3);
    }

    @Test
    public void selectionExcludesBansAndNeverDuplicates() {
        ItemSelectionPolicy policy = new ItemSelectionPolicy(new Random(7), 1.0, 4.5, 1.35);
        List<Material> candidates = List.of(
                Material.DIRT, Material.STONE, Material.IRON_INGOT,
                Material.DIAMOND, Material.BLAZE_ROD, Material.ELYTRA);
        Map<Material, Integer> difficulty = new HashMap<>();
        difficulty.put(Material.DIRT, 1);
        difficulty.put(Material.STONE, 1);
        difficulty.put(Material.IRON_INGOT, 2);
        difficulty.put(Material.DIAMOND, 3);
        difficulty.put(Material.BLAZE_ROD, 4);
        difficulty.put(Material.ELYTRA, 5);

        List<Material> selected = policy.select(candidates,
                EnumSet.of(Material.ELYTRA), List.of(1, 2, 3, 4, 5),
                difficulty::get, ItemSelectionPolicy.Mode.PROGRESSIVE);

        assertEquals(5, selected.size());
        assertFalse(selected.contains(Material.ELYTRA));
        assertEquals(selected.size(), new HashSet<>(selected).size());
    }

    @Test
    public void rerollSequenceKeepsAbsoluteProgress() {
        ItemSelectionPolicy policy = new ItemSelectionPolicy(new Random(12), 1.0, 4.5, 1.35);

        List<Integer> lateSequence = policy.createDifficultySequence(
                20, 13, 8, ItemSelectionPolicy.Mode.STRICT);

        assertEquals(8, lateSequence.size());
        assertTrue(lateSequence.get(0) >= 3);
        assertEquals(Integer.valueOf(5), lateSequence.get(7));
    }

    @Test
    public void strictModeReturnsNonDecreasingDifficulty() {
        ItemSelectionPolicy policy = new ItemSelectionPolicy(new Random(99), 1.0, 5.0, 1.0);
        List<Material> candidates = List.of(
                Material.DIRT, Material.STONE, Material.IRON_INGOT,
                Material.DIAMOND, Material.BLAZE_ROD, Material.ELYTRA);
        Map<Material, Integer> difficulty = Map.of(
                Material.DIRT, 1,
                Material.STONE, 1,
                Material.IRON_INGOT, 2,
                Material.DIAMOND, 3,
                Material.BLAZE_ROD, 4,
                Material.ELYTRA, 5);
        List<Integer> sequence = policy.createDifficultySequence(
                5, 1, 5, ItemSelectionPolicy.Mode.STRICT);

        List<Material> selected = policy.select(candidates, Set.of(), sequence,
                difficulty::get, ItemSelectionPolicy.Mode.STRICT);

        for (int index = 1; index < selected.size(); index++) {
            assertTrue(difficulty.get(selected.get(index)) >= difficulty.get(selected.get(index - 1)));
        }
    }
}
