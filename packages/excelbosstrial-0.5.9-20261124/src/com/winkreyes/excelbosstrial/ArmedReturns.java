/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial;

import com.winkreyes.excelbosstrial.core.ArmedReturnStore;
import com.winkreyes.excelbosstrial.core.AtomicStateFile;
import com.winkreyes.excelbosstrial.emloot.EmLootGateway;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * 0.5.9: join-armed returns - the adapter half (the pure
 * store and command words live in core/ArmedReturnStore and
 * core/ArmReturnPlan).
 *
 * WHY THIS EXISTS (Mike, Nov 24, 2026): an item return used
 * to need an outside watcher polling the player list every
 * few minutes to spot a login, because the credit must run
 * while the player is online - EliteMobs applies its
 * Soulbind only when the recipient stands in front of the
 * construction, so an offline credit is a genuine but
 * unbound item. But Paper already announces every join to
 * every plugin: the server's own signal is the trigger, so
 * the plugin finishes the job in the moment of the join
 * itself - no polling, no watchers, no tokens spent
 * watching a screen. An admin ARMS the return ahead of time
 * (data only; the player may be offline for days), and this
 * listener hears the join, builds the item with the player
 * present through the same EmLootGateway construction
 * grantclaim uses, files it into the player's claims
 * through TeamClaims.grantClaim, disarms, and tells the
 * player in chat and on screen, naming the item.
 *
 * Ordering against the claims presentation: this handler
 * runs at NORMAL priority and grants synchronously inside
 * the join event; TeamClaims' own join presentation is a
 * MONITOR handler that only schedules its attempt seconds
 * later (and grantClaim schedules its own), so the claim is
 * already in the ledger when the chest presents.
 *
 * Double-grant safety: the grant and the disarm cannot be
 * one atomic write (two stores), so the join path defends
 * the seam - BEFORE building, it asks the claims ledger
 * whether an unclaimed console-grant claim already holds
 * this exact item for this player (the state a crash
 * between grant and disarm would leave). A hit removes the
 * arm and grants nothing. And the arm is removed only AFTER
 * a successful grant, so every failure - EliteMobs gone, a
 * build failure, the claims store refusing - keeps the arm
 * for the next join and logs a greppable ARMED-RETURN
 * FAILED line instead of telling the player a success that
 * did not happen.
 */
final class ArmedReturns implements Listener {

    private final JavaPlugin plugin;
    private final TeamClaims claims;
    private final EmLootGateway gateway;
    private final Path storeFile;
    private ArmedReturnStore store;

    ArmedReturns(JavaPlugin plugin, TeamClaims claims, EmLootGateway gateway) {
        this.plugin = plugin;
        this.claims = claims;
        this.gateway = gateway;
        this.storeFile = plugin.getDataFolder().toPath().resolve("armed-returns.dat");
        this.store = load();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        if (store.size() > 0) {
            plugin.getLogger().info("Armed returns: " + store.size()
                    + " return(s) waiting for their players' next join.");
        }
    }

    /** A store file that will not verify is never half-read:
     *  log loudly, keep the bad bytes aside for a human, and
     *  start empty - an admin can re-arm; a wedged plugin
     *  would strand every return AND everything else. */
    private ArmedReturnStore load() {
        try {
            if (Files.exists(storeFile)) {
                return ArmedReturnStore.decode(Files.readString(storeFile));
            }
        } catch (Exception unreadable) {
            plugin.getLogger().severe("ARMED-RETURN STORE UNREADABLE (" + unreadable.getMessage()
                    + ") - starting with no armed returns; the unreadable file is kept aside. "
                    + "Re-arm any return it held.");
            try {
                Files.move(storeFile, storeFile.resolveSibling(
                        "armed-returns.dat.corrupt-" + System.currentTimeMillis()));
            } catch (Exception ignored) {
                // The severe line above is the record; a failed
                // rename must not wedge the boot.
            }
        }
        return ArmedReturnStore.empty();
    }

    private void persist() throws IOException {
        AtomicStateFile.write(storeFile, store.encode());
    }

    /** Arm one return (the adapter validated + probed first).
     *  False = the pair is already armed (a refusal). A
     *  persist failure rolls the in-memory arm back out, so a
     *  caller told "stored" is never holding a phantom. */
    synchronized boolean arm(ArmedReturnStore.Arm arm) throws IOException {
        if (!store.add(arm)) {
            return false;
        }
        try {
            persist();
        } catch (IOException failure) {
            store.remove(arm.player(), arm.lootFile());
            throw failure;
        }
        return true;
    }

    /** Disarm one pair, or every arm a player holds when the
     *  loot file is null. Returns how many arms went. */
    synchronized int disarm(UUID player, String lootFile) throws IOException {
        int removed = lootFile == null
                ? store.removeAll(player)
                : (store.remove(player, lootFile) ? 1 : 0);
        if (removed > 0) {
            persist();
        }
        return removed;
    }

    synchronized List<ArmedReturnStore.Arm> list() {
        return store.all();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("commands.armreturn-enabled", true)) {
            return;
        }
        Player player = event.getPlayer();
        List<ArmedReturnStore.Arm> waiting;
        synchronized (this) {
            waiting = store.forPlayer(player.getUniqueId());
        }
        for (ArmedReturnStore.Arm arm : waiting) {
            handleArm(player, arm);
        }
    }

    private void handleArm(Player player, ArmedReturnStore.Arm arm) {
        UUID owner = player.getUniqueId();
        // The crash guard first: a grant that landed while its
        // disarm did not is recovered, never repeated.
        ItemStack existing = claims.findUnclaimedConsoleEmItem(owner, arm.lootFile());
        if (existing != null) {
            disarmQuietly(arm);
            plugin.getLogger().info("ARMED-RETURN RECOVERED: " + player.getName()
                    + " (" + owner + ") already holds an unclaimed console-grant claim of "
                    + arm.lootFile() + " (an earlier grant landed; its disarm did not) - "
                    + "arm removed, no second grant.");
            announce(player, existing);
            return;
        }
        // Build WITH the player - the soulbound construction,
        // the entire reason the credit waits for a join.
        EmLootGateway.Build built = gateway.build(arm.lootFile(), player);
        if (built.status() != EmLootGateway.Status.OK) {
            plugin.getLogger().severe("ARMED-RETURN FAILED for " + player.getName()
                    + " (" + owner + "), loot " + arm.lootFile() + ": EliteMobs build status "
                    + built.status() + " - the arm is KEPT and will retry at the next join.");
            return;
        }
        String run;
        try {
            run = claims.grantClaim(owner, List.of(built.stack()));
        } catch (Exception failure) {
            plugin.getLogger().severe("ARMED-RETURN FAILED for " + player.getName()
                    + " (" + owner + "), loot " + arm.lootFile()
                    + ": the claims store refused the grant (" + failure.getMessage()
                    + ") - the arm is KEPT and will retry at the next join.");
            return;
        }
        // Only a landed grant earns the disarm.
        disarmQuietly(arm);
        plugin.getLogger().info("ARMED-RETURN GRANTED: " + player.getName() + " (" + owner
                + ") received EM loot " + arm.lootFile() + " as claim " + run + "/" + owner
                + " (armed by " + arm.armedBy() + "); it waits in their /bossrewards.");
        announce(player, built.stack());
    }

    /** Remove a spent arm after its grant (or its recovery).
     *  A persist failure here is survivable by design - the
     *  ledger check at the next join refuses a second grant -
     *  so it warns instead of failing the join. */
    private void disarmQuietly(ArmedReturnStore.Arm arm) {
        synchronized (this) {
            try {
                if (store.remove(arm.player(), arm.lootFile())) {
                    persist();
                }
            } catch (IOException failure) {
                plugin.getLogger().warning("ARMED-RETURN: could not persist the disarm for "
                        + arm.player() + " / " + arm.lootFile() + ": " + failure.getMessage()
                        + " (the claims-ledger check at the next join prevents a second grant).");
            }
        }
    }

    /** Tell the player, in chat and on screen, naming the
     *  item by its own display name. Templates are config
     *  (messages.return-*), {item} is the placeholder. */
    private void announce(Player player, ItemStack stack) {
        String item = itemName(stack);
        LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();
        String title = fill(plugin.getConfig().getString("messages.return-title",
                "&6Your {item} is back"), item);
        String subtitle = fill(plugin.getConfig().getString("messages.return-subtitle",
                "&eIt is waiting in your boss rewards"), item);
        String chat = fill(plugin.getConfig().getString("messages.return-chat",
                "&eA return was waiting for you: &f{item}&e is in your "
                        + "&f/bossrewards&e - open it to collect."), item);
        player.showTitle(Title.title(legacy.deserialize(title), legacy.deserialize(subtitle),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3),
                        Duration.ofSeconds(1))));
        player.sendMessage(legacy.deserialize(chat));
    }

    private static String fill(String template, String item) {
        return template.replace("{item}", item);
    }

    private static String itemName(ItemStack stack) {
        var meta = stack.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        }
        return stack.getType().name();
    }
}
