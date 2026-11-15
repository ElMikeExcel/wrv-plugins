package com.winkreyes.excelmenu.core.render;

import com.winkreyes.excelmenu.core.platform.Platform;
import com.winkreyes.excelmenu.core.registry.EntryDef;
import com.winkreyes.excelmenu.core.registry.EntryRegistry;
import com.winkreyes.excelmenu.core.registry.EntryResolver;
import com.winkreyes.excelmenu.core.registry.RegistryConfig;
import com.winkreyes.excelmenu.core.registry.ResolvedEntry;
import com.winkreyes.excelmenu.core.registry.ViewerContext;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertTrue;

/** Shared fixtures for the renderer suites: the PRODUCTION entries
 *  and layouts, resolved for a fully-green viewer. */
final class RenderFixtures {

    static final RegistryConfig CONFIG = new RegistryConfig(
            RegistryConfig.SERVER_ZONE,
            "&eThat isn't available right now.",
            Map.of(
                    "server_menu", List.of("travel", "me", "shops", "help", "settings"),
                    "commands_menu", List.of("getting_around", "money_jobs",
                            "skills_levels", "shops_trading", "claims_land",
                            "guild_quests", "server_info", "fixes_help",
                            "your_items_help")),
            Map.of("travel", "&1", "me", "&5", "shops", "&2", "help", "&n",
                    "settings", "&8"));

    static final Clock IN_WINDOW = Clock.fixed(
            Instant.parse("2026-11-01T18:00:00Z"), ZoneOffset.UTC);
    static final Clock OUT_WINDOW = Clock.fixed(
            Instant.parse("2026-11-07T18:00:00Z"), ZoneOffset.UTC);

    private RenderFixtures() {}

    static Path entriesDir() {
        return Path.of(System.getProperty("harness.entries",
                "src/main/resources/entries"));
    }

    static Path layoutsDir() {
        return Path.of(System.getProperty("harness.layouts",
                "src/main/resources/layouts"));
    }

    static Map<String, EntryDef> allEntries() {
        EntryRegistry registry = new EntryRegistry();
        EntryRegistry.LoadOutcome outcome = registry.loadDirectory(entriesDir());
        assertTrue("production entries must all load: " + outcome.failed(),
                outcome.failed().isEmpty());
        return outcome.loaded();
    }

    static LayoutDef layout(String id) throws Exception {
        LayoutLoader.Result r = LayoutLoader.load(layoutsDir().resolve(id + ".yml"));
        assertTrue("layout " + id + " must load: " + r.errors(), r.ok());
        return r.def();
    }

    /** A viewer with every guard green and every toggle ON. */
    static ViewerContext viewer(Platform platform, Set<String> absentPlugins) {
        return new ViewerContext() {
            @Override public Platform platform() { return platform; }
            @Override public boolean hasPermission(String p) { return true; }
            @Override public boolean pluginPresent(String name) {
                return !absentPlugins.contains(name);
            }
            @Override public String resolvePlaceholder(String token) {
                return switch (token) {
                    case "%guildgate_unlocked%" -> "true";
                    case "%menujoin_status%" -> "ON";
                    case "%silkwarnings_status%" -> "ON";
                    case "%aimassist_status%" -> "ON";
                    default -> null;
                };
            }
        };
    }

    static List<ResolvedEntry> resolve(String menu, Platform platform, Clock clock) {
        return EntryResolver.resolve(menu, allEntries().values(),
                viewer(platform, Set.of()), clock, CONFIG);
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
