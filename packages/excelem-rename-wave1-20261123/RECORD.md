# ExcelEM Rename Wave 1 — RECORD (D163/D211)

**Installed live:** Nov 23, 2026, 7:03–7:15 PM CT, parent hand window
hand-20261123-190345-guildvendor-rename-tw (Panel API route; D189
escalation after three consecutive install-failsafe D180 exits).
**Install record:** RUN-RECORD.md in this package (full window record;
the rename wave was entry 4 of 4 in that window).
**Status:** INSTALLED + boot-verified live. This PR is the records
package only — it stays an OPEN DRAFT, never merged.

## What changed (5 jar swaps, one stop/start cycle)

ExcelEM naming law (D163): EliteMobs-related plugins carry the ExcelEM
prefix. Wave 1 = the four cheap standalones from the Nov 20 rename
audit, plus the D211 companion build that the SealLog rename required.

1. **ExcelEliteMobsAbilityBedrockBridge 0.2.0 → ExcelEMAbilityBedrockBridge 0.2.0**
   Jar 31,990 B / sha256 9c4158212c7443f49be5371784aab229855381cc1f8ba46359a7637b7a32a4c1.
   Jar-level rename (plugin.yml name line only). Command /abilities and
   permission node unchanged. Data folder migrated (config.yml copied
   byte-exact).
2. **ExcelPartyDrops 0.1.0 → ExcelEMPartyDrops 0.1.0**
   Jar 14,543 B / sha256 12f3352341b42a14a19977e0ee43a97eee13b1e79062c8e0e0345e33ed0f8df8.
   Command /partydrops unchanged. Config migrated byte-exact.
3. **ExcelWitherSkulls 0.1.0 → ExcelEMWitherSkulls 0.1.0**
   Jar 14,862 B / sha256 9361dd79f9b0dd639d2b0256bfb866f57d3160ebe1e850e718d01a80e6abf8ed.
   Command /witherskulls (+/wskulls) unchanged. Config migrated byte-exact.
4. **ExcelSealLog 1.0.0 → ExcelEMSealLog 1.0.0**
   Jar 8,344 B / sha256 efa5719c33993c4704bdfe20af47c8d6b5133010cfcb071cffb65b89741c2c92.
   Stateless (no data folder). plugin.yml keeps `prefix: SealLog`
   deliberately — the log prefix is not plugin identity.
5. **ExcelSealAlerts 0.1.2 → 0.1.3 (companion, name unchanged)**
   Jar 43,153 B / sha256 26c2bb34b89b7f7baf48cde5c98b7bc260eb8da70249967aaa9f4346fc985159.
   D211: SealAlerts taps SealLog's logger BY PLUGIN NAME, so its
   by-name reference moved to ExcelEMSealLog at THREE source sites —
   the TAPPED list, the plugin.yml softdepend, and the SealParser
   dispatch key (SealParser.java:76; a TAPPED-only fix would have
   attached the tap yet silently dropped every SealLog fight line —
   dispatch harness 4/4 PASS is the counterfactual proof). Changes
   4+5 landed atomically in the same window; rig controls C2/D2 prove
   both mismatch directions fail silently. SealAlerts' data folder
   was never written (config hash unchanged).

**NOT renamed:** Together and RaidBuff keep their current names —
Mike declined those renames Nov 22, 2026 (settled D163 exceptions).
They are not part of this package.

## Live verification (from RUN-RECORD.md)

- Boot Done (37.785s), SEVERE 0; enables under all four new names,
  no old-name enables.
- ExcelEMSealLog READY version=1.0.0; ExcelSealAlerts v0.1.3 READY
  taps=[ExcelBossTrial, ExcelGuildGate, ExcelEMSealLog] — all three.
- sealalerts status post-boot: Watching [ExcelEMSealLog,
  ExcelGuildGate, ExcelBossTrial], posts ok/failed 1/0 — the
  server_started event posted: the D211 live pair proof.
- partydrops / witherskulls / abilities status read-backs all match
  the migrated live configs.
- Player downtime ~3.6 min at 0 players; fire→verified close ≈ 11 min.

## Package contents

- RECORD.md — this file.
- STAGING-RECORD.md — wave-1 staging record (D163): live census,
  per-plugin safety scans, rig boots A/B/C1/C2, the SealLog blocker
  as found, and the D211 addendum.
- D211-FILING-RECORD.md — the companion-build record: three-site
  coupling note, deterministic double build, parser dispatch
  harness, rig boots D1/D2, packet rewrite + MANIFEST filing.
- INSTALL-PACKET.md — the filed 5-change install packet (atomicity
  requirement, gates, window steps, boot-verify lines).
- RECEIPTS.md — wave-1 live evidence (receipts sha256 abae3d3a…).
- RECEIPTS-D211.md — D211 live evidence (sha256 0ff32b1f…).
- RUN-RECORD.md — the hand window's full run record (install proof).
- INSTALLED.txt — the packet folder's installed marker.
- JAR-SHA256SUMS.txt — the five candidate jars (all five re-verified
  locally with `sha256sum -c` at records time: 5/5 OK).
- SHA256SUMS — hashes of this package's files.

Candidate jars themselves are not in this repo package (house shape:
jars are cited by hash; the staged builds live in
~/workspace/rename-wave1-staging-20261123/builds/).

## Still owed (unchanged lanes, not part of this PR)

- Rename-wave same-pass surfaces: website pair name swap + What's
  New entries (sibling legs of the same surfaces pass).
- Wave 3 note (carried in the packet): when GuildGate/BossTrial
  rename, SealAlerts' TAPPED list + parser keys for those names must
  move in the same window.
