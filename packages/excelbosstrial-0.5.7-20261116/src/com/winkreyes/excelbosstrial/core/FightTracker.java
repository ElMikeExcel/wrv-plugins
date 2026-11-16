/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The fight record (ExcelBossTrial 0.3.1): per-player damage and
 * healing tables for boss encounters, snapshotted at every verdict
 * and every elder trio progress point.
 *
 * Why this exists: the EncounterLedger has always tracked
 * dealtByPlayer / takenByPlayer, but only in memory - the verdict
 * line carried the aggregate share and the per-player table died
 * with the encounter (the Oct 31, 2026 elder fight is the standing
 * example: 97.0% player damage, and no record of WHO dealt what).
 * Mike asked for the breakdown (Nov 1, 2026), so the ledger's table
 * is now written down before it evaporates, and healing joins it.
 *
 * Healing, honestly bounded (the investigation is recorded in the
 * 0.3.1 SOURCE.md): Bukkit's EntityRegainHealthEvent carries an
 * amount and a cause but NO source player. Effective healing
 * (capped at the health actually missing) is therefore always
 * recorded per RECEIVING player during an open encounter. Healing
 * DONE is attributed only where a real source exists: the Magic
 * plugin's CastEvent names the caster, the spell and the target
 * entity, and Magic's HealAction fires its regain event (cause
 * CUSTOM) inside the same cast - so a heal-spell cast claim and a
 * CUSTOM regain on the same target within the claim window are
 * matched at snapshot time. Everything unmatched stays in a
 * clearly labeled unattributed bucket. No fake precision: a heal
 * whose source nobody named is never guessed onto a player.
 *
 * This class is the data layer a future live meter (the
 * Details!-style direction Mike named Nov 1, 2026) reads: one
 * record per encounter, flat per-player rows with stable keys,
 * written both as a YAML file and as log lines complete enough to
 * rebuild the record from latest.log alone.
 *
 * Adapter-style by design (house build style): the chapters and
 * the ledger are untouched - verdicts, rewards, pools and ledger
 * math read nothing from here. The adapter opens an encounter
 * beside each ledger, feeds healing events and cast claims in,
 * and asks for the snapshot at the verdict. A failure here can
 * never reach the fight.
 */
public final class FightTracker {

    /** Regain cause name Magic's HealAction fires (its own event,
     *  constructed with RegainReason.CUSTOM before applying). Only
     *  CUSTOM regains are eligible for cast-claim matching. */
    public static final String SPELL_HEAL_CAUSE = "CUSTOM";

    private static final class HealEvent {
        final UUID target;
        final double effective;
        final String cause;
        final long timeMs;

        HealEvent(UUID target, double effective, String cause, long timeMs) {
            this.target = target;
            this.effective = effective;
            this.cause = cause;
            this.timeMs = timeMs;
        }
    }

    private static final class CastClaim {
        final UUID caster;
        final UUID target;
        final String spellKey;
        final long timeMs;

        CastClaim(UUID caster, UUID target, String spellKey, long timeMs) {
            this.caster = caster;
            this.target = target;
            this.spellKey = spellKey;
            this.timeMs = timeMs;
        }
    }

    private static final class Encounter {
        final BossKind kind;
        final long openedMs;
        final Map<UUID, Double> receivedByPlayer = new LinkedHashMap<>();
        final List<HealEvent> healEvents = new ArrayList<>();
        final List<CastClaim> claims = new ArrayList<>();
        final Set<UUID> roster = new LinkedHashSet<>();

        Encounter(BossKind kind, long openedMs) {
            this.kind = kind;
            this.openedMs = openedMs;
        }
    }

    private final Map<UUID, Encounter> open = new LinkedHashMap<>();

    /** Open tracking beside a freshly created ledger. Re-opening
     *  an already-tracked boss resets its healing state (a ledger
     *  re-open is a new encounter by the retention rules too). */
    public synchronized void openEncounter(UUID bossUuid, BossKind kind, long nowMs) {
        open.put(bossUuid, new Encounter(kind, nowMs));
    }

    /** Drop an encounter's healing state without a snapshot (the
     *  boss burrowed, escaped, despawned or was pruned). */
    public synchronized void closeEncounter(UUID bossUuid) {
        open.remove(bossUuid);
    }

    public synchronized boolean isOpen(UUID bossUuid) {
        return open.containsKey(bossUuid);
    }

    /** Players with any healing activity in this encounter (healed,
     *  cast a heal claim, or were a claim's target). */
    public synchronized Set<UUID> roster(UUID bossUuid) {
        Encounter e = open.get(bossUuid);
        return e == null ? Set.of() : new LinkedHashSet<>(e.roster);
    }

    public synchronized boolean rosterContains(UUID bossUuid, UUID player) {
        Encounter e = open.get(bossUuid);
        return e != null && e.roster.contains(player);
    }

    /** True when a heal-spell cast claim names this player as its
     *  target in this encounter (the association that lets a heal
     *  land on a champion who has not yet dealt or taken damage). */
    public synchronized boolean hasClaimFor(UUID bossUuid, UUID target) {
        Encounter e = open.get(bossUuid);
        if (e == null) {
            return false;
        }
        for (CastClaim c : e.claims) {
            if (c.target.equals(target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Record one regain event against an open encounter. Only the
     * EFFECTIVE healing counts: the raw amount capped at the
     * health the target was actually missing (overheal is not
     * healing). Returns the effective amount recorded (0 when the
     * encounter is not open, the amount is empty, or the target
     * was already full).
     */
    public synchronized double recordHealing(UUID bossUuid, UUID target, double rawAmount,
                                             double healthBefore, double maxHealth,
                                             String cause, long nowMs) {
        Encounter e = open.get(bossUuid);
        if (e == null || target == null) {
            return 0;
        }
        double effective = effectiveHealing(rawAmount, healthBefore, maxHealth);
        if (effective <= 0) {
            return 0;
        }
        e.receivedByPlayer.merge(target, effective, Double::sum);
        e.healEvents.add(new HealEvent(target, effective,
                cause == null ? "UNKNOWN" : cause, nowMs));
        e.roster.add(target);
        return effective;
    }

    /** Effective healing of one regain: never negative, never
     *  more than the health that was missing. */
    public static double effectiveHealing(double rawAmount, double healthBefore,
                                          double maxHealth) {
        if (Double.isNaN(rawAmount) || rawAmount <= 0) {
            return 0;
        }
        double missing = maxHealth - healthBefore;
        if (Double.isNaN(missing) || missing <= 0) {
            return 0;
        }
        return Math.min(rawAmount, missing);
    }

    /**
     * Record a heal-spell cast claim (the Magic feed): this caster
     * cast a known heal spell at this target. The claim does not
     * move any healing by itself - at snapshot time it absorbs the
     * CUSTOM regain events on its target inside the claim window.
     * Returns false when no encounter is open for the boss (a cast
     * outside any fight is not fight healing).
     */
    public synchronized boolean recordClaim(UUID bossUuid, UUID caster, UUID target,
                                            String spellKey, long nowMs) {
        Encounter e = open.get(bossUuid);
        if (e == null || caster == null || target == null) {
            return false;
        }
        e.claims.add(new CastClaim(caster, target, spellKey, nowMs));
        e.roster.add(caster);
        e.roster.add(target);
        return true;
    }

    /**
     * Build the fight record for a finished encounter. The ledger
     * may be null (a verdict on a boss no governed encounter ever
     * opened for): the record still stands, with empty damage rows
     * and whatever healing was tracked. Does NOT close the
     * encounter - the adapter closes it (death) or drops it
     * (verdict-less endings) explicitly.
     *
     * @param verdictLabel   the verdict name (CREDIT, DENY_SHARE,
     *                       REPEAT_CREDIT, ...) or an elder
     *                       progress label (ELDER_PROGRESS n of 3)
     * @param names          display names by UUID (missing = the
     *                       UUID string stands in)
     * @param claimWindowMs  the cast-claim matching window
     */
    public synchronized FightRecord snapshot(UUID bossUuid, EncounterLedger ledger,
                                             String verdictLabel, UUID killer,
                                             Collection<UUID> championsStanding,
                                             Map<UUID, String> names,
                                             long claimWindowMs, long nowMs,
                                             String context) {
        Encounter e = open.get(bossUuid);
        BossKind kind = ledger != null ? ledger.kind()
                : e != null ? e.kind : null;
        if (kind == null) {
            return null;
        }
        long startedMs = ledger != null ? ledger.startedMs()
                : e != null ? e.openedMs : nowMs;

        // Resolve cast claims against CUSTOM regain events:
        // nearest claim for the same target inside the window
        // absorbs the amount; a claim may absorb several events
        // (a heal-over-time ticks), and events are matched in
        // time order so earlier heals claim first.
        Map<UUID, Double> healingDone = new LinkedHashMap<>();
        double unattributedSpell = 0;
        Map<String, Double> unattributedByCause = new LinkedHashMap<>();
        if (e != null) {
            for (HealEvent h : e.healEvents) {
                if (!SPELL_HEAL_CAUSE.equals(h.cause)) {
                    unattributedByCause.merge(h.cause, h.effective, Double::sum);
                    continue;
                }
                CastClaim best = null;
                long bestDelta = Long.MAX_VALUE;
                for (CastClaim c : e.claims) {
                    if (!c.target.equals(h.target)) {
                        continue;
                    }
                    long delta = Math.abs(c.timeMs - h.timeMs);
                    if (delta <= claimWindowMs && delta < bestDelta) {
                        best = c;
                        bestDelta = delta;
                    }
                }
                if (best != null) {
                    healingDone.merge(best.caster, h.effective, Double::sum);
                } else {
                    unattributedSpell += h.effective;
                }
            }
        }

        Set<UUID> rowPlayers = new LinkedHashSet<>();
        if (ledger != null) {
            rowPlayers.addAll(ledger.participants());
        }
        if (e != null) {
            rowPlayers.addAll(e.roster);
        }
        List<FightRecord.Row> rows = new ArrayList<>();
        for (UUID p : rowPlayers) {
            rows.add(new FightRecord.Row(p, nameOf(names, p),
                    ledger != null ? ledger.dealtBy(p) : 0.0,
                    ledger != null ? ledger.takenBy(p) : 0.0,
                    healingDone.getOrDefault(p, 0.0),
                    e != null ? e.receivedByPlayer.getOrDefault(p, 0.0) : 0.0));
        }
        rows.sort((a, b) -> {
            int c = Double.compare(b.damageDealt, a.damageDealt);
            if (c != 0) {
                return c;
            }
            c = Double.compare(b.healingDone, a.healingDone);
            if (c != 0) {
                return c;
            }
            c = Double.compare(b.healingReceived, a.healingReceived);
            if (c != 0) {
                return c;
            }
            c = a.name.compareToIgnoreCase(b.name);
            return c != 0 ? c : a.uuid.toString().compareTo(b.uuid.toString());
        });

        List<UUID> standing = championsStanding == null
                ? List.of() : List.copyOf(championsStanding);
        List<String> standingNames = new ArrayList<>();
        for (UUID u : standing) {
            standingNames.add(nameOf(names, u));
        }
        return new FightRecord(kind, bossUuid, verdictLabel,
                killer, killer == null ? null : nameOf(names, killer),
                ledger != null ? ledger.maxHealth() : 0.0,
                ledger != null ? ledger.totalDamage() : 0.0,
                ledger != null ? ledger.playerDamage() : 0.0,
                ledger != null ? ledger.playerSharePercent() : 0.0,
                ledger != null ? ledger.otherDamage() : 0.0,
                ledger != null ? ledger.healedPool() : 0.0,
                ledger != null ? ledger.netDamage() : 0.0,
                standing, standingNames, startedMs, nowMs,
                rows, unattributedSpell, unattributedByCause, context);
    }

    // ------------------------------------------------------------------
    // Reading a record back (0.4.1 - ㉚.9's data door)
    // ------------------------------------------------------------------

    /**
     * Parse one record file's text (the exact shape
     * {@link FightRecord#toYaml} writes) back into a record:
     * the /bosstrial stats lookup's read path, so the stats
     * surface re-shows the SAME record the close wrote rather
     * than keeping a second accounting. Returns null on
     * anything malformed (no seal, no verdict, a boss UUID
     * that does not parse): a half-written or foreign file is
     * skipped, never guessed at.
     *
     * One honest loss, by the file's own design:
     * champions-standing persists NAMES, not UUIDs, so a
     * parsed record's championsStanding list is empty and
     * the names list carries the standing party.
     */
    public static FightRecord parseRecord(String yaml) {
        if (yaml == null) {
            return null;
        }
        String seal = null;
        String bossUuidText = null;
        String context = null;
        String verdict = null;
        String killerName = null;
        String killerUuidText = null;
        double pool = 0, totalDamage = 0, playerDamage = 0, otherDamage = 0;
        double healedPool = 0, netDamage = 0, sharePercent = 0;
        double unattributedSpell = 0;
        Map<String, Double> unattributedByCause = new LinkedHashMap<>();
        List<String> standingNames = new ArrayList<>();
        long startedMs = 0, endedMs = 0;
        List<FightRecord.Row> rows = new ArrayList<>();

        String section = "";
        Map<String, String> row = null;
        for (String rawLine : yaml.split("\n")) {
            String line = rawLine.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (!rawLine.startsWith(" ") && !rawLine.startsWith("\t")) {
                // A top-level key ends any indented section.
                if (row != null) {
                    addParsedRow(rows, row);
                    row = null;
                }
                section = "";
                String[] kv = splitKey(line);
                if (kv == null) {
                    continue;
                }
                String key = kv[0];
                String value = kv[1];
                switch (key) {
                    case "seal" -> seal = unquote(value);
                    case "boss-uuid" -> bossUuidText = unquote(value);
                    case "context" -> context = unquote(value);
                    case "verdict" -> verdict = unquote(value);
                    case "killer" -> killerName = unquote(value);
                    case "killer-uuid" -> killerUuidText = unquote(value);
                    case "pool" -> pool = number(value);
                    case "total-damage" -> totalDamage = number(value);
                    case "player-damage" -> playerDamage = number(value);
                    case "other-damage" -> otherDamage = number(value);
                    case "healed-pool" -> healedPool = number(value);
                    case "net-damage" -> netDamage = number(value);
                    case "player-share-percent" -> sharePercent = number(value);
                    case "champions-standing-names" -> standingNames = quotedList(value);
                    case "started" -> startedMs = instantMs(value);
                    case "ended" -> endedMs = instantMs(value);
                    case "unattributed-healing" -> section = "unattributed";
                    case "players" -> section = "players";
                    default -> {
                        // boss display name, champions-standing
                        // count, duration-seconds: derivable or
                        // display-only - not record state.
                    }
                }
                continue;
            }
            if (section.equals("unattributed")) {
                if (line.startsWith("spell:")) {
                    unattributedSpell = number(line.substring("spell:".length()));
                } else if (line.equals("other-by-cause:")) {
                    section = "bycause";
                }
                // "other:" is the by-cause sum, which the
                // record itself derives - not state.
                continue;
            }
            if (section.equals("bycause")) {
                String[] kv = splitKey(line);
                if (kv != null) {
                    unattributedByCause.put(kv[0], number(kv[1]));
                }
                continue;
            }
            if (section.equals("players")) {
                if (line.startsWith("- name:")) {
                    if (row != null) {
                        addParsedRow(rows, row);
                    }
                    row = new LinkedHashMap<>();
                    row.put("name", unquote(line.substring("- name:".length())));
                } else if (row != null) {
                    String[] kv = splitKey(line);
                    if (kv != null) {
                        row.put(kv[0], kv[1]);
                    }
                }
            }
        }
        if (row != null) {
            addParsedRow(rows, row);
        }

        BossKind kind = seal == null ? null : BossKind.fromSealId(seal);
        UUID bossUuid = parseUuid(bossUuidText);
        if (kind == null || verdict == null || bossUuid == null) {
            return null;
        }
        return new FightRecord(kind, bossUuid, verdict,
                parseUuid(killerUuidText), killerName,
                pool, totalDamage, playerDamage, sharePercent,
                otherDamage, healedPool, netDamage,
                List.of(), standingNames, startedMs, endedMs,
                rows, unattributedSpell, unattributedByCause, context);
    }

    private static void addParsedRow(List<FightRecord.Row> rows,
                                     Map<String, String> fields) {
        UUID uuid = parseUuid(unquote(fields.get("uuid")));
        if (uuid == null) {
            return;
        }
        String name = fields.get("name");
        rows.add(new FightRecord.Row(uuid,
                name == null || name.isBlank() ? uuid.toString() : name,
                number(fields.get("damage-dealt")),
                number(fields.get("damage-taken")),
                number(fields.get("healing-done")),
                number(fields.get("healing-received"))));
    }

    /** "key: value" split at the FIRST ": " (values may
     *  themselves contain ": " inside quotes). A trailing
     *  "key:" section header yields an empty value. */
    private static String[] splitKey(String line) {
        int idx = line.indexOf(": ");
        if (idx < 0) {
            if (line.endsWith(":")) {
                return new String[] {line.substring(0, line.length() - 1), ""};
            }
            return null;
        }
        return new String[] {line.substring(0, idx), line.substring(idx + 2)};
    }

    /** Inverse of {@link FightRecord#yamlQuote}: strips the
     *  quotes and unescapes; the literal null reads as null;
     *  anything else stands as written. */
    private static String unquote(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        if (v.equals("null")) {
            return null;
        }
        if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
            String inner = v.substring(1, v.length() - 1);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < inner.length(); i++) {
                char c = inner.charAt(i);
                if (c == '\\' && i + 1 < inner.length()) {
                    sb.append(inner.charAt(++i));
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }
        return v;
    }

    /** The quoted items of a flow list ("["A", "B"]"). */
    private static List<String> quotedList(String value) {
        List<String> out = new ArrayList<>();
        if (value == null) {
            return out;
        }
        int i = 0;
        while (i < value.length()) {
            if (value.charAt(i) != '"') {
                i++;
                continue;
            }
            StringBuilder sb = new StringBuilder();
            i++;
            while (i < value.length()) {
                char c = value.charAt(i);
                if (c == '\\' && i + 1 < value.length()) {
                    sb.append(value.charAt(++i));
                    i++;
                } else if (c == '"') {
                    i++;
                    break;
                } else {
                    sb.append(c);
                    i++;
                }
            }
            out.add(sb.toString());
        }
        return out;
    }

    private static double number(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (RuntimeException bad) {
            return 0;
        }
    }

    private static long instantMs(String value) {
        String v = unquote(value);
        if (v == null) {
            return 0;
        }
        try {
            return Instant.parse(v).toEpochMilli();
        } catch (RuntimeException bad) {
            return 0;
        }
    }

    private static UUID parseUuid(String text) {
        if (text == null) {
            return null;
        }
        try {
            return UUID.fromString(text.trim());
        } catch (RuntimeException bad) {
            return null;
        }
    }

    private static String nameOf(Map<UUID, String> names, UUID uuid) {
        if (names != null) {
            String n = names.get(uuid);
            if (n != null && !n.isBlank()) {
                return n;
            }
        }
        return uuid.toString();
    }

    // ------------------------------------------------------------------
    // The record itself
    // ------------------------------------------------------------------

    /** One finished encounter's table: immutable, meter-ready. */
    public static final class FightRecord {

        /** One player's line of the table. */
        public static final class Row {
            public final UUID uuid;
            public final String name;
            public final double damageDealt;
            public final double damageTaken;
            public final double healingDone;
            public final double healingReceived;

            Row(UUID uuid, String name, double damageDealt, double damageTaken,
                double healingDone, double healingReceived) {
                this.uuid = uuid;
                this.name = name;
                this.damageDealt = damageDealt;
                this.damageTaken = damageTaken;
                this.healingDone = healingDone;
                this.healingReceived = healingReceived;
            }
        }

        public final BossKind kind;
        public final UUID bossUuid;
        public final String verdict;
        public final UUID killerUuid;
        public final String killerName;
        public final double pool;
        public final double totalDamage;
        public final double playerDamage;
        public final double sharePercent;
        /** 0.4.0 (PM-9): the unattributed half of the pool -
         *  mob-on-boss and environment damage - persisted so a
         *  spent pool is always attributable after the fact
         *  (Nov 7: the final 5,470 could not be split). */
        public final double otherDamage;
        /** 0.4.0 (PM-1): pool-space healing the boss received. */
        public final double healedPool;
        /** 0.4.0 (PM-1): gross dealt minus healing - the number
         *  the bar and the verdict judged on. */
        public final double netDamage;
        public final List<UUID> championsStanding;
        public final List<String> championsStandingNames;
        public final long startedMs;
        public final long endedMs;
        public final List<Row> rows;
        /** CUSTOM-cause healing no cast claim absorbed. */
        public final double unattributedSpellHealing;
        /** Non-CUSTOM healing by cause (never source-attributed). */
        public final Map<String, Double> unattributedByCause;
        /** Free context (the monument id for elder fights). */
        public final String context;

        FightRecord(BossKind kind, UUID bossUuid, String verdict,
                    UUID killerUuid, String killerName,
                    double pool, double totalDamage, double playerDamage,
                    double sharePercent, double otherDamage, double healedPool,
                    double netDamage, List<UUID> championsStanding,
                    List<String> championsStandingNames,
                    long startedMs, long endedMs, List<Row> rows,
                    double unattributedSpellHealing,
                    Map<String, Double> unattributedByCause, String context) {
            this.kind = kind;
            this.bossUuid = bossUuid;
            this.verdict = verdict;
            this.killerUuid = killerUuid;
            this.killerName = killerName;
            this.pool = pool;
            this.totalDamage = totalDamage;
            this.playerDamage = playerDamage;
            this.sharePercent = sharePercent;
            this.otherDamage = otherDamage;
            this.healedPool = healedPool;
            this.netDamage = netDamage;
            this.championsStanding = championsStanding;
            this.championsStandingNames = championsStandingNames;
            this.startedMs = startedMs;
            this.endedMs = endedMs;
            this.rows = List.copyOf(rows);
            this.unattributedSpellHealing = unattributedSpellHealing;
            this.unattributedByCause = Map.copyOf(unattributedByCause);
            this.context = context;
        }

        public long durationSeconds() {
            return Math.max(0L, (endedMs - startedMs) / 1000L);
        }

        /** fights/&lt;seal&gt;-&lt;yyyyMMdd-HHmmss&gt;-&lt;boss8&gt;.yml */
        public String fileName() {
            String ts = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochMilli(endedMs));
            String boss8 = bossUuid.toString().substring(0, 8);
            return kind.sealId() + "-" + ts + "-" + boss8 + ".yml";
        }

        /** The STABLE name of an in-progress snapshot (0.4.0,
         *  PM-8): the 60-second mid-fight snapshots overwrite
         *  this one file per encounter, so even a hard crash
         *  leaves a current table; the ending's own fileName()
         *  write supersedes it and the adapter removes this. */
        public String stableFileName() {
            String boss8 = bossUuid.toString().substring(0, 8);
            return kind.sealId() + "-open-" + boss8 + ".yml";
        }

        /** The structured record file's text (stable keys, one
         *  flat row per player - the meter's data shape). */
        public String toYaml() {
            StringBuilder sb = new StringBuilder();
            sb.append("# ExcelBossTrial fight record - one row per player per encounter.\n");
            sb.append("# healing-done counts only source-attributed healing (a named\n");
            sb.append("# heal-spell cast matched to its regain); every other heal lands\n");
            sb.append("# in healing-received and the unattributed buckets below.\n");
            sb.append("seal: ").append(kind.sealId()).append('\n');
            sb.append("boss: ").append(yamlQuote(kind.displayName())).append('\n');
            sb.append("boss-uuid: ").append(yamlQuote(bossUuid.toString())).append('\n');
            if (context != null) {
                sb.append("context: ").append(yamlQuote(context)).append('\n');
            }
            sb.append("verdict: ").append(yamlQuote(verdict)).append('\n');
            sb.append("killer: ").append(killerName == null ? "null"
                    : yamlQuote(killerName)).append('\n');
            sb.append("killer-uuid: ").append(killerUuid == null ? "null"
                    : yamlQuote(killerUuid.toString())).append('\n');
            sb.append("pool: ").append(num(pool)).append('\n');
            sb.append("total-damage: ").append(num(totalDamage)).append('\n');
            sb.append("player-damage: ").append(num(playerDamage)).append('\n');
            sb.append("other-damage: ").append(num(otherDamage)).append('\n');
            sb.append("healed-pool: ").append(num(healedPool)).append('\n');
            sb.append("net-damage: ").append(num(netDamage)).append('\n');
            sb.append("player-share-percent: ").append(num(sharePercent)).append('\n');
            sb.append("champions-standing: ").append(championsStanding.size()).append('\n');
            sb.append("champions-standing-names: [");
            for (int i = 0; i < championsStandingNames.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(yamlQuote(championsStandingNames.get(i)));
            }
            sb.append("]\n");
            sb.append("duration-seconds: ").append(durationSeconds()).append('\n');
            sb.append("started: ").append(yamlQuote(iso(startedMs))).append('\n');
            sb.append("ended: ").append(yamlQuote(iso(endedMs))).append('\n');
            sb.append("unattributed-healing:\n");
            sb.append("  spell: ").append(num(unattributedSpellHealing)).append('\n');
            sb.append("  other: ").append(num(otherUnattributed())).append('\n');
            if (!unattributedByCause.isEmpty()) {
                sb.append("  other-by-cause:\n");
                for (Map.Entry<String, Double> e : unattributedByCause.entrySet()) {
                    sb.append("    ").append(e.getKey()).append(": ")
                            .append(num(e.getValue())).append('\n');
                }
            }
            sb.append("players:\n");
            for (Row r : rows) {
                sb.append("  - name: ").append(yamlQuote(r.name)).append('\n');
                sb.append("    uuid: ").append(yamlQuote(r.uuid.toString())).append('\n');
                sb.append("    damage-dealt: ").append(num(r.damageDealt)).append('\n');
                sb.append("    damage-taken: ").append(num(r.damageTaken)).append('\n');
                sb.append("    healing-done: ").append(num(r.healingDone)).append('\n');
                sb.append("    healing-received: ").append(num(r.healingReceived)).append('\n');
            }
            return sb.toString();
        }

        /** Log lines carrying the whole record (header + one line
         *  per player + the unattributed line), so the record can
         *  be rebuilt from latest.log alone. */
        public List<String> logLines(String fileName) {
            List<String> out = new ArrayList<>();
            String head = "BossTrial fight record [" + kind.displayName() + "] verdict="
                    + verdict + " pool=" + num(pool) + " total-damage=" + num(totalDamage)
                    + " player-damage=" + num(playerDamage)
                    + " other-damage=" + num(otherDamage)
                    + " healed-pool=" + num(healedPool)
                    + " net-damage=" + num(netDamage)
                    + " share=" + num(sharePercent) + "%"
                    + " champions-standing=" + championsStanding.size()
                    + " killer=" + (killerName == null ? "none" : killerName)
                    + " duration=" + durationSeconds() + "s"
                    + (context != null ? " context=" + context : "")
                    + " file=fights/" + fileName;
            out.add(head);
            for (Row r : rows) {
                out.add("BossTrial fight record [" + kind.displayName() + "] player "
                        + r.name + " (" + r.uuid + "): dealt=" + num(r.damageDealt)
                        + " taken=" + num(r.damageTaken)
                        + " healing-done=" + num(r.healingDone)
                        + " healing-received=" + num(r.healingReceived));
            }
            if (unattributedSpellHealing > 0 || otherUnattributed() > 0) {
                StringBuilder sb = new StringBuilder("BossTrial fight record ["
                        + kind.displayName() + "] unattributed healing: spell="
                        + num(unattributedSpellHealing) + " other="
                        + num(otherUnattributed()));
                if (!unattributedByCause.isEmpty()) {
                    sb.append(" by-cause{");
                    boolean first = true;
                    for (Map.Entry<String, Double> e : unattributedByCause.entrySet()) {
                        if (!first) {
                            sb.append(", ");
                        }
                        first = false;
                        sb.append(e.getKey()).append('=').append(num(e.getValue()));
                    }
                    sb.append('}');
                }
                out.add(sb.toString());
            }
            return out;
        }

        public double otherUnattributed() {
            double sum = 0;
            for (double v : unattributedByCause.values()) {
                sum += v;
            }
            return sum;
        }

        private static String iso(long ms) {
            return DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(ms));
        }

        /** One decimal, always (the verdict lines' own style). */
        static String num(double v) {
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                return "0.0";
            }
            return String.format(java.util.Locale.ROOT, "%.1f", v);
        }

        static String yamlQuote(String s) {
            return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
