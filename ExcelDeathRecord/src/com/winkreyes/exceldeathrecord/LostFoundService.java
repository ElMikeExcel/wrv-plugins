package com.winkreyes.exceldeathrecord;

import com.winkreyes.exceldeathrecord.core.BurnFormat;
import com.winkreyes.exceldeathrecord.core.BurnIndex;
import com.winkreyes.exceldeathrecord.core.DeathRecord;
import com.winkreyes.exceldeathrecord.core.DestructionRecord;
import com.winkreyes.exceldeathrecord.core.LostFoundCsv;
import com.winkreyes.exceldeathrecord.core.LostFoundRules;
import com.winkreyes.exceldeathrecord.core.LostFoundState;
import com.winkreyes.exceldeathrecord.core.RecordFormat;
import com.winkreyes.exceldeathrecord.core.RestoreCsv;
import com.winkreyes.exceldeathrecord.core.TypeGlob;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 0.3.0 Lost &amp; Found: a player brings back one of their OWN Burn
 * Ledger entries (lava, fire, void, despawned valuables) from the
 * last 7 days, at most once every 24 hours. The first recovery is
 * free for a player with under $10,000; after that each costs the
 * configured price ($16,000).
 *
 * The confirm is one main-thread step under a lock, in this order:
 *   1. the offer the player tapped must still be open (taken out at
 *      once, so a double tap or a stale screen can never run twice);
 *   2. the record is re-read: owner, 7 days, not yet brought back;
 *   3. the 24 h cooldown;
 *   4. the slot plan (RestoreService.prepare): no room, no charge;
 *   5. the price is decided again from the balance NOW; if it is not
 *      what the screen showed, nothing happens and the new price is
 *      shown instead;
 *   6. the money is taken (Vault);
 *   7. the once-only restore mark is written (restoredBy SELF); if
 *      that fails the money goes back;
 *   8. the player state is saved (free used, cooldown start);
 *   9. the item is given; if that fails, the mark and the state are
 *      put back and the money is refunded;
 *  10. restores.csv (staff SELF) and lostfound.csv get their rows.
 */
final class LostFoundService {

    static final String PERM = "exceldeathrecord.lostfound";
    static final String SELF = "SELF";
    static final int MAX_LIST = 45;

    /** An item the player tapped, waiting for Yes. */
    static final class Offer {
        final long id;
        final File file;
        final String folder;
        final DestructionRecord record;
        final LostFoundRules.Price price;
        Offer(long id, File file, String folder, DestructionRecord record, LostFoundRules.Price price) {
            this.id = id; this.file = file; this.folder = folder; this.record = record; this.price = price;
        }
    }

    /** What a confirm did. */
    static final class Result {
        boolean ok;
        boolean silent;          // a duplicate tap of an offer that already went through
        Offer newOffer;          // set when the price changed: show this one instead
        final List<String> lines = new ArrayList<>();
    }

    private final ExcelDeathRecord plugin;
    private final RecordStore store;
    private final RestoreService restore;
    private Money money = Money.none();
    final LostFoundRules.Settings settings;
    final TypeGlob types;
    final boolean enabled;
    final boolean notifyOwner;
    final String notifyText;
    private final long notifyThrottleMs = 20_000L;
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<UUID, Offer> offers = new HashMap<>();
    private final Map<UUID, Long> doneOffers = new HashMap<>();
    private final Map<UUID, Long> lastNotice = new HashMap<>();
    private long nextOfferId = 1;

    LostFoundService(ExcelDeathRecord plugin, RecordStore store, RestoreService restore,
                     LostFoundRules.Settings settings, TypeGlob types, boolean enabled,
                     boolean notifyOwner, String notifyText) {
        this.plugin = plugin;
        this.store = store;
        this.restore = restore;
        this.settings = settings;
        this.types = types;
        this.enabled = enabled;
        this.notifyOwner = notifyOwner;
        this.notifyText = notifyText;
    }

    void hookMoney() { money = Money.find(); }
    Money money() { return money; }

    // ---- reads ----

    /** The player's recoverable entries, menu order (valuables first, newest first). */
    List<BurnIndex.Entry> eligible(Player p) {
        String uuid = p.getUniqueId().toString();
        long now = System.currentTimeMillis();
        List<BurnIndex.Entry> ok = new ArrayList<>();
        for (BurnIndex.Entry e : store.readBurnFolder(uuid)) {
            if (LostFoundRules.ineligible(e.record, e.folder, uuid, now, settings) == null) ok.add(e);
        }
        return LostFoundRules.menuOrder(ok, types, MAX_LIST);
    }

    LostFoundState state(UUID uuid) {
        try { return store.readLostFoundState(uuid); }
        catch (Exception e) {
            plugin.getLogger().warning("ExcelDeathRecord [Lost & Found]: could not read state for " + uuid + ": " + e.getMessage());
            return null;
        }
    }

    void tipOk(UUID id) {
        LostFoundState st = state(id);
        if (st == null || st.tipOk) return;
        st.tipOk = true;
        try { store.writeLostFoundState(id, st); }
        catch (Exception e) { plugin.getLogger().warning("ExcelDeathRecord [Lost & Found]: could not save tip OK: " + e.getMessage()); }
    }

    LostFoundRules.Price quote(Player p, LostFoundState st) {
        return LostFoundRules.decide(st, money.ready() ? money.balance(p) : 0, settings);
    }

    long cooldownLeft(LostFoundState st) {
        return LostFoundRules.cooldownLeft(st, System.currentTimeMillis(), settings);
    }

    /** Remember what the player tapped; replaces any earlier offer. */
    Offer offer(Player p, BurnIndex.Entry e, LostFoundRules.Price price) {
        Offer o = new Offer(nextOfferId++, store.burnFile(e.folder, e.fileName), e.folder, e.record, price);
        offers.put(p.getUniqueId(), o);
        return o;
    }

    void dropOffer(Player p) { offers.remove(p.getUniqueId()); }

    // ---- the confirm ----

    Result confirm(Player p, long offerId) {
        Result r = new Result();
        UUID uuid = p.getUniqueId();
        Offer o = offers.get(uuid);
        if (o == null || o.id != offerId) {
            Long done = doneOffers.get(uuid);
            if (done != null && done == offerId) { r.silent = true; return r; }
            r.lines.add("That choice is no longer open. Open Lost & Found again.");
            return r;
        }
        offers.remove(uuid);
        if (!lock.tryLock()) { r.lines.add("Lost & Found is busy. Try again in a moment."); return r; }
        try {
            return confirmLocked(p, o, r);
        } finally {
            lock.unlock();
        }
    }

    private Result confirmLocked(Player p, Offer o, Result r) {
        UUID uuid = p.getUniqueId();
        if (!enabled) { r.lines.add("Lost & Found is closed right now."); return r; }
        if (!money.ready()) { r.lines.add("Lost & Found can't reach the server's money system right now. Nothing was charged."); return r; }
        long now = System.currentTimeMillis();
        String original;
        DestructionRecord rec;
        try {
            original = store.readText(o.file);
            rec = BurnFormat.read(original);
        } catch (Exception e) {
            r.lines.add("That item's record could not be read. Nothing was charged.");
            return r;
        }
        String why = LostFoundRules.ineligible(rec, o.folder, uuid.toString(), now, settings);
        if (why != null) { r.lines.add(why + " Nothing was charged."); return r; }
        LostFoundState st = state(uuid);
        if (st == null) { r.lines.add("Your Lost & Found info could not be read. Nothing was charged."); return r; }
        long cd = LostFoundRules.cooldownLeft(st, now, settings);
        if (cd > 0) {
            r.lines.add("You can bring back one item every " + LostFoundRules.durationText(settings.cooldownMillis)
                    + ". Your next one is ready in " + LostFoundRules.durationText(cd) + ". Nothing was charged.");
            return r;
        }

        RestoreService.Source src = new RestoreService.Source();
        src.burned = true;
        src.file = o.file;
        src.sourceName = rec.ownerName;
        RestoreService.Prepared pr = restore.prepare(src, "1", null, p, false, false);
        if (!pr.ok()) { r.lines.add(pr.error + " Nothing was charged."); return r; }
        if (!pr.plan.fits()) { r.lines.add("Make room in your inventory first (one free slot). Nothing was charged."); return r; }

        double balance = money.balance(p);
        LostFoundRules.Price price = LostFoundRules.decide(st, balance, settings);
        if (!price.equals(o.price)) {
            r.newOffer = new Offer(nextOfferId++, o.file, o.folder, rec, price);
            offers.put(uuid, r.newOffer);
            r.lines.add("The price changed to " + LostFoundRules.priceText(price) + " because your balance changed. Check it and confirm again.");
            return r;
        }
        double charged = 0;
        if (!price.free) {
            if (balance + 1e-9 < price.amount) {
                r.lines.add("That costs " + LostFoundRules.money(price.amount) + " and you have " + LostFoundRules.money(balance)
                        + ". You need " + LostFoundRules.money(price.amount - balance) + " more. Nothing was charged.");
                return r;
            }
            if (!money.withdraw(p, price.amount)) {
                r.lines.add("The payment didn't go through. Nothing was charged.");
                return r;
            }
            charged = price.amount;
        }
        String rel = store.relative(o.file);
        DeathRecord.ItemEntry e = pr.burn.item;
        String desc = e.id + (e.enchants.isEmpty() ? "" : " [" + RecordFormat.packEnchantsPublic(e.enchants) + "]")
                + (e.customName.isEmpty() ? "" : " \"" + e.customName + "\"");
        String ptext = LostFoundRules.priceText(price);

        // once-only mark, before the give
        e.restoredTimes++;
        e.restoredTo = p.getName();
        e.restoredToUuid = uuid.toString();
        e.restoredBy = SELF;
        e.restoredEpoch = now;
        e.restoredAt = RestoreCsv.markTime(now);
        try {
            store.rewrite(o.file, BurnFormat.write(pr.burn));
        } catch (Exception ex) {
            refund(p, charged, rel, desc, ptext, balance, "refunded-mark-failed", now);
            r.lines.add("Something went wrong saving it" + (charged > 0 ? ", so your money was given back" : "") + ". Nothing changed.");
            return r;
        }

        LostFoundState before = st.copy();
        st.recoveries++;
        st.lastRecoveryEpoch = now;
        if (price.free) { st.freeUsed = true; st.freeUsedEpoch = now; }
        try {
            store.writeLostFoundState(uuid, st);
        } catch (Exception ex) {
            undoMark(o.file, original);
            refund(p, charged, rel, desc, ptext, balance, "refunded-state-failed", now);
            r.lines.add("Something went wrong saving it" + (charged > 0 ? ", so your money was given back" : "") + ". Nothing changed.");
            return r;
        }

        ItemStack stack = pr.stacks.get(0);
        int given;
        Map<Integer, ItemStack> left;
        try {
            left = p.getInventory().addItem(stack.clone());
            int leftCount = 0;
            for (ItemStack s : left.values()) leftCount += s.getAmount();
            given = stack.getAmount() - leftCount;
        } catch (Throwable t) {
            left = null;
            given = -1;
        }
        if (given <= 0) {
            undoMark(o.file, original);
            try { store.writeLostFoundState(uuid, before); } catch (Exception ignored) { }
            refund(p, charged, rel, desc, ptext, balance, "refunded-give-failed", now);
            r.lines.add("Your item couldn't be handed over" + (charged > 0 ? ", so your money was given back" : "") + ". Nothing changed.");
            return r;
        }
        if (left != null && !left.isEmpty()) {
            for (ItemStack s : left.values()) p.getWorld().dropItemNaturally(p.getLocation(), s);
            r.lines.add("Part of it didn't fit and was dropped at your feet.");
        }

        try {
            store.appendRestores(List.of(RestoreCsv.row(now, SELF, rec.ownerName.isEmpty() ? "unknown" : rec.ownerName, rel, desc,
                    p.getName(), "1", e.count, "burned", false)));
        } catch (Exception ex) {
            plugin.getLogger().warning("ExcelDeathRecord [Lost & Found]: could not append restores.csv: " + ex.getMessage());
        }
        try {
            store.appendLostFound(List.of(LostFoundCsv.row(now, p.getName(), desc, rel, ptext, charged, balance, "ok")));
        } catch (Exception ex) {
            plugin.getLogger().warning("ExcelDeathRecord [Lost & Found]: could not append lostfound.csv: " + ex.getMessage());
        }
        doneOffers.put(uuid, o.id);
        plugin.getLogger().info("ExcelDeathRecord [Lost & Found]: " + p.getName() + " brought back " + desc + " x" + e.count
                + " (" + rel + ") for " + ptext + (charged > 0 ? ", charged " + LostFoundRules.money(charged) : "")
                + ", balance before " + LostFoundRules.money(balance));
        r.ok = true;
        r.lines.add("You got your " + RestoreService.pretty(e) + " back" + (price.free
                ? ". This one was free (your first recovery)." : " for " + LostFoundRules.money(charged) + "."));
        r.lines.add("You can bring back another item in " + LostFoundRules.durationText(settings.cooldownMillis) + ".");
        return r;
    }

    private void undoMark(File f, String original) {
        try { store.rewrite(f, original); }
        catch (Exception ex) {
            plugin.getLogger().warning("ExcelDeathRecord [Lost & Found]: could not undo the mark on " + store.relative(f) + ": " + ex.getMessage());
        }
    }

    private void refund(Player p, double charged, String rel, String desc, String ptext, double balance, String result, long now) {
        boolean back = charged <= 0 || money.deposit(p, charged);
        if (!back) {
            plugin.getLogger().severe("ExcelDeathRecord [Lost & Found]: REFUND FAILED for " + p.getName() + " ("
                    + LostFoundRules.money(charged) + ", " + rel + "). Give it back by hand.");
        }
        try {
            store.appendLostFound(List.of(LostFoundCsv.row(now, p.getName(), desc, rel, ptext, charged, balance,
                    back ? result : result + "-REFUND-FAILED")));
        } catch (Exception ignored) { }
        plugin.getLogger().warning("ExcelDeathRecord [Lost & Found]: " + result + " for " + p.getName() + " (" + rel + ")"
                + (charged > 0 ? (back ? ", refunded " : ", NOT refunded ") + LostFoundRules.money(charged) : ""));
    }

    // ---- "your item despawned" line ----

    void onRecorded(DestructionRecord rec) {
        if (!enabled || !notifyOwner || rec.ownerUuid.isEmpty() || rec.item.bytesB64.isEmpty()) return;
        if (!LostFoundRules.valuable(rec.item, types)) return;
        UUID u;
        try { u = UUID.fromString(rec.ownerUuid); } catch (IllegalArgumentException e) { return; }
        Player p = Bukkit.getPlayer(u);
        if (p == null || !p.hasPermission(PERM)) return;
        long now = System.currentTimeMillis();
        Long last = lastNotice.get(u);
        if (last != null && now - last < notifyThrottleMs) return;
        lastNotice.put(u, now);
        String line = notifyText.replace("{item}", RestoreService.pretty(rec.item))
                .replace("{how}", LostFoundRules.causeWords(rec.cause))
                .replace("{days}", LostFoundRules.daysText(settings.windowMillis));
        p.sendMessage("\u00a7e" + line);
    }

    // ---- ops: /deathrecord lostfound ----

    List<String> statusLines(String name, UUID u) {
        List<String> out = new ArrayList<>();
        LostFoundState st = state(u);
        if (st == null) { out.add("Could not read the state file."); return out; }
        long cd = cooldownLeft(st);
        out.add("Lost & Found for " + name + ": recoveries " + st.recoveries + ", free used " + st.freeUsed
                + ", cooldown " + (cd > 0 ? LostFoundRules.durationText(cd) + " left" : "ready") + ", tip OK " + st.tipOk);
        Player p = Bukkit.getPlayer(u);
        if (p != null) {
            out.add("Balance " + (money.ready() ? LostFoundRules.money(money.balance(p)) : "n/a (no economy)")
                    + ", next price " + LostFoundRules.priceText(quote(p, st)) + ", entries " + eligible(p).size());
        }
        return out;
    }

    /** what: cooldown | free | tip | all */
    String reset(UUID u, String what) {
        LostFoundState st = state(u);
        if (st == null) return "Could not read the state file.";
        switch (what) {
            case "cooldown" -> st.lastRecoveryEpoch = 0;
            case "free" -> { st.freeUsed = false; st.freeUsedEpoch = 0; st.recoveries = 0; }
            case "tip" -> st.tipOk = false;
            case "all" -> st = new LostFoundState();
            default -> { return "Reset what? cooldown | free | tip | all"; }
        }
        try { store.writeLostFoundState(u, st); } catch (Exception e) { return "Could not save: " + e.getMessage(); }
        return "Reset " + what + ".";
    }
}
