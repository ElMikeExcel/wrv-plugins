# PUSH NOTES — ExcelTrainingDummy 1.0.0 (NOT published by the build lane)

Staged by the desk build lane (Nov 12, 2026) for the board
supervisor's close-out. Nothing was pushed to GitHub or Drive.

Supervisor close-out checklist:
1. Duplicate guard FIRST: run
   `~/workspace/workflow-tools/pr-exists.sh wrv-plugins exceltrainingdummy`
   (and the website repo equivalent if surfaces are published) —
   exit 3 = verify the existing PR instead of opening a new one.
2. Records PR (wrv-plugins): this package's jar + source zip +
   BUILD-RECORD.md / SOURCE.md / FINGERPRINTS.md / SHA256SUMS /
   TEST-RESULTS.txt / RIG-RESULTS.md / MANIFEST-ENTRY-DRAFT.md,
   as an OPEN DRAFT (never merged), with the close-out line:
   "Publication claim: wrv-plugins PR #___ (duplicate check run
   <timestamp>, result ___)".
3. Drive deposit: jar + source zip into the shared Drive
   "Minecraft WRV" folder at install/close-out time per house
   routing.
4. File the MANIFEST entry from MANIFEST-ENTRY-DRAFT.md after
   resolving every GATE-VERIFY line with fresh live pulls.
