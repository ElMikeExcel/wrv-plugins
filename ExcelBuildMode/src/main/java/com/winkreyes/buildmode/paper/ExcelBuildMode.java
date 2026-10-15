package com.winkreyes.buildmode.paper;

import com.winkreyes.buildmode.core.*;
import java.util.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.logging.Level;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.server.*;
import org.bukkit.inventory.*;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;

/** OFFLINE PROTOTYPE. This is a trust-based inventory switch, not an anti-cheat/security boundary. */
public final class ExcelBuildMode extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private enum Phase { BUILD, LANDING, RECOVERY, FAILED }
    private final Map<UUID, Phase> phases = new HashMap<>();
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();
    private final Set<UUID> changingMode = new HashSet<>();
    private final Set<UUID> quitting = new HashSet<>();
    private RecoveryJournal journal;
    private SessionCoordinator coordinator;
    private ActiveSessionIndex sessionIndex;
    private ReviewHoldStore reviewHolds;
    private final Set<UUID> manualReview = new HashSet<>();
    private CohortStore.Result cohortState = new CohortStore.Result(Set.of(), false, "Private recovery cohort is not loaded.");
    private Set<UUID> eligibleBuilders = Set.of();
    private Path playerDataDirectory;
    private NamespacedKey recoveryMarker;
    private boolean journalReady;
    private String startupProblem;
    // Volatile publication occurs only after every existing recovery record is guarded.
    private volatile RewardLedger rewards;
    private boolean blockTransfers, blockCommands, allowChatHelp, worldEdit, stopping, pilot;

    @Override public void onEnable() {
        saveDefaultConfig();
        pilot = getConfig().getBoolean("pilot-construction-only", true);
        blockTransfers = getConfig().getBoolean("guard-accidental-transfers", true);
        blockCommands = getConfig().getBoolean("guard-commands", true);
        allowChatHelp = getConfig().getBoolean("allow-chat-help-commands", true);
        worldEdit = getConfig().getBoolean("temporary-worldedit", false)
            && getServer().getPluginManager().isPluginEnabled("WorldEdit");
        recoveryMarker = new NamespacedKey(this, "recovery");
        Set<String> loadedProviders = new HashSet<>();
        for (String name : RewardLedger.COVERAGE.keySet())
            if (getServer().getPluginManager().getPlugin(name) != null) loadedProviders.add(name);
        RewardLedger startupRewards = new RewardLedger(loadedProviders);
        cohortState = CohortStore.inspect(getDataFolder().toPath().resolve("cohort-primary.txt"), getDataFolder().toPath().resolve("cohort-backup.txt"));
        try {
            Set<UUID> eligible = new HashSet<>();
            for (String value : getConfig().getStringList("eligible-builder-uuids")) {
                UUID id = UUID.fromString(value);
                if (!id.toString().equals(value) || !eligible.add(id)) throw new IOException("Noncanonical or duplicate builder UUID");
            }
            if (!cohortState.verified()) throw new IOException(cohortState.problem());
            new RecoveryCohort(cohortState.known(), eligible);
            eligibleBuilders = Set.copyOf(eligible);
            if (eligibleBuilders.isEmpty()) throw new IOException("No private builder eligibility has been configured.");
        } catch (Exception e) { startupProblem = "Build entry requires verified private cohort and eligibility data."; getLogger().log(Level.SEVERE, startupProblem, e); }
        try {
            reviewHolds = new ReviewHoldStore(getDataFolder().toPath().resolve("review-holds"));
            manualReview.addAll(reviewHolds.players());
        } catch (Exception e) {
            startupProblem = "Independent review-hold storage is unavailable; new Build entry is disabled.";
            getLogger().log(Level.SEVERE, startupProblem, e);
        }
        try {
            Path journalPath = getDataFolder().toPath().resolve("recovery"), indexPath = getDataFolder().toPath().resolve("active-index");
            if (!Files.isDirectory(journalPath, LinkOption.NOFOLLOW_LINKS) || !Files.isDirectory(indexPath, LinkOption.NOFOLLOW_LINKS))
                throw new IOException("Recovery directories must be provisioned by the reviewed private installation; missing storage is not an empty history.");
            StorageAdmission storage = new StorageAdmission(getConfig().getLong("storage.maximum-retained-bytes", StorageAdmission.DEFAULT_BYTES),
                getConfig().getLong("storage.minimum-free-bytes", StorageAdmission.DEFAULT_RESERVE), getConfig().getLong("storage.maximum-retained-sessions", StorageAdmission.DEFAULT_SESSIONS));
            journal = new RecoveryJournal(journalPath, stage -> {}, storage);
            sessionIndex = new ActiveSessionIndex(indexPath);
            for (UUID id : journal.players()) {
                try { if (journal.active(id).isPresent()) phases.put(id, Phase.RECOVERY); }
                catch (Exception e) { phases.put(id, Phase.FAILED); getLogger().log(Level.SEVERE, "Unreadable recovery for " + id, e); }
                if (phases.containsKey(id)) startupRewards.guardRecovery(id);
            }
            for (UUID id : sessionIndex.players()) {
                try {
                    var indexed = sessionIndex.read(id);
                    if (indexed.filter(ActiveSessionIndex.Entry::active).isPresent()) {
                        var active = journal.active(id);
                        if (active.isEmpty() || !active.get().id().equals(indexed.get().session())) phases.put(id, Phase.FAILED);
                        else phases.putIfAbsent(id, Phase.RECOVERY);
                    }
                } catch (Exception e) { phases.put(id, Phase.FAILED); getLogger().log(Level.SEVERE, "Unreadable active index for " + id, e); }
                if (phases.containsKey(id)) startupRewards.guardRecovery(id);
            }
            for (UUID id : manualReview) { phases.put(id, Phase.FAILED); startupRewards.guardRecovery(id); }
            // Pilot profile: the coordinator gets no RewardLedger, so entry never waits on
            // reward-provider registration. The ledger still exists for the API bridge and
            // recovery guarding; its completeness is simply not an admission condition here.
            coordinator = new SessionCoordinator(journal, pilot ? null : startupRewards, sessionIndex);
            journalReady = true;
        } catch (Exception e) {
            startupProblem = "Recovery storage is unavailable. New Build entry is disabled; affected builders need administrator help.";
            Set<UUID> affected = new HashSet<>(cohortState.known()); affected.addAll(manualReview);
            for (UUID id : affected) { phases.put(id, Phase.FAILED); startupRewards.guardRecovery(id); }
            getLogger().log(Level.SEVERE, startupProblem, e);
        }
        try {
            Object playerList = getServer().getClass().getMethod("getHandle").invoke(getServer());
            Object playerIo = playerList.getClass().getField("playerIo").get(playerList);
            java.io.File directory = (java.io.File) playerIo.getClass().getMethod("getPlayerDir").invoke(playerIo);
            playerDataDirectory = directory.toPath().toAbsolutePath().normalize();
            if (!Files.isDirectory(playerDataDirectory, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Native player-data directory is unavailable");
        } catch (Exception e) {
            startupProblem = "Native player-data verification is unavailable; new Build entry is disabled.";
            getLogger().log(Level.SEVERE, startupProblem, e);
        }
        // Published even in recovery-only mode: unrelated Survival remains eligible.
        if (startupProblem != null) startupRewards.blockAdmission(startupProblem);
        rewards = startupRewards;
        Objects.requireNonNull(getCommand("buildmode")).setExecutor(this);
        getCommand("buildmode").setTabCompleter(this);
        getServer().getPluginManager().registerEvents(this, this);
        for (Player p : getServer().getOnlinePlayers()) {
            inspectJoiningPlayer(p);
            if (phases.get(p.getUniqueId()) == Phase.RECOVERY) recover(p, false);
        }
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player p : getServer().getOnlinePlayers()) {
                Phase phase = phases.get(p.getUniqueId());
                if ((phase == Phase.LANDING && safeGround(p) && !p.isDead())
                    || (phase == Phase.RECOVERY && !p.isDead())) recover(p, false);
                else if (phase == Phase.BUILD && (!p.hasPermission("excelbuildmode.use") || (!pilot && rewards.readinessProblem() != null))) recover(p, false);
            }
        }, 10L, 10L);
        getLogger().warning(pilot
            ? "ExcelBuildMode 1.1 PILOT: construction-only profile. Reward-provider admission is OFF by configuration; earning is suppressed by the session jobs.use denial and the pilot input gates. Trust-based building, not a security boundary."
            : "ExcelBuildMode 1.1: cold-start reward integration; trust-based shared-world building. Validate provider artifacts before deployment.");
    }
    @Override public void onDisable() {
        stopping = true;
        if (rewards != null) rewards.closeOrigins();
        if (coordinator == null) return;
        for (Player p : List.copyOf(getServer().getOnlinePlayers())) {
            try {
                if (guarded(p)) recover(p, false);
                removeAttachment(p);
                if (guarded(p)) p.kickPlayer("Build Mode recovery is waiting for a safe landing. Rejoin after the plugin is enabled.");
            } catch (Exception e) { failClosed(p, e); }
        }
    }
    // Frozen reflective bridge v1: JDK-only signatures, safe from provider async threads.
    public int rewardApiVersion() { return RewardLedger.API_VERSION; }
    public boolean rewardIsActorGuarded(UUID actor) { RewardLedger r = rewards; return r == null || r.isActorGuarded(actor); }
    public UUID rewardApiEpoch() { RewardLedger r = rewards; return r == null ? null : r.epoch(); }
    public boolean rewardRegisterProvider(String provider, String revision, String[] coverage) {
        RewardLedger r = rewards; return r != null && r.registerProvider(provider, revision, coverage);
    }
    public UUID rewardBegin(UUID actor, String provider, String source, UUID target, boolean sourceAllowed) {
        RewardLedger r = rewards; return r == null ? null : r.begin(actor, provider, source, target, sourceAllowed);
    }
    public boolean rewardIsAllowed(UUID token) { RewardLedger r = rewards; return r != null && r.isAllowed(token); }
    public long rewardGeneration(UUID token) { RewardLedger r = rewards; return r == null ? -1 : r.generation(token); }
    public boolean rewardCanReceive(UUID token, UUID recipient) { RewardLedger r = rewards; return r != null && r.canReceive(token, recipient); }
    public UUID rewardFork(UUID token, UUID recipient) { RewardLedger r = rewards; return r == null ? null : r.fork(token, recipient); }
    public boolean rewardRetain(UUID token) { RewardLedger r = rewards; return r != null && r.retain(token); }
    public void rewardComplete(UUID token) { RewardLedger r = rewards; if (r != null) r.complete(token); }
    @EventHandler(priority = EventPriority.MONITOR) public void onProviderEnable(PluginEnableEvent event) {
        if (rewards != null) rewards.providerLoaded(event.getPlugin().getName());
    }
    @EventHandler(priority = EventPriority.MONITOR) public void onProviderDisable(PluginDisableEvent event) {
        String provider = event.getPlugin().getName();
        if (rewards == null || !RewardLedger.COVERAGE.containsKey(provider)) return;
        rewards.providerUnavailable(provider);
        if (pilot) { if (!stopping) getLogger().warning(provider + " stopped. Pilot Build sessions do not depend on reward providers; entry and recovery continue under the construction-only profile."); return; }
        if (!stopping) getLogger().severe(provider + " stopped. New Build entry is blocked until a full restart; active sessions will recover safely.");
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Use /buildmode in game. It only changes your own mode."); return true; }
        if (args.length > 1) { tell(p, "Use /buildmode, /buildmode on, /buildmode off, or /buildmode status."); return true; }
        if (args.length == 0) { openMenu(p); return true; }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "on" -> enter(p);
            case "off" -> recover(p, true);
            case "status" -> tell(p, "Your mode: " + status(p) + ". Use /buildmode for the buttons.");
            default -> tell(p, "Use /buildmode, /buildmode on, /buildmode off, or /buildmode status.");
        }
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? List.of("on", "off", "status").stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList() : List.of();
    }
    private void enter(Player p) {
        if (startupProblem != null || !journalReady) { tell(p, "Build Mode is unavailable until its recovery setup is verified."); return; }
        if (!eligibleBuilders.contains(p.getUniqueId())) { tell(p, "You are not in the private builder eligibility list."); return; }
        if (!p.hasPermission("excelbuildmode.use")) { tell(p, "You don't have Build Mode access."); return; }
        if (guarded(p)) { tell(p, "Your mode is " + status(p) + ". Choose Play Normally first."); return; }
        if (p.getPersistentDataContainer().getKeys().contains(recoveryMarker)) {
            holdForReview(p, new IOException("Entry cannot overwrite an existing recovery marker")); return;
        }
        if (p.isDead() || p.getGameMode() != GameMode.SURVIVAL || p.isInsideVehicle() || !safeGround(p)) {
            tell(p, "Start in Survival, standing on solid ground and out of a vehicle."); return;
        }
        if (!empty(p.getItemOnCursor()) || craftingItems(p)) { tell(p, "Put away the item on your cursor and empty your crafting grid first."); return; }
        if (worldEdit && BuildPolicy.WORLD_EDIT_PERMISSIONS.stream().anyMatch(p::hasPermission)) {
            tell(p, "You already have WorldEdit permissions outside Build Mode. An administrator must review those before temporary access can be used."); return;
        }
        p.closeInventory();
        if (!empty(p.getItemOnCursor())) { tell(p, "Put the item on your cursor away first."); return; }
        try {
            coordinator.enter(p.getUniqueId(), port(p));
            tell(p, "Build Mode is on. Your Survival items and XP are saved. Use /buildmode to play normally.");
        } catch (RewardLedger.AdmissionRejected blocked) {
            tell(p, blocked.getMessage());
        } catch (StorageAdmission.EntryRefused blocked) {
            tell(p, blocked.getMessage());
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Build entry failed for " + p.getUniqueId(), e);
            try {
                if (journal.active(p.getUniqueId()).isPresent()) { phases.put(p.getUniqueId(), Phase.RECOVERY); recover(p, false); }
                else if (rewards.isGuarded(p.getUniqueId())) failClosed(p, e);
                else { phases.remove(p.getUniqueId()); tell(p, "Couldn't save your recovery snapshot. Build Mode was not started."); }
            } catch (Exception readFailure) { failClosed(p, readFailure); }
        }
    }
    private void recover(Player p, boolean voluntary) {
        if (!guarded(p)) { tell(p, "You're already playing normally."); return; }
        if (manualReview.contains(p.getUniqueId())) { failClosed(p, new IOException("This player's persistent review hold requires administrator verification")); return; }
        if (!journalReady || coordinator == null) { failClosed(p, new IOException("Recovery storage is unavailable; no inventory mutation was attempted")); return; }
        if (p.isDead()) { removeAttachment(p); phases.put(p.getUniqueId(), Phase.RECOVERY); return; }
        if (voluntary && !safeGround(p)) { tell(p, "Land on solid ground first, then choose Play Normally."); return; }
        try {
            // Decode and validate BEFORE clearing anything. A corrupt snapshot locks rather than guesses.
            var session = journal.active(p.getUniqueId());
            if (session.isEmpty()) {
                // A missing/corrupt record cannot silently clear an authoritative failed guard.
                coordinator.recover(p.getUniqueId(), port(p));
                phases.remove(p.getUniqueId()); removeAttachment(p); return;
            }
            SurvivalSnapshot.decode(session.get().snapshot());
            if (!safeGround(p)) { landingHold(p); return; }
            phases.put(p.getUniqueId(), Phase.RECOVERY);
            p.closeInventory();
            coordinator.recover(p.getUniqueId(), port(p));
            phases.remove(p.getUniqueId());
            tell(p, "You're playing normally. Your Survival items, ender chest and XP are back.");
        } catch (Exception e) { failClosed(p, e); }
    }
    private SessionCoordinator.MarkerPort port(Player p) {
        return new SessionCoordinator.MarkerPort() {
            public byte[] capture() throws Exception { return SurvivalSnapshot.capture(p); }
            public void enterBuild() {
                phases.put(p.getUniqueId(), Phase.BUILD);
                clearBuildState(p); setMode(p, GameMode.CREATIVE);
                p.setAllowFlight(true); p.setFlying(false);
                // Session attachment (1.1): always created. It carries the jobs.use denial
                // and, only when temporary WorldEdit is enabled, the fixed WorldEdit set.
                PermissionAttachment attachment = p.addAttachment(ExcelBuildMode.this);
                attachments.put(p.getUniqueId(), attachment);
                for (var node : BuildPolicy.sessionPermissions(worldEdit).entrySet())
                    attachment.setPermission(node.getKey(), node.getValue());
                p.recalculatePermissions();
                if (p.hasPermission("jobs.use"))
                    getLogger().warning("Session jobs.use denial is not effective for " + p.getUniqueId() + "; another permission source wins. Jobs may still pay this builder while building.");
                if (worldEdit) {
                    for (String node : BuildPolicy.WORLD_EDIT_PERMISSIONS)
                        if (!p.hasPermission(node)) throw new IllegalStateException("Temporary WorldEdit permission was not effective: " + node);
                }
            }
            public void restore(byte[] bytes) throws Exception {
                SurvivalSnapshot snapshot = SurvivalSnapshot.decode(bytes);
                removeAttachment(p); clearBuildState(p); setMode(p, GameMode.SURVIVAL); snapshot.restore(p);
            }
            public void savePlayerData() { p.saveData(); }
            public void commitBuildMarker(RecoveryJournal.Session session) throws Exception { persistMarker(p, "v1:active:" + session.id()); }
            public void validateRecoveryMarker(RecoveryJournal.Session session) throws Exception {
                var proof = PlayerDataProof.inspect(playerFile(p), p.getUniqueId(), recoveryMarker.toString());
                if (proof.status() == PlayerDataProof.Status.UNKNOWN) throw new IOException("Native playerdata is unknown; retain recovery for administrator review");
                if (proof.status() == PlayerDataProof.Status.MARKED && !Objects.equals(proof.marker(), "v1:active:" + session.id())
                    && !Objects.equals(proof.marker(), "v1:restored:" + session.id())) throw new IOException("Native marker belongs to another or malformed recovery session");
                if (p.getPersistentDataContainer().getKeys().contains(recoveryMarker)) {
                    String memory = p.getPersistentDataContainer().get(recoveryMarker, PersistentDataType.STRING);
                    if (!Objects.equals(memory, "v1:active:" + session.id()) && !Objects.equals(memory, "v1:restored:" + session.id()))
                        throw new IOException("Loaded player recovery marker disagrees with journal");
                } else if (p.getGameMode() != GameMode.SURVIVAL) throw new IOException("Unmarked loaded player is not verified Survival");
            }
            public void commitRestoredMarker(RecoveryJournal.Session session) throws Exception { persistMarker(p, "v1:restored:" + session.id()); }
            public void clearRecoveryMarker(RecoveryJournal.Session session) throws Exception {
                String previous = p.getPersistentDataContainer().get(recoveryMarker, PersistentDataType.STRING);
                try {
                    p.getPersistentDataContainer().remove(recoveryMarker); p.saveData();
                    PlayerDataProof.forceVerifiedMarker(playerFile(p), p.getUniqueId(), recoveryMarker.toString(), null, true);
                } catch (Exception failure) {
                    if (previous != null) p.getPersistentDataContainer().set(recoveryMarker, PersistentDataType.STRING, previous);
                    throw failure;
                }
            }
        };
    }
    private Path playerFile(Player p) throws IOException {
        if (playerDataDirectory == null) throw new IOException("Native player-data location is unverified");
        return playerDataDirectory.resolve(p.getUniqueId() + ".dat");
    }
    private void persistMarker(Player p, String marker) throws Exception {
        p.getPersistentDataContainer().set(recoveryMarker, PersistentDataType.STRING, marker); p.saveData();
        PlayerDataProof.forceVerifiedMarker(playerFile(p), p.getUniqueId(), recoveryMarker.toString(), marker, true);
    }
    private void inspectJoiningPlayer(Player p) {
        if (p.getPersistentDataContainer().getKeys().contains(recoveryMarker)) {
            if (!guarded(p)) failClosed(p, new IOException("Persisted recovery marker has no verified active session"));
            return;
        }
        // A verified active snapshot follows recovery, never an inference that absent playerdata is safe.
        if (journalReady && phases.get(p.getUniqueId()) == Phase.RECOVERY) return;
        if (!journalReady || !cohortState.verified() || cohortState.known().contains(p.getUniqueId())) {
            try {
                var proof = PlayerDataProof.inspect(playerFile(p), p.getUniqueId(), recoveryMarker.toString());
                if (proof.status() != PlayerDataProof.Status.VERIFIED_UNMARKED_SURVIVAL || p.getGameMode() != GameMode.SURVIVAL) {
                    holdForReview(p, new IOException("Cannot verify this player's safe unmarked Survival state: " + proof.reason())); return;
                }
                if (!journalReady && cohortState.known().contains(p.getUniqueId()))
                    failClosed(p, new IOException("Known builder recovery cannot be checked while journal storage is unavailable"));
            } catch (Exception e) { holdForReview(p, e); }
        }
    }
    private void holdForReview(Player p, Exception reason) {
        manualReview.add(p.getUniqueId());
        try {
            if (reviewHolds != null) reviewHolds.hold(p.getUniqueId());
            if (!p.getPersistentDataContainer().getKeys().contains(recoveryMarker))
                p.getPersistentDataContainer().set(recoveryMarker, PersistentDataType.STRING, "v1:review:" + UUID.randomUUID());
            String marker = p.getPersistentDataContainer().get(recoveryMarker, PersistentDataType.STRING);
            p.saveData();
            if (marker != null) PlayerDataProof.forceVerifiedMarker(playerFile(p), p.getUniqueId(), recoveryMarker.toString(), marker, false);
        } catch (Exception persistenceFailure) { reason.addSuppressed(persistenceFailure); }
        failClosed(p, reason);
    }
    private void landingHold(Player p) {
        boolean first = phases.get(p.getUniqueId()) != Phase.LANDING;
        phases.put(p.getUniqueId(), Phase.LANDING); removeAttachment(p);
        p.closeInventory(); clearBuildState(p); setMode(p, GameMode.SURVIVAL);
        p.setAllowFlight(true); p.setFlying(true); p.setFallDistance(0); p.saveData();
        if (first) tell(p, "Recovery is waiting for a safe landing. Fly down to solid ground; your Survival items will return there.");
    }
    private void clearBuildState(Player p) {
        p.setItemOnCursor(null); p.getInventory().clear(); p.getInventory().setArmorContents(new ItemStack[4]);
        p.getInventory().setItemInOffHand(null); p.getEnderChest().clear();
        p.setExp(0); p.setLevel(0); p.setTotalExperience(0); p.setFallDistance(0);
    }
    private void setMode(Player p, GameMode mode) {
        changingMode.add(p.getUniqueId());
        try { p.setGameMode(mode); if (p.getGameMode() != mode) throw new IllegalStateException("Another plugin refused the mode change"); }
        finally { changingMode.remove(p.getUniqueId()); }
    }
    private void removeAttachment(Player p) {
        PermissionAttachment a = attachments.get(p.getUniqueId());
        if (a != null) {
            p.removeAttachment(a); attachments.remove(p.getUniqueId()); p.recalculatePermissions();
            if (worldEdit) for (String node : BuildPolicy.WORLD_EDIT_PERMISSIONS)
                if (p.hasPermission(node)) throw new IllegalStateException("WorldEdit permission remains after attachment removal: " + node);
        }
    }
    private void failClosed(Player p, Exception e) {
        phases.put(p.getUniqueId(), Phase.FAILED);
        if (rewards != null) rewards.guardRecovery(p.getUniqueId());
        try { removeAttachment(p); } catch (RuntimeException cleanup) { e.addSuppressed(cleanup); }
        getLogger().log(Level.SEVERE, "Recovery blocked for " + p.getUniqueId() + "; retain journal and get administrator help", e);
        if (!quitting.contains(p.getUniqueId())) p.kickPlayer("Your Build Mode recovery needs administrator help. Your saved snapshot has been retained.");
    }
    private boolean guarded(Player p) { return phases.containsKey(p.getUniqueId()); }
    private boolean building(Player p) { return phases.get(p.getUniqueId()) == Phase.BUILD; }
    private boolean transferGuard(Player p) { return guarded(p) && (blockTransfers || !building(p)); }
    private static boolean empty(ItemStack item) { return item == null || item.getType().isAir(); }
    private static boolean craftingItems(Player p) {
        return p.getOpenInventory().getTopInventory() instanceof CraftingInventory crafting
            && Arrays.stream(crafting.getMatrix()).anyMatch(item -> !empty(item));
    }
    private static boolean safeGround(Player p) {
        Location at = p.getLocation();
        Material support = at.clone().subtract(0, 0.12, 0).getBlock().getType();
        Material feet = at.getBlock().getType();
        return p.isOnGround() && !p.isInsideVehicle() && !p.isInWater() && !p.isDead()
            && support.isSolid() && !Set.of(Material.CACTUS, Material.MAGMA_BLOCK, Material.CAMPFIRE, Material.SOUL_CAMPFIRE).contains(support)
            && !Set.of(Material.FIRE, Material.SOUL_FIRE, Material.LAVA, Material.POWDER_SNOW, Material.SWEET_BERRY_BUSH).contains(feet);
    }
    private String status(Player p) {
        Phase phase = phases.get(p.getUniqueId());
        return phase == null ? "Play Normally" : phase == Phase.BUILD ? "Build Mode" : phase == Phase.LANDING ? "safe landing needed" : "recovery pending";
    }
    private static void tell(Player p, String text) { p.sendMessage("§6Build Mode §7» §f" + text); }
    private static final class ModeMenu implements InventoryHolder {
        private Inventory inventory;
        @Override public Inventory getInventory() { return inventory; }
    }
    private void openMenu(Player p) {
        if (!empty(p.getItemOnCursor()) || craftingItems(p)) { tell(p, "Put away the item on your cursor and empty your crafting grid first."); return; }
        if (!guarded(p) && !p.hasPermission("excelbuildmode.use")) { tell(p, "You don't have Build Mode access."); return; }
        ModeMenu holder = new ModeMenu(); holder.inventory = Bukkit.createInventory(holder, 9, "Build or play normally");
        holder.inventory.setItem(2, button(Material.BRICKS, "§aBuild Mode", "§7Creative building; Survival items saved"));
        holder.inventory.setItem(4, button(Material.PAPER, "§e" + status(p), "§7This changes only your own mode"));
        holder.inventory.setItem(6, button(Material.IRON_PICKAXE, "§bPlay Normally", "§7Stand on solid ground to restore items"));
        p.openInventory(holder.inventory);
    }
    private static ItemStack button(Material material, String title, String lore) {
        ItemStack item = new ItemStack(material); var meta = item.getItemMeta(); meta.setDisplayName(title); meta.setLore(List.of(lore)); item.setItemMeta(meta); return item;
    }
    @EventHandler(priority = EventPriority.LOWEST) public void onJoinProof(PlayerJoinEvent e) { inspectJoiningPlayer(e.getPlayer()); }
    @EventHandler(priority = EventPriority.HIGHEST) public void onJoin(PlayerJoinEvent e) {
        if (guarded(e.getPlayer())) recover(e.getPlayer(), false);
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer(); quitting.add(p.getUniqueId());
        try { if (guarded(p)) recover(p, false); removeAttachment(p); }
        catch (Exception failure) { failClosed(p, failure); }
        finally { quitting.remove(p.getUniqueId()); }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void onMode(PlayerGameModeChangeEvent e) {
        if (!guarded(e.getPlayer()) || changingMode.contains(e.getPlayer().getUniqueId())) return;
        e.setCancelled(true);
        if (!stopping) getServer().getScheduler().runTask(this, () -> { if (e.getPlayer().isOnline()) recover(e.getPlayer(), false); });
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity(); if (!guarded(p)) return;
        phases.put(p.getUniqueId(), Phase.RECOVERY);
        e.getDrops().clear(); e.getItemsToKeep().clear(); e.setDroppedExp(0); e.setKeepInventory(false);
        e.setKeepLevel(false); e.setNewExp(0); e.setNewLevel(0); e.setNewTotalExp(0);
        try { clearBuildState(p); removeAttachment(p); }
        catch (Exception failure) { failClosed(p, failure); }
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void onRespawn(PlayerRespawnEvent e) {
        if (!guarded(e.getPlayer()) || stopping) return;
        // Loaded after MenuJoin: restoration runs after its known one-tick compass injection.
        getServer().getScheduler().runTaskLater(this, () -> { if (e.getPlayer().isOnline()) recover(e.getPlayer(), false); }, 1L);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && guarded(p)) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void onAttack(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p && guarded(p)) e.setCancelled(true);
    }
    // ---- Pilot input gates (1.1): deny NEW earning/combat inputs at the earliest priority,
    // before Jobs, mcMMO or EliteMobs handlers run. Building players only; Play Normally is untouched.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true) public void onAttackEarly(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p && building(p)) { e.setCancelled(true); return; }
        if (e.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player p && building(p)) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true) public void onProjectileLaunch(ProjectileLaunchEvent e) {
        if (e.getEntity().getShooter() instanceof Player p && building(p)) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true) public void onFish(PlayerFishEvent e) {
        if (building(e.getPlayer())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true) public void onInteractAtEntity(PlayerInteractAtEntityEvent e) {
        // EliteMobs NPCs listen on this event, not only PlayerInteractEntityEvent; both are gated.
        if (building(e.getPlayer())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true) public void onSwapHand(PlayerSwapHandItemsEvent e) {
        // EliteMobs class abilities trigger from the swap-hand input. Tradeoff: no offhand swap while building.
        if (building(e.getPlayer())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true) public void onInteractPilot(PlayerInteractEvent e) {
        Player p = e.getPlayer(); if (!building(p)) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) return;
        Material held = e.getItem() == null ? Material.AIR : e.getItem().getType();
        if (BuildPolicy.blocksHarvestInteraction(e.getClickedBlock().getType(), held)) {
            e.setCancelled(true); tell(p, "Switch to Play Normally for harvesting or stripping.");
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void onDrop(PlayerDropItemEvent e) {
        if (transferGuard(e.getPlayer())) { e.setCancelled(true); tell(e.getPlayer(), "Switch to Play Normally before dropping items."); }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p && transferGuard(p)) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void onXp(PlayerExpChangeEvent e) { if (guarded(e.getPlayer())) e.setAmount(0); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void onOpen(InventoryOpenEvent e) {
        if (e.getPlayer() instanceof Player p && transferGuard(p) && !(e.getInventory().getHolder() instanceof ModeMenu)) {
            e.setCancelled(true); tell(p, "Switch to Play Normally to use containers or shops.");
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getView().getTopInventory().getHolder() instanceof ModeMenu) {
            e.setCancelled(true); int slot = e.getRawSlot();
            Inventory expectedMenu = e.getView().getTopInventory();
            if ((slot == 2 || slot == 6) && !stopping) getServer().getScheduler().runTask(this, () -> {
                if (!p.isOnline() || p.getOpenInventory().getTopInventory() != expectedMenu) return;
                p.closeInventory(); if (slot == 2) enter(p); else recover(p, true);
            });
            return;
        }
        if (!transferGuard(p)) return;
        InventoryType top = e.getView().getTopInventory().getType();
        if (!building(p) || (top != InventoryType.CRAFTING && top != InventoryType.CREATIVE)
                || e.getRawSlot() < 0 || e.getClick() == ClickType.DROP || e.getClick() == ClickType.CONTROL_DROP)
            e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getView().getTopInventory().getHolder() instanceof ModeMenu) { e.setCancelled(true); return; }
        if (!transferGuard(p)) return;
        InventoryType top = e.getView().getTopInventory().getType();
        if (!building(p) || (top != InventoryType.CRAFTING && top != InventoryType.CREATIVE)) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer(); if (!guarded(p)) return;
        if (!building(p)) { e.setCancelled(true); return; }
        if (!blockTransfers || e.getClickedBlock() == null || e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        var block = e.getClickedBlock();
        if (block.getState() instanceof InventoryHolder || block.getType().name().contains("SIGN")) {
            e.setCancelled(true); tell(p, "Switch to Play Normally to use containers or shops.");
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (transferGuard(e.getPlayer())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void onArmorStand(PlayerArmorStandManipulateEvent e) {
        if (transferGuard(e.getPlayer())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void onBreak(BlockBreakEvent e) {
        if (!guarded(e.getPlayer())) return;
        if (!building(e.getPlayer())) e.setCancelled(true);
        else { e.setDropItems(false); e.setExpToDrop(0); }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void onPlace(BlockPlaceEvent e) {
        if (guarded(e.getPlayer()) && !building(e.getPlayer())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer(); if (!guarded(p) || (!blockCommands && building(p))) return;
        if (!BuildPolicy.allowsCommand(e.getMessage(), worldEdit && building(p), allowChatHelp)) {
            e.setCancelled(true); tell(p, "Switch to Play Normally to use that command.");
        }
    }
}
