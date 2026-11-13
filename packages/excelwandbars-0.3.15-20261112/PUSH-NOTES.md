HELD — do not publish until the supervisor releases this package after ExcelWandBars 0.3.16 lands and Mike's live proof passes.

# ExcelWandBars 0.3.15 — push notes (legs for the parent / board-manager)

Staged Nov 12, 2026 (records catch-up, mirroring the 0.3.14 package).
The staging lane ran NO connector writes, NO Drive upload, NO panel
work, and NO MANIFEST edit. The build is ALREADY INSTALLED (Nov 12,
2026, parent hand window — see INSTALLED.txt / BUILD-RECORD.md
section 6), so there is no MANIFEST filing or install leg left.
Remaining legs are HELD with the rest of this package's close-out:
do not run any leg below until the supervisor releases this package
after ExcelWandBars 0.3.16 lands and Mike's live proof passes. (The
legs themselves are pre-approved recording work — Mike, Nov 4:
recording stuff needs no approval. When released, route: BROWSER
route (overnight / Mike not actively tapping) — zero approval
dialogs; do not park these legs on an untapped connector card.)

1. **Duplicate guard first (G2):** before either publication push,
   run `~/workspace/workflow-tools/pr-exists.sh ElMikeExcel/wrv-plugins excelwandbars-0.3.15`.
   Exit 3 = an open/draft PR for this package already exists — verify
   that PR instead of pushing a new one. The close-out record carries
   the line: "Publication claim: ElMikeExcel/wrv-plugins PR #___
   (duplicate check run <timestamp>, result ___)".
2. **Drive deposit** (shared Drive "Minecraft WRV" folder, per the
   standing layout): the installed jar `ExcelWandBars-0.3.15.jar`
   (550,474 B, SHA-256 `61b278b46d8708dff04eb08a0e99a87ee25ac78b19310eeb10ba0b2411b700f2`)
   + the source zip `ExcelWandBars-0.3.15-src.zip`, with this
   package's SOURCE.md alongside (build
   `EXC-WANDBARS-0.3.15-WRV-20261112-01-9bc99f`). Byte-verify both
   deposits after upload. No passwords/tokens/IPs in anything
   uploaded. NOTE the jar's GitHub leg below EXCLUDES the jar per
   house rule (same convention as the 0.3.14 package); the Drive leg
   is where the jar itself is deposited.
3. **GitHub record PR** in `ElMikeExcel/wrv-plugins`: branch
   `record/excelwandbars-0.3.15-20261112`; draft PR carrying this
   package EXCLUDING the jar (same shape as the 0.3.14 record —
   the source zip, src/ and tests/ trees, docs, and sums are carried;
   the jar is not), left OPEN as a draft, never merged.
4. **Same-pass surfaces (supervisor close-out, not the records
   lane) — HELD with this package:** What's New note on BOTH surfaces
   (website section + in-game `ExcelWhatsNew/changelog.yml`); Guide
   knowledge line; tips check. Mike's post-install live Bedrock proof
   of 0.3.15 FAILED on Nov 12, 2026 (~12:58 PM CST, "Still the
   same"); tracker row 61 stays open for the ExcelWandBars 0.3.16
   successor and Mike's live proof on that build.
