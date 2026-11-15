package com.winkreyes.excelsummons;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The adopted-rename gate (ExcelSummons 0.1.9, Mike's Nov 13,
 * 2026 pet-names commission). The Summons menu's per-pet page
 * offers a Rename action for ADOPTED pets; this class is its
 * pure decision layer — eligibility, name sanitation, and the
 * audit line — so the harness proves the gate the live path
 * (SummonsListener.renameAdopted) runs. Same shape as
 * AdoptedDismissPolicy, and deliberately as narrow: the
 * record must exist, must belong to the acting owner, and
 * must be ADOPTED origin. A CAST record is refused: a cast
 * summon's name belongs to whatever set it (Magic's configured
 * name, if a spell ever sets one), and the nameplate
 * composition rule is NEVER CLOBBER — the menu rename does
 * not become a second name source fighting the first.
 *
 * Sanitation is policy, not cosmetics: a menu-set name becomes
 * the entity's nameplate base through the composition rule, so
 * a name the policy accepts must be a name the composition
 * keeps whole. Hence three refusals inside {@code acceptable}:
 * empty after cleaning (a pet renamed to nothing is a pet
 * renamed by accident); over the length cap (menu buttons and
 * plate lines must stay readable); and a name ENDING in the
 * nameplate's own health suffix (" — 12/20") — the next plate
 * refresh would strip that tail as if it were ours, silently
 * mangling the player's name.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class RenamePolicy {
    private RenamePolicy() {}

    /** The longest name the menu will set. Name-tag anvil names
     *  run longer, but a plate line and a form button have to
     *  stay legible; 32 characters is generous for a pet name. */
    public static final int MAX_NAME_LENGTH = 32;

    /** The outcome of one rename attempt, named so the menu can
     *  answer each case in its own words: RENAMED (the name is
     *  set and the plate re-rendered), REFUSED (the record is
     *  not this player's adopted pet — the flow never explains
     *  which part failed), INVALID_NAME (the cleaned name is
     *  empty, over the cap, or ends in the plate's health
     *  suffix), NOT_LOADED (the pet's entity is not loaded, so
     *  there is nothing to name — stand near it and retry). */
    public enum Result { RENAMED, REFUSED, INVALID_NAME, NOT_LOADED }

    private static final Pattern LEGACY_CODE =
            Pattern.compile("(?i)[§&][0-9a-fk-or]");
    private static final Pattern WHITESPACE_RUN = Pattern.compile("\\s+");

    /** May this single record be renamed through the menu flow
     *  by this owner? Mirrors AdoptedDismissPolicy.eligible. */
    public static boolean eligible(SummonRecord rec, UUID owner) {
        return rec != null && owner != null
                && owner.equals(rec.ownerId())
                && rec.origin() == SummonRecord.Origin.ADOPTED;
    }

    /** Clean a raw submitted name: legacy colour codes (§x and
     *  &x) removed entirely — a menu name is plain text, colours
     *  in a button label or plate line are noise — ISO control
     *  characters (newlines included) removed, whitespace runs
     *  collapsed to one space, ends trimmed. Null cleans to "". */
    public static String sanitize(String raw) {
        if (raw == null) return "";
        String noCodes = LEGACY_CODE.matcher(raw).replaceAll("");
        StringBuilder sb = new StringBuilder(noCodes.length());
        for (int i = 0; i < noCodes.length(); i++) {
            char c = noCodes.charAt(i);
            if (!Character.isISOControl(c)) sb.append(c);
        }
        return WHITESPACE_RUN.matcher(sb.toString()).replaceAll(" ").trim();
    }

    /** Is a CLEANED name one the menu may set? (Clean first with
     *  {@link #sanitize}; this judges the cleaned form.) */
    public static boolean acceptable(String cleaned) {
        if (cleaned == null || cleaned.isEmpty()) return false;
        if (cleaned.length() > MAX_NAME_LENGTH) return false;
        // A name ending in our own health suffix would be eaten
        // by the plate composition on the next refresh.
        return Nameplate.stripHealthSuffix(cleaned).equals(cleaned);
    }

    /** One-call convenience for the live path: sanitize + judge. */
    public static boolean acceptableRaw(String raw) {
        return acceptable(sanitize(raw));
    }

    /** The INFO audit line for one menu rename. It names the
     *  acting player, the pet's spell/type key, and the name
     *  that was set — a rename is how a player tells their
     *  animals apart, and the log should be able to say who
     *  named what. */
    public static String auditLine(String actorName, UUID actorId, SummonRecord rec,
                                   String newName) {
        String who = actorName == null || actorName.isBlank() ? "UNKNOWN" : actorName;
        return "Summons rename by " + who + " (" + actorId + "): adopted "
                + rec.spellKey() + " (" + rec.branch().name().toLowerCase()
                + ") renamed to '" + newName + "', entity " + rec.entityId()
                + " — menu rename flow.";
    }
}
