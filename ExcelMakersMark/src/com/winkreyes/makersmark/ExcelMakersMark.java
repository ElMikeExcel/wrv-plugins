package com.winkreyes.makersmark;

import com.winkreyes.makersmark.core.Gear;
import com.winkreyes.makersmark.core.NameClean;
import com.winkreyes.makersmark.core.Trophies;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * ExcelMakersMark (Wink Reyes Valley): crafted gear gets a gray "Crafted by <name>" line rebuilt from an item tag,
 * plus optional kill, hits-blocked and boss-trophy counters. Skips Magic (elmakers) items, EliteMobs (MagmaGuy)
 * items and ExcelShops items.
 */
public final class ExcelMakersMark extends JavaPlugin {
    private Marks marks;
    private Prefs prefs;
    private Tips tips;
    private Bedrock bedrock;
    private StatsHooks stats;
    private HistoryRecoveryService historyRecovery;
    private OrdinaryHistoryRecorder historyRecorder;
    private MagicPhysicalItems magicPhysicalItems;
    private MakerMagicBridge makerMagicBridge;
    private HistoryOnlyMagicRegistration historyOnlyRegistration;
    private SharedHistoryMagicRegistration sharedHistoryRegistration;
    private final Set<String> excludedNs = new HashSet<>();
    private final Map<String, String> bossLabels = new HashMap<>();
    private final List<Method> magicChecks = new ArrayList<>();
    private Plugin magic;
    private ZoneId zone;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        marks = new Marks(this);
        prefs = new Prefs(this);
        bedrock = new Bedrock(getLogger());
        tips = new Tips(this);
        loadSettings();
        if(getConfig().getBoolean("experimental.magic-history",false)) {
            Plugin installed=getServer().getPluginManager().getPlugin("Magic");
            try{
                if(!(installed instanceof com.elmakers.mine.bukkit.magic.MagicPlugin nativeMagic))throw new IllegalStateException("Magic is unavailable");
                Class.forName("wrv.magic.evidence.WandLoreBridge",false,installed.getClass().getClassLoader());
                Class.forName("wrv.magic.evidence.EffectObserver",false,installed.getClass().getClassLoader());
                magicPhysicalItems=new MagicPhysicalItems(nativeMagic);
            }catch(ReflectiveOperationException|LinkageError|RuntimeException missing){getLogger().severe("Magic history unavailable: verified native bridge missing. Ordinary gameplay remains enabled: "+missing.getClass().getSimpleName());}
        }
        stats = new StatsHooks(this);
        getServer().getPluginManager().registerEvents(new CraftHooks(this), this);
        getServer().getPluginManager().registerEvents(new AcquisitionHooks(this), this);
        getServer().getPluginManager().registerEvents(stats, this);
        stats.start();
        if ((getConfig().getBoolean("experimental.history-recovery", false) || getConfig().getBoolean("experimental.ordinary-history", false))) {
            try { historyRecovery = new HistoryRecoveryService(this); historyRecovery.start(); if(getConfig().getBoolean("experimental.ordinary-history", false)) {historyRecorder = new OrdinaryHistoryRecorder(this, historyRecovery.ledger());historyRecorder.resume();
                getServer().getPluginManager().registerEvents(new IdentityProvisioningHooks(this),this);
                getServer().getPluginManager().registerEvents(new ObservedGearMergeHooks(this,historyRecovery.ledger()),this);
                Bukkit.getScheduler().runTask(this,()->{for(Player player:Bukkit.getOnlinePlayers())if(counts(player))OwnedSlotIdentity.provision(player,this);});
            } }
            catch (Exception e) { getLogger().severe("History recovery unavailable; archive left untouched: " + e.getMessage()); }
        }
        if(magicPhysicalItems!=null&&historyRecorder!=null)try{makerMagicBridge=new MakerMagicBridge(this);getServer().getPluginManager().registerEvents(makerMagicBridge,this);Bukkit.getScheduler().runTask(this,()->{for(Player player:Bukkit.getOnlinePlayers())makerMagicBridge.provision(player);});}catch(LinkageError|RuntimeException failure){makerMagicBridge=null;magicPhysicalItems=null;getLogger().severe("Magic history bridge could not start; ordinary gameplay remains enabled: "+failure); }
        boolean sharedHistory=getConfig().getBoolean("experimental.magic-shared-history-provider",false);
        boolean soleHistory=getConfig().getBoolean("experimental.magic-history-only-owner",false);
        if(sharedHistory&&soleHistory) {
            getLogger().severe("Magic history ownership conflict: shared and history-only both enabled. Neither owner started.");
        } else if(sharedHistory) {
            if(makerMagicBridge==null) getLogger().severe("Configured shared Magic history is unavailable; no fallback owner started.");
            else try {
                sharedHistoryRegistration=new SharedHistoryMagicRegistration(this,(com.elmakers.mine.bukkit.magic.MagicPlugin)getServer().getPluginManager().getPlugin("Magic"),makerMagicBridge);
                sharedHistoryRegistration.start();
            } catch(LinkageError|RuntimeException failure) {
                if(sharedHistoryRegistration!=null)sharedHistoryRegistration.close();
                sharedHistoryRegistration=null;
                getLogger().severe("Configured shared Magic history is inactive; no fallback: "+failure);
            }
        } else if(makerMagicBridge!=null&&soleHistory) {
            historyOnlyRegistration=new HistoryOnlyMagicRegistration(this,(com.elmakers.mine.bukkit.magic.MagicPlugin)getServer().getPluginManager().getPlugin("Magic"),makerMagicBridge);
            historyOnlyRegistration.start();
        }

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try { getLogger().info("PlaceholderAPI placeholders %makersmark_mark% / %makersmark_stats%: " + (PapiHook.register(this) ? "registered" : "FAILED")); }
            catch (Throwable t) { getLogger().warning("PlaceholderAPI hook failed: " + t); }
        }
        getLogger().info("ExcelMakersMark " + getPluginMeta().getVersion() + " enabled (signing " + (markEnabled() ? "on" : "off")
                + ", stats " + (statsEnabled() ? "on" : "off") + ", Magic check " + (magicChecks.isEmpty() ? "off" : "on")
                + ", tips via " + (tips.viaExcelTips() ? "ExcelTips" : "built-in") + ")");
    }

    @Override
    public void onDisable() {
        if(sharedHistoryRegistration!=null)sharedHistoryRegistration.close();
        if(historyOnlyRegistration!=null)historyOnlyRegistration.close();
        if(makerMagicBridge!=null)makerMagicBridge.close();
        if (historyRecorder != null) historyRecorder.close();
        if (historyRecovery != null) try { historyRecovery.close(); } catch (Exception e) { getLogger().severe("History archive close failed: " + e.getMessage()); }
        if (stats != null) stats.flushAll();
        if (prefs != null) { prefs.changed(); prefs.save(); }
    }

    /** Single coordinated Magic bootstrap consumes this object as both Sink and Decorator; no registration occurs here. */
    public Object magicHistoryBridge(){return makerMagicBridge;}
    private boolean nativeHistoryClear(ItemStack item){return historyRecorder!=null&&!historyRecorder.nativeQuarantined(com.elmakers.mine.bukkit.wand.Wand.getWandId(item));}
    boolean isHistoryWand(ItemStack item){return magicPhysicalItems!=null&&MagicPhysicalItems.wand(item);}
    boolean historyWorkstationEligible(ItemStack item){return eligible(item)||(magicPhysicalItems!=null&&magicNamespaceSafe(item)&&nativeHistoryClear(item)&&magicPhysicalItems.detached(item));}
    boolean historyMagicEdit(ItemStack input,ItemStack result){
        if(magicPhysicalItems==null||!magicNamespaceSafe(input)||!magicNamespaceSafe(result)||!magicPhysicalItems.detached(input)||!MagicPhysicalItems.wand(result))return false;
        String before=com.elmakers.mine.bukkit.wand.Wand.getWandId(input),after=com.elmakers.mine.bukkit.wand.Wand.getWandId(result);
        var killReceipt=new NamespacedKey(this,"magic_kill_receipt_v1");
        try{String oldReceipt=input.getItemMeta().getPersistentDataContainer().get(killReceipt,org.bukkit.persistence.PersistentDataType.STRING),newReceipt=result.getItemMeta().getPersistentDataContainer().get(killReceipt,org.bukkit.persistence.PersistentDataType.STRING);
            if(!java.util.Objects.equals(oldReceipt,newReceipt)||oldReceipt!=null&&!java.util.UUID.fromString(oldReceipt).toString().equals(oldReceipt))return false;
        }catch(RuntimeException malformed){return false;}
        Marks.Data a=marks.read(input),b=marks.read(result);
        return before!=null&&before.equals(after)&&a.id!=null&&a.id.equals(b.id)&&a.mob==b.mob&&a.pvp==b.pvp&&a.hits==b.hits&&a.creditUnits()==b.creditUnits()&&a.slain.equals(b.slain)&&java.util.Objects.equals(a.by,b.by)&&java.util.Objects.equals(a.name,b.name)&&java.util.Objects.equals(a.date,b.date)&&java.util.Objects.equals(a.foundBy,b.foundBy)&&java.util.Objects.equals(a.foundName,b.foundName)&&java.util.Objects.equals(a.foundDate,b.foundDate);
    }
    boolean historyEligible(Player owner,int slot,ItemStack item){return eligible(item)||(magicPhysicalItems!=null&&magicNamespaceSafe(item)&&nativeHistoryClear(item)&&magicPhysicalItems.supported(owner,slot,item));}
    private boolean magicNamespaceSafe(ItemStack item){
        if(item==null||!item.hasItemMeta())return false;
        for(var key:item.getItemMeta().getPersistentDataContainer().getKeys())if(excludedNs.contains(key.getNamespace())&&!key.getNamespace().equals("magic"))return false;
        return true;
    }
    void historyReplace(Player owner,int slot,ItemStack before,ItemStack after){
        if(magicPhysicalItems!=null&&MagicPhysicalItems.wand(before))magicPhysicalItems.replace(owner,slot,before,after);
        else {if(!eligible(before)||!before.equals(owner.getInventory().getItem(slot)))throw new IllegalStateException("History slot changed");owner.getInventory().setItem(slot,after);}
    }
    boolean historyMagicRedraw(Player owner,int slot,ItemStack item){if(magicPhysicalItems==null||!MagicPhysicalItems.wand(item))return false;magicPhysicalItems.redraw(owner,slot,item);return true;}

    void loadSettings() {
        reloadConfig();
        excludedNs.clear();
        for (String s : getConfig().getStringList("exclude.pdc-namespaces")) excludedNs.add(s.toLowerCase(Locale.ROOT));
        excludedNs.remove(getName().toLowerCase(Locale.ROOT)); // never exclude our own tags
        bossLabels.clear();
        ConfigurationSection b = getConfig().getConfigurationSection("trophies.bosses");
        if (b != null) for (String k : b.getKeys(false)) bossLabels.put(k.toUpperCase(Locale.ROOT), b.getString(k, Trophies.pretty(k)));
        if (getConfig().getBoolean("trophies.raid-captain.enabled", false)) bossLabels.put("RAID_CAPTAIN", getConfig().getString("trophies.raid-captain.label", "Raid Captain"));
        for (Map<?, ?> m : getConfig().getMapList("trophies.named.list")) {
            Object id = m.get("id"), label = m.get("label");
            if (id != null) bossLabels.put("NAME:" + id.toString().toUpperCase(Locale.ROOT), label == null ? Trophies.pretty(id.toString()) : label.toString());
        }
        magicChecks.clear();
        magic = getServer().getPluginManager().getPlugin("Magic");
        if (magic != null && getConfig().getBoolean("exclude.magic-items", true)) {
            for (String n : List.of("isWand", "isUpgrade", "isSpell", "isBrush")) {
                try { magicChecks.add(magic.getClass().getMethod(n, ItemStack.class)); } catch (NoSuchMethodException ignored) { }
            }
        }
        String z = getConfig().getString("date-time-zone", "America/Chicago");
        try { zone = ZoneId.of(z); } catch (Exception e) { zone = ZoneId.systemDefault(); }
        if (prefs != null) prefs.load();
    }

    // ---- shared helpers ----
    OrdinaryHistoryRecorder historyRecorder(){ return historyRecorder; }
    Marks marks() { return marks; }
    Prefs prefs() { return prefs; }
    Tips tips() { return tips; }
    Bedrock bedrock() { return bedrock; }
    boolean markEnabled() { return getConfig().getBoolean("mark.enabled", true); }
    boolean statsEnabled() { return getConfig().getBoolean("stats.enabled", true); }
    int wrapAt() { return getConfig().getInt("trophies.wrap-at", 40); }
    boolean signs(Player p) { return markEnabled() && prefs.get(p.getUniqueId()).mark; }
    boolean counts(Player p) { return statsEnabled() && prefs.get(p.getUniqueId()).stats; }
    boolean showsStats(Player p) { return p == null ? statsEnabled() : counts(p); }
    String today() { return LocalDate.now(zone).toString(); }
    Map<String, String> bossLabels() { return bossLabels; }

    String trophyLabel(String id) { String l = bossLabels.get(id.toUpperCase(Locale.ROOT)); return l != null ? l : Trophies.pretty(id); }

    /** Nickname at craft time (EssentialsX), without the '~' nickname prefix or the Floodgate '.' prefix. */
    String crafterName(Player p) {
        String nick = null;
        Plugin ess = getServer().getPluginManager().getPlugin("Essentials");
        if (ess != null && ess.isEnabled()) {
            try {
                Object user = ess.getClass().getMethod("getUser", Player.class).invoke(ess, p);
                if (user != null) nick = (String) user.getClass().getMethod("getNickname").invoke(user);
            } catch (Throwable ignored) { }
        }
        return NameClean.clean(nick, p.getName(), getConfig().getStringList("names.strip-prefixes"), getConfig().getInt("names.max-length", 24));
    }

    /** Eligible: allowlisted non-stackable gear that is not a Magic, EliteMobs or shop item. */
    boolean eligible(ItemStack it) {
        if (it == null || it.getType().isAir() || it.getAmount() != 1) return false;
        if (!Gear.markable(it.getType().name(), it.getMaxStackSize())) return false;
        return !excluded(it);
    }

    boolean excluded(ItemStack it) {
        if (it.hasItemMeta()) {
            ItemMeta m = it.getItemMeta();
            for (NamespacedKey k : m.getPersistentDataContainer().getKeys()) if (excludedNs.contains(k.getNamespace())) return true;
        }
        for (Method mm : magicChecks) {
            try { if (Boolean.TRUE.equals(mm.invoke(magic, it))) return true; } catch (Throwable ignored) { }
        }
        return false;
    }

    // ---- /mark ----
    /** Claim only an exact, successfully acquired weapon supplied by AcquisitionHooks.
     * Never scans inventories, invents earlier ownership, or replaces existing provenance. */
    boolean claimFound(Player p, ItemStack item) {
        if (p == null || !p.isOnline() || !p.hasPermission("excelmakersmark.use") || !signs(p)) return false;
        if (p.getGameMode() != org.bukkit.GameMode.SURVIVAL && p.getGameMode() != org.bukkit.GameMode.ADVENTURE) return false;
        if (!eligible(item) || !Gear.weapon(item.getType().name())) return false;
        Marks.Data d = marks.read(item);
        if (d.hasProvenance()) return false;
        d.foundBy = p.getUniqueId().toString();
        d.foundName = crafterName(p);
        d.foundDate = today();
        if (d.id == null) d.id = java.util.UUID.randomUUID().toString(); // This path runs only after confirmed acquisition.
        marks.apply(item, d, showsStats(p));
        return true;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        String sub = a.length == 0 ? "" : a[0].toLowerCase(Locale.ROOT);
        boolean admin = s.hasPermission("excelmakersmark.admin");
        if (sub.equals("history") && a.length>1 && a[1].equalsIgnoreCase("page")) {
            if(!(s instanceof Player p) || historyRecorder==null){s.sendMessage("Ordinary weapon history is disabled or unavailable.");return true;}
            try{String id=marks.read(p.getInventory().getItemInMainHand()).id;if(id==null)throw new IllegalArgumentException("No tracked item in main hand");int page=a.length>2?Integer.parseInt(a[2])-1:0;historyRecorder.page(p,java.util.UUID.fromString(id),page);}catch(Exception e){s.sendMessage("Hold your tracked weapon, then use /mark history page [number].");}return true;
        }
        if (sub.equals("history")) {
            if (historyRecovery == null) { s.sendMessage("Experimental history recovery is disabled or unavailable. No item changes were made."); return true; }
            return historyRecovery.command(s, a);
        }
        if (admin) {
            switch (sub) {
                case "reload" -> { loadSettings(); if (stats != null) stats.clearNamed(); s.sendMessage("ExcelMakersMark reloaded. Bosses tracked: " + bossLabels.keySet()); return true; }
                case "inspect" -> { inspect(s, a); return true; }
                case "tipreset" -> {
                    if (a.length < 2) { s.sendMessage("/mark tipreset <player>"); return true; }
                    OfflinePlayer op = Bukkit.getOfflinePlayer(a[1]);
                    tips.reset(op.getUniqueId());
                    s.sendMessage("Tip reset for " + a[1]);
                    return true;
                }
                default -> { }
            }
        }
        if (!(s instanceof Player p)) { s.sendMessage("Players only (admin: /mark reload | inspect <player> [slot] | tipreset <player>)."); return true; }
        Prefs.P pr = prefs.get(p.getUniqueId());
        switch (sub) {
            case "on", "off", "toggle" -> {
                pr.mark = sub.equals("toggle") ? !pr.mark : sub.equals("on");
                prefs.changed();
                say(p, pr.mark ? "Maker's mark is ON. Crafts are signed and unmarked weapons you acquire get 'Found by' you." : "Maker's mark is OFF. New crafts and acquired weapons will not receive your name.", pr.mark);
            }
            case "stats" -> {
                String v = a.length > 1 ? a[1].toLowerCase(Locale.ROOT) : "toggle";
                if (!v.equals("on") && !v.equals("off") && !v.equals("toggle")) { p.sendMessage(Component.text("Use /mark stats on or /mark stats off", NamedTextColor.GRAY)); return true; }
                pr.stats = v.equals("toggle") ? !pr.stats : v.equals("on");
                prefs.changed();
                stats.redrawInventory(p);
                say(p, pr.stats ? "Gear stats are ON. Weapons count kills and bosses, armor counts hits blocked." : "Gear stats are OFF. Nothing is counted and the lines are hidden on your gear.", pr.stats);
            }
            case "ok" -> { tips.ack(p, true); return true; }
            case "menu" -> { if (!openForm(p)) chatStatus(p, pr); }
            default -> {
                // Bedrock: /mark (also from a compass button) opens a two-switch pop-up. Java gets the chat lines.
                if (sub.isEmpty() && openForm(p)) break;
                chatStatus(p, pr);
            }
        }
        tips.use(p);
        return true;
    }

    /** Bedrock toggle pop-up: tap a switch to flip it. Returns false when no form could be sent (Java, no Geyser). */
    private boolean openForm(Player p) {
        if (!getConfig().getBoolean("bedrock-toggle-form", true) || !Bedrock.isBedrock(p)) return false;
        Prefs.P pr = prefs.get(p.getUniqueId());
        boolean m = markEnabled() && pr.mark, st = counts(p);
        String content = "Crafted by: your name on the tools, weapons and armor you craft.\n\n"
                + "Found by: the first tracked taker of an unmarked weapon, including older chest items.\n\nGear stats: weapons count kills and bosses, armor counts hits blocked.\n\nTap a switch to flip it.";
        java.util.List<String> btn = java.util.List.of(
                "Maker's mark: " + (m ? "ON" : "OFF") + "\nTap to turn " + (m ? "off" : "on"),
                "Gear stats: " + (st ? "ON" : "OFF") + "\nTap to turn " + (st ? "off" : "on"),
                "Close");
        return bedrock.simple(p, "Maker's Mark settings", content, btn, clicked -> Bukkit.getScheduler().runTask(this, () -> {
            if (!p.isOnline()) return;
            if (clicked == 0) p.performCommand("mark " + (m ? "off" : "on"));
            else if (clicked == 1) p.performCommand("mark stats " + (st ? "off" : "on"));
        }));
    }

    private void chatStatus(Player p, Prefs.P pr) {
                p.sendMessage(Component.text("Maker's mark: ", NamedTextColor.GOLD).append(Component.text(markEnabled() && pr.mark ? "ON" : "OFF", markEnabled() && pr.mark ? NamedTextColor.GREEN : NamedTextColor.RED))
                        .append(Component.text("   Gear stats: ", NamedTextColor.GOLD)).append(Component.text(counts(p) ? "ON" : "OFF", counts(p) ? NamedTextColor.GREEN : NamedTextColor.RED)));
                p.sendMessage(Component.text("/mark on | off   sign crafts and claim unmarked weapons you acquire", NamedTextColor.GRAY));
                p.sendMessage(Component.text("/mark stats on | off   count kills, bosses and hits blocked", NamedTextColor.GRAY));
    }

    private void say(Player p, String msg, boolean on) { p.sendMessage(Component.text(msg, on ? NamedTextColor.GREEN : NamedTextColor.YELLOW)); }

    private void inspect(CommandSender s, String[] a) {
        Player t = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : (s instanceof Player p ? p : null);
        if (t == null) { s.sendMessage("/mark inspect <player> [slot|head|chest|legs|feet|offhand]"); return; }
        ItemStack it;
        String where = a.length > 2 ? a[2].toLowerCase(Locale.ROOT) : "hand";
        switch (where) {
            case "hand" -> it = t.getInventory().getItemInMainHand();
            case "head" -> it = t.getInventory().getHelmet();
            case "chest" -> it = t.getInventory().getChestplate();
            case "legs" -> it = t.getInventory().getLeggings();
            case "feet" -> it = t.getInventory().getBoots();
            case "offhand" -> it = t.getInventory().getItemInOffHand();
            default -> { try { it = t.getInventory().getItem(Integer.parseInt(where)); } catch (Exception e) { it = null; } }
        }
        if (it == null || it.getType().isAir()) { s.sendMessage("[MM-INSPECT] " + t.getName() + " " + where + " empty"); return; }
        Marks.Data d = marks.read(it);
        List<String> lore = new ArrayList<>();
        if (it.hasItemMeta() && it.getItemMeta().lore() != null)
            for (Component c : it.getItemMeta().lore()) lore.add(PlainTextComponentSerializer.plainText().serialize(c));
        s.sendMessage("[MM-INSPECT] " + t.getName() + " " + where + " " + it.getType() + " x" + it.getAmount()
                + " by=" + d.by + " name=" + d.name + " date=" + d.date + " id=" + (d.id != null) + " mob=" + d.mob + " pvp=" + d.pvp
                + " hits=" + d.hits + " slain=" + Trophies.serialize(d.slain) + " excluded=" + excluded(it) + " lore=" + lore
                + " dmg=" + (it.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable dm ? dm.getDamage() : -1));
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] a) {
        List<String> opts = new ArrayList<>();
        if (a.length == 1) {
            opts.addAll(List.of("on", "off", "stats", "ok", "menu"));
            if (s.hasPermission("excelmakersmark.admin")) opts.addAll(List.of("reload", "inspect", "tipreset"));
        } else if (a.length == 2 && a[0].equalsIgnoreCase("stats")) opts.addAll(List.of("on", "off"));
        else if (a.length == 2 && s.hasPermission("excelmakersmark.admin")) return null;
        String pre = a[a.length - 1].toLowerCase(Locale.ROOT);
        opts.removeIf(o -> !o.startsWith(pre));
        return opts;
    }
}
