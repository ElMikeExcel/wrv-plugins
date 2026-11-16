# ExcelDowned 0.1.4 — BUILD RECORD (P-DOWNED-PROVIDER-1)

- Worker: board supervisor run 417, worker W7, Mon Nov 16, 2026
  (America/Chicago). Desk build only: no MANIFEST filing, no
  live contact, no mirror session (see MIRROR-PLAN.md).
- Fix scope: `~/workspace/board-r416/downed-s6/S6-SCOPING.md` §5
  (RC-1). ONE production file changes.

## Base gate — PROVEN (twice)

The r416 partial build
(`~/workspace/board-r416/downed-014-build/work/base-gate-0.1.3/`)
was assessed first: its log shows the untouched 0.1.3 tree
rebuilt byte-identical, 34/34 tests, fingerprint PASS. The gate
was then re-proven fresh in this run's tree
(`work/base-gate-0.1.3/`, log `work/base-gate-build.log`):

- `ExcelDowned-0.1.3.jar` — 59,875 B, SHA-256
  `31f278bda631fd6dde7cbce1eba267633f35e430489ea29c6437dc92544d9e93`
- Double-build byte-identical; suite 34/34; fingerprint gate
  PASS (build `EXC-DOWNED-0.1.3-WRV-20261114-01-d0wn3d`). The
  hash matches the live 0.1.3 lineage named in S6 §5.3.

## The fix — TogetherPartyProvider.java only

`membersOf()` now reads the **`members` FIELD first** — public
`getField("members")`, then `getDeclaredField` + `setAccessible`
— accepting a `Collection` value, *before* the existing
method-name list, which is kept verbatim as the fallback.
Fail-soft unchanged (any doubt → empty list; UNKNOWN-on-empty
not loosened). No change to `partyMembersOf` / `membershipOf`
logic, the `PartyProvider` interface, `MagicBridge`,
`ExcelDowned`, config, or spells. RC-2 hardening and the
`sameParty` route (S6 §5.1 options) are OUT of this minimal
packet. Patch: `TogetherPartyProvider.patch` (this folder).

Production diff vs the 0.1.3 tree (verified `diff -rq`): only
`TogetherPartyProvider.java` (the fix) and `BuildInfo.java`
(the 0.1.4 fingerprint fragments). `config.yml` identical.

- Fixed provider SHA-256:
  `c1403123f653b5007132e5a9501a0c54d3f632500faadfe59fcb2deb5818561f`
  (was `bc31a59f3508c772c8de21aa1df8c5b9f4d2baeb608d6826ca5d672206580b3b`,
  the S6-pinned 0.1.3 hash)
- Test file SHA-256:
  `d5e6e114de83a5ece9daed51e162412168b037d6183e9990337b006949c5cf7e`

## Tests (S6 §5.2) — field-shape fakes

New cases in `DownedTest` (suite 34 → 39): the fake party
mirrors `PartyManager.Party` exactly — `public final
List<UUID> members` field, NO accessor methods, sole method
`size()`. Seams: `membersOf` invoked reflectively (so the
negative control runs against the pristine 0.1.3 file) and
manager injection by reflective field set — no production test
hooks added. Cases: (1) real-shape `membersOf` regression;
(2) `partyMembersOf` returns the other member only;
(3) `membershipOf` tri-state (PARTY / NOT_PARTY /
NOT_PARTY-on-null / UNKNOWN-on-empty); (4) legacy method-shape
fake still read; (5) garbage/throwing party fails soft.

## Candidate

- `ExcelDowned-0.1.4.jar` — **59,998 B**, SHA-256
  **`703d7f9a45b4c0d00f49bfddb1f404bd5990601ae37e9b60e5f1937599792339`**
- Deterministic double-build byte-identical; full suite
  **39/39 passed, 0 failed**; fingerprint gate PASS
  (build `EXC-DOWNED-0.1.4-WRV-20261116-01-d0wn3d`).
  Build log: `work/build-0.1.4.log`.

## Negative control — PASS (defect isolated)

The 0.1.4 test tree with the PRISTINE 0.1.3 provider restored
(SHA-256 `bc31a59f…80b3b`, byte-identical to the base gate)
fails on exactly the 3 real-shape tests — 36 passed, 3 failed
(log: `work/neg-control.log`):

- `realShapeFieldPartyMembersAreRead` — expected 2, was 0
- `partyMembersOfRealShapeReturnsOtherMemberOnly` — expected
  the party-mate, was `[]`
- `membershipOfRealShapeTriState` — expected PARTY, was UNKNOWN

The legacy-shape and garbage fail-soft tests pass in both
worlds, as designed (they pin the fallback and the fail-soft
contract, not the fix).

## Trees

- `work/base-gate-0.1.3/` — untouched 0.1.3 tree + rebuilt jar.
- `work/build-0.1.4/` — the 0.1.4 tree of record (src, tests,
  `DESIGN-0.1.4.md`, `local-build.sh`, candidate + repro jars).
- `work/neg-control/` — negative-control tree (0.1.4 tests +
  pristine 0.1.3 provider).

## Not done here (by dispatch scope)

No MANIFEST filing, no mirror session, no live contact.
Pre-filing gate = MIRROR-PLAN.md (Together AT-1 re-run, then
Downed CAST-1 + N3 + N4). Sibling flag from S6 §5.4 stands:
ExcelSummons carries a byte-identical defective `membersOf()` —
its own ticket, NOT absorbed by this packet.
