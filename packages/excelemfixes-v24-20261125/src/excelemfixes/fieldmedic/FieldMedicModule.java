package excelemfixes.fieldmedic;

import excelemfixes.FixModule;
import excelemfixes.ModuleContext;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

/**
 * Field Medic module: the Field Bandage — an in-combat
 * heal-over-time consumable (D195 item 3, Nov 22, 2026).
 *
 * <p>The gap (Nov 22, 2026 combat investigation, healing lane):
 * every meaningful heal on the server is out-of-combat-gated —
 * EliteMobs' trickle and this pack's food-heal budgets all pause
 * while a player is fighting — while the hits that kill arrive IN
 * combat (elite hits median ~10–12 HP against 62–76 HP pools;
 * focus-fire bursts of ~76 HP in ~2 s). The only in-combat heal a
 * player owns is a brewed Instant Health II potion (8 HP). The
 * bandage is the missing middle: right-click it mid-fight and it
 * returns 35% of max health over 5 seconds — enough to turn a
 * mistimed fight, never enough to out-heal focus fire. Once a
 * minute, one at a time, no stacking: a decision, not a crutch.
 *
 * <p>The bandage is an item-carrying SOLUTION module (D199): the
 * item registers once through ExcelItems (permanent registry ID 6,
 * key field_bandage — see {@code BandageItem}), and this module is
 * its behavior. ExcelItems is therefore a SOFT dependency of the
 * pack and a HARD requirement of this module: without it the
 * module stays OFF and logs exactly that (below). This class
 * imports no ExcelItems type — the API lives behind
 * {@code BandageItems}/{@code FieldMedicService}, loaded only
 * after the presence check passes — so a server without ExcelItems
 * loads the pack exactly as before (the jobs-dummy-gate pattern).
 *
 * <p>Healing delivery goes through EntityRegainHealthEvent, never
 * raw setHealth: ExcelProtect records only regain events, and
 * event-less healing is invisible to the server's heal accounting
 * (the food-heal blindness, Nov 22, 2026 — the standing lesson).
 *
 * <p>The module also carries the batch's buy flows (same
 * investigation: the brewed potion was sold nowhere). It
 * registers the Strong Healing Potion through ExcelItems
 * (permanent registry ID 7, key strong_healing_potion — see
 * {@code PotionItem}) and runs two review/confirm purchase
 * commands, registered dynamically below so they do not exist
 * while the module is off: {@code /fieldmedic} (the potion, at
 * the ExcelShops list price or the guild vendor price) and
 * {@code /guildvendor} (the generic vendor flow for the pearls
 * and the empty map). Vault is needed only for the purchases;
 * without an economy the bandage and the registrations stand
 * and the commands report purchases unavailable.
 */
public final class FieldMedicModule implements FixModule {

    private FieldMedicService service;

    @Override
    public String id() {
        return "field-medic";
    }

    @Override
    public String displayName() {
        return "Field Medic";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        if (Bukkit.getPluginManager().getPlugin("ExcelItems") == null) {
            ctx.logger().warning("Field Medic module OFF: ExcelItems is not available — "
                    + "the Field Bandage (registry id 6) registers through ExcelItems, "
                    + "so this module stays off until ExcelItems is installed and enabled. "
                    + "No items were registered.");
            return;
        }
        try {
            service = new FieldMedicService(ctx);
            service.start();
        } catch (Throwable t) {
            service = null;
            ctx.logger().warning("Field Medic module OFF: ExcelItems is present but "
                    + "unusable (" + t.getMessage() + ") — the module stays off and "
                    + "registered nothing.");
            return;
        }
        ctx.registerCommand("fieldmedic", java.util.List.of(),
                "/fieldmedic buy|confirm", new FieldMedicCommand(this));
        ctx.registerCommand("guildvendor", java.util.List.of(),
                "/guildvendor buy <good>|confirm", new GuildVendorCommand(this));
    }

    /** The live service, or null while the module is standing down. */
    public FieldMedicService service() {
        return service;
    }

    /** The shared vendor buy flow, or null when purchases are unavailable. */
    public VendorBuyFlow buyFlow() {
        return service == null ? null : service.buyFlow();
    }

    /** The hooked Vault economy, or null when none is available. */
    public Economy economy() {
        return service == null ? null : service.economy();
    }

    /** Mint a Strong Healing Potion (admin give). Only valid while active. */
    public ItemStack createPotion() {
        return service.createPotion();
    }

    @Override
    public void onDisable() {
        if (service != null) {
            service.stop();
            service = null;
        }
    }

    @Override
    public void reload() {
        if (service != null) service.reload();
    }
}
