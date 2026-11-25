# ExcelCalmBars

**XP bars wait until the fight is over.** ExcelCalmBars holds back the
XP-gain boss bars from Jobs, mcMMO and EliteMobs skills while a player
is in combat — and for a quiet period after the last hit (30 seconds
by default) — then shows them as normal. It exists because of a player
report: in back-to-back fights the XP bars stack up at the top of the
screen and block the view exactly when the player needs to see.

Real boss bars, quest bars, and standing status bars (such as the
ExcelDeathProtection "Inventory Protected" bar) are **never** held.
Only bars whose titles positively match the Jobs, mcMMO, or EliteMobs
skill-bar formats are gated; everything else always passes.

Requires: Paper 26.x. No dependency on Jobs, mcMMO, or EliteMobs — the
gate works at the packet level and fails open if anything is absent or
changes: every decision path passes the packet on any error, a held
bar is released at the end of the quiet period or the max-hold cap
(default 120 s), whichever comes first, and death, world change,
logout, and plugin disable all release or drop held bars cleanly.

## Install

1. Put `ExcelCalmBars-0.1.0.jar` in your `plugins/` folder and restart.
2. Open `plugins/ExcelCalmBars/config.yml`: `enabled`,
   `quiet-period-seconds` (default 30), `max-hold-seconds` (default
   120), `debug-logging`, the never-hold substring list, and the three
   title-pattern families (Jobs / mcMMO / EliteMobs skill bars).

## Commands

- `/calmbars status` — gate state, quiet period, boss packets seen,
  bars currently held (player form; from console it points you here).
- `/calmbars flush` — release everything held, right now.
- `/calmbars reload` — reload the config (console).

## How it works

Combat state comes from Bukkit damage events (dealt or taken,
projectile shooters resolved) — no dependency on any plugin's combat
API. One Netty handler per player connection watches boss-bar packets
before encoding; a bar is an XP bar only when its plain title matches
one of the three pattern families (patterns derived from the live
plugins' own title formats). While the player is in combat or inside
the quiet window, XP-bar packets are merged into per-bar state and
held; when combat starts, XP bars already on screen are retracted.
Release rebuilds the bar from the merged state. All server-internal
access is reflection-only, so a future server build that moves the
internals makes the plugin report itself unavailable and gate nothing
rather than break.
