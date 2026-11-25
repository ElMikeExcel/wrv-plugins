package com.winkreyes.exceldowned;

/**
 * Pure policy for the Revive Potion feature (ExcelDowned 0.1.6,
 * spec A — board-r506/revive/A-DOWNED-FEATURE-DESIGN.md sections
 * 3–5, 7, 8; decisions D106 adopt the spec defaults S-1..S-8).
 *
 * No Bukkit types: every branch the listener/UI/command layers apply
 * is decided here so the harness (DownedTest) exercises production
 * logic, in the FixPolicy / AdminPolicy house pattern. The Bukkit
 * mechanics (PDC scan, UI presentation, the revive call) live in
 * RevivePotionItem / RevivePotionUi / ExcelDowned and stay thin.
 *
 * Rules pinned here:
 *  - OPEN targeting (1.0.2 — SUPERSEDES design S-1's party-only
 *    potion targeting, Mike's decision, Nov 19, 2026: "being able
 *    to just use it on anyone you come across to be helpful"):
 *    the Revive Potion works on ANY downed player — no party
 *    membership is consulted anywhere on the potion path (prompt,
 *    interact, confirm). The Resurrection SPELL keeps its own
 *    party rule (MagicBridge) and the Cleric Raise Ally gate is
 *    untouched; both live outside this policy.
 *  - Self-revive excluded (spec 5.3): carrier == target never
 *    prompts and never confirms.
 *  - Ranges (S-7): prompt 5.0 / confirm 6.0 defaults (config-fed).
 *  - Cooldown (Mike, Nov 17, 2026, 7:47 PM — SUPERSEDES the spec's
 *    2 s anti-double-click guard): a successful potion revive puts
 *    the CARRIER on a 10-minute per-player cooldown
 *    (config revive-potion.use-cooldown-seconds, default 600). It
 *    is a SEPARATE cooldown from the Cleric ability's own
 *    10-minute cooldown — the two run in parallel and each can fire
 *    once back-to-back. CONFIG RULE (Mike, 7:48 PM): every tunable
 *    is config-fed into this policy; nothing is hardcoded here.
 *  - Ordering invariant (spec 3.5): a potion is consumed ONLY after
 *    the revive has landed — shouldConsume(reviveLanded) is the
 *    whole rule, and it is reviveLanded itself.
 *  - The give seam (spec 7/8) is gated by the EXISTING admin node
 *    via AdminPolicy.mayUseAdmin (console || holdsAdmin), denial
 *    precedes all target handling (0.1.2 discipline).
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class RevivePotionPolicy {

    private RevivePotionPolicy() {}

    // ---------- targeting (spec 3.1) ----------

    /**
     * Prompt eligibility (spec 3.2): the carrier is standing, holds
     * at least one potion, and the target is a downed player in the
     * same world within prompt range. Self never prompts. Party
     * membership is NOT an input (1.0.2 open targeting — see the
     * class header): any downed player prompts.
     */
    public static boolean promptEligible(boolean enabled, boolean self,
                                         boolean carrierDowned, boolean targetDowned,
                                         boolean sameWorld, double distanceSquared,
                                         double promptRange, int potionCount,
                                         boolean cooldownActive) {
        if (!enabled || self || carrierDowned || !targetDowned) return false;
        if (cooldownActive) return false;
        if (potionCount < 1) return false;
        return FixPolicy.withinReviveRange(sameWorld, distanceSquared, promptRange);
    }

    /** Why a confirm was refused (the UI maps each to one line). */
    public enum ConfirmVerdict {
        OK,
        FEATURE_DISABLED,
        STALE_TICKET,
        SELF_TARGET,
        CARRIER_DOWNED,
        CARRIER_ON_COOLDOWN,
        TARGET_NOT_DOWNED,
        WRONG_WORLD,
        OUT_OF_RANGE,
        NO_POTION
    }

    /**
     * The confirm decision table (spec 3.5): everything is
     * re-derived at click time — ticket currency, carrier state,
     * cooldown, target validity at CONFIRM range, and a fresh
     * potion count. The copy in the UI is never trusted.
     */
    public static ConfirmVerdict confirmVerdict(boolean enabled, boolean ticketCurrent,
                                                boolean self, boolean carrierDowned,
                                                boolean cooldownActive, boolean targetDowned,
                                                boolean sameWorld, double distanceSquared,
                                                double confirmRange, int potionCount) {
        if (!enabled) return ConfirmVerdict.FEATURE_DISABLED;
        if (!ticketCurrent) return ConfirmVerdict.STALE_TICKET;
        if (self) return ConfirmVerdict.SELF_TARGET;
        if (carrierDowned) return ConfirmVerdict.CARRIER_DOWNED;
        if (cooldownActive) return ConfirmVerdict.CARRIER_ON_COOLDOWN;
        if (!targetDowned) return ConfirmVerdict.TARGET_NOT_DOWNED;
        if (!sameWorld) return ConfirmVerdict.WRONG_WORLD;
        if (!FixPolicy.withinReviveRange(true, distanceSquared, confirmRange)) {
            return ConfirmVerdict.OUT_OF_RANGE;
        }
        if (potionCount < 1) return ConfirmVerdict.NO_POTION;
        return ConfirmVerdict.OK;
    }

    // ---------- ordering invariant (spec 3.5) ----------

    /**
     * THE anti-loss rule: consumption happens if and only if the
     * revive landed. A lost race (spell / another potion / operator
     * / bleed-out / give-up in the same instant) consumes nothing.
     */
    public static boolean shouldConsume(boolean reviveLanded) {
        return reviveLanded;
    }

    // ---------- cooldowns + throttles (spec 4, 5.2, 5.4) ----------

    /** Per-carrier use-cooldown: active only after a SUCCESS. */
    public static boolean cooldownActive(long nowMs, long lastSuccessMs, long cooldownMs) {
        return lastSuccessMs > 0 && cooldownMs > 0 && nowMs - lastSuccessMs < cooldownMs;
    }

    /** Zero-potion hint throttle, per (carrier, target) pair. */
    public static boolean hintAllowed(long nowMs, long lastHintMs, long throttleMs) {
        return lastHintMs <= 0 || throttleMs <= 0 || nowMs - lastHintMs >= throttleMs;
    }

    // ---------- inventory counting / consumption (pure core) ----------
    // RevivePotionItem extracts per-slot (tagged, amount) arrays from
    // the live inventory and delegates the arithmetic here, so the
    // count/consume-once rules are harness-tested on production logic.

    /** Total potions across tagged slots (amounts summed). */
    public static int countFromSlots(boolean[] tagged, int[] amounts) {
        int total = 0;
        int n = Math.min(tagged.length, amounts.length);
        for (int i = 0; i < n; i++) {
            if (tagged[i] && amounts[i] > 0) total += amounts[i];
        }
        return total;
    }

    /**
     * Index of the slot consumeOne must decrement (the first tagged
     * slot with amount >= 1), or -1 when there is nothing to consume.
     */
    public static int consumeSlotIndex(boolean[] tagged, int[] amounts) {
        int n = Math.min(tagged.length, amounts.length);
        for (int i = 0; i < n; i++) {
            if (tagged[i] && amounts[i] >= 1) return i;
        }
        return -1;
    }

    // ---------- the give seam (spec 7/8) ----------

    /** What a /downed revivepotion invocation routes to. */
    public enum GiveRoute {
        /** Not a revivepotion invocation at all. */
        NOT_OURS,
        /** give <player> [amount], gate passed. */
        GIVE,
        /** confirm — the chat-fallback door (player-only, ungated). */
        CONFIRM,
        /** give invocation with a missing/extra/bad argument. */
        USAGE,
        /** give invocation from a sender without the gate. */
        DENIED
    }

    /** A routed invocation; target/amount are set only for GIVE. */
    public record GiveParsed(GiveRoute route, String target, int amount) {
        static GiveParsed of(GiveRoute route) { return new GiveParsed(route, null, 0); }
    }

    public static boolean isRevivePotionVerb(String arg) {
        return arg != null && "revivepotion".equalsIgnoreCase(arg);
    }

    /**
     * Parse a give amount: absent means 1; a positive integer up to
     * 64 is accepted; anything else is -1 (a usage error, never a
     * partial mint).
     */
    public static int parseGiveAmount(String raw) {
        if (raw == null || raw.isBlank()) return 1;
        try {
            int v = Integer.parseInt(raw.trim());
            return (v >= 1 && v <= 64) ? v : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Route one /downed invocation's revivepotion forms:
     *   revivepotion give <player> [amount]   (admin-gated, spec 8)
     *   revivepotion confirm                  (chat fallback, S-5)
     * The gate precedes usage and target handling for give, so a
     * sender without the node learns nothing (0.1.2 discipline).
     *
     * The paid verb is NOT routed here: "revivepotion buy" belongs
     * to routeBuy below (W6 section 3.2). Before the charge leg,
     * routeGive mapped "buy" to the free GIVE mint — that alias is
     * retired: no spelling in which "buy" means "free mint" may
     * survive the shop opening.
     */
    public static GiveParsed routeGive(String[] args, boolean consoleSender, boolean holdsAdmin) {
        if (args == null || args.length == 0 || !isRevivePotionVerb(args[0])) {
            return GiveParsed.of(GiveRoute.NOT_OURS);
        }
        if (args.length >= 2 && "confirm".equalsIgnoreCase(args[1])) {
            return args.length == 2 ? GiveParsed.of(GiveRoute.CONFIRM)
                    : GiveParsed.of(GiveRoute.USAGE);
        }
        if (args.length >= 2 && "give".equalsIgnoreCase(args[1])) {
            if (!AdminPolicy.mayUseAdmin(consoleSender, holdsAdmin)) {
                return GiveParsed.of(GiveRoute.DENIED);
            }
            if (args.length < 3 || args[2].isBlank()) return GiveParsed.of(GiveRoute.USAGE);
            if (args.length > 4) return GiveParsed.of(GiveRoute.USAGE);
            int amount = parseGiveAmount(args.length == 4 ? args[3] : null);
            if (amount < 0) return GiveParsed.of(GiveRoute.USAGE);
            return new GiveParsed(GiveRoute.GIVE, args[2], amount);
        }
        return GiveParsed.of(GiveRoute.USAGE);
    }

    // ---------- the paid buy verb (W6 section 3.2, charge leg) ----------

    /** What a /downed revivepotion buy invocation routes to. */
    public enum BuyRoute {
        /** Not a buy invocation at all. */
        NOT_OURS,
        /** buy <player> from the console: open the purchase review. */
        BUY,
        /** buy invocation with a missing/extra/bad argument. */
        USAGE,
        /** buy from a non-console sender: refused, shop pointer. */
        DENIED
    }

    /** A routed buy invocation; target is set only for BUY. */
    public record BuyParsed(BuyRoute route, String target) {
        static BuyParsed of(BuyRoute route) { return new BuyParsed(route, null); }
    }

    /**
     * Route the paid verb (W6 section 3.2): the ONLY menu target is
     * the console dispatch "[console] downed revivepotion buy
     * %player_name%". Console-only by design — the review opens for
     * the NAMED buyer, exactly as "mobveil buy <player>" /
     * "deathprotection buy basic <player>" resolve theirs. A player
     * sender is DENIED (the command layer answers with the shop
     * pointer; there is no typed second shop). Quantity is fixed
     * at 1: an amount argument is a usage error, never a quantity.
     *
     * The retired free-buy spellings never route to a mint: this
     * route claims ONLY "revivepotion buy <player>" (exactly 3
     * args); the old "revive buy ..." shop alias returns NOT_OURS
     * here (and is not a revivepotion verb for routeGive either),
     * so it lands on the generic usage surface — never GIVE.
     */
    public static BuyParsed routeBuy(String[] args, boolean consoleSender) {
        if (args == null || args.length == 0 || !isRevivePotionVerb(args[0])) {
            return BuyParsed.of(BuyRoute.NOT_OURS);
        }
        if (args.length < 2 || !"buy".equalsIgnoreCase(args[1])) {
            return BuyParsed.of(BuyRoute.NOT_OURS);
        }
        if (!consoleSender) return BuyParsed.of(BuyRoute.DENIED);
        if (args.length != 3 || args[2].isBlank()) return BuyParsed.of(BuyRoute.USAGE);
        return new BuyParsed(BuyRoute.BUY, args[2]);
    }
}
