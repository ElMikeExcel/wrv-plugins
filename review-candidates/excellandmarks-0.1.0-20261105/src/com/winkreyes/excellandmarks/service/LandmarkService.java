package com.winkreyes.excellandmarks.service;

import com.winkreyes.excellandmarks.ExcelLandmarks;
import com.winkreyes.excellandmarks.core.Attribution;
import com.winkreyes.excellandmarks.core.Money;
import com.winkreyes.excellandmarks.core.RentClock;
import com.winkreyes.excellandmarks.core.Settings;
import com.winkreyes.excellandmarks.core.Spot;
import com.winkreyes.excellandmarks.core.SpotMenuModel;
import com.winkreyes.excellandmarks.store.Store;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * All Landmark business logic (SPEC §4–§5): registration, purchase /
 * renew / release, the rent clock, and the ONE pack-and-clear finalize
 * path shared by lapse, voluntary release, and admin force-release.
 */
public final class LandmarkService {

    public record Outcome(boolean ok, String message) {
        static Outcome ok(String m) { return new Outcome(true, m); }
        static Outcome fail(String m) { return new Outcome(false, m); }
    }

    private final ExcelLandmarks plugin;

    public LandmarkService(ExcelLandmarks plugin) {
        this.plugin = plugin;
    }

    private Settings cfg() { return plugin.settings(); }
    private Store store() { return plugin.store(); }

    // -------------------------------------------------------- register

    public Spot register(World world, int x, int y, int z, String name,
                         Spot.Type type, Spot.Tier tier, Integer sizeOverride) {
        Settings cfg = cfg();
        Spot s = new Spot(plugin.registry().nextId(), world.getName(), x, y, z,
                name == null || name.isBlank() ? "Unnamed Spot" : name.trim(),
                type, tier,
                cfg.rentFor(tier), cfg.buyFor(tier),
                type == Spot.Type.PLOT ? (sizeOverride != null ? sizeOverride : cfg.sizeFor(tier)) : 0,
                System.currentTimeMillis());
        plugin.registry().add(s);
        persist(s);
        return s;
    }

    public void removeSpot(Spot s) {
        if (!s.isAvailable()) finalize(s, "admin-remove");
        plugin.registry().remove(s.id);
        store().saveSpots(plugin.registry());
    }

    // -------------------------------------------------------- purchase

    private Outcome charge(Player player, long cents, String what) {
        Economy econ = plugin.economy();
        if (econ == null) return Outcome.fail("The economy is unavailable right now — nothing was charged.");
        if (!econ.has(player, cents / 100.0)) {
            return Outcome.fail("Not enough money: " + what + " costs " + Money.fmt(cents)
                    + ", you have " + Money.fmt((long) (econ.getBalance(player) * 100)) + ".");
        }
        EconomyResponse r = econ.withdrawPlayer(player, cents / 100.0);
        if (r == null || !r.transactionSuccess()) {
            return Outcome.fail("Payment failed — nothing was charged.");
        }
        return Outcome.ok(null);
    }

    public Outcome rent(Player player, Spot s) {
        if (!s.isAvailable()) return Outcome.fail("That spot was just taken.");
        Outcome c = charge(player, s.rentCents, "Rent");
        if (!c.ok()) return c;
        long now = System.currentTimeMillis();
        s.ownerId = player.getUniqueId();
        s.ownerName = player.getName();
        s.mode = Spot.Mode.RENTED;
        s.paidThroughMs = RentClock.rentStart(now, cfg().rentPeriodDays);
        ensureClaim(s);
        persist(s);
        return Outcome.ok("You rented " + s.name + " through " + SpotMenuModel.date(s.paidThroughMs) + ".");
    }

    public Outcome buy(Player player, Spot s) {
        boolean mine = player.getUniqueId().equals(s.ownerId);
        if (!s.isAvailable() && !mine) return Outcome.fail("That spot is not available.");
        if (mine && s.mode == Spot.Mode.OWNED) return Outcome.fail("You already own this spot.");
        Outcome c = charge(player, s.buyCents, "Buying");
        if (!c.ok()) return c;
        s.ownerId = player.getUniqueId();
        s.ownerName = player.getName();
        s.mode = Spot.Mode.OWNED;
        s.paidThroughMs = 0L;
        ensureClaim(s);
        persist(s);
        return Outcome.ok(s.name + " is yours for good.");
    }

    public Outcome renew(Player player, Spot s) {
        if (!player.getUniqueId().equals(s.ownerId) || s.mode != Spot.Mode.RENTED) {
            return Outcome.fail("Only the renter can renew this spot.");
        }
        Outcome c = charge(player, s.rentCents, "Renewing");
        if (!c.ok()) return c;
        s.paidThroughMs = RentClock.renew(System.currentTimeMillis(), s.paidThroughMs, cfg().rentPeriodDays);
        persist(s);
        return Outcome.ok(s.name + " renewed through " + SpotMenuModel.date(s.paidThroughMs) + ".");
    }

    /** Voluntary release by the owner (menu) — same finalize path as a lapse. */
    public Outcome release(Spot s) {
        if (s.isAvailable()) return Outcome.fail("That spot is not taken.");
        String name = s.name;
        finalize(s, "release");
        return Outcome.ok(name + " released. Anything left inside was packed into your redemption box.");
    }

    private void ensureClaim(Spot s) {
        if (!s.isPlot() || s.claimId != null) return;
        Settings cfg = cfg();
        Long id = plugin.gp().createSubdivision(s.world,
                s.x - s.size, s.y - cfg.plotYBelow, s.z - s.size,
                s.x + s.size, s.y + cfg.plotYAbove, s.z + s.size, s.ownerId);
        if (id != null) s.claimId = id;
    }

    // ----------------------------------------------------- clock + lapse

    /** Run the rent clock once; returns the ids finalized this pass. */
    public List<String> sweepClock() {
        long now = System.currentTimeMillis();
        List<String> done = new ArrayList<>();
        for (Spot s : plugin.registry().all()) {
            if (s.mode == Spot.Mode.RENTED && RentClock.dueForFinalize(s, now, cfg().graceDays)) {
                finalize(s, "lapse");
                done.add(s.id);
            } else if (!s.isAvailable()) {
                refreshSign(s); // keeps grace wording on the sign face current
            }
        }
        return done;
    }

    /**
     * THE finalize path (SPEC §5): pack containers + loose items, tally
     * the renter's materials from ExcelProtect attribution, clear exactly
     * those blocks, deliver the redemption box (NEVER expires), delete
     * the GP subdivision, free the spot.
     */
    public void finalize(Spot s, String reason) {
        UUID owner = s.ownerId;
        String ownerName = s.ownerName;
        List<ItemStack> packed = new ArrayList<>();

        if (s.isPlot() && owner != null) {
            World w = Bukkit.getWorld(s.world);
            if (w != null) {
                Settings cfg = cfg();
                // 1. containers + loose items inside the region
                for (int bx = s.x - s.size; bx <= s.x + s.size; bx++) {
                    for (int bz = s.z - s.size; bz <= s.z + s.size; bz++) {
                        for (int by = s.y - cfg.plotYBelow; by <= s.y + cfg.plotYAbove; by++) {
                            Block b = w.getBlockAt(bx, by, bz);
                            if (b.getState() instanceof Container cont) {
                                for (ItemStack it : cont.getInventory().getContents()) {
                                    if (it != null && it.getType() != Material.AIR) packed.add(it.clone());
                                }
                                cont.getInventory().clear();
                            }
                        }
                    }
                }
                Location centre = new Location(w, s.x, s.y, s.z);
                for (Entity e : w.getNearbyEntities(centre, s.size + 1.0, cfg.plotYAbove + cfg.plotYBelow + 1.0, s.size + 1.0)) {
                    if (e instanceof Item item && s.inRegion(s.world, item.getLocation().getBlockX(),
                            item.getLocation().getBlockY(), item.getLocation().getBlockZ(),
                            cfg.plotYBelow, cfg.plotYAbove)) {
                        packed.add(item.getItemStack().clone());
                        item.remove();
                    }
                }
                // 2. materials tally from attribution (fail-soft: empty when unavailable)
                Attribution.Tally tally = plugin.protect().tallyFor(s, owner, cfg.plotYBelow, cfg.plotYAbove);
                // 3. clear exactly the attributed blocks, then stack their materials
                for (Attribution.Entry e : tally.positions.values()) {
                    Block b = w.getBlockAt(e.x(), e.y(), e.z());
                    if (!b.getType().getKey().toString().equals(e.block())) continue; // changed since — not theirs to clear
                    if (b.getState() instanceof Container cont) cont.getInventory().clear();
                    b.setType(Material.AIR, false);
                }
                for (Map.Entry<String, Integer> m : tally.materials.entrySet()) {
                    Material mat = Material.matchMaterial(m.getKey());
                    if (mat == null) continue;
                    int left = m.getValue();
                    while (left > 0) {
                        int n = Math.min(left, mat.getMaxStackSize());
                        packed.add(new ItemStack(mat, n));
                        left -= n;
                    }
                }
                plugin.getLogger().info("ExcelLandmarks: finalize " + s.id + " (" + reason + ") packed "
                        + packed.size() + " stacks, attribution tally " + tally.totalBlocks() + " blocks");
            }
        }

        if (owner != null && !packed.isEmpty()) {
            deliverRedemption(s, owner, ownerName, packed);
        }
        if (s.claimId != null) {
            plugin.gp().deleteSubdivision(s.claimId);
        }
        s.clearOwnership();
        persist(s);
    }

    /**
     * Redemption delivery (SPEC §5): physical chest(s) beside the sign,
     * virtual holding when no free cell exists. Either way it NEVER
     * expires and no sweep ever touches it again.
     */
    private void deliverRedemption(Spot s, UUID owner, String ownerName, List<ItemStack> items) {
        World w = Bukkit.getWorld(s.world);
        List<ItemStack> remaining = new ArrayList<>(items);
        Store.Box box = null;
        if (w != null) {
            int radius = cfg().redemptionSearchRadius;
            outer:
            for (int r = 0; r <= radius && !remaining.isEmpty(); r++) {
                for (int dx = -r; dx <= r && !remaining.isEmpty(); dx++) {
                    for (int dz = -r; dz <= r && !remaining.isEmpty(); dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                        for (int dy : new int[]{0, 1, -1}) {
                            int bx = s.x + dx, by = s.y + dy, bz = s.z + dz;
                            if (bx == s.x && by == s.y && bz == s.z) continue;
                            if (plugin.registry().bySign(s.world, bx, by, bz) != null) continue;
                            if (store().boxAt(s.world, bx, by, bz) != null) continue;
                            Block b = w.getBlockAt(bx, by, bz);
                            if (b.getType() != Material.AIR) continue;
                            b.setType(Material.CHEST, false);
                            if (box == null) {
                                box = new Store.Box();
                                box.owner = owner;
                                box.ownerName = ownerName;
                                box.spotId = s.id;
                                box.world = s.world;
                                box.createdMs = System.currentTimeMillis();
                            }
                            box.chests.add(new int[]{bx, by, bz});
                            if (b.getState() instanceof Container cont) {
                                Map<Integer, ItemStack> left = cont.getInventory().addItem(remaining.toArray(new ItemStack[0]));
                                remaining = new ArrayList<>(left.values());
                            }
                            if (remaining.isEmpty()) break outer;
                        }
                    }
                }
            }
        }
        if (box != null) store().boxes.add(box);
        if (!remaining.isEmpty()) {
            Store.Virtual v = store().virtual.computeIfAbsent(owner, k -> new Store.Virtual());
            v.ownerName = ownerName;
            for (ItemStack it : remaining) v.itemBytesB64.add(Store.encodeItem(it));
        }
        store().saveRedemptions();
    }

    // ------------------------------------------------------------- signs

    public void refreshSign(Spot s) {
        World w = Bukkit.getWorld(s.world);
        if (w == null) return;
        Block b = w.getBlockAt(s.x, s.y, s.z);
        if (!(b.getState() instanceof Sign sign)) return;
        String[] lines = SpotMenuModel.signLines(s, System.currentTimeMillis(), cfg().graceDays);
        var front = sign.getSide(Side.FRONT);
        front.setLine(0, "§6" + lines[0]);
        front.setLine(1, lines[1]);
        front.setLine(2, lines[2] == null ? "" : lines[2]);
        front.setLine(3, lines[3]);
        sign.update(true, false);
    }

    private void persist(Spot s) {
        store().saveSpots(plugin.registry());
        refreshSign(s);
    }
}
