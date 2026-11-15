package com.winkreyes.excelsummons;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The /summons menu page model (ExcelSummons 0.1.7, Mike's Nov 12,
 * 2026 compass-page commission + his ~6:01 PM refinement). Pure —
 * no Bukkit types — so the harness proves the page's content the
 * same way it proves the list lines: the ONLY Bukkit-dependent
 * input is each entity's rendered HP text, which the plugin passes
 * in as a map (entity id -> "HP a/b" or "not loaded"), computed
 * with the exact expression /summons list uses.
 *
 * Page shape (the refinement): the MAIN page carries CAST summons
 * only — one entry per cast record with the same facts as the
 * /summons list line (Nameplate.listLine over spell, branch, time
 * left, HP, and the APPLIED hold from ReservePolicy) — plus the
 * total applied held percent and the three existing dismiss
 * scopes as buttons. ADOPTED pets never render on the main page:
 * they live on a sub-page reached by the "Adopted pets (N)"
 * button, where every row carries the ADOPTED badge and "holds
 * no mana" indicators. In 0.1.7 the sub-page's only action was a
 * PER-PET dismiss behind a confirm step that names the pet and
 * warns it permanently removes the real tamed animal (Mike's
 * ~6:05 PM ruling): no bulk adopted dismissal exists, and the
 * command scopes still never select adopted records
 * (AdminPolicy) — the confirm-gated menu flow is the only
 * adopted-dismiss door, gated by AdoptedDismissPolicy. 0.1.9
 * (below) routes the row tap through a per-pet page that adds
 * Rename beside that same confirm-gated dismiss.
 *
 * 0.1.8 adds the out-now counts (Mike, Nov 12, 2026, evening —
 * "I was hoping for some number of mounts or number of cast
 * pets would show"): the main page carries how many mounts,
 * cast pets (wolf / cat / familiar) and combat summons the
 * player has OUT right now, plus the total cast out, next to
 * the held line. The counts derive from the page's CAST
 * entries via SummonRecord.baseKey (tiered keys fold into
 * their base) and are shown ALWAYS — a group at zero prints
 * its zero, so "no pets out" is never confused with "no
 * data". Adopted pets are never counted as "out": they are
 * permanent animals, and this line describes what a cast has
 * put on the field. Both renderers read the same derivation:
 * the Bedrock body line and the Java info book's lore.
 *
 * 0.1.9 adds PET NAMES (Mike, Nov 13, 2026 — "Can we already
 * rename the wolfs? So we know which one is which?"): an
 * entry's display name is the entity's BASE NAME — its custom
 * name with the nameplate HP suffix stripped, exactly the base
 * Nameplate.baseName computes and refreshNameplate composes
 * from — whenever the pet carries one, and the spell type only
 * as the fallback for an unnamed summon. Through 0.1.8 the
 * display name was ALWAYS the type, so a pack of 8 wolves
 * rendered 8 identical rows and 8 identical "Dismiss Wolf"
 * buttons — including on the confirm step for a dismissal that
 * permanently removes a real animal. The name now flows to the
 * list rows, the per-pet buttons, and the confirm text in one
 * move (they all read displayName), and the adopted Bedrock
 * body line leads with it. The base names arrive pre-computed
 * as a map, the same pattern as the HP text: the plugin reads
 * the live entities, MenuModel stays pure. Cast summons are
 * covered by the same map — a cast summon that carries a name
 * shows it — and the per-pet page (§ rename, RenamePolicy)
 * hangs off the adopted entries.
 *
 * 0.1.10 (BB-11, Nov 14, 2026) adds the MAIN page's Back: the
 * main page is a top-level screen reached from the Server
 * Menu, and through 0.1.9 it had no way back on either
 * platform (back-button audit FIX-2, 2 rows). MAIN_BACK_LABEL /
 * MAIN_BACK_INDEX / mainFormButtons below are the Bedrock
 * half; the Java half is SummonsMenus.SLOT_BACK_TO_MENU. The
 * adopted sub-page and the dismiss confirm already carried
 * their return controls and are unchanged.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class MenuModel {

    private MenuModel() {}

    /** Java inventory content slots per page (a 54-slot view's
     *  bottom row is the control row). */
    public static final int PAGE_SIZE = 45;

    /** The indicator text every adopted row carries, in the Java
     *  lore and the Bedrock body alike. */
    public static final String ADOPTED_BADGE = "ADOPTED";
    public static final String ADOPTED_NO_MANA = "holds no mana";

    // ----------------- adopted dismiss (confirm-gated, per-pet)

    /** The per-pet dismiss button label on the adopted sub-page.
     *  There is one per pet and NO bulk form anywhere. */
    public static String dismissButtonLabel(Entry e) {
        return "Dismiss " + e.displayName();
    }

    /** The confirm step's body: it names the specific pet and
     *  states plainly that this permanently removes the player's
     *  real tamed/bred animal — not a spell summon, not
     *  re-summonable, not undoable. */
    public static String confirmBody(Entry e) {
        return "Dismiss your " + e.displayName() + "?\n\n"
                + "This is your real tamed animal — not a spell summon. "
                + "Dismissing removes it permanently, and it cannot be "
                + "re-summoned. This cannot be undone.";
    }

    /** The confirm action's label — the ONLY control that can
     *  dismiss an adopted pet. */
    public static String confirmButtonLabel(Entry e) {
        return "Yes — remove my " + e.displayName() + " for good";
    }

    /** The confirm step's way out: nothing is dismissed. */
    public static final String KEEP_BUTTON_LABEL = "No — keep my pet";

    // ----------------- per-pet page + rename (0.1.9)

    /** The per-pet page's rename button label (Java view and
     *  Bedrock pet form alike). */
    public static String renameButtonLabel(Entry e) {
        return "Rename " + e.displayName();
    }

    /** The Bedrock adopted form's buttons (0.1.9): one button
     *  per pet, labelled with the pet's own display name —
     *  tapping it opens that pet's page (rename / dismiss) —
     *  then the way back. The handler routes index < pet count
     *  to that pet and the last index to Back. */
    public static List<String> adoptedFormButtons(Page page) {
        List<String> buttons = new ArrayList<>();
        for (Entry e : page.adoptedEntries()) buttons.add(e.displayName());
        buttons.add(ADOPTED_BACK_LABEL);
        return List.copyOf(buttons);
    }

    /** The adopted form's last button — back to the main page. */
    public static final String ADOPTED_BACK_LABEL = "Back to your summons";

    /** The Bedrock pet form's buttons, in order: rename,
     *  dismiss (which routes to the confirm step), back to the
     *  adopted page. Indices are the PET_* constants below. */
    public static List<String> petFormButtons(Entry e) {
        return List.of(renameButtonLabel(e), dismissButtonLabel(e), PET_BACK_LABEL);
    }

    public static final int PET_RENAME_INDEX = 0;
    public static final int PET_DISMISS_INDEX = 1;
    public static final int PET_BACK_INDEX = 2;

    /** The pet form's way back to the adopted page. */
    public static final String PET_BACK_LABEL = "Back to adopted pets";

    /** One rendered summon row. {@code listLine} is byte-for-byte
     *  the /summons list line for this record (Nameplate.listLine
     *  over the same inputs listLineFor computes). {@code
     *  displayName} is the entity's base name when it carries one
     *  (0.1.9), else its spell type's display name. */
    public record Entry(SummonRecord record, String listLine, String displayName,
                        String branchLower, String originLower, String timeLeft,
                        String hp, int holdPercent, boolean adopted) {
        /** The Bedrock body line. A cast summon's line is its
         *  list line. An adopted pet's line LEADS with its
         *  display name (0.1.9 — so a pack of named wolves reads
         *  as eight different animals in the body text, not only
         *  on the buttons), then the same facts the list line
         *  carries, then the ADOPTED / no-mana indicators. */
        public String bedrockLine() {
            if (!adopted) return listLine;
            return "- " + displayName + " (" + branchLower + ", " + timeLeft
                    + ", " + hp + ") — " + ADOPTED_BADGE + ", " + ADOPTED_NO_MANA;
        }
    }

    /** One dismiss button: label + the EXISTING scope it routes
     *  to. There are exactly three — the scopes the commands have
     *  always had (bare dismiss = MOUNT_ONLY, pets, all). */
    public record DismissButton(String label, AdminPolicy.DismissScope scope) {}

    public static final List<DismissButton> DISMISS_BUTTONS = List.of(
            new DismissButton("Dismiss mount", AdminPolicy.DismissScope.MOUNT_ONLY),
            new DismissButton("Dismiss cast pets", AdminPolicy.DismissScope.PETS),
            new DismissButton("Dismiss all (mount + combat)", AdminPolicy.DismissScope.ALL));

    /** The scope a Bedrock main-form button index routes to, or
     *  null when the index is not a dismiss button. The main
     *  form's button order is DISMISS_BUTTONS then the adopted
     *  sub-page button at index ADOPTED_BUTTON_INDEX. */
    public static AdminPolicy.DismissScope scopeForButton(int index) {
        if (index < 0 || index >= DISMISS_BUTTONS.size()) return null;
        return DISMISS_BUTTONS.get(index).scope();
    }

    /** Index of the "Adopted pets (N)" button on the Bedrock main
     *  form (immediately after the three dismiss buttons). */
    public static final int ADOPTED_BUTTON_INDEX = DISMISS_BUTTONS.size();

    // ----------------- main page Back (0.1.10, BB-11 FIX-2)

    /** The main page's Back label — identical on the Java view
     *  (an ARROW item) and the Bedrock main form (its last
     *  button). It lands on the ExcelMenu Server Menu, opened
     *  through the fleet performCommand("menu") opener. */
    public static final String MAIN_BACK_LABEL = "Back to Server Menu";

    /** Index of the Back button on the Bedrock main form —
     *  immediately after the adopted door, so every pre-0.1.10
     *  button index (dismiss 0-2, adopted 3) is unchanged and
     *  Back is LAST. It is NOT a dismiss button:
     *  scopeForButton(MAIN_BACK_INDEX) is null. */
    public static final int MAIN_BACK_INDEX = ADOPTED_BUTTON_INDEX + 1;

    /** The Bedrock MAIN form's buttons, in order (0.1.10): the
     *  three dismiss labels, the adopted sub-page door, then
     *  Back to Server Menu. The handler routes MAIN_BACK_INDEX
     *  to the Server Menu opener, ADOPTED_BUTTON_INDEX to the
     *  adopted form, and 0-2 through scopeForButton. */
    public static List<String> mainFormButtons(Page page) {
        List<String> buttons = new ArrayList<>();
        for (DismissButton b : DISMISS_BUTTONS) buttons.add(b.label());
        buttons.add(page.adoptedButtonLabel());
        buttons.add(MAIN_BACK_LABEL);
        return List.copyOf(buttons);
    }

    /** The dismiss result message — the exact text the /summons
     *  dismiss command sends, so the menu and the command can
     *  never report a dismissal differently. */
    public static String resultMessage(int dismissed) {
        return dismissed == 0 ? "Nothing to dismiss."
                : "Dismissed " + dismissed + " summon(s).";
    }

    /** A built page: the viewing player's split registry + totals. */
    public static final class Page {
        private final List<Entry> castEntries;
        private final List<Entry> adoptedEntries;
        private final int heldPercent;
        private final OutCounts outCounts;

        private Page(List<Entry> castEntries, List<Entry> adoptedEntries, int heldPercent,
                     OutCounts outCounts) {
            this.castEntries = castEntries;
            this.adoptedEntries = adoptedEntries;
            this.heldPercent = heldPercent;
            this.outCounts = outCounts;
        }

        public List<Entry> castEntries() { return castEntries; }
        public List<Entry> adoptedEntries() { return adoptedEntries; }
        public int totalRegistered() { return castEntries.size() + adoptedEntries.size(); }
        public int heldPercent() { return heldPercent; }
        public OutCounts outCounts() { return outCounts; }

        /** The 0.1.8 out-now line — the same text on the Bedrock
         *  main body and in the Java info book's lore. All three
         *  group counts ALWAYS print (a zero is information, not
         *  absence); with nothing cast out, the line says so. */
        public String outNowLine() {
            if (outCounts.totalCastOut() == 0) return "Nothing out right now.";
            return "Out now: " + counted(outCounts.mountsOut(), "mount")
                    + " \u00B7 " + counted(outCounts.castPetsOut(), "cast pet")
                    + " \u00B7 " + counted(outCounts.combatOut(), "combat summon")
                    + " (" + outCounts.totalCastOut() + " total)";
        }

        private static String counted(int n, String noun) {
            return n + " " + noun + (n == 1 ? "" : "s");
        }

        /** The main page's adopted button label, count included. */
        public String adoptedButtonLabel() {
            return "Adopted pets (" + adoptedEntries.size() + ")";
        }

        public int castPageCount() { return pageCount(castEntries.size()); }
        public int adoptedPageCount() { return pageCount(adoptedEntries.size()); }

        /** One Java page of cast entries (index clamped). */
        public List<Entry> castPage(int page) { return slice(castEntries, page); }
        /** One Java page of adopted entries (index clamped). */
        public List<Entry> adoptedPage(int page) { return slice(adoptedEntries, page); }

        /** The Bedrock MAIN form body: totals, then EVERY cast
         *  line (no pagination on Bedrock — the body is text). */
        public String mainBody() {
            StringBuilder sb = new StringBuilder();
            sb.append("You have ").append(totalRegistered())
              .append(" summon(s) registered.\n");
            sb.append(outNowLine()).append("\n");
            if (heldPercent > 0) {
                sb.append("Mana held by your summons: ").append(heldPercent)
                  .append("% of your pool.\n");
            }
            if (castEntries.isEmpty()) {
                sb.append("No cast summons out right now.");
            } else {
                for (Entry e : castEntries) sb.append(e.listLine()).append("\n");
            }
            return sb.toString().strip();
        }

        /** The Bedrock ADOPTED form body: EVERY adopted line, each
         *  carrying the ADOPTED / no-mana indicators. Each pet's
         *  own dismiss button sits below the body; removing one
         *  asks for confirmation first. */
        public String adoptedBody() {
            StringBuilder sb = new StringBuilder();
            sb.append("Your own tamed animals. They hold no mana. Tap a pet "
                    + "below to open its page — rename it, or dismiss it "
                    + "(dismissing asks you to confirm first).\n");
            if (adoptedEntries.isEmpty()) {
                sb.append("No adopted pets registered.");
            } else {
                for (Entry e : adoptedEntries) sb.append(e.bedrockLine()).append("\n");
            }
            return sb.toString().strip();
        }
    }

    static int pageCount(int total) {
        return Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private static List<Entry> slice(List<Entry> all, int page) {
        int pages = pageCount(all.size());
        int p = Math.max(0, Math.min(page, pages - 1));
        int from = p * PAGE_SIZE;
        int to = Math.min(all.size(), from + PAGE_SIZE);
        return List.copyOf(all.subList(from, to));
    }

    /**
     * Build the page for one owner. {@code hpByEntity} carries the
     * pre-rendered HP text per entity id ("HP a/b"); a record with
     * no entry renders "not loaded" — the same fallback the list
     * uses for an unloaded entity. {@code baseNameByEntity}
     * carries each entity's base name (0.1.9: Nameplate.baseName
     * over its live custom name); a record with no entry, or a
     * blank one, renders under its spell type. Holds come from ReservePolicy
     * exactly as listLineFor computes them: a record shows its
     * family percent only when recordHolds says the applied state
     * actually carries it (adopted records and clamp-skipped cast
     * records show no hold).
     */
    public static Page build(SummonRegistry reg, UUID owner,
                             Map<String, Integer> percents,
                             int ceilingPercent, int floorPercent,
                             long nowMillis, Map<UUID, String> hpByEntity,
                             Map<UUID, String> baseNameByEntity) {
        List<Entry> cast = new ArrayList<>();
        List<Entry> adopted = new ArrayList<>();
        for (SummonRecord r : reg.ofOwner(owner)) {
            String left = r.expiresAtMillis() == null ? "persistent"
                    : Math.max(0, (r.expiresAtMillis() - nowMillis) / 1000) + "s left";
            String hp = hpByEntity == null ? "not loaded"
                    : hpByEntity.getOrDefault(r.entityId(), "not loaded");
            int hold = ReservePolicy.recordHolds(reg, owner, r.entityId(),
                    percents, ceilingPercent, floorPercent)
                    ? ReservePolicy.percentFor(percents, r.spellKey()) : 0;
            boolean isAdopted = r.origin() == SummonRecord.Origin.ADOPTED;
            // 0.1.9: the entity's own base name when it carries
            // one; the spell type's display name only as the
            // unnamed fallback.
            String baseName = baseNameByEntity == null
                    ? null : baseNameByEntity.get(r.entityId());
            String displayName = baseName != null && !baseName.isBlank()
                    ? baseName
                    : Nameplate.spellDisplayName(SummonRecord.baseKey(r.spellKey()));
            Entry e = new Entry(r, Nameplate.listLine(r.spellKey(),
                    r.branch().name().toLowerCase(), left, hp, hold),
                    displayName,
                    r.branch().name().toLowerCase(),
                    r.origin().name().toLowerCase(), left, hp, hold, isAdopted);
            (isAdopted ? adopted : cast).add(e);
        }
        int held = ReservePolicy.appliedPercent(reg, owner, percents,
                ceilingPercent, floorPercent);
        return new Page(List.copyOf(cast), List.copyOf(adopted), held,
                outCountsOf(cast));
    }

    /** The 0.1.8 out-now counts over a page's CAST entries.
     *  Grouping by SummonRecord.baseKey: "mount" is a mount;
     *  "wolf" / "cat" / "familiar" are cast pets; every other
     *  cast base key (archer / bruiser / guardian / revenant /
     *  ghast) is a combat summon. Adopted records are never in
     *  the cast list, so they can never be counted here. */
    static OutCounts outCountsOf(List<Entry> castEntries) {
        int mounts = 0, pets = 0, combat = 0;
        for (Entry e : castEntries) {
            switch (SummonRecord.baseKey(e.record().spellKey())) {
                case "mount" -> mounts++;
                case "wolf", "cat", "familiar" -> pets++;
                default -> combat++;
            }
        }
        return new OutCounts(mounts, pets, combat, castEntries.size());
    }

    /** The out-now counts for one page — mounts out, cast pets
     *  out, combat summons out, and the total cast out. Counts
     *  are over CAST summons only; adopted pets never appear. */
    public record OutCounts(int mountsOut, int castPetsOut, int combatOut, int totalCastOut) {}
}
