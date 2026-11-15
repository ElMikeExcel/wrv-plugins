package com.winkreyes.excelmenu.harness;

import com.winkreyes.excelmenu.core.platform.Platform;
import com.winkreyes.excelmenu.core.registry.EntryDef;
import com.winkreyes.excelmenu.core.registry.EntryRegistry;
import com.winkreyes.excelmenu.core.registry.EntryResolver;
import com.winkreyes.excelmenu.core.registry.RegistryConfig;
import com.winkreyes.excelmenu.core.registry.ResolvedEntry;
import com.winkreyes.excelmenu.core.registry.ViewerContext;
import org.junit.Test;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * THE GOLDEN-RENDER GATE (DESIGN-2.0 §4): three columns over the
 * viewer-state matrix — the OLD Java visible set (from the live
 * server_menu.yml, computed by an independent model), the OLD Bedrock
 * visible set (from 1.5.28's BUTTONS/COMMANDS, machine-extracted —
 * the live predecessor, re-based at Phase 2; 1.5.29's arrays are
 * byte-identical to 1.5.28's, so the column covers both),
 * and 2.0's resolved set from the converted entry fixtures.
 *
 * The gate is exact: visible sets (id, label, order, dispatch) must
 * match the platform-appropriate old column, EXCEPT for differences
 * classified into the named acceptance table below. An unclassified
 * difference fails. An acceptance that never occurs also fails (no
 * stale acceptances). The accepted deltas:
 *
 *   D1 Bedrock autojoin label: 2.0 shows the ON/OFF state text Java
 *      always had; 1.5.27 showed static text. (Bedrock gains state.)
 *   D2 Java unavailable-toggle tap: 0.1.8's unavailable trio item had
 *      no action (tap = nothing); 2.0's guard answers with the
 *      standard message — which is what 1.5.27 already did on Bedrock.
 *   D3 Bedrock halloween presence, in-window: the recorded parity gap
 *      (Java-only button) closing by construction (DESIGN-2.0 §3.1).
 *      Since the Phase 2 fixture re-base (the old Bedrock column is
 *      1.5.28, the live predecessor), this covers 'halloween' only —
 *      1.5.28 itself shipped the 'halloween_scares' button, so that
 *      half of the gap closed before the unification.
 *   D4 Halloween absence, out-of-window: the dated mechanism
 *      retiring the entries at render time; the old system had no
 *      dates and would keep showing them until a manual removal
 *      pass. Covers 'halloween' + 'halloween_scares' on Java, and
 *      'halloween_scares' on Bedrock (the only seasonal row the
 *      1.5.28 column carries).
 *   D5 Bedrock makersmark label: one label for both platforms —
 *      "Maker's Mark settings" (the Java wording) replaces
 *      "Maker's mark and Gear stats"; same door, same feature.
 *   D6 Bedrock skills/makersmark dispatch: bespoke form/screen in
 *      the old column; the entry's dispatch key stays the
 *      menu-definition dispatch, which is now the FALLBACK — since
 *      the Phase 2 screen port (§2.4) the form layer opens the
 *      ported screen (Skills & Perks form / the owner's /mark form)
 *      before the entry's actions run. The Java column matches
 *      exactly, as before.
 *   D7 Bedrock ask dispatch: text-box screen in the old column; the
 *      entry's dispatch key stays the [message] hint (the Java
 *      behaviour), which is now the FALLBACK — since the Phase 2
 *      screen port the form layer opens the Ask text box first.
 *      Both reach /ask.
 *   D8 Bedrock commands dispatch: /servercommands (BedrockMenu's own
 *      catalogue) today; the 2.0 entry opens commands_menu. They
 *      converge when the catalogue converts to registry entries
 *      (Amendment B, Phase 1).
 *   D9 Autojoin on unknown state: Java-old showed NOTHING at the slot
 *      (both pair items' requirements fail); 2.0's guard contract
 *      shows the default variant with the standard message, on both
 *      platforms — a guarded door explains itself instead of
 *      vanishing (the ExcelMenu founding lesson).
 *   D10 RETIRED in 3.0 (DESIGN §5): the magiclevel entry it
 *      classified was retired, so the class and its acceptance
 *      were removed — the harness's no-stale-acceptances rule
 *      required it the moment the row ceased to exist.
 *   D10 (historical) Live-data fold-in (Nov 1, 2026; bundled at the Phase 2
 *      baseline, Nov 2, 2026): 'magiclevel' — placed into the live
 *      registry as a data file on Nov 1, 2026 with the ExcelMagicLevel
 *      ship; absent from both old columns because both predate the
 *      placement. Guarded by requires-plugin ExcelMagicLevel; the
 *      harness viewer lacks that plugin, so the row resolves to its
 *      default variant on both platforms in every state.
 *   D11 Scares-door repair (Phase 2, Nov 2, 2026): on Java
 *      in-window, 'halloween_scares' changes dispatch from
 *      [openguimenu] halloween_scares — a definition that lived
 *      only in the live data folder and was renamed aside at the
 *      Phase 1 install, so the door opened nothing — to
 *      [player] scares, the owner's own command (the screen
 *      BedrockMenu's guarded dispatch opened; on Bedrock the keys
 *      now match exactly, no delta). The entry also gained the
 *      requires-plugin guard its Phase 1 conversion omitted; the
 *      harness viewer models the owner plugin present, as the live
 *      server has it through the season.
 */
public class GoldenHarnessTest {

    private static final RegistryConfig CONFIG = new RegistryConfig(
            RegistryConfig.SERVER_ZONE,
            "&eThat isn't available right now.",
            Map.of("server_menu", List.of("travel", "me", "shops", "help", "settings")),
            Map.of("travel", "&1", "me", "&5", "shops", "&2", "help", "&n",
                    "settings", "&8"));

    private static final Set<String> REQUIRED_DELTAS =
            Set.of("D1", "D2", "D3", "D4", "D5", "D6", "D7", "D8", "D9", "D11"); // D10 retired in 3.0 with magiclevel (DESIGN §5)

    private final Set<String> occurredDeltas = new HashSet<>();
    private final List<String> failures = new ArrayList<>();
    private int statesCompared = 0;
    private int rowsCompared = 0;

    @Test
    public void threeWayGoldenMatrix() throws Exception {
        Path fixtures = Path.of(System.getProperty("harness.fixtures", "fixtures"));
        Path entriesDir = Path.of(System.getProperty(
                "harness.entries", "src/main/resources/entries"));
        OldJavaModel oldJava = new OldJavaModel(fixtures.resolve("server_menu.yml"));
        OldBedrockModel oldBedrock = new OldBedrockModel(
                fixtures.resolve("bedrock-arrays.json"));

        EntryRegistry registry = new EntryRegistry();
        EntryRegistry.LoadOutcome outcome = registry.loadDirectory(entriesDir);
        assertTrue("fixture entries must all load: " + outcome.failed(),
                outcome.failed().isEmpty());
        Map<String, EntryDef> entries = outcome.loaded();

        String[] toggles = {"ON", "OFF", "UNAVAILABLE"};
        String[] autojoinStates = {"ON", "OFF", "UNKNOWN"};
        for (boolean locked : new boolean[]{true, false}) {
            for (String silk : toggles) {
                for (String aim : toggles) {
                    for (String autojoin : autojoinStates) {
                        for (boolean inWindow : new boolean[]{true, false}) {
                            HarnessSupport.ViewerState s = new HarnessSupport.ViewerState(
                                    locked, silk, aim, autojoin, inWindow);
                            compare(Platform.JAVA, s, oldJava.visible(s),
                                    newRows(Platform.JAVA, s, entries));
                            compare(Platform.BEDROCK, s, oldBedrock.visible(s),
                                    newRows(Platform.BEDROCK, s, entries));
                            statesCompared++;
                        }
                    }
                }
            }
        }

        for (String d : REQUIRED_DELTAS) {
            if (!occurredDeltas.contains(d)) {
                failures.add("acceptance " + d + " never occurred — stale acceptance;"
                        + " remove it or fix the build");
            }
        }
        System.out.println("GOLDEN HARNESS: " + statesCompared + " viewer states x2"
                + " platforms, " + rowsCompared + " rows compared, deltas occurred: "
                + occurredDeltas);
        if (!failures.isEmpty()) {
            fail("golden-render gate FAILED (" + failures.size() + "):\n - "
                    + String.join("\n - ", failures));
        }
    }

    // ---------- the 2.0 column ----------

    private List<HarnessSupport.Row> newRows(Platform platform,
                                             HarnessSupport.ViewerState s,
                                             Map<String, EntryDef> entries) {
        ViewerContext viewer = new ViewerContext() {
            @Override public Platform platform() { return platform; }
            @Override public boolean hasPermission(String p) {
                return p.equals("excelsilkwatch.toggle")
                        || p.equals("excelaimassist.toggle");
            }
            @Override public boolean pluginPresent(String name) {
                return Set.of("ExcelGuildGate", "ExcelSilkWatch", "ExcelAimAssist",
                        "ExcelMenuJoin", "ExcelHalloweenToggles").contains(name);
            }
            @Override public String resolvePlaceholder(String token) {
                return switch (token) {
                    case "%guildgate_unlocked%" -> s.guildLocked() ? "false" : "true";
                    case "%menujoin_status%" -> s.autojoin();
                    case "%silkwarnings_status%" -> s.silk();
                    case "%aimassist_status%" -> s.aim();
                    default -> null;
                };
            }
        };
        Clock clock = Clock.fixed(Instant.parse(
                s.inWindow() ? "2026-11-01T18:00:00Z" : "2026-11-07T18:00:00Z"),
                ZoneOffset.UTC);
        List<ResolvedEntry> resolved = EntryResolver.resolve(
                "server_menu", entries.values(), viewer, clock, CONFIG);
        List<HarnessSupport.Row> rows = new ArrayList<>();
        for (ResolvedEntry r : resolved) {
            String label = platform == Platform.BEDROCK
                    ? EntryResolver.bedrockButtonLabel(r, CONFIG) : r.label();
            rows.add(new HarnessSupport.Row(r.id(), label,
                    HarnessSupport.dispatchKey(r.action())));
        }
        return rows;
    }

    // ---------- comparison with classified deltas ----------

    private void compare(Platform platform, HarnessSupport.ViewerState s,
                         List<HarnessSupport.Row> oldRows,
                         List<HarnessSupport.Row> newRows) {
        Map<String, HarnessSupport.Row> oldById = index(oldRows);
        Map<String, HarnessSupport.Row> newById = index(newRows);
        String where = platform + " @ " + s.describe();

        Set<String> presenceNew = new HashSet<>();
        for (String id : newById.keySet()) {
            if (oldById.containsKey(id)) continue;
            String delta = classifyPresenceNew(platform, s, id);
            if (delta == null) {
                failures.add(where + ": entry '" + id + "' appears in 2.0 but not in"
                        + " the old column (unclassified presence)");
            } else {
                occurredDeltas.add(delta);
                presenceNew.add(id);
            }
        }
        Set<String> presenceOld = new HashSet<>();
        for (String id : oldById.keySet()) {
            if (newById.containsKey(id)) continue;
            String delta = classifyPresenceOld(platform, s, id);
            if (delta == null) {
                failures.add(where + ": entry '" + id + "' is in the old column but"
                        + " missing from 2.0 (unclassified absence)");
            } else {
                occurredDeltas.add(delta);
                presenceOld.add(id);
            }
        }

        List<String> oldOrder = oldRows.stream().map(HarnessSupport.Row::id)
                .filter(id -> !presenceOld.contains(id)).toList();
        List<String> newOrder = newRows.stream().map(HarnessSupport.Row::id)
                .filter(id -> !presenceNew.contains(id)).toList();
        if (!oldOrder.equals(newOrder)) {
            failures.add(where + ": ORDER differs\n   old: " + oldOrder
                    + "\n   new: " + newOrder);
        }

        for (String id : oldById.keySet()) {
            HarnessSupport.Row o = oldById.get(id);
            HarnessSupport.Row n = newById.get(id);
            if (n == null) continue;
            rowsCompared++;
            if (!o.label().equals(n.label())) {
                String delta = classifyLabel(platform, s, id);
                if (delta == null) {
                    failures.add(where + ": LABEL differs for '" + id + "'\n   old: "
                            + o.label() + "\n   new: " + n.label());
                } else {
                    occurredDeltas.add(delta);
                }
            }
            if (!o.key().equals(n.key())) {
                String delta = classifyKey(platform, s, id);
                if (delta == null) {
                    failures.add(where + ": DISPATCH differs for '" + id + "'\n   old: "
                            + o.key() + "\n   new: " + n.key());
                } else {
                    occurredDeltas.add(delta);
                }
            }
        }
    }

    private static Map<String, HarnessSupport.Row> index(List<HarnessSupport.Row> rows) {
        Map<String, HarnessSupport.Row> map = new LinkedHashMap<>();
        for (HarnessSupport.Row r : rows) map.put(r.id(), r);
        return map;
    }

    private static String classifyPresenceNew(Platform p, HarnessSupport.ViewerState s,
                                              String id) {
        if (p == Platform.BEDROCK && s.inWindow()
                && id.equals("halloween")) return "D3";
        if (p == Platform.JAVA && s.autojoin().equals("UNKNOWN")
                && id.equals("autojoin")) return "D9";
        return null;
    }

    private static String classifyPresenceOld(Platform p, HarnessSupport.ViewerState s,
                                              String id) {
        if (!s.inWindow() && p == Platform.JAVA
                && (id.equals("halloween") || id.equals("halloween_scares"))) return "D4";
        if (!s.inWindow() && p == Platform.BEDROCK
                && id.equals("halloween_scares")) return "D4";
        return null;
    }

    private static String classifyLabel(Platform p, HarnessSupport.ViewerState s,
                                        String id) {
        if (p == Platform.BEDROCK && id.equals("autojoin")
                && HarnessSupport.isOnOff(s.autojoin())) return "D1";
        if (p == Platform.BEDROCK && id.equals("makersmark_settings")) return "D5";
        return null;
    }

    private static String classifyKey(Platform p, HarnessSupport.ViewerState s,
                                      String id) {
        if (p == Platform.JAVA && id.equals("silkwarnings")
                && s.silk().equals("UNAVAILABLE")) return "D2";
        if (p == Platform.JAVA && id.equals("aimassist")
                && s.aim().equals("UNAVAILABLE")) return "D2";
        if (p == Platform.BEDROCK && id.equals("skills")) return "D6";
        if (p == Platform.BEDROCK && id.equals("makersmark_settings")) return "D6";
        if (p == Platform.BEDROCK && id.equals("ask")) return "D7";
        if (p == Platform.BEDROCK && id.equals("commands")) return "D8";
        if (p == Platform.BEDROCK && id.equals("autojoin")
                && s.autojoin().equals("UNKNOWN")) return "D9";
        if (p == Platform.JAVA && id.equals("halloween_scares")
                && s.inWindow()) return "D11";
        return null;
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
