# ExcelRaidBuff 0.1.2 — SOURCE

**Ours, all of it.** ExcelRaidBuff was written from scratch for
Wink Reyes Valley (author: Argus) — design, core math, Bukkit
glue, config and tests are original work in
`ExcelRaidBuff-0.1.2-source.zip` (56,552 B, MD5
`8cc7a36f109cfaf53a3a64b731f1d3f2`, SHA-256
`ca8b8333f0e03361523d7d830cc19f35a17d7e212fa40a9741ce7dd324214c36`).

**What 0.1.2 adds (Mike's order, Oct 23, 2026, 7:28 AM CDT —
"the raid is still going; set a limit of after 6 hours it dies
down and they have to start over again"):** raid expiry, a hard
lifetime cap on every raid. Each raid's clock starts when the
raid triggers (or at first sight, for a raid already in
progress when the plugin enables) and is stored in
`expiry-starts.yml` in the plugin's data folder, so a server
restart does NOT reset it — the cap would be meaningless
otherwise, with the server restarting daily at 5 AM. The clock
is wall-clock: the hours the server spends down count like any
other hours. Fighting does NOT extend the cap — there is
deliberately no progress input to the expiry core (that is the
0.1.1 reveal's quiet clock, a different thing).

At the cap the raid DIES DOWN: the raid state is retired
first (the game's own raid stop, reached reflectively — the
plugin compiles against the Paper API only, the same seam
discipline as the EliteMobs bridge; the mechanism is the one
the 0.1.1 rig proved end-to-end), then every living member is
removed where it stands. Removed, never killed: no drops, no
XP orbs, no EliteMobs coins, no death events, and no Hero of
the Village — the raid is neither won nor lost, it is over,
and it has to be started over. Any reveal glow is cleared
first. One line to the players within 128 blocks of the raid
center (the reveal's announce radius): "The raid has died
down — the raiders have given up and gone." Plus a console
line the nightly combat analysis can find: `RaidBuff expiry:
raid <key> died down at the <cap> cap (age <age>) - N
member(s) removed, no loot, line sent to M nearby player(s)`.

Config: a new `expiry:` block (enabled default true / hours
default 6, clamped 0.01–168 so rig-sized values stay legal /
line), read live, `/raidbuff reload` safe — a reload that
disables the feature retires every clock, persisted file
included. A raid that ends on its own before the cap —
victory, defeat, despawn — is untouched beyond the janitor's
existing strip; its clock simply drops. Multiple raids are
tracked independently. One honest bound, stated plainly: a
raid first tracked mid-progress starts its clock at first
sight (the plugin cannot know when it truly began), and a
raid sitting in unloaded chunks is still seen by the sweep's
world raid list, so its clock also starts at first sight —
the 0.1.2 rig expired two such pre-existing world raids at
their first-sight cap with 0 members in loaded chunks, which
is the feature working as documented.

**Config discipline:** the bundled `config.yml` is the
CURRENT LIVE config bytes (the Oct 23 tuned columns incl.
level bonus III 5 / IV 10 / V 15 + the 0.1.1 `reveal:` block,
exactly as installed, 5,029 B) with ONLY the `expiry:` block
appended — verified: the tree config's first 5,029 bytes are
byte-identical to the installed live config, and its first
4,180 bytes are the elite-tuning record file itself. Every
value outside the new block is unchanged.

**Upstream, used via public API only (no fork, no shaded code):**
- **EliteMobs 10.9.10** by MagmaGuy — called reflectively at
  runtime through its public surface (unchanged from 0.1.0).
  EliteMobs is NOT on the compile classpath and NOT a plugin.yml
  hard dependency (softdepend only).
- **Paper API 26.2** (compile-time only, not distributed).

**Artifact:** `ExcelRaidBuff-0.1.2.jar`, 42,438 B, SHA-256
`009284dfcc91acbdcb237562d45fb79666f1c5c12cca15bc11507814d3e3149d`,
MD5 `bbc6e7e71ee04131034f7d05c35283ad`. Base discipline: the
untouched 0.1.1 tree rebuilt byte-identical to the shipped
0.1.1 jar before the delta (`build-0.1.2/BASE-VERIFICATION.txt`,
snapshot `build-0.1.2/base-0.1.1-tree-verified/`); the 0.1.2 jar
rebuilds byte-identically in-tree (A/B) and clean-room from
this source zip (both verified Oct 23, 2026).

**Build:** JDK 25, `javac --release 21`, manual route per
`src/ExcelRaidBuff/build.sh`; tests via `run-tests.sh`
(316 checks, plain-JVM harnesses: 275 carried from 0.1.1,
+ 14 expiry pins in RaidBuffConfigTest, + 27 new
ExpiryTrackerTest). Change scope vs 0.1.1 — by class-byte diff
against the shipped 0.1.1 jar: `ExcelRaidBuff` (adapter: trigger
listener, sweep expiry step, expire/stop path, persistence,
status line) + its strip runnable, `core/RaidBuffConfig`
(expiry keys + clamp), NEW `core/ExpiryTracker`, config.yml
(live bytes + expiry block), plugin.yml (version +
description); **every other class is byte-identical to the
0.1.1 jar — including `ExcelRaidBuffAPI`, so the
TrainingWheels compensation seam (Rylie's 0.25) is untouched,
and no damage path changed.** Excel signature: plugin name,
log prefix lines, `/raidbuff` command, PDC namespace
`excelraidbuff` and modifier keys `excelraidbuff:raid_health` /
`excelraidbuff:raid_damage`.

**Drive copies** (folder `Minecraft WRV`
`1BASUtSwJOFl7j608gfTfyxfbloUg8J5-`; MD5 read-back verified
Oct 23, 2026): jar in `Build files`
(`1c4f3TZDseHvgWXuN7SAD-PxST9xuVqgI`); source zip in
`Source` (`1twtEkZWxY3m-93vUJ_5zLuDYdXWBxJjz`); the file ids
are recorded in `BUILD-0.1.2.md`. This file and
`INSTALL-PACKET-0.1.2.md` are deposited in `Source` alongside.

**Verification summary:** 316/316 JVM checks (incl. expiry at
exactly the cap, no progress reset, restart survival via
snapshot/restore, per-raid independence, disabled parsing);
A/B in-tree builds and a clean-room rebuild from the
deposited source zip all byte-identical. Scratch-rig proof
(Paper 26.2-123 + the usual rig set, boots m/n/o/p): a raid
expired at exactly its test cap (72 s) with its glowing
members removed, no drops (crossbow/emerald/iron-axe census
0), no Hero of the Village, the line delivered to the one
nearby player and to no one else; a control raid won before
the cap paid Hero of the Village normally with zero expiry
lines; a raid's clock survived a graceful stop + relaunch and
expired at its original deadline (age exactly the 144 s test
cap, downtime counted, 7 members removed); with the feature
disabled a raid sailed past the would-be cap untouched. Zero
SEVERE in every boot. Full evidence is in `BUILD-0.1.2.md`.
Player proof owed after install: a real raid crossing 6 hours
and dying down (event-driven; nobody is asked to stage a
6-hour raid).
