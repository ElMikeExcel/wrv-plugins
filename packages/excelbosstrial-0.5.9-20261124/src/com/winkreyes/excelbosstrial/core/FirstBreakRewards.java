/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.time.LocalDate;
import java.util.List;

/**
 * The WARDEN FIRST-BREAK layer (ExcelBossTrial 0.5.5; design
 * ~/workspace/warden-firstbreak-20261110/DESIGN-AND-BUILDSCOPE.md,
 * commissioned by Mike, Nov 10, 2026 - the Warden first-break
 * table approved the same morning, cash settled by decision-log
 * D6).
 *
 * Before 0.5.5 the repeat slate paid ONLY on REPEAT_CREDIT: a
 * first break (verdict CREDIT) received the signature pool and
 * ordinary bonus rolls, and none of the cash / accelerant /
 * war-chest / cosmetic / healer rows. This layer gives the
 * Warden's first break that slate at first-break values - cash
 * $10,000 per champion TOTAL, replacing the $5,000 repeat rate
 * for this kill, never stacked on it - plus the first-break-only
 * extras: the one-time party title "Sealbreakers" (a claim-stored
 * name tag, the suite's only title mechanism), a
 * provenance-stamped Warden trophy item per champion, and a
 * vanilla-consumables EliteMobs starter kit per champion
 * (consumables only, NOT gear - EM gear caps at diamond and sits
 * below the family's carried netherite).
 *
 * Pure data + math like the rest of core: the Bukkit adapter
 * (ExcelBossTrial) owns the once-flag persistence (state.yml
 * first-break-paid.warden, written and flushed BEFORE any
 * first-break reward moves, so a crash can underpay but never
 * double-pay), the payment rails (the repeat layer's own -
 * Vault cash, Underdog-scaled accelerants, adapter war-chest
 * services, /bossrewards claims), and the potion-item builder.
 */
public final class FirstBreakRewards {

    /** The one-time party title (commission wording, verbatim). */
    public static final String TITLE = "Sealbreakers";

    /** The personal trophy's display name (design section 2.2). */
    public static final String TROPHY_NAME = "Heart of the Deep \u2014 Warden Trophy";

    /** The trophy's material: a display shard, deliberately an
     *  item and not gear - no stats, never confused with the
     *  Hushwalkers signature. */
    public static final String TROPHY_MATERIAL = "ECHO_SHARD";

    /** The claims distribution id for the first-break store. */
    public static final String DISTRIBUTION_ID = "first-break-v1";

    /**
     * One EM-kit row: a plain material stack, or a potion row
     * ({@code potion} = the effect key the adapter maps to a
     * base potion type, {@code level} = its tier, 1 or 2). The
     * material itself carries drink vs splash (POTION vs
     * SPLASH_POTION); the adapter owns PotionMeta.
     */
    public record KitItem(String material, int count, String potion, int level) {
        public KitItem {
            material = material == null || material.isBlank() ? "STONE" : material.trim();
            count = Math.max(1, count);
            potion = potion == null || potion.isBlank() ? null : potion.trim().toUpperCase(java.util.Locale.ROOT);
            level = Math.max(1, Math.min(2, level));
        }

        public static KitItem plain(String material, int count) {
            return new KitItem(material, count, null, 1);
        }

        public static KitItem potion(String material, int count, String potion, int level) {
            return new KitItem(material, count, potion, level);
        }
    }

    /**
     * The first-break slate spec (config block
     * {@code first-break.warden:}, mirroring the {@code repeat:}
     * key names). Every value is config-driven; {@link #settled()}
     * is the commissioned table.
     */
    public record Spec(boolean enabled, long cashPerChampion, int jackpotExtraRolls,
                       long mcMmoXp, long magicLevelXp, double claimflyHours,
                       boolean deathProtectionPremium, boolean healerRecognitionEnabled,
                       String cosmeticMaterial, String title, List<KitItem> emKit) {
        public Spec {
            cashPerChampion = Math.max(0L, cashPerChampion);
            jackpotExtraRolls = Math.max(0, jackpotExtraRolls);
            mcMmoXp = Math.max(0L, mcMmoXp);
            magicLevelXp = Math.max(0L, magicLevelXp);
            claimflyHours = Math.max(0.0, claimflyHours);
            cosmeticMaterial = cosmeticMaterial == null || cosmeticMaterial.isBlank()
                    ? "ECHO_SHARD" : cosmeticMaterial.trim();
            title = title == null || title.isBlank() ? TITLE : title.trim();
            emKit = emKit == null ? List.of() : List.copyOf(emKit);
        }

        /** The commissioned first-break table (design section 2.2/2.4). */
        public static Spec settled() {
            return new Spec(true, 10000L, 2, 5000L, 250L, 2.0, true, true,
                    "ECHO_SHARD", TITLE, settledKit());
        }

        /** The EM starter kit exactly as designed (design section
         *  2.4): vanilla consumables only, per roster champion. */
        public static List<KitItem> settledKit() {
            return List.of(
                    KitItem.plain("GOLDEN_CARROT", 16),
                    KitItem.plain("ENCHANTED_GOLDEN_APPLE", 1),
                    KitItem.plain("GOLDEN_APPLE", 4),
                    KitItem.plain("TOTEM_OF_UNDYING", 1),
                    KitItem.potion("SPLASH_POTION", 4, "HEALING", 2),
                    KitItem.potion("POTION", 2, "STRENGTH", 2),
                    KitItem.potion("POTION", 2, "SWIFTNESS", 2),
                    KitItem.plain("MILK_BUCKET", 2),
                    KitItem.plain("EXPERIENCE_BOTTLE", 16));
        }
    }

    private FirstBreakRewards() {
    }

    /**
     * The eligibility matrix (design section 2.1): the layer
     * fires only on the break itself - verdict CREDIT, the
     * WARDEN chapter, not an elder trio-progress point, the
     * layer enabled, and the durable once-flag not yet paid.
     * By construction of CreditJudge, a Warden CREDIT can only
     * be judged while the seal stands unbroken; the flag is the
     * belt over that brace (admin revoke + re-break, a state
     * restore, a crash between the GuildGate credit and this
     * consequence pass). REPEAT_CREDIT never fires it, so
     * repeat breaks pay the ordinary repeat slate unchanged.
     */
    public static boolean eligible(CreditJudge.Verdict verdict, BossKind kind,
                                   boolean elderProgress, boolean alreadyPaid, Spec spec) {
        return spec != null && spec.enabled()
                && verdict == CreditJudge.Verdict.CREDIT
                && kind == BossKind.WARDEN
                && !elderProgress
                && !alreadyPaid;
    }

    /** Rolls for the first-break kill: the ordinary bonus rolls
     *  plus the first-break jackpot extras, drawn over the
     *  combined bonus + jackpot table - the repeat shape. */
    public static int rolls(int presentParticipants, Spec spec) {
        int extra = spec == null ? 0 : spec.jackpotExtraRolls();
        return SealLoot.repeatRolls(presentParticipants, extra);
    }

    /** The party title item's lore (design section 2.2): the
     *  award line, then the Deep line carrying the break date. */
    public static List<String> titleLore(LocalDate date) {
        return List.of(
                "Awarded for the first breaking of the Warden seal.",
                "The Deep listened \u2014 and the party answered. "
                        + ProvenanceLore.formatDate(date) + ".");
    }

    /** The trophy's lore: the earner's provenance lines (via
     *  {@link ProvenanceLore}, the same stamp named signature
     *  gear carries), then the party line the guild-hall plaque
     *  step reads back from the first-break record. */
    public static List<String> trophyLore(String earnerName, LocalDate date,
                                          List<String> partyNames) {
        java.util.List<String> out = new java.util.ArrayList<>(
                ProvenanceLore.lines(earnerName, date));
        out.add(partyLine(partyNames));
        return List.copyOf(out);
    }

    /** The "First break party: ..." line (names joined; an
     *  empty party degrades to "a champion", never a blank). */
    public static String partyLine(List<String> partyNames) {
        List<String> names = partyNames == null ? List.of()
                : partyNames.stream().filter(n -> n != null && !n.isBlank()).toList();
        return "First break party: " + (names.isEmpty() ? "a champion" : String.join(", ", names));
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
