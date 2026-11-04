# ExcelTogether 0.5.0 — SOURCE.md

What this build is, where every byte came from, and who owns what.
Design of record: `~/workspace/party-mesh/DESIGN.md`. Base evidence: `~/workspace/party-mesh/BASE-RECOVERY.md`.

## The artifact

| | |
|---|---|
| Jar | `ExcelTogether-0.5.0.jar` |
| SHA-256 | `f4490814a9d4ec2e33540126adf6b418a0b58a50b27172119e45950d67353529` |
| Size | 149,568 bytes |
| Entries | 76 (74 classes + plugin.yml + config.yml) |
| Reproduction | `build.sh` in this folder rebuilds it byte-identical (asserted twice per run; verified across three separate runs) |
| Toolchain | JDK 25.0.5+8 (`~/workspace/.jdk25`), `javac --release 21`, Paper API 26.2 |

## Base provenance (D1: reconstruction first)

0.5.0 is built on the **reconstructed 0.4.3 tree**, never on the 0.3.0 folder alone:

- **0.3.0 anchor.** The surviving 0.3.0 source (`~/workspace/exceltogether-0.3.0/`) rebuilds byte-identical to its own shipped jar: SHA-256 `55a6b3eacdbf04754755a3703991a267b18740a5d0f3af363268fdc06768d4b2`, proven twice from a clean copy.
- **0.4.x delta recovered from the shipped jar.** The live-pulled 0.4.3 jar (SHA-256 `2bd4d99832ef06e03352cec8467e2fb530f5ba4094db3e0ff831a1d2a59f68aa`, 121,494 B; kept at `work/pulled-0.4.3.jar`) was fully inventoried: 62 classes, every per-class MD5 matching BASE-RECOVERY Appendix A, bundled plugin.yml/config.yml extracted verbatim. The 0.4.0–0.4.3 features (friendly fire + party drops, change-only range indicator, Bedrock % escaping, HP refresh, RewardGuard observed-grant hook) were reconstructed into source class by class, arbitrated by the shipped bytecode (`javap -c -p`) wherever the CFR decompile disagreed with it — CFR was wrong in at least three places (see TEST-REPORT).
- **Base proof (the gate the task set).** The unmodified reconstructed tree assembles into a jar **byte-identical to the shipped 0.4.3 jar** — same SHA-256, same size — proven twice by `work/build-base.sh`. Per-class: 48 of 62 classes compile byte-identical from the reconstructed source; 12 more are instruction-identical (only LineNumberTable/debug layout differs); 2 (`PartyRules.upperSet`, `PartyCombatDrops.notifyParty`) are instruction-identical modulo a continue-branch trampoline goto with zero behavioral content. The base-proof jar takes source-compiled bytes where identical and shipped bytes elsewhere (the hybrid rule below).

## Assembly / parity rule for 0.5.0

The 0.5.0 jar is assembled deterministically (sorted entries, fixed zip timestamp 2026-11-01, ZIP_DEFLATED):

- **45 classes** compile byte-identical to shipped 0.4.3 and are kept.
- **13 classes** the mesh design does not change are carried as **shipped 0.4.3 bytes** (their recompiled bytes differ only in debug layout / branch shape): `BarPolicy` ×4, `GeyserForms` ×2, `McmmoXpShare`, `PartyCombatDrops`, `PartyRules` ×4, `PartyText`.
- **16 classes** come from 0.5.0 source — the changed + the new: `Controller` ×3, `ExcelTogether`, `EmLink` ×4, `EmSync` ×3, `ReflectEmLink` ×3, `MagicLevelPartyLookup`, `PartyLookup`.
- Class inventory 62 → 74: exactly the 12 new mesh/API classes added, none removed (verified by jar diff).

Changed files vs the 0.4.3 tree, per DESIGN §1.1/§1.2 and nothing else: `Controller.java`, `ExcelTogether.java`, `plugin.yml`, bundled `config.yml`. Plus the five new files. Plus **one deliberate base change**: 0.5.0 adds an `onDisable()` (0.4.x had none) whose only job is `unregisterAll(this)` for the service registrations (API note §3c).

## Ownership split

- **Ours (this build):** the Together hub and all pre-0.5.0 behavior (unchanged bytes above), `EmSync` (the EliteMobs reconciler), `ReflectEmLink` (the EliteMobs adapter), `EmLink` (the adapter contract), `MagicLevelPartyLookup` (the PartyLookup provider), the PartyLookup publication incl. the ExcelMagicLevel bridge.
- **The contract (shared, verbatim):** `com.winkreyesvalley.excelmagiclevel.adapter.together.PartyLookup` — copied character-for-character from `~/workspace/magic-level/build/TOGETHER-API-NOTE.md` §3a (package, javadoc, single method). It is ExcelMagicLevel's package by design: the consumer defines the contract, the provider implements it.
- **Upstream, untouched:** EliteMobs 10.9.12 (no fork; its fully public/static `PartyManager` is driven reflectively at runtime, so Together carries no compile-time or class-load-time EliteMobs reference outside `ReflectEmLink`), mcMMO 2.3.003-WRV, Jobs Reborn, Paper. No upstream jar is modified, shaded, or renamed.

## EliteMobs desk answers (from the 10.9.12 bytecode; rig items settled without a rig)

- **R-1:** the boolean parameter of EM's create/invite/accept/leave is a **notify flag** — EM sends its own result message only when it is true. Adapter-driven calls pass `false` (`ReflectEmLink.EM_QUIET`), because Together has already told the player. 
- **R-4:** `Party.getId()` is a random UUID fixed at construction — `partyKey` is stable across membership changes.
- `Party.getLeader()` returns a UUID; `getMembersInDisplayOrder()` returns UUIDs, leader first. `PartyManager.getParties()` returns `Map<UUID, Party>`. `PartyManager.invite` does **not** auto-create a party. `PartyOperationResult` has exactly the 13 constants DESIGN §1.4 maps. Dungeon freeze events exist: `PlayerJoin/LeaveDungeonEvent`, `PlayerJoin/LeaveArenaEvent`, each exposing the player.
- Still open (player/rig proofs, listed in the packet): R-2 (EM's leader-promotion choice), R-3 (ghost-invite accept-popup behavior), R-5 (mirror-invite popup on the EM face), R-6 (party commands by Bedrock-style names).

## Interpretation calls (where the design left a reading to make)

1. **PartyLookup publication mechanics (the one real deviation — read this).** API note §3b/§3c as literally written (register our provider under our own `PartyLookup.class` in onEnable) **cannot connect**: Bukkit's `SimpleServicesManager` keys registrations by Class *object* (verified in the Paper 26.3 paper-api bytecode: `providers` is a `Map` keyed by Class), and ExcelMagicLevel loads its own copy of the interface from its own jar, so its `getRegistration(PartyLookup.class)` never sees ours. MagicLevel's own Phase-2 rig missed this because its stub plugin softdepended on ExcelMagicLevel and therefore shared *its* Class object. Proven on the 0.5.0 rig: with §3b as written, MagicLevel 0.2.0 — enabling after Together, softdepend satisfied — still reported no service and stayed on proximity. **Fix, contract unchanged:** `publishPartyLookup()` registers the provider under our Class as specified, and additionally registers a small reflective **bridge proxy** under ExcelMagicLevel's Class object (loaded through its plugin classloader) when that plugin is present; every bridge call lands on the same provider. With the bridge, the rig shows MagicLevel's awake line flipping to `share: party`. Fail-soft either way. The §3c publication log line is unchanged.
2. **Parties-enabled probe is deferred, not an enable-time gate.** DESIGN §9 lists an awake variant `OFF (EliteMobs parties disabled)`. On the rig, EliteMobs 10.9.12 was still loading configs seconds after its onEnable returned, and the probe read the static default `false` while Party.yml said `enabled: true`. An enable-time refusal would kill the mesh on exactly the boots where EM starts slowly — including, potentially, live. So: a `false` probe at enable never refuses; a recheck 30 s later stands the link down with one log line if parties really are disabled, and EM's own `DISABLED` drive result does the same immediately. The awake line therefore prints the ON segment in that situation (the mesh *is* on until the verdict lands); the stand-down line carries the §9 information truthfully instead.
3. **§2.3's removal sentence vs §11D/B8.** "EM members not in Together are removed with `emLink.leave(extra)`" is enforced only for *suppressed* extras (a deliberate removal Together ordered, still showing in EM under the same partyKey). All other EM extras are adopted in or wait-listed, never yanked by a poll — the only reading under which B8 (two full different parties sharing one member → no churn) and the deficiency rule (no polls create or destroy membership nobody asked for) both hold.
4. **L4 (leader leaves at the EM door).** Leaving EM alone never leaves the Together party: EM is a view, so the member is driven back in by materialisation/convergence. Only moving to a *different* EM party moves the Together membership (Case E).
5. **login-materialize=false** gates only *quiet* parties: a party already meshed interactively (or adopted/converged) stays live in `EmSync`'s live set; a party that has only ever existed on paper at login does not materialise until someone interacts.
6. **EmLink contract addition:** one default method, `inviteDriven(inviter, target)` (default `false`), so §5.2's accept flow can ask the adapter about the driven-invite ledger that §1.3 places in `ReflectEmLink`. No other contract change.
7. **Awake-line order.** §9 says the line "gains, in order" the publication and mesh segments; they are appended after the legacy segments (`... PartyLookup service: published.` last-but-one ordering as printed by the rig build: mesh segment, then the PartyLookup segment), keeping every legacy segment byte-stable for log comparisons.
8. **Bonus-note sentence.** §10's "One party everywhere…" sentence is appended to the in-game bonus note only when `mesh.em.enabled` is true in config *and* the EliteMobs plugin is present and enabled — the two facts knowable where the note is built. If the EM API then differs, the note over-promises while the awake line reports the mesh OFF; noted as a wart, accepted for this pass.
9. **plugin.yml description quoting.** The mandated 0.5.0 description sentence contains `0.5.0: ` — a colon-space inside an unquoted YAML scalar, which makes the whole descriptor invalid (the first rig boot failed to load the plugin for exactly this). The description is therefore single-quoted in plugin.yml; the *parsed* description is character-for-character the design's text. `build.sh` now parses both descriptors as a build gate so this class of failure fails the build, not the boot.
10. **Em-dash style.** The codebase's inherited test forbids em dashes in `.java` sources; the API note's provider class and the new mesh sources use hyphens in comments instead (comment-only change; the provider's code is otherwise the note's §3b unchanged). plugin.yml's description keeps the design's em dash — it is not a `.java` source and the design mandates the sentence verbatim.
