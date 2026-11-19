package com.winkreyes.exceldowned;

import com.winkreyes.excelitems.api.ExcelItemsAPI;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ExcelDowned — lethal damage becomes a DOWNED state instead of death,
 * and the Resurrection spell (Magic) brings a downed party member back
 * at 50% health. See DESIGN.md (~/workspace/downed-revive/).
 *
 * The registry is keyed by UUID and persisted on every transition, so
 * logout and server restart neither extend nor shorten a bleed-out —
 * the deadline is a wall-clock timestamp.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class ExcelDowned extends JavaPlugin {

    // settings (config.yml)
    long bleedOutMs;
    double downedDamageFactor;
    double damageSecondsPerPoint;
    boolean damageEatsWindow;
    double downedHealth;
    double reviveHealthFraction;
    double gateRange;
    double reviveRange;
    long giveUpWindowMs;
    double scrubRadius;
    int slownessAmplifier;
    // revive-potion: section (1.0.0, spec A section 4; CONFIG RULE —
    // Mike, Nov 17, 2026: every tunable lives in config.yml)
    boolean revivePotionEnabled;
    boolean revivePotionPromptEnabled;
    double revivePotionPromptRange;
    double revivePotionConfirmRange;
    long revivePotionUseCooldownMs;
    long revivePotionZeroHintCooldownMs;
    int revivePotionPrice;
    // revive-potion purchase keys (1.0.0 charge leg, W6 section 5;
    // CONFIG RULE — every purchase tunable lives in config.yml)
    long reviveBuyConfirmWindowMs;
    long reviveBuySuccessCooldownMs;
    boolean reviveBuyRequireEmptySlot;
    // cleric-revive: section (1.0.0, design B; CONFIG RULE — every
    // tunable lives in config.yml; cooldown/unlock per D108)
    boolean clericReviveEnabled;
    double clericReviveGraceCost;
    long clericReviveCooldownMs;
    double clericReviveRange;
    int clericReviveUnlockLevel;
    boolean clericReviveRequireParty;

    private final Map<UUID, DownedSession> sessions = new ConcurrentHashMap<>();
    private final Set<UUID> bypassDeath = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String[]> savedNames = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastReviveByCaster = new ConcurrentHashMap<>();
    private TogetherPartyProvider party;
    private GiveUpUi giveUpUi;
    private RevivePotionUi revivePotionUi;
    private ClericRevive clericRevive;
    private File stateFile;
    // shop charge leg (W6): Vault is a SOFT depend — the economy
    // reference stays null when Vault is absent/disabled and only
    // purchases stand down; core Downed + the admin give mint are
    // never affected (the revive mechanic must not die with the
    // economy).
    private Economy economy;
    private RevivePurchaseJournal purchaseJournal;
    private RevivePurchaseCoordinator purchases;
    private RevivePurchaseUi revivePurchaseUi;

    /**
     * 0.1.3 (Defect 2 fix shape 2): register the 'Resurrect' Magic action
     * at LOAD time — before Magic's onEnable loads its spell templates —
     * so the resurrection template can resolve its action class at load
     * instead of failing lazily at cast/grant time. The onEnable call
     * remains as a second attempt (registration is idempotent-shaped:
     * a duplicate registration failure is caught and logged by the
     * bridge, and the CastEvent route still carries the spell).
     */
    @Override
    public void onLoad() {
        MagicBridge.registerAction(getLogger());
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        party = new TogetherPartyProvider(getLogger(),
                getConfig().getBoolean("party.fallback-all-online", false));
        stateFile = new File(getDataFolder(), "downed.yml");
        loadState();
        getServer().getPluginManager().registerEvents(new DownedListener(this), this);
        giveUpUi = new GiveUpUi(this);
        getServer().getPluginManager().registerEvents(giveUpUi, this);
        revivePotionUi = new RevivePotionUi(this);
        getServer().getPluginManager().registerEvents(revivePotionUi, this);
        // Shop charge leg (W6 sections 3.3/4): soft Vault wiring +
        // the purchase coordinator/journal. Any failure here disables
        // purchases only — never Downed itself.
        initializePurchases();
        revivePurchaseUi = new RevivePurchaseUi(this);
        getServer().getPluginManager().registerEvents(revivePurchaseUi, this);
        // M1 migration (1.0.1, P-ITEM-REGISTRY-1): register the
        // revive behavior handler against registry ID 4. The handler
        // BODY stays in this plugin (RevivePotionUi / the walk-up
        // revive flow) — ExcelItems keeps only the routing table
        // (design law: core stays small, stable, boring). Guarded
        // fail-soft like the Cleric seam: a registration failure
        // warns and Downed still enables (identification itself has
        // its own legacy fallback in RevivePotionItem).
        try {
            ExcelItemsAPI.get().registerBehavior(RevivePotionItem.REGISTRY_ID,
                    () -> "exceldowned:revive-potion");
            getLogger().info("ExcelDowned: registered revive behavior for ExcelItems ID "
                    + RevivePotionItem.REGISTRY_ID + " (exceldowned:revive-potion).");
        } catch (Throwable t2) {
            getLogger().warning("ExcelDowned: ExcelItems behavior registration failed ("
                    + t2.getClass().getSimpleName() + ": " + t2.getMessage()
                    + ") — revive identification keeps its legacy fallback; Downed is unaffected.");
        }
        // Cleric "Raise Ally" (design B; D107): the EM seam is built
        // behind a Throwable guard — if EliteMobs is absent or its
        // internals moved, the gate is null, the ability reports
        // UNAVAILABLE, and Downed itself is unaffected (softdepend).
        ClericGate clericGate = null;
        try {
            clericGate = new EmClericGate(getLogger());
        } catch (Throwable t) {
            getLogger().warning("ExcelDowned: Raise Ally gate unavailable at enable ("
                    + t.getClass().getSimpleName() + ": " + t.getMessage()
                    + ") — the ability stands down; Downed is unaffected.");
        }
        clericRevive = new ClericRevive(this, clericGate);
        MagicBridge bridge = new MagicBridge(this);
        getServer().getPluginManager().registerEvents(bridge, this);
        bridge.tryRegisterAction();
        scheduleSpellTemplateCheck();
        DownedCommands cmds = new DownedCommands(this);
        if (getCommand("giveup") != null) getCommand("giveup").setExecutor(cmds);
        if (getCommand("downed") != null) getCommand("downed").setExecutor(cmds);
        if (getCommand("raiseally") != null) getCommand("raiseally").setExecutor(cmds);
        // state task: slowness refresh, action bar, bleed-out clock (1 s)
        Bukkit.getScheduler().runTaskTimer(this, this::stateTick, 20L, 20L);
        // deaggro scrub (2 s): clear any mob found targeting a downed player
        Bukkit.getScheduler().runTaskTimer(this, this::scrubTick, 40L, 40L);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isDowned(p.getUniqueId())) applyDownedState(p);
        }
        getLogger().info("ExcelDowned " + getDescription().getVersion() + " ready — build "
                + BuildInfo.BUILD_ID + "; sessions restored: " + sessions.size()
                + "; bleed-out " + (bleedOutMs / 1000) + "s; downed factor " + downedDamageFactor);
    }

    @Override
    public void onDisable() {
        if (giveUpUi != null) giveUpUi.shutdown();
        if (revivePotionUi != null) revivePotionUi.shutdown();
        if (revivePurchaseUi != null) revivePurchaseUi.shutdown();
        if (clericRevive != null) clericRevive.shutdown();
        // The purchase journal is append-only and forced durable on
        // every append; there is nothing to flush — its unresolved
        // (dirty) state is exactly what the next boot re-reads and
        // locks on (W6 section 4).
        saveState();
    }

    /**
     * Boot self-check — the two spell keys Downed depends on
     * (resurrection, grouprenewal) must resolve to live Magic
     * templates at startup. A dead template is a boot WARNING naming
     * the key, never a mid-fight discovery. The probe is reflective
     * (the live controller's shape is not assumed); a probe that
     * cannot run is itself a WARNING, never a silent pass (the
     * pre-fix gate probe failed open — that shape is not repeated).
     *
     * 0.1.5 (Defect-2 template leg, diagnosis board-r427): Magic
     * loads its spell registry in a DEFERRED phase that completes
     * ~7 s after this plugin's onEnable, so the pre-0.1.5 synchronous
     * probe always read an empty registry and WARNed falsely on
     * every boot. The check now runs on a delayed task (+10 s, with
     * bounded retries at +20 s / +40 s) and probes the known-good
     * builtin control key "heal" alongside the targets
     * (TemplateCheckPolicy): a null control means the registry is
     * simply not loaded yet — one INFO "deferred" line and a retry,
     * never a template WARN. Only a key that still does not resolve
     * in the POPULATED registry at the settled/final check WARNs,
     * exactly once, naming that key.
     */
    private void scheduleSpellTemplateCheck() {
        long delay = TemplateCheckPolicy.delayTicksForAttempt(0);
        Bukkit.getScheduler().runTaskLater(this, () -> runSpellTemplateCheck(0), delay);
    }

    private void runSpellTemplateCheck(int attempt) {
        String[] keys = {MagicBridge.SPELL_KEY, "grouprenewal"};
        Boolean control = probeSpellTemplate(TemplateCheckPolicy.CONTROL_KEY);
        Boolean[] targets = new Boolean[keys.length];
        for (int i = 0; i < keys.length; i++) targets[i] = probeSpellTemplate(keys[i]);
        TemplateCheckPolicy.Verdict verdict =
                TemplateCheckPolicy.verdict(control, targets);
        switch (verdict) {
            case ALL_RESOLVE -> {
                for (String key : keys) {
                    getLogger().info("ExcelDowned: spell template '" + key + "' resolves.");
                }
            }
            case MISSING_KEYS -> {
                // The registry is populated (control resolved), so a
                // false target is genuinely absent: fail loud, once,
                // per missing key. Wording corrected in 0.1.5: since
                // ExcelMagicGate 0.3.2, grant-time verification runs
                // at wand-sync time against the populated registry —
                // the old "grants will NPE" claim is dropped.
                for (int i = 0; i < keys.length; i++) {
                    if (targets[i]) {
                        getLogger().info("ExcelDowned: spell template '" + keys[i] + "' resolves.");
                    } else {
                        getLogger().warning("ExcelDowned: spell template '" + keys[i]
                                + "' does NOT resolve in Magic's loaded spell registry "
                                + "(final self-check) — the key is absent from the loaded "
                                + "registry; check the Magic spells data for this key.");
                    }
                }
            }
            case REGISTRY_NOT_LOADED -> {
                if (TemplateCheckPolicy.shouldRetry(verdict, attempt)) {
                    getLogger().info("ExcelDowned: Magic spell registry not yet loaded "
                            + "— spell-template self-check deferred (attempt "
                            + (attempt + 1) + " of " + TemplateCheckPolicy.MAX_ATTEMPTS + ").");
                    long delay = TemplateCheckPolicy.delayTicksForAttempt(attempt + 1);
                    Bukkit.getScheduler().runTaskLater(this,
                            () -> runSpellTemplateCheck(attempt + 1), delay);
                } else {
                    getLogger().warning("ExcelDowned: Magic spell registry still not loaded "
                            + "at final spell-template self-check — template verification "
                            + "for '" + keys[0] + "' and '" + keys[1] + "' is UNPROVEN at boot.");
                }
            }
            case PROBE_UNAVAILABLE -> {
                if (TemplateCheckPolicy.shouldRetry(verdict, attempt)) {
                    getLogger().info("ExcelDowned: spell-template self-check could not probe "
                            + "Magic yet — deferred (attempt " + (attempt + 1)
                            + " of " + TemplateCheckPolicy.MAX_ATTEMPTS + ").");
                    long delay = TemplateCheckPolicy.delayTicksForAttempt(attempt + 1);
                    Bukkit.getScheduler().runTaskLater(this,
                            () -> runSpellTemplateCheck(attempt + 1), delay);
                } else {
                    for (String key : keys) {
                        getLogger().warning("ExcelDowned: spell-template self-check could not probe '"
                                + key + "' (Magic controller shape unavailable) — grant verification "
                                + "for this key is UNPROVEN at boot.");
                    }
                }
            }
        }
    }

    /** Reflective template probe. Null = probe unavailable; never throws. */
    private Boolean probeSpellTemplate(String key) {
        try {
            Object magic = Bukkit.getPluginManager().getPlugin("Magic");
            if (magic == null) return null;
            Object controller = null;
            try { controller = magic.getClass().getMethod("getController").invoke(magic); }
            catch (Throwable ignored) {}
            if (controller == null) {
                try {
                    Object api = Bukkit.getServicesManager().load(
                            Class.forName("com.elmakers.mine.bukkit.api.magic.MagicAPI"));
                    if (api != null) controller = api.getClass()
                            .getMethod("getController").invoke(api);
                } catch (Throwable ignored) {}
            }
            if (controller == null) return null;
            Object template = controller.getClass()
                    .getMethod("getSpellTemplate", String.class).invoke(controller, key);
            return template != null;
        } catch (Throwable t) {
            return null;
        }
    }

    void loadSettings() {
        bleedOutMs = getConfig().getLong("bleed-out-seconds", 300) * 1000L;
        downedDamageFactor = getConfig().getDouble("downed-damage-factor", 0.25);
        damageSecondsPerPoint = getConfig().getDouble("damage-seconds-per-point", 1.0);
        damageEatsWindow = getConfig().getBoolean("damage-eats-window", false);
        downedHealth = getConfig().getDouble("downed-health", 1.0);
        reviveHealthFraction = getConfig().getDouble("revive-health-fraction", 0.5);
        gateRange = getConfig().getDouble("gate-range", 24.0);
        reviveRange = getConfig().getDouble("revive-range", 20.0);
        giveUpWindowMs = getConfig().getLong("giveup-window-seconds", 10) * 1000L;
        scrubRadius = getConfig().getDouble("scrub-radius", 32.0);
        slownessAmplifier = getConfig().getInt("slowness-amplifier", 4);
        revivePotionEnabled = getConfig().getBoolean("revive-potion.enabled", true);
        revivePotionPromptEnabled = getConfig().getBoolean("revive-potion.prompt-enabled", true);
        revivePotionPromptRange = getConfig().getDouble("revive-potion.prompt-range", 5.0);
        revivePotionConfirmRange = getConfig().getDouble("revive-potion.confirm-range", 6.0);
        revivePotionUseCooldownMs =
                getConfig().getLong("revive-potion.use-cooldown-seconds", 600) * 1000L;
        revivePotionZeroHintCooldownMs =
                getConfig().getLong("revive-potion.zero-hint-cooldown-seconds", 5) * 1000L;
        revivePotionPrice = getConfig().getInt("revive-potion.price", 5000);
        reviveBuyConfirmWindowMs =
                getConfig().getLong("revive-potion.buy-confirm-window-seconds", 20) * 1000L;
        reviveBuySuccessCooldownMs =
                getConfig().getLong("revive-potion.buy-success-cooldown-seconds", 1) * 1000L;
        reviveBuyRequireEmptySlot =
                getConfig().getBoolean("revive-potion.buy-require-empty-slot", true);
        if (!revivePotionEnabled && revivePurchaseUi != null) {
            revivePurchaseUi.invalidateAll(); // feature-disable kills open reviews
        }
        clericReviveEnabled = getConfig().getBoolean("cleric-revive.enabled", true);
        clericReviveGraceCost = getConfig().getDouble("cleric-revive.grace-cost", 85.0);
        clericReviveCooldownMs =
                getConfig().getLong("cleric-revive.cooldown-seconds", 600) * 1000L;
        clericReviveRange = getConfig().getDouble("cleric-revive.range", 20.0);
        clericReviveUnlockLevel = getConfig().getInt("cleric-revive.unlock-em-level", 10);
        clericReviveRequireParty =
                getConfig().getBoolean("cleric-revive.require-party", false);
    }

    // ---------- registry ----------

    public boolean isDowned(UUID id) { return id != null && sessions.containsKey(id); }
    public DownedSession sessionOf(UUID id) { return id == null ? null : sessions.get(id); }

    /**
     * Find a (possibly offline) durable session by player name
     * (0.1.2, console admin): sessions persist in downed.yml across
     * logout, so an operator can clear a stuck offline session by the
     * name it was recorded under. Case-insensitive; null when absent.
     */
    public DownedSession sessionByName(String name) {
        if (name == null) return null;
        for (DownedSession s : sessions.values()) {
            if (s.playerName() != null && s.playerName().equalsIgnoreCase(name)) return s;
        }
        return null;
    }
    public PartyProvider party() { return party; }
    public GiveUpUi giveUpUi() { return giveUpUi; }
    public RevivePotionUi revivePotionUi() { return revivePotionUi; }
    public RevivePurchaseUi revivePurchaseUi() { return revivePurchaseUi; }
    public ClericRevive clericRevive() { return clericRevive; }
    Set<UUID> bypassDeath() { return bypassDeath; }

    // ---------- state transitions ----------

    /** Enter DOWNED. Returns false if already downed (exactly-once entry). */
    public boolean enterDowned(Player player, String cause) {
        UUID id = player.getUniqueId();
        if (sessions.containsKey(id)) return false;
        DownedSession s = new DownedSession(id, player.getName(), System.currentTimeMillis(),
                bleedOutMs, cause,
                player.getWorld().getName(),
                player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());
        sessions.put(id, s);
        saveState();
        if (revivePotionUi != null) revivePotionUi.invalidateCarrier(id); // a carrier who goes down loses their UI
        if (revivePurchaseUi != null) revivePurchaseUi.invalidate(id); // a buyer who goes down loses their review
        applyDownedState(player);
        clearAggroOn(player);
        record("downed_enter", baseFields(player)
                + ",\"cause\":\"" + esc(cause) + "\""
                + ",\"world\":\"" + esc(player.getWorld().getName()) + "\"");
        Bukkit.getPluginManager().callEvent(new DownedEvents.DownedEnterEvent(player, cause));
        alertParty(player, ChatColor.RED + "✝ " + player.getName() + " is DOWN — "
                + "Resurrection can bring them back (" + formatClock(s.remainingMs(now())) + ")");
        player.sendMessage(ChatColor.RED + "✝ You are DOWN. You will bleed out in "
                + formatClock(bleedOutMs) + " unless someone revives you. /giveup to let go.");
        return true;
    }

    /** Apply the visible/practical downed state to an online player. */
    public void applyDownedState(Player player) {
        if (!isDowned(player.getUniqueId())) return;
        try { player.setHealth(downedHealth); } catch (Throwable ignored) {}
        player.setFireTicks(0); // 0.1.3 hardening: a downed player does not keep burning
        player.setSprinting(false);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60,
                slownessAmplifier, false, false, true));
        savedNames.putIfAbsent(player.getUniqueId(),
                new String[]{player.getDisplayName(), player.getPlayerListName()});
        String marked = ChatColor.RED + "✝ DOWNED " + ChatColor.RESET + player.getName();
        try { player.setDisplayName(marked); player.setPlayerListName(marked); } catch (Throwable ignored) {}
    }

    private void clearDownedPresentation(Player player) {
        player.setFireTicks(0);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        String[] orig = savedNames.remove(player.getUniqueId());
        if (orig != null) {
            try { player.setDisplayName(orig[0]); player.setPlayerListName(orig[1]); }
            catch (Throwable ignored) {}
        }
    }

    /** Revive a downed player. Returns true only if this call did it. */
    public boolean revive(Player healer, Player target) {
        return reviveInternal(healer, target, null, false, 0, false);
    }

    /**
     * Cleric "Raise Ally" revive (1.0.0, design B): the SAME
     * exactly-once path the spell and the potion use, with the
     * Cleric as healer and a cleric_revive record whose source
     * names the door (D-B8 — a Cleric revive never masquerades as
     * the Magic spell). The GRACE charge and the cooldown live in
     * ClericRevive and run only when this returns true. Returns
     * true only if this call did it.
     */
    public boolean reviveByCleric(Player caster, Player target) {
        if (caster == null || target == null) return false;
        return reviveInternal(caster, target, null, false, 0, true);
    }

    /**
     * Console/operator revive (0.1.2, console-admin audit §4): the
     * SAME exactly-once revive path the Resurrection spell uses, with
     * no healer — the record and the log name the operator actor
     * instead of a spell, so an admin revive never masquerades as a
     * cast. Returns true only if this call did it.
     */
    public boolean adminRevive(String actor, Player target) {
        return reviveInternal(null, target, actor == null || actor.isBlank() ? "CONSOLE" : actor,
                false, 0, false);
    }

    /**
     * Revive Potion revive (1.0.0, spec A section 6): the SAME
     * exactly-once path, with the carrier as healer and a
     * potion_revive record. potionsRemaining is the carrier's count
     * AFTER the caller's post-landing consume (count - 1 at confirm
     * time — the consume runs on the same tick, immediately after
     * this returns true; if this returns false nothing is consumed
     * and nothing is recorded). Returns true only if this call did it.
     */
    public boolean reviveByPotion(Player carrier, Player target) {
        if (carrier == null || target == null) return false;
        int count = RevivePotionItem.countPotions(this, carrier.getInventory());
        return reviveInternal(carrier, target, null, true, Math.max(0, count - 1), false);
    }

    private boolean reviveInternal(Player healer, Player target, String adminActor,
                                   boolean potion, int potionsRemaining, boolean cleric) {
        if (target == null) return false;
        DownedSession s = sessions.remove(target.getUniqueId());
        if (s == null) return false;
        if (giveUpUi != null) giveUpUi.invalidate(target.getUniqueId()); // an open confirm dies with the down
        if (revivePotionUi != null) revivePotionUi.invalidateForTarget(target.getUniqueId());
        saveState();
        double max = target.getAttribute(Attribute.MAX_HEALTH) != null
                ? target.getAttribute(Attribute.MAX_HEALTH).getValue() : 20.0;
        double after = max * reviveHealthFraction;
        double given = after - downedHealth;
        clearDownedPresentation(target);
        try { target.setHealth(after); } catch (Throwable ignored) {}
        if (healer != null) lastReviveByCaster.put(healer.getUniqueId(), System.currentTimeMillis());
        if (adminActor != null) {
            record("admin_revive", "\"actor\":\"" + esc(adminActor) + "\""
                    + ",\"target\":\"" + esc(target.getName()) + "\""
                    + ",\"target_uuid\":\"" + target.getUniqueId() + "\""
                    + ",\"amount_restored\":" + round2(given)
                    + ",\"effective\":" + round2(given)
                    + ",\"health_after\":" + round2(after));
            getLogger().info("Downed admin revive by " + adminActor + " for "
                    + target.getName() + " (" + target.getUniqueId() + "): revived at "
                    + round2(after) + " health.");
            Bukkit.getPluginManager().callEvent(new DownedEvents.DownedReviveEvent(null, target, given));
            alertParty(target, ChatColor.GREEN + "✝ " + target.getName()
                    + " was revived by an operator!");
            target.sendMessage(ChatColor.GREEN + "✝ An operator revived you at 50% health.");
            return true;
        }
        if (cleric) {
            record("cleric_revive", "\"healer\":\"" + esc(healer == null ? "" : healer.getName()) + "\""
                    + ",\"healer_uuid\":\"" + (healer == null ? "" : healer.getUniqueId()) + "\""
                    + ",\"target\":\"" + esc(target.getName()) + "\""
                    + ",\"target_uuid\":\"" + target.getUniqueId() + "\""
                    + ",\"amount_restored\":" + round2(given)
                    + ",\"effective\":" + round2(given)
                    + ",\"health_after\":" + round2(after)
                    + ",\"method\":\"cleric-ability\""
                    + ",\"source\":\"cleric-ability\""
                    + ",\"ability\":\"" + ClericRevivePolicy.ABILITY_ID + "\""
                    + ",\"grace_cost\":" + round2(clericReviveGraceCost));
            Bukkit.getPluginManager().callEvent(new DownedEvents.DownedReviveEvent(healer, target, given));
            alertParty(target, ChatColor.GREEN + "✝ " + target.getName() + " was raised by "
                    + (healer == null ? "a Cleric" : healer.getName()) + " (Raise Ally)!");
            target.sendMessage(ChatColor.GREEN + "✝ You were raised at 50% health by "
                    + (healer == null ? "a Cleric's" : healer.getName() + "'s") + " Raise Ally.");
            return true;
        }
        if (potion) {
            record("potion_revive", "\"healer\":\"" + esc(healer == null ? "" : healer.getName()) + "\""
                    + ",\"healer_uuid\":\"" + (healer == null ? "" : healer.getUniqueId()) + "\""
                    + ",\"target\":\"" + esc(target.getName()) + "\""
                    + ",\"target_uuid\":\"" + target.getUniqueId() + "\""
                    + ",\"amount_restored\":" + round2(given)
                    + ",\"effective\":" + round2(given)
                    + ",\"health_after\":" + round2(after)
                    + ",\"method\":\"potion\""
                    + ",\"potions_remaining\":" + potionsRemaining);
            Bukkit.getPluginManager().callEvent(new DownedEvents.DownedReviveEvent(healer, target, given));
            alertParty(target, ChatColor.GREEN + "✝ " + target.getName() + " was revived by "
                    + (healer == null ? "a Revive Potion" : healer.getName()) + " (Revive Potion)!");
            target.sendMessage(ChatColor.GREEN + "✝ You were revived at 50% health by "
                    + (healer == null ? "a" : healer.getName() + "'s") + " Revive Potion.");
            return true;
        }
        record("revive", "\"spell\":\"resurrection\""
                + ",\"healer\":\"" + esc(healer == null ? "" : healer.getName()) + "\""
                + ",\"healer_uuid\":\"" + (healer == null ? "" : healer.getUniqueId()) + "\""
                + ",\"target\":\"" + esc(target.getName()) + "\""
                + ",\"target_uuid\":\"" + target.getUniqueId() + "\""
                + ",\"amount_restored\":" + round2(given)
                + ",\"effective\":" + round2(given)
                + ",\"health_after\":" + round2(after));
        Bukkit.getPluginManager().callEvent(new DownedEvents.DownedReviveEvent(healer, target, given));
        alertParty(target, ChatColor.GREEN + "✝ " + target.getName() + " was revived by "
                + (healer == null ? "Resurrection" : healer.getName()) + "!");
        target.sendMessage(ChatColor.GREEN + "✝ You were revived at 50% health.");
        return true;
    }

    /**
     * The admin mint seam (1.0.0, spec A section 8): called ONLY by
     * the admin-gated /downed revivepotion give verb. This seam
     * takes NO money and is NEVER a shop menu target (W6 section
     * 3.2) — the paid path is the purchase flow below, whose deliver
     * port mints through RevivePotionItem directly. This seam only
     * mints, logs, and records, so every potion in the economy is
     * accounted for at birth (potion_mint). Overflow drops at the
     * player's feet (vanilla addItem shape) — an admin-mint
     * behaviour the paid path deliberately does NOT share (W6 R-4).
     * Returns the number actually minted (0 when the feature is off).
     */
    public int giveRevivePotions(String actor, Player target, int amount) {
        if (target == null || amount < 1) return 0;
        if (!revivePotionEnabled) {
            getLogger().warning("ExcelDowned: revivepotion give refused for "
                    + target.getName() + " — revive-potion.enabled is false.");
            return 0;
        }
        int minted = 0;
        for (int i = 0; i < amount; i++) {
            java.util.Map<Integer, org.bukkit.inventory.ItemStack> overflow =
                    target.getInventory().addItem(RevivePotionItem.create(this));
            for (org.bukkit.inventory.ItemStack left : overflow.values()) {
                target.getWorld().dropItemNaturally(target.getLocation(), left);
            }
            minted++;
        }
        String who = actor == null || actor.isBlank() ? "CONSOLE" : actor;
        record("potion_mint", "\"actor\":\"" + esc(who) + "\""
                + ",\"target\":\"" + esc(target.getName()) + "\""
                + ",\"target_uuid\":\"" + target.getUniqueId() + "\""
                + ",\"amount\":" + minted);
        getLogger().info("ExcelDowned: minted " + minted + " Revive Potion(s) for "
                + target.getName() + " by " + who + " (source: revivepotion give seam).");
        return minted;
    }

    // ---------- shop charge leg (1.0.0, W6 sections 3.3 + 4) ----------
    // ExcelDowned is the seller of record for the Revive Potion
    // (spec C D-C1a): the charge and the mint live in this one
    // plugin, in one journal-coordinated transaction run by
    // RevivePurchaseCoordinator. All policy (token, strict money
    // classification, lock/refund semantics) is the coordinator's;
    // this section is only the Bukkit/Vault mechanics behind its
    // Ports, the review-UI gateway, and the buyer-facing report.

    private void initializePurchases() {
        // Soft Vault wiring: look the Economy up via ServicesManager
        // ONLY when a Vault plugin is actually present, so a missing
        // Vault can never fail this plugin's enable. Absent/disabled
        // economy = purchases disabled (warning, never SEVERE); the
        // core Downed mechanic and the admin give mint are unaffected.
        try {
            if (getServer().getPluginManager().getPlugin("Vault") != null) {
                RegisteredServiceProvider<Economy> provider =
                        getServer().getServicesManager().getRegistration(Economy.class);
                economy = provider == null ? null : provider.getProvider();
            }
        } catch (Throwable t) {
            economy = null;
            getLogger().warning("ExcelDowned: Vault economy lookup failed ("
                    + t.getClass().getSimpleName() + ": " + t.getMessage()
                    + ") — Revive Potion purchases disabled; Downed is unaffected.");
        }
        if (economy == null) {
            getLogger().warning("ExcelDowned: Vault economy unavailable — Revive Potion "
                    + "purchases disabled; the revive mechanic and admin give are unaffected.");
        }
        try {
            purchaseJournal = new RevivePurchaseJournal(
                    new File(getDataFolder(), "purchases.journal").toPath());
            purchases = new RevivePurchaseCoordinator(new RevivePurchaseCoordinator.Ports() {
                @Override public RevivePurchaseCoordinator.Eligibility check(
                        UUID id, RevivePurchaseCoordinator.Product product) {
                    return purchaseCheck(id);
                }
                @Override public double balance(UUID id) { return purchaseBalance(id); }
                @Override public RevivePurchaseCoordinator.MoneyResult withdraw(UUID id, double price) {
                    return purchaseWithdraw(id, price);
                }
                @Override public RevivePurchaseCoordinator.MoneyResult deposit(UUID id, double price) {
                    return purchaseDeposit(id, price);
                }
                @Override public RevivePurchaseCoordinator.DeliveryOutcome deliver(
                        UUID id, RevivePurchaseCoordinator.Product product) {
                    return deliverRevivePurchase(id);
                }
            }, purchaseJournal, System::currentTimeMillis,
                    reviveBuyConfirmWindowMs, reviveBuySuccessCooldownMs);
            if (!purchaseJournal.unresolved().isEmpty()) {
                getLogger().warning("ExcelDowned: unresolved purchase journal entries: "
                        + purchaseJournal.unresolved().size()
                        + ". Affected players are locked from purchases until manual "
                        + "reconciliation; do not automatically retry payment/refund.");
            }
        } catch (IOException | RuntimeException ex) {
            purchases = null;
            getLogger().warning("ExcelDowned: purchase journal unavailable — Revive Potion "
                    + "purchases disabled: " + ex.getMessage());
        }
    }

    private RevivePurchaseCoordinator.Eligibility purchaseCheck(UUID id) {
        if (!revivePotionEnabled || economy == null) {
            return RevivePurchaseCoordinator.Eligibility.UNAVAILABLE;
        }
        Player p = Bukkit.getPlayer(id);
        if (p == null || !p.isOnline() || p.isDead()) {
            return RevivePurchaseCoordinator.Eligibility.OFFLINE;
        }
        if (isDowned(id)) return RevivePurchaseCoordinator.Eligibility.DOWNED;
        if (reviveBuyRequireEmptySlot && p.getInventory().firstEmpty() < 0) {
            return RevivePurchaseCoordinator.Eligibility.NO_SPACE;
        }
        return RevivePurchaseCoordinator.Eligibility.ELIGIBLE;
    }

    private double purchaseBalance(UUID id) {
        Player p = Bukkit.getPlayer(id);
        if (economy == null || p == null) return Double.NaN;
        return economy.getBalance(p);
    }

    /** Strict EconomyResponse mapping (ShopAdapter shape): a null
     *  response stays null — the coordinator reads null, a wrong
     *  amount, or an inconsistent balance as AMBIGUOUS and locks. */
    private RevivePurchaseCoordinator.MoneyResult purchaseWithdraw(UUID id, double price) {
        Player p = Bukkit.getPlayer(id);
        if (economy == null || p == null) return null;
        EconomyResponse r = economy.withdrawPlayer(p, price);
        return r == null ? null
                : new RevivePurchaseCoordinator.MoneyResult(r.transactionSuccess(), r.amount, r.balance);
    }

    private RevivePurchaseCoordinator.MoneyResult purchaseDeposit(UUID id, double price) {
        Player p = Bukkit.getPlayer(id);
        if (economy == null || p == null) return null;
        EconomyResponse r = economy.depositPlayer(p, price);
        return r == null ? null
                : new RevivePurchaseCoordinator.MoneyResult(r.transactionSuccess(), r.amount, r.balance);
    }

    /**
     * The paid delivery (W6 section 4 step 5): mint exactly 1 via
     * the RevivePotionItem factory into the buyer's storage — NO
     * overflow ground drop is permitted on this path. SUCCESS
     * requires the potion count observed at before+1, an empty
     * overflow, and a completed save call. A verified non-delivery
     * with the inventory provably restored to its snapshot (and
     * saved) is FAILED_ROLLED_BACK; anything unverifiable is
     * UNKNOWN (the coordinator locks and withholds the refund).
     */
    private RevivePurchaseCoordinator.DeliveryOutcome deliverRevivePurchase(UUID id) {
        Player p = Bukkit.getPlayer(id);
        if (p == null || !p.isOnline() || p.isDead()) {
            return RevivePurchaseCoordinator.DeliveryOutcome.FAILED_ROLLED_BACK;
        }
        PlayerInventory inv = p.getInventory();
        ItemStack[] before;
        int countBefore;
        try {
            before = cloneItems(inv.getStorageContents());
            countBefore = RevivePotionItem.countPotions(this, inv);
        } catch (RuntimeException ex) {
            return RevivePurchaseCoordinator.DeliveryOutcome.FAILED_ROLLED_BACK;
        }
        try {
            java.util.Map<Integer, ItemStack> leftover = inv.addItem(RevivePotionItem.create(this));
            if (leftover == null || !leftover.isEmpty()) {
                throw new IllegalStateException("Paid delivery overflowed — ground drop is not permitted");
            }
            if (RevivePotionItem.countPotions(this, inv) != countBefore + 1) {
                throw new IllegalStateException("Paid delivery verification failed");
            }
            p.saveData();
            return RevivePurchaseCoordinator.DeliveryOutcome.SUCCESS;
        } catch (RuntimeException ex) {
            getLogger().warning("ExcelDowned: paid Revive Potion delivery failed for "
                    + p.getName() + "; attempting rollback: " + ex);
            try {
                inv.setStorageContents(cloneItems(before));
                p.saveData();
                if (Arrays.equals(before, inv.getStorageContents())
                        && RevivePotionItem.countPotions(this, inv) == countBefore) {
                    return RevivePurchaseCoordinator.DeliveryOutcome.FAILED_ROLLED_BACK;
                }
            } catch (RuntimeException rollback) {
                getLogger().warning("ExcelDowned: paid delivery rollback uncertain for "
                        + p.getName() + ": " + rollback);
            }
            return RevivePurchaseCoordinator.DeliveryOutcome.UNKNOWN;
        }
    }

    private static ItemStack[] cloneItems(ItemStack[] source) {
        ItemStack[] clone = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) clone[i] = source[i] == null ? null : source[i].clone();
        return clone;
    }

    // ---------- purchase gateway (the review UI + the buy verb) ----------

    private RevivePurchaseCoordinator.Result purchaseUnavailable(Player buyer) {
        return new RevivePurchaseCoordinator.Result(
                RevivePurchaseCoordinator.Status.UNAVAILABLE,
                RevivePurchaseCoordinator.Product.REVIVE_POTION,
                revivePotionPrice, Double.NaN, null,
                "Economy or purchase journal unavailable");
    }

    /** Quote (review open): arms the token, charges nothing. */
    public RevivePurchaseCoordinator.Result purchaseQuote(Player buyer) {
        if (purchases == null || economy == null) return purchaseUnavailable(buyer);
        return purchases.quote(buyer.getUniqueId(),
                RevivePurchaseCoordinator.Product.REVIVE_POTION, revivePotionPrice);
    }

    /** Preview (review refresh): never arms, never charges. */
    public RevivePurchaseCoordinator.Result purchasePreview(Player buyer) {
        if (purchases == null || economy == null) return purchaseUnavailable(buyer);
        return purchases.preview(buyer.getUniqueId(),
                RevivePurchaseCoordinator.Product.REVIVE_POTION, revivePotionPrice);
    }

    /**
     * Confirm (the only debiting call): on PURCHASED, the Downed
     * JSONL record is written here — potion_mint with
     * "source":"shop_buy", the price, and the transaction id — so
     * every shop-bought potion's birth carries its money leg
     * (W6 section 4 step 6). Admin give rows keep their existing
     * shape ("source":"give" semantics: the give seam's actor row).
     */
    public RevivePurchaseCoordinator.Result purchaseConfirm(Player buyer) {
        if (purchases == null || economy == null) return purchaseUnavailable(buyer);
        RevivePurchaseCoordinator.Result result = purchases.confirm(buyer.getUniqueId(),
                RevivePurchaseCoordinator.Product.REVIVE_POTION, revivePotionPrice);
        if (result.status() == RevivePurchaseCoordinator.Status.PURCHASED) {
            record("potion_mint", "\"actor\":\"" + esc(buyer.getName()) + "\""
                    + ",\"target\":\"" + esc(buyer.getName()) + "\""
                    + ",\"target_uuid\":\"" + buyer.getUniqueId() + "\""
                    + ",\"amount\":1"
                    + ",\"source\":\"shop_buy\""
                    + ",\"price\":" + (long) result.price()
                    + ",\"tx\":\"" + result.transactionId() + "\"");
            getLogger().info("ExcelDowned: shop buy — minted 1 Revive Potion for "
                    + buyer.getName() + " at $" + String.format("%,d", (long) result.price())
                    + " (tx " + result.transactionId() + ").");
        }
        return result;
    }

    public void purchaseCancel(Player buyer) {
        if (purchases != null) purchases.cancel(buyer.getUniqueId());
    }

    public void purchaseCancelId(UUID id) {
        if (purchases != null) purchases.cancel(id);
    }

    /**
     * Open the purchase review for a named buyer (the console shop
     * dispatch). Returns the quote result; ARMED means the review
     * is standing. Any other status has already been reported to
     * the buyer by the UI layer and charged nothing.
     */
    public RevivePurchaseCoordinator.Result openRevivePurchase(Player buyer) {
        if (buyer == null) return null;
        if (purchases == null || economy == null) {
            RevivePurchaseCoordinator.Result unavailable = purchaseUnavailable(buyer);
            reportPurchase(buyer, unavailable);
            return unavailable;
        }
        if (revivePurchaseUi == null) return purchaseUnavailable(buyer);
        return revivePurchaseUi.openReview(buyer);
    }

    /** Buyer-facing purchase report (precedent wording, W6 section 4). */
    public void reportPurchase(Player buyer, RevivePurchaseCoordinator.Result result) {
        if (buyer == null || result == null) return;
        String price = "$" + RevivePurchaseUi.money(result.price());
        switch (result.status()) {
            case ARMED -> buyer.sendMessage(ChatColor.YELLOW + "✝ Revive Potion " + price
                    + ". Review the price and balance, then select Confirm. No charge yet.");
            case PURCHASED -> {
                buyer.sendMessage(ChatColor.GREEN + "✝ Revive Potion bought.");
                buyer.sendMessage(ChatColor.GRAY + "Paid " + price + "; balance $"
                        + RevivePurchaseUi.money(result.balance()) + ".");
            }
            case INELIGIBLE -> {
                String detail = switch (result.detail() == null ? "" : result.detail()) {
                    case "NO_SPACE" -> "Make an empty inventory slot first; no charge.";
                    case "DOWNED" -> "You cannot buy while downed; no charge.";
                    default -> "This purchase is unavailable right now; no charge.";
                };
                buyer.sendMessage(ChatColor.YELLOW + "✝ Cannot buy. " + detail);
            }
            case INSUFFICIENT_FUNDS -> buyer.sendMessage(ChatColor.RED + "✝ Not enough money. "
                    + "Costs " + price + "; short $"
                    + RevivePurchaseUi.money(Math.max(0, result.price() - result.balance())) + ".");
            case DECLINED -> buyer.sendMessage(ChatColor.RED
                    + "✝ Payment declined. The economy provider declined this purchase.");
            case REFUNDED -> buyer.sendMessage(ChatColor.YELLOW + "✝ Purchase refunded. "
                    + "Delivery failed; " + price + " was returned.");
            case COOLDOWN, BUSY -> buyer.sendMessage(ChatColor.YELLOW
                    + "✝ Please wait — your last purchase is already being handled.");
            case LOCKED -> {
                getLogger().warning("ExcelDowned: purchase needs manual reconciliation for "
                        + buyer.getName() + " (" + buyer.getUniqueId() + "), transaction "
                        + result.transactionId() + ": " + result.detail());
                buyer.sendMessage(ChatColor.RED + "✝ Purchase needs admin review. "
                        + "Payment or delivery is uncertain. Do not retry; tell an admin.");
            }
            case UNAVAILABLE, INVALID_PRICE -> buyer.sendMessage(ChatColor.RED
                    + "✝ Shop unavailable. No purchase can be completed right now; tell an admin.");
        }
    }

    /**
     * Console/operator clear (0.1.2, console-admin audit §4): remove a
     * downed session outright — state surgery for a stuck session.
     * Works for an ONLINE player (presentation cleared) and for an
     * OFFLINE durable session (the persisted record is dropped, so
     * the player returns standing at their next login). Deliberately
     * NOT a heal and NOT a death: no health is written, no death path
     * runs, nothing drops. Returns true only if a session existed.
     */
    public boolean adminClear(String actor, UUID id) {
        if (id == null) return false;
        DownedSession s = sessions.remove(id);
        if (s == null) return false;
        String who = actor == null || actor.isBlank() ? "CONSOLE" : actor;
        if (giveUpUi != null) giveUpUi.invalidate(id); // an open confirm dies with the down
        if (revivePotionUi != null) revivePotionUi.invalidateForTarget(id);
        saveState();
        Player p = Bukkit.getPlayer(id);
        boolean online = p != null;
        if (online) clearDownedPresentation(p);
        record("admin_clear", "\"actor\":\"" + esc(who) + "\""
                + ",\"player\":\"" + esc(s.playerName()) + "\""
                + ",\"uuid\":\"" + id + "\""
                + ",\"online\":" + online);
        getLogger().info("Downed admin clear by " + who + " for " + s.playerName()
                + " (" + id + "): downed session removed (online=" + online
                + "); no health restored, no death run.");
        if (online) {
            p.sendMessage(ChatColor.GOLD + "✝ An operator cleared your downed state. "
                    + "You are still badly hurt — heal up.");
        }
        return true;
    }

    /**
     * Run the one real death. Consumes the session + the death latch
     * first, so no path can ever produce a second death for one down.
     */
    public boolean finalizeDeath(Player player, DownedSession.Exit exit) {
        UUID id = player.getUniqueId();
        DownedSession s = sessions.get(id);
        if (s == null || !s.consumeDeath()) return false;
        sessions.remove(id);
        if (giveUpUi != null) giveUpUi.invalidate(id); // an open confirm dies with the down
        if (revivePotionUi != null) revivePotionUi.invalidateForTarget(id);
        saveState();
        bypassDeath.add(id);
        clearDownedPresentation(player);
        String type = exit == DownedSession.Exit.GIVE_UP ? "give_up"
                : exit == DownedSession.Exit.BLEED_OUT ? "bleed_out" : "downed_death_external";
        record(type, baseFields(player));
        Bukkit.getPluginManager().callEvent(new DownedEvents.DownedDeathEvent(player, exit));
        if (player.isOnline()) {
            try { player.setHealth(0.0); } catch (Throwable ignored) {}
        }
        return true;
    }

    /** External death (e.g. /kill) while downed: clear state, no second path. */
    public void noteExternalDeath(Player player) {
        if (sessions.remove(player.getUniqueId()) != null) {
            if (giveUpUi != null) giveUpUi.invalidate(player.getUniqueId());
            if (revivePotionUi != null) revivePotionUi.invalidateForTarget(player.getUniqueId());
            saveState();
            clearDownedPresentation(player);
            record("downed_death_external", baseFields(player));
        }
    }

    public boolean wasRevivedByRecently(UUID casterId, long withinMs) {
        Long t = lastReviveByCaster.get(casterId);
        return t != null && System.currentTimeMillis() - t <= withinMs;
    }

    public void consumeReviveMark(UUID casterId) { lastReviveByCaster.remove(casterId); }

    // ---------- tasks ----------

    private void stateTick() {
        long now = System.currentTimeMillis();
        for (DownedSession s : sessions.values()) {
            Player p = Bukkit.getPlayer(s.playerId());
            if (p == null) {
                // offline: wall clock rules; an expired offline session is
                // finalized at login (PlayerJoin handler), not here.
                continue;
            }
            if (s.isExpired(now)) {
                p.sendMessage(ChatColor.DARK_RED + "✝ You bled out.");
                finalizeDeath(p, DownedSession.Exit.BLEED_OUT);
                continue;
            }
            if (p.getFireTicks() > 0) p.setFireTicks(0); // 0.1.3: fire never ticks on a downed body
            // keep the freeze exact + the slowness on (Cure strips slow)
            if (Math.abs(p.getHealth() - downedHealth) > 0.01) {
                try { p.setHealth(downedHealth); } catch (Throwable ignored) {}
            }
            if (!p.hasPotionEffect(PotionEffectType.SLOWNESS)) applyDownedState(p);
            p.sendActionBar(ChatColor.RED + "✝ DOWNED — bleed out in "
                    + formatClock(s.remainingMs(now)) + " · /giveup to let go");
        }
        if (giveUpUi != null) giveUpUi.tickExpire(); // a lapsed arm closes its menu
        if (revivePotionUi != null) revivePotionUi.promptTick(); // carrier prompt, same 1 s cadence
        if (revivePurchaseUi != null) revivePurchaseUi.tickRefresh(); // purchase review balance refresh, same cadence
    }

    private void scrubTick() {
        for (DownedSession s : sessions.values()) {
            Player p = Bukkit.getPlayer(s.playerId());
            if (p != null) clearAggroOn(p);
        }
    }

    /** Clear current mob targets on a downed player (entry + scrub). */
    public void clearAggroOn(Player player) {
        for (org.bukkit.entity.Entity e : player.getNearbyEntities(scrubRadius, scrubRadius, scrubRadius)) {
            if (e instanceof org.bukkit.entity.Mob) {
                org.bukkit.entity.Mob mob = (org.bukkit.entity.Mob) e;
                if (mob.getTarget() == player) mob.setTarget(null);
            }
        }
    }

    void alertParty(Player subject, String message) {
        Set<UUID> members = party.partyMembersOf(subject.getUniqueId());
        for (UUID id : members) {
            Player m = Bukkit.getPlayer(id);
            if (m != null) m.sendMessage(message);
        }
    }

    // ---------- persistence ----------

    private synchronized void saveState() {
        try {
            YamlConfiguration y = new YamlConfiguration();
            for (DownedSession s : sessions.values()) {
                y.set("sessions." + s.playerId(), s.toMap());
            }
            y.save(stateFile);
        } catch (IOException e) {
            getLogger().warning("ExcelDowned: state save failed: " + e.getMessage());
        }
    }

    private void loadState() {
        if (!stateFile.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(stateFile);
        if (y.getConfigurationSection("sessions") == null) return;
        for (String key : y.getConfigurationSection("sessions").getKeys(false)) {
            Map<String, Object> m = new HashMap<>();
            for (String k : y.getConfigurationSection("sessions." + key).getKeys(false)) {
                m.put(k, y.get("sessions." + key + "." + k));
            }
            try { sessions.put(UUID.fromString(key), DownedSession.fromMap(m)); }
            catch (Throwable t) { getLogger().warning("ExcelDowned: skipped bad session " + key); }
        }
    }

    // ---------- records (Protect-shaped JSONL, DESIGN §7) ----------

    private String baseFields(Player p) {
        return "\"player\":\"" + esc(p.getName()) + "\",\"uuid\":\"" + p.getUniqueId() + "\"";
    }

    public void record(String type, String fields) {
        String day = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        File dir = new File(getDataFolder(), "records");
        dir.mkdirs();
        File f = new File(dir, "records-" + day + ".jsonl");
        String line = "{\"ts\":" + System.currentTimeMillis() + ",\"type\":\"" + type + "\","
                + fields + "}";
        try (PrintWriter w = new PrintWriter(new FileWriter(f, true))) {
            w.println(line);
        } catch (IOException e) {
            getLogger().warning("ExcelDowned: record write failed: " + e.getMessage());
        }
    }

    static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
    static long now() { return System.currentTimeMillis(); }

    static String formatClock(long ms) {
        long sec = Math.max(0, (ms + 999) / 1000);
        return (sec / 60) + ":" + String.format("%02d", sec % 60);
    }
}
