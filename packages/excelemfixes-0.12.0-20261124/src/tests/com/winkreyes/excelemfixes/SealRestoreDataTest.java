import excelemfixes.seal.SealRestoreData;
import java.util.List;
import java.util.Map;

/** Seal Restore data parsing + the restore guard. */
public final class SealRestoreDataTest {

    public static void run() {
        System.out.println("SealRestoreDataTest");
        Object rows = List.of(
                Map.of("boss", "arena_boss.yml", "blocks", List.of(
                        Map.of("world", "world", "x", 100, "y", 64, "z", 200,
                                "restore", "minecraft:air", "sealed", "minecraft:barrier"),
                        Map.of("world", "world", "x", 101, "y", 64, "z", 200,
                                "restore", "minecraft:stone_bricks"))),
                Map.of("boss", "other_boss.yml", "blocks", List.of(
                        Map.of("world", "em_x", "x", -5, "y", 70, "z", 9,
                                "restore", "minecraft:polished_blackstone",
                                "sealed", "minecraft:red_nether_brick_slab[type=top,waterlogged=false]"))),
                Map.of("noboss", true),
                "garbage row");
        SealRestoreData data = SealRestoreData.parse(rows);
        Check.eq(2, data.bosses().size(), "two boss rows parsed, junk skipped");
        Check.eq(3, data.totalBlocks(), "three blocks total");
        Check.eq(2, data.blocksFor("arena_boss.yml").size(), "arena boss has two blocks");
        Check.eq(0, data.blocksFor("missing.yml").size(), "unknown boss yields no blocks");
        Check.eq(0, SealRestoreData.parse(null).totalBlocks(), "null input parses empty");
        Check.eq(0, SealRestoreData.parse("junk").totalBlocks(), "non-list input parses empty");

        SealRestoreData.BlockEntry guarded = data.blocksFor("arena_boss.yml").get(0);
        Check.ok(SealRestoreData.shouldRestore("minecraft:barrier", guarded),
                "guard: still sealed -> restore");
        Check.ok(!SealRestoreData.shouldRestore("minecraft:air", guarded),
                "guard: already restored -> skip");
        Check.ok(!SealRestoreData.shouldRestore("minecraft:gold_block", guarded),
                "guard: changed to something else -> never touch");

        SealRestoreData.BlockEntry unguarded = data.blocksFor("arena_boss.yml").get(1);
        Check.ok(SealRestoreData.shouldRestore("minecraft:barrier", unguarded),
                "no seal state: differs from target -> restore");
        Check.ok(!SealRestoreData.shouldRestore("minecraft:stone_bricks", unguarded),
                "no seal state: already at target -> skip");
    }
}
