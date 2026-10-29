/* Excel WRV maintained source; original attribution and license preserved. */ package com.winkreyes.makersmark.core;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CoreTest {
    private static final List<String> PRE = List.of("~", ".");

    @Test void namesStripPrefixesAndColors() {
        assertEquals("Builder", NameClean.clean("~Builder", "x", PRE, 24));
        assertEquals("Builder", NameClean.clean("\u00a7a~\u00a7lBuilder", "x", PRE, 24));
        assertEquals("BedrockKid", NameClean.clean(null, ".BedrockKid", PRE, 24));
        assertEquals("Mixed", NameClean.clean("~.Mixed", "x", PRE, 24));
        assertEquals("Fallback", NameClean.clean("  ", "Fallback", PRE, 24));
        assertEquals("Hex", NameClean.clean("&x&f&f&0&0&0&0Hex", "x", PRE, 24));
        assertEquals("Someone", NameClean.clean(null, ".", PRE, 24));
        assertEquals(5, NameClean.clean("LongNickname", "x", PRE, 5).length());
    }

    @Test void gearAllowlist() {
        for (String m : List.of("DIAMOND_SWORD", "NETHERITE_AXE", "IRON_PICKAXE", "WOODEN_HOE", "STONE_SHOVEL", "SHEARS", "BOW", "CROSSBOW",
                "TRIDENT", "MACE", "IRON_SPEAR", "SHIELD", "ELYTRA", "TURTLE_HELMET", "LEATHER_BOOTS", "NETHERITE_CHESTPLATE", "FISHING_ROD"))
            assertTrue(Gear.markable(m, 1), m);
        for (String m : List.of("STICK", "TORCH", "BUCKET", "IRON_HORSE_ARMOR", "LEGACY_IRON_SWORD", "CHEST", "WOLF_ARMOR"))
            assertFalse(Gear.markable(m, 1), m);
        assertFalse(Gear.markable("DIAMOND_SWORD", 64), "stackable never signed");
        assertTrue(Gear.weapon("BOW")); assertTrue(Gear.weapon("TRIDENT")); assertTrue(Gear.weapon("NETHERITE_AXE"));
        // Expanded user-requested ordinary-gear history: tools and shield melee are now supported.
        assertTrue(Gear.weapon("IRON_PICKAXE")); assertTrue(Gear.weapon("SHIELD"));
        assertTrue(Gear.blockStats("SHIELD")); assertTrue(Gear.blockStats("DIAMOND_CHESTPLATE"));
        assertFalse(Gear.blockStats("ELYTRA")); assertFalse(Gear.weapon("STICK"));
        assertTrue(Gear.armor("IRON_HELMET")); assertFalse(Gear.armor("ELYTRA"));
        assertEquals(Gear.Kind.RANGED, Gear.kind("CROSSBOW"));
    }

    @Test void trophiesRoundTripAndMax() {
        Map<String, Integer> m = Trophies.parse("ENDER_DRAGON=1;WITHER=2;bad;X=0;Y=z");
        assertEquals(Map.of("ENDER_DRAGON", 1, "WITHER", 2), m);
        assertEquals("ENDER_DRAGON=1;WITHER=2", Trophies.serialize(m));
        Map<String, Integer> other = new LinkedHashMap<>(); other.put("WITHER", 5); other.put("WARDEN", 1);
        assertEquals(Map.of("ENDER_DRAGON", 1, "WITHER", 5, "WARDEN", 1), Trophies.max(m, other));
        assertTrue(Trophies.parse(null).isEmpty());
        assertEquals("Elder Guardian", Trophies.pretty("ELDER_GUARDIAN"));
        assertEquals("Zombie King", Trophies.pretty("NAME:ZOMBIE_KING"));
    }

    @Test void slainLineWraps() {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("ENDER_DRAGON", 1); m.put("WITHER", 2);
        assertEquals(List.of("Slain: Ender Dragon, Wither x2"), Trophies.lines(m, Trophies::pretty, 40));
        m.put("ELDER_GUARDIAN", 3); m.put("WARDEN", 1);
        List<String> l = Trophies.lines(m, Trophies::pretty, 40);
        assertEquals(2, l.size());
        assertEquals("Slain: Ender Dragon, Wither x2,", l.get(0));
        assertEquals("  Elder Guardian x3, Warden", l.get(1));
        assertTrue(l.get(0).length() <= 40);
    }

    @Test void loreLines() {
        Map<String, Integer> t = new LinkedHashMap<>(); t.put("WITHER", 1);
        assertEquals(List.of("Crafted by Builder", "Kills: 12", "Slain: Wither"),
                LoreModel.lines("Builder", true, false, 12, 0, 0, t, true, Trophies::pretty, 40));
        assertEquals(List.of("Kills: 13 (Players: 1)"), LoreModel.lines(null, true, false, 12, 1, 0, Map.of(), true, Trophies::pretty, 40));
        assertEquals(List.of("Crafted by Builder", "Hits blocked: 34"), LoreModel.lines("Builder", false, true, 0, 0, 34, Map.of(), true, Trophies::pretty, 40));
        assertEquals(List.of("Crafted by Builder"), LoreModel.lines("Builder", true, false, 12, 1, 0, t, false, Trophies::pretty, 40), "stats hidden");
        assertEquals(List.of(), LoreModel.lines(null, true, false, 0, 0, 0, Map.of(), true, Trophies::pretty, 40));
        assertTrue(LoreModel.looksOurs("Crafted by Mike"));
        assertTrue(LoreModel.looksOurs("Kills: 4 (Players: 2)"));
        assertTrue(LoreModel.looksOurs("Hits blocked: 9"));
        assertTrue(LoreModel.looksOurs("Slain: Wither"));
        assertFalse(LoreModel.looksOurs("A sharp blade"));
        assertFalse(LoreModel.looksOurs("Kills: lots"));
    }
}
