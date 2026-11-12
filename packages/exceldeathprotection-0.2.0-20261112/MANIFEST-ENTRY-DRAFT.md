# MANIFEST ENTRY DRAFT — ExcelDeathProtection 0.2.0 (for the board supervisor to file)

Drafted by the desk build lane (Nov 12, 2026). The supervisor
verifies and files this as a PENDING entry in
`~/workspace/wrv-install-queue/MANIFEST.md`. The build lane did NOT
edit the MANIFEST, the parent-backlog, or board state, and ran NO
live pulls — every live-state claim below is marked GATE-VERIFY
for the filing/install gates to check fresh.

---

## PENDING — ExcelDeathProtection 0.2.0 (premium banked-credit seam, decision D9: adds ExcelDeathProtectionAPI creditPremium(Player[,int]) for BossTrial's war-chest row, PDC banked-credit store with respawn auto-rearm, console credit/uncredit/status surface; all 0.1.3 two-tier purchase/journal/review behaviour preserved) — packet: this draft + ~/workspace/deathprotection-020-build-20261112/build-0.2.0/BUILD-RECORD.md

- **Packet:** build record
  `~/workspace/deathprotection-020-build-20261112/build-0.2.0/BUILD-RECORD.md`;
  design of record `~/workspace/deathprotection-premium-seam-20261110/DESIGN.md`
  + `BUILD-BRIEF.md`; decision D9 in the decision ledger;
  rig proof `~/workspace/deathprotection-020-build-20261112/build-0.2.0/rig-0.2.0/RIG-RESULTS.md`
  (Paper 26.3 desk rig, phase 1 14/14 + phase 2 3/3, SEVERE 0);
  unit tests 118/118 (`TEST-RESULTS.txt` in the build folder).
- **Base proof:** built on the recovered 0.1.3 source (wrv-plugins
  PR #13, commit 18dd40ae), 13/13 SOURCE-MANIFEST-0.1.3.json hashes
  verified on the copy before editing (BUILD-RECORD §1).
- **Artifacts (jar swap + config note, one window):**
  - `~/workspace/deathprotection-020-build-20261112/build-0.2.0/ExcelDeathProtection-0.2.0.jar` —
    **59,390 B** — SHA-256
    `ed819bb6547b5fd712e4b00d21d710bb0bae0885f23c69f9b53f493f6a230719`
    — build `EXC-DPROTECT-0.2.0-WRV-20261112-01-d2b348`
    (double-build byte-identical; fingerprint gate PASS).
  - **Config:** NO config swap required. The jar's bundled
    config.yml adds an optional `premium:` block whose defaults
    (enabled true, auto-rearm true, max-per-call 5, max-banked 0)
    apply when the live config lacks the block — a carried-over
    0.1.3 config runs the seam ON. GATE-VERIFY the live config
    carries `basic-price: 15000.0` / `premium-price: 30000.0`
    (the only two price keys; a legacy `price` key is ignored by
    design, as in 0.1.3). Optionally append the `premium:` block
    to the live config for explicitness, with an in-place .bak —
    not required for function.
  - **Data:** no data migration. Player PDC gains one key
    (`premium-credits`) written only when a credit is granted;
    existing `active`/`tier` data is read exactly as 0.1.3 wrote
    it. `purchases.journal` format unchanged.
- **Live-premise claims — ALL GATE-VERIFY (no live pull was run by
  the build lane):**
  - GATE-VERIFY: the live plugins folder holds
    `ExcelDeathProtection-0.1.3.jar`, **58,521 B**, SHA-256
    `f0224d18a51e430b81ec8fb02f9408fd6038e64b893d84d738368fd288cb4093`
    (the recovery lane's recorded live premise, Nov 12, 2026) —
    pull fresh, hash it, and STOP the window on any mismatch:
    0.2.0's base proof is that exact lineage.
  - GATE-VERIFY: live ExcelBossTrial is a build whose adapter
    probes `ExcelDeathProtectionAPI.creditPremium` (0.5.4/0.5.5
    line per the design) — after install, the first repeat /
    first-break verdict is the live seam proof (below).
  - GATE-VERIFY: live config prices (above) and an empty or
    reconciled `purchases.journal` unresolved set — 0.1.3 already
    locks purchases for unresolved entries; the swap changes
    nothing there, but the window should know the state it boots
    into.
  - GATE-VERIFY: no other live jar provides a class in package
    `com.winkreyes.exceldeathprotection` (single-jar swap; a
    duplicate would shadow the API class).
- **Marker path (hand window / failsafe per queue rules):**
  `~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt` — create
  at window start, delete at finish; re-check foreign markers
  before Shut Down and before Start.
- **Gates:** BACKUP FIRST + cap-at-6 (name prunees; never the Daily
  newest or the two newest manuals); emptiness `list` = 0 twice.
- **Swap:** remove `ExcelDeathProtection-0.1.3.jar`, add
  `ExcelDeathProtection-0.2.0.jar`. Keep the live
  `plugins/ExcelDeathProtection/` data folder (config +
  purchases.journal) in place.
- **Boot-verify lines (all must appear):**
  - `Enabling ExcelDeathProtection v0.2.0`;
  - `ExcelDeathProtection 0.2.0: basic $15,000.00, premium $30,000.00 (build EXC-DPROTECT-0.2.0-WRV-20261112-01-d2b348)`;
  - `ExcelDeathProtection API: premium-credit seam active (creditPremium)`;
  - SEVERE 0 attributable;
  - console `dp version` answers with build
    `EXC-DPROTECT-0.2.0-WRV-20261112-01-d2b348`;
  - console seam smoke (safe, reversible): `dp credit <online tester> 1`
    logs `Premium credit: … source=admin`; `dp status <tester>`
    shows the tier/bank; `dp uncredit <tester> all` restores the
    tester if the credit was only a smoke test.
- **Same-pass surfaces (install window, per house rule):**
  - **Guide:** knowledge + intent for banked premium credits
    (what a credit is, auto-rearm, `/dp status`) — text live-edit
    rules apply to the text placement; the jar install stays gated.
    Player surfaces must not promise the premium row before this
    install lands (backlog filing rule).
  - **What's New BOTH surfaces:** website section + in-game
    ExcelWhatsNew changelog entry (boss-reward premium credits
    now bank and re-arm; `/dp status` shows the bank).
  - **Tips check:** broadcaster tip candidate — "Boss rewards can
    bank a Death Protection premium credit — check yours with
    /dp status." (file or explicitly skip with the reason).
  - **Menus check:** no menu change in the plugin itself; verify
    the existing Spawn Shop Death Protection entries (slots/prices)
    still route to the two-tier buy commands after the swap.
- **Player proof owed after install (the deciding evidence):**
  the first real BossTrial repeat/first-break verdict — server
  log shows DP `Premium credit:` lines and does NOT show
  `ExcelDeathProtection exposes no premium-credit API`; the
  champion's `/dp status` shows active/banked as expected; one
  Java and one Bedrock read of the status + re-arm icon; a
  banked-credit death/re-arm cycle in real play.
- **Rollback:** swap the 0.1.3 jar back (58,521 B /
  `f0224d18…4093`), restart. Player data note: 0.1.3 ignores the
  `premium-credits` PDC key (it neither reads nor removes it), so
  banked credits written under 0.2.0 sit dormant — not lost — and
  become live again on any later 0.2.0 reinstall; the
  `active`/`tier` state is fully shared both ways.
- **Records:** package staged at
  `~/workspace/records-catchup-20261103/packages/exceldeathprotection-0.2.0-20261112/`
  (publication claim line belongs to the supervisor's close-out —
  the build lane published nothing).
