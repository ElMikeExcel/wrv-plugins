package excelemfixes.seal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure data model for the seal-restore module: rows of boss
 * filename -> blocks to restore when that boss dies. Parsed from
 * plain maps (the module feeds it Bukkit's YAML maps) so it is
 * unit-testable without a server.
 *
 * <p>Each block carries the blockdata string it should end up as
 * ({@code restore}) and optionally the blockdata string the seal
 * left there ({@code sealed}). The guard matters: the module
 * only ever touches a block that is either still wearing its
 * seal or (when no seal state is declared) simply differs from
 * the restore target — it never overwrites a block a player or
 * another system changed on purpose.
 */
public final class SealRestoreData {

    /** One block to restore. */
    public record BlockEntry(String world, int x, int y, int z, String restore, String sealed) {
    }

    /** One boss row. */
    public record Row(String boss, List<BlockEntry> blocks) {
    }

    private final Map<String, List<BlockEntry>> byBoss = new LinkedHashMap<>();

    private SealRestoreData() {
    }

    /** Parse the {@code restores} list from seal-restore.yml. */
    public static SealRestoreData parse(Object restoresList) {
        SealRestoreData data = new SealRestoreData();
        if (!(restoresList instanceof List<?> rows)) return data;
        for (Object rowObj : rows) {
            if (!(rowObj instanceof Map<?, ?> row)) continue;
            Object boss = row.get("boss");
            if (boss == null) continue;
            List<BlockEntry> blocks = new ArrayList<>();
            if (row.get("blocks") instanceof List<?> blockList) {
                for (Object blockObj : blockList) {
                    if (!(blockObj instanceof Map<?, ?> b)) continue;
                    if (b.get("world") == null || b.get("restore") == null) continue;
                    blocks.add(new BlockEntry(
                            String.valueOf(b.get("world")),
                            asInt(b.get("x")), asInt(b.get("y")), asInt(b.get("z")),
                            String.valueOf(b.get("restore")),
                            b.get("sealed") == null ? null : String.valueOf(b.get("sealed"))));
                }
            }
            if (!blocks.isEmpty()) {
                data.byBoss.computeIfAbsent(String.valueOf(boss), k -> new ArrayList<>()).addAll(blocks);
            }
        }
        return data;
    }

    private static int asInt(Object value) {
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Blocks registered for a boss filename (empty when none). */
    public List<BlockEntry> blocksFor(String bossFilename) {
        if (bossFilename == null) return List.of();
        return byBoss.getOrDefault(bossFilename, List.of());
    }

    /** Boss filenames with at least one registered block. */
    public List<String> bosses() {
        return List.copyOf(byBoss.keySet());
    }

    public int totalBlocks() {
        int total = 0;
        for (List<BlockEntry> blocks : byBoss.values()) total += blocks.size();
        return total;
    }

    /**
     * The restore guard: restore when the block still wears its
     * declared seal state, or — with no seal state declared —
     * whenever it differs from the restore target.
     */
    public static boolean shouldRestore(String currentBlockData, BlockEntry entry) {
        if (currentBlockData == null || entry == null || entry.restore() == null) return false;
        if (entry.sealed() != null) return currentBlockData.equals(entry.sealed());
        return !currentBlockData.equals(entry.restore());
    }
}
