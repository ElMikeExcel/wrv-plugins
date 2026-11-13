# ExcelSummons 0.1.8 — BUILD RECORD

**Product:** ExcelSummons 0.1.8 — the 0.1.7 compass Summons
page with its Bedrock buttons fixed and out-now counts added.
**Commission:** Mike, Nov 12, 2026, 6:51–6:52 PM CST — his live
screenshot showed the 0.1.7 Bedrock form rendering with dead
buttons, and he asked for out counts on the page. Full
commission: `../COMMISSION-018.md`. Design: DESIGN-0.1.8.md in
the source zip.
**Build:** EXC-SUMMONS-0.1.8-WRV-20261112-01-f8d6ba
**Built:** 2026-11-12 (America/Chicago)
**Base:** 0.1.7 exactly as installed live (90,538 B /
2fc1a226…). Sources differ from 0.1.7 in exactly four files:
BedrockForms.java (rewritten), MenuModel.java (counts),
SummonsMenus.java (info-book lore only), BuildInfo.java +
plugin.yml (version). config.yml is byte-identical to 0.1.7
(SHA-256 659d9e2c…c3a9).

## Deliverables (this folder)

- `ExcelSummons-0.1.8.jar` — **92,387 B** — SHA-256
  `835419aaeff434b61e6be2f5cda1340233299e11262feebf96206cdd00bb8841`
- `ExcelSummons-0.1.8-sources.zip` — 117,441 B — SHA-256
  `6c6e2943bdb35f2fd7ce17adc0eb5bb50d1f7ad4f65437442beaa77eb9029974`
  (src/ + tests/ + plugin.yml + config.yml + local-build.sh +
  DESIGN-0.1.3…0.1.8.md, timestamps pinned 2026-11-12)
- `TEST-RESULTS.md` — 94/94 harness tests (89 carried, 2 of
  them re-pins; 5 new).
- `RIG-RESULTS.md` — two-phase desk rig (see below).
- `packet-0118-20261112/INSTALL-PACKET.md` — jar-only swap;
  predecessor 0.1.7 held as .bak; no menu-entry change; no
  config change.
- `SHA256SUMS` — jar + source zip.

## The two changes

1. **Bedrock buttons fixed.** BedrockForms now mirrors the
   live-proven ExcelWandBars implementation: Geyser class
   loader ONLY (Floodgate attempt removed), NEW Cumulus
   (`org.geysermc.cumulus.form.SimpleForm` via
   `GeyserApi.connectionByUuid`), response handling via
   `FormBuilder.resultHandler(BiConsumer)` unwrapping
   `ValidFormResponseResult.response()` →
   `SimpleFormResponse.clickedButtonId()`, send via the
   connection's assignable 1-arg sendForm hunt. The 0.1.7
   failure mode — handler attach by `validResultHandler` hunt
   against the OLD Cumulus, exceptions swallowed silently — is
   gone: handler attach failure fails the build loudly, and
   unparseable responses / missing connections / failed sends
   log at WARNING. The send API
   (`sendSimpleForm(...) → boolean`, false → chat fallback) is
   unchanged, as are all three SummonsMenus form flows.
   WandBars' FormContrast label normalization was NOT imported
   (Summons labels are plain already).
2. **Out-now counts.** MenuModel derives `OutCounts` from the
   page's CAST entries via `SummonRecord.baseKey`: mount →
   mounts out; wolf/cat/familiar → cast pets out; all other
   cast keys → combat summons out; total = cast list size.
   Adopted pets are never counted. `Page.outNowLine()` renders
   `Out now: M mount(s) · P cast pet(s) · C combat summon(s)
   (T total)` (all three groups always print) or `Nothing out
   right now.`; it sits in the Bedrock main body after the
   registered line and leads the Java info book's lore (name
   still "Mana held: H% of your pool").

Unchanged from 0.1.7, by design and by test: adopted pets hold
0% mana (Mike's Nov 12 ruling), caps, percent table, dismiss
scopes, the confirm-gated per-pet adopted dismiss as the only
adopted-dismiss door, `/summons list` output, persistence.

## Verification

- **Harness 94/94** (TEST-RESULTS.md), including the
  behavioral handler test against the real Geyser jar's
  Cumulus classes — every button of all three form shapes,
  both dispatch paths, closed → −1.
- **Desk rig, two phases** (RIG-RESULTS.md): phase A (no
  Geyser/Floodgate) **24/24 PASS, SEVERE 0** — all 22 of
  0.1.7's checks carried + the 2 counts checks; phase B (+
  Geyser-Spigot 2.12.1-b1292 + Floodgate b141, the live mix)
  **30/30 PASS, SEVERE 0** — the sequence re-proven plus the
  form-response section: forms built through the production
  buildForm against the real Cumulus classes, synthetic
  responses driven through the attached handler for EVERY
  button of all three forms on both dispatch paths
  (callResultHandler and the raw-payload entry), closed → −1,
  Keep at confirm index 1. Both phases boot the exact build
  line `EXC-SUMMONS-0.1.8-WRV-20261112-01-f8d6ba`.
- **Triple build byte-identical:** the project double-build
  (local-build.sh builds twice and `cmp`s) plus a clean-room
  rebuild from the shipped source zip in a fresh directory —
  all three jars SHA-256 `835419aa…8841`; the clean-room run
  also re-ran the harness green.
- **Source diff vs 0.1.7:** exactly the four files named above
  (+ tests); config.yml byte-identical verified by hash.

## NOT done here (by commission)

- No MANIFEST entry filed, no install — the parent verifies
  and files; the installer runs the packet.
- A real Bedrock client's tap is Mike's post-install live
  proof (packet Acceptance #1). The rig's synthetic-response
  proof is the strongest desk proof available and is stated
  as exactly that.

Excel — Wink Reyes Valley (Iris / Mike).
