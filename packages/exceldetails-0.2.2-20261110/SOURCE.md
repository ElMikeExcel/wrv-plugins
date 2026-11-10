# ExcelDetails 0.2.2 — SOURCE

Excel — Wink Reyes Valley. Authors: Iris / Mike.

- **What:** 0.2.1's party-scoped checkbox-mix combat meter, plus
  (Mike's commission, Nov 9, 2026 — "Details should scale. And
  it should be prepared for elite mobs world"):
  (1) BOSS SCALING — damage done to a seal boss (wither, elder
  guardian, warden, ender dragon: BossTrial's own kindOf type
  set) renders in ExcelBossTrial's pop-up space: the MONITOR
  final damage is divided by BossTrial's live intake factor
  and multiplied by BossTrial's live `feedback.display-scale`
  (read from BossTrial's own config at runtime; intake read
  from its live encounter state by a soft reflection seam;
  both fail soft — BossTrial absent = no scaling, seam unbound
  = f 1). Damage taken and healing never scale. Pure math:
  `BossScale`; seam: `BossScaleLookup`. (2) ELITEMOBS PREP —
  damage done to an EliteMobs-tracked elite, and any damage
  event beyond every real HP pool on the server (>100,000),
  routes to a separate EM bucket inside the segment: tracked
  per player, visible on /details status, never ranked, never
  rendered, never merged into overall totals. Pure routing:
  `EmCombat`; detection seam (EntityTracker.isEliteMob,
  reflection, fail-soft): `EliteLookup`. Everything else is
  0.2.1, behavior-identical: checkbox mix, party scoping,
  persistence (data.yml format untouched — the EM bucket is
  session-scoped), churn fix, placeholders, piggyback
  rendering. Full detail:
  `~/workspace/details-meter-20261109/build-0.2.2/DESIGN.md` and
  `BUILD-RECORD.md`.
- **Source:** this folder (`src/com/winkreyes/exceldetails/`),
  built by `local-build.sh` (deterministic double-build, JUnit
  first, fingerprint gate). Base: the shipped 0.2.1 tree, copied
  unchanged except for the 0.2.2 deltas; the unmodified copy was
  rebuilt first and reproduced the shipped 0.2.1 jar
  byte-for-byte (sha256 7c5d56f3…) before any edit was made.
- **Candidate jar:** `ExcelDetails-0.2.2.jar`, sha256 recorded in
  `~/workspace/details-meter-20261109/build-0.2.2/BUILD-RECORD.md`.
- **Build ID:** EXC-DETAILS-0.2.2-WRV-20261110-01-b22e10
  (FINGERPRINT-STANDARD Layer B, verified in-jar by the build).
- **Status:** BUILT at desk, NOT installed. Install packet:
  `~/workspace/details-meter-20261109/build-0.2.2/INSTALL-PACKET.md`.
