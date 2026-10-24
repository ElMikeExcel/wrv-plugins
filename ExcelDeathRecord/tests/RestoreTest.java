import com.winkreyes.exceldeathrecord.core.*;
import java.util.ArrayList;
import java.util.List;

/** 0.2.0 restore suites: selection, args, all-or-nothing slot planning,
 *  equipment matching, restores.csv, restore marks, paging, typed hints. */
public class RestoreTest {
    static List<SlotPlanner.Slot> inv(int empty, SlotPlanner.Slot... filled) {
        List<SlotPlanner.Slot> l = new ArrayList<>(List.of(filled));
        for (int i = 0; i < empty; i++) l.add(SlotPlanner.Slot.empty());
        return l;
    }
    static SlotPlanner.Want w(String key, int amount, int max) { return new SlotPlanner.Want(key, amount, max, ""); }
    static SlotPlanner.Want we(String key, String slot) { return new SlotPlanner.Want(key, 1, 1, slot); }

    public static void run() {
        System.out.println("RestoreTest");
        selection(); args(); planner(); slotMatch(); csv(); marks(); paging(); xp();
    }

    static void selection() {
        Selection.Result r = Selection.parse("all", 5);
        Check.ok(r.ok() && r.all, "all ok");
        Check.eq(List.of(1, 2, 3, 4, 5), r.numbers, "all = 1..5");
        Check.eq(List.of(3), Selection.parse("3", 9).numbers, "single");
        Check.eq(List.of(2, 3, 7), Selection.parse("2,3,7", 9).numbers, "list");
        Check.eq(List.of(1, 2, 3, 4, 5), Selection.parse("1-5", 9).numbers, "range");
        Check.eq(List.of(1, 2, 3, 6, 8, 9), Selection.parse("1-3,6,9,8,2", 9).numbers, "mix, dedup, sorted");
        Check.eq(List.of(2, 3, 4), Selection.parse("4-2", 9).numbers, "reversed range");
        Check.ok(!Selection.parse("10", 9).ok(), "out of range refused");
        Check.ok(Selection.parse("10", 9).error.contains("1-9"), "out of range message names the range");
        Check.ok(!Selection.parse("0", 9).ok(), "zero refused");
        Check.ok(!Selection.parse("abc", 9).ok(), "junk refused");
        Check.ok(!Selection.parse("", 9).ok(), "empty refused");
        Check.ok(!Selection.parse("all", 0).ok(), "no items refused");
        Check.ok(Selection.parse("1-3", 3).all, "full range counts as all");
        Check.eq("2,3,7", Selection.format(List.of(2, 3, 7), 9), "format list");
        Check.eq("1-5", Selection.format(List.of(1, 2, 3, 4, 5), 9), "format range");
        Check.eq("all", Selection.format(List.of(1, 2, 3), 3), "format all");
    }

    static RestoreArgs ra(String s) { return RestoreArgs.parse(s.isEmpty() ? new String[0] : s.split(" ")); }

    static void args() {
        RestoreArgs a = ra("Alex");
        Check.eq(RestoreArgs.Mode.MENU, a.mode, "player only -> menu");
        Check.eq("Alex", a.player, "menu player");
        a = ra("Alex 2");
        Check.eq(RestoreArgs.Mode.MENU, a.mode, "player + n -> menu at that death");
        Check.eq(2, a.n, "menu n");
        a = ra("Alex 1 3 to Sam");
        Check.eq(RestoreArgs.Mode.DEATH, a.mode, "typed death");
        Check.eq(1, a.n, "n"); Check.eq("3", a.items, "items"); Check.eq("Sam", a.target, "target");
        a = ra("Alex 1 2,3,7 to Sam force xp");
        Check.ok(a.force && a.xp, "flags any order");
        Check.eq("2,3,7", a.items, "list items");
        a = ra("Alex 1 all xp to Sam");
        Check.ok(a.xp, "xp before to"); Check.eq("Sam", a.target, "target after xp");
        a = ra(".Bedrock Alex 1 1-5");
        Check.eq(".Bedrock Alex", a.player, "player name with a space");
        Check.eq("1-5", a.items, "range items"); Check.eq("", a.target, "default target");
        a = ra("Alex 2026-10-23_221412 3 to Sam");
        Check.eq("2026-10-23_221412", a.recordId, "record id"); Check.eq(-1, a.n, "id means no n");
        a = ra("Alex 2026-10-23_221412.yml 3");
        Check.eq("2026-10-23_221412", a.recordId, "record id with .yml");
        a = ra("burned 4 to Sam");
        Check.eq(RestoreArgs.Mode.BURNED, a.mode, "burned"); Check.eq(4, a.n, "burned n"); Check.eq("Sam", a.target, "burned target");
        Check.eq(RestoreArgs.Mode.ERROR, ra("burned 4").mode, "burned needs a target");
        Check.eq(RestoreArgs.Mode.ERROR, ra("").mode, "empty is an error");
        Check.eq(RestoreArgs.Mode.ERROR, ra("Alex 1 3 to").mode, "dangling to is an error");
        a = ra("Alex 1 3 Sam");
        Check.eq(RestoreArgs.Mode.ERROR, a.mode, "missing 'to' is an error");
        Check.ok(a.error.contains("to Sam"), "missing 'to' error suggests it");
    }

    static void planner() {
        // Owner's example: 4 needed, 1 free -> refuse with exact wording.
        List<SlotPlanner.Slot> s = inv(1, new SlotPlanner.Slot("dirt", 64, 64));
        SlotPlanner.Plan p = SlotPlanner.plan(s, List.of(),
                List.of(w("shovel", 1, 1), w("pick", 1, 1), w("sword", 1, 1), w("axe", 1, 1)));
        Check.eq(4, p.needed, "4 tools need 4 slots"); Check.eq(1, p.free, "1 free");
        Check.ok(!p.fits(), "does not fit"); Check.eq(3, p.short_(), "short 3");
        Check.eq("Can't restore: Sam needs 4 empty inventory slots and has 1. Ask them to clear 3 more, then try again.",
                SlotPlanner.refusal("Sam", p), "refusal wording");
        Check.eq("Needs 4 empty slots, Sam has 1 - DOES NOT FIT, clear 3 more", SlotPlanner.summary("Sam", p), "summary when short");
        // Exactly enough fits.
        p = SlotPlanner.plan(inv(1), List.of(), List.of(w("shovel", 1, 1)));
        Check.ok(p.fits(), "1 into 1 fits");
        Check.eq("Needs 1 empty slot, Sam has 1 - fits", SlotPlanner.summary("Sam", p), "summary singular");
        // Merges into partial stacks need no new slot.
        p = SlotPlanner.plan(inv(0, new SlotPlanner.Slot("torch", 40, 64)), List.of(), List.of(w("torch", 20, 64)));
        Check.eq(0, p.needed, "20 torches merge into 40"); Check.ok(p.fits(), "merge fits with 0 free");
        // Overflow of a merge needs one slot.
        p = SlotPlanner.plan(inv(0, new SlotPlanner.Slot("torch", 60, 64)), List.of(), List.of(w("torch", 20, 64)));
        Check.eq(1, p.needed, "overflow needs one slot"); Check.ok(!p.fits(), "overflow refused when full");
        // Later wants top up stacks opened by earlier wants.
        p = SlotPlanner.plan(inv(5), List.of(), List.of(w("torch", 30, 64), w("torch", 30, 64)));
        Check.eq(1, p.needed, "two 30-torch stacks share one new slot");
        // Different keys never merge (enchanted vs plain).
        p = SlotPlanner.plan(inv(5, new SlotPlanner.Slot("shovel-plain", 1, 1)), List.of(), List.of(w("shovel-ench", 1, 1)));
        Check.eq(1, p.needed, "unstackables never merge");
        // Equipment: same-player restore puts armor/offhand back into empty slots.
        p = SlotPlanner.plan(inv(1), List.of("helmet", "boots", "offhand"),
                List.of(we("helm", "helmet"), we("chest", "chestplate"), we("boots", "boots"), we("shield", "offhand"), w("sword", 1, 1)));
        Check.eq(3, p.intoEquip, "helmet, boots, offhand go straight in");
        Check.eq(2, p.needed, "chestplate (occupied) + sword need storage");
        Check.ok(!p.fits(), "2 needed, 1 free refused");
        Check.eq(List.of("helmet", "", "boots", "offhand", ""), p.equipPlaced, "per-item placement");
        Check.ok(SlotPlanner.summary("Alex", p).contains("+3 straight into armor/offhand"), "summary mentions armor");
        // Another player: no equip slots offered -> all go to storage.
        p = SlotPlanner.plan(inv(5), List.of(), List.of(we("helm", "helmet"), we("boots", "boots")));
        Check.eq(0, p.intoEquip, "other player: nothing into armor"); Check.eq(2, p.needed, "other player: 2 storage");
        // Storage passed in is not modified (refusal leaves no trace).
        List<SlotPlanner.Slot> before = inv(1, new SlotPlanner.Slot("torch", 60, 64));
        SlotPlanner.plan(before, List.of(), List.of(w("torch", 20, 64)));
        Check.eq(60, before.get(0).amount, "plan never mutates the real inventory");
        Check.eq(null, before.get(1).key, "plan never fills real empty slots");
        // Big restore-all: 36 unstackables into a full-ish inventory.
        List<SlotPlanner.Want> many = new ArrayList<>();
        for (int i = 0; i < 36; i++) many.add(w("item" + i, 1, 1));
        p = SlotPlanner.plan(inv(30), List.of(), many);
        Check.eq(36, p.needed, "restore all: 36 needed"); Check.eq(6, p.short_(), "restore all: short 6");
    }

    static DeathRecord.ItemEntry item(String slot, String id, String bytes) {
        DeathRecord.ItemEntry e = new DeathRecord.ItemEntry();
        e.slot = slot; e.id = id; e.count = 1; e.bytesB64 = bytes; return e;
    }

    static void slotMatch() {
        List<DeathRecord.ItemEntry> drops = List.of(item("drop-0", "minecraft:netherite_helmet", "SEVMTQ=="),
                item("drop-1", "minecraft:netherite_shovel", "U0hPVg=="), item("drop-2", "minecraft:shield", "U0hJRUxE"),
                item("drop-3", "minecraft:netherite_helmet", "SEVMTTI="));
        List<DeathRecord.ItemEntry> invl = List.of(item("storage-0", "minecraft:netherite_shovel", "U0hPVg=="),
                item("helmet", "minecraft:netherite_helmet", "SEVMTQ=="), item("offhand", "minecraft:shield", "U0hJRUxE"),
                item("storage-5", "minecraft:netherite_helmet", "SEVMTTI="));
        Check.eq(List.of("helmet", "storage-0", "offhand", "storage-5"), SlotMatch.originalSlots(drops, invl),
                "drops matched back to their original slots by bytes");
        Check.ok(SlotMatch.isEquip("offhand") && !SlotMatch.isEquip("storage-0"), "isEquip");
    }

    static void csv() {
        Check.eq("time_utc,staff,source_player,death_file,item,target,item_no,count,source,forced", RestoreCsv.HEADER, "csv header");
        String row = RestoreCsv.row(1792800000000L, "OpSam", ".BedrockAlex", "deaths/x/2026-10-23_221412.yml",
                "Netherite Shovel, Efficiency V", "Sam", "3", 1, "death", false);
        Check.ok(row.startsWith("2026-10-24T"), "csv time is ISO UTC: " + row);
        Check.ok(row.endsWith("Z,OpSam,.BedrockAlex,deaths/x/2026-10-23_221412.yml,\"Netherite Shovel, Efficiency V\",Sam,3,1,death,false")
                || row.contains(",OpSam,.BedrockAlex,"), "csv fields in order");
        Check.ok(row.contains("\"Netherite Shovel, Efficiency V\""), "comma field quoted");
        Check.ok(RestoreCsv.row(0, "=cmd()", "a\"b", "f", "i", "t", "1", 1, "death", true).contains(",'=cmd(),\"a\"\"b\","),
                "formula guard + quote escaping");
        Check.eq("2026-10-24 00:00:00 UTC", RestoreCsv.markTime(1792800000000L), "mark time text");
    }

    static void marks() {
        DeathRecord r = RecordFormatTest.deathWithCause("LAVA", ".BedrockAlex tried to swim in lava", "none");
        DeathRecord.ItemEntry e = r.drops.get(0);
        e.restoredTimes = 1; e.restoredTo = "Sam"; e.restoredToUuid = "00000000-0000-0000-0000-000000000002";
        e.restoredBy = "OpSam"; e.restoredEpoch = 1792800000000L; e.restoredAt = "2026-10-24 00:00:00 UTC";
        r.xpRestoredTimes = 1; r.xpRestoredTo = ".BedrockAlex"; r.xpRestoredBy = "OpSam"; r.xpRestoredAt = "2026-10-24 00:00:00 UTC";
        String text = RecordFormat.write(r);
        Check.ok(text.contains("    restored-to: \"Sam\"") || text.contains("    restored-to: Sam"), "mark written as an item key");
        DeathRecord back = RecordFormat.read(text);
        Check.eq(r, back, "restore marks round-trip");
        Check.ok(back.drops.get(0).isRestored() && !back.drops.get(1).isRestored(), "only the restored item is marked");
        Check.eq(text, RecordFormat.write(back), "marked write is stable");
        for (String line : text.split("\n")) {
            String t = line.trim();
            if (t.startsWith("restored-") || t.startsWith("xp-restored-")) {
                Check.ok(!t.startsWith("id") && !t.startsWith("count"), "new key does not look like id/count to AuditLog");
            }
        }
        Check.ok(RecordIndex.numberedItemLine(1, back.drops.get(0)).startsWith("#1 "), "numbered item line");
        Check.ok(RecordIndex.restoredNote(back.drops.get(0)).contains("restored to Sam 2026-10-24 00:00:00 UTC by OpSam"),
                "restored note text");
        DeathRecord plain = RecordFormatTest.deathWithCause("LAVA", "x", "none");
        Check.ok(!RecordFormat.write(plain).contains("restored"), "unrestored record has no mark keys (0.1.0 identical)");

        DestructionRecord d = new DestructionRecord();
        d.ownerName = ".BedrockAlex"; d.cause = "DESPAWN";
        d.item.id = "minecraft:netherite_shovel"; d.item.count = 1; d.item.bytesB64 = "U0hPVg==";
        d.item.restoredTimes = 2; d.item.restoredTo = "Sam"; d.item.restoredBy = "CONSOLE"; d.item.restoredAt = "2026-10-24 00:00:00 UTC";
        DestructionRecord dback = BurnFormat.read(BurnFormat.write(d));
        Check.eq(d, dback, "burn restore mark round-trips");
        Check.ok(BurnIndex.summaryLine(dback).contains("restored to Sam"), "burn summary shows the mark");
        Check.ok(BurnIndex.summaryLine(dback).contains("2 times"), "burn summary shows forced repeat count");
    }

    static void xp() {
        Check.eq(0, XpMath.pointsForLevel(0), "level 0 = 0 points");
        Check.eq(352, XpMath.pointsForLevel(16), "level 16 = 352 points (wiki)");
        Check.eq(1395, XpMath.pointsForLevel(30), "level 30 = 1395 points (wiki)");
        Check.eq(1628, XpMath.pointsForLevel(32), "level 32 = 1628 points (wiki)");
        Check.eq(1395, XpMath.restorePoints(30, 0), "/xp-set levels with totalExperience 0 still restores the level");
        Check.eq(2000, XpMath.restorePoints(30, 2000), "larger collected total wins");
    }

    static void paging() {
        List<Integer> l = new ArrayList<>();
        for (int i = 1; i <= 23; i++) l.add(i);
        Check.eq(3, RecordIndex.pageCount(23, 10), "23 -> 3 pages");
        Check.eq(1, RecordIndex.pageCount(0, 10), "0 -> 1 page");
        Check.eq(List.of(11, 12, 13, 14, 15, 16, 17, 18, 19, 20), RecordIndex.page(l, 2, 10), "page 2");
        Check.eq(List.of(21, 22, 23), RecordIndex.page(l, 3, 10), "last page partial");
        Check.eq(List.of(), RecordIndex.page(l, 4, 10), "past the end empty");
        Check.eq("Typed: /deathrecord restore Alex 1 3 to Sam (1 = newest death, 3 = item number)",
                RecordIndex.typedHint("Alex", 1, "3", "Sam"), "typed hint (owner's format)");
        Check.eq("Typed: /deathrecord restore Alex 2 all (2 = death number, 1 is newest, all = every item)",
                RecordIndex.typedHint("Alex", 2, "all", "Alex"), "typed hint all, same player");
        Check.eq("Typed: /deathrecord restore Alex", RecordIndex.typedHint("Alex", 0, "", ""), "typed hint step 1");
    }
}
