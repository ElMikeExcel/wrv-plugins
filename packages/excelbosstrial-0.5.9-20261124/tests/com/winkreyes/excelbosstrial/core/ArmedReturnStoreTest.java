/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * 0.5.9: the armed-returns store rules - one arm per
 * (player, loot file) pair, the encode/decode round trip,
 * and the checksum posture: a tampered or malformed file is
 * refused whole, never half-read (a half-read arm store
 * could drop an owed return or double it). Plain JVM; the
 * store is Bukkit-free by design.
 */
public final class ArmedReturnStoreTest {

    private static int passed;
    private static int failed;

    private static void check(boolean cond, String label) {
        if (cond) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }

    private static final UUID JESTER = UUID.fromString("e739fb4d-7cec-48f4-8a2b-7c25e27d8b9e");
    private static final UUID MARVIN = UUID.fromString("8399c623-786f-448c-8b11-f54504721819");
    private static final String DAGGER = "primis_ice_village_alanus_dagger.yml";

    private static ArmedReturnStore.Arm arm(UUID player, String file, long at) {
        return new ArmedReturnStore.Arm(player, "Player", file, "CONSOLE", at);
    }

    public static void main(String[] args) throws Exception {
        // Empty store round-trips.
        ArmedReturnStore empty = ArmedReturnStore.empty();
        check(empty.isEmpty() && empty.size() == 0, "empty store is empty");
        ArmedReturnStore emptyBack = ArmedReturnStore.decode(empty.encode());
        check(emptyBack.isEmpty(), "empty store survives the round trip");

        // Add + find + list.
        ArmedReturnStore store = ArmedReturnStore.empty();
        check(store.add(arm(JESTER, DAGGER, 2000)), "first arm accepted");
        check(store.add(arm(JESTER, "other_item.yml", 1000)), "second file for one player accepted");
        check(store.add(arm(MARVIN, DAGGER, 3000)), "same file for another player accepted");
        check(store.size() == 3, "three arms held");
        check(store.find(JESTER, DAGGER).isPresent(), "find hits the armed pair");
        check(store.find(JESTER, "missing.yml").isEmpty(), "find misses an unarmed pair");
        check(store.find(MARVIN, "other_item.yml").isEmpty(), "find is pair-exact");
        List<ArmedReturnStore.Arm> jesterArms = store.forPlayer(JESTER);
        check(jesterArms.size() == 2
                        && jesterArms.get(0).lootFile().equals("other_item.yml"),
                "forPlayer is oldest-first");
        check(store.all().size() == 3 && store.all().get(0).armedAtMs() == 1000,
                "all() is oldest-first");

        // Duplicate pair: refused, nothing changes.
        check(!store.add(arm(JESTER, DAGGER, 9999)), "duplicate pair refused");
        check(store.size() == 3, "duplicate changed nothing");
        check(store.find(JESTER, DAGGER).orElseThrow().armedAtMs() == 2000,
                "the original arm survives a duplicate attempt");

        // Round trip preserves every field.
        ArmedReturnStore back = ArmedReturnStore.decode(store.encode());
        check(back.size() == 3, "round trip keeps every arm");
        ArmedReturnStore.Arm reread = back.find(JESTER, DAGGER).orElseThrow();
        check(reread.playerName().equals("Player") && reread.armedBy().equals("CONSOLE")
                        && reread.armedAtMs() == 2000,
                "round trip preserves name, armed-by and timestamp");

        // Tamper: any body edit breaks the checksum.
        String encoded = store.encode();
        String tampered = encoded.replace("other_item.yml", "other_item2.yml");
        boolean refused = false;
        try {
            ArmedReturnStore.decode(tampered);
        } catch (IOException expected) {
            refused = true;
        }
        check(refused, "a tampered body is refused whole (checksum)");

        // A file with no checksum line is refused.
        refused = false;
        try {
            ArmedReturnStore.decode("# just a header\n");
        } catch (IOException expected) {
            refused = true;
        }
        check(refused, "a checksum-less file is refused");

        // A malformed arm line under a VALID checksum is refused.
        String body = "# header\nnot-a-uuid\tPlayer\t" + DAGGER + "\tCONSOLE\t1000\n";
        String forged = body + "sha256:" + sha256(body) + "\n";
        refused = false;
        try {
            ArmedReturnStore.decode(forged);
        } catch (IOException expected) {
            refused = true;
        }
        check(refused, "a malformed line under a valid checksum is refused");

        // A duplicated pair inside one file is refused.
        String dupBody = "# header\n"
                + JESTER + "\tPlayer\t" + DAGGER + "\tCONSOLE\t1000\n"
                + JESTER + "\tPlayer\t" + DAGGER + "\tCONSOLE\t2000\n";
        String dupForged = dupBody + "sha256:" + sha256(dupBody) + "\n";
        refused = false;
        try {
            ArmedReturnStore.decode(dupForged);
        } catch (IOException expected) {
            refused = true;
        }
        check(refused, "a file arming one pair twice is refused");

        // Removes.
        check(store.remove(JESTER, DAGGER), "remove takes the named pair");
        check(!store.remove(JESTER, DAGGER), "a second remove finds nothing");
        check(store.removeAll(JESTER) == 1, "removeAll takes the player's remaining arm");
        check(store.removeAll(JESTER) == 0, "removeAll on an empty player takes none");
        check(store.size() == 1 && store.find(MARVIN, DAGGER).isPresent(),
                "removes never touch another player");

        // Field discipline: separators and path shapes are refused.
        boolean rejected = false;
        try {
            new ArmedReturnStore.Arm(JESTER, "Bad\tName", DAGGER, "CONSOLE", 0);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        check(rejected, "a tabbed field is rejected at construction");
        rejected = false;
        try {
            new ArmedReturnStore.Arm(JESTER, "Player", "../evil.yml", "CONSOLE", 0);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        check(rejected, "a path-shaped loot file is rejected at construction");
        rejected = false;
        try {
            new ArmedReturnStore.Arm(JESTER, "Player", DAGGER, "CONSOLE", -1);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        check(rejected, "a negative timestamp is rejected at construction");

        System.out.println("ArmedReturnStoreTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static String sha256(String body) throws Exception {
        var digest = java.security.MessageDigest.getInstance("SHA-256");
        return java.util.HexFormat.of().formatHex(
                digest.digest(body.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
}
