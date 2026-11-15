# ExcelSummons 0.1.11 — summons tuning (P-SUMMONS-TUNING-1)

Base: ExcelSummons 0.1.10 (BB-11 back-button build tree,
~/workspace/back-button-wave3-r356/BB-11/build-0.1.10/), itself
base-gated byte-identical on the live 0.1.9 seq-02 tree
(ead3beec…). This build does NOT mutate the BB-11 tree/packet;
it copies it. Install order: AFTER BB-11 0.1.10 lands — this
packet's live-jar gate is the 0.1.10 candidate (97e44216…).
Installing it instead of BB-11 would also work mechanically
(it contains the BB-11 fixes), but the filed order is after.

Changes (design of record: board-r363 DESIGN-DRAFT.md, D64):
1. Raised mana holds (D64) in config.yml summon-reserve-percent:
   archer 20, bruiser 25, guardian 35, revenant 30, ghast 35,
   wolf 15, familiar 10, cat 10, mount 15; ceiling 75, floor 20,
   adopted 0 unchanged. The 18 Magic modifier templates MUST be
   replaced in the same window (packet modifiers file) — config
   and templates are one number, two readers.
2. Nameplate carries the owner: "Archer (MikeExcel) — 20/20".
   Exact-match owner-segment strip keeps re-renders idempotent
   and parenthesised pet names intact; unresolvable owner renders
   the old plate (never a UUID). Final-15s countdown segment.
3. Despawn legibility: DespawnNotice.Reason {EXPIRED, DISMISSED,
   DIED, UNLOADED}; T-30 (durations >= 150s) + T-15 (all timed)
   fade warnings, once per threshold, on the existing sweep tick;
   one finalizeRemoval funnel routes sweep expiry (EXPIRED),
   chunk-load + grace retire (UNLOADED), death (DIED) and
   dismissals (DISMISSED, aggregated to one summary per action).
   Mount toggle now sends the standard DISMISSED line (the old
   "drifts home" flavor line is retired per draft 4.3, veto
   point 3). Caster logout/death and shutdown stay silent by
   design (draft 4.1).
