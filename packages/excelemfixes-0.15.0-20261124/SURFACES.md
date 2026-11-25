# SURFACES — ExcelEMFixes 0.15.0 (skills-menu)

Same-pass surfaces for the 0.15.0 install window. The jar + config land via INSTALL-PACKET.md; the items below ride the same window / parent follow-ups.

## In-game What's New (ExcelWhatsNew) — TEXT entry, companion live-edit

Entry id: `2026-11-24-skills-menu` · Date: 2026-11-24 · Suggested category: Menus

> **Your skill bonuses got a proper screen 🎉**
>
> ✨ NEW: **/skillmenu** — your skills and bonuses, made for your device
>
> 📱 Bedrock players get a real form — no more clunky chest screen
> 🖱️ Java players get the chest menu, same as always
>
> 🔓 See every bonus, what's unlocked, and what level it needs
> 👆 Tap a bonus to switch it on or off — up to 3 per skill
>
> 🧭 Also inside **/emguide** → Skills → My skill bonuses

(Player-announcement format per Mike's Nov 23, 2026 ruling: hook first, one idea per line, blank line between sections.)

## Website (winkreyes.com / wrv-site)

- Plugins table: ExcelEMFixes row **0.14.0 → 0.15.0**, note: "skills-menu module — native /skillmenu screen (Java GUI + Bedrock form) for EliteMobs skill bonuses; toggles run through EliteMobs' own selection state."
- Wherever the site documents skill bonuses / getting started: name **/skillmenu** as the way to switch bonuses on, and /emguide → Skills as the explainer. (The website is the full-detail canon the Guide answers from — keep the wording in step with the bundled clarity text.)

## In-game Guide / clarity text

Ships **inside the jar** (bundled clarity-menus.yml): /emguide Skills page gains the "My skill bonuses" button + route, the How-to text leads with the native screen, and the Claim button is relabeled "(EliteMobs' screens)". No file placement at install; the packet's gate 3 confirms no data-folder clarity-menus.yml override exists on live. Guide proper remains hibernated (Nov 16) — no Guide change.

## GitHub (wrv-plugins)

- Records close-out per the standing pattern: draft PR with the 0.15.0 source + a proper detailed changelog (module, mechanism, EM-sequence fidelity, rig proof incl. the DB read-back, Bedrock proof owed to the post-install PC-Bedrock run — Iris via Mike's PC client as .MikeExcel; Marvin's walk is a bonus confirmation). Draft stays open, never merged.

## Discord / player asks (parent follow-ups at install)

- **Jester (Java)** — proof ask: /emguide → Skills → "My skill bonuses"; switch one bonus on and off; peek at EliteMobs' own alt-menu Skills screen and confirm both screens agree. (💃🏾-first DM, no sign-off, per standing format.)
- **Bedrock form proof — owed, run by Iris (not a player ask):** post-install, Iris drives the Bedrock client on Mike's PC (PC agent), joins the live server as .MikeExcel, opens /skillmenu, and verifies the Cumulus form renders and a bonus toggle performs the EM activation and reads back active server-side in EM's player_data.db — the same read-back standard as the Java rig proof. Mike has Bedrock on his PC (his direction, Nov 24, 2026, 4:18 PM CT), so this proof does not wait on a player. **This is the owed Bedrock proof for this build** — the rig could not run Geyser inside the build VM (see INSTALL-PACKET.md, Proof).
  - **STATUS Nov 24, 2026 (evening run): NOT DISCHARGED — blocked at the join step, proof still owed.** Iris drove the PC Bedrock client (v26.65, signed in as MikeExcel) through three join routes — LAN 10.0.0.84:20795, winkreyes.duckdns.org:20795, Tailscale 100.72.156.1:20795 — and all three failed with the Bedrock "Terracotta" error (InitialConnection-149) before any login reached the server. Server side proven healthy from the same machine (RakNet PONG from Geyser on 127.0.0.1 and 10.0.0.84 from a normal process); the failure is the Windows app-container loopback-isolation class — the host PC's own Bedrock app cannot connect to its own host. .MikeExcel's EM data verified byte-identical before/after (never joined; nothing to restore). Unblock paths: CheckNetIsolation loopback exemption (admin, needs Mike's go), a Nethernet relay entry (featheredpineapple.wdn.gg:443), or a second LAN device. Full record: `~/workspace/em-native-skills-menu-20261124/BEDROCK-PROOF.md`. This is G1 tooling-gap work, not a 0.15.0 defect.
- **Marvin (Bedrock)** — bonus confirmation ask (not the gating proof): type /skillmenu; confirm a native form opens (not a chest/container), the buttons show his levels and [ON] state, and a tap switches a bonus with EliteMobs' message arriving.
- No announcement post beyond the What's New entry + the two proof-ask DMs; the nudge message players may receive already routes to /emguide's Skills page (0.13.0), which now leads with the new screen.
