# RIG RESULTS — ExcelShops 1.3.8 (console admin re-based on live 1.3.7)

Rig: local Paper 26.2-123 server — a scratch copy of the
1.3.4 rig runtime (Vault 2.0.0, EssentialsX 2.22.0,
GriefPrevention 16.18.7, ExcelMenu 0.1.0, and the RigCheck
harness plugin), with ExcelShops-1.3.8-rig.jar in place of
the 1.3.4 rig jar. Offline mode. THREE boots (logs:
`boot.log` = boot 1, `boot2.log`, `boot3.log` in this
folder; console commands: `cmdfile.txt`,
`cmdfile-boot2.txt`, `cmdfile-boot3.txt`). The scratch
runtime was deleted after evidence capture.

Rig copy disclosure (same as 1.3.2–1.3.5): the rig jar's
plugin.yml adds `Essentials` to softdepend so the Vault
provider registers first; **all 108 class files verified
byte-identical to the shipped 1.3.8 jar** — the plugin.yml
line is the only difference.

Every command was typed on the server's stdin — the REAL
console sender (`CONSOLE`). The rig's shops.dat carries 0
registered shops, so the status exercise covers the
no-shops and unresolvable-target paths live; the
with-shops formatting is harness-proven (see Limitations).

## Boot 1 — defaults (session proof OFF, as shipped)

Data-folder config carried no 1.3.7 keys, so jar defaults
applied: purchase-log ON, session-proof OFF,
no-activity-auto-recovery OFF.

- "Enabling ExcelShops v1.3.8"; data check
  `EXISTING_TARGET`; `ExcelShops purchases available.`;
  enable line `ExcelShops enabled. Legacy ChestShop
  confirmation: unavailable/absent. Economy: EssentialsX
  Economy` — exactly the expected shape.
- ExcelMenu: 16 menus live, `/shop -> spawnshop`
  registered. RigCheck: `CATALOG offers=18`, categories
  4/5/5/4/3, `MOBILE products=21 ordinary=18 specials=3`,
  `SPOT torch bundle=64 price=6000`, **`PARITY OK`**.
- The nine 1.3.5 admin/excelshop commands all replied
  verbatim as in the 1.3.5 rig (6-line no-shops status
  block, "Nothing was looked up." rejection, both usage
  lines, reload success + audit line with 18 offers /
  21 products, identical status block after reload, the
  "Use this in game…" console gate for `/excelshop`).
- The four new 1.3.7 stats commands: `excelshop stats abc`
  and `excelshop stats 99999` printed
  `Usage: /excelshop stats [days 1-3650]` (parse failure /
  out of range). `excelshop stats` and
  `excelshop stats 30` dispatched their async reads
  correctly — but their replies race the cmdfile's
  immediate `stop` (same second) and were dropped at
  shutdown. That observation gap is closed by boot 2,
  where the same two commands reply in full.
- SEVERE: 0. Java exit 0. Clean stop:
  "Disabling ExcelShops v1.3.8". (The log's ERROR lines
  are the sandbox's Mojang/Paper/Essentials update-fetch
  SSL failures plus Essentials' unsupported-version
  notice — environment noise, present in prior rigs; no
  ExcelShops error anywhere.)

## Boot 2 — session proof ON, first enable

Scratch config edited before boot:
`session-proof-enabled: true`,
`no-activity-auto-recovery: false`. All other state
inherited from boot 1's clean stop.

- Enable lines identical to boot 1, including
  `ExcelShops purchases available.` — no recovery line,
  no protection-only mode: with no prior envelope/fence
  and clean journals, the guard's decision is the silent
  CLEAN_START path, and `recordReady` armed the session
  (envelope lifecycle proven live in boot 3 below).
- **Stats replies captured verbatim** (feeder waited
  between commands):
  - `excelshop stats` → `Shop sales, last 7 days:
    0 purchases by 0 players, $0.00` +
    `Spawn shop $0.00 | player shops $0.00`
  - `excelshop stats 30` → `Shop sales, last 30 days:
    0 purchases by 0 players, $0.00` +
    `Spawn shop $0.00 | player shops $0.00`
  (empty purchases.csv — it does not exist until the
  first purchase; the read path handles that cleanly).
- `excelshopadmin reload` and the no-shops status block
  behaved exactly as in boot 1, under the armed guard.
- SEVERE: 0. Java exit 0. Clean stop.
- Post-boot data folder: `global-session-v2` envelope
  ABSENT and `global-session-v2.fence` ABSENT (removed
  by the guard's clean close), `purchases.csv.analytics.lock`
  present (0 B, created at first PurchaseLog init),
  `purchases.csv` still absent (no purchases — lazy
  creation), journals back to `transactions.log` only.

## Boot 3 — session proof ON, after a clean proof-on session

Same config and data folder as boot 2 left. Feeder held
the session open ~40 s after the reload so the guard's
on-disk state could be snapshot mid-flight.

- Enable lines identical; `ExcelShops purchases
  available.`; RigCheck `PARITY OK`; reload + status
  verbatim; SEVERE: 0; Java exit 0; clean stop.
- **Mid-session snapshot (server running, after
  enable + reload):**
  - `global-session-v2` PRESENT (471 B): header
    `ExcelShops global session 2`, a fresh session
    UUID, `READY`, the root binding, one fingerprint
    line per journal (`checkout` and `legacy-checkout`,
    each with entry count 0 and the SHA-256 fingerprints
    of the empty journals), and a trailing checksum
    line — the READY envelope exactly as reconstructed.
  - `global-session-v2.fence` ABSENT — correct: no
    mutation had occurred (the admin reload deliberately
    never calls the mutation fence; status is
    read-only). The fence is written by the first real
    mutation and, at the latest, by shutdown
    persistence, then removed by the clean close.
  - Sibling lock `.ExcelShops-global-session-v2.lock`
    present in the plugins folder (the guard's
    cross-process lock).
- Post-boot data folder: envelope and fence ABSENT
  again — the second consecutive clean close removed
  them; no `global-session-v2-audit` file was produced
  by any of the three boots.

**Session-proof verdict:** the reconstructed
GlobalSessionGuard lifecycle is rig-proven end to end —
arm on enable (READY envelope with both journal
fingerprints), silent CLEAN_START on a clean slate and
after a clean close, admin work unimpeded while armed,
and a clean shutdown that removes envelope + fence so
the next start is clean again. Proven twice in a row.

## Limitations, stated plainly

- **Crash paths not exercised:** no boot was killed
  mid-session, so `closeUnclean` / the UNCERTAIN fence
  and the `NO_OP_RECOVERY_ELIGIBLE` no-activity recovery
  decision were not rig-observed. They are pinned by
  the bytecode-fidelity work on GlobalSessionGuard
  (skeleton-equal to the live class) and the carried
  unit tests. A kill-and-reboot rig is the owed extra
  proof if the family ever wants session proof enabled
  live (it ships OFF by default; enabling it is a
  separate decision, not part of the 1.3.8 swap).
- **The mutation fence write was not observed** — it
  needs a real shop mutation (chest/sign placement,
  stock edit) and the rig registry holds 0 shops with
  no player driver. The fence call sites are in the
  0-javap-diff classes (WRVShops, FileJournal,
  ShopRegistry, CheckoutService).
- **Status for a player WITH shops was not
  rig-observed** (same as 1.3.5): with-shops formatting,
  offline-owner name resolution, and the pending-quote
  counts are pinned by AdminCommandTest (18 checks)
  against the real state shapes; the owed live proof is
  one console `excelshopadmin status <a real shop
  owner>` after install.
- **The non-admin denial was NOT exercised**: only the
  console and RigCheck senders exist here. The gate
  arithmetic and both permission nodes
  (`excelshops.admin`, `wrvshops.admin`, default op)
  are pinned by AdminCommandTest against the shipped
  plugin.yml.
- **The reload FAILURE path was not rig-forced** (same
  as 1.3.5): failure text harness-pinned, all-or-nothing
  swap code-reviewed.
- No Geyser/floodgate and no real client in the rig, so
  no player-facing flow was driven — by design: 1.3.8
  changes no player-facing path relative to live 1.3.7
  (see BUILD-RECORD-1.3.8.md class accounting).
