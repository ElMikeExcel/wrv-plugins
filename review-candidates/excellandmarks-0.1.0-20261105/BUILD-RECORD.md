# ExcelLandmarks 0.1.0 — BUILD RECORD

- **Built:** Nov 5, 2026 (America/Chicago), by Iris (build lane).
  Commissioned by Mike Nov 5, 2026, ~4:56 AM CST: "Yes to landmark" —
  build ExcelLandmarks per
  `~/workspace/village-identity/REMAP-DIRECTION-20261103.md`; the Nov 3
  price sketch adopted as config defaults the same day (rent
  $500/$1,500/$3,000 per month; buy $10,000/$25,000/$50,000). Mike's two
  menu directives followed the same morning (~4:59 AM and ~5:06 AM CST):
  tapping a landmark sign pops the spot menu through the server menu
  system, and the menu shows only the options available for that spot +
  viewer.
- **Tree:** `~/workspace/landmarks-build/` — SPEC.md (the build contract:
  direction doc + settled lapse policy of Nov 4, 2026 + the sign-menu
  directives in §6), src/ (19 classes: core rent clock / attribution /
  tap routing / menu model, GP + Protect hooks, Floodgate form +
  inventory menu renderers, service + store), tests/ (5 suites),
  build.sh, menu-entry/landmarks.yml (the "My Landmarks" compass door),
  rig/ (scratch Paper 26.3-235 rig, NMS fake players firing real
  events; evidence logs in rig/evidence/).
- **Candidate:** `ExcelLandmarks-0.1.0.jar`, sha256
  `466c14c4a4002b87fb72e399411355d4df442b8d19b6fcbc58122073521246dc`,
  77,996 B. build.sh assembles the jar twice and hash-asserts the pair;
  two full build.sh runs produced byte-identical jars.
- **Tests: 101,018 checks, 0 failures** — RentClock (100,904: ms-exact
  rent/grace boundaries, renewal stacking, 20,000-case randomized
  sweep), Attribution (29), TapRouter (11, incl. 299/300/301 ms
  debounce edges), SpotMenuModel (42, incl. someone-else's-spot =
  Close-only), Core (32, incl. Mike's adopted prices pinned as config
  defaults). Paper 26.3-235 rig, SEVERE 0: Boot A (Vault + EssentialsX
  + GriefPrevention 16.18.7 + ExcelProtect 0.1.1) 28/28 PASS — real
  `[Landmark]` sign registration, tap → menu → confirm with the exact
  $500 Vault charge, subdivision Build rights live, guest view-only,
  unregistered sign untouched, grace hold, lapse finalize with
  redemption contents exactly {5 diamonds, 3 emeralds, 2 glowstone,
  1 chest}; Boot B (GP + Protect jars ABSENT) 9/9 PASS fail-soft, hooks
  report OFF by name. Detail: TEST-REPORT.md + TEST-RESULTS.txt.
- **Rig-found defects, fixed pre-ship:** (1) GP subdivisions cannot
  change owners (`changeClaimOwner` throws NoTransferException) — the
  shipped rights model is TRUST (buyer gets Build + manager on the
  subdivision, verified by re-read; subdivision deleted wholesale at
  finalize; SPEC §5 amended). (2) The ExcelProtect logs-dir resolved
  one folder too high (`../ExcelProtect`), so attribution would have
  read nothing on the live server — fixed at both call sites.
- **Named rig limitation (not a failure):** the Bedrock form *send*
  path cannot be exercised by fake Java players; the form is built from
  the same test-pinned menu model and falls back to the inventory menu.
  The player proof is named in the packet (Mike, Bedrock, after
  activation).
- **Install:** filed PENDING in `~/workspace/wrv-install-queue/MANIFEST.md`
  Nov 5, 2026 (INSTALL-PACKET.md, gated: jar hash exact, census 78→79,
  registry 85→86); INSTALLED LIVE Nov 5, 2026, 7:05–7:23 AM CST by
  wrv-install-failsafe — see INSTALLED.txt. Activation (the in-game
  admin sign-placement pass + tier inventory) is a later step owned by
  Mike/the parent; the plugin is inert at spots=0 until then. Player
  proofs owed after activation: Mike (Bedrock) sign-tap form + rent;
  Jester (Java) chest menu + view-only; redemption-box owner-only open.
