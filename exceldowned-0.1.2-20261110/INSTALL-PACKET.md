# INSTALL PACKET — ExcelDowned 0.1.2 (console admin)

An Excel plugin by Iris / Mike for Wink Reyes Valley. Prepared
Nov 10, 2026. Status: BUILT + STAGED ONLY — do not install from
this packet without a proper install window under the standing
gates (the parent/failsafe installs; this desk packet installs
nothing). Commissioned from the console-admin coverage audit
(Nov 9, 2026) under Mike's Nov 3, 2026 console-admin rule: every
Excel plugin ships console-runnable admin commands for support
ops on a player's state. 0.1.1 was a FAIL: an operator could
inspect a downed player from console but had no revive/clear —
a stuck downed session could only bleed out. 0.1.2 closes it.

## Why

Verified from the 0.1.1 source (audit + this build's base
check): `/downed` offered console only inspection + reload,
both already gated by `exceldowned.admin`; no subcommand could
mutate another player's session. 0.1.2 adds
`/downed revive <player>` (force-resolve an online downed
session without death, through the existing spell revive path)
and `/downed clear <player>` (remove a downed session outright —
online or offline-durable — restoring nothing and killing
nobody), gated by the EXISTING `exceldowned.admin` (default op),
console explicitly allowed for those forms only. Full design:
DESIGN-0.1.2.md in the source zip.

## The artifact

- ExcelDowned-0.1.2.jar — 56,778 bytes
  SHA-256 2320e812d6c33a21f179b8e64d3f37723f260c97dbefc2eadf73cef97c519396
  MD5 d2bcc275d60cda833d0125a8d847d5ee
  Build EXC-DOWNED-0.1.2-WRV-20261110-01-d0wn3d
  Staged at ~/workspace/downed-012-build/ExcelDowned-0.1.2.jar
  (build folder root; build record + SOURCE record alongside).
- **Live-jar gate (what must be live before the swap):**
  ExcelDowned-0.1.1.jar — 47,199 bytes — SHA-256
  331e3b103ef07da6f81455f212d56c849916e1105232d0bb0b5bd472ae871d2f.
  If the live jar does not hash to this, STOP — the delta was
  built and verified against exactly this base.
- Delta vs live 0.1.1 (base tree verified to reproduce it
  byte-identically BEFORE the change): new AdminPolicy, the
  command routing + admin handlers in DownedCommands,
  adminRevive/adminClear/sessionByName in ExcelDowned (the
  spell revive's record and messages unchanged), plugin.yml
  (version, usage, descriptions). config.yml is byte-identical
  — no config edit, no data migration (downed.yml format
  unchanged), no permission grants required (console works at
  once; exceldowned.admin already exists at default op).
- Verification: DownedTest 27/27 (17 carried + 10 new); triple
  build reproduction (in-tree double-build, clean-room,
  source-zip) all byte-identical at the hash above; rig boot
  (Paper 26.3-235 + Magic 11.2.5): clean enable, Resurrect
  registered, 0.1.2 ready line (bleed-out 300 s, factor 0.25),
  zero SEVERE, 17/17 console behavior checks through the real
  console sender (rig-0.1.2/RIG-RESULTS.md).

## The swap (one jar; standard gates)

Preconditions: `list` = 0 twice per the standing gate (nobody
downed mid-swap in any case — the registry reloads from
downed.yml, but an empty server is the gate); guildgate LOCKED
count pre-flight as usual; marker discipline
(~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt at start,
deleted at finish, foreign-marker re-checks before Shut Down
and before Start); BACKUP FIRST with cap-at-6 (name prunees;
never the Daily Backup's newest copy or the two newest manual
restore points).

1. Stop the server fully (panel Shut Down — console `stop`
   auto-restarts on this host).
2. Rename the live jar: plugins/ExcelDowned-0.1.1.jar →
   ExcelDowned-0.1.1.jar.bak-20261110 (keep alongside; only one
   ExcelDowned jar loadable at a time).
3. Upload ExcelDowned-0.1.2.jar as plugins/ExcelDowned-0.1.2.jar
   (new filename — no rename-over dance needed). Panel has no
   checksum field: verify by exact size, 56,778 bytes, in the
   file listing.
4. NO config edit (0.1.2 ships a byte-identical config.yml; do
   not overwrite the live config), NO data change, NO spells
   packet change (Resurrection is untouched), NO permission
   grants required.
5. Start the server; run the boot-verify + console smoke below.

## Boot-verify (all must hold; any failure → rollback)

- "Enabling ExcelDowned v0.1.2" present.
- "ExcelDowned: custom Magic action 'Resurrect' registered."
- Ready line names **0.1.2** and build
  **EXC-DOWNED-0.1.2-WRV-20261110-01-d0wn3d**, bleed-out 300 s,
  downed factor 0.25; sessions-restored count sane.
- Zero SEVERE attributable to the swap; guildgate LOCKED count
  unchanged; MagicGate stage line unchanged; plugin count
  unchanged from pre-swap.

## Console smoke (from the Shockbyte console, immediately post-boot)

1. `downed revive NobodyHereXYZ` → "Downed admin: no player
   named 'NobodyHereXYZ' is online and no downed session exists
   for that name. Nothing was changed."
2. `downed clear NobodyHereXYZ` → the same unknown-name line.
3. `downed revive` → "Usage: /downed revive <player> | /downed
   clear <player>"
4. `giveup` → "Only a player can give up." (the player path
   still rejects the console — the allowance is the
   revive/clear branch only, by design)
5. With any player online standing: `downed <player>` → "…
   not downed" (legacy inspection unchanged) and
   `downed revive <player>` → "… is not downed; nothing to
   revive." (clean no-op). Do NOT smoke a real revive/clear
   outside the player proof below.

## Player proof (one volunteer — post-install)

1. Volunteer takes a controlled lethal hit → DOWN.
   Console `downed <volunteer>` shows the remaining clock;
   console `downed revive <volunteer>` → volunteer back at 50%
   health, standing, no death, no drops; the log carries
   "Downed admin revive by CONSOLE for …" and the records
   stream an `admin_revive` line.
2. Volunteer downs again; console `downed clear <volunteer>` →
   standing at half a heart (clear restores nothing), slowness
   gone, no death; volunteer heals up normally afterwards
   (regen/potions work again — the state is truly gone).
3. Regression pair (unchanged behavior): a party member's real
   Resurrection cast still revives a downed volunteer at 50%,
   and the /giveup button menu flow (Java menu + Bedrock form)
   still behaves exactly as the 0.1.1 proofs specced.

## Rollback

Stop; delete ExcelDowned-0.1.2.jar; rename
ExcelDowned-0.1.1.jar.bak-20261110 → ExcelDowned-0.1.1.jar;
start; verify the 0.1.1 ready line (build
EXC-DOWNED-0.1.1-WRV-20261108-02-d0wn3d) and normal down/revive
behavior. No data, config, or permission state was touched by
this swap; sessions in downed.yml load identically under 0.1.1
(any admin_revive/admin_clear lines in the records stream are
inert history there).

## Same-pass surfaces owed at install

Per the same-pass rule, closing this install also lands/verifies:
Guide knowledge + intents for the two admin forms
(operator-facing — confirm with the Guide owner surface);
What's New BOTH surfaces (website section AND the in-game
ExcelWhatsNew changelog) noting the console rescue for stuck
downed players in player terms; rotation tips checked (no tip
change expected — downed tips are player-facing and the player
surface is unchanged; state the check in the install report);
in-game menus checked (ExcelMenu + ExcelBedrockMenu — no menu
change: the /giveup confirm menu/form is untouched and the
admin forms are console/operator surface; state the check in
the install report).

## Records owed at install

INSTALLED marker + daily log, Route Sheet row, Big Brain (local
+ shared + Drive), records package already staged at
~/workspace/records-catchup-20261103/packages/exceldowned-0.1.2-20261110/,
and a one-line note closing the audit's ExcelDowned FAIL row
(console-admin audit §2/§4) once the player proof passes.
