# INSTALL PACKET — ExcelTogether 0.5.3 (jar swap only) — SUPERSEDES the 0.5.2 and 0.5.1 packets

Candidate: `ExcelTogether-0.5.3.jar` in this folder,
sha256 `494073dcabf05ad0637846e21095faea0cef46046f09a7d50d0987f19f43f9f5`, 176,042 bytes.
Provenance + change scope: SOURCE.md. Verdict: BUG-VERDICT.md. Evidence:
TEST-RESULTS.txt (890 unit checks, 0 failures; rig boot 6/6 admin verdicts PASS,
SEVERE 0).

**Install 0.5.3, never 0.5.1 or 0.5.2** — neither was ever installed, and 0.5.3
carries both cumulatively (the party-repair Magic actions + the confirmation gate)
plus the party-leave fix and the console admin surface. Like the 0.5.2 packet, this
jar swap has a companion spells pass (build-0.5.2/INSTALL-PACKET-REPAIREQUIP.md —
the repairequipment anti-bounce cooldown + repairitem 30-minute cooldown) that
runs in the SAME window right after the jar: until it lands, the live
`repairequipment` block is still the stock one and the repair gate is simply never
invoked. Follow that packet's own gates for the spells half.

What players get from this jar: leaving the party at ANY door (/excelparty,
mcMMO /party, /em party — including a /party disband) now ends the party for the
leaver everywhere and stays ended across relogs (the Nov 3, 2026 Jester/Marvin
bug), and the console gains `/excelparty admin status|remove|disband <player>`.

Config: 0.5.3 reads the existing `plugins/ExcelTogether/config.yml` — **do NOT
replace it with the candidate's config.yml** (live values are settled). The one
new key, `mesh.em.door-leave-ends-party`, defaults to `true` (the fix) when
absent, so no config edit is required. Appending the key + its comment from the
candidate's config.yml (under `mesh.em:`, beside `leader-reform:`) is recommended
for in-file visibility and may ride this window as a plain config edit with an
in-place `.bak` first; it is not a gate for the swap.

## Gates (all must hold before touching the server)

1. **Live jar gate.** Pull the live `plugins/ExcelTogether-0.5.0.jar` and hash it.
   It MUST equal `f4490814a9d4ec2e33540126adf6b418a0b58a50b27172119e45950d67353529`
   (the shipped 0.5.0). Any other hash = STOP and report: the delta chain's base
   is no longer proven against live. (If a 0.5.1/0.5.2 jar is somehow live
   instead, STOP and report that too.)
2. **Candidate gate.** The staged candidate hashes to `494073dc…` above, size
   176,042 bytes.
3. **Emptiness gate (standing).** Server verified empty (list = 0, twice) or the
   weekend countdown protocol; BACKUP-FIRST per the standing rule (cap-at-6
   applies); panel window marker per the standing install discipline. No
   BossTrial/GuildGate state is touched by this swap. Note: if Jester_Turtle is
   still stuck in the phantom party with GumGumJetPistol at install time, the
   swap itself resolves the visible symptom only if the party data clears — it
   does NOT retroactively leave anyone; after the boot, use the new console
   surface (step 5) to check and clear their state.

## Steps

1. Panel **Shut Down** (never a bare console stop).
2. Rename-aside the live jar in place:
   `ExcelTogether-0.5.0.jar` -> `ExcelTogether-0.5.0.jar.held-<date>-pre053`
   (nothing is deleted).
3. Upload the candidate under its own name `ExcelTogether-0.5.3.jar`
   (Upload menu left open, element ref without '@', Overwrite if prompted);
   verify the panel-side size is 176,042 bytes.
4. Start the server. Boot-verify from the log:
   - Load phase: `[ExcelTogether] ExcelTogether: party-repair Magic actions
     registered (ExcelPartyRepair, ExcelRepairItem).`
   - Awake line: `ExcelTogether 0.5.3 enabled: ...` with every 0.5.0 segment
     unchanged (x1.25, PARTY, radius 50, sharing, friendly fire, drops, hp
     objective) and the mesh segment reading
     `EliteMobs mesh: ON (poll ~2s, login materialize ON, invites mirrored, EM-door leave ends the party)`,
     tail `PartyLookup service: published. Magic party repair: ON (ExcelPartyRepair + ExcelRepairItem registered), repair confirm ON (10s window, 4h 0m lockout).`
   - Console routing proof (works with nobody online): run `excelparty admin`
     in the console — it must answer
     `Usage: /excelparty admin status <player> | remove <player> | disband <player>`
     (in 0.5.0/0.5.2 the console got "Players only." for everything). A plain
     `excelparty list` from the console must still answer "Players only."
   - No `ExcelTogether` stack traces; no new SEVERE attributable to the swap
     (the pre-existing Essentials unsupported-version ERROR is not ours).
   - Guild state unchanged: LOCKED 3 of 5. TPS 20.0 after warmup. Plugin count
     unchanged (77).
   - If anyone is online and in a party, `excelparty admin status <name>` from
     the console should report all three faces (Together + mcMMO + EliteMobs).
5. If Jester/Marvin are still in the phantom party: `excelparty admin status
   Jester_Turtle` to see the state, then `excelparty admin remove Jester_Turtle`
   (or `admin disband Jester_Turtle`) to clear it — that is what the surface is
   for.
6. Then run build-0.5.2/INSTALL-PACKET-REPAIREQUIP.md in the same window.

## Same-pass surfaces (standing rule — land with the install, report each)

- **What's New (BOTH surfaces):** an entry stating the fixed behavior as a
  how-to: "Leaving the party works from every menu now — /excelparty leave,
  /party leave and the EliteMobs party leave all end the one party everywhere,
  and it stays left." Website section + in-game ExcelWhatsNew changelog.
- **Website:** the party section (winkreyesvalley) — check the party copy for
  anything describing the EliteMobs party as a view only; correct if present.
- **Guide:** party knowledge chunks — same check (any "leaving EliteMobs only
  leaves the view" claim is now wrong); /ask retest: "how do I leave the party"
  should name all three doors as equivalent.
- **Menus / tips:** no menu entries change (no command names changed); broadcaster
  tips — check only, no party-door tip exists to correct.
- **GitHub/Drive records:** the 0.5.3 record package (this build folder) joins the
  records catch-up set.

## Player proofs owed after install

- Jester + Marvin (the incident pair): one leaves via `/em party leave` → stays
  out everywhere (Together list, mcMMO, EliteMobs sidebar); one `/party DISBAND`
  → stays disbanded; a relog by both re-forms nothing.
- The 0.5.2 Repair Equipment gate proofs (still owed — see
  build-0.5.2/TEST-REPORT.md's list) ride the spells pass in step 6.

## Rollback

Shut Down; delete `ExcelTogether-0.5.3.jar`; rename
`ExcelTogether-0.5.0.jar.held-<date>-pre053` back to `ExcelTogether-0.5.0.jar`;
start; confirm the 0.5.0 awake line. Party data formats are unchanged between
0.5.0 and 0.5.3. If the optional config key was appended, it is inert under
0.5.0 — leave it or restore the `.bak`. If the spells pass already landed, roll
it back per its own packet.
