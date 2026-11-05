package rig;

import com.mojang.authlib.GameProfile;
import com.winkreyes.excelbraingames.ExcelBrainGames;
import com.winkreyes.excelbraingames.GameConfig;
import com.winkreyes.excelbraingames.PlayerData;
import com.winkreyes.excelbraingames.core.Payouts;
import com.winkreyes.excelbraingames.games.memory.MemoryRound;
import com.winkreyes.excelbraingames.games.wordsearch.WordSearchRound;
import com.winkreyes.excelbraingames.gui.BrainHolder;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;

/**
 * Rig-only driver for the ExcelBrainGames Phase 1 proof. NOT shipped.
 *
 * A fake player (NMS ServerPlayer on an embedded channel, the fleet rig
 * pattern) plays REAL rounds through the plugin's own GUI: /games opens the
 * picker, and every tap is a genuine InventoryClickEvent fired into the
 * plugin manager, so the exact event path a phone tap takes is exercised.
 * The driver reads round state through the plugin's public API only to aim
 * its taps (which cells hold which word) and to assert outcomes; payouts are
 * verified against the real Vault/Essentials balance.
 *
 * Boot A: kid-tier Word Search as a non-kid player (Mike's half-payout
 * amendment), Standard Word Search, Memory Match with a deliberate miss,
 * pays-once re-settle attempt, then a Challenge round seeded with one word
 * found. Boot B: the seeded round must resume intact after the restart and
 * complete with the correct payout.
 */
public final class BrainRig extends JavaPlugin {

    private Player player;
    private ExcelBrainGames bg;
    private Economy econ;
    private String phase = "A";
    private WordSearchRound completedRef; // for the pays-once re-settle attempt
    private long balanceBeforePhase;

    void later(long ticks, Runnable r) {
        new BukkitRunnable() { @Override public void run() { r.run(); } }.runTaskLater(this, ticks);
    }

    void result(String name, boolean ok, String detail) {
        getLogger().info("RIGRESULT " + name + " " + (ok ? "PASS" : "FAIL") + " " + detail);
    }

    void note(String detail) { getLogger().info("RIGRESULT NOTE " + detail); }

    @Override
    public void onEnable() {
        try {
            File f = new File(getDataFolder(), "phase.txt");
            if (f.exists()) phase = Files.readString(f.toPath()).trim();
        } catch (Exception ignored) { }
        later(60, () -> {
            try { setup(); }
            catch (Throwable t) { note("SETUP THREW " + t); t.printStackTrace(); }
        });
    }

    // ------------------------------------------------------------ fake player

    Player fakePlayer(World world, String name) {
        MinecraftServer server = ((org.bukkit.craftbukkit.CraftServer) getServer()).getServer();
        ServerLevel level = ((org.bukkit.craftbukkit.CraftWorld) world).getHandle();
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name);
        ServerPlayer sp = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
        Connection conn = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(conn);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        sp.connection = new ServerGamePacketListenerImpl(server, conn, sp, cookie);
        server.getPlayerList().placeNewPlayer(conn, sp, cookie);
        // placeNewPlayer fires the join machinery itself; no manual
        // PlayerJoinEvent (its annotated constructor needs the JetBrains
        // annotations jar, which is not in this rig's compile classpath).
        return sp.getBukkitEntity();
    }

    // ------------------------------------------------------------ gui driving

    BrainHolder holder() {
        var h = player.getOpenInventory().getTopInventory().getHolder();
        return h instanceof BrainHolder bh ? bh : null;
    }

    Integer slotFor(String actionPrefix) {
        BrainHolder h = holder();
        if (h == null) return null;
        for (Map.Entry<Integer, String> e : h.actions.entrySet()) {
            if (e.getValue().equals(actionPrefix) || e.getValue().startsWith(actionPrefix)) return e.getKey();
        }
        return null;
    }

    boolean clickAction(String action) {
        Integer slot = slotFor(action);
        if (slot == null) { note("no slot for action " + action + " (view=" + (holder() == null ? "none" : holder().view) + ")"); return false; }
        click(slot);
        return true;
    }

    void click(int slot) {
        InventoryView view = player.getOpenInventory();
        InventoryClickEvent ev = new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER,
                slot, ClickType.LEFT, InventoryAction.NOTHING);
        Bukkit.getPluginManager().callEvent(ev);
    }

    void clickCell(int idx, int cols) {
        int slot = (idx / cols) * 9 + (idx % cols);
        Integer found = null;
        BrainHolder h = holder();
        for (Map.Entry<Integer, String> e : h.actions.entrySet()) {
            if (e.getValue().equals("cell:" + idx)) found = e.getKey();
        }
        click(found != null ? found : slot);
    }

    long balanceCents() {
        return Math.round(econ.getBalance(player) * 100.0);
    }

    PlayerData data() { return bg.service().data(player); }

    // ------------------------------------------------------------ phases

    void setup() {
        bg = (ExcelBrainGames) Bukkit.getPluginManager().getPlugin("ExcelBrainGames");
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        econ = rsp == null ? null : rsp.getProvider();
        result("SETUP", bg != null && econ != null,
                "plugin=" + (bg != null) + " economy=" + (econ == null ? "none" : econ.getName()));
        player = fakePlayer(Bukkit.getWorlds().get(0), "RigPlayer");
        note("phase=" + phase + " uuid=" + player.getUniqueId());
        if (phase.equals("B")) later(20, this::phaseB1);
        else later(20, this::phaseA1);
    }

    // ---- Boot A ----

    void phaseA1() {
        balanceBeforePhase = balanceCents();
        result("A-BALANCE-START", balanceBeforePhase == 0, "balance=" + balanceBeforePhase);
        Bukkit.dispatchCommand(player, "games");
        later(10, () -> {
            BrainHolder h = holder();
            result("A-PICKER-OPENS", h != null && h.view == BrainHolder.View.PICKER,
                    "view=" + (h == null ? "none" : h.view));
            later(5, this::phaseA2KidRound);
        });
    }

    void phaseA2KidRound() {
        // Word Search -> Animals -> Kid tier. RigPlayer is NOT kid-flagged:
        // Mike's amendment pays grown-ups HALF the kid payout ($40 -> $20),
        // plus the $10 day-1 streak bonus on this first paid completion.
        if (!clickAction("game:wordsearch")) { result("A-KID-START", false, "no game tile"); return; }
        later(5, () -> {
            if (!clickAction("theme:animals")) { result("A-KID-START", false, "no theme"); return; }
            later(5, () -> {
                if (!clickAction("tier:wordsearch:kid")) { result("A-KID-START", false, "no kid tier (gate?)"); return; }
                later(5, () -> {
                    PlayerData pd = data();
                    result("A-KID-START", pd.wordSearch != null && pd.wordSearch.tierId().equals("kid"),
                            "round=" + (pd.wordSearch == null ? "none" : pd.wordSearch.tierId()));
                    solveWordSearch(() -> {
                        long delta = balanceCents() - balanceBeforePhase;
                        long expectedBase = Payouts.adjust(4000, 0, true, false); // 2000
                        result("A-KID-HALF-PAYOUT", delta == expectedBase + 1000,
                                "delta=" + delta + " expected=" + (expectedBase + 1000) + " (half kid payout + streak)");
                        result("A-KID-CLEARED", data().wordSearch == null, "round cleared after completion");
                        balanceBeforePhase = balanceCents();
                        later(5, this::phaseA3StandardRound);
                    });
                });
            });
        });
    }

    void phaseA3StandardRound() {
        Bukkit.dispatchCommand(player, "games");
        later(8, () -> {
            if (!clickAction("game:wordsearch")) { result("A-STD-START", false, "no game tile"); return; }
            later(5, () -> {
                if (!clickAction("theme:animals")) { result("A-STD-START", false, "no theme"); return; }
                later(5, () -> {
                    if (!clickAction("tier:wordsearch:standard")) { result("A-STD-START", false, "no standard tier"); return; }
                    later(5, () -> {
                        PlayerData pd = data();
                        result("A-STD-START", pd.wordSearch != null, "round started");
                        completedRef = pd.wordSearch;
                        long started = pd.wordSearch.startedMillis();
                        solveWordSearch(() -> {
                            long delta = balanceCents() - balanceBeforePhase;
                            long elapsedSec = (System.currentTimeMillis() - started) / 1000;
                            int bonusPct = elapsedSec <= 360 ? 20 : 0;
                            long expected = Payouts.adjust(9000, bonusPct, false, false);
                            result("A-STD-PAYOUT", delta == expected,
                                    "delta=" + delta + " expected=" + expected + " (speedBonus=" + (bonusPct > 0) + ")");
                            // pays-once: settling the same round object again must be refused
                            Object again = bg.service().completeWordSearch(player, data(), completedRef);
                            result("A-PAYS-ONCE", again == null && balanceCents() - balanceBeforePhase == expected,
                                    "re-settle refused, balance unchanged");
                            balanceBeforePhase = balanceCents();
                            later(5, this::phaseA4Memory);
                        });
                    });
                });
            });
        });
    }

    void phaseA4Memory() {
        Bukkit.dispatchCommand(player, "games");
        later(8, () -> {
            if (!clickAction("game:memory")) { result("A-MEM-START", false, "no game tile"); return; }
            later(5, () -> {
                if (!clickAction("tier:memory:standard")) { result("A-MEM-START", false, "no standard tier"); return; }
                later(5, () -> {
                    PlayerData pd = data();
                    result("A-MEM-START", pd.memory != null && pd.memory.pairs() == 8,
                            "round=" + (pd.memory == null ? "none" : pd.memory.tierId()));
                    MemoryRound round = pd.memory;
                    // deliberate miss: two cards with different symbols
                    int a = 0, foundB = -1;
                    for (int i = 1; i < round.cardCount(); i++) {
                        if (round.symbolAt(i) != round.symbolAt(0)) { foundB = i; break; }
                    }
                    final int b = foundB;
                    clickCard(a);
                    later(4, () -> {
                        clickCard(b);
                        result("A-MEM-MISS", round.missPending(), "deliberate miss shows both cards");
                        waitMissResolved(round, 0);
                    });
                });
            });
        });
    }

    void waitMissResolved(MemoryRound round, int tries) {
        if (!round.missPending()) {
            result("A-MEM-MISS-RESOLVED", round.stateAt(0) == MemoryRound.DOWN, "missed cards flipped back");
            solveMemory(round);
            return;
        }
        if (tries > 40) { result("A-MEM-MISS-RESOLVED", false, "miss never resolved"); return; }
        later(5, () -> waitMissResolved(round, tries + 1));
    }

    void solveMemory(MemoryRound round) {
        // flip every pair by deck knowledge, a few ticks apart
        int[] order = new int[round.cardCount()];
        int n = 0;
        boolean[] done = new boolean[round.cardCount()];
        for (int i = 0; i < round.cardCount(); i++) {
            if (done[i]) continue;
            for (int j = i + 1; j < round.cardCount(); j++) {
                if (!done[j] && round.symbolAt(j) == round.symbolAt(i)) {
                    order[n++] = i; order[n++] = j; done[i] = done[j] = true; break;
                }
            }
        }
        flipNext(round, order, 0);
    }

    void flipNext(MemoryRound round, int[] order, int pos) {
        if (pos >= order.length) {
            later(10, () -> {
                long delta = balanceCents() - balanceBeforePhase;
                int bonusPct = round.flips() <= 34 ? 20 : 0;
                long expected = Payouts.adjust(5500, bonusPct, false, false);
                result("A-MEM-PAYOUT", data().memory == null && delta == expected,
                        "delta=" + delta + " expected=" + expected + " flips=" + round.flips());
                balanceBeforePhase = balanceCents();
                later(5, this::phaseA5SeedResume);
            });
            return;
        }
        clickCard(order[pos]);
        later(4, () -> flipNext(round, order, pos + 1));
    }

    void clickCard(int card) {
        Integer slot = slotFor("card:" + card);
        if (slot != null) click(slot);
    }

    void phaseA5SeedResume() {
        // Start a Challenge round, find exactly one word, leave it saved.
        Bukkit.dispatchCommand(player, "games");
        later(8, () -> {
            if (!clickAction("game:wordsearch")) { result("A-SEED", false, "no game tile"); return; }
            later(5, () -> {
                if (!clickAction("theme:animals")) { result("A-SEED", false, "no theme"); return; }
                later(5, () -> {
                    if (!clickAction("tier:wordsearch:challenge")) { result("A-SEED", false, "no challenge tier"); return; }
                    later(5, () -> {
                        PlayerData pd = data();
                        WordSearchRound round = pd.wordSearch;
                        result("A-SEED-START", round != null && round.placements().size() == 10,
                                "placements=" + (round == null ? -1 : round.placements().size()));
                        WordSearchRound.Placement p = round.placements().get(0);
                        clickCell(p.startIdx(round.cols()), round.cols());
                        later(4, () -> {
                            clickCell(p.endIdx(round.cols()), round.cols());
                            later(4, () -> {
                                result("A-SEED-ONE-FOUND", round.found().size() == 1,
                                        "found=" + round.found().size());
                                note("RIGREADY STOP-A");
                            });
                        });
                    });
                });
            });
        });
    }

    // ---- Boot B ----

    void phaseB1() {
        PlayerData pd = data();
        result("B-RESUME-STATE", pd.wordSearch != null
                        && pd.wordSearch.tierId().equals("challenge")
                        && pd.wordSearch.placements().size() == 10
                        && pd.wordSearch.found().size() == 1,
                "round=" + (pd.wordSearch == null ? "none"
                        : pd.wordSearch.tierId() + " found=" + pd.wordSearch.found().size()
                        + "/" + pd.wordSearch.placements().size()));
        balanceBeforePhase = balanceCents();
        WordSearchRound round = pd.wordSearch;
        long started = round.startedMillis();
        Bukkit.dispatchCommand(player, "games");
        later(8, () -> {
            if (!clickAction("game:wordsearch")) { result("B-RESUME-OPEN", false, "no game tile"); return; }
            later(5, () -> {
                BrainHolder h = holder();
                result("B-RESUME-OPEN", h != null && h.view == BrainHolder.View.RESUME,
                        "view=" + (h == null ? "none" : h.view));
                if (!clickAction("resume:wordsearch")) { result("B-RESUME-CLICK", false, "no resume action"); return; }
                later(5, () -> {
                    BrainHolder h2 = holder();
                    result("B-RESUME-CLICK", h2 != null && h2.view == BrainHolder.View.WS_BOARD,
                            "view=" + (h2 == null ? "none" : h2.view));
                    solveWordSearch(() -> {
                        long delta = balanceCents() - balanceBeforePhase;
                        long elapsedSec = (System.currentTimeMillis() - started) / 1000;
                        int bonusPct = elapsedSec <= 600 ? 20 : 0;
                        long expected = Payouts.adjust(15000, bonusPct, false, false);
                        result("B-RESUME-PAYOUT", delta == expected,
                                "delta=" + delta + " expected=" + expected + " elapsedSec=" + elapsedSec);
                        note("RIGREADY STOP-B");
                    });
                });
            });
        });
    }

    // ---- shared solving ----

    /** Tap every unfound placement's endpoints through the open board view. */
    void solveWordSearch(Runnable done) {
        PlayerData pd = data();
        WordSearchRound round = pd.wordSearch;
        java.util.List<WordSearchRound.Placement> remaining = new java.util.ArrayList<>();
        for (int i = 0; i < round.placements().size(); i++) {
            if (!round.found().contains(i)) remaining.add(round.placements().get(i));
        }
        tapNext(round, remaining, 0, done);
    }

    void tapNext(WordSearchRound round, java.util.List<WordSearchRound.Placement> remaining, int pos, Runnable done) {
        if (pos >= remaining.size()) { later(10, done); return; }
        WordSearchRound.Placement p = remaining.get(pos);
        clickCell(p.startIdx(round.cols()), round.cols());
        later(3, () -> {
            clickCell(p.endIdx(round.cols()), round.cols());
            later(3, () -> tapNext(round, remaining, pos + 1, done));
        });
    }
}
