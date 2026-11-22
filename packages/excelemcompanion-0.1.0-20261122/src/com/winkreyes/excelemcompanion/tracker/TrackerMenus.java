package com.winkreyes.excelemcompanion.tracker;

import com.winkreyes.excelemcompanion.shared.TextUtil;
import com.winkreyes.excelemcompanion.shared.Views;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Progress-tracker menus: a player's OWN progress only (v1).
 * Panels: Quests, Dungeons, Bosses, Combat, Classes & Skills.
 *
 * Labels stay honest about the data (D186/D187): EliteMobs
 * completions and runs are total counters (repeats count) and are
 * labeled as such; distinct progress comes from the quest-cooldown
 * registry and the boss-lockout list and is labeled as such;
 * per-boss and per-dungeon counts come from ExcelProtect records,
 * colored by the expertise scale, with downed/not-downed shown
 * wherever a reliable count cannot be sourced.
 */
public final class TrackerMenus {

    private final TrackerCatalog catalog;

    public TrackerMenus(TrackerCatalog catalog) {
        this.catalog = catalog;
    }

    public TrackerCatalog catalog() {
        return catalog;
    }

    // ------------------------------------------------------------ figures

    public int distinctCount(TrackerSnapshot snap) {
        int n = 0;
        for (String file : snap.distinctQuests()) {
            if (catalog.quest(file) != null) n++;
        }
        return n;
    }

    public int mainChainDone(TrackerSnapshot snap) {
        int n = 0;
        for (String file : catalog.mainChain()) {
            if (snap.distinctQuests().contains(file)) n++;
        }
        return n;
    }

    /** "Main quest 27" style position of the player's current main
     *  quest, or null when they are not on the chain. */
    public String currentMainQuestLine(TrackerSnapshot snap) {
        for (String file : snap.activeQuestFiles()) {
            int idx = catalog.mainChain().indexOf(file);
            if (idx >= 0) {
                TrackerCatalog.QuestInfo info = catalog.quest(file);
                String name = info != null
                        ? TextUtil.plain(info.name()) : file;
                String pos = idx == 0 ? "the prologue"
                        : "Main quest " + idx;
                return "Now on " + pos + ": " + name;
            }
        }
        return null;
    }

    public int dungeonsDowned(TrackerSnapshot snap) {
        int n = 0;
        for (TrackerCatalog.DungeonInfo d : catalog.dungeons()) {
            if (isDowned(snap, d)) n++;
        }
        return n;
    }

    public boolean isDowned(TrackerSnapshot snap,
                            TrackerCatalog.DungeonInfo dungeon) {
        for (String file : dungeon.bossFiles()) {
            if (snap.lockoutBossFiles().contains(file)) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- hub

    public Views.View hubView(TrackerSnapshot snap) {
        List<Views.Button> buttons = new ArrayList<>();
        if (!snap.available()) {
            buttons.add(Views.Button.content("core:noop",
                    "&eProgress unavailable", "PAPER",
                    List.of("&7EliteMobs data could not be read.",
                            "&7Try again in a moment.")));
            buttons.add(Views.Button.nav("core:close", "&fClose", "BARRIER"));
            return new Views.View("My Progress", buttons);
        }
        buttons.add(Views.Button.content("tracker:quests", "&fQuests", "BOOK",
                List.of("&7Completions: &f" + fmt(snap.questCompletions()),
                        "&7Distinct: &f" + distinctCount(snap)
                                + " of " + catalog.questTotal(),
                        "&7Main chain: &f" + mainChainDone(snap)
                                + " of " + catalog.mainChain().size())));
        buttons.add(Views.Button.content("tracker:dungeons", "&fDungeons",
                "COMPASS",
                List.of("&7Runs: &f" + fmt(snap.dungeonRuns()),
                        "&7Dungeons downed: &f" + dungeonsDowned(snap)
                                + " of " + catalog.dungeons().size())));
        buttons.add(Views.Button.content("tracker:bosses", "&fBosses",
                "WITHER_SKELETON_SKULL",
                List.of("&7Per-boss kill counts, colored",
                        "&7by expertise — see the legend",
                        "&7inside.")));
        buttons.add(Views.Button.content("tracker:combat", "&fCombat",
                "IRON_SWORD",
                List.of("&7Elite kills: &f" + fmt(snap.kills()),
                        "&7Highest elite killed: &fLevel "
                                + snap.highestKilled(),
                        "&7Deaths: &f" + fmt(snap.deaths()))));
        List<String> classLore = new ArrayList<>();
        for (TrackerSnapshot.FormProgress form : snap.forms()) {
            if (form.selected()) {
                classLore.add("&7Class: &f" + prettyId(form.formId())
                        + " &a(current)");
            }
        }
        classLore.add("&7Forms: &f" + snap.forms().size()
                + " &7— Skills: &f" + snap.skills().size());
        buttons.add(Views.Button.content("tracker:classes",
                "&fClasses & Skills", "EXPERIENCE_BOTTLE", classLore));
        buttons.add(Views.Button.nav("core:close", "&fClose", "BARRIER"));
        return new Views.View("My Progress", buttons,
                "Your own progress record:");
    }

    // -------------------------------------------------------------- quests

    public Views.View questsView(TrackerSnapshot snap) {
        List<Views.Button> buttons = new ArrayList<>();
        buttons.add(Views.Button.content("core:noop", "&fQuest Completions",
                "PAPER",
                List.of("&f" + fmt(snap.questCompletions())
                                + " &7total completions",
                        "&7Repeats count — finishing the",
                        "&7same quest twice counts twice.")));
        buttons.add(Views.Button.content("core:noop", "&fDistinct Quests",
                "PAPER",
                List.of("&f" + distinctCount(snap) + " of "
                                + catalog.questTotal()
                                + " &7different quests finished",
                        "&7Counted from your quest",
                        "&7cooldown record.")));
        List<String> chainLore = new ArrayList<>();
        chainLore.add("&f" + mainChainDone(snap) + " of "
                + catalog.mainChain().size() + " &7chain quests finished");
        String current = currentMainQuestLine(snap);
        if (current != null) chainLore.add("&7" + current);
        buttons.add(Views.Button.content("core:noop", "&fMain Chain",
                "PAPER", chainLore));
        buttons.add(Views.Button.content("tracker:quests-done:0",
                "&aCompleted Quests", "WRITTEN_BOOK",
                List.of("&7" + distinctCount(snap) + " quests")));
        int remaining = catalog.questTotal() - distinctCount(snap);
        buttons.add(Views.Button.content("tracker:quests-open:0",
                "&fQuests Remaining", "BOOK",
                List.of("&7" + remaining + " quests")));
        buttons.add(Views.Button.nav("core:back", "&fBack", "ARROW"));
        return new Views.View("Quests", buttons, "Your quest record:");
    }

    public Views.View questListView(TrackerSnapshot snap, boolean done,
                                    int page, int pageSize) {
        List<TrackerCatalog.QuestInfo> all = new ArrayList<>();
        for (String file : sortedQuestFiles()) {
            boolean isDone = snap.distinctQuests().contains(file);
            if (isDone == done) all.add(catalog.quest(file));
        }
        int pages = pageCount(all.size(), pageSize);
        page = clampPage(page, pages);
        List<Views.Button> buttons = new ArrayList<>();
        for (TrackerCatalog.QuestInfo q : slice(all, page, pageSize)) {
            buttons.add(Views.Button.content("core:noop", q.name(), "PAPER",
                    done ? List.of("&aFinished") : List.of("&7Not finished yet")));
        }
        if (buttons.isEmpty()) {
            buttons.add(Views.Button.content("core:noop", "&7Nothing here",
                    "PAPER", List.of()));
        }
        buttons.add(Views.Button.nav("core:back", "&fBack", "ARROW"));
        String kind = done ? "quests-done" : "quests-open";
        if (page > 0) buttons.add(Views.Button.nav(
                "tracker:" + kind + ":" + (page - 1), "&fPrevious Page",
                "ARROW"));
        if (page < pages - 1) buttons.add(Views.Button.nav(
                "tracker:" + kind + ":" + (page + 1), "&fNext Page",
                "ARROW"));
        return new Views.View(done ? "Completed Quests" : "Quests Remaining",
                buttons);
    }

    private List<String> sortedQuestFiles() {
        List<TrackerCatalog.QuestInfo> all = new ArrayList<>();
        for (String file : catalog.questFiles()) {
            all.add(catalog.quest(file));
        }
        all.sort(Comparator.comparing(
                q -> TextUtil.plain(q.name()).toLowerCase(Locale.ROOT)));
        List<String> files = new ArrayList<>();
        for (TrackerCatalog.QuestInfo q : all) files.add(q.file());
        return files;
    }

    // ------------------------------------------------------------ dungeons

    public Views.View dungeonsView(TrackerSnapshot snap,
                                   Map<String, Integer> kills) {
        List<Views.Button> buttons = new ArrayList<>();
        buttons.add(Views.Button.content("core:noop", "&fDungeon Runs",
                "PAPER",
                List.of("&f" + fmt(snap.dungeonRuns()) + " &7total runs",
                        "&7Repeats count — every run adds",
                        "&7to the counter.")));
        for (TrackerCatalog.DungeonInfo d : catalog.dungeons()) {
            List<String> lore = new ArrayList<>();
            int clears = ProtectKills.dungeonClears(kills, d);
            if (kills == null) {
                lore.add("&8Counting server records…");
            } else if (clears > 0) {
                lore.add(Expertise.countLine("Clears", clears));
                lore.add(Expertise.tierLine(clears));
            } else if (isDowned(snap, d)) {
                lore.add("&aDowned ✓");
                lore.add("&8Clears not in server records");
            } else {
                lore.add("&8Not cleared yet");
            }
            if (!d.objectives().isEmpty()) {
                lore.add("&7Final boss: "
                        + TextUtil.plain(String.join(", ", d.objectives())));
            }
            buttons.add(Views.Button.content("core:noop", d.name(),
                    "ENDER_EYE", lore));
        }
        buttons.add(Expertise.legendButton());
        buttons.add(Views.Button.nav("core:back", "&fBack", "ARROW"));
        return new Views.View("Dungeons", buttons, "Your dungeon record:");
    }

    // -------------------------------------------------------------- bosses

    public Views.View bossPlacesView(int page, int pageSize) {
        List<TrackerCatalog.PlaceInfo> all = catalog.places();
        int pages = pageCount(all.size(), pageSize);
        page = clampPage(page, pages);
        List<Views.Button> buttons = new ArrayList<>();
        for (TrackerCatalog.PlaceInfo p : slice(all, page, pageSize)) {
            buttons.add(Views.Button.content(
                    "tracker:boss-place:" + p.id() + ":0", p.name(), "MAP",
                    List.of("&7" + p.bosses().size()
                            + (p.bosses().size() == 1 ? " boss" : " bosses"),
                            "&8Click to see your kill counts")));
        }
        buttons.add(Expertise.legendButton());
        buttons.add(Views.Button.nav("core:back", "&fBack", "ARROW"));
        if (page > 0) buttons.add(Views.Button.nav(
                "tracker:bosses:" + (page - 1), "&fPrevious Page", "ARROW"));
        if (page < pages - 1) buttons.add(Views.Button.nav(
                "tracker:bosses:" + (page + 1), "&fNext Page", "ARROW"));
        return new Views.View("Bosses — Places", buttons,
                "Pick a place to see your per-boss kill counts:");
    }

    public Views.View bossPlaceView(String placeId, TrackerSnapshot snap,
                                    Map<String, Integer> kills, int page,
                                    int pageSize) {
        TrackerCatalog.PlaceInfo place = catalog.placeById(placeId);
        if (place == null) return bossPlacesView(0, pageSize);
        List<TrackerCatalog.BossInfo> all = place.bosses();
        int pages = pageCount(all.size(), pageSize);
        page = clampPage(page, pages);
        List<Views.Button> buttons = new ArrayList<>();
        for (TrackerCatalog.BossInfo boss : slice(all, page, pageSize)) {
            List<String> lore = new ArrayList<>();
            if (!boss.level().isBlank()) {
                lore.add("&7Level " + boss.level() + " elite");
            }
            int count = ProtectKills.countFor(kills, catalog, place, boss);
            if (kills == null) {
                lore.add("&8Counting server records…");
            } else if (count > 0) {
                lore.add(Expertise.countLine("Kills", count));
                lore.add(Expertise.tierLine(count));
            } else if (!boss.file().isBlank()
                    && snap.lockoutBossFiles().contains(boss.file())) {
                lore.add("&aDowned ✓");
                lore.add("&8No kill count in server records");
            } else {
                lore.add("&8No recorded kills yet");
            }
            buttons.add(Views.Button.content("core:noop", boss.name(),
                    "WITHER_SKELETON_SKULL", lore));
        }
        buttons.add(Expertise.legendButton());
        buttons.add(Views.Button.nav("core:back", "&fBack", "ARROW"));
        if (page > 0) buttons.add(Views.Button.nav(
                "tracker:boss-place:" + place.id() + ":" + (page - 1),
                "&fPrevious Page", "ARROW"));
        if (page < pages - 1) buttons.add(Views.Button.nav(
                "tracker:boss-place:" + place.id() + ":" + (page + 1),
                "&fNext Page", "ARROW"));
        return new Views.View(TextUtil.plain(place.name()), buttons,
                "Your kill counts here:");
    }

    // -------------------------------------------------------------- combat

    public Views.View combatView(TrackerSnapshot snap) {
        List<Views.Button> buttons = new ArrayList<>();
        buttons.add(Views.Button.content("core:noop", "&fElite Kills",
                "IRON_SWORD",
                List.of("&f" + fmt(snap.kills()),
                        "&7EliteMobs' own lifetime counter.")));
        buttons.add(Views.Button.content("core:noop",
                "&fHighest Elite Killed", "GOLDEN_SWORD",
                List.of("&fLevel " + snap.highestKilled())));
        buttons.add(Views.Button.content("core:noop", "&fDeaths",
                "SKELETON_SKULL", List.of("&f" + fmt(snap.deaths()))));
        buttons.add(Views.Button.content("core:noop", "&fEliteMobs Level",
                "EXPERIENCE_BOTTLE", List.of("&f" + snap.playerLevel())));
        buttons.add(Views.Button.nav("core:back", "&fBack", "ARROW"));
        return new Views.View("Combat", buttons, "Your combat record:");
    }

    // ------------------------------------------------------------- classes

    public Views.View classesView(TrackerSnapshot snap) {
        List<Views.Button> buttons = new ArrayList<>();
        if (snap.forms().isEmpty()) {
            buttons.add(Views.Button.content("core:noop",
                    "&7No class progress yet", "PAPER", List.of()));
        }
        for (TrackerSnapshot.FormProgress form : snap.forms()) {
            List<String> lore = new ArrayList<>();
            lore.add("&7XP: &f" + fmt(form.xp()));
            lore.add("&7Level: &f" + form.level());
            if (form.selected()) lore.add("&aYour current class");
            buttons.add(Views.Button.content("core:noop",
                    "&f" + prettyId(form.formId()), "BOOK", lore));
        }
        if (!snap.skills().isEmpty()) {
            buttons.add(Views.Button.content("core:noop", "&6Skill XP",
                    "NETHER_STAR", List.of("&7Your top skills:")));
            int shown = 0;
            for (TrackerSnapshot.SkillProgress skill : snap.skills()) {
                if (shown++ >= 11) break;
                buttons.add(Views.Button.content("core:noop",
                        "&f" + prettyId(skill.type()), "PAPER",
                        List.of("&7XP: &f" + fmt(skill.xp()),
                                "&7Skill level: &f" + skill.level())));
            }
        }
        buttons.add(Views.Button.nav("core:back", "&fBack", "ARROW"));
        return new Views.View("Classes & Skills", buttons,
                "Your classes and skills:");
    }

    // ------------------------------------------------------------ helpers

    static String prettyId(String id) {
        if (id == null || id.isBlank()) return "";
        String[] parts = id.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1));
        }
        return sb.toString();
    }

    static String fmt(long n) {
        return NumberFormat.getInstance(Locale.US).format(n);
    }

    public static int pageCount(int size, int pageSize) {
        if (pageSize <= 0) pageSize = 45;
        return Math.max(1, (size + pageSize - 1) / pageSize);
    }

    static int clampPage(int page, int pages) {
        return Math.max(0, Math.min(page, pages - 1));
    }

    static <T> List<T> slice(List<T> all, int page, int pageSize) {
        if (pageSize <= 0) pageSize = 45;
        int from = Math.min(page * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());
        return all.subList(from, to);
    }
}
