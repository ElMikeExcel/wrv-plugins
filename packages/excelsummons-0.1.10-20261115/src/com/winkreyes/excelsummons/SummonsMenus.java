package com.winkreyes.excelsummons;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * The /summons menu views (0.1.7): the compass Summons page.
 *
 * MAIN page — CAST summons only (one icon per cast record, facts
 * identical to /summons list via MenuModel), the applied held
 * percent, the three EXISTING dismiss scopes as buttons, and the
 * "Adopted pets (N)" door. ADOPTED sub-page — every adopted pet
 * with its ADOPTED / holds-no-mana indicators and a Back button;
 * its only action is a PER-PET dismiss behind a confirm step
 * (Mike's Nov 12, 2026, ~6:05 PM ruling): the confirm names the
 * pet and warns it permanently removes the real tamed animal,
 * and only the confirm action dismisses — exactly that one
 * record, through SummonsListener.dismissOneAdopted. There is
 * no "dismiss all adopted" control anywhere.
 *
 * Java players get inventory GUIs; Bedrock players get Cumulus
 * simple forms carrying the same content (form bodies include
 * EVERY line — no pagination in text). A form failure falls back
 * to the chat list, never to silence. Dismiss buttons route
 * through SummonsListener.dismiss — the exact path the commands
 * use — and report MenuModel.resultMessage, the command's own
 * text. All content decisions live in MenuModel (pure, harness-
 * tested); this class is Bukkit mechanics only, shaped on the
 * TrainingDummy DummyMenus pattern.
 *
 * 0.1.9: entries carry the pet's own name (MenuModel), and the
 * adopted row tap / pet button opens a PER-PET PAGE (Java view /
 * Bedrock pet form) with the pet's facts and two actions:
 * Rename and Dismiss (dismiss still routes to the confirm step
 * above, unchanged). Rename follows the house buttons rule —
 * Bedrock gets a Cumulus CustomForm text input
 * (BedrockForms.sendCustomForm); Java gets a chat prompt: the
 * view closes, the player's next chat message within 30 seconds
 * is taken as the name (and cancelled as chat, so the name
 * never broadcasts), with "cancel" as the way out. Both land in
 * finishRename -> SummonsListener.renameAdopted, which sets the
 * name through the nameplate composition so it shows over the
 * animal's head.
 *
 * 0.1.10 (BB-11 back-button fix, Nov 14, 2026): the MAIN page
 * gains a Back to Server Menu control on both platforms — the
 * audit's two FIX-2 rows. Java: an ARROW item at
 * SLOT_BACK_TO_MENU whose click closes the view and runs the
 * fleet opener (performCommand "menu"). Bedrock: the main form
 * gains the same label as its LAST button
 * (MenuModel.mainFormButtons / MAIN_BACK_INDEX), routed to the
 * same opener. The adopted sub-page and the dismiss confirm
 * already carried their Back/return controls and are untouched.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class SummonsMenus {
    private SummonsMenus() {}

    // Control-row slots (54-slot view; entries occupy 0..44).
    static final int SLOT_PREV = 45;
    static final int SLOT_ADOPTED = 46;
    static final int SLOT_DISMISS_FIRST = 47; // 47 mount, 49 pets, 51 all
    static final int SLOT_INFO = 48;
    static final int SLOT_NEXT = 53;
    static final int SLOT_BACK = 49;
    // 0.1.10: the MAIN page's Back to Server Menu slot. Slot 49
    // above is the ADOPTED page's Back (and the main page's
    // Dismiss-pets slot), so the main page's Back takes slot 52
    // — free in every main-page state (Prev/Next occupy 45/53
    // only when paginated, dismiss items 47/49/51, info 48).
    static final int SLOT_BACK_TO_MENU = 52;

    // ============================== holders ==============================

    public static final class MainMenu implements InventoryHolder {
        public final int page;
        private Inventory inventory;
        public MainMenu(int page) { this.page = page; }
        @Override public Inventory getInventory() { return inventory; }
        void set(Inventory inv) { this.inventory = inv; }
    }

    public static final class AdoptedMenu implements InventoryHolder {
        public final int page;
        private Inventory inventory;
        public AdoptedMenu(int page) { this.page = page; }
        @Override public Inventory getInventory() { return inventory; }
        void set(Inventory inv) { this.inventory = inv; }
    }

    /** The adopted-dismiss confirm step (Java view): carries the
     *  ONE pet it may dismiss and the sub-page page to return to. */
    public static final class ConfirmDismissMenu implements InventoryHolder {
        public final java.util.UUID entityId;
        public final String displayName;
        public final int fromPage;
        private Inventory inventory;
        public ConfirmDismissMenu(java.util.UUID entityId, String displayName, int fromPage) {
            this.entityId = entityId;
            this.displayName = displayName;
            this.fromPage = fromPage;
        }
        @Override public Inventory getInventory() { return inventory; }
        void set(Inventory inv) { this.inventory = inv; }
    }

    /** The per-pet page (0.1.9, Java view): carries the ONE pet
     *  it acts on and the sub-page page to return to. */
    public static final class PetMenu implements InventoryHolder {
        public final java.util.UUID entityId;
        public final String displayName;
        public final int fromPage;
        private Inventory inventory;
        public PetMenu(java.util.UUID entityId, String displayName, int fromPage) {
            this.entityId = entityId;
            this.displayName = displayName;
            this.fromPage = fromPage;
        }
        @Override public Inventory getInventory() { return inventory; }
        void set(Inventory inv) { this.inventory = inv; }
    }

    // Confirm-view slots (27-slot view).
    static final int SLOT_CONFIRM = 11;
    static final int SLOT_KEEP = 15;

    // Per-pet page slots (27-slot view).
    static final int SLOT_PET_RENAME = 11;
    static final int SLOT_PET_DISMISS = 13;
    static final int SLOT_PET_BACK = 15;

    // ============================== entry points ==============================

    public static void openMain(ExcelSummons plugin, Player player) {
        openMain(plugin, player, 0);
    }

    public static void openMain(ExcelSummons plugin, Player player, int page) {
        if (PlatformDetect.isBedrock(player, plugin.getLogger())) {
            if (sendMainForm(plugin, player)) return;
            sendChatFallback(plugin, player);
            return;
        }
        MenuModel.Page model = plugin.menuPageFor(player.getUniqueId());
        int pages = model.castPageCount();
        int p = clamp(page, pages);
        MainMenu holder = new MainMenu(p);
        Inventory inv = Bukkit.createInventory(holder, 54,
                Component.text(pages > 1 ? "Your Summons — page " + (p + 1) + "/" + pages
                        : "Your Summons", NamedTextColor.DARK_PURPLE));
        holder.set(inv);
        fillMain(model, inv, p);
        player.openInventory(inv);
    }

    public static void openAdopted(ExcelSummons plugin, Player player, int page) {
        if (PlatformDetect.isBedrock(player, plugin.getLogger())) {
            if (sendAdoptedForm(plugin, player)) return;
            sendChatFallback(plugin, player);
            return;
        }
        MenuModel.Page model = plugin.menuPageFor(player.getUniqueId());
        int pages = model.adoptedPageCount();
        int p = clamp(page, pages);
        AdoptedMenu holder = new AdoptedMenu(p);
        Inventory inv = Bukkit.createInventory(holder, 54,
                Component.text(pages > 1 ? "Adopted pets — page " + (p + 1) + "/" + pages
                        : "Adopted pets", NamedTextColor.DARK_PURPLE));
        holder.set(inv);
        fillAdopted(model, inv, p);
        player.openInventory(inv);
    }

    private static int clamp(int page, int pages) {
        return Math.max(0, Math.min(page, pages - 1));
    }

    /** The adopted-dismiss confirm step for ONE pet. Java: a
     *  confirm view. Bedrock: a confirm form. Nothing is dismissed
     *  by opening this — only its confirm action dismisses. */
    public static void openConfirm(ExcelSummons plugin, Player player,
                                   MenuModel.Entry entry, int fromPage) {
        if (PlatformDetect.isBedrock(player, plugin.getLogger())) {
            if (sendConfirmForm(plugin, player, entry, fromPage)) return;
            // form failed: NO dismissal happened; back to the list
            player.sendMessage("The confirm form didn't open — your "
                    + entry.displayName() + " was NOT dismissed.");
            return;
        }
        ConfirmDismissMenu holder = new ConfirmDismissMenu(
                entry.record().entityId(), entry.displayName(), fromPage);
        Inventory inv = Bukkit.createInventory(holder, 27,
                Component.text("Dismiss " + entry.displayName() + "?",
                        NamedTextColor.DARK_RED));
        holder.set(inv);
        List<String> lore = new ArrayList<>();
        for (String line : MenuModel.confirmBody(entry).split("\n")) lore.add(line);
        inv.setItem(4, item(Material.BOOK, "Dismiss your " + entry.displayName() + "?", lore));
        inv.setItem(SLOT_CONFIRM, item(Material.REDSTONE_BLOCK,
                MenuModel.confirmButtonLabel(entry),
                List.of("This removes the animal permanently.")));
        inv.setItem(SLOT_KEEP, item(Material.LIME_DYE, MenuModel.KEEP_BUTTON_LABEL,
                List.of("Nothing is dismissed.")));
        player.openInventory(inv);
    }

    /** The per-pet page for ONE adopted pet (0.1.9). Java: a
     *  27-slot view carrying the pet's facts, Rename, and
     *  Dismiss. Bedrock: the pet form. Opening the page changes
     *  nothing — Rename starts its own flow, and Dismiss routes
     *  to the confirm step. */
    public static void openPet(ExcelSummons plugin, Player player,
                               MenuModel.Entry entry, int fromPage) {
        if (PlatformDetect.isBedrock(player, plugin.getLogger())) {
            if (sendPetForm(plugin, player, entry)) return;
            sendChatFallback(plugin, player);
            return;
        }
        PetMenu holder = new PetMenu(entry.record().entityId(), entry.displayName(), fromPage);
        Inventory inv = Bukkit.createInventory(holder, 27,
                Component.text(entry.displayName(), NamedTextColor.DARK_PURPLE));
        holder.set(inv);
        inv.setItem(4, item(iconFor(entry.record().spellKey()), entry.displayName(),
                List.of(entry.listLine(),
                        MenuModel.ADOPTED_BADGE + " — your own tamed animal",
                        MenuModel.ADOPTED_NO_MANA)));
        inv.setItem(SLOT_PET_RENAME, item(Material.NAME_TAG,
                MenuModel.renameButtonLabel(entry),
                List.of("You will type the new name in chat.")));
        inv.setItem(SLOT_PET_DISMISS, item(Material.REDSTONE_BLOCK,
                MenuModel.dismissButtonLabel(entry),
                List.of("You will be asked to confirm —",
                        "removing is permanent.")));
        inv.setItem(SLOT_PET_BACK, item(Material.ARROW, MenuModel.PET_BACK_LABEL, List.of()));
        player.openInventory(inv);
    }

    // ============================== rename (0.1.9) ==============================

    /** Java rename, step 1: close the view and arm the chat
     *  prompt (SummonsMenuListener owns the prompt state and the
     *  chat event). The player's next chat message is the name —
     *  it is consumed as the prompt's answer, never broadcast. */
    public static void beginRenamePrompt(ExcelSummons plugin, Player player,
                                         MenuModel.Entry entry, int fromPage) {
        player.closeInventory();
        plugin.menuListener().beginRenamePrompt(player.getUniqueId(),
                entry.record().entityId(), entry.displayName(), fromPage);
        player.sendMessage("Type a new name for your " + entry.displayName()
                + " in chat — it won't be posted as a chat message.");
        player.sendMessage("You have 30 seconds. Type 'cancel' to keep the current name.");
    }

    /** Rename, final step — shared by the Java chat prompt and
     *  the Bedrock text-input form. Runs the gated listener path,
     *  answers in the outcome's own words, and returns the player
     *  to the adopted page/form. */
    public static void finishRename(ExcelSummons plugin, Player player,
                                    java.util.UUID entityId, String rawText, int fromPage) {
        RenamePolicy.Result result = plugin.listener().renameAdopted(
                player.getUniqueId(), entityId, rawText, player.getName());
        switch (result) {
            case RENAMED -> player.sendMessage("Your pet is now named "
                    + RenamePolicy.sanitize(rawText) + ".");
            case INVALID_NAME -> player.sendMessage("That name can't be used — 1 to "
                    + RenamePolicy.MAX_NAME_LENGTH + " characters of plain text.");
            case NOT_LOADED -> player.sendMessage("That pet isn't loaded right now — "
                    + "stand near it and try renaming again.");
            case REFUSED -> player.sendMessage("That pet can't be renamed right now.");
        }
        if (PlatformDetect.isBedrock(player, plugin.getLogger())) {
            if (!sendAdoptedForm(plugin, player)) sendChatFallback(plugin, player);
        } else {
            openAdopted(plugin, player, fromPage);
        }
    }

    // ============================== Java menus ==============================

    private static void fillMain(MenuModel.Page model, Inventory inv, int page) {
        List<MenuModel.Entry> entries = model.castPage(page);
        for (int i = 0; i < entries.size(); i++) {
            inv.setItem(i, entryItem(entries.get(i)));
        }
        if (model.totalRegistered() == 0) {
            inv.setItem(22, item(Material.BOOK, "No summons registered",
                    List.of("Cast a summon spell and it will show up here.")));
        } else if (entries.isEmpty()) {
            inv.setItem(22, item(Material.BOOK, "No cast summons out",
                    List.of("Your adopted pets live on their own page —",
                            "use the Adopted pets button below.")));
        }
        if (model.castPageCount() > 1) {
            inv.setItem(SLOT_PREV, item(Material.ARROW, "Previous page", List.of()));
            inv.setItem(SLOT_NEXT, item(Material.ARROW, "Next page", List.of()));
        }
        inv.setItem(SLOT_ADOPTED, item(Material.LEAD, model.adoptedButtonLabel(),
                List.of("Your own tamed animals — they hold no mana.",
                        "Removing one asks for confirmation first.")));
        inv.setItem(SLOT_INFO, item(Material.BOOK,
                "Mana held: " + model.heldPercent() + "% of your pool",
                List.of(model.outNowLine(),
                        model.adoptedEntries().size() + " adopted pet(s) registered.",
                        "Adopted pets hold no mana — only cast summons do.")));
        for (int i = 0; i < MenuModel.DISMISS_BUTTONS.size(); i++) {
            MenuModel.DismissButton b = MenuModel.DISMISS_BUTTONS.get(i);
            inv.setItem(SLOT_DISMISS_FIRST + i * 2, item(dismissIcon(b.scope()), b.label(),
                    List.of(dismissLore(b.scope()))));
        }
        inv.setItem(SLOT_BACK_TO_MENU, item(Material.ARROW, MenuModel.MAIN_BACK_LABEL,
                List.of("Back to the Server Menu.")));
    }

    private static void fillAdopted(MenuModel.Page model, Inventory inv, int page) {
        List<MenuModel.Entry> entries = model.adoptedPage(page);
        for (int i = 0; i < entries.size(); i++) {
            MenuModel.Entry e = entries.get(i);
            inv.setItem(i, item(iconFor(e.record().spellKey()), e.displayName(),
                    List.of(e.listLine(),
                            MenuModel.ADOPTED_BADGE + " — your own tamed animal",
                            MenuModel.ADOPTED_NO_MANA,
                            "Tap to open its page — rename or dismiss")));
        }
        if (entries.isEmpty()) {
            inv.setItem(22, item(Material.BOOK, "No adopted pets registered",
                    List.of("Tame an animal and it will be kept here.")));
        }
        if (model.adoptedPageCount() > 1) {
            inv.setItem(SLOT_PREV, item(Material.ARROW, "Previous page", List.of()));
            inv.setItem(SLOT_NEXT, item(Material.ARROW, "Next page", List.of()));
        }
        inv.setItem(SLOT_BACK, item(Material.ARROW, "Back to your summons", List.of()));
    }

    private static ItemStack entryItem(MenuModel.Entry e) {
        return item(iconFor(e.record().spellKey()), e.displayName(), List.of(e.listLine()));
    }

    private static Material iconFor(String spellKey) {
        switch (SummonRecord.baseKey(spellKey == null ? "" : spellKey)) {
            case "archer":   return Material.BOW;
            case "bruiser":  return Material.IRON_AXE;
            case "guardian": return Material.SHIELD;
            case "revenant": return Material.WITHER_SKELETON_SKULL;
            case "ghast":    return Material.GHAST_TEAR;
            case "wolf":     return Material.BONE;
            case "cat":      return Material.STRING;
            case "familiar": return Material.RABBIT_FOOT;
            case "mount":    return Material.SADDLE;
            default:         return Material.NAME_TAG;
        }
    }

    private static Material dismissIcon(AdminPolicy.DismissScope scope) {
        switch (scope) {
            case PETS: return Material.BONE;
            case ALL:  return Material.REDSTONE_BLOCK;
            case MOUNT_ONLY:
            default:   return Material.SADDLE;
        }
    }

    private static String dismissLore(AdminPolicy.DismissScope scope) {
        switch (scope) {
            case PETS: return "Releases your cast pets only — adopted pets stay.";
            case ALL:  return "Your mount and every combat summon.";
            case MOUNT_ONLY:
            default:   return "Takes your mount only.";
        }
    }

    // ============================== click handling ==============================

    /** Returns true when the click was handled (event already cancelled by caller). */
    public static boolean handleClick(ExcelSummons plugin, Player player,
                                      InventoryHolder holder, int slot) {
        if (holder instanceof MainMenu m) {
            if (slot == SLOT_BACK_TO_MENU) {
                player.closeInventory();
                openServerMenu(player);
                return true;
            }
            if (slot == SLOT_ADOPTED) { openAdopted(plugin, player, 0); return true; }
            if (slot == SLOT_PREV && m.page > 0) { openMain(plugin, player, m.page - 1); return true; }
            if (slot == SLOT_NEXT) { openMain(plugin, player, m.page + 1); return true; }
            for (int i = 0; i < MenuModel.DISMISS_BUTTONS.size(); i++) {
                if (slot == SLOT_DISMISS_FIRST + i * 2) {
                    dismissAndReport(plugin, player,
                            MenuModel.DISMISS_BUTTONS.get(i).scope());
                    openMain(plugin, player, 0);
                    return true;
                }
            }
            return true;
        }
        if (holder instanceof AdoptedMenu a) {
            if (slot == SLOT_BACK) { openMain(plugin, player, 0); return true; }
            if (slot == SLOT_PREV && a.page > 0) { openAdopted(plugin, player, a.page - 1); return true; }
            if (slot == SLOT_NEXT) { openAdopted(plugin, player, a.page + 1); return true; }
            if (slot >= 0 && slot < MenuModel.PAGE_SIZE) {
                // A pet row: opens that pet's PAGE (0.1.9 —
                // rename / dismiss). The click itself does
                // nothing to the pet.
                MenuModel.Page model = plugin.menuPageFor(player.getUniqueId());
                List<MenuModel.Entry> rows = model.adoptedPage(a.page);
                if (slot < rows.size()) {
                    openPet(plugin, player, rows.get(slot), a.page);
                }
            }
            return true;
        }
        if (holder instanceof PetMenu pm) {
            if (slot == SLOT_PET_BACK) { openAdopted(plugin, player, pm.fromPage); return true; }
            // Re-read the entry from the live model: the page was
            // built from a snapshot, and both actions must act on
            // the pet as it stands now.
            MenuModel.Entry entry = null;
            for (MenuModel.Entry e : plugin.menuPageFor(player.getUniqueId()).adoptedEntries()) {
                if (e.record().entityId().equals(pm.entityId)) { entry = e; break; }
            }
            if (entry == null) {
                // The pet left the registry while the page was
                // open (dismissed elsewhere, died): back to the list.
                openAdopted(plugin, player, pm.fromPage);
                return true;
            }
            if (slot == SLOT_PET_RENAME) {
                beginRenamePrompt(plugin, player, entry, pm.fromPage);
                return true;
            }
            if (slot == SLOT_PET_DISMISS) {
                openConfirm(plugin, player, entry, pm.fromPage);
                return true;
            }
            return true;
        }
        if (holder instanceof ConfirmDismissMenu c) {
            if (slot == SLOT_CONFIRM) {
                boolean done = plugin.listener().dismissOneAdopted(
                        player.getUniqueId(), c.entityId, player.getName());
                player.sendMessage(done
                        ? "Your " + c.displayName + " has been dismissed."
                        : "That pet couldn't be dismissed right now — nothing was removed.");
                openAdopted(plugin, player, c.fromPage);
                return true;
            }
            if (slot == SLOT_KEEP) {
                // The way out: nothing is dismissed, ever, here.
                openAdopted(plugin, player, c.fromPage);
                return true;
            }
            return true;
        }
        return false;
    }

    /** Back to the ExcelMenu Server Menu (0.1.10, BB-11): the
     *  fleet opener — the same performCommand("menu") the
     *  ExcelMenu spawn-shop Back and the ExcelShops mobile root
     *  use. The main summons page is a top-level screen reached
     *  from the Server Menu, so its Back lands there. If the
     *  opener fails, say so instead of stranding the player. */
    static void openServerMenu(Player player) {
        if (!player.performCommand("menu")) {
            player.sendMessage("The Server Menu could not open.");
        }
    }

    /** The dismiss itself: the EXISTING listener path (the same
     *  call /summons dismiss makes), reported with the command's
     *  own result text. */
    static void dismissAndReport(ExcelSummons plugin, Player player,
                                 AdminPolicy.DismissScope scope) {
        int n = plugin.listener().dismiss(player.getUniqueId(), scope);
        player.sendMessage(MenuModel.resultMessage(n));
    }

    // ============================== Bedrock forms ==============================

    private static boolean sendMainForm(ExcelSummons plugin, Player player) {
        MenuModel.Page model = plugin.menuPageFor(player.getUniqueId());
        // 0.1.10: buttons come from MenuModel.mainFormButtons —
        // the three dismiss labels, the adopted door, then Back
        // to Server Menu LAST (MAIN_BACK_INDEX); every pre-0.1.10
        // index is unchanged.
        List<String> buttons = MenuModel.mainFormButtons(model);
        return BedrockForms.sendSimpleForm(player, "Your Summons", model.mainBody(), buttons,
                idx -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;
                    if (idx == MenuModel.MAIN_BACK_INDEX) {
                        openServerMenu(player);
                        return;
                    }
                    if (idx == MenuModel.ADOPTED_BUTTON_INDEX) {
                        if (!sendAdoptedForm(plugin, player)) sendChatFallback(plugin, player);
                        return;
                    }
                    AdminPolicy.DismissScope scope = MenuModel.scopeForButton(idx);
                    if (scope == null) return;
                    dismissAndReport(plugin, player, scope);
                    if (!sendMainForm(plugin, player)) sendChatFallback(plugin, player);
                }), plugin.getLogger());
    }

    private static boolean sendAdoptedForm(ExcelSummons plugin, Player player) {
        MenuModel.Page model = plugin.menuPageFor(player.getUniqueId());
        List<MenuModel.Entry> pets = model.adoptedEntries();
        // 0.1.9: one button per pet, labelled with the pet's own
        // name (MenuModel.adoptedFormButtons); the last is Back.
        return BedrockForms.sendSimpleForm(player, "Adopted pets", model.adoptedBody(),
                MenuModel.adoptedFormButtons(model),
                idx -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;
                    if (idx >= 0 && idx < pets.size()) {
                        // A pet button opens that pet's PAGE
                        // (rename / dismiss); it does nothing
                        // to the pet itself.
                        if (!sendPetForm(plugin, player, pets.get(idx))) {
                            sendChatFallback(plugin, player);
                        }
                        return;
                    }
                    if (idx == pets.size() && !sendMainForm(plugin, player)) {
                        sendChatFallback(plugin, player);
                    }
                }), plugin.getLogger());
    }

    /** The Bedrock per-pet form (0.1.9): the pet's line, then
     *  Rename / Dismiss / Back (MenuModel.petFormButtons).
     *  Dismiss routes to the confirm form — this form dismisses
     *  nothing itself. */
    private static boolean sendPetForm(ExcelSummons plugin, Player player,
                                       MenuModel.Entry entry) {
        return BedrockForms.sendSimpleForm(player, entry.displayName(),
                entry.bedrockLine() + "\n\nRename it, or dismiss it — "
                        + "dismissing asks you to confirm first.",
                MenuModel.petFormButtons(entry),
                idx -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;
                    if (idx == MenuModel.PET_RENAME_INDEX) {
                        if (!sendRenameForm(plugin, player, entry)) {
                            player.sendMessage("The rename form didn't open — "
                                    + "the name was not changed.");
                            if (!sendPetForm(plugin, player, entry)) {
                                sendChatFallback(plugin, player);
                            }
                        }
                        return;
                    }
                    if (idx == MenuModel.PET_DISMISS_INDEX) {
                        if (!sendConfirmForm(plugin, player, entry, 0)) {
                            player.sendMessage("The confirm form didn't open — your "
                                    + entry.displayName() + " was NOT dismissed.");
                        }
                        return;
                    }
                    if (idx == MenuModel.PET_BACK_INDEX) {
                        if (!sendAdoptedForm(plugin, player)) sendChatFallback(plugin, player);
                    }
                }), plugin.getLogger());
    }

    /** The Bedrock rename form (0.1.9): a CustomForm with one
     *  text input, prefilled with the pet's current name.
     *  Submitting lands in finishRename; closing the form
     *  submits null — nothing is renamed, and the player lands
     *  back on the pet form. */
    private static boolean sendRenameForm(ExcelSummons plugin, Player player,
                                          MenuModel.Entry entry) {
        return BedrockForms.sendCustomForm(player, MenuModel.renameButtonLabel(entry),
                "New name", "Type the new name", entry.displayName(),
                text -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;
                    if (text == null) {
                        if (!sendPetForm(plugin, player, entry)) {
                            sendChatFallback(plugin, player);
                        }
                        return;
                    }
                    finishRename(plugin, player, entry.record().entityId(), text, 0);
                }), plugin.getLogger());
    }

    /** The Bedrock confirm form for ONE adopted pet: button 0 is
     *  the only control in the plugin that can dismiss an adopted
     *  pet; button 1 (keep) and closing the form dismiss nothing. */
    private static boolean sendConfirmForm(ExcelSummons plugin, Player player,
                                           MenuModel.Entry entry, int fromPage) {
        return BedrockForms.sendSimpleForm(player,
                "Dismiss " + entry.displayName() + "?",
                MenuModel.confirmBody(entry),
                List.of(MenuModel.confirmButtonLabel(entry), MenuModel.KEEP_BUTTON_LABEL),
                idx -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;
                    if (idx == 0) {
                        boolean done = plugin.listener().dismissOneAdopted(
                                player.getUniqueId(), entry.record().entityId(),
                                player.getName());
                        player.sendMessage(done
                                ? "Your " + entry.displayName() + " has been dismissed."
                                : "That pet couldn't be dismissed right now — nothing was removed.");
                    }
                    if (!sendAdoptedForm(plugin, player)) sendChatFallback(plugin, player);
                }), plugin.getLogger());
    }

    /** Form failure fallback: the chat list — the same lines
     *  /summons list prints (cast + adopted), then the held line.
     *  A form failure never leaves the player with nothing. */
    private static void sendChatFallback(ExcelSummons plugin, Player player) {
        MenuModel.Page model = plugin.menuPageFor(player.getUniqueId());
        player.sendMessage("The menu form didn't open — here is your list:");
        if (model.totalRegistered() == 0) {
            player.sendMessage("You have no summons registered.");
            return;
        }
        for (MenuModel.Entry e : model.castEntries()) player.sendMessage(e.listLine());
        for (MenuModel.Entry e : model.adoptedEntries()) player.sendMessage(e.listLine());
        if (model.heldPercent() > 0) {
            player.sendMessage("Mana held by your summons: " + model.heldPercent()
                    + "% of your pool.");
        }
    }

    // ============================== helpers ==============================

    static ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.WHITE));
            List<Component> loreC = new ArrayList<>();
            for (String l : lore) loreC.add(Component.text(l, NamedTextColor.GRAY));
            meta.lore(loreC);
            it.setItemMeta(meta);
        }
        return it;
    }
}
