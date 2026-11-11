package com.winkreyes.excelsummons;

/**
 * The archer equipment guarantee (0.1.5): the spell definitions of
 * record (packet-summons-20261108, summons-archer delta; DESIGN
 * §2.2–2.3) specify the archer's gear at every tier — a bow in hand
 * at all tiers, an iron helmet at the base tier, a chainmail helmet
 * at tiers 2 and 3 (the helmet is also the skeleton's sun-proofing).
 * The applied spells config carries those values correctly at every
 * tier, but Magic's SpawnEntity layer does not apply item_in_hand
 * on spawn — Jester's Nov 10, 2026 report: the archer arrived
 * bowless (diagnosis of record:
 * archer-bow-diagnosis-20261110.md, layer-1 verdict). So the bridge
 * guarantees the gear itself when it registers a CAST archer: this
 * class is the pure spec (which keys, which tier, which material
 * names); SummonsListener applies it in the applyHarness soft-fail
 * shape and verifies by read-back.
 *
 * Tier knowledge: the registry record folds tiers into the family
 * base key, but the CAST-TIME spell key (archer|2) is in hand at
 * the one moment the guarantee runs — cast capture in
 * SummonsListener.onCast — so the tier is known exactly, never
 * guessed. The guarantee therefore does not need (and does not
 * add) a tier field to the record.
 *
 * Pure logic — material NAMES, no Bukkit types — so the spec is
 * unit-testable off-server, in the house policy shape.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class ArcherEquipment {

    private ArcherEquipment() {}

    /** The family this guarantee covers. */
    public static final String FAMILY = "archer";

    /** Hand item at every tier (the spell's item_in_hand: bow). */
    public static final String HAND_MATERIAL = "BOW";

    /** Helmet at the base tier (entity_parameters helmet:
     *  iron_helmet). */
    public static final String BASE_HELMET_MATERIAL = "IRON_HELMET";

    /** Helmet at tiers 2 and 3 (entity_parameters helmet:
     *  chainmail_helmet). */
    public static final String TIERED_HELMET_MATERIAL = "CHAINMAIL_HELMET";

    /** True when the guarantee covers this spell key: the archer
     *  family, any tier. Null-safe; near-miss keys (archerx) do not
     *  match — the family comes from SummonRecord.baseKey, the same
     *  fold the registry itself uses. */
    public static boolean guarantees(String spellKey) {
        return FAMILY.equals(SummonRecord.baseKey(spellKey));
    }

    /** The tier of a spell key: archer -> 1, archer|2 -> 2,
     *  archer|3 -> 3. A missing, unparsable, or sub-1 suffix reads
     *  as the BASE tier — the fail-safe direction: an unknown tier
     *  gets the base spec, never a guessed upgrade. */
    public static int tierOf(String spellKey) {
        if (spellKey == null) return 1;
        int bar = spellKey.indexOf('|');
        if (bar < 0 || bar == spellKey.length() - 1) return 1;
        try {
            int tier = Integer.parseInt(spellKey.substring(bar + 1).trim());
            return tier < 1 ? 1 : tier;
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /** The guaranteed main-hand material name for a cast of this
     *  spell key, or null when the guarantee does not cover the
     *  family (the caller then applies nothing). */
    public static String handMaterialName(String spellKey) {
        return guarantees(spellKey) ? HAND_MATERIAL : null;
    }

    /** The guaranteed helmet material name for a cast of this
     *  spell key, or null when the guarantee does not cover the
     *  family. Base tier: iron; tier 2 and above: chainmail (the
     *  spell book defines tiers 2 and 3 with chainmail; a future
     *  higher tier continues the tiered spec rather than dropping
     *  back to the base helmet). */
    public static String helmetMaterialName(String spellKey) {
        if (!guarantees(spellKey)) return null;
        return tierOf(spellKey) >= 2 ? TIERED_HELMET_MATERIAL : BASE_HELMET_MATERIAL;
    }

    /** Read-back verification: true only when the observed main-hand
     *  and helmet material names BOTH equal this key's guaranteed
     *  spec. Anything else — empty hand (null / AIR), the wrong
     *  helmet, a family the guarantee does not cover — is not a
     *  verified archer. The listener logs a soft note on a false
     *  read-back and moves on; verification never fails the cast. */
    public static boolean matches(String handMaterialName, String helmetMaterialName,
                                  String spellKey) {
        if (!guarantees(spellKey)) return false;
        return HAND_MATERIAL.equals(handMaterialName)
                && helmetMaterialName(spellKey).equals(helmetMaterialName);
    }
}
