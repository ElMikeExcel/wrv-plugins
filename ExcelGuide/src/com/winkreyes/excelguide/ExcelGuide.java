package com.winkreyes.excelguide;

import com.winkreyes.excelguide.core.AiLog;
import com.winkreyes.excelguide.core.GuideConfig;
import com.winkreyes.excelguide.core.GuideEngine;
import com.winkreyes.excelguide.core.IntentSet;
import com.winkreyes.excelguide.core.KnowledgeBase;
import com.winkreyes.excelguide.core.LearnCache;
import com.winkreyes.excelguide.core.LearnedAnswers;
import com.winkreyes.excelguide.core.PeerCandidates;
import com.winkreyes.excelguide.core.PeerTip;
import com.winkreyes.excelguide.core.UnansweredLog;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ExcelGuide - the Valley Guide. Wiring only: files in, engine built,
 * listener and commands registered. All decisions live in the pure
 * core (com.winkreyes.excelguide.core) where the test harnesses can
 * reach them; this class owns Bukkit.
 *
 * Data folder: config.yml, intents.yml, knowledge.yml (bundled copies
 * ship in the jar and are dropped on first run), player-toggles.txt,
 * and the unanswered-question log. /guide reload re-reads the three
 * YAML files with no restart (SPEC 8).
 */
public final class ExcelGuide extends JavaPlugin implements CommandExecutor {

    private GuideConfig config;
    private KnowledgeBase knowledge;
    private GuideEngine engine;
    private int intentCount;
    private Path togglesFile;
    private Bedrock bedrock;
    private PeerTip peerTip;

    @Override
    public void onEnable() {
        saveBundled("config.yml");
        saveBundled("intents.yml");
        saveBundled("knowledge.yml");
        saveBundled("learned.yml");
        togglesFile = getDataFolder().toPath().resolve("player-toggles.txt");
        bedrock = new Bedrock(getLogger());
        try {
            buildEngine();
        } catch (Exception e) {
            getLogger().severe("ExcelGuide could not load its files: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onQuit(org.bukkit.event.player.PlayerQuitEvent e) {
                PeerTip t = peerTip;
                if (t != null) {
                    t.endSession(e.getPlayer().getUniqueId());
                }
                GuideEngine g = engine;
                if (g != null) {
                    g.forgetPlayer(e.getPlayer().getUniqueId()); // 0.3.2: drop their waiting question
                }
            }
        }, this);
        if (getCommand("guide") != null) {
            getCommand("guide").setExecutor(this);
        }
        if (getCommand("ask") != null) {
            getCommand("ask").setExecutor(this);
        }
        // 0.2.3: peer-learning windows close on a timer too, so a
        // capture settles even when chat goes quiet (handle() also
        // closes them lazily on every message). 0.3.2: every second,
        // so a delayed plain-chat answer lands on time.
        getServer().getScheduler().runTaskTimer(this, () -> {
            GuideEngine e = engine;
            if (e != null) {
                e.tick();
            }
        }, 20L, 20L);
        getLogger().info("Valley Guide awake: knowledge " + knowledge.version()
                + " (" + knowledge.size() + " chunks), "
                + intentCount + " intents, "
                + engine.learnedCount() + " learned answers, "
                + engine.aiCacheSize() + " cached answers, "
                + engine.peerLearnedCount() + " approved player answers, "
                + engine.peerReviewCount() + " waiting for /guide review.");
        // 0.3.1: the AI switch state is on the console at every boot
        // (status only - never the key).
        getLogger().info("AI fallback: " + (engine.aiActive() ? "ON" : "off")
                + " (provider " + config.aiProviderName + ", model " + config.aiModel
                + ", log " + config.aiLogFile + ", ambient hint "
                + (config.aiAmbientHint ? "on" : "off") + ").");
        // 0.3.2: the delayed plain-chat answer layer.
        getLogger().info("Delayed chat answers: " + (engine.delayedActive() ? "ON" : "off")
                + " (delay " + config.delayedDelaySeconds + "s, AI " + config.delayedAiTiming
                + (engine.aiActive() ? "" : " but AI off") + ", reply " + config.delayedReplyChannel
                + ", player answers " + (config.peerAutoApproveThanked ? "auto-approve on thanks" : "need /guide review")
                + ").");
    }

    @Override
    public void onDisable() {
        if (engine != null) {
            engine.flushCache();
            engine.flushPeer();
        }
        saveToggles();
    }

    // ---------- wiring accessors for the listener ----------

    GuideEngine engine() {
        return engine;
    }

    GuideConfig config() {
        return config;
    }

    String prefix() {
        return "§a[" + config.personaName + "]§r ";
    }

    // ---------- loading ----------

    /**
     * Build everything into locals and swap the live fields only when
     * every file parsed - a bad edit on reload keeps the old engine.
     */
    private void buildEngine() throws IOException {
        Path dir = getDataFolder().toPath();
        GuideConfig newConfig = GuideConfig.fromYaml(readFile(dir.resolve("config.yml")));
        IntentSet intents = IntentSet.fromYaml(readFile(dir.resolve("intents.yml")));
        KnowledgeBase newKnowledge = loadKnowledge(dir, newConfig);
        UnansweredLog log = newConfig.unansweredEnabled
                ? new UnansweredLog(dir.resolve(newConfig.unansweredFile), newConfig.unansweredMaxEntries)
                : null;
        // 0.2.0 AI fallback wiring. The provider is always built (it
        // is inert without a key); the engine decides per question
        // whether the config lets it engage. Provider calls run on
        // the server's async scheduler - never on a chat/command
        // thread - and answers come back through each call site's
        // sink, which delivers on the main thread.
        BukkitStats stats = new BukkitStats();
        AiLog aiLog = new AiLog(dir.resolve(newConfig.aiLogFile), newConfig.aiLogMaxEntries);
        // 0.2.1 learn wiring. The cache loads fail-soft (a corrupt
        // cache file is an empty cache plus a warning, never a dead
        // plugin); learned.yml parses fail-soft the same way - both
        // are re-read on every build, so /guide reload picks up
        // learned.yml edits and cache state from disk.
        LearnedAnswers.LoadResult learnedLoad = LearnedAnswers.load(
                readFileIfExists(dir.resolve(newConfig.learnedFile)));
        if (learnedLoad.warning() != null) {
            getLogger().warning("learned answers: " + learnedLoad.warning());
        }
        LearnCache learnCache = new LearnCache(
                dir.resolve(newConfig.aiCacheFile), newConfig.aiCacheMaxEntries);
        if (learnCache.loadWarning() != null) {
            getLogger().warning("ai learn-cache: " + learnCache.loadWarning());
        }
        // Peer-learning wiring (0.3.0). The review queue loads
        // fail-soft like the cache (corrupt file = empty queue + a
        // warning). The 0.2.3 store (peer-answers.yml) is no longer
        // read: nothing in it was reviewed, so none of it is served.
        // The 0.2.3 AI adjudicator is gone with it.
        HttpAiProvider aiProvider = new HttpAiProvider(newConfig.aiEndpoint, newConfig.aiApiKey);
        PeerCandidates peerStore = new PeerCandidates(
                dir.resolve(newConfig.peerReviewFile), newConfig.peerMaxEntries);
        if (peerStore.loadWarning() != null) {
            getLogger().warning("peer review queue: " + peerStore.loadWarning());
        }
        PeerTip newTip = new PeerTip(dir.resolve(newConfig.peerTipFile));
        newTip.enabled = newConfig.peerTipEnabled;
        if (newTip.loadWarning() != null) {
            getLogger().warning("player-answer tip: " + newTip.loadWarning());
        }
        com.winkreyes.excelguide.core.PeerLog peerLog =
                new com.winkreyes.excelguide.core.PeerLog(
                        dir.resolve(newConfig.peerLogFile), newConfig.peerLogMaxEntries);
        GuideEngine fresh = new GuideEngine(newConfig, intents, newKnowledge, stats,
                System::currentTimeMillis, log,
                aiProvider,
                new BukkitFacts(stats), aiLog,
                task -> Bukkit.getScheduler().runTaskAsynchronously(this, task),
                learnCache, learnedLoad.answers(), peerStore, peerLog);
        // 0.3.1: AI failures and ai.log write failures reach the console.
        fresh.setWarnSink(msg -> getLogger().warning(msg));
        // 0.3.2: delayed plain-chat answers arrive later, by player id,
        // from the timer or the AI thread: hop to the main thread and
        // deliver only if the asker is still online.
        fresh.setDelayedSink((id, replies) -> Bukkit.getScheduler().runTask(this, () -> {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline() && !replies.isEmpty()) {
                deliver(p, replies);
            }
        }));
        // A new answer waiting for review: tell online ops (chat can
        // arrive on an async thread, so hop to the main thread first).
        fresh.setPeerNotifier(msg -> Bukkit.getScheduler().runTask(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("excelguide.admin")) {
                    p.sendMessage(prefix() + msg);
                }
            }
        }));
        if (Files.exists(togglesFile)) {
            fresh.loadToggles(Files.readString(togglesFile, StandardCharsets.UTF_8));
        }
        config = newConfig;
        knowledge = newKnowledge;
        intentCount = intents.intents().size();
        engine = fresh;
        peerTip = newTip;
    }

    private KnowledgeBase loadKnowledge(Path dir, GuideConfig cfg) throws IOException {
        if ("bundled".equalsIgnoreCase(cfg.knowledgeSource)) {
            return KnowledgeBase.fromYaml(readBundled("knowledge.yml"));
        }
        Path file = dir.resolve(cfg.knowledgeFile);
        if (Files.exists(file)) {
            return KnowledgeBase.fromYaml(Files.readString(file, StandardCharsets.UTF_8));
        }
        getLogger().warning("knowledge file " + cfg.knowledgeFile
                + " not found in data folder; using the bundled copy.");
        return KnowledgeBase.fromYaml(readBundled("knowledge.yml"));
    }

    private void saveBundled(String name) {
        Path out = getDataFolder().toPath().resolve(name);
        if (!Files.exists(out)) {
            saveResource(name, false);
        }
    }

    private String readFile(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    /** File contents, or null when absent (the learned/cache files are optional). */
    private String readFileIfExists(Path file) {
        try {
            return Files.exists(file) ? Files.readString(file, StandardCharsets.UTF_8) : null;
        } catch (IOException e) {
            getLogger().warning("could not read " + file.getFileName() + ": " + e.getMessage());
            return null;
        }
    }

    private String readBundled(String name) throws IOException {
        try (InputStream in = getResource(name)) {
            if (in == null) {
                throw new IOException("bundled resource missing: " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void saveToggles() {
        if (engine == null || togglesFile == null) {
            return;
        }
        try {
            Files.createDirectories(togglesFile.getParent());
            Files.writeString(togglesFile, engine.togglesText(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            getLogger().warning("could not save player toggles: " + e.getMessage());
        }
    }

    // ---------- commands ----------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("ask")) {
            return onAsk(sender, args);
        }
        return onGuide(sender, args);
    }

    private boolean onAsk(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("The Valley Guide answers players in game.");
            return true;
        }
        if (!player.hasPermission("excelguide.ask")) {
            player.sendMessage("You don't have permission to ask the Guide.");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage("§a[" + config.personaName + "]§r Ask me anything: /ask <question>");
            return true;
        }
        String question = String.join(" ", args);
        List<String> onlineNames = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            onlineNames.add(p.getName());
        }
        engine.noteNicknames(Nicknames.online()); // 0.3.4: nicknames count as names
        List<GuideEngine.Reply> replies = engine.handle(
                new GuideEngine.PlayerRef(player.getUniqueId(), player.getName()),
                question, GuideEngine.Door.COMMAND, onlineNames, false,
                asyncReplies -> Bukkit.getScheduler().runTask(this, () -> {
                    if (player.isOnline()) {
                        deliver(player, asyncReplies);
                    }
                }));
        deliver(player, replies);
        return true;
    }

    private boolean onGuide(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§a[" + config.personaName + "]§r /ask <question> - ask me anything about the server.");
            sender.sendMessage("/guide on|off - my chat-watching, your choice. /guide status - how I'm doing (admin).");
            sender.sendMessage("/guide learned - promoted answers + AI cache size (admin).");
            sender.sendMessage("/guide review - approve or reject answers players taught me (admin).");
            sender.sendMessage("/guide ok - hide the tip about \"A player said\" answers.");
            return true;
        }
        switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
            case "status" -> {
                if (!sender.hasPermission("excelguide.admin")) {
                    sender.sendMessage("Admin only.");
                    return true;
                }
                sender.sendMessage("§a[" + config.personaName + "]§r status:");
                sender.sendMessage(" knowledge: version " + knowledge.version() + ", "
                        + knowledge.size() + " chunks (source " + config.knowledgeSource + ")");
                sender.sendMessage(" doors: command=" + config.doorCommand + " summon=" + config.doorSummon
                        + " ambient=" + config.doorAmbient + " | wand self-restore: "
                        + config.wandSelfRestoreEnabled);
                sender.sendMessage(" answers served this run: " + engine.answersServed()
                        + " | unanswered logged: " + engine.unansweredCount());
                sender.sendMessage(" ai fallback: " + (engine.aiActive() ? "ON" : "off")
                        + " (provider " + config.aiProviderName + ", model " + config.aiModel
                        + ") | ai calls today: " + engine.aiCallsToday()
                        + " | ai answered this run: " + engine.aiAnsweredCount()
                        + " | ai failed this run: " + engine.aiFailedCount());
                sender.sendMessage(" delayed chat answers: " + (engine.delayedActive() ? "ON" : "off")
                        + " (" + config.delayedDelaySeconds + "s, AI " + config.delayedAiTiming + ")"
                        + " | waiting now: " + engine.delayedPendingCount()
                        + " | this run: " + engine.delayedScheduledCount() + " seen, "
                        + engine.delayedPostedCount() + " answered, "
                        + engine.delayedHeldCount() + " left to players, "
                        + engine.delayedCancelledCount() + " cancelled, "
                        + engine.delayedKeptPrivateCount() + " kept private"
                        + " | reply " + config.delayedReplyChannel);
                sender.sendMessage(" learned answers: " + engine.learnedCount()
                        + " | ai cache: " + engine.aiCacheSize() + " entries, "
                        + engine.aiCacheServedCount() + " served this run");
                sender.sendMessage(" peer learning: " + (engine.peerEnabled() ? "ON" : "off")
                        + " | player answers: " + engine.peerLearnedCount() + " approved, "
                        + engine.peerReviewCount() + " waiting for /guide review, "
                        + engine.peerOpenWindows() + " questions listening | "
                        + engine.peerServedCount() + " served this run");
                return true;
            }
            case "learned" -> {
                if (!sender.hasPermission("excelguide.admin")) {
                    sender.sendMessage("Admin only.");
                    return true;
                }
                sender.sendMessage("§a[" + config.personaName + "]§r learned answers: "
                        + engine.learnedCount() + " | ai cache: " + engine.aiCacheSize()
                        + " entries, " + engine.aiCacheServedCount() + " served this run");
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission("excelguide.admin")) {
                    sender.sendMessage("Admin only.");
                    return true;
                }
                try {
                    String toggles = engine.togglesText();
                    engine.flushCache(); // persist hit counters before the swap
                    engine.flushPeer();
                    buildEngine();
                    engine.loadToggles(toggles);
                    sender.sendMessage("§a[" + config.personaName + "]§r Reloaded: knowledge "
                            + knowledge.version() + " (" + knowledge.size() + " chunks).");
                } catch (Exception e) {
                    sender.sendMessage("Reload failed, old knowledge kept: " + e.getMessage());
                    getLogger().warning("reload failed: " + e.getMessage());
                }
                return true;
            }
            case "review" -> {
                if (!sender.hasPermission("excelguide.admin")) {
                    sender.sendMessage("Admin only.");
                    return true;
                }
                return onReview(sender, args);
            }
            case "off", "on" -> {
                boolean off = args[0].equalsIgnoreCase("off");
                if (args.length >= 2) {
                    if (!sender.hasPermission("excelguide.admin")) {
                        sender.sendMessage("Admin only for other players.");
                        return true;
                    }
                    Player target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) {
                        sender.sendMessage("No online player named " + args[1] + ".");
                        return true;
                    }
                    engine.setPlayerOff(target.getUniqueId(), off);
                    saveToggles();
                    sender.sendMessage("Guide chat-watching " + (off ? "off" : "on")
                            + " for " + target.getName() + ".");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Console: /guide off <player>");
                    return true;
                }
                engine.setPlayerOff(player.getUniqueId(), off);
                saveToggles();
                player.sendMessage("§a[" + config.personaName + "]§r My chat-watching is "
                        + (off ? "off for you. /ask still works any time." : "on for you again."));
                return true;
            }
            case "ok", "tipok" -> {
                return onTipOk(sender);
            }
            case "tipreset" -> {
                return onTipReset(sender, args);
            }
            default -> {
                sender.sendMessage("/guide [status|reload|on|off|learned|review|ok]");
                return true;
            }
        }
    }

    // ---------- /guide review (0.3.0) ----------

    /**
     * The owner review queue. Player-taught answers wait here and are
     * never served until approved.
     *   /guide review                 list waiting answers (Bedrock: a pop-up)
     *   /guide review list            the same list in chat, on any client
     *   /guide review show <id>       one answer in full
     *   /guide review approve <id>    serve it from now on as "A player said: ..."
     *   /guide review reject <id>     delete it
     *   /guide review approved        list approved answers
     *   /guide review remove <id>     stop serving an approved answer (deletes it)
     */
    private boolean onReview(CommandSender sender, String[] args) {
        String sub = args.length >= 2 ? args[1].toLowerCase(java.util.Locale.ROOT) : "";
        String id = args.length >= 3 ? args[2] : "";
        String p = prefix();
        switch (sub) {
            case "" -> {
                if (sender instanceof Player player && Bedrock.isBedrock(player)
                        && sendReviewForm(player)) {
                    return true;
                }
                listReview(sender);
            }
            case "list" -> listReview(sender);
            case "show" -> {
                PeerCandidates.Entry e = engine.peerFind(id);
                if (e == null) {
                    sender.sendMessage(p + "No answer #" + id + ". /guide review list");
                    return true;
                }
                showEntry(sender, e);
            }
            case "approve", "yes" -> {
                PeerCandidates.Entry ok = engine.peerApprove(id);
                sender.sendMessage(ok == null
                        ? p + "No waiting answer #" + id + ". /guide review list"
                        : p + "Approved #" + ok.id() + ". I'll answer \"" + ok.question()
                                + "\" with: " + config.peerServePrefix + ok.answer());
            }
            case "reject", "no", "remove" -> {
                PeerCandidates.Entry gone = engine.peerReject(id);
                sender.sendMessage(gone == null
                        ? p + "No answer #" + id + ". /guide review list"
                        : p + (gone.isApproved() ? "Removed" : "Rejected") + " #" + gone.id()
                                + " - I won't use it.");
            }
            case "approved" -> {
                List<PeerCandidates.Entry> list = engine.peerApprovedList();
                sender.sendMessage(p + list.size() + " approved player answer(s).");
                for (PeerCandidates.Entry e : list) {
                    sender.sendMessage(" #" + e.id() + " Q: " + clip(e.question(), 60) + " | A: "
                            + clip(e.answer(), 80) + " | served " + e.hits() + "x");
                }
                if (!list.isEmpty()) {
                    sender.sendMessage(" Stop using one: /guide review remove <id>");
                }
            }
            default -> sender.sendMessage(
                    "/guide review [list|show <id>|approve <id>|reject <id>|approved|remove <id>]");
        }
        return true;
    }

    private void listReview(CommandSender sender) {
        List<PeerCandidates.Entry> list = engine.peerReviewQueue();
        sender.sendMessage(prefix() + list.size() + " player answer(s) waiting for review."
                + (list.isEmpty() ? "" : " Nothing here is used until you approve it."));
        int shown = 0;
        for (PeerCandidates.Entry e : list) {
            if (shown++ >= 10) {
                sender.sendMessage(" ... and " + (list.size() - 10) + " more.");
                break;
            }
            sender.sendMessage(" #" + e.id() + " Q: " + clip(e.question(), 60) + " | A: "
                    + clip(e.answer(), 80) + " | " + e.how());
        }
        if (!list.isEmpty()) {
            sender.sendMessage(" /guide review approve <id>  |  /guide review reject <id>  |  show <id>");
        }
    }

    private void showEntry(CommandSender sender, PeerCandidates.Entry e) {
        sender.sendMessage(prefix() + "#" + e.id() + " (" + (e.isApproved() ? "approved" : "waiting") + ")");
        sender.sendMessage(" Question: " + e.question());
        sender.sendMessage(" Answer: " + e.answer());
        sender.sendMessage(" Why it's here: " + e.how() + " | asked " + e.asks() + "x");
        String blocked = engine.peerServeCheck(e);
        if (blocked != null) {
            sender.sendMessage(" Note: the filters would block this now (" + blocked + ").");
        }
        sender.sendMessage(e.isApproved() ? " /guide review remove " + e.id()
                : " /guide review approve " + e.id() + "  |  /guide review reject " + e.id());
    }

    private static String clip(String s, int n) {
        if (s == null) {
            return "";
        }
        return s.length() <= n ? s : s.substring(0, n - 3) + "...";
    }

    /** Bedrock: the queue as a pop-up. False when no form could be sent (caller falls back to chat). */
    private boolean sendReviewForm(Player player) {
        List<PeerCandidates.Entry> list = engine.peerReviewQueue();
        List<String> buttons = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (PeerCandidates.Entry e : list) {
            if (ids.size() >= 20) {
                break;
            }
            buttons.add(clip(e.question(), 40));
            ids.add(e.id());
        }
        buttons.add("Close");
        String content = list.isEmpty() ? "No player answers are waiting."
                : list.size() + " player answer(s) waiting. Nothing is used until you approve it.";
        String sent = bedrock.send(player, "Guide review", content, buttons,
                clicked -> Bukkit.getScheduler().runTask(this, () -> {
                    if (player.isOnline() && clicked >= 0 && clicked < ids.size()
                            && player.hasPermission("excelguide.admin")) {
                        sendEntryForm(player, ids.get(clicked));
                    }
                }));
        return sent != null;
    }

    private void sendEntryForm(Player player, String id) {
        PeerCandidates.Entry e = engine.peerFind(id);
        if (e == null || e.isApproved()) {
            player.sendMessage(prefix() + "That answer is no longer waiting.");
            return;
        }
        String blocked = engine.peerServeCheck(e);
        String content = "Question: " + e.question() + "\n\nAnswer: " + e.answer()
                + "\n\nWhy it's here: " + e.how()
                + (blocked == null ? "" : "\n\nNote: the filters would block this now (" + blocked + ").");
        String sent = bedrock.send(player, "Answer #" + e.id(), content,
                List.of("Approve", "Reject", "Back"),
                clicked -> Bukkit.getScheduler().runTask(this, () -> {
                    if (!player.isOnline() || !player.hasPermission("excelguide.admin")) {
                        return;
                    }
                    if (clicked == 0) {
                        onReview(player, new String[] {"review", "approve", id});
                    } else if (clicked == 1) {
                        onReview(player, new String[] {"review", "reject", id});
                    } else if (clicked == 2) {
                        sendReviewForm(player);
                    }
                }));
        if (sent == null) {
            showEntry(player, e);
        }
    }

    /** Main thread. Shared by /ask and the chat listener (0.3.0: also shows the player-answer tip). */
    void deliver(Player asker, List<GuideEngine.Reply> replies) {
        String prefix = prefix();
        boolean peerAnswer = false;
        // 0.3.3: one platform control note per reader per delivery
        // (PlatformText), after that reader's last line. Public replies
        // pick the note per reader, so a Bedrock player and a Java player
        // reading the same public answer each get their own controls.
        boolean notes = config.platformNotes;
        java.util.Map<java.util.UUID, String> pending = new java.util.LinkedHashMap<>();
        java.util.Map<java.util.UUID, Boolean> platform = new java.util.HashMap<>();
        for (GuideEngine.Reply reply : replies) {
            String line = prefix + reply.text();
            if (PeerTip.isPeerAnswer(reply.text(), config.peerServePrefix)) {
                peerAnswer = true;
            }
            if (reply.channel() == GuideEngine.Channel.PUBLIC) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(line);
                    if (notes) {
                        noteFor(p, reply.text(), pending, platform);
                    }
                }
            } else if (asker.isOnline()) {
                asker.sendMessage(line);
                if (notes) {
                    noteFor(asker, reply.text(), pending, platform);
                }
            }
        }
        for (java.util.Map.Entry<java.util.UUID, String> e : pending.entrySet()) {
            Player p = Bukkit.getPlayer(e.getKey());
            if (p != null && p.isOnline()) {
                p.sendMessage(prefix + "§7" + e.getValue());
            }
        }
        if (peerAnswer && asker.isOnline()) {
            showPeerTip(asker);
        }
    }

    private static void noteFor(Player reader, String text, java.util.Map<java.util.UUID, String> pending,
                                java.util.Map<java.util.UUID, Boolean> platform) {
        java.util.UUID id = reader.getUniqueId();
        if (pending.containsKey(id)) {
            return;
        }
        boolean br = platform.computeIfAbsent(id, k -> Bedrock.isBedrock(reader));
        String note = com.winkreyes.excelguide.core.PlatformText.noteFor(text, br);
        if (note != null) {
            pending.put(id, note);
        }
    }

    // ---------- first-use tip for "A player said" answers (0.3.0) ----------

    private void showPeerTip(Player player) {
        PeerTip tip = peerTip;
        if (tip == null || !tip.shouldShow(player.getUniqueId())) {
            return;
        }
        boolean br = Bedrock.isBedrock(player);
        List<String> lines = PeerTip.lines(br);
        for (int i = 0; i < lines.size() - 1; i++) {
            player.sendMessage((i == 0 ? "§6" : "§e") + lines.get(i));
        }
        if (!br) {
            player.sendMessage(net.kyori.adventure.text.Component.text("Click ",
                            net.kyori.adventure.text.format.NamedTextColor.YELLOW)
                    .append(net.kyori.adventure.text.Component.text("[OK]",
                                    net.kyori.adventure.text.format.NamedTextColor.GREEN)
                            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                                    net.kyori.adventure.text.Component.text("Hide this tip")))
                            .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/guide ok")))
                    .append(net.kyori.adventure.text.Component.text(" to hide this tip.",
                            net.kyori.adventure.text.format.NamedTextColor.YELLOW)));
            return;
        }
        player.sendMessage("§e" + lines.get(lines.size() - 1));
        if (!tip.takeForm(player.getUniqueId())) {
            return;
        }
        // A moment later, so the pop-up does not cover the answer itself.
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!player.isOnline() || tip.acked(player.getUniqueId())) {
                return;
            }
            String sent = bedrock.send(player, PeerTip.TITLE, PeerTip.formContent(),
                    List.of(PeerTip.OK_BUTTON), clicked -> {
                        if (clicked != 0) {
                            return; // closed with X: not an OK, the chat tip keeps coming
                        }
                        Bukkit.getScheduler().runTask(this, () -> {
                            if (player.isOnline() && tip.ack(player.getUniqueId())) {
                                player.sendMessage(prefix() + PeerTip.DONE_ACK);
                            }
                        });
                    });
            if (sent == null) {
                getLogger().info("Player-answer tip pop-up not sent (no Geyser form connection); chat tip stays.");
            }
        }, 40L);
    }

    private boolean onTipOk(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        PeerTip tip = peerTip;
        if (tip == null) {
            return true;
        }
        player.sendMessage(prefix() + (tip.ack(player.getUniqueId()) ? PeerTip.DONE_ACK : PeerTip.ALREADY));
        return true;
    }

    private boolean onTipReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("excelguide.admin")) {
            sender.sendMessage("Admin only.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("/guide tipreset <player>");
            return true;
        }
        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[1]);
        if (target == null) {
            sender.sendMessage("No player " + args[1] + " has been seen here.");
            return true;
        }
        PeerTip tip = peerTip;
        boolean ch = tip != null && tip.reset(target.getUniqueId());
        sender.sendMessage(prefix() + (ch ? "The player-answer tip will show again for " + args[1] + "."
                : args[1] + " had not said OK to it yet."));
        return true;
    }
}
