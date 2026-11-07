# BUILD-RECORD — ExcelAutoSort 0.2.0 "Max Organization"

Built Nov 7, 2026 (Iris, subagent session for the Minecraft side
chat), commissioned by Mike in chat Sat Nov 7, 2026, 7:48 AM CST
("Commission the ExcelAutoSort 0.2.0 build"), to `SPEC.md` §15 as
amended. **Not installed.** Filed PENDING in
`~/workspace/wrv-install-queue/MANIFEST.md`; the swap packet is
`INSTALL-PACKET.md` in this folder. Records staged in
`PUSH-NOTES.md` for the parent/board-manager (nothing pushed from
this session). The 0.1.0 build record is preserved as
`BUILD-RECORD-0.1.0.md`; 0.1.0 shipped and is LIVE (installed
Nov 7, 2:05–2:25 AM CST) — every 0.1.0 behavior is carried into
0.2.0 unchanged and re-proven by the rig's regression phases.

## What shipped (SPEC §15, settled rulings — none re-opened)

- **Entitlement.** Per-player Max Organization access in
  `plugins/ExcelAutoSort/data/maxorg.yml` (`owned` flag /
  `rent-until` epoch-ms), persisted through the 0.1.0 DataStore
  discipline (dirty flags, 30 s autosave, save-on-disable, atomic
  replace, build-header stamp, fail-soft load). Access derives
  from one clock read; owned beats any rental timestamp forever.
- **Pricing, exactly as ruled.** Rent **$1,000 / 30 days**, buy
  **$15,000 permanent**, through Vault (the house economy seam —
  `hook/EconomyPort.java` JDK seam + `economy/VaultGateway.java`,
  provider resolved LAZILY and retried on use: plugin enable order
  is not a contract, and a purchase attempted in a provider gap
  refuses cleanly with "the server economy isn't answering").
  **No rent credit toward buy** — and the unlock screen, the
  confirm screen, and the text path all say so in plain words
  before any charge. Confirm-before-charge on every surface
  (Java confirm screen, Bedrock modal, `/autosort max rent|buy`
  prints the confirm wording first and charges only with
  `--confirm`). One charge path (`MaxOrgService.purchase`):
  re-checks access, funds, and double-charge state at charge time;
  withdraws first, applies state second, re-deposits on any save
  failure. Insufficient funds = no charge, no state change, one
  plain line. Every purchase Protect-logged (kind, amount,
  expiry-after, `via: max-org`).
- **The network view.** `/autosort max` (menu entry too) opens a
  paged 54-slot window (45 entries/page) onto every chest/barrel
  within **radius 16** of where the player stands whose canonical
  record is not claim-suspended and where the player holds
  **CONFIGURE rights under the exact §6.4 rule** (the same
  `FilterEngine.canConfigure` the 0.1.0 rights layer uses —
  ContainerTrust/AccessTrust alone never suffices). The view holds
  nothing: entries are slot references re-rendered from the live
  inventories at click time. **Takes** move a whole stack (or what
  fits — the rest stays in the chest), re-verifying per move:
  rights, suspension, and radius re-checked from the player's
  CURRENT position, live inventory re-read, removal verified
  before crediting hands, credit verified with rollback to the
  source chest on a shortfall. **Smart deposit** (button + network
  GUI path) plans per destination filter: whitelist chests receive
  only listed items and are PREFERRED for them; blacklist chests
  refuse listed items; unmarked chests accept all; destinations
  walked in network order; execution is simulation-first and
  per-move re-verified; unplaceable items stay in the player's
  hands and are reported, never voided. **Every take and deposit
  Protect-logged** (`autosort_network_take` /
  `autosort_network_deposit`, `via: network-view`, distinguishable
  from 0.1.0's filter causes).
- **Lapse.** A lapsed rental refuses the view with the expiry
  line; zero item movement, zero charges, no auto-charge anywhere
  in the design. Re-rent restores instantly.
- **Surfaces.** Java inventory GUIs (unlock / confirm / network),
  Bedrock Cumulus forms on the same service (SimpleForm unlock,
  ModalForm purchase confirms, SimpleForm network with text entry
  labels, tap-only throughout — no typed step anywhere), the
  `/autosort max [status|rent|buy]` text path, the ExcelMenu entry
  (`packet/maxorg.yml`, Settings group 20/210, state-driven label
  from `%autosort_maxorg%` → OWNED / RENTED / OFF — the menu
  toggle standard: the display shows the CURRENT state), and
  `/autosort status` gains the entitlement line.
- **Config-first.** The whole feature lives under `max-org:` in
  `config.yml` — `enabled` (kill-switch, see decisions), prices,
  rent days, radius, page size — and every player-facing sentence
  is a `messages:` key (`maxorg-*`, `gui-maxorg-*`,
  `gui-network-title`, `status-maxorg`). Defaults in code equal
  the config values, so a missing key degrades to the settled
  behavior, never to a crash.

## Implementer's readings (documented, consistent with §15's intent)

1. **Rental renewal (§15.2).** Renting while a rental is ACTIVE
   extends from the current expiry; renting after a lapse starts
   from now. (Spec: "re-rent restores instantly" — silent on
   stacking.) Unit-pinned.
2. **Radius measurement (§15.3).** Measured from the center of
   the block the player stands in to the center of the chest
   block — NOT from the raw feet point. The rig's boundary case
   convicted the feet-point reading: a chest on the same floor
   exactly 16 blocks away sits at distance² 256.25 from the feet
   point (the +0.5 vertical to the chest's center), so the
   feature's real reach would have been 15 on flat ground while
   the config says 16. Block-center to block-center is the
   distance players mean; the boundary rig case now proves 16 IN
   / 17 OUT on the same floor. Per-move re-verification uses the
   same measure from the player's current position (walking away
   ends moves for far chests — the view is "from where they
   stand").
3. **Kill-switch (§15.4).** §15.4 names the config knobs; an
   `enabled` switch was added alongside them (default on) so
   staff can retire the feature without a jar swap — the same
   shape as 0.1.0's `hopper-filtering` kill-switch Mike approved.
   Off = unlock path refuses plainly; existing entitlements are
   kept on disk, untouched.

## Defects found and fixed in-build

- **Radius half-block shrink** (above) — found by the acceptance
  rig's boundary case (run9: the dx-16 chest missing from the
  view), fixed in `MaxOrgService` (block-center origin for both
  network resolution and per-move re-verification), re-proven in
  runs 10/13. A real player-facing defect caught before install —
  the 0.2.0 counterpart of 0.1.0's double-chest keying catch.
- **Network view had no route back to the unlock screen**, so a
  rented player could never reach Buy from the view (found in
  code review during the build). Fixed in-build: slot-46 "Max
  Organization" button in the Java network view + an unlock
  button in the Bedrock network form; the rent|buy text path
  also remains.
- **Economy bind timing.** The first build resolved the Vault
  provider once at enable; enable order isn't a contract, so the
  gateway now resolves lazily and retries per use, refusing
  purchases cleanly while unbound (the rig's phases 1–3 sandbox
  boots with `economy UNBOUND` and the full 0.1.0 set unaffected).
- Rig-side findings (NOT product defects, recorded so the run
  history reads honestly): run9's deposit case initially named
  the wrong expected chest (the planner correctly chose another
  unmarked chest — assertions now check the unmarked set +
  filtered-chest exclusion); two log-line verdicts raced
  Protect's asynchronous items-writer flush (the lines were in
  the evidence file with correct timestamps both times — read
  delays lengthened); the clientless-player disconnect needed
  the player-list removal a real disconnect ends in (fires
  PlayerQuitEvent, which is what drops the session in
  production); run12 died on a port collision with an orphaned
  rig server from an interrupted run11 (infrastructure, rerun
  clean as run13).

## Tests

- **Unit: 29/29** (`tests/.../AutoSortTest.java`; 19 carried
  0.1.0 tests + 10 new: entitlement access states, rent/renewal/
  buy math incl. no-proration, exact radius boundary, view
  flatten + full-stack fit, deposit preference order + unplaced
  remainder + partial top-up accounting, Protect record shapes
  for all three new types, money formatting, max-org config
  defaults + message-key presence). Fresh confirmation run at
  close-out: 29 successful, 0 failed.
- **Acceptance rig: 76/76** (`rig/drive.py` run13, Nov 7, 2026;
  Paper 26.3-231, real GriefPrevention 16.18.7, ExcelProtect
  0.1.7, Vault 2.0.0 + EssentialsX economy, fake players on real
  NMS connections). Phases 1–3 = the full 0.1.0 set against the
  0.2.0 jar (50 checks, incl. the live awake line + Protect
  stage-file verdicts). Phases 4–5 = 25 MaxOrg M-cases + boots:
  economy bound; insufficient funds charges nothing (balance,
  access, and log all verified); text-path rent; confirm screen
  before any charge; unlock screen is the no-access landing;
  rent charges exactly once + persists + logs + opens the view;
  **radius/rights boundary** (16 in / 17 out; claim owner's chest
  in; ContainerTrust-only OUT; PermissionTrust in); take moves +
  logs; partial take leaves the remainder; smart deposit
  (whitelist preference, blacklist refusal, unmarked totals,
  hands empty, 3 log lines); buy after rent charges the FULL
  15,000 (no proration) + logs; second buy tap charges nothing;
  full destination keeps every item; view of only-the-full-chest;
  take into a full inventory moves nothing; disconnect mid-view
  conserves every chest exactly; **crash-stop conservation**
  (phase-5 boot totals equal phase-4's recorded totals to the
  item); **buy permanence across restart** (entitlements 3 on the
  phase-5 awake line, economy BOUND); lapse refuses the view;
  re-rent restores instantly with both rent purchases in the
  Protect stream. Run history: run8 (phases 1–3 clean; phase 4
  prep path error), run9 72/76 (radius defect + 3 rig-side),
  run10 74/76 (2 flush races), run12 (port collision, void),
  run13 **76/76**.
- **Deterministic double-build: byte-identical**, reproduced
  again at close-out with the same hash.

## Artifacts

- `ExcelAutoSort-0.2.0.jar` — 154,773 B — SHA-256
  `6164d943cfaeef498e8966978d9c3255d2352ab81ccc0b36875064d4d0013138`
  (+ `-repro.jar`, identical)
- `ExcelAutoSort-0.2.0-source.zip` — 89,449 B — SHA-256
  `bfdd503a8eaef6ac3fb6b1c55e5d17bb882e7724e06cee0114c8a5a3f405088a`
  (40 files: src/ + tests/ + plugin.yml + config.yml +
  local-build.sh; timestamps pinned 2026-11-07)
- Build ID `EXC-AUTOSORT-0.2.0-WRV-20261107-01-78a48a`;
  fingerprint gate PASS; semantic markers carried forward +
  MaxOrg markers added (FINGERPRINTS.md).
- 0.1.0 artifacts untouched (`ExcelAutoSort-0.1.0.jar`,
  `-repro.jar`, `-source.zip`, `INSTALLED.txt` verified present
  at close-out).

## Named post-install player proofs (NOT rig-claimed)

- **Bedrock Max Organization tap flow end-to-end (Mike):** menu
  entry → unlock form → rent via the purchase modal (charge lands
  exactly once) → network form → take a stack → smart deposit →
  menu label shows RENTED. The Bedrock layer is built on the same
  service the rig proved through the Java surfaces; the live
  Geyser/Cumulus pass is this proof (same posture as 0.1.0).
- **Java GUI flow (any family player):** unlock → confirm → view
  → take → smart deposit, incl. the network view's Max
  Organization button returning to the unlock screen.
- **One real rent + one real buy** on the live economy, verified
  against Protect's `autosort_maxorg_purchase` lines.
- **Live lapse observation:** a rental expiring on a real account
  (or staff-aged) refuses the view plainly, zero item movement.

## Open questions for Mike

None. The two §15 readings (renewal stacking, radius
measurement) and the kill-switch addition are documented above
as implementer's readings, not questions — each follows the
spec's stated intent, and the radius reading is the one the
rig's boundary evidence supports.
