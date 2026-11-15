# INSTALL PACKET — ExcelAttunement 0.1.8 (Fix A: a spell arrow is not a bow shot)

Status: BUILT, TESTED, NOT INSTALLED. Candidate jar:
`~/workspace/excelattunement/fix-0.1.8/jars/ExcelAttunement-0.1.8.jar`
SHA-256 `971292ede865b0a0ae6fd436ce90d48fa3a96e58cc73d26f8e152b2e4822262a`,
70,796 B (two builds byte-identical). Companion evidence: SOURCE.md,
TEST-REPORT.md, CHANGELOG-0.1.8.md in the same folder.

House window rules apply (marker file, emptiness/weekend gate, panel
Shut Down → swap → Start, never console stop). This is a jar swap only:
NO config, NO spell table, NO data-folder file is touched — the live
106-row override in `plugins/ExcelAttunement/` stays exactly where it is.

## STEP 0 — PRE-INSTALL HARD GATE: prove the live jar is the build base

The fix base was recovered and proven offline (SOURCE.md), but the live
jar gets re-verified in the window before anything is swapped. If ANY
check below fails, STOP — do not install, and report the mismatch.

1. Download the live `plugins/ExcelAttunement-0.1.7.jar` (the live
   filename per the Oct 26, 2026 install record) to the build box.
2. `sha256sum` it. It MUST equal:
   `4b5d8837eca4039e0d3f63a33fdd849a94e245718cbe769c51725c0d71665dcc`
   (the hash in the Oct 26 install record and the Drive 2026-10-26
   backup). A different hash means live is not the jar this fix was
   built on — stop.
3. Extract and disassemble the three classifier-pipeline methods:
   ```
   cd <scratch> && unzip -o ExcelAttunement-0.1.7.jar 'com/winkreyes/excelattunement/*'
   javap -p -c -classpath . com.winkreyes.excelattunement.core.Weapons | sed -n '/kindOfProjectile/,/^$/p'
   javap -p -classpath . com.winkreyes.excelattunement.ExcelAttunement   # onLaunch + resolveAttack present
   ```
   `kindOfProjectile` MUST show the defect's shape: ARROW/SPECTRAL_ARROW
   → `kindOfItemName(hand)` → `CROSSBOW` check → otherwise an
   unconditional `WeaponKind.BOW` return (i.e. `getstatic
   WeaponKind.BOW; areturn` as the fall-through). That fallback is what
   0.1.8 replaces; if the live bytecode has already been fixed or shaped
   differently, this build does not describe live — stop.
4. Note the live data-folder state for the post-boot comparison (do not
   modify): `plugins/ExcelAttunement/spell-classes.txt` (106 rows),
   `config.yml`, `players.yml`.

## STEP 1 — Backup (in-window, before the swap)

- Rename in the panel file manager (nothing is deleted):
  `plugins/ExcelAttunement-0.1.7.jar` →
  `plugins/ExcelAttunement-0.1.7.jar.bak-<install-date>-arrowfix`
  (the Oct 26 window used the same convention:
  `ExcelAttunement-0.1.6.jar.bak-20261026-stance`).
- The downloaded Step-0 copy plus `recovered-0.1.7/` in this build
  folder are the second and third copies of the same bytes.

## STEP 2 — Swap

- Upload the candidate under its own name:
  `plugins/ExcelAttunement-0.1.8.jar` (the panel upload lands under the
  granted file's on-disk name — stage it locally under exactly this
  filename; Upload menu LEFT OPEN, element ref without '@', Overwrite
  dialog if prompted).
- Verify in the panel listing: `ExcelAttunement-0.1.8.jar` present at
  70,796 B, the 0.1.7 `.bak` present, and NO other `ExcelAttunement*.jar`
  active (two live copies of the same plugin = duplicate-plugin boot
  failure).
- Panel Start. Create/delete `WINDOW-IN-PROGRESS.txt` per the standing
  marker rule.

## STEP 3 — Boot verification (all must hold; quote the lines)

From `logs/latest.log` after Done:

- `[ExcelAttunement] Loading server plugin ExcelAttunement v0.1.8`
- `[ExcelAttunement] Enabling ExcelAttunement v0.1.8`
- `[ExcelAttunement] Using spell table override: plugins/ExcelAttunement/spell-classes.txt`
  ← proves the live 106-row table is still the one in force (Gate 2).
- `[ExcelAttunement] Magic found: cast interception ON (PreCastEvent block + CastEvent attunement).`
- `[ExcelAttunement] ExcelAttunement enabled: ON, window 30s, combat context 10s, spell table 106 spells.`
- `[ExcelAttunement] Stance lock: weapon-lock ON, wand-lock ON, actionbar ON (live ON), sounds ON.`
- Done (…s), **SEVERE 0**, TPS 20.
- The guild-ward line will read its live variant (EliteMobs present) —
  that is expected; the rig printed "EliteMobs not present" only because
  the scratch server has no EliteMobs.
- Console: `attunement status` → prints the ON/window/combat/table line.
- Console: `guildgate status` → **guild LOCKED, seal count unchanged**
  (3 of 5 at packet time; the point is: unchanged by this window).
- BossTrial is untouched by this swap; the seal-governance note still
  applies to any boot read: quote the BossTrial awake lines as usual.
- Post-boot: `plugins/ExcelAttunement/spell-classes.txt` byte-identical
  to its pre-window state (nothing in this window writes it).

If the boot shows Attunement failing to enable, a duplicate-plugin
error, or the "spell table 106 spells" line missing → roll back
(Step 4) and report.

## STEP 4 — Rollback (one restart)

1. Panel Shut Down.
2. Delete (or rename aside) `plugins/ExcelAttunement-0.1.8.jar`.
3. Rename `plugins/ExcelAttunement-0.1.7.jar.bak-<install-date>-arrowfix`
   back to `plugins/ExcelAttunement-0.1.7.jar`.
4. Panel Start; verify `Enabling ExcelAttunement v0.1.7` + the same
   table/stance lines + SEVERE 0.
No data migration exists in either direction — 0.1.8 reads and writes
the same `players.yml` format as 0.1.7.

## STEP 5 — Player proof (owed; the rig cannot close these)

1. **Jester (Java; he hit the bug):** cast **Arrow** and land a solid
   hit on a spider (or any mob). Expected: damage lands, and NO
   "stance set: FIGHTER" — the actionbar shows the casting stance
   (Arcane/Caster) from the cast itself, wand NOT stilled. Repeat with
   a ground hit beside a mob (splash) — same expectation.
2. **Bow control (Jester or Mike):** shoot a mob with a real bow, then
   a real crossbow — fighter stance MUST still set on the hit, exactly
   as before. (If it doesn't, that's a regression — report immediately;
   the unit pins and rig controls both say it sets.)
3. **Scroll sanity (any Attunement user):** the 0.1.7 stance-scroll hop
   and the hotbar locks behave as before (untouched code riding the
   same swap; one in-game scroll confirms no jar mix-up).

## Deposit / records (for the filing pass)

- Jar + `source/ExcelAttunement-0.1.8-src.zip`
  (SHA-256 `e4c3992745d30ad1c9036764ea79d91440dbcaaddff8f3ad29c0e0097f0afbad`)
  into the Drive Minecraft WRV deposit folder with this packet's
  SOURCE.md/CHANGELOG alongside, per the standing deposit rule.
- The recovered 0.1.7 tree (`gh-0.1.7/` in this build folder) should be
  filed as the Attunement source of record, closing the "source
  deposited = 0.1.2, live = 0.1.7" drift.
- Manifest/queue note: this build is NOT in the install manifest; add
  it as a PENDING entry when filing (jar-only swap, rides any normal
  empty-server window; no config/template companions).
