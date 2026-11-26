# SURFACES — ExcelEMFixes v24 (party-drops) — staged, land in the install pass

Same-pass surfaces rule: every item below lands in the install
window's surfaces pass, or is explicitly named owed here.
NOTE: this packet is STAGED ONLY — it is filed into the MANIFEST
as a follow-on once the ExcelEMFixes 0.23.0 install closes
(G1: a v24 live-premise claim would be false while 0.23.0
is still installing). Surfaces land with that filing's window.

## Big-release ★ indicator — GENERIC RULE for every surfaces pass (D247 amendment)

Standing rule for this brief and every future surfaces brief /
template in this programme (full rule: PROGRAMME.md §4 gate 11):

- A **big** release carries a STAR + short name on ALL display
  surfaces. The star is ★ (U+2605), a plain text character (not
  an emoji), rendering on Java, Bedrock, Discord, and web.
- **Criteria (any one makes a release big):** a whole platform
  folds in; a new system players touch daily; or a headline
  announcement change. Ordinary folds/fixes are not big.
- **Surfaces that must carry it when big:** in-game What's New
  (ExcelWhatsNew) AND website What's New (two surfaces), the
  website generally, GitHub release titles, and announcements.
- **Format:** `v<N> ★ <ShortName>` — example format as recorded
  in D247: `v30 ★ Guilds`.
- **Exception:** jar filenames and `plugin.yml` / plugin
  internals stay PLAIN numbers — no star, no short name, no
  symbols in filenames.
- **Procedure:** the big / not-big verdict and, if big, the
  short name are decided and recorded at packet time, BEFORE
  surface text is staged, so every surface uses the identical
  form; a surfaces lane never invents a short name, and if no
  big-release decision is on record the release ships plain.
- Milestone-jump numbering was NOT adopted — numbers count up
  by one regardless of size.

**Application to THIS packet:** no record designates v24
(PartyDrops fold — a stateless module fold) as a big release,
so v24 ships PLAIN everywhere in this brief, exactly as staged
below. This section states the generic rule only; it does not
reclassify v24.

## In-game What's New (ExcelWhatsNew changelog.yml) — STAGED TEXT

Entry to add at install (version v24):

    - "Party drop bonus moved into ExcelEMFixes (no change to how it works — hunting natural elites together still earns up to 3x vanilla drops; solo is unchanged). /partydrops works as before."

## Website What's New + plugin row — STAGED TEXT

- What's New entry (winkreyesvalley): "ExcelEMFixes v24 —
  Party Drops: the natural-elite party drop multiplier
  (formerly the standalone ExcelEMPartyDrops plugin) now lives
  inside ExcelEMFixes as the party-drops module. Same rates,
  same /partydrops command; the standalone jar is retired."
- Plugin row update: ExcelEMFixes version 0.23.0 → v24,
  module count 25 → 26, add party-drops to the module list;
  remove/retire the standalone ExcelEMPartyDrops row if one
  exists (mark "folded into ExcelEMFixes v24").

## Records draft PR — OWED (close-out leg, do NOT push from build lane)

- wrv-plugins records draft PR for the v24 build (jar +
  source + this packet) — opened by the install close-out lane
  per the standing pattern (duplicate check first, Homebase
  git lane). Publication claim line goes in the close-out
  record.

## Guide — VERIFY-NO-CHANGE

- Guide is hibernated (since Nov 16, 2026): no knowledge/intent
  changes. Verified: the fold changes no player-facing
  behavior, command, or rate, so no Guide content is stale.
  Noted per programme gate 5.

## Tips + menus — CHECKED, NO CHANGE

- Broadcaster tips: checked — no tip references PartyDrops or
  the party drop rates in a way the fold changes (the behavior
  is unchanged). No tip edit owed.
- ExcelMenu / ExcelBedrockMenu: checked — no menu entry names
  the standalone plugin; /partydrops is an admin command with
  no menu surface. No menu edit owed.
