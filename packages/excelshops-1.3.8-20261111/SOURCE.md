# ExcelShops 1.3.8 — SOURCE record

Excel — Wink Reyes Valley. Authors: Iris / Mike. November 11, 2026.
Commission: the console-admin coverage audit (Nov 9, 2026,
`~/workspace/console-admin-audit-20261109/AUDIT.md` §4) under
Mike's Nov 3, 2026 console-admin rule. Full build record:
`BUILD-RECORD.md` in this package (build folder of record:
`~/workspace/shops138-rebase-20261111/`).

## Provenance — read the caveat first

- **No original 1.3.6/1.3.7 source exists on disk** (board
  supervisor run-183 source-recovery sweep, Nov 11, 2026, 11
  locations enumerated, coverage in
  `hidden_files/shops137-source-sweep-20261111.md`). The 1.3.7
  base of this build was **reconstructed from the live jar**:
  the live `ExcelShops-1.3.7-analytics-OFFLINE.jar` (252,335 B /
  SHA-256 12cf683a…) was pulled through the PC rclone route
  Nov 11, 2026 (see `PULL-RECORD.md` in this package) and
  decompiled (Vineflower), diffed against the 1.3.4 decompile
  so decompiler artifacts cancel.
- Fidelity vs the live jar (build record §3): 70/99 classes
  byte-identical; 18 instruction-identical (0 raw javap diff
  lines); 11 skeleton-equal (same ops/constants/calls per
  method; slot/branch/exception-table layout residuals only);
  all resources byte-identical; reconstructed base passes
  412/412 carried tests. This is a stronger-tested base than a
  blind decompile, but it is NOT the original source.
- Project tree: `~/workspace/shops138-rebase-20261111/tree-1.3.8/`
  (plugin.yml version 1.3.8, main com.winkreyes.shops.WRVShops).
  The reconstructed 1.3.7 tree is kept beside it as `tree-1.3.7/`.
- Built with the fleet harness (`build.sh` in the build folder;
  JDK 25 at `~/workspace/.jdk25`, `--release 21`, Paper-API +
  Vault + kyori 4.20 classpath — the same harness as 1.3.4/1.3.5)
  and packaged by `package-1.3.8.py` (fixed zip stamp
  2026-11-11 00:00; manifest bytes identical to the live 1.3.7
  jar's).
- Ownership: the console-admin delta is ours (Iris / Mike
  lineage); the carried base is the reconstructed live 1.3.7
  behaviour, unchanged except as listed below. No new runtime
  dependencies.

## What 1.3.8 changes vs live 1.3.7 (the whole delta)

CONSOLE ADMIN, re-based: the 1.3.5 console-admin delta (which
was HELD — its 1.3.4 premise was stale) re-based onto the
reconstructed live 1.3.7 tree.

- ONE separate console-safe command: `/excelshopadmin status
  <player>` (read-only: shops owned with every registered
  offer + price, the player's Vault balance, and the
  checkout's pending state — recovery flag + live-quote
  count) and `/excelshopadmin reload` (config + spawn catalog
  + mobile catalog, all-or-nothing; registry and checkout
  deliberately NOT reloaded), gated by the `excelshops.admin`
  node (default op; checked in code). Target resolution:
  online exact name, then raw UUID, then the registry's own
  owner-name records.
- Jar class delta vs the reconstructed 1.3.7: among the 99
  carried classes only `WRVShops` and `CheckoutService`
  differ; exactly 9 new classes (AdminCommand + 5 nested,
  ShopAdminCommand + 2 nested); 108 classes total.
- Everything else is live 1.3.7 behaviour carried intact
  (purchase analytics + `/excelshop stats`, session-proof
  code — shipped default OFF and the live config is NOT
  touched by the swap — mutation fences). No config edit, no
  data migration; player-facing change relative to live: none.
- Deliberately NO money-moving, grant, revoke, delete, or
  edit op (money-adjacent plugin, per the audit's sizing).

## Artifacts

- ExcelShops-1.3.8.jar — 266,417 bytes
  SHA-256 b8a1b8fd9a56f420accdb066a3ae3e9c6989638668cadd2f5497a52117a4adb9
  Deterministic: two independent builds package byte-identical;
  re-proven by re-packaging both surviving class trees to the
  same hash.
- ExcelShops-1.3.8-source.zip — 186,824 bytes
  SHA-256 78217dadf450dad9b4c4778b01bf23ebe963687a93cfca2f145dfe76e1613c76
  (74 entries: the tree-1.3.8 project under an `ExcelShops/`
  prefix, fixed timestamps.)

## Verification summary

Desk 430/430 on two independent builds (412 carried + 18
AdminCommand); rig (Paper 26.2-123 + Vault + EssentialsX +
GriefPrevention + ExcelMenu 0.1.0 + RigCheck): three clean
boots, SEVERE 0, RigCheck PARITY OK every boot, console
commands typed on the real console stdin, two-boot
session-proof lifecycle exercised (READY envelope observed
mid-session; clean close removes envelope + fence). Stated
limitations (full text in `RIG-RESULTS.md`): with-shops
status, owner-name resolution, and the non-admin denial are
harness-pinned, not rig-observed — the MANIFEST entry's
console smoke is their live proof; crash / no-op-recovery
session-proof paths not rig-forced (guard ships OFF).
NOT installed at staging — built + filed PENDING in the
MANIFEST Nov 11, 2026 (board supervisor run 194); the
failsafe lane owns the install under full gates.
