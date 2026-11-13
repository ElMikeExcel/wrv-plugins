# INSTALL PACKET — ExcelSummons 0.1.8 (Bedrock buttons fixed + out-now counts)

Status: STAGED, NOT INSTALLED. Filed by the parent into the
install queue; the installer (failsafe lane or parent hand
window) runs this packet. Commission:
`../../COMMISSION-018.md` — Mike's live findings Nov 12, 2026,
6:51–6:52 PM CST (the 0.1.7 Bedrock form rendered but every
button was dead; plus his out-counts request).
Build record: `../BUILD-RECORD.md`. Tests: `../TEST-RESULTS.md`
(94/94). Rig: `../RIG-RESULTS.md` (phases A + B, SEVERE 0,
synthetic form RESPONSES driven through the attached handler
against the real Geyser Cumulus classes for every button of
all three forms; a real Bedrock client's tap is still Mike's
live proof — see Acceptance).

## Payload

1. **ExcelSummons-0.1.8.jar** — 92,387 B — SHA-256
   `835419aaeff434b61e6be2f5cda1340233299e11262feebf96206cdd00bb8841`
   — build `EXC-SUMMONS-0.1.8-WRV-20261112-01-f8d6ba`
   → placed as `plugins/ExcelSummons-0.1.8.jar` (its own
   filename, the house jar-swap pattern); predecessor
   `plugins/ExcelSummons-0.1.7.jar` renamed in place to
   `ExcelSummons-0.1.7.jar.bak-<install date>` and held.
2. **Nothing else.** NO ExcelMenu entry change (the Summons
   door from the 0.1.7 window is already live; the registry
   stays 94). NO config.yml change (byte-identical to 0.1.7 —
   SHA-256 `659d9e2c643189c9f65e243c1eb8517c7f5d4775a7cad5ed9cb2502662f3c3a9`).
   No other plugin's files. No Magic changes. No data
   migration (the registry format is unchanged from 0.1.7).

## What the jar changes (two things, commission-exact)

- **Bedrock form buttons work.** BedrockForms is replaced with
  the live-proven WandBars shape: Geyser class loader only
  (the Floodgate-first attempt is gone), NEW Cumulus
  (`org.geysermc.cumulus.form.SimpleForm`), response handling
  via `FormBuilder.resultHandler` unwrapping
  `ValidFormResponseResult.response()` →
  `SimpleFormResponse.clickedButtonId()`. Handler/response
  failures log at WARNING — nothing is silent anymore. The
  send API and the chat fallback are unchanged.
- **Out-now counts on the main page.** The main page (Bedrock
  body + Java info book) shows mounts out · cast pets out
  (wolf/cat/familiar) · combat summons out · total cast out,
  derived from the page's cast entries (adopted pets are never
  counted), always shown (zeros print), next to the held line:
  `Out now: 1 mount · 1 cast pet · 2 combat summons (4 total)`,
  or `Nothing out right now.` when nothing is cast out.
- Everything else is 0.1.7: adopted = 0% mana, caps, dismiss
  scopes, the confirm-gated per-pet adopted dismiss as the
  ONLY adopted-dismiss door (no dismiss-all-adopted exists).

## G1 — fresh premise pull (STOP on any mismatch)

At window open, pull FRESH via the PC lane (panel File Manager
is the labeled fallback):

- `plugins/ExcelSummons-0.1.7.jar` must be **90,538 B /
  SHA-256 2fc1a226f11a25d4d54e5b11ce0f663a9bec1500f3ae79ff0f4cd60ed4475d56**
  (the live 0.1.7, verified at its install Nov 12, 2026).
  Any other size/hash = STOP, re-diagnose.
- `plugins/ExcelMenu/entries/summons.yml` must EXIST (the
  0.1.7 door); the ExcelMenu registry must read **94** entries.
  This packet does not touch it either way.
- ExcelSummons data premise: `plugins/ExcelSummons/config.yml`
  unchanged (this packet does not touch it); the registry data
  file carries forward as-is.

## Window discipline

- ONE Shockbyte backup at window open (the window's only
  backup), named per house pattern ("Summons 0.1.8 pre-install
  <date>"). **Cap-at-6:** prune the OLDEST manual backup first
  if the post-creation total would exceed 6; never delete the
  Daily Backup's newest copy or the two newest manual restore
  points; name any prunee in the install report.
- **Emptiness gate:** `list` = 0 twice before the swap (a jar
  swap — fully gated, NOT a text live-edit). Window marker
  discipline per the standing rules; the failsafe re-checks
  the marker before Shut Down and before Start.
- Lane: PC route primary (rclone/SFTP transfers + hash
  verification), panel only for the backup + Shut Down/Start;
  automatic panel fallback with the one-line fallback notice
  if the PC route is down.
- Swap: place the candidate jar, pull back and hash-verify
  EXACT (835419aa…8841) before boot; rename the predecessor
  to its .bak name.

## Boot verification (all required)

- Log: `Enabling ExcelSummons v0.1.8` and the enable line
  "ExcelSummons 0.1.8 enabled (build
  EXC-SUMMONS-0.1.8-WRV-20261112-01-f8d6ba); registry holds
  N summon(s)." (N = the live count at boot; 272 at the 0.1.7
  window — report the actual).
- Plugin count **83**; guild **LOCKED 4 of 5**; **SEVERE 0**.
- NO `/excelmenu reload` is required (no entry change); the
  registry stays **94**.
- Console smoke: `/summons menu` from console returns the
  players-only line (no stack trace); registry count unchanged.
- Server left ONLINE.

## Acceptance (post-install, closes the commission)

1. **Mike's Bedrock tap test (THE acceptance — a real client
   is the one proof the rig cannot give):** compass → My
   Summons → the main page shows the out-now counts line;
   tap **Adopted pets (N)** → the sub-page OPENS (this tap was
   dead in 0.1.7); tap a pet → the confirm form opens; tap
   **Keep** → back on the sub-page, nothing dismissed; tap
   **Back** → the main page returns. Dismiss-scope buttons
   act and re-open the main page.
2. No player-visible change to `/summons list`, dismiss
   scopes, mana holds, or caps — any report of one is a defect
   against this packet, not a tuning note.

## Same-pass surfaces (after the install verifies)

- **What's New, BOTH surfaces:** one entry for 0.1.8 — the
  Bedrock Summons buttons fixed (they rendered but did
  nothing in 0.1.7) + the new out-now counts on the Summons
  page. Website section AND the in-game
  `ExcelWhatsNew/changelog.yml` (via
  `~/workspace/whatsnew-pipeline/registry/whatsnew.yml`), the
  in-game file verified by live pull, never assumed.
- **Guide:** knowledge + intents note the Summons page's
  Bedrock buttons now work and that the main page shows the
  out counts (mounts / cast pets / combat). No behavior claims
  change (scopes, mana, adopted rules are 0.1.7's). ExcelGuide
  SRC synced in the same pass.
- **Website:** index.html + ai-knowledge.json Summons section
  updated in the same pass (browser-route PR, merged +
  byte-verified per the standing routing).
- **Records:** wrv-plugins record package for 0.1.8 (source
  zip + records, jar to Drive only per house pattern) as an
  OPEN DRAFT PR; Drive deposit MD5-verified.
- **Tips / menus check:** broadcaster tips — CHECKED, no new
  tip required; in-game menus — NO menu change in this packet
  (the door is 0.1.7's).
- **Jester note (owed with the landing):** a short Discord
  note that the Summons page's Bedrock buttons now work end
  to end (the Adopted page opens, the confirm flow runs) and
  the page now shows his out counts. Parent/supervisor sends.

## Rollback

Restore the window's backup, OR (jar-level): stop, remove
`ExcelSummons-0.1.8.jar`, rename
`ExcelSummons-0.1.7.jar.bak-<install date>` back to
`ExcelSummons-0.1.7.jar`, start, verify
`Enabling ExcelSummons v0.1.7` + 83 plugins + SEVERE 0. The
registry data format is unchanged from 0.1.7, so a jar
rollback loses nothing (it only re-breaks the Bedrock buttons
and drops the counts line).
