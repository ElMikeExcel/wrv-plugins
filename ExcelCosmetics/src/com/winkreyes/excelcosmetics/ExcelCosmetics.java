package com.winkreyes.excelcosmetics;

import com.winkreyes.excelcosmetics.core.Catalog;
import com.winkreyes.excelcosmetics.core.GrantStore;
import com.winkreyes.excelcosmetics.core.Kind;
import com.winkreyes.excelcosmetics.core.PlayerState;
import com.winkreyes.excelcosmetics.core.PurchaseService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

/**
 * ExcelCosmetics — chat colors, name effects, archetype sets and
 * the emote command pack, rented weekly with an own-it tier.
 *
 * Sales run through ExcelShops' special-product shelf, which
 * dispatches `excelcosmetics buy ...` as a console command; this
 * plugin charges Vault and records the grant as one guarded step
 * (PurchaseService: a failed charge never grants, a failed grant
 * refunds). Ownership lives in players.yml — no voucher items,
 * so nothing can be forged, lost on death, or duped.
 *
 * Expiry is silent: no messages, ever. Remaining time exists only
 * inside /cosmetics, when the player goes looking for it.
 */
public final class ExcelCosmetics extends JavaPlugin {

    private Catalog catalog;
    private StateStore stateStore;
    private VaultEconomy economy;
    private PurchaseService purchases;
    private boolean masterEnabled;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        try {
            catalog = ConfigCatalog.from(getConfig());
        } catch (IllegalArgumentException e) {
            getLogger().severe("ExcelCosmetics: " + e.getMessage());
            getLogger().severe("ExcelCosmetics: refusing to enable with a broken catalog.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        masterEnabled = getConfig().getBoolean("enabled", true);

        stateStore = new StateStore(new File(getDataFolder(), "players.yml"), getLogger());
        stateStore.load();

        economy = new VaultEconomy();
        boolean economyOk = economy.setup();
        purchases = new PurchaseService(catalog, economy, this::applyAndPersist, this::ownsForever);

        getServer().getPluginManager().registerEvents(new ChatListener(this,
                masterEnabled && getConfig().getBoolean("chat.renderer-enabled", true),
                masterEnabled && getConfig().getBoolean("chat.token-replacement", true)), this);
        getServer().getPluginManager().registerEvents(new PickerListener(this), this);

        CosmeticsCommand cosmetics = new CosmeticsCommand(this);
        PluginCommand cosmeticsCmd = getCommand("cosmetics");
        if (cosmeticsCmd != null) cosmeticsCmd.setExecutor(cosmetics);
        PluginCommand adminCmd = getCommand("excelcosmetics");
        if (adminCmd != null) adminCmd.setExecutor(new AdminCommand(this));
        EmoteCommand emotes = new EmoteCommand(this);
        for (String name : new String[]{"shrug", "hug", "tableflip", "unflip", "wave", "highfive"}) {
            PluginCommand c = getCommand(name);
            if (c != null) c.setExecutor(emotes);
        }

        // Silent housekeeping: lapsed grants are dropped from state;
        // the renderer already ignores them the instant they lapse.
        getServer().getScheduler().runTaskTimer(this, () -> {
            int removed = stateStore.store().pruneAll(System.currentTimeMillis());
            if (removed > 0) {
                getLogger().fine("ExcelCosmetics: pruned " + removed + " lapsed grant(s)");
                stateStore.save();
            }
        }, 1200L, 1200L);

        getLogger().info("ExcelCosmetics enabled: " + catalog.colors().size() + " colors, "
                + catalog.effects().size() + " effects, " + catalog.sets().size() + " sets, "
                + "emote pack ready; economy " + (economyOk ? "linked via Vault" : "NOT linked (buy route disabled, grants still work)")
                + " (Excel)");
    }

    @Override
    public void onDisable() {
        if (stateStore != null) stateStore.save();
    }

    /** The PurchaseService grant seam: apply + persist. False is
     *  the refund trigger — persistence failure must never keep
     *  the player's money. */
    private boolean ownsForever(java.util.UUID uuid, Kind kind, String id) {
        com.winkreyes.excelcosmetics.core.PlayerState s = stateStore.store().peek(uuid);
        if (s == null) return false;
        com.winkreyes.excelcosmetics.core.Grant g = s.findGrant(kind, id);
        return g != null && g.isPermanent();
    }

    /** After a successful PURCHASE (0.1.2): wear it now (a buyer expects
     *  to see what they paid for) and say so in chat. Hand grants keep
     *  the old fill-an-empty-slot rule. The message uses only exact
     *  Bedrock code colors, so Java and Bedrock (Geyser) read the same. */
    void equipAfterPurchase(java.util.UUID uuid, Kind kind, String id, int weeks, boolean own) {
        String term = own ? "own forever" : (weeks == 1 ? "1 week" : weeks + " weeks");
        String what = catalog.displayName(kind, id);
        boolean setRemoved = false;
        if (kind != Kind.EMOTES) {
            com.winkreyes.excelcosmetics.core.PlayerState s = stateStore.store().state(uuid);
            s.setEquipped(kind, id);
            // A worn set paints over a color/effect, so take the set off or
            // the buyer would not see what they just paid for (the set stays
            // owned and is one click away in /cosmetics).
            if ((kind == Kind.COLOR || kind == Kind.EFFECT)
                    && s.appearance(catalog, System.currentTimeMillis()).setId() != null) {
                s.setEquipped(Kind.SET, null);
                setRemoved = true;
            }
            stateStore.save();
        }
        Player p = getServer().getPlayer(uuid);
        if (p == null) return;
        p.sendMessage(purchaseMessage(kind, id, what, term, setRemoved));
    }

    /** "You bought Pink (1 week)! It's now on. Switch anytime with
     *  /cosmetics, or /cosmetics equip color pink." */
    Component purchaseMessage(Kind kind, String id, String what, String term, boolean setRemoved) {
        TextColor green = TextColor.color(0x55FF55);   // Bedrock &a
        TextColor yellow = TextColor.color(0xFFFF55);  // Bedrock &e
        Integer rgb = null;
        if (kind == Kind.COLOR && catalog.colors().get(id) != null) rgb = catalog.colors().get(id).rgb();
        if (kind == Kind.SET && catalog.sets().get(id) != null) rgb = catalog.sets().get(id).rgb();
        Component name = Component.text(what).color(rgb != null ? TextColor.color(rgb) : yellow);
        Component msg = Component.text("You bought ").color(green).append(name)
                .append(Component.text(" (" + term + ")! ").color(green));
        if (kind == Kind.EMOTES) {
            return msg.append(Component.text("Try /shrug /hug /tableflip /unflip /wave /highfive.").color(green));
        }
        String cmd = "/cosmetics equip " + kind.name().toLowerCase(java.util.Locale.ROOT) + " " + id;
        msg = msg.append(Component.text("It's now on. ").color(green));
        if (setRemoved) msg = msg.append(Component.text("Your set was taken off so it shows. ").color(green));
        return msg.append(Component.text("Switch anytime with ").color(green))
                .append(Component.text("/cosmetics").color(yellow))
                .append(Component.text(", or ").color(green))
                .append(Component.text(cmd).color(yellow))
                .append(Component.text(".").color(green));
    }

    private boolean applyAndPersist(java.util.UUID uuid, Kind kind, String id, int weeks, boolean own) {
        long duration = own ? 0 : catalog.prices().rentalMillis(weeks);
        stateStore.store().grant(uuid, kind, id, System.currentTimeMillis(), duration, own);
        return stateStore.save();
    }

    /** Equip + persist + tell the player (their own action, so a
     *  confirmation is right — unlike expiry, which stays silent). */
    void equipAndSave(Player player, Kind kind, String id) {
        PlayerState state = stateStore.store().state(player.getUniqueId());
        state.setEquipped(kind, id);
        stateStore.save();
        player.sendMessage(Component.text("Equipped " + catalog.displayName(kind, id) + ".")
                .color(TextColor.color(0x7DFF9E)));
    }

    List<PickerModel.Entry> pickerEntries(java.util.UUID uuid) {
        return PickerModel.entries(catalog, stateStore.store().peek(uuid), System.currentTimeMillis());
    }

    void reloadAll() {
        reloadConfig();
        try {
            catalog = ConfigCatalog.from(getConfig());
        } catch (IllegalArgumentException e) {
            getLogger().severe("ExcelCosmetics reload: " + e.getMessage() + " — keeping the previous catalog");
            return;
        }
        masterEnabled = getConfig().getBoolean("enabled", true);
        purchases = new PurchaseService(catalog, economy, this::applyAndPersist, this::ownsForever);
        stateStore.store().clear();
        stateStore.load();
        getLogger().info("ExcelCosmetics reloaded (catalog + state). Chat listener keeps the startup toggles; restart to change them.");
    }

    Catalog catalog() { return catalog; }
    GrantStore store() { return stateStore.store(); }
    StateStore stateStore() { return stateStore; }
    PurchaseService purchases() { return purchases; }
    boolean masterEnabled() { return masterEnabled; }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
