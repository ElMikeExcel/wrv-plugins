package com.winkreyes.excelwhatsnew;

import com.winkreyes.excelwhatsnew.core.Changelog;
import com.winkreyes.excelwhatsnew.core.Entry;
import com.winkreyes.excelwhatsnew.core.Nav;
import com.winkreyes.excelwhatsnew.core.Render;
import com.winkreyes.excelwhatsnew.core.Seen;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * ExcelWhatsNew 0.1.1 (Wink Reyes Valley): server news on join, once per entry, until OK.
 * Entries live in changelog.yml (newest first). Bedrock gets a Floodgate form with an OK
 * button; Java gets a chat box with a clickable [OK]. Closing without OK means it shows
 * again on the next join. /whatsnew (alias /news) reads the latest entries any time.
 * 0.1.1: Newer news button (Java and Bedrock) to page back toward the newest entry.
 */
public final class ExcelWhatsNew extends JavaPlugin implements Listener {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final String RULE = "&6&m                                                     ";

    private Changelog log;
    private Bedrock bedrock;
    private File playersFile;

    // per player: newest entry they pressed OK on, and the day they did
    private final Map<UUID, String> okId = new HashMap<>();
    private final Map<UUID, LocalDate> okDate = new HashMap<>();
    private final Map<UUID, Integer> shows = new HashMap<>();      // join shows of the entry in showsFor
    private final Map<UUID, String> showsFor = new HashMap<>();

    // session state
    private final Map<UUID, long[]> waiting = new HashMap<>();     // [readyAt, give-up-at]
    private final Map<UUID, Long> lastMove = new HashMap<>();
    private final Map<UUID, Long> lastCombat = new HashMap<>();
    private final Map<UUID, Integer> formOpen = new HashMap<>();
    private final Map<UUID, Boolean> shownThisSession = new HashMap<>();

    // settings
    private boolean joinOn, requireMove, debug;
    private long delayMs, maxWaitMs, moveWindowMs, combatQuietMs;
    private int maxAgeDays, showCount;
    private String footerJava, footerBedrock, okButton, olderButton, newerButton, closeButton;
    private ZoneId zone;

    @Override public void onEnable() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), "changelog.yml").exists()) saveResource("changelog.yml", false);
        playersFile = new File(getDataFolder(), "players.yml");
        bedrock = new Bedrock(getLogger());
        loadAll();
        loadPlayers();
        getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, this::tick, 20L, 20L);
        getLogger().info("ExcelWhatsNew " + getPluginMeta().getVersion() + ": " + log.entries.size() + " news entries"
                + (log.entries.isEmpty() ? "" : " (newest " + log.entries.get(0).id + ")") + ", " + okId.size() + " players on file"
                + (log.warnings.isEmpty() ? "." : ", " + log.warnings.size() + " warnings (/whatsnew check)."));
    }

    @Override public void onDisable() { savePlayers(); }

    private void loadAll() {
        reloadConfig();
        FileConfiguration c = getConfig();
        joinOn = c.getBoolean("join.enabled", true);
        delayMs = (long) (c.getDouble("join.delay-seconds", 8) * 1000);
        maxWaitMs = (long) (c.getDouble("join.max-wait-seconds", 180) * 1000);
        requireMove = c.getBoolean("join.bedrock-require-recent-move", true);
        moveWindowMs = (long) (c.getDouble("join.move-window-seconds", 3) * 1000);
        combatQuietMs = (long) (c.getDouble("join.combat-quiet-seconds", 10) * 1000);
        maxAgeDays = c.getInt("join.max-age-days", 30);
        showCount = Math.max(1, c.getInt("show-count", 3));
        footerJava = c.getString("text.footer-java", "Read it again any time: /whatsnew");
        footerBedrock = c.getString("text.footer-bedrock", "Read it again any time: /whatsnew");
        okButton = c.getString("text.ok-button", "OK");
        olderButton = c.getString("text.older-button", "Older news");
        newerButton = c.getString("text.newer-button", "Newer news");
        closeButton = c.getString("text.close-button", "Close");
        debug = c.getBoolean("debug", false);
        String z = c.getString("time-zone", "America/Chicago");
        try { zone = ZoneId.of(z); } catch (Exception e) { zone = ZoneId.of("America/Chicago"); }
        File f = new File(getDataFolder(), "changelog.yml");
        YamlConfiguration y = new YamlConfiguration();
        loadError = null;
        try {
            y.load(f);
        } catch (Exception ex) {
            // A typo in changelog.yml must not wipe the news: keep what was loaded before.
            String m = String.valueOf(ex.getMessage()).replace('\n', ' ');
            loadError = "changelog.yml could not be read, kept the old news: " + (m.length() > 200 ? m.substring(0, 200) + "..." : m);
            getLogger().warning(loadError);
            if (log != null) return;
            log = Changelog.parse(null);
            return;
        }
        log = Changelog.parse(y.getList("entries"));
        for (String w : log.warnings) getLogger().warning("changelog.yml: " + w);
    }

    private String loadError;

    private LocalDate today() { return LocalDate.now(zone); }

    // ------------------------------------------------------------- join flow

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        shownThisSession.remove(id);
        if (!joinOn) return;
        long now = System.currentTimeMillis();
        if (Seen.pending(log.entries, okId.get(id), okDate.get(id), today(), maxAgeDays) == null) return;
        waiting.put(id, new long[]{now + delayMs, now + delayMs + maxWaitMs});
        if (debug) getLogger().info(e.getPlayer().getName() + " has news waiting");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        waiting.remove(id); lastMove.remove(id); lastCombat.remove(id); formOpen.remove(id); shownThisSession.remove(id);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (e.getFrom().getBlockX() != e.getTo().getBlockX() || e.getFrom().getBlockZ() != e.getTo().getBlockZ()
                || e.getFrom().getYaw() != e.getTo().getYaw() || e.getFrom().getBlockY() != e.getTo().getBlockY())
            lastMove.put(e.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHurt(EntityDamageEvent e) {
        long now = System.currentTimeMillis();
        if (e.getEntity() instanceof Player p) lastCombat.put(p.getUniqueId(), now);
        if (e instanceof EntityDamageByEntityEvent be) {
            Entity d = be.getDamager();
            if (d instanceof Projectile pr && pr.getShooter() instanceof Player sp) d = sp;
            if (d instanceof Player dp) lastCombat.put(dp.getUniqueId(), now);
        }
    }

    /** Why the moment is not calm, or null when it is. */
    String notCalm(Player p, long now) {
        UUID id = p.getUniqueId();
        if (formOpen.containsKey(id)) return "our form open";
        Long c = lastCombat.get(id);
        if (c != null && now - c < combatQuietMs) return "combat";
        if (p.isDead() || p.isSleeping()) return "dead or sleeping";
        InventoryType top = p.getOpenInventory().getTopInventory().getType();
        if (top != InventoryType.CRAFTING && top != InventoryType.CREATIVE) return "screen open (" + top + ")";
        if (requireMove && Bedrock.isBedrock(p)) {
            // A Bedrock player cannot move while any form (menu, tip) is open, so a recent move = no form up.
            Long m = lastMove.get(id);
            if (m == null || now - m > moveWindowMs) return "no recent move (another pop-up may be open)";
        }
        return null;
    }

    private void tick() {
        if (waiting.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (UUID id : new ArrayList<>(waiting.keySet())) {
            long[] w = waiting.get(id);
            Player p = Bukkit.getPlayer(id);
            if (p == null) { waiting.remove(id); continue; }
            if (now < w[0]) continue;
            if (now > w[1]) { waiting.remove(id); if (debug) getLogger().info("news for " + p.getName() + " gave up waiting (next join)"); continue; }
            String why = notCalm(p, now);
            if (why != null) { if (debug && (now / 1000) % 10 == 0) getLogger().info("news for " + p.getName() + " waits: " + why); continue; }
            waiting.remove(id);
            Entry e = Seen.pending(log.entries, okId.get(id), okDate.get(id), today(), maxAgeDays);
            if (e == null) continue;
            if (!e.id.equals(showsFor.get(id))) { showsFor.put(id, e.id); shows.put(id, 0); }
            shows.merge(id, 1, Integer::sum);
            savePlayers();
            shownThisSession.put(id, true);
            if (debug) getLogger().info("news " + e.id + " to " + p.getName() + " (join show " + shows.get(id) + ")");
            open(p, 0, true);
        }
    }

    // ------------------------------------------------------------- showing

    private Component legacy(String s) { return LEGACY.deserialize(s); }

    private Component button(String label, NamedTextColor color, String cmd, String hover) {
        return Component.text("[" + label + "]", color, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand(cmd)).hoverEvent(HoverEvent.showText(Component.text(hover)));
    }

    private int shown() { return Math.min(showCount, log.entries.size()); }

    /** Show entry idx (0 = newest). fromJoin = the once-per-join announcement. */
    void open(Player p, int idx, boolean fromJoin) {
        if (log.entries.isEmpty()) { p.sendMessage(legacy("&7No news yet.")); return; }
        if (idx < 0 || idx >= shown()) idx = 0;
        Entry e = log.entries.get(idx);
        if (Bedrock.isBedrock(p) && openForm(p, idx, e, fromJoin)) return;
        chat(p, idx, e);
    }

    private boolean pendingFor(UUID id, Entry e) {
        Entry pe = Seen.pending(log.entries, okId.get(id), okDate.get(id), today(), 0);
        return pe != null && pe.id.equals(e.id);
    }

    private void chat(Player p, int idx, Entry e) {
        boolean br = Bedrock.isBedrock(p);
        boolean pend = pendingFor(p.getUniqueId(), e);
        p.sendMessage(legacy(RULE));
        for (String l : Render.java(e)) p.sendMessage(legacy(l));
        if (br) {
            // Form could not open: Bedrock cannot click chat, so tell them what to type (both directions).
            String hint = Nav.typedHint(pend, idx, shown());
            if (!hint.isEmpty()) p.sendMessage(legacy(hint));
        } else {
            Component row = Component.empty();
            boolean any = false;
            for (Nav.Act act : Nav.javaButtons(pend, idx, shown())) {
                Component b = switch (act) {
                    case OK -> button(okButton, NamedTextColor.GREEN, "/whatsnew ok " + e.id, "Got it, don't show this again");
                    case NEWER -> button(newerButton, NamedTextColor.AQUA, "/whatsnew " + Nav.newerNumber(idx), "Read the news after this");
                    case OLDER -> button(olderButton, NamedTextColor.AQUA, "/whatsnew " + Nav.olderNumber(idx), "Read the news before this");
                    default -> null;
                };
                if (b == null) continue;
                row = row.append(Component.text(any ? "  " : "")).append(b);
                any = true;
            }
            if (any) p.sendMessage(row);
            if (footerJava != null && !footerJava.isEmpty()) p.sendMessage(legacy("&7" + footerJava));
        }
        p.sendMessage(legacy(RULE));
    }

    private boolean openForm(Player p, int idx, Entry e, boolean fromJoin) {
        UUID id = p.getUniqueId();
        boolean pend = pendingFor(id, e);
        // Buttons are conditional, so the clicked id is mapped through the same list that built the form.
        final List<Nav.Act> acts = Nav.formButtons(pend, idx, shown());
        List<String> buttons = new ArrayList<>();
        for (Nav.Act act : acts) buttons.add(switch (act) {
            case OK -> okButton;
            case NEWER -> newerButton;
            case OLDER -> olderButton;
            case CLOSE -> closeButton;
        });
        final int fi = idx; final String eid = e.id;
        String via = bedrock.send(p, Render.formTitle(e), Render.bedrock(e, footerBedrock), buttons, clicked ->
                Bukkit.getScheduler().runTask(this, () -> {
                    formOpen.remove(id);
                    Player q = Bukkit.getPlayer(id);
                    Nav.Act act = Nav.clicked(acts, clicked);
                    if (debug) getLogger().info("news form " + eid + " answer " + clicked + " (" + act + ") from " + (q == null ? id : q.getName()));
                    if (q == null) return;
                    if (act == Nav.Act.OK) ack(q, eid, true);
                    else if (act == Nav.Act.NEWER) open(q, fi - 1, false);
                    else if (act == Nav.Act.OLDER) open(q, fi + 1, false);
                    else if (act == null && pend && fromJoin) q.sendMessage(legacy("&7Closed. The news shows again next time you join. Type &e/whatsnew &7to read it."));
                }));
        if (via == null) return false;
        formOpen.put(id, 1);
        if (debug) getLogger().info("news form " + e.id + " to " + p.getName() + " via " + via);
        return true;
    }

    /** Mark entry as OK'd. Never moves a player's mark backwards to an older entry. */
    boolean ack(Player p, String eid, boolean loud) {
        UUID id = p.getUniqueId();
        int idx = log.indexOf(eid);
        if (idx < 0) { if (loud) p.sendMessage(legacy("&7That news entry is gone.")); return false; }
        int cur = log.indexOf(okId.get(id));
        if (cur >= 0 && cur <= idx) { if (loud) p.sendMessage(legacy("&7You already said OK to that news.")); return false; }
        Entry e = log.entries.get(idx);
        okId.put(id, e.id);
        okDate.put(id, e.posted != null ? e.posted : today());
        waiting.remove(id);
        savePlayers();
        if (loud) p.sendMessage(legacy("&aGot it! &7You won't see that news on join again. &e/whatsnew &7shows it any time."));
        return true;
    }

    // ------------------------------------------------------------- storage

    private void loadPlayers() {
        okId.clear(); okDate.clear(); shows.clear(); showsFor.clear();
        if (!playersFile.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(playersFile);
        ConfigurationSection ps = y.getConfigurationSection("players");
        if (ps == null) return;
        for (String k : ps.getKeys(false)) {
            UUID id;
            try { id = UUID.fromString(k); } catch (IllegalArgumentException ex) { continue; }
            String ok = ps.getString(k + ".ok");
            if (ok != null && !ok.isEmpty()) okId.put(id, ok);
            String d = ps.getString(k + ".ok-day");
            if (d != null) try { okDate.put(id, LocalDate.parse(d)); } catch (Exception ignored) { }
            String sf = ps.getString(k + ".shows-for");
            if (sf != null) { showsFor.put(id, sf); shows.put(id, ps.getInt(k + ".shows", 0)); }
        }
    }

    void savePlayers() {
        YamlConfiguration y = new YamlConfiguration();
        y.options().setHeader(List.of("ExcelWhatsNew: player id -> ok (newest news id they said OK to), ok-day, shows-for/shows (join shows of the newest news). Edit with /whatsnew reset."));
        java.util.Set<UUID> all = new java.util.HashSet<>(okId.keySet()); all.addAll(showsFor.keySet());
        for (UUID id : all) {
            String b = "players." + id;
            if (okId.containsKey(id)) y.set(b + ".ok", okId.get(id));
            if (okDate.containsKey(id)) y.set(b + ".ok-day", okDate.get(id).toString());
            if (showsFor.containsKey(id)) { y.set(b + ".shows-for", showsFor.get(id)); y.set(b + ".shows", shows.getOrDefault(id, 0)); }
        }
        try {
            getDataFolder().mkdirs();
            File tmp = new File(getDataFolder(), "players.yml.tmp");
            y.save(tmp);
            Files.move(tmp.toPath(), playersFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception ex) { getLogger().warning("players.yml save failed: " + ex.getMessage()); }
    }

    // ------------------------------------------------------------- commands

    private void say(CommandSender s, String m) { s.sendMessage(legacy(m)); }

    @SuppressWarnings("deprecation")
    private OfflinePlayer find(String name) {
        Player p = Bukkit.getPlayerExact(name);
        if (p != null) return p;
        OfflinePlayer o = Bukkit.getOfflinePlayerIfCached(name);
        return o != null && (o.hasPlayedBefore() || okId.containsKey(o.getUniqueId())) ? o : null;
    }

    @Override public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        boolean admin = s.hasPermission("excelwhatsnew.admin");
        String sub = a.length == 0 ? "" : a[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "" -> { if (s instanceof Player p) open(p, 0, false); else list(s); }
            case "ok", "okay" -> {
                if (!(s instanceof Player p)) { say(s, "&cPlayers only."); return true; }
                if (log.entries.isEmpty()) { say(s, "&7No news yet."); return true; }
                String eid = a.length > 1 ? a[1] : log.entries.get(0).id;
                ack(p, eid, true);
            }
            case "list", "all" -> list(s);
            case "reload" -> {
                if (!admin) { say(s, "&cOnly staff can reload the news."); return true; }
                loadAll();
                if (loadError != null) { say(s, "&c" + loadError); say(s, "&7Fix the file and run /whatsnew reload again."); return true; }
                say(s, "&aExcelWhatsNew reloaded: " + log.entries.size() + " entries" + (log.entries.isEmpty() ? "" : ", newest " + log.entries.get(0).id)
                        + (log.warnings.isEmpty() ? ", no warnings." : ", " + log.warnings.size() + " warnings (see /whatsnew check)."));
                // Players online with the new entry waiting get it at their next calm moment, once.
                int queued = 0;
                if (joinOn) for (Player p : Bukkit.getOnlinePlayers()) {
                    UUID id = p.getUniqueId();
                    if (waiting.containsKey(id) || formOpen.containsKey(id)) continue;
                    Entry e = Seen.pending(log.entries, okId.get(id), okDate.get(id), today(), maxAgeDays);
                    if (e != null && !e.id.equals(showsFor.get(id))) { long now = System.currentTimeMillis(); waiting.put(id, new long[]{now + delayMs, now + delayMs + maxWaitMs}); queued++; }
                }
                if (queued > 0) say(s, "&7New news queued for " + queued + " online player(s).");
            }
            case "check" -> {
                if (!admin) { say(s, "&cStaff only."); return true; }
                if (loadError != null) say(s, "&c" + loadError);
                if (log.warnings.isEmpty()) say(s, "&achangelog.yml: " + log.entries.size() + " entries, no warnings.");
                else for (String w : log.warnings) say(s, "&e" + w);
            }
            case "reset" -> {
                if (!admin || a.length < 2) { say(s, admin ? "&7/whatsnew reset <player>" : "&cStaff only."); return true; }
                OfflinePlayer o = find(a[1]);
                if (o == null) { say(s, "&cNo player " + a[1] + " on file."); return true; }
                UUID id = o.getUniqueId();
                okId.remove(id); okDate.remove(id); shows.remove(id); showsFor.remove(id); shownThisSession.remove(id);
                savePlayers();
                say(s, "&a" + a[1] + " will see the news again on their next join.");
            }
            case "status" -> {
                if (!admin || a.length < 2) { say(s, admin ? "&7/whatsnew status <player>" : "&cStaff only."); return true; }
                OfflinePlayer o = find(a[1]);
                if (o == null) { say(s, "&cNo player " + a[1] + " on file."); return true; }
                UUID id = o.getUniqueId();
                Entry pe = Seen.pending(log.entries, okId.get(id), okDate.get(id), today(), maxAgeDays);
                say(s, "&e" + a[1] + "&7: ok=" + okId.getOrDefault(id, "none") + " pending=" + (pe == null ? "none" : pe.id)
                        + " shows=" + (showsFor.containsKey(id) ? shows.getOrDefault(id, 0) + " of " + showsFor.get(id) : "0")
                        + (waiting.containsKey(id) ? " (waiting for a calm moment)" : ""));
            }
            case "push" -> {
                if (!admin || a.length < 2) { say(s, admin ? "&7/whatsnew push <player>  (run the join announcement now)" : "&cStaff only."); return true; }
                Player p = Bukkit.getPlayerExact(a[1]);
                if (p == null) { say(s, "&cNot online."); return true; }
                long now = System.currentTimeMillis();
                if (Seen.pending(log.entries, okId.get(p.getUniqueId()), okDate.get(p.getUniqueId()), today(), maxAgeDays) == null) { say(s, "&7Nothing pending for " + a[1] + "."); return true; }
                waiting.put(p.getUniqueId(), new long[]{now, now + maxWaitMs});
                say(s, "&aQueued for the next calm moment.");
            }
            default -> {
                int n;
                try { n = Integer.parseInt(sub); } catch (NumberFormatException ex) { n = -1; }
                if (n >= 1 && s instanceof Player p) { if (n > shown()) n = shown(); open(p, n - 1, false); return true; }
                say(s, "&7/whatsnew &f- the latest news   &7/whatsnew list &f- recent titles   &7/whatsnew ok &f- hide it on join");
                if (admin) say(s, "&7Staff: /whatsnew reload | check | status <player> | reset <player> | push <player>");
            }
        }
        return true;
    }

    private void list(CommandSender s) {
        if (log.entries.isEmpty()) { say(s, "&7No news yet."); return; }
        say(s, "&e&lRecent news");
        for (int i = 0; i < shown(); i++) {
            Entry e = log.entries.get(i);
            Component line = legacy(" &7" + (i + 1) + ". &f" + e.title + (e.date.isEmpty() ? "" : " &7(" + e.date + ")"));
            if (s instanceof Player p && !Bedrock.isBedrock(p))
                line = line.append(Component.text(" ")).append(button("Read", NamedTextColor.AQUA, "/whatsnew " + (i + 1), "Open this news"));
            s.sendMessage(line);
        }
        if (s instanceof Player p && Bedrock.isBedrock(p)) { String h = Nav.listHint(shown()); if (!h.isEmpty()) say(s, h); }
    }

    @Override public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            List<String> opts = new ArrayList<>(List.of("ok", "list"));
            for (int i = 1; i <= shown(); i++) opts.add(String.valueOf(i));
            if (s.hasPermission("excelwhatsnew.admin")) opts.addAll(List.of("reload", "check", "status", "reset", "push"));
            for (String o : opts) if (o.startsWith(a[0].toLowerCase(Locale.ROOT))) out.add(o);
        } else if (a.length == 2 && s.hasPermission("excelwhatsnew.admin") && List.of("status", "reset", "push").contains(a[0].toLowerCase(Locale.ROOT))) {
            for (Player p : Bukkit.getOnlinePlayers()) if (p.getName().toLowerCase(Locale.ROOT).startsWith(a[1].toLowerCase(Locale.ROOT))) out.add(p.getName());
        }
        return out;
    }
}
