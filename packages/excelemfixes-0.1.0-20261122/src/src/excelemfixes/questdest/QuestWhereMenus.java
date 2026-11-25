package excelemfixes.questdest;

import excelemfixes.shared.TextUtil;
import excelemfixes.shared.Views;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the /questwhere screens as shared Views (the renderer
 * turns them into a Java chest GUI or a Bedrock Cumulus form with
 * identical content). Pure view building: no Bukkit state, so the
 * plain-JVM suites exercise these directly.
 */
public final class QuestWhereMenus {

    private final QuestDestinations destinations;

    public QuestWhereMenus(QuestDestinations destinations) {
        this.destinations = destinations;
    }

    public QuestDestinations destinations() {
        return destinations;
    }

    /**
     * The root screen: one entry per tracker-blank quest the
     * player currently has, or the all-clear screen when none of
     * their active quests needs us.
     */
    public Views.View rootView(List<QuestDestinations.ActiveQuest> active,
                               boolean emAvailable) {
        List<Views.Button> buttons = new ArrayList<>();
        if (!emAvailable) {
            buttons.add(Views.Button.content("core:noop",
                    "&eEliteMobs data is unavailable",
                    "BARRIER",
                    List.of("&7Quest data could not be read just now.",
                            "&7Try again in a moment.")));
            buttons.add(Views.Button.nav("core:close", "&fClose", "BARRIER"));
            return new Views.View("Quest Destinations", buttons,
                    "Quest data could not be read just now. Try again in a moment.");
        }
        List<String> activeFiles = new ArrayList<>();
        for (QuestDestinations.ActiveQuest q : active) {
            activeFiles.add(q.file());
        }
        List<QuestDestinations.Destination> fixed =
                destinations.forActive(activeFiles);
        if (fixed.isEmpty()) {
            List<String> lore = new ArrayList<>();
            lore.add("&7None of your current quests are missing");
            lore.add("&7a destination in the EliteMobs tracker.");
            if (!active.isEmpty()) {
                lore.add("");
                lore.add("&7Your active quests:");
                int shown = 0;
                for (QuestDestinations.ActiveQuest q : active) {
                    if (shown++ >= 5) {
                        lore.add("&7...and more");
                        break;
                    }
                    String name = TextUtil.plain(q.name());
                    lore.add("&f- " + (name.isBlank() ? q.file() : name));
                }
            } else {
                lore.add("&7You have no active quests right now.");
            }
            buttons.add(Views.Button.content("core:noop",
                    "&aYou're all set", "COMPASS", lore));
            buttons.add(Views.Button.nav("core:close", "&fClose", "BARRIER"));
            return new Views.View("Quest Destinations", buttons,
                    "None of your current quests are missing a destination "
                            + "in the EliteMobs tracker.");
        }
        for (QuestDestinations.Destination d : fixed) {
            buttons.add(Views.Button.content(
                    "questwhere:quest:" + d.questFile(),
                    "&a" + d.questName(),
                    d.icon(),
                    List.of(
                            "&7EliteMobs' tracker shows no destination",
                            "&7for this quest. We have it:",
                            "&fTarget: &7" + d.target(),
                            "&fPlace: &7" + d.place(),
                            "&fEntry: &7" + d.entry() + " — " + d.coords(),
                            "&7Tap for directions and guidance.")));
        }
        buttons.add(Views.Button.nav("core:close", "&fClose", "BARRIER"));
        return new Views.View("Quest Destinations", buttons,
                "EliteMobs' tracker shows no destination for these quests. "
                        + "Tap one for directions and guidance.");
    }

    /**
     * One quest's screen: the summary with its directions behind
     * one tap, the glow-guidance button (when glow is enabled),
     * and Back. This is also the screen another plugin's
     * progress entry can deep-link to via /questwhere show.
     */
    public Views.View questView(QuestDestinations.Destination d,
                                boolean glowEnabled, int glowSeconds) {
        List<Views.Button> buttons = new ArrayList<>();
        Views.Button directions = new Views.Button(
                "core:noop",
                "&fDirections",
                "COMPASS",
                List.of(
                        "&fTarget: &7" + d.target(),
                        "&fPlace: &7" + d.place(),
                        "&fEntry: &7" + d.entry(),
                        "&fCoordinates: &7" + d.coords()
                                + " (" + d.worldLabel() + ")",
                        "&7Tap for step-by-step directions."),
                detailFor(d),
                false);
        buttons.add(directions);
        if (glowEnabled) {
            buttons.add(Views.Button.content(
                    "questwhere:guide:" + d.questFile(),
                    "&eGuide me there",
                    "GLOWSTONE_DUST",
                    List.of(
                            "&7Lights the destination with a glow",
                            "&7for " + glowSeconds + " seconds, so you",
                            "&7can spot it through the world.",
                            "&7Guidance only — no teleport.")));
        }
        buttons.add(Views.Button.nav("core:back", "&fBack", "ARROW"));
        return new Views.View(d.questName(), buttons,
                d.target() + " — " + d.place() + ". Entry: " + d.entry()
                        + " at " + d.coords() + " (" + d.worldLabel() + ").");
    }

    /** The directions screen for one destination. */
    public Views.Detail detailFor(QuestDestinations.Destination d) {
        List<String> lines = new ArrayList<>();
        lines.add("&a" + d.questName());
        lines.add("&7Target: &f" + d.target());
        lines.add("&7Place: &f" + d.place());
        lines.add("&7Entry point: &f" + d.entry());
        lines.add("&7Coordinates: &f" + d.coords()
                + " &7(" + d.worldLabel() + ")");
        lines.add("");
        for (String step : d.directions()) {
            lines.add("&f" + step);
        }
        lines.add("");
        lines.add("&8This is information only — getting there is the");
        lines.add("&8usual walking and EliteMobs wormholes.");
        return new Views.Detail(d.questName(), d.icon(), lines);
    }
}
