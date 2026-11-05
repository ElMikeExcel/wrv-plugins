# ExcelLandmarks 0.1.0 — source notes

- **Origin:** first-party Wink Reyes Valley code, written fresh for this
  version in `~/workspace/landmarks-build/` from the settled design
  contract SPEC.md (itself written from
  `~/workspace/village-identity/REMAP-DIRECTION-20261103.md` + Mike's
  Nov 5, 2026 menu directives). No base version exists; 0.1.0 is the
  first release, so this record carries no FUNCTIONAL-CHANGE.diff.
- **Design inputs, all settled before the build:** spawn is a premade
  pasted build; the admin `[Landmark]` sign is the whole registration
  interface; LANDMARKS = naming only, PLOTS = naming + a GP
  subdivision; prices config-keyed with Mike's adopted defaults; lapse
  policy (7-day grace → pack contents + tally materials from
  ExcelProtect attribution + clear + never-expiring redemption box)
  settled Nov 4, 2026; sign tap pops the menu, viewer-available options
  only, confirm before any charge (Mike, Nov 5, 2026).
- **Menu stack note (verified against source, not assumed):**
  ExcelMenu's public API is entry-registration only, so the spot menu
  renders on ExcelMenu's own dual stack — one core model (`SpotMenuModel`)
  feeding the Floodgate/Cumulus form renderer (Bedrock) and the chest
  inventory renderer (Java) — while the compass "My Landmarks" door IS
  a true ExcelMenu registry entry (`menu-entry/landmarks.yml`).
- **Two rig-found fixes are part of this source, not patches on it:**
  (1) the GP rights model is TRUST (`hook/GpAdapter` grants Build +
  manager; title transfer is impossible — NoTransferException);
  (2) `hook/ProtectAttribution` resolves the ExcelProtect logs dir
  against the plugins folder correctly (the first cut resolved one
  folder too high and would have read nothing live; both call sites
  fixed).
- **Toolchain (this VM):** JDK 25 (Temurin 25.0.4.1,
  `~/workspace/.jdk25`), `javac --release 21`. Compile inputs are
  hash-pinned in build.sh and DEPENDENCIES.sha256: Paper API 26.3
  build 231, Vault 2.0.0, GriefPrevention 16.18.7, Floodgate (the live
  server's floodgate.jar), plus the Paper runtime library set from
  `~/workspace/excelbuildmode-pilot/runtime-server/libraries`.
  build.sh gates on the descriptors parsing (plugin.yml name/version,
  config.yml, menu entry) before compiling, runs the five test suites,
  then assembles the jar deterministically (fixed zip timestamps,
  sorted entries, two assemblies hash-asserted per run).
- **Ownership:** all first-party Wink Reyes Valley code. Compile-only
  deps: Paper API + its library set, Vault API, GriefPrevention API,
  Floodgate API (form rendering). Runtime deps: Vault economy
  (EssentialsX on the live server); GriefPrevention + ExcelProtect are
  soft hooks — absent, the plugin runs naming-only (fail-soft).
