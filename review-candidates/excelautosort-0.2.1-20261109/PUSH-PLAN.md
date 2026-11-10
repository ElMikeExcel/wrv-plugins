# PUSH PLAN — ExcelAutoSort 0.2.1 record PR (for the parent to execute)

Prepared Nov 9, 2026 (desk build lane). NOT installed and NOT
pushed — this plan is executed by the parent/board supervisor.
Per Mike's standing GitHub routing rule (Oct 25, 2026): when Mike
is busy/asleep/unknown use the BROWSER route (zero approval
dialogs); when he is awake and available, the connector route
batched in ONE push_files call. Before creating the branch,
re-check that `record/excelautosort-0.2.1-20261109` still does not
exist; if it unexpectedly exists at main's head with no extra
commits, use it as-is. The jar itself goes to Drive (Minecraft
WRV "Build files"), NOT to GitHub (text-only rule). Drive leg is
PENDING (parent deposits + byte-verifies, then fills the IDs into
SOURCE-PROVENANCE.json and the PR body).

## Target

- Repo: `ElMikeExcel/wrv-plugins`
- Base: `main`
- New branch: `record/excelautosort-0.2.1-20261109`
- Local source dir (paths below are relative to it):
  `/home/hatch/workspace/records-catchup-20261103/packages/excelautosort-0.2.1-20261109`
  (push the package contents under
  `review-candidates/excelautosort-0.2.1-20261109/`)
- Commit message: `Record ExcelAutoSort 0.2.1 (admin per-player surface) — built Nov 9, 2026`
- PR title: `Draft source record: ExcelAutoSort 0.2.1 — admin per-player surface`
- PR body: Source record for ExcelAutoSort 0.2.1 (built Nov 9,
  2026; NOT YET INSTALLED — the swap packet is filed PENDING in
  the install MANIFEST from the package's
  MANIFEST-ENTRY-DRAFT.md). 0.2.1 fixes the 0.2.0 defect Mike
  commissioned the same evening (~8:07 PM CST): the admin surface
  was prune|reload only — no player-targeted status/revoke —
  violating the Nov 3, 2026 console admin rule. It adds, under
  `excelautosort.admin` (op) and console-runnable:
  `/autosort admin status <player>` (sort ON/OFF; Max
  Organization OWNED / RENTED with exact expiry / LAPSED / NONE),
  `/autosort admin revoke <player>` (buy flag AND any rent
  cleared at once, persisted in data/maxorg.yml, Protect-logged
  as autosort_maxorg_admin_revoke), and
  `/autosort admin grant <player> max` (no-charge permanent
  grant, included by ExcelCosmetics grant|revoke symmetry,
  Protect-logged as autosort_maxorg_admin_grant). Targets resolve
  by exact online name, then by played-before record; never-joined
  names are refused. Everything else is 0.2.0 behavior unchanged.
  Tests: unit 35/35 (29 carried + 6 new); admin behavior rig
  phase 6 21/21 (console grant/revoke proven LIVE + persisted
  mid-boot on Paper 26.3); full 0.1.0/0.2.0 regression rig 75/76
  with the one miss proven a Protect async-flush race from the
  final evidence stream (BUILD-RECORD.md). Record package: 52
  files under review-candidates/excelautosort-0.2.1-20261109/,
  SHA256SUMS-verified. Jar of record: ExcelAutoSort-0.2.1.jar,
  159,240 B, SHA-256
  48a5c0fed431fc9853e0e92ff199b24887ab18485a5febb4132735759617e266
  (Drive deposit PENDING — parent fills the file ID + MD5 here
  after byte-verification; source zip
  ExcelAutoSort-0.2.1-source.zip, 94,174 B, SHA-256
  99f8371e49db29fb0f8c9810238307a67b5847cffc7d74dc774eaa3a784c26c0,
  same treatment). Draft record PR per standing rule: never
  merged.
- The browser UI may need several commits (one per directory
  batch); use the exact commit message above for every batch. PR
  stays an OPEN DRAFT, never merged (standing rule).

## Files (52)

Generated in FILE-LIST.txt in this package (the supervisor
re-generates the list from the package directory at push time if
anything changed; every file except SHA256SUMS itself is hashed
in SHA256SUMS).

## After the push (parent)

- Verify via the GitHub API: PR open + draft + unmerged, head =
  the new branch, 52 files.
- Drive deposit (jar + source zip into the Minecraft WRV folder),
  byte-verify after upload, fill IDs into SOURCE-PROVENANCE.json.
