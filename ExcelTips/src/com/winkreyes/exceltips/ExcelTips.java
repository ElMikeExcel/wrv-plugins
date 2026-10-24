package com.winkreyes.exceltips;

import com.winkreyes.exceltips.core.TipBook;
import com.winkreyes.exceltips.core.TipDef;
import com.winkreyes.exceltips.core.Words;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Tag;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerLoadEvent;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ExcelTips: short first-use tips for every server feature, shown the first
 * few times a player uses it, until they say OK. Tips live in tips.yml;
 * settings in config.yml; per player progress in players.yml.
 * Java: a clickable [OK] in chat. Bedrock: an OK pop-up (Floodgate form) at
 * a calm moment, never in combat. /tips ok works everywhere, and a plain
 * "ok" in chat right after a tip works too.
 */
public final class ExcelTips extends JavaPlugin implements Listener {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final TipBook book = new TipBook();
    private Map<String, TipDef> defs = new LinkedHashMap<>();
    /** Tips other plugins registered through ExcelTipsAPI.register (tips.yml wins on the same name). */
    private final Map<String, TipDef> registered = new LinkedHashMap<>();
    private Map<String, TipDef> fileDefs = new LinkedHashMap<>();
    private final Map<String, List<TipDef>> byCommand = new HashMap<>();
    private final Map<String, List<TipDef>> byType = new HashMap<>();
    private final List<Listener> eventHooks = new ArrayList<>();
    private Bedrock bedrock;

    // settings
    private int defaultMax = 3; private long useGapMs = 3000, minGapAnyMs = 10_000, combatQuietMs = 10_000;
    private String prefix = "&b[Tip] &f"; private String lineColor = "&f"; private String hintColor = "&7";
    private boolean okReplies = true; private long okWindowMs = 120_000;
    private boolean formsOn = true; private long formCooldownMs = 300_000, calmMs = 8_000, moveWindowMs = 3_000, maxWaitMs = 180_000, quietAfterJoinMs = 45_000;
    private boolean requireMove = true;
    private String formButtonOk = "OK, got it!", formButtonLater = "Remind me later";
    private long delayCommand = 10, delayJoin = 100, delayRespawn = 40, delayDefault = 2;
    private boolean debug = false;

    // session
    private final Map<UUID, Long> lastBusy = new HashMap<>(), lastMove = new HashMap<>(), joinedAt = new HashMap<>();
    private final Map<UUID, String[]> pendingForm = new HashMap<>();   // feature, createdMs
    private final Map<UUID, Set<String>> insideRegions = new HashMap<>();
    private final Map<UUID, Integer> formOpen = new HashMap<>();
    private File playersFile;

    TipBook book() { return book; }

    @Override public void onEnable() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), "tips.yml").exists()) saveResource("tips.yml", false);
        bedrock = new Bedrock(getLogger());
        playersFile = new File(getDataFolder(), "players.yml");
        loadPlayers();
        String err = loadAll();
        if (err != null) getLogger().warning(err);
        getServer().getPluginManager().registerEvents(this, this);
        long save = Math.max(5, getConfig().getLong("save-interval-seconds", 30)) * 20L;
        Bukkit.getScheduler().runTaskTimer(this, () -> { if (book.dirty()) savePlayers(); }, save, save);
        Bukkit.getScheduler().runTaskTimer(this, this::formTick, 20L, 20L);
        ExcelTipsAPI.plugin = this;
        getLogger().info("ExcelTips " + getPluginMeta().getVersion() + ": " + defs.size() + " tips (" + defs.values().stream().filter(d -> d.enabled).count() + " switched on), " + book.players().size() + " players on file.");
    }

    @Override public void onDisable() {
        if (book.dirty()) savePlayers();
        ExcelTipsAPI.plugin = null;
    }

    // ------------------------------------------------------------- loading

    /** (Re)load config.yml and tips.yml. Returns an error message, or null. Keeps the old tips on a bad file. */
    String loadAll() {
        reloadConfig();
        var c = getConfig();
        defaultMax = c.getInt("default-max-shows", 3);
        useGapMs = (long) (c.getDouble("use-gap-seconds", 3) * 1000);
        minGapAnyMs = (long) (c.getDouble("min-gap-between-tips-seconds", 10) * 1000);
        combatQuietMs = (long) (c.getDouble("combat-quiet-seconds", 10) * 1000);
        prefix = c.getString("chat.prefix", prefix); lineColor = c.getString("chat.line-color", lineColor); hintColor = c.getString("chat.hint-color", hintColor);
        okReplies = c.getBoolean("chat.ok-replies", true); okWindowMs = (long) (c.getDouble("chat.ok-reply-window-seconds", 120) * 1000);
        formsOn = c.getBoolean("bedrock-form.enabled", true);
        formCooldownMs = (long) (c.getDouble("bedrock-form.cooldown-seconds", 300) * 1000);
        calmMs = (long) (c.getDouble("bedrock-form.calm-seconds", 8) * 1000);
        requireMove = c.getBoolean("bedrock-form.require-recent-move", true);
        moveWindowMs = (long) (c.getDouble("bedrock-form.move-window-seconds", 3) * 1000);
        maxWaitMs = (long) (c.getDouble("bedrock-form.max-wait-seconds", 180) * 1000);
        quietAfterJoinMs = (long) (c.getDouble("bedrock-form.quiet-after-join-seconds", 45) * 1000);
        formButtonOk = c.getString("bedrock-form.ok-button", formButtonOk); formButtonLater = c.getString("bedrock-form.later-button", formButtonLater);
        delayCommand = c.getLong("delays.command-ticks", 10); delayJoin = c.getLong("delays.join-ticks", 100);
        delayRespawn = c.getLong("delays.respawn-ticks", 40); delayDefault = c.getLong("delays.default-ticks", 2);
        debug = c.getBoolean("debug", false);

        File f = new File(getDataFolder(), "tips.yml");
        YamlConfiguration y = new YamlConfiguration();
        try { y.load(f); } catch (Exception e) { return "tips.yml not loaded (" + e.getMessage() + "); keeping " + defs.size() + " tips"; }
        ConfigurationSection s = y.getConfigurationSection("tips");
        if (s == null) return "tips.yml has no 'tips:' section; keeping " + defs.size() + " tips";
        Map<String, TipDef> nd = new LinkedHashMap<>();
        for (String id : s.getKeys(false)) {
            ConfigurationSection t = s.getConfigurationSection(id);
            if (t == null) continue;
            nd.put(TipBook.norm(id), parse(TipBook.norm(id), t));
        }
        fileDefs = nd;
        merge();
        for (TipDef d : defs.values()) for (String p : d.problems()) getLogger().warning("tips.yml: " + p);
        return null;
    }

    /** defs = tips.yml + registered tips that tips.yml does not override. Re-indexes triggers. */
    private void merge() {
        Map<String, TipDef> m = new LinkedHashMap<>(fileDefs);
        for (var e : registered.entrySet()) m.putIfAbsent(e.getKey(), e.getValue());
        defs = m;
        index();
    }

    /** Register a default tip from another plugin. tips.yml entries with the same name win. */
    boolean register(Plugin owner, String feature, String title, List<String> java, List<String> bedrock, int maxShows) {
        String f = TipBook.norm(feature);
        if (owner == null || f.isEmpty()) return false;
        TipDef d = new TipDef();
        d.id = f; d.owner = owner.getName(); d.needsPlugin = owner.getName();
        d.title = title == null || title.isBlank() ? "Tip" : title;
        d.java = java == null ? new ArrayList<>() : new ArrayList<>(java);
        d.bedrock = bedrock == null || bedrock.isEmpty() ? new ArrayList<>(d.java) : new ArrayList<>(bedrock);
        d.maxShows = maxShows;
        TipDef.Trigger t = new TipDef.Trigger(); t.type = "api"; d.triggers.add(t);
        List<String> probs = d.problems();
        if (!probs.isEmpty()) { for (String p : probs) getLogger().warning("register from " + owner.getName() + ": " + p); if (d.java.isEmpty()) return false; }
        registered.put(f, d);
        merge();
        if (debug) getLogger().info("tip " + f + " registered by " + owner.getName() + (fileDefs.containsKey(f) ? " (tips.yml entry wins)" : ""));
        return hasFeature(f);
    }

    boolean unregister(Plugin owner, String feature) {
        String f = TipBook.norm(feature);
        TipDef d = registered.get(f);
        if (d == null || owner == null || !d.owner.equals(owner.getName())) return false;
        registered.remove(f); merge(); return true;
    }

    /** Tip is switched on and its plugin conditions hold right now. */
    boolean active(TipDef d) {
        if (d == null || !d.enabled) return false;
        if (!d.needsPlugin.isBlank() && !pluginAtLeast(d.needsPlugin)) return false;
        if (!d.unlessPlugin.isBlank() && pluginAtLeast(d.unlessPlugin)) return false;
        return true;
    }

    private static boolean pluginAtLeast(String spec) {
        String[] sp = Words.pluginSpec(spec);
        Plugin p = Bukkit.getPluginManager().getPlugin(sp[0]);
        return p != null && p.isEnabled() && Words.versionAtLeast(p.getPluginMeta().getVersion(), sp[1]);
    }

    // Event classes from plugins that start after us (ExcelTrash, ...) only exist later: re-hook then.
    @org.bukkit.event.EventHandler public void onServerLoad(ServerLoadEvent e) { index(); }
    @org.bukkit.event.EventHandler public void onPluginEnable(PluginEnableEvent e) { if (e.getPlugin() != this) index(); }
    @org.bukkit.event.EventHandler public void onPluginDisable(PluginDisableEvent e) {
        if (e.getPlugin() == this) return;
        String n = e.getPlugin().getName();
        if (registered.values().removeIf(d -> d.owner.equals(n))) merge(); else index();
    }

    private static List<String> strs(ConfigurationSection t, String key) {
        if (t.isList(key)) return t.getStringList(key);
        String v = t.getString(key);
        return v == null ? new ArrayList<>() : new ArrayList<>(List.of(v));
    }

    private TipDef parse(String id, ConfigurationSection t) {
        TipDef d = new TipDef();
        d.id = id;
        d.title = t.getString("title", "Tip");
        d.enabled = t.getBoolean("enabled", true);
        d.maxShows = t.getInt("max-shows", -1);
        d.useGapMs = t.contains("use-gap-seconds") ? (long) (t.getDouble("use-gap-seconds") * 1000) : -1;
        d.form = t.getBoolean("bedrock-popup", true);
        d.permission = t.getString("permission", "");
        d.needsPlugin = t.getString("needs-plugin", "");
        d.unlessPlugin = t.getString("unless-plugin", "");
        d.lines = strs(t, "text"); d.java = strs(t, "java"); d.bedrock = strs(t, "bedrock");
        for (Map<?, ?> m : t.getMapList("triggers")) {
            TipDef.Trigger tr = new TipDef.Trigger();
            tr.type = String.valueOf(m.get("type")).toLowerCase(Locale.ROOT);
            tr.values = list(m, tr.type.equals("command") ? "commands" : "match");
            if (tr.values.isEmpty()) tr.values = list(m, "values");
            tr.pdcKeys = list(m, "pdc-keys"); tr.names = list(m, "name-contains"); tr.notNames = list(m, "name-not-contains");
            tr.metadata = list(m, "metadata"); tr.holders = list(m, "holder-contains");
            if (m.get("world") != null) tr.world = String.valueOf(m.get("world"));
            tr.min = box(m.get("min")); tr.max = box(m.get("max"));
            if (m.get("class") != null) tr.eventClass = String.valueOf(m.get("class"));
            if (m.get("player-path") != null) tr.playerPath = String.valueOf(m.get("player-path"));
            if (m.get("delay-ticks") instanceof Number n) tr.delayTicks = n.longValue();
            d.triggers.add(tr);
        }
        return d;
    }

    private static List<String> list(Map<?, ?> m, String k) {
        Object o = m.get(k);
        List<String> out = new ArrayList<>();
        if (o instanceof List<?> l) for (Object x : l) out.add(String.valueOf(x));
        else if (o != null) out.add(String.valueOf(o));
        return out;
    }

    private static int[] box(Object o) {
        if (!(o instanceof List<?> l) || l.size() != 3) return null;
        int[] b = new int[3];
        for (int i = 0; i < 3; i++) b[i] = ((Number) l.get(i)).intValue();
        return b;
    }

    private void index() {
        byCommand.clear(); byType.clear();
        for (Listener l : eventHooks) HandlerList.unregisterAll(l);
        eventHooks.clear();
        for (TipDef d : defs.values()) {
            if (!d.enabled) continue;
            for (TipDef.Trigger t : d.triggers) {
                if (t.type.equals("command")) for (String c : t.values) byCommand.computeIfAbsent(Words.label(c), k -> new ArrayList<>()).add(d);
                else if (t.type.equals("event")) hookEvent(d, t);
                else byType.computeIfAbsent(t.type, k -> new ArrayList<>()).add(d);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void hookEvent(TipDef d, TipDef.Trigger t) {
        Class<? extends Event> cls = null;
        for (var p : Bukkit.getPluginManager().getPlugins()) {
            try { cls = (Class<? extends Event>) Class.forName(t.eventClass, false, p.getClass().getClassLoader()); break; } catch (Throwable ignored) { }
        }
        if (cls == null) { if (debug) getLogger().info("tip " + d.id + ": event " + t.eventClass + " not found (plugin missing?), skipped"); return; }
        Listener l = new Listener() { };
        String[] path = t.playerPath.split("\\.");
        final Class<? extends Event> c2 = cls;
        Bukkit.getPluginManager().registerEvent(cls, l, EventPriority.MONITOR, (ls, ev) -> {
            if (!c2.isInstance(ev)) return;
            Object o = ev;
            try { for (String m : path) { if (o == null) return; Method mm = o.getClass().getMethod(m); mm.setAccessible(true); o = mm.invoke(o); } }
            catch (Throwable e) { return; }
            if (o instanceof Player pl) later(pl, d, t, delayDefault);
        }, this, false);
        eventHooks.add(l);
    }

    // ------------------------------------------------------------- core

    boolean hasFeature(String f) { return active(defs.get(TipBook.norm(f))); }

    private int maxFor(TipDef d) { return d.maxShows >= 0 ? d.maxShows : defaultMax; }

    boolean finished(UUID id, String f) { TipDef d = defs.get(TipBook.norm(f)); return book.finished(id, f, d == null ? defaultMax : maxFor(d)); }

    /** One use of a feature (by name). */
    boolean fire(Player p, String feature, String source) {
        TipDef d = defs.get(TipBook.norm(feature));
        return d != null && fire(p, d, source);
    }

    boolean fire(Player p, TipDef d, String source) {
        if (p == null || !p.isOnline() || d == null) return false;
        if (!d.permission.isEmpty() && !p.hasPermission(d.permission)) return false;
        long now = System.currentTimeMillis();
        TipBook.Result r = book.onUse(p.getUniqueId(), d.id, now, active(d), maxFor(d), d.useGapMs >= 0 ? d.useGapMs : useGapMs, minGapAnyMs, combatQuietMs);
        if (debug) getLogger().info("tip " + d.id + " for " + p.getName() + " (" + source + "): " + r + " shows=" + book.shows(p.getUniqueId(), d.id));
        if (r != TipBook.Result.SHOW) return false;
        show(p, d, true);
        return true;
    }

    private void later(Player p, TipDef d, TipDef.Trigger t, long defTicks) {
        long ticks = t != null && t.delayTicks >= 0 ? t.delayTicks : defTicks;
        if (ticks <= 0) fire(p, d, t == null ? "?" : t.type);
        else Bukkit.getScheduler().runTaskLater(this, () -> fire(p, d, t == null ? "?" : t.type), ticks);
    }

    private Component legacy(String s) { return LEGACY.deserialize(s); }

    /** Put the tip in chat. counted = a real use (may queue the Bedrock pop-up); false = /tips show replay. */
    void show(Player p, TipDef d, boolean counted) {
        boolean br = Bedrock.isBedrock(p);
        List<String> lines = d.linesFor(br);
        for (int i = 0; i < lines.size(); i++) p.sendMessage(legacy((i == 0 ? prefix : lineColor) + lines.get(i)));
        if (book.acked(p.getUniqueId(), d.id) && !counted) return;
        if (!br) {
            Component ok = Component.text("[OK]", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/tips ok " + d.id))
                    .hoverEvent(HoverEvent.showText(Component.text("Hide this tip")));
            p.sendMessage(legacy(hintColor + "Click ").append(ok).append(legacy(hintColor + " to hide this tip.")));
            return;
        }
        boolean popup = counted && formsOn && d.form;
        if (popup) pendingForm.put(p.getUniqueId(), new String[]{d.id, String.valueOf(System.currentTimeMillis())});
        p.sendMessage(legacy(hintColor + (popup ? "Tap OK on the pop-up, or type /tips ok, to hide this tip." : "Type /tips ok to hide this tip.")));
    }

    /** Every second: send a waiting Bedrock pop-up when the moment is calm. */
    private void formTick() {
        if (pendingForm.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (UUID id : new ArrayList<>(pendingForm.keySet())) {
            String[] pf = pendingForm.get(id);
            Player p = Bukkit.getPlayer(id);
            TipDef d = defs.get(pf[0]);
            long created = Long.parseLong(pf[1]);
            if (p == null || d == null || book.acked(id, d.id) || now - created > maxWaitMs) { pendingForm.remove(id); continue; }
            if (!calm(p, now)) continue;
            if (!book.takeForm(id, now, formCooldownMs, combatQuietMs)) { if (debug) getLogger().info("pop-up for " + p.getName() + " skipped (cooldown)"); pendingForm.remove(id); continue; }
            pendingForm.remove(id);
            String content = String.join("\n\n", d.linesFor(true));
            final String fid = d.id;
            String via = bedrock.send(p, d.title, content, List.of(formButtonOk, formButtonLater), clicked ->
                    Bukkit.getScheduler().runTask(this, () -> {
                        formOpen.remove(id);
                        lastBusy.put(id, System.currentTimeMillis());
                        Player q = Bukkit.getPlayer(id);
                        if (clicked == 0 && q != null) ackLoud(q, fid, "popup");
                        if (debug) getLogger().info("pop-up " + fid + " answer " + clicked + " from " + (q == null ? id : q.getName()));
                    }));
            if (via != null) { formOpen.put(id, 1); lastBusy.put(id, now); }
            if (debug) getLogger().info("pop-up " + fid + " to " + p.getName() + ": " + (via == null ? "NOT SENT" : "sent via " + via));
        }
    }

    boolean calm(Player p, long now) {
        UUID id = p.getUniqueId();
        if (formOpen.containsKey(id)) return false;
        if (book.inCombat(id, now, combatQuietMs)) return false;
        Long j = joinedAt.get(id);
        if (j != null && now - j < quietAfterJoinMs) return false;
        Long b = lastBusy.get(id);
        if (b != null && now - b < calmMs) return false;
        if (requireMove) { Long m = lastMove.get(id); if (m == null || now - m > moveWindowMs) return false; }
        if (p.isDead() || p.isSleeping()) return false;
        InventoryType top = p.getOpenInventory().getTopInventory().getType();
        return top == InventoryType.CRAFTING || top == InventoryType.CREATIVE;
    }

    /** Ack with a chat confirmation. */
    boolean ackLoud(Player p, String f) { return ackLoud(p, f, "chat"); }

    boolean ackLoud(Player p, String f, String source) {
        TipDef d = defs.get(TipBook.norm(f));
        boolean n = book.ack(p.getUniqueId(), f);
        if (n) Bukkit.getPluginManager().callEvent(new ExcelTipsAckEvent(p.getUniqueId(), TipBook.norm(f), source));
        pendingForm.computeIfPresent(p.getUniqueId(), (k, v) -> v[0].equals(TipBook.norm(f)) ? null : v);
        String name = d == null ? f : d.title;
        p.sendMessage(legacy(hintColor + (n ? "Got it! The " + name + " tip won't show again. /tips shows all your tips." : "That tip is already hidden.")));
        return n;
    }

    /** Ack from the API (no chat line; the caller says its own thing). */
    boolean ackQuiet(UUID id, String f) {
        boolean n = book.ack(id, f);
        if (n) Bukkit.getPluginManager().callEvent(new ExcelTipsAckEvent(id, TipBook.norm(f), "api"));
        pendingForm.computeIfPresent(id, (k, v) -> v[0].equals(TipBook.norm(f)) ? null : v);
        return n;
    }

    // ------------------------------------------------------------- triggers

    private void busy(Player p) { lastBusy.put(p.getUniqueId(), System.currentTimeMillis()); }

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer(); busy(p);
        String label = Words.label(e.getMessage());
        if (label.equals("tips") || label.equals("tip")) return;
        List<TipDef> l = byCommand.get(label);
        if (l == null) return;
        for (TipDef d : l) later(p, d, trig(d, "command"), delayCommand);
    }

    private static TipDef.Trigger trig(TipDef d, String type) {
        for (TipDef.Trigger t : d.triggers) if (t.type.equals(type)) return t;
        return null;
    }

    private static boolean pdcMatch(ItemStack it, List<String> keys) {
        if (keys.isEmpty()) return true;
        if (it == null || !it.hasItemMeta()) return false;
        ItemMeta m = it.getItemMeta();
        PersistentDataContainer c = m.getPersistentDataContainer();
        for (String k : keys) {
            NamespacedKey nk = NamespacedKey.fromString(k.toLowerCase(Locale.ROOT));
            if (nk != null && c.getKeys().contains(nk)) return true;
        }
        return false;
    }

    private static boolean matMatch(Material m, List<String> vals, boolean block) {
        if (vals.isEmpty()) return true;
        if (m == null) return false;
        for (String v : vals) {
            if (v.startsWith("#")) {
                Tag<Material> tag = Bukkit.getTag(block ? Tag.REGISTRY_BLOCKS : Tag.REGISTRY_ITEMS, NamespacedKey.minecraft(v.substring(1).toLowerCase(Locale.ROOT)), Material.class);
                if (tag != null && tag.isTagged(m)) return true;
            } else if (m.name().equalsIgnoreCase(v)) return true;
        }
        return false;
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent e) {
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        ItemStack it = e.getItem();
        for (TipDef d : byType.getOrDefault("item-use", List.of())) for (TipDef.Trigger t : d.triggers) {
            if (!t.type.equals("item-use") || it == null) continue;
            if (matMatch(it.getType(), t.values, false) && pdcMatch(it, t.pdcKeys)) { if (!t.pdcKeys.isEmpty()) busy(p); later(p, d, t, delayDefault); break; }
        }
        if (a == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null) {
            Material bm = e.getClickedBlock().getType();
            for (TipDef d : byType.getOrDefault("block-use", List.of())) for (TipDef.Trigger t : d.triggers) {
                if (t.type.equals("block-use") && matMatch(bm, t.values, true)) { later(p, d, t, delayDefault); break; }
            }
        }
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR)
    public void onBed(PlayerBedEnterEvent e) {
        for (TipDef d : byType.getOrDefault("bed-enter", List.of())) later(e.getPlayer(), d, trig(d, "bed-enter"), 20);
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR)
    public void onEntity(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer(); busy(p);
        Entity en = e.getRightClicked();
        String name = en.customName() == null ? "" : PLAIN.serialize(en.customName()).toLowerCase(Locale.ROOT);
        for (TipDef d : byType.getOrDefault("entity-use", List.of())) for (TipDef.Trigger t : d.triggers) {
            if (!t.type.equals("entity-use")) continue;
            if (!t.values.isEmpty() && t.values.stream().noneMatch(v -> v.equalsIgnoreCase(en.getType().name()))) continue;
            if (!t.names.isEmpty() && t.names.stream().noneMatch(v -> name.contains(v.toLowerCase(Locale.ROOT)))) continue;
            if (t.notNames.stream().anyMatch(v -> name.contains(v.toLowerCase(Locale.ROOT)))) continue;
            if (!t.metadata.isEmpty() && t.metadata.stream().noneMatch(en::hasMetadata)) continue;
            later(p, d, t, delayCommand); break;
        }
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR)
    public void onOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        busy(p);
        List<TipDef> l = byType.get("inventory-open");
        if (l == null) return;
        String title = PLAIN.serialize(e.getView().title()).toLowerCase(Locale.ROOT);
        InventoryHolder h = e.getInventory().getHolder(false);
        String holder = h == null ? "" : h.getClass().getName();
        for (TipDef d : l) for (TipDef.Trigger t : d.triggers) {
            if (!t.type.equals("inventory-open")) continue;
            boolean tm = !t.values.isEmpty() && t.values.stream().anyMatch(v -> title.contains(v.toLowerCase(Locale.ROOT)));
            boolean hm = !t.holders.isEmpty() && t.holders.stream().anyMatch(holder::contains);
            if (tm || hm) { later(p, d, t, delayCommand); break; }
        }
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent e) { if (e.getPlayer() instanceof Player p) busy(p); }

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Location f = e.getFrom(), to = e.getTo();
        if (f.getX() == to.getX() && f.getY() == to.getY() && f.getZ() == to.getZ()) return;
        Player p = e.getPlayer();
        lastMove.put(p.getUniqueId(), System.currentTimeMillis());
        if (f.getBlockX() == to.getBlockX() && f.getBlockY() == to.getBlockY() && f.getBlockZ() == to.getBlockZ() && f.getWorld() == to.getWorld()) return;
        regions(p, to);
    }

    private void regions(Player p, Location to) {
        List<TipDef> l = byType.get("region");
        if (l == null) return;
        Set<String> in = insideRegions.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>());
        for (TipDef d : l) for (int i = 0; i < d.triggers.size(); i++) {
            TipDef.Trigger t = d.triggers.get(i);
            if (!t.type.equals("region") || t.min == null || t.max == null) continue;
            String key = d.id + "#" + i;
            boolean inside = to.getWorld() != null && to.getWorld().getName().equals(t.world)
                    && between(to.getBlockX(), t.min[0], t.max[0]) && between(to.getBlockY(), t.min[1], t.max[1]) && between(to.getBlockZ(), t.min[2], t.max[2]);
            if (inside && in.add(key)) later(p, d, t, delayDefault);
            else if (!inside) in.remove(key);
        }
    }

    private static boolean between(int v, int a, int b) { return v >= Math.min(a, b) && v <= Math.max(a, b); }

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR)
    public void onWorld(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        for (TipDef d : byType.getOrDefault("world-enter", List.of())) for (TipDef.Trigger t : d.triggers) {
            if (t.type.equals("world-enter") && t.values.stream().anyMatch(w -> w.equals(p.getWorld().getName()))) { later(p, d, t, 40); break; }
        }
        regions(p, p.getLocation());
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        long now = System.currentTimeMillis();
        joinedAt.put(p.getUniqueId(), now); lastBusy.put(p.getUniqueId(), now);
        for (TipDef d : byType.getOrDefault("join", List.of())) later(p, d, trig(d, "join"), delayJoin);
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        book.clearCombat(e.getPlayer().getUniqueId()); // the fight ended with the death
        for (TipDef d : byType.getOrDefault("respawn", List.of())) later(e.getPlayer(), d, trig(d, "respawn"), delayRespawn);
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        book.endSession(id);
        lastBusy.remove(id); lastMove.remove(id); joinedAt.remove(id); pendingForm.remove(id); insideRegions.remove(id); formOpen.remove(id);
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHurt(EntityDamageEvent e) {
        long now = System.currentTimeMillis();
        if (e.getEntity() instanceof Player p) book.combat(p.getUniqueId(), now);
        if (e instanceof EntityDamageByEntityEvent be) {
            Entity d = be.getDamager();
            if (d instanceof Projectile pr && pr.getShooter() instanceof Player sp) d = sp;
            if (d instanceof Player dp) book.combat(dp.getUniqueId(), now);
        }
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        if (!okReplies) return;
        String msg = PLAIN.serialize(e.message());
        if (!Words.isOk(msg)) return;
        UUID id = e.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTask(this, () -> {
            String f = book.lastShown(id);
            Player p = Bukkit.getPlayer(id);
            if (p == null || f == null || System.currentTimeMillis() - book.lastShownAt(id) > okWindowMs || book.acked(id, f)) return;
            ackLoud(p, f);
        });
    }

    // ------------------------------------------------------------- storage

    private void loadPlayers() {
        book.clearAll();
        if (!playersFile.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(playersFile);
        ConfigurationSection ps = y.getConfigurationSection("players");
        if (ps == null) return;
        for (String k : ps.getKeys(false)) {
            UUID id;
            try { id = UUID.fromString(k); } catch (IllegalArgumentException ex) { continue; }
            ConfigurationSection fs = ps.getConfigurationSection(k);
            if (fs == null) continue;
            for (String f : fs.getKeys(false)) book.put(id, f, fs.getInt(f + ".shows", 0), fs.getBoolean(f + ".ok", false));
        }
        book.saved();
    }

    void savePlayers() {
        YamlConfiguration y = new YamlConfiguration();
        y.options().setHeader(List.of("ExcelTips progress: player id -> tip -> shows (times shown) and ok (said OK). Edit with /tips reset."));
        for (var e : book.all().entrySet()) for (var f : e.getValue().entrySet()) {
            String base = "players." + e.getKey() + "." + f.getKey();
            y.set(base + ".shows", f.getValue().shows);
            y.set(base + ".ok", f.getValue().acked);
        }
        try {
            getDataFolder().mkdirs();
            File tmp = new File(getDataFolder(), "players.yml.tmp");
            y.save(tmp);
            Files.move(tmp.toPath(), playersFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            book.saved();
        } catch (Exception ex) { getLogger().warning("players.yml save failed: " + ex.getMessage()); }
    }

    // ------------------------------------------------------------- commands

    private void say(CommandSender s, String m) { s.sendMessage(legacy(m)); }

    @Override public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        boolean admin = s.hasPermission("exceltips.admin");
        String sub = a.length == 0 ? "" : a[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "" -> { if (s instanceof Player p) list(p); else help(s, admin); }
            case "ok", "okay", "hide" -> {
                if (!(s instanceof Player p)) { say(s, "&cPlayers only."); return true; }
                String f = a.length > 1 ? TipBook.norm(a[1]) : book.lastShown(p.getUniqueId());
                if (f == null) { say(p, hintColor + "No tip to hide right now. /tips shows all your tips."); return true; }
                if (!defs.containsKey(f)) { say(p, "&cNo tip called " + f + ". /tips shows them all."); return true; }
                ackLoud(p, f);
            }
            case "show", "read" -> {
                if (!(s instanceof Player p)) { say(s, "&cPlayers only."); return true; }
                if (a.length < 2 || !defs.containsKey(TipBook.norm(a[1])) || !(admin || active(defs.get(TipBook.norm(a[1]))))) { say(p, "&eUse /tips show <tip>. /tips lists them."); return true; }
                show(p, defs.get(TipBook.norm(a[1])), false);
            }
            case "help" -> help(s, admin);
            case "reset" -> {
                if (!admin) { noPerm(s); return true; }
                if (a.length < 2) { say(s, "&eUse /tips reset <player> [tip]"); return true; }
                UUID id = resolve(a[1]);
                if (id == null) { say(s, "&cNo player called " + a[1] + " has played here."); return true; }
                String f = a.length > 2 ? TipBook.norm(a[2]) : null;
                if (f != null && !defs.containsKey(f) && book.state(id, f) == null) { say(s, "&cNo tip called " + f + "."); return true; }
                int n = book.reset(id, f);
                pendingForm.remove(id);
                savePlayers();
                say(s, "&aReset " + (f == null ? "all tips (" + n + " on file)" : "tip " + f + (n == 0 ? " (was not on file)" : "")) + " for " + a[1] + ".");
            }
            case "status" -> {
                if (!admin) { noPerm(s); return true; }
                if (a.length < 2) { say(s, "&eUse /tips status <player>"); return true; }
                UUID id = resolve(a[1]);
                if (id == null) { say(s, "&cNo player called " + a[1] + " has played here."); return true; }
                say(s, "&bTips for " + a[1] + ":");
                for (TipDef d : defs.values()) say(s, " &f" + d.id + " &7" + stateText(id, d) + gateText(d));
            }
            case "trigger" -> {
                if (!admin) { noPerm(s); return true; }
                if (a.length < 3) { say(s, "&eUse /tips trigger <player> <tip>"); return true; }
                Player p = Bukkit.getPlayerExact(a[1]);
                if (p == null) { say(s, "&c" + a[1] + " is not online."); return true; }
                TipDef d = defs.get(TipBook.norm(a[2]));
                if (d == null) { say(s, "&cNo tip called " + a[2] + "."); return true; }
                long now = System.currentTimeMillis();
                boolean shown = fire(p, d, "admin");
                say(s, "&7trigger " + d.id + " for " + p.getName() + ": " + (shown ? "&ashown" : "&enot shown (" + why(p.getUniqueId(), d, now) + ")"));
            }
            case "reload" -> {
                if (!admin) { noPerm(s); return true; }
                String err = loadAll();
                say(s, err == null ? "&aExcelTips reloaded: " + defs.size() + " tips." : "&c" + err);
            }
            case "check" -> {
                if (!admin) { noPerm(s); return true; }
                int warn = 0;
                for (TipDef d : defs.values()) {
                    for (String p : d.problems()) { say(s, "&e" + p); warn++; }
                    if (!active(d)) { say(s, "&7" + d.id + ":" + gateText(d) + " (not checked)"); continue; }
                    for (TipDef.Trigger t : d.triggers) {
                        if (t.type.equals("command")) for (String c : t.values) if (Bukkit.getCommandMap().getCommand(Words.label(c)) == null) { say(s, "&e" + d.id + ": command /" + c + " is not on this server"); warn++; }
                        if (t.type.equals("item-use") || t.type.equals("block-use")) for (String m : t.values) if (!m.startsWith("#") && Material.matchMaterial(m) == null) { say(s, "&e" + d.id + ": unknown material " + m); warn++; }
                    }
                }
                say(s, (warn == 0 ? "&a" : "&e") + "check: " + defs.size() + " tips (" + defs.values().stream().filter(this::active).count() + " on, " + registered.size() + " registered by plugins), " + warn + " warnings.");
            }
            default -> help(s, admin);
        }
        return true;
    }

    private String why(UUID id, TipDef d, long now) {
        if (!d.enabled) return "tip is off";
        if (book.acked(id, d.id)) return "player said OK";
        if (maxFor(d) > 0 && book.shows(id, d.id) >= maxFor(d)) return "shown " + book.shows(id, d.id) + " of " + maxFor(d);
        if (book.inCombat(id, now, combatQuietMs)) return "in combat";
        return "too soon after another tip";
    }

    /** " (tip off)", " (needs X)", " (off while X)", " (from Plugin)" or "". */
    private String gateText(TipDef d) {
        String g = "";
        if (!d.owner.isEmpty()) g += " (from " + d.owner + ")";
        if (!d.enabled) return g + " (tip off)";
        if (!d.needsPlugin.isBlank() && !pluginAtLeast(d.needsPlugin)) g += " (needs " + d.needsPlugin + ")";
        if (!d.unlessPlugin.isBlank() && pluginAtLeast(d.unlessPlugin)) g += " (off while " + d.unlessPlugin + ")";
        return g;
    }

    private String stateText(UUID id, TipDef d) {
        TipBook.State st = book.state(id, d.id);
        int max = maxFor(d);
        if (st == null) return "not seen yet";
        if (st.acked) return "hidden (said OK after " + st.shows + ")";
        if (max > 0 && st.shows >= max) return "done (shown " + st.shows + " of " + max + ")";
        return "shown " + st.shows + (max > 0 ? " of " + max : "");
    }

    private void noPerm(CommandSender s) { say(s, "&cOnly admins can do that."); }

    private UUID resolve(String name) {
        Player p = Bukkit.getPlayerExact(name);
        if (p != null) return p.getUniqueId();
        OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(name);
        return op == null ? null : op.getUniqueId();
    }

    private void list(Player p) {
        say(p, "&bYour tips &7(type /tips show <tip> to read one again):");
        List<String> names = new ArrayList<>();
        for (TipDef d : defs.values()) {
            if (!active(d) || (!d.permission.isEmpty() && !p.hasPermission(d.permission))) continue;
            TipBook.State st = book.state(p.getUniqueId(), d.id);
            String mark = st == null ? "&f" : st.acked ? "&a" : "&e";
            names.add(mark + d.id);
        }
        say(p, String.join("&7, ", names));
        say(p, hintColor + "Green = hidden, yellow = seen, white = new. /tips ok hides the last tip.");
    }

    private void help(CommandSender s, boolean admin) {
        say(s, "&b/tips &7list your tips   &b/tips ok [tip] &7hide a tip   &b/tips show <tip> &7read it again");
        if (admin) say(s, "&b/tips reset <player> [tip]  /tips status <player>  /tips trigger <player> <tip>  /tips check  /tips reload");
    }

    @Override public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        boolean admin = s.hasPermission("exceltips.admin");
        List<String> subs = new ArrayList<>(List.of("ok", "show", "help"));
        if (admin) subs.addAll(List.of("reset", "status", "trigger", "reload", "check"));
        if (a.length == 1) return filter(subs, a[0]);
        String sub = a[0].toLowerCase(Locale.ROOT);
        if (a.length == 2 && (sub.equals("ok") || sub.equals("show"))) return filter(new ArrayList<>(defs.values().stream().filter(this::active).map(d -> d.id).toList()), a[1]);
        if (a.length == 2 && admin && List.of("reset", "status", "trigger").contains(sub))
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), a[1]);
        if (a.length == 3 && admin && (sub.equals("reset") || sub.equals("trigger"))) return filter(new ArrayList<>(defs.keySet()), a[2]);
        return Collections.emptyList();
    }

    private static List<String> filter(List<String> l, String pre) {
        String p = pre.toLowerCase(Locale.ROOT);
        return l.stream().filter(x -> x.toLowerCase(Locale.ROOT).startsWith(p)).sorted().collect(Collectors.toList());
    }
}
