# TEST RESULTS — ExcelSummons 0.1.5 (Nov 11, 2026)

## Unit / logic harness (SummonsTest, via local-build.sh)

**74/74 tests successful, 0 failed** — reproduced identically
in three independent builds (in-tree double-build, clean-room
rebuild, rebuild from the shipped source zip; all three jars
byte-identical at SHA-256 5cdf16a0…f61e).

Composition: the 69 carried 0.1.4 tests, of which 2 were
re-pinned for the version bump (pluginYmlPins014 →
pluginYmlPins015: version + fingerprint lineage assertions only;
configPins014 → configPins015: rename only — config.yml is
byte-identical to 0.1.4, every assertion carried verbatim), plus
5 new 0.1.5 tests, all on the pure ArcherEquipment spec:

- archerEquipmentCoversEveryTier — bow in hand at archer /
  archer|2 / archer|3; IRON_HELMET base, CHAINMAIL_HELMET at
  tiers 2 and 3; tier parsing 1/2/3.
- archerEquipmentIgnoresOtherFamilies — bruiser(+tier),
  guardian, revenant, ghast, mount, wolf, familiar, cat,
  near-miss "archerx", empty and null keys: not covered, null
  spec.
- archerEquipmentTierParsingFailsSafeToBase — unparsable /
  empty / sub-1 tier suffix reads as the BASE tier (iron
  helmet), never a guessed upgrade; a tier above the book
  continues the chainmail spec.
- archerEquipmentVerifyMatchesExactSpec — the read-back check
  passes on the exact spec at every tier.
- archerEquipmentVerifyRejectsMissingOrWrongGear — the reported
  defect itself (empty hand), helmetless, the wrong tier's
  helmet in both directions, and non-archer families all fail
  verification.

Spell-logic gate: this packet changes no spells.yml and no
spell config of any kind (bridge plugin change only), so the
spell harness has nothing to re-run; the config pins
(configPins015) confirm the bridge config — caps, durations,
reserve rows — is unchanged from 0.1.4.

## Desk rig (Paper 26.3 + Magic 11.2.5)

**19/19 behavior checks PASS, SEVERE 0, reserve-template
warnings 0, archer-equipment soft-fail lines 0.** The 14 carried
0.1.4 checks at their 0.1.4 verdicts + phase F's 5 new checks
(base / tier-2 / tier-3 equipment on real spawned skeletons
through the bridge's own registration-path method, the
archer-only guard, the non-living soft-fail). Full results:
`rig-0.1.5/RIG-RESULTS.md`; log: `rig-0.1.5/boot.log`.

## Not in this run (by design)

The in-game archer cast proof (a real Magic cast producing a
geared skeleton on the live-shaped spells packet) is Jester's
post-install proof, named in INSTALL-PACKET.md — the diagnosis
assigns it to the player, and the desk rig's Magic carries no
summons spells packet. This desk run makes no claim about
Magic's SpawnEntity internals beyond the diagnosis of record.
