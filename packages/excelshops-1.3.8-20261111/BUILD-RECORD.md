# BUILD RECORD — ExcelShops 1.3.8 (console admin re-based on live 1.3.7)

Desk build, board-manager run 190, Nov 11, 2026. Desk side
only: no panel work, no install, no MANIFEST edit, no
GitHub/Drive publishing was done or attempted.

**Deliverable:** `ExcelShops-1.3.8.jar` — **266,417 B** —
SHA-256
`b8a1b8fd9a56f420accdb066a3ae3e9c6989638668cadd2f5497a52117a4adb9`
— 108 classes. Deterministic (see §5). Tests 430/430 on
two independent builds. Rig: 3 clean boots, SEVERE 0
(see §6 and `rig-1.3.8/RIG-RESULTS-1.3.8.md`).

**What 1.3.8 is:** the read-only console-admin delta
built for the HELD 1.3.5 packet (`/excelshopadmin status`
+ `/excelshopadmin reload`, gate `excelshops.admin`),
re-based onto the TRUE live base — the
1.3.7-analytics-OFFLINE jar that is actually running —
instead of 1.3.5's stale 1.3.4 base. It supersedes the
HELD 1.3.5 packet, which must not be installed. Version
1.3.8 because 1.3.5 is burned by the earlier held build
and 1.3.6/1.3.7 are the live lineage's own numbers.

## 1. Inputs

- **Live jar (the base):**
  `ExcelShops-1.3.7-analytics-OFFLINE.jar` — 252,335 B —
  SHA-256
  `12cf683a3b3125f4d1376ebe180699770ed630fb392a4b2c43f956a06a29988f`,
  pulled Nov 11, 2026, 09:22 CST via rclone through
  Mike's PC (`PULL-RECORD.md` in this folder).
  plugin.yml: version `1.3.7-analytics-OFFLINE`, main
  `com.winkreyes.shops.WRVShops`.
- **The delta:** the HELD 1.3.5 build at
  `~/workspace/shops-135-build/` (jar, source zip,
  BUILD-RECORD.md, INSTALL-PACKET.md,
  MANIFEST-ENTRY-DRAFT.md). Its delta vs its own base
  was identified from its source tree and build record
  and re-applied unchanged in logic (§4).
- **Base of record:** tree-1.3.4 at
  `~/workspace/excelshops/build-1.3.4/tree-1.3.4/`
  (the shipped 1.3.4's source; shipped jar 218,618 B /
  SHA-256 `8c38f67e…f9005`).
- **Why reconstruction was necessary:** the prior source
  sweep (board run 183,
  `~/workspace/goals/family-minecraft-server-build/hidden_files/shops137-source-sweep-20261111.md`)
  enumerated 11 locations and found the
  1.3.5-recurrence / 1.3.6 / 1.3.7 source **NOT FOUND**
  anywhere on disk. The live jar is the only surviving
  copy of the 1.3.7 code.

## 2. Tree reconstruction (tree-1.3.7/)

- Decompiler: **Vineflower 1.11.1**, downloaded from
  Maven Central (1,847,869 B; MD5
  `2085a89b817db0168d97f656b9b549fa`, matching Maven's
  published checksum). No decompiler was installed
  locally (only `javap`).
- Method: decompile BOTH the live 1.3.7 jar and the
  shipped 1.3.4 jar, then diff the two decompilations.
  Decompiler artifacts appear on both sides and cancel;
  what remains is the true 1.3.4 → 1.3.7 delta:
  **6 changed source files** (`LegacyShopBridge`,
  `ShopUi`, `WRVShops`, `checkout/CheckoutService`,
  `checkout/FileJournal`, `store/ShopRegistry`),
  **3 new files** (`checkout/GlobalSessionGuard`,
  `log/PurchaseLog`, `log/Platform`), everything else
  identical. Class inventory agrees: live jar has 99
  classes = 74 byte-identical to 1.3.4 + 13 differing +
  12 new.
- tree-1.3.7 assembly: the 24 unchanged source files
  copied **verbatim** from tree-1.3.4; the 6 changed
  files produced by applying the diffed delta onto the
  original dense-style sources by hand; the 3 new files
  transcribed from the decompilation with meaningful
  variable names. Resources: `plugin.yml` and
  `config.yml` taken from the live jar;
  `spawn-catalog.yml` and `mobile-menus/` verified
  byte-identical between the live jar and 1.3.4.
- One Vineflower failure marker exists, in
  `JavaNumberDialogs` — a class byte-identical to
  1.3.4, so the original source covers it. No impact.

### What 1.3.7 itself added over 1.3.4 (the reconstructed delta)

- **Purchase analytics:** `PurchaseLog` appends every
  completed purchase to `purchases.csv` (guarded by
  `purchases.csv.analytics.lock`); `/excelshop stats
  [days]` (permission `wrvshops.admin`, default op;
  default 7 days, range 1–3650; read runs async and
  replies on the main thread) with a spawn-vs-player
  revenue split and a top-items list. Config:
  `purchase-log: true`.
- **Session proof (ships OFF):** `GlobalSessionGuard`
  writes a READY envelope (`global-session-v2`, with
  per-journal fingerprints from
  `FileJournal.proof()`), a mutation fence
  (`global-session-v2.fence`), and clean-close removal
  of both; `markerOnlyRecovery()` /
  `acceptNoActivityRecovery()` support the
  no-activity recovery decision. Config:
  `session-proof-enabled: false`,
  `no-activity-auto-recovery: false`.
- **Mutation fences:** `WRVShops.beforeShopMutation` /
  `fenceEvent` on chest/sign place+break, stock
  exposure and owner stock edits;
  `ShopRegistry.MutationGuard`;
  `FileJournal.beforeExternalAction` / `markUncertain`;
  CheckoutService external-lease / prepare fences;
  `LegacyShopBridge` now takes a shared `FileJournal`
  (4-arg constructor) so legacy trades sit under the
  same journal discipline.

## 3. Fidelity of the reconstruction (the honest accounting)

There is no original 1.3.7 source; fidelity is therefore
proven against the live jar's bytecode, in tiers:

- **Resources:** plugin.yml, config.yml,
  spawn-catalog.yml, mobile-menus — byte-identical to
  the live jar.
- **Tests:** the carried harness passes **412/412** on
  the reconstructed tree.
- **Classes (99): 70 byte-identical** to the live jar.
  The other **29 are fully accounted for**:
  - **18 classes — instruction-identical.** From the 6
    hand-edited files: LegacyShopBridge; ShopUi (+$1,
    $Draft, $MobileOrigin, $Page, $ViewKind); WRVShops
    (+$PendingSign); CheckoutService (+$Journal,
    $ExternalLease); FileJournal (+$1, $2);
    ShopRegistry (+$ClaimPair, $SignBackup). For each,
    `javap -c -p` output vs the live class has **0 raw
    diff lines** — identical instruction streams; the
    class files differ only in metadata (line-number /
    local-variable layout implied by source formatting).
  - **11 classes — skeleton-equal.** From the 3
    transcribed files: GlobalSessionGuard (+$Decision,
    $Fingerprint); Platform; PurchaseLog (+$1,
    $CloseResult, $ItemStat, $Purchase, $Stats,
    $Status). A structural comparison shows the same
    opcodes, constants and call sequences per method in
    the same order; residual differences are local-slot
    numbering, branch layout and exception-table row
    ordering only.
- **Caveats, plainly:**
  - Live classes carry a LineNumberTable but **no
    LocalVariableTable** — local variable names in the
    3 transcribed files are meaningful reconstructions,
    not recovered originals. Byte-identity for those 29
    classes was not achievable without the original
    sources and was not faked; instruction/skeleton
    equality + 412/412 tests + the 3-boot rig are the
    gates used instead.
  - One reconstruction judgement call: in
    `WRVShops.onEnable` the bytecode holds a
    constant-true boolean feeding the
    no-activity-auto-recovery decision and the branch
    `if (!flag) beforeMutation("unknown mutation
    coverage; automatic recovery disabled")`. It is
    reconstructed as the local
    `boolean mutationCoverageComplete = true;` — the
    behaviour is bytecode-faithful; the NAME is
    reconstructed.

## 4. The 1.3.8 delta (console admin, from HELD 1.3.5, logic unchanged)

- **New files:** `AdminCommand.java` (Bukkit-free gate,
  routing, texts, status formatter) and
  `ShopAdminCommand.java` (`/excelshopadmin` glue),
  copied from the 1.3.5 build with header comments
  updated to 1.3.8. Both compile to classes
  **javap-skeleton-identical** to the 1.3.5 build's.
- **`WRVShops`:** `excelshopadmin` executor registered
  in `onEnable` immediately after the `excelshop`
  registration; `reloadAdminData()` added (all-or-
  nothing reload of config + spawn catalog + mobile
  catalog; registry and checkout deliberately NOT
  reloaded) — the verbatim 1.3.5 method.
- **`CheckoutService`:** `activeQuoteCount()` and
  `activeQuotesFor(UUID)` read-only counters added
  (verbatim 1.3.5).
- **`plugin.yml` (1.3.8):** built from the LIVE 1.3.7
  file — version `1.3.8`; keeps the 1.3.7 `stats
  [days]` usage and the `wrvshops.admin` node (default
  op); adds the `excelshopadmin` command (no
  command-level permission — the gate is in code) and
  the `excelshops.admin` node (default op). The two
  admin nodes are distinct and both ship.
  `pom.xml` version 1.3.8.
- **`AdminCommandTest.java`:** copied from 1.3.5;
  adapted only in the version assertion
  (`'1.3.8'`), the header comment, and one comment
  line. 18 checks.
- **Class delta, 1.3.8 vs rebuilt 1.3.7 (exact):**
  among the 99 carried classes, **only `WRVShops` and
  `CheckoutService` differ**; exactly **9 new classes**
  (AdminCommand + 5 nested, ShopAdminCommand + 2
  nested); the other 97 carried classes are
  byte-identical to the rebuilt 1.3.7. Total 108.

## 5. Build, tests, determinism

- Harness: the fleet `build.sh` (JDK 25 at
  `~/workspace/.jdk25`, `--release 21`, Paper-API +
  Vault + kyori 4.20 classpath) — the same harness as
  1.3.4/1.3.5. Packager: `package-1.3.8.py` (fixed zip
  stamp 2026-11-11 00:00; manifest bytes identical to
  the live 1.3.7 jar's: `Created-By: 25.0.4 (Eclipse
  Adoptium)`).
- **Tests: 430/430 on both builds** (412 carried +
  18 AdminCommand; JUnit summaries preserved in
  `work/out-138-a/junit.txt` and
  `work/out-138-b/junit.txt`). The reconstructed
  1.3.7 base alone: **412/412**
  (`work/out-137-final/junit.txt`).
- **Deterministic verdict: DETERMINISTIC.** Two
  independent builds were packaged and came back
  byte-identical. After a scratch cleanup wiped the
  second packaged jar, the verdict was re-proven from
  the surviving class trees: re-packaging
  `work/out-138-a/classes` and `work/out-138-b/classes`
  each reproduces the shipped jar's SHA-256 exactly
  (`cmp` clean against the shipped jar).
- **Source zip:** `ExcelShops-1.3.8-source.zip` —
  74 entries (the tree-1.3.8 project under an
  `ExcelShops/` prefix, fixed timestamps) — 186,824 B
  — SHA-256
  `78217dadf450dad9b4c4778b01bf23ebe963687a93cfca2f145dfe76e1613c76`.
- Trees kept in this folder: `tree-1.3.7/`
  (reconstruction) and `tree-1.3.8/` (shipped source);
  build outputs under `work/`; `build.sh` and
  `package-1.3.8.py` alongside.

## 6. Rig (summary — full evidence in rig-1.3.8/RIG-RESULTS-1.3.8.md)

Paper 26.2-123 scratch server (Vault, EssentialsX,
GriefPrevention, ExcelMenu 0.1.0, RigCheck), rig jar's
108 classes byte-identical to the shipped jar (only a
plugin.yml softdepend line differs — the standing rig
tweak). Three boots, all Java exit 0, **SEVERE 0**,
RigCheck **PARITY OK** every boot:

1. **Defaults boot:** all nine 1.3.5 admin/excelshop
   console checks verbatim; stats argument validation
   verbatim (usage line for `abc` and `99999`); the two
   valid stats calls dispatched async (replies raced
   the immediate stop — captured in boot 2).
2. **Session proof ON, first enable:** silent
   CLEAN_START, purchases available, stats replies
   captured verbatim for 7 and 30 days on the empty
   log, reload + status clean, and the clean close
   removed the guard's envelope/fence.
3. **Session proof ON, second enable:** mid-session
   snapshot shows the READY envelope on disk (both
   journal fingerprints), fence correctly absent
   before any mutation, sibling lock present; clean
   close removed envelope + fence again.

Not rig-exercised (stated in the rig results): crash /
UNCERTAIN-fence / no-op-recovery paths, the mutation
fence write (needs a real shop mutation), with-shops
status, owner-name resolution, non-admin denial,
reload failure path. With-shops status and the denial
remain owed as post-install live proofs, exactly as
they were for 1.3.5.

## 7. Scope limits honoured

Desk side only. No panel work, no install, no MANIFEST
edit (a draft entry text is in
`MANIFEST-ENTRY-DRAFT.md` in this folder for the
supervisor to file), no GitHub/Drive publishing.
Nothing was staged into the install queue by this lane.
