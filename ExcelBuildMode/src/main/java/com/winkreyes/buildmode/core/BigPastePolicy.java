package com.winkreyes.buildmode.core;

import java.util.OptionalInt;

/**
 * Big paste (ExcelBuildMode 1.3.2, Mike's approval Oct 21, 2026).
 *
 * The live WorldEdit config caps every session at max-blocks-changed 5000
 * (the pilot fragment) and BuildPolicy blocks //limit inside Build Mode by
 * design, so a Build Mode edit could never exceed 5,000 blocks — Marvin's
 * 70,326-block village paste had to run in Play Normally under a temporary
 * LuckPerms worldedit.* grant with a fragile exit dance (any external
 * WorldEdit grant trips the Build Mode exit probe).
 *
 * The fix keeps every one of those safeguards and adds ONE deliberate
 * exception: a player holding the excelbuildmode.bigpaste permission —
 * our own node, NOT a WorldEdit node, so it may be granted permanently
 * with zero interaction with the entry/exit probes — gets their WorldEdit
 * LocalSession block-change limit raised by the plugin itself while a
 * Build Mode session's WorldEdit grant is in effect. The raise lives in
 * the session-attach path, so normal entry and relog-restore both get it,
 * and it dies with the WorldEdit session. //limit stays blocked for
 * everyone; the 5000 config default stands for non-holders; WorldEdit's
 * own config is never edited.
 *
 * This class is the pure decision: given the facts, what limit (if any)
 * should the plugin apply? The Bukkit/WorldEdit mechanics live in the
 * paper bridge (WorldEditLimitBridge).
 */
public final class BigPastePolicy {
    private BigPastePolicy() { }

    /** Our own permission node. Deliberately NOT a worldedit.* node. */
    public static final String PERMISSION = "excelbuildmode.bigpaste";

    /**
     * Built-in default for the config knob big-paste.block-change-limit.
     * A large FINITE ceiling, chosen deliberately over -1 (unlimited):
     * the biggest job on record is the village — 70,326 blocks changed by
     * the paste, 433,998 positions in the //copy bounding box — and
     * 1,000,000 covers that with ~2.3x headroom while still bounding a
     * runaway edit (a mistyped //set over a huge selection) before it can
     * eat the server. The node is the gate; this number is only the size
     * of the room it opens.
     */
    public static final int DEFAULT_LIMIT = 1_000_000;

    /**
     * Sanitize a configured limit. Valid values are -1 (unlimited — an
     * operator's deliberate choice, honored) and any positive cap.
     * Anything else (0, below -1, i.e. a typo) falls back to the default
     * rather than silently disabling big pastes or unbounding them.
     */
    public static int sanitizeLimit(int configured) {
        if (configured == -1 || configured >= 1) return configured;
        return DEFAULT_LIMIT;
    }

    /**
     * The limit to apply to this player's WorldEdit session, or empty
     * when the session must be left untouched: the raise applies only
     * while the session's WorldEdit grant is in effect (the attach path
     * with grantWorldEdit) AND the player holds the bigpaste node.
     * Non-holders keep exactly the WorldEdit config default.
     */
    public static OptionalInt limitToApply(boolean sessionWorldEditGranted, boolean holdsBigPaste, int configuredLimit) {
        if (!sessionWorldEditGranted || !holdsBigPaste) return OptionalInt.empty();
        return OptionalInt.of(sanitizeLimit(configuredLimit));
    }
}
