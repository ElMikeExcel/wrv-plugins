# excelemfixes-0.21.0-20261125 — PUSH NOTES

- Repo: ElMikeExcel/wrv-plugins. Branch: record/excelemfixes-0.21.0-20261125. Package path: packages/excelemfixes-0.21.0-20261125/.
- Lane: Homebase native git is the standing default; from this worker seat outbound SSH to Homebase is blocked, so the fallback GitHub connector route carries the push if it lands from here (route actually used is stated in the surfaces PUBLISH-RECORD.md). No browser route used.
- Package modeled on the canonical ExcelEMFixes records shape (0.19.0 PR #308, 0.20.0 PR #309 packages): source tree of record + BUILD-RECORD + RIG-RESULTS + FINGERPRINTS + PUSH-NOTES + RECEIPTS + SOURCE + FILE-LIST + SHA256SUMS + INSTALL-PACKET + INSTALLED. Jar NOT in git (hash-referenced in FINGERPRINTS.md).
- Record PRs stay OPEN DRAFTS, never merged (house rule).
- Duplicate guard: GitHub connector PR list for wrv-plugins checked before assembly (Nov 25, 2026) — no PR for excelemfixes-0.21.0; latest records PR is #309 (0.20.0). Re-checked immediately before push.
