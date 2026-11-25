package excelemfixes.foodstack;

/**
 * Pure merge arithmetic for one inventory's slots: given each
 * slot's similarity group, amount, and max stack size, compute
 * the amounts after pulling later stacks into earlier ones of
 * the same group. Slot order is preserved, nothing moves
 * between groups, and no slot ever exceeds its max — the same
 * end state a player reaches by hand-stacking, so an inventory
 * full of freshly normalized copies converges to full stacks
 * plus at most one partial stack per item.
 *
 * <p>Kept Bukkit-free so the suites can prove the arithmetic;
 * the module assigns group ids with {@code ItemStack.isSimilar}
 * against each group's first stack and applies the returned
 * amounts (0 = clear the slot).
 */
public final class StackMerger {

    private StackMerger() {}

    /**
     * @param groups    similarity group id per slot (equal ids
     *                  may merge; negative = never merges, e.g.
     *                  an empty slot)
     * @param amounts   current amount per slot
     * @param maxStacks max stack size per slot
     * @return the new amount per slot, same length as the inputs
     */
    public static int[] merge(int[] groups, int[] amounts, int[] maxStacks) {
        int n = amounts.length;
        int[] out = amounts.clone();
        for (int i = 0; i < n; i++) {
            if (groups[i] < 0 || out[i] <= 0 || out[i] >= maxStacks[i]) {
                continue;
            }
            for (int j = i + 1; j < n && out[i] < maxStacks[i]; j++) {
                if (groups[j] != groups[i] || out[j] <= 0) {
                    continue;
                }
                int move = Math.min(maxStacks[i] - out[i], out[j]);
                out[i] += move;
                out[j] -= move;
            }
        }
        return out;
    }
}
