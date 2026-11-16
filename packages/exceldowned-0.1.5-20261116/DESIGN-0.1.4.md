# DESIGN — ExcelDowned 0.1.4 (P-DOWNED-PROVIDER-1)

Base: verified 0.1.3 tree, base gate re-proven in this build folder
(unmodified rebuild byte-identical to the shipped 0.1.3 jar,
SHA-256 31f278bd…). Diagnosis of record:
`~/workspace/board-r416/downed-s6/S6-SCOPING.md` (RC-1, §5 fix scope).

## The defect (S6 RC-1, code-proven)

`TogetherPartyProvider.membersOf()` read a party's members by
reflective *method* lookup only (`getMembers`, `members`,
`getPlayers`, `players`, `getMemberIds`, `memberIds` via
`getMethod`). ExcelTogether's `PartyManager.Party` exposes its
members as a public *field* (`public final List<UUID> members`;
its only method is `size()`). Every lookup missed, every real
party read as empty: `partyMembersOf()` → `[]`,
`membershipOf()` → UNKNOWN, Resurrection PreCast always
cancelled, party DOWN/revived alerts silently suppressed.

## The fix — ONE production file

`TogetherPartyProvider.java` only. `membersOf()` now reads the
`members` FIELD first (public `getField`, then declared-field +
`setAccessible` fallback) and accepts a `Collection` value from
it, *before* the existing method-name list, which is kept
verbatim as the legacy/other-shape fallback. Fail-soft semantics
unchanged: any doubt still degrades to the empty list, and
`membershipOf`'s UNKNOWN-on-empty rule is NOT loosened (UNKNOWN
never counts as party). No change to `partyMembersOf` /
`membershipOf` logic, the `PartyProvider` interface,
`MagicBridge`, `ExcelDowned`, config, or spells. The optional
RC-2 `manager()` hardening and the `sameParty` belt-and-braces
route (S6 §5.1) are OUT of this minimal packet by design.

## Tests (S6 §5.2)

The 0.1.3 suite had zero provider tests. New cases in
`DownedTest` fake the *field* shape (a fake party whose members
are a `public final List<UUID>` field with no accessor methods,
sole method `size()` — mirroring `PartyManager.Party` exactly):
(1) real-shape `membersOf` regression test — fails on 0.1.3
code, passes on the fix; (2) `partyMembersOf` returns the other
member only; (3) `membershipOf` tri-state on the real shape
(PARTY / NOT_PARTY / NOT_PARTY-on-null / UNKNOWN-on-empty);
(4) legacy method-shape fake still read; (5) garbage/throwing
party fails soft, never throws. Provider seams: `membersOf`
invoked reflectively (so the negative control runs against the
pristine 0.1.3 provider file) and manager injection by
reflective field set — no production test hooks added.
