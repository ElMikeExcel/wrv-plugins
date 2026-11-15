package com.winkreyes.excelmenu.core.registry;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 3.0 prefs codec (DESIGN §3.8/§7 — the core side of PrefsStoreTest). */
public class PrefsCodecTest {

    @Test
    public void roundTripFullRecord() {
        PlayerMenuPrefs prefs = new PlayerMenuPrefs(1, "server_menu",
                List.of("me", "travel"),
                Map.of("travel", List.of("party", "guild"),
                        "me", List.of("summons", "skills")));
        PrefsCodec.Result r = PrefsCodec.parse(PrefsCodec.toYaml(prefs));
        assertTrue(String.valueOf(r.error()), r.ok());
        assertEquals(prefs, r.prefs());
    }

    @Test
    public void roundTripPartialRecord() {
        PlayerMenuPrefs prefs = new PlayerMenuPrefs(1, "server_menu",
                List.of(), Map.of("me", List.of("trash", "skills")));
        PrefsCodec.Result r = PrefsCodec.parse(PrefsCodec.toYaml(prefs));
        assertTrue(r.ok());
        assertEquals(prefs, r.prefs());
    }

    @Test
    public void roundTripEmptyRecord() {
        PlayerMenuPrefs prefs = PlayerMenuPrefs.empty("server_menu");
        PrefsCodec.Result r = PrefsCodec.parse(PrefsCodec.toYaml(prefs));
        assertTrue(r.ok());
        assertEquals(prefs.menu(), r.prefs().menu());
        assertTrue(r.prefs().isEmpty());
    }

    @Test
    public void corruptTextIsNoPrefsWithError() {
        PrefsCodec.Result r = PrefsCodec.parse("version: [unclosed\n  bad: : :");
        assertFalse(r.ok());
        assertTrue(r.error() != null && !r.error().isBlank());
    }

    @Test
    public void wrongTypeIsNoPrefs() {
        assertFalse(PrefsCodec.parse("version: 1\nmenu: server_menu\ngroups: 42\n").ok());
        assertFalse(PrefsCodec.parse("version: 1\nmenu: server_menu\ngroup-order: me\n").ok());
    }

    @Test
    public void futureVersionIsNoPrefs() {
        PrefsCodec.Result r = PrefsCodec.parse("version: 99\nmenu: server_menu\n");
        assertFalse(r.ok());
        assertTrue(r.error().contains("99"));
    }

    @Test
    public void unknownEntryIdsSurviveVerbatim() {
        // Dropping stale ids is the merge's job, not the codec's.
        PlayerMenuPrefs prefs = new PlayerMenuPrefs(1, "server_menu",
                List.of(), Map.of("me", List.of("long_gone_door", "skills")));
        PrefsCodec.Result r = PrefsCodec.parse(PrefsCodec.toYaml(prefs));
        assertTrue(r.ok());
        assertEquals(List.of("long_gone_door", "skills"), r.prefs().groupList("me"));
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
