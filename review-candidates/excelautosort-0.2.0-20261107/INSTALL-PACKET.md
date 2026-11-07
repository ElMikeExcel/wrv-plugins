# INSTALL PACKET — ExcelAutoSort 0.2.0 "Max Organization"

**Status: STAGED, NOT INSTALLED.** Filed as PENDING in
`~/workspace/wrv-install-queue/MANIFEST.md`. Executor: the install
failsafe / a hand window under the standing gates. Build record:
`~/workspace/autosort-plugin/BUILD-RECORD.md`. Spec:
`~/workspace/autosort-plugin/SPEC.md` (§15 is the design of record).

(The 0.1.0 packet is preserved as `INSTALL-PACKET-0.1.0.md`.)

## What this swap does

A 1-for-1 version increment of the live ExcelAutoSort: 0.1.0 out
(held aside), 0.2.0 in. Everything 0.1.0 does is unchanged (sort
parity engine, per-chest whitelist/blacklist filters, recovery
buffer, the §6.4 configure-rights rule). 0.2.0 adds **Max
Organization** (SPEC §15): rent $1,000/30 days or buy $15,000
permanent to open a paged window onto every chest in radius 16 the
player holds configure rights over — takes and filter-respecting
smart deposits, every move Protect-logged. Entitlements persist in
`plugins/ExcelAutoSort/data/maxorg.yml` (new file; created on first
boot if absent).

Expected plugin count after the swap: **76 — UNCHANGED** (1-for-1).

## Artifacts

- `~/workspace/autosort-plugin/ExcelAutoSort-0.2.0.jar` — 154,773 B,
  SHA-256 `6164d943cfaeef498e8966978d9c3255d2352ab81ccc0b36875064d4d0013138`
  (deterministic double-build byte-identical; fingerprint gate PASS;
  build ID `EXC-AUTOSORT-0.2.0-WRV-20261107-01-78a48a`).
- Menu entry file: `~/workspace/autosort-plugin/packet/maxorg.yml`
  (NEW; Settings group, order 20 / bedrock 210, state-driven on
  `%autosort_maxorg%`).
- Config: the jar's bundled `config.yml` carries the new `max-org:`
  section + messages. **The live config must be MERGED, not
  replaced** (live values win): add the `max-org:` section and the
  `maxorg-*` / `gui-maxorg-*` / `gui-network-title` / `status-maxorg`
  message keys from the bundled file; change nothing else. If the
  merge is skipped the plugin still runs on config defaults for the
  new keys (defaults = the settled values), but the merge is the
  packet step — do it.

## Steps

1. **Gates first** (below). Do not start the swap without them.
2. Rename the live jar aside in place:
   `ExcelAutoSort-0.1.0.jar` → append `.held-20261107-pre020`
   (identify it by the `/plugins` list name `ExcelAutoSort`, version
   0.1.0 — not by guessing).
3. Place `ExcelAutoSort-0.2.0.jar` in `plugins/`. Verify its panel
   Compute checksum equals the SHA-256 above before booting.
4. Merge the live `plugins/ExcelAutoSort/config.yml` per above
   (in-place backup first: `config.yml.bak-20261107-pre020`).
   Data folder otherwise untouched: `players.yml`, `chests.yml`,
   `buffers.yml` carry over as-is; `maxorg.yml` is new.
5. Menu registry: add `packet/maxorg.yml` to the ExcelMenu entries
   directory (new file; no existing entry changes — `autosort.yml`,
   `chestfilter.yml`, `filterrecover.yml` stay byte-identical).
6. Boot-verify lines (all must appear):
   - the awake line naming version 0.2.0 + build
     `EXC-AUTOSORT-0.2.0-WRV-20261107-01-78a48a`, with
     `protect sink BOUND`, `gp rights BOUND`, and the new segment
     `max-org on (economy BOUND, entitlements N)` — economy MUST
     read BOUND (Vault + EssentialsX economy are live); if it reads
     UNBOUND, stop and report (purchases would refuse — fail-soft,
     but not the intended state);
   - `ExcelProtect audit sink BOUND (stream: items)`;
   - plugin count 76; SEVERE 0 attributable to ExcelAutoSort;
   - guild seal state unchanged: **LOCKED 3 of 5**.
7. Console smoke (no charge): `/autosort status` from console
   prints version 0.2.0 + the build ID. Do NOT run a real purchase
   from any account as a smoke test — the first real rent/buy is a
   player proof, below.
8. Same-pass surfaces in the install window (standing rule): Guide
   knowledge + intents for Max Organization (unlock, rent/buy,
   lapse, the view, smart deposit), the website pair, What's New
   (website section AND the in-game ExcelWhatsNew changelog), and a
   broadcaster-tips check. Player-facing canon: rent $1,000 per
   30 days / buy $15,000 forever; rent paid never counts toward
   buying; the view holds nothing — items live in the real chests.

## Gates (standing house discipline)

- **BACKUP-FIRST:** fresh Shockbyte backup before the swap;
  **cap-at-6** — prune the oldest manual backups first so the total
  never exceeds 6; never delete the Daily Backup's newest copy or
  the two newest manual restore points; name prunees in the install
  report.
- **Emptiness:** verified empty (`list` = 0 twice) before shutdown,
  or the maintenance window; panel marker discipline — create
  `~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt` for a hand
  window, re-check foreign markers before Shut Down and before
  Start (coordination abort on a foreign marker < 3h old).
- **Seal note:** this packet touches **neither BossTrial nor
  GuildGate jars**, and no guild content — chapter acceptance does
  NOT re-open. The seal line in boot-verify is a state check only
  (LOCKED 3 of 5 expected).

## Rollback

Remove `ExcelAutoSort-0.2.0.jar`; restore the held 0.1.0 jar to its
original name; restore `config.yml.bak-20261107-pre020`; delete the
`maxorg.yml` menu entry file (`maxorg.yml` in the entries
directory); restart. Leave `plugins/ExcelAutoSort/data/maxorg.yml`
in place — it is inert under 0.1.0 (unknown file, ignored) and is
the only record of any entitlements bought since the swap; note
its presence in the rollback report. Any player who rented/bought
under 0.2.0 keeps the money-side fact in Protect's stream
(`autosort_maxorg_purchase`); refunds are a manual staff economy
action (SPEC §15.2), never automatic.

## Player proofs owed after install (not install gates)

- **Bedrock (Mike):** the full Max Organization flow tap-only —
  menu entry → unlock screen → rent confirm (charge lands once) →
  network view → take a stack → smart deposit → the state label on
  the menu entry flips to RENTED. (The Bedrock form flow is built
  on the same service as the rig-proven Java flow; the live-client
  pass is this proof — same posture as 0.1.0's Bedrock selector
  proof.)
- **Java (any family player):** the same flow via the inventory
  GUI, incl. the network view's Max Organization button returning
  to the unlock screen.
- One real rent and one real buy on the live economy, verified
  against Protect's `autosort_maxorg_purchase` lines.
- A lapsed rental on a live account (or a staff-aged expiry)
  showing the view refused plainly with zero item movement.
