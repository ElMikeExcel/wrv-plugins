# ExcelGuildGate 0.3.0 — INSTALL PACKET (narration)

**Status: BUILT, TESTED, SCRATCH-VERIFIED, DEPOSITED — NOT INSTALLED.**
Per Mike's standing rules this install rides the standard empty-server
auto-watch: it lands on its own the moment the server is empty, with
the gate checked twice, a marker against re-runs, and the rollback
below written in advance. Do NOT install while anyone is online.

**Sequencing note.** Live runs ExcelGuildGate **0.1.0** today, and the
0.2.0 install watch is armed and may land first. 0.3.0 is verified to
install **directly over 0.1.0 or over 0.2.0, whichever is live**: the
jar swap is the same either way, no data migration exists in either
step (state.yml is untouched by 0.3.0; the 0.2.0 `seen:` section, if
present, is read exactly as 0.2.0 wrote it), and the narration +
catch-up features run on built-in defaults under the 0.1.0 config and
on the merged config under 0.2.0. If 0.2.0 lands first, its packet's
steps are simply already done — do not redo them.

## The change in one line

Replace the live GuildGate jar with `ExcelGuildGate-0.3.0.jar`
(narration: narrator voice lines + stage guides + catch-up replay),
publish the narrator resource pack on the website, and point
`server.properties` at it. Voice approval from Mike is a **gate on
this install** — if he wants a different voice, run
`record-narration.sh` with `VOICE=` set first and use the NEW SHA-1 it
prints everywhere below.

## Step 0 — host the resource pack (website, do this first)

The Java client downloads the pack from a URL; the website's GitHub
Pages hosting is the stable home.

1. Add `wrv-guildgate-narrator-pack.zip` (377,104 bytes) to the
   website repo (`ElMikeExcel/winkreyesvalley`) at
   **`packs/wrv-guildgate-narrator-pack.zip`**, committed as a regular
   file — **NOT Git LFS** (LFS pointers would serve the wrong bytes).
2. After Pages deploys, the pack URL is:
   `https://elmikeexcel.github.io/winkreyesvalley/packs/wrv-guildgate-narrator-pack.zip`
3. **Verify the served bytes**: download that URL and confirm its
   SHA-1 is `e1ef3b835d59280e613ae435f6efca760389045e` (PACK-SHA1.txt)
   before touching server.properties. A mismatch means the upload was
   transformed — fix the hosting, not the hash.
   (If the pack was re-recorded for voice approval, every hash in this
   packet is superseded by the script's new output — use those.)

## Step 1 — the jar swap (empty server, standard rules)

1. In `/plugins`, confirm the live jar's exact filename
   (`ExcelGuildGate-0.1.0.jar` or `ExcelGuildGate-0.2.0.jar`). Rename
   it in place to the same name + `.bak` (the standing .bak rule).
2. Upload `ExcelGuildGate-0.3.0.jar` (38,447 bytes, SHA-256 in
   SHA256SUMS.txt) to `/plugins`.
3. Config: no change is required. Optional polish: merge the new
   `narration:` section from the packet's `config.yml` into the live
   one so the guide wording is visible/editable in place. (Under the
   0.1.0 config the defaults are identical; merging is never a
   correctness step.)
4. Do NOT restart yet if server.properties (step 2) is being edited in
   the same pass — one restart covers both.

## Step 2 — server.properties (same restart)

Set exactly (these are the 26.2 key names, read from the live file's
own shape — note `require-resource-pack`, not the old order):

```
resource-pack=https://elmikeexcel.github.io/winkreyesvalley/packs/wrv-guildgate-narrator-pack.zip
resource-pack-sha1=e1ef3b835d59280e613ae435f6efca760389045e
require-resource-pack=false
resource-pack-prompt=The valley has a narrator now. Accept to hear the seal falls spoken aloud — declining changes nothing else.
```

- `require-resource-pack=false` is non-negotiable: **declining the
  pack can never lock a player out.** A player who declines (or whose
  download fails) joins normally and gets the full text experience.
- Back up the original server.properties values before editing (the
  four keys' previous values; resource-pack was empty).

## Step 3 — restart and verify

1. Restart via the panel Shut Down button only (never console stop).
2. Startup log must show:
   - `Enabling ExcelGuildGate v0.3.0`
   - `ExcelGuildGate enabled: 0 of 5 seals broken, guild LOCKED`
     (the campaign MUST still read 0 of 5 — anything else, roll back)
   - `Join catch-up: on, N players tracked.`
   - `Narration: on, catch-up replay on, guide delay 2s (pack sounds wrv:guildgate.seal1..5)`
   - No ExcelGuildGate errors or stack traces. (If the campaign had
     legitimately advanced past 0 of 5 by install day, the line shows
     the real count instead — the check is "matches /guildgate
     status", not the literal 0.)
3. Console spot checks: `guildgate status` agrees with the log;
   `guildgate narrate 1` logs the seal1 dispatch + sends the 5 guide
   lines; `guildgate catchup <any known player>` answers as before.
4. Checksum discipline: hash the installed jar in place; it must
   equal SHA256SUMS.txt before this is called installed.

## Player spot checks (first real session after install)

- **Java**: join, accept the pack at the prompt, have Mike run
  `/guildgate narrate 1` — the player should HEAR the narrator and
  see the guide lines follow. Decline-path check on a second join or
  second account: declining still joins fine.
- **Bedrock**: join through Geyser — expect **no audio** (verified
  Geyser limitation, see README) but the guide text and join catch-up
  intact; confirm no pack prompt weirdness blocks joining.
- A genuine seal fall remains the end-to-end proof (kill → voice →
  guide → silent rejoin); it happens when the family earns it, and
  `/guildgate catchup <player> reset` + a rejoin demos the replay
  half any time.

## Rollback

1. Remove `ExcelGuildGate-0.3.0.jar`; rename the `.bak` jar back to
   its live name; restart. (0.1.0/0.2.0 read the same state.yml and
   config — narration leaves nothing behind to clean up.)
2. server.properties: clear `resource-pack=` and `resource-pack-sha1=`
   (restore the backed-up values), keep
   `require-resource-pack=false`; restart if not already restarting.
   The hosted zip may stay on the website harmlessly (a future 0.3.x
   reuses the URL) or be removed in the same website pass.

## Publishing notes (at install time, per the completion gate)

- **wrv-plugins record:** draft PR for ExcelGuildGate 0.3.0 (source +
  README/CHANGELOG refresh), left as an open draft per standing rule.
  If 0.2.0's PR is still open when 0.3.0 lands, note the supersession
  in both PRs honestly.
- **Website:** the pack hosting commit (step 0) plus the GuildGate
  entry updated with the 0.3.0 line (narration, the narrator, the
  narrate command, the Bedrock-audio limitation stated plainly), PR
  merged + byte-verified, and the served-pack SHA-1 re-verified after
  deploy.
- **Changelog bullet:** the 0.3.0 entry in the site changelog in the
  same pass.
- **Drive deposit (already done at build time, DEPOSIT-AT-BUILD):**
  jar + pack zip → Build files (`1c4f3TZDseHvgWXuN7SAD-PxST9xuVqgI`),
  source zip → Source (`1twtEkZWxY3m-93vUJ_5zLuDYdXWBxJjz`),
  size-verified at deposit. At install, no second deposit is needed
  unless the installed jar differs from the build (it must not —
  checksum step above).
