# ExcelSummons 0.1.13 — summons mana +25% (P-SUMMONS-MANA-25)

Base: ExcelSummons 0.1.12 (GameplayEvent emitter adoption,
board-r365 tree), itself carrying the 0.1.11 D64 holds +
nameplate/despawn work. This build does NOT mutate the 0.1.12
tree/packet; it copies it (base gate: the unmodified copy
rebuilt byte-identical to the 0.1.12 candidate a21c584f…,
123/123, before any edit). Install order: AFTER ExcelSummons
0.1.12 lands — this packet's live-jar gate is the 0.1.12
candidate (116,690 B / a21c584f…). Installing it instead of
0.1.12 would drop the emitter adoption; basing on 0.1.11 was
rejected for the same reason (scope §3).

Decision of record: Mike, Nov 15, 2026, ~12:28 PM CST —
"Go with the +25% draft table." Design of record:
board-r392/summons-mana25/TABLE-DRAFT.md (024fcc7f…) as scoped
by board-r394/summons-mana-scope/SCOPE.md (52da51ea…).

Changes (ONE gated unit: jar + config + Magic modifiers —
the 0.1.11 shape, NOT the jar-only 0.1.12 shape; scope flag B):
1. Raised mana holds (+25% on D64, whole-percent halves-up)
   in config.yml summon-reserve-percent: archer 25, bruiser
   31, guardian 44, revenant 38, ghast 44, wolf 19, familiar
   13, cat 13, mount 19; ceiling 75, floor 20, adopted 0
   unchanged. The 18 Magic modifier templates MUST be
   replaced in the same window (packet
   modifiers-replacement-0.1.13.yml) — config and templates
   are one number, two readers (DESIGN-DRAFT §2.5).
2. ReservePolicy.java: history-comment append only — no
   logic edit anywhere (the mechanism is config-driven).
3. Accepted consequences (draft §4, adopted with the table):
   one elite at a time (guardian+ghast 88, elite+revenant 82
   — second elite PreCast-refused); the Master 5-wolf pack
   caps at 3 mana-holding wolves (57; a fresh 4th cast is
   PreCast-refused at 57+19=76>75 — scope flag A path 1 —
   while grandfathered 4th/5th wolves re-derive to holding
   nothing, never trimmed — flag A path 2); exact-fit fields
   stay legal (archer+bruiser+wolf 75, guardian+bruiser 75,
   archer+bruiser+mount 75).
4. Carried unchanged: the 0.1.12 GameplayEvent emitter
   (summon_refusal / summon_spawn / summon_despawn /
   mana_reserve into ExcelProtect's audit door) — its field
   builders report the NEW percents automatically because
   they carry the bridge's applied numbers; emitter tests
   re-pinned to the +25% values as regression proof.

NOT changed: caps, durations, cast costs, dismiss scopes,
rename, registry data format, emitter contract, ceiling /
floor law, adopted-origin exemption.
