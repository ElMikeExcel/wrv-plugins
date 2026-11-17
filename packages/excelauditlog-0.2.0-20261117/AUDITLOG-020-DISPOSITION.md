# Disposition — ExcelAuditLog 0.2.0 "drops a queued row on disk-append failure"

**Date:** Oct 31, 2026 (daylight analysis; no live changes, no installs)
**Subject:** ExcelAuditLog 0.2.0 — the LIVE version (boot census Oct 31, 2026: "ExcelAuditLog 0.2.0 (live)")
**Claim (Moon audit, findings email Oct 30, 2026, §9 + follow-up finding 1):** "ExcelAuditLog 0.2.0 can discard queued audit records when a disk append fails… `AuditWriter.java`, lines 40–58, removes rows from its queue before writing the batch. There is no retry or requeue after an IOException."

## Verdict: **CONFIRMED** — severity LOW–MODERATE

The mechanism is exactly as claimed, verified two independent ways against the 0.2.0 source of record: (1) a line-level read of the queue/append path, and (2) a fresh standalone reproduction run on this machine today (details below). One precision on the word "silently": the failure is **logged but unrecoverable** — the caller catches the exception and prints one console warning (`flush failed: …`); the rows themselves are gone, nothing retries them, and no operator action can get them back.

**Open boundary (stated, not hidden):** the live jar's hash has never been matched to this source — the same caveat Moon herself set ("match the current binary before applying a fix"). What would close it: one panel touch to pull the live ExcelAuditLog jar and compare its `AuditWriter.class` against a build of this source (or its SHA against the deposit's SHA256SUMS). The disposition governs the source of record and the 0.2.0 release; if a future pull ever showed the live binary differs, the disposition would need re-running against that binary.

## Source of record

- No local `~/workspace` deposit exists (full search: no ExcelAuditLog folder, source, or jar on this machine).
- Source used: the shared Drive Source collection Moon cited as this plugin's source-record reference — `AuditLog-evidence-quality-ROOT-REVIEWED-OFFLINE-20261028-0848.tar.gz` (downloaded Oct 31, 2026; tarball SHA-256 `7816fd2845a7d426e359929227a2126c2010f79bb3a360694b6b2004aaf8f8d0`; internal `SHA256SUMS.txt` verifies clean). Working copy extracted to `~/workspace/moon-audit-reconciliation/auditlog-src/`.
- Identity: the deposit's own `plugin.yml` declares `name: ExcelAuditLog`, `version: '0.2.0'`, main `com.winkreyes.auditlog.ExcelAuditLog`. This is the only 0.2.0 source in any record.

## The mechanism (quoted, not summarized)

`AuditWriter.flush()` — the drain happens **before** any disk write:

```java
public synchronized void flush() throws IOException {
    if (q.isEmpty()) return;
    dir.mkdirs();
    String[] r;
    File cur = null; List<String> buf = new ArrayList<>();
    while ((r = q.poll()) != null) {                    // rows LEAVE the queue here
        File f = new File(dir, "audit-" + r[0].substring(0, 10) + ".csv");
        if (cur != null && !f.equals(cur)) { write(cur, buf); buf.clear(); }
        cur = f; buf.add(Csv.row(r));
    }
    if (cur != null) write(cur, buf);                   // the append happens only now
}
```

`q.poll()` removes each row from the `ConcurrentLinkedQueue` into a local `ArrayList` buffer; the only disk append is `write()` (a `Files.newBufferedWriter(..., CREATE, APPEND)`), called after the drain (or at a day-boundary switch mid-drain). If `write()` throws `IOException` — unwritable path, disk full/quota, permission — every already-polled row exists only in the discarded local buffer. Nothing in `AuditWriter` catches, retries, or requeues.

The caller, `ExcelAuditLog.flushQuiet()`:

```java
private void flushQuiet() { try { writer.flush(); } catch (Exception e) { getLogger().warning("flush failed: " + e); } }
```

Flush runs async every 100 ticks (5 s) and once more on disable. The warning is the failure's only trace; the next scheduled flush starts from an empty queue. Mid-batch nuance: rows not yet polled when a day-boundary `write()` throws stay queued (the exception exits the loop) — but in the common single-day case the entire drained batch is lost, and a partial write can also leave a truncated last line in the CSV with the remainder of the batch lost.

## Independent reproduction (Oct 31, 2026, this machine)

Compiled the deposit's own `AuditWriter.java` + `Csv.java` standalone (JDK 25) and drove the real class:

- Writer pointed at a directory whose parent is a regular file (append must fail): `pending()` = **1** → `flush()` threw `java.nio.file.FileSystemException: …/audit-2026-10-31.csv: Not a directory` → `pending()` = **0** → **no audit file exists**. The row is neither on disk nor in the queue.
- Control, healthy directory: flush succeeds, the day's CSV contains the header + the row verbatim.

This matches Moon's probe result for result (her probe: invalid parent directory, pending 1 → 0, exception, no file).

## Real-world weight on this server

**What writes rows** (from `ExcelAuditLog.java` + `MoneyWatcher`): Vault/Essentials money-balance changes polled every 10 s (the economy trail — payments, shop income, console eco adjustments such as a refund all surface here), allowlisted commands (`/pay`, `/eco`, `/tpa*`, `/spawn`, `/warp`, `/home`) logged verbatim, teleports (>2 blocks, with a 15-second recent-command context attached), ender-chest open/close item diffs, XP level changes, enchanting. Rows flush every 5 s into one CSV per day under `plugins/ExcelAuditLog/`, and the `/audit` query renders from those same CSVs — a lost batch is a permanent hole in the queryable trail.

**Attribution correction:** BossTrial 0.2.11's grantclaim trail (the Oct 31 remediation grants) is audit-logged by **BossTrial's own** logging ("Every attempt, granted or refused, is audit-logged" — `ExcelBossTrial.java`), **not** by ExcelAuditLog rows. This defect does not touch those records. What it does erode is the *corroborating* money trail: an economy-side remediation visible as a balance change (e.g. Kailey's $6,000 Lost & Found refund, executed as a console balance adjustment) is the kind of event ExcelAuditLog exists to evidence.

**Plausible triggers here:** Shockbyte disk quota exhaustion or a full disk, a transient filesystem/permission fault on the data folder, a failed `mkdirs()` race at day rollover. All are disk-level faults — none is known to have occurred: no `flush failed` warning has ever been reported from the live console (with the caveat that nobody has grepped historical console output for it; the warning is a mid-run console line, not a boot-log line).

**Blast radius per incident:** one flush batch — up to ~5 s of audit events (plus whatever money-poll rows landed in the same window). The plugin is strictly read-only: it never touches balances, items, or gameplay, so the defect cannot corrupt the economy itself — it can only make the record of it incomplete. For an audit trail whose entire purpose is completeness, that is a real defect; at seconds-per-incident behind a disk fault that has never been observed, it is not an emergency.

**Severity: LOW–MODERATE.** Confirmed correctness defect in a compliance-grade trail; low likelihood, small per-incident loss, no gameplay/economy impact, no known live incident.

## Fix shape for a hypothetical 0.2.1 (scoped, NOT built)

1. **Retain until durable commit:** stop draining into a throwaway buffer. Snapshot the batch under the existing `synchronized` flush, write it, and remove exactly those rows only after the write returns; on `IOException`, leave/requeue the batch at the head in original order (e.g. swap the `ConcurrentLinkedQueue` for a lock-guarded deque, or hold the failed batch in a `pendingBatch` field retried first on the next flush, with a sane cap + loud drop warning if the cap is ever hit).
2. **Partial-write policy, stated explicitly:** count lines as they are written (per-line flush or an explicit line counter) so a mid-batch failure retries only the unwritten remainder — no duplicates, no silent truncation. (At-least-once + query-time dedupe on time/player/event/details is the fallback if exact counting proves awkward.)
3. **Keep the warning, add a counter:** retain the `flush failed` log; add a retained-rows/last-failure readout (e.g. on the `/audit types` status line) so a sick writer is visible without console archaeology.
4. **Tests before "ready" (Moon's list, adopted):** failed open, partial write, retry success, shutdown flush, and CDT date-rollover batch split — the rollover path is the one place the current code writes mid-drain.
5. **Gate before any install:** match the live jar to the 0.2.0 source of record first (the open boundary above); build the delta on the verified tree per house rule.

## Records

- Daily log: appended Oct 31, 2026.
- Upkeep checklist row E4: AuditLog struck from the undispositioned list (CONFIRMED, this file); four candidate defects remain undispositioned (ExcelCosmetics 0.1.2, ExcelWaypoints 0.1.0, ExcelTelemetry 0.1.1, WealthBoard).
- Related but separate Moon items, NOT part of this disposition: the recent-command-context privacy note and the empty-CoreProtect-query interpretation note (findings email §9) — both remain as recorded.
