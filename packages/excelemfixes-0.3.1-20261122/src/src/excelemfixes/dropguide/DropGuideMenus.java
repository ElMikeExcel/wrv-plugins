package excelemfixes.dropguide;

import excelemfixes.shared.TextUtil;
import excelemfixes.shared.Views;

import java.util.ArrayList;
import java.util.List;

/**
 * Drop-guide navigation model (D185): Places -> Mobs -> the mob's
 * FULL drop list. Both renderers (Java chest GUI, Bedrock Cumulus
 * forms) build from these views, so content cannot drift between
 * platforms.
 *
 * No view ever contains a drop chance or percentage for a drop
 * (Mike's ruling). The only numbers shown are levels, counts of
 * entries, and the item's own configured stats. Class-loot drops
 * state that their stats roll when they drop — no fake fixed stats.
 */
public final class DropGuideMenus {

    public static final String QUALITY_LADDER =
            "&8Gray &7-> &fWhite &7-> &aGreen &7-> &9Blue &7-> &5Purple "
                    + "&7-> &6Gold &7-> &bMythic";

    private final DropIndex index;
    private java.util.Set<String> glowMobIds = java.util.Set.of();

    public DropGuideMenus(DropIndex index) {
        this.index = index;
    }

    /** Mob ids the glow guidance can light a way into (the module
     *  fills this from the runtime entry-point scan; empty = no
     *  glow buttons anywhere). */
    public void setGlowMobIds(java.util.Set<String> mobIds) {
        this.glowMobIds = mobIds == null ? java.util.Set.of() : mobIds;
    }

    public DropIndex index() {
        return index;
    }

    // ------------------------------------------------------------- views

    public Views.View placesView(int page, int pageSize) {
        List<DropIndex.Place> all = index.places();
        int pages = pageCount(all.size(), pageSize);
        page = clampPage(page, pages);
        List<Views.Button> buttons = new ArrayList<>();
        for (DropIndex.Place p : slice(all, page, pageSize)) {
            buttons.add(Views.Button.content(
                    "drops:place:" + p.id() + ":0", p.name(), p.icon(),
                    placeLore(p)));
        }
        if (page > 0) buttons.add(Views.Button.nav(
                "drops:places:" + (page - 1), "&fPrevious Page", "ARROW"));
        if (page < pages - 1) buttons.add(Views.Button.nav(
                "drops:places:" + (page + 1), "&fNext Page", "ARROW"));
        buttons.add(Views.Button.nav("core:close", "&fClose", "BARRIER"));
        return new Views.View("Drop Guide — Places", buttons,
                "What does it drop? Pick a dungeon or region:");
    }

    public Views.View mobsView(String placeId, int page, int pageSize) {
        DropIndex.Place place = index.placeById(placeId);
        if (place == null) return placesView(0, pageSize);
        List<DropIndex.Mob> all = place.mobs();
        int pages = pageCount(all.size(), pageSize);
        page = clampPage(page, pages);
        List<Views.Button> buttons = new ArrayList<>();
        for (DropIndex.Mob m : slice(all, page, pageSize)) {
            buttons.add(Views.Button.content(
                    "drops:mob:" + place.id() + ":" + m.id() + ":0",
                    m.name(), m.icon(), mobLore(m)));
        }
        if (all.stream().anyMatch(m -> glowMobIds.contains(m.id()))) {
            buttons.add(Views.Button.content(
                    "drops:glowplace:" + place.id(),
                    "&eGlow the way in", "COMPASS",
                    List.of("&7Lights up the entrance to",
                            "&7this place for a short while.")));
        }
        buttons.add(Views.Button.nav("core:back", "&fBack", "ARROW"));
        if (page > 0) buttons.add(Views.Button.nav(
                "drops:place:" + place.id() + ":" + (page - 1),
                "&fPrevious Page", "ARROW"));
        if (page < pages - 1) buttons.add(Views.Button.nav(
                "drops:place:" + place.id() + ":" + (page + 1),
                "&fNext Page", "ARROW"));
        return new Views.View(TextUtil.plain(place.name()), buttons,
                "Pick a mob or boss to see everything it drops:");
    }

    public Views.View dropsView(String placeId, String mobId, int page,
                                int pageSize) {
        DropIndex.Place place = index.placeById(placeId);
        if (place == null) return placesView(0, pageSize);
        DropIndex.Mob mob = place.mobById(mobId);
        if (mob == null) return mobsView(placeId, 0, pageSize);
        List<DropIndex.Drop> all = mob.drops();
        int pages = pageCount(all.size(), pageSize);
        page = clampPage(page, pages);
        List<Views.Button> buttons = new ArrayList<>();
        int start = page * pageSize;
        List<DropIndex.Drop> slice = slice(all, page, pageSize);
        for (int i = 0; i < slice.size(); i++) {
            DropIndex.Drop d = slice.get(i);
            List<String> lore = dropLore(d);
            List<String> detailLines = new ArrayList<>();
            detailLines.add("&7Dropped by &f" + TextUtil.plain(mob.name()));
            detailLines.add("");
            detailLines.addAll(lore);
            buttons.add(new Views.Button("core:noop", d.name(), d.material(),
                    lore, new Views.Detail(TextUtil.plain(d.name()),
                            d.material(), detailLines), false));
        }
        if (page == 0 && glowMobIds.contains(mob.id())) {
            buttons.add(Views.Button.content(
                    "drops:glowmob:" + place.id() + ":" + mob.id(),
                    "&eGlow the way in", "COMPASS",
                    List.of("&7Lights up where this boss",
                            "&7is found — follow the glow.")));
        }
        if (page == pages - 1 && mob.standardLoot()) {
            List<String> lore = List.of(
                    "&7This boss also drops standard EliteMobs loot —",
                    "&7generated gear and currency for its level.");
            buttons.add(new Views.Button("core:noop", "&6EliteMobs Loot",
                    "GOLD_INGOT", lore,
                    new Views.Detail("EliteMobs Loot", "GOLD_INGOT", lore),
                    false));
        }
        buttons.add(Views.Button.nav("core:back", "&fBack", "ARROW"));
        if (page > 0) buttons.add(Views.Button.nav(
                "drops:mob:" + place.id() + ":" + mob.id() + ":" + (page - 1),
                "&fPrevious Page", "ARROW"));
        if (page < pages - 1) buttons.add(Views.Button.nav(
                "drops:mob:" + place.id() + ":" + mob.id() + ":"
                        + (page + 1), "&fNext Page", "ARROW"));
        return new Views.View(TextUtil.plain(mob.name()), buttons,
                "Everything " + TextUtil.plain(mob.name()) + " drops:");
    }

    // ------------------------------------------------------- lore builders

    public static List<String> placeLore(DropIndex.Place p) {
        List<String> lore = new ArrayList<>();
        String kindLabel = switch (p.kind()) {
            case "dungeon" -> "Dungeon";
            case "arena" -> "Arena";
            case "event" -> "Event content";
            case "zone" -> "Adventure region";
            default -> "Elites";
        };
        lore.add("&7" + kindLabel);
        if (!p.parent().isBlank()) {
            lore.add("&7Part of " + p.parent());
        }
        lore.add("&7" + p.mobs().size() + (p.mobs().size() == 1
                ? " mob & boss" : " mobs & bosses"));
        lore.add("&8Click to see its mobs");
        return lore;
    }

    public static List<String> mobLore(DropIndex.Mob m) {
        List<String> lore = new ArrayList<>();
        StringBuilder line = new StringBuilder("&7");
        if (!m.level().isBlank()) line.append("Level ").append(m.level());
        if (!m.entity().isBlank()) {
            if (!m.level().isBlank()) line.append(" ");
            line.append(prettyEntity(m.entity()));
        }
        if (line.length() > 2) lore.add(line.toString());
        lore.add("&7" + m.drops().size() + (m.drops().size() == 1
                ? " drop" : " drops"));
        lore.add("&8Click to see its drops");
        return lore;
    }

    /**
     * The drop's description. Fixed items show their configured lore
     * and enchantments (exactly what the real item shows). Class-loot
     * items state plainly that their stats roll when they drop.
     */
    public static List<String> dropLore(DropIndex.Drop d) {
        List<String> lore = new ArrayList<>();
        switch (d.kind()) {
            case "classloot" -> {
                lore.add("&7Class loot" + (d.category().isBlank()
                        ? "" : " — " + d.category()));
                lore.addAll(d.lore());
                lore.add("");
                lore.add("&eStats are rolled when this drops.");
                lore.add("&7Its level and enchantments come from");
                lore.add("&7the boss you killed, and its name takes");
                lore.add("&7EliteMobs' quality color from the roll:");
                lore.add(QUALITY_LADDER);
            }
            case "classloot-generic" -> {
                lore.add("&7Class loot — " + prettyRank(d.rank()) + " rank");
                lore.add("");
                lore.add("&eStats are rolled when this drops.");
                lore.add("&7This boss drops generated EliteMobs class");
                lore.add("&7loot: weapons and armor rolled for its");
                lore.add("&7rank when it dies. Quality color from");
                lore.add("&7the roll:");
                lore.add(QUALITY_LADDER);
            }
            case "random" -> {
                lore.add("&7Random loot");
                lore.add("");
                lore.add("&eStats are rolled when this drops.");
                lore.add("&7This boss drops generated random loot,");
                lore.add("&7rolled at its level when it dies.");
            }
            case "quest" -> {
                lore.addAll(d.lore());
                lore.addAll(enchantLines(d));
                lore.add("");
                lore.add("&aQuest item");
            }
            default -> {
                lore.addAll(d.lore());
                lore.addAll(enchantLines(d));
            }
        }
        return lore;
    }

    private static List<String> enchantLines(DropIndex.Drop d) {
        if (d.enchantments().isEmpty()) return List.of();
        List<String> lines = new ArrayList<>();
        lines.add("");
        for (String e : d.enchantments()) lines.add("&9" + e);
        return lines;
    }

    // ------------------------------------------------------------ helpers

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

    static String prettyEntity(String entity) {
        String[] parts = entity.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1));
        }
        return sb.toString();
    }

    static String prettyRank(String rank) {
        if (rank == null || rank.isBlank()) return "Boss";
        String r = rank.toLowerCase();
        return Character.toUpperCase(r.charAt(0)) + r.substring(1);
    }
}
