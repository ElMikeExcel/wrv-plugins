# BUILD-RECORD — ExcelAutoSort 0.2.1 (admin per-player surface)

Built Nov 9, 2026 (Iris, desk build lane for the WRV board
supervisor), commissioned by Mike Nov 9, 2026, ~8:07 PM CST.
**Not installed.** The install packet is `INSTALL-PACKET.md` in
this folder; the MANIFEST entry is drafted in
`MANIFEST-ENTRY-DRAFT.md` for the supervisor to file after
verification (this lane did not touch the MANIFEST, the
parent-backlog, or board state). Records staged at
`~/workspace/records-catchup-20261103/packages/excelautosort-0.2.1-20261109/`;
GitHub/Drive legs are the parent's (see `PUSH-NOTES.md`).
The 0.2.0 tree at `~/workspace/autosort-plugin/` is untouched —
its jar still hashes to the 0.2.0 record value (verified at
close-out). Design of record: `SPEC.md` §16 (appended to this
build's SPEC copy; §§1–15 unchanged).

## The defect (why 0.2.1 exists)

0.2.0's admin surface was only `prune|reload` — no player-targeted
status or revoke — violating Mike's Nov 3, 2026 console admin
rule: every player-targeted state an admin may need to inspect or
correct must be reachable from the console, permission-gated,
live-effect, and persisted through the plugin's own data path.
Max Organization entitlement was exactly such a state: staff
could see it only by reading `data/maxorg.yml` by hand, and could
not correct it at all short of editing the file with the server
stopped.

## What shipped (SPEC §16 — the whole change, nothing else)

- **`autosort admin status <player>`** — prints the target's
  auto-sort ON/OFF; Max Organization state as OWNED (permanent) /
  RENTED with the exact expiry instant + time left / LAPSED with
  the expiry instant / NONE (not unlocked); filter chests owned;
  recovery-box stacks. Read-only.
- **`autosort admin revoke <player>`** — removes the entitlement
  ENTIRELY in one action: the permanent buy flag AND any rent
  (active or lapsed) are cleared together; the record leaves the
  store map and `data/maxorg.yml` in the same pass
  (`DataStore.removeMaxOrg`), so the live access check flips in
  the same tick and a restart loads the same truth. No money
  moves — a refund is a manual staff economy action (SPEC §15.2),
  never a plugin action. Revoking a player who holds nothing is a
  clean no-op that reports "nothing held" and logs nothing.
  Successful revokes are Protect-logged
  (`autosort_maxorg_admin_revoke`, before-state carried,
  `via: admin`).
- **`autosort admin grant <player> max`** — INCLUDED, by symmetry
  with ExcelCosmetics. **Decision + reasoning:** ExcelCosmetics'
  admin surface is `grant|buy|revoke|status|setexpiry|reload`,
  where `grant` is the no-charge hand grant and `revoke` its
  mirror; the pair exists so staff can both remove AND repair an
  entitlement from the console. A revoke-only AutoSort would leave
  a wrongly-revoked (or otherwise lost) Max Organization
  entitlement unrepairable except by faking a purchase through the
  economy. The grant therefore ships: no charge, lands in exactly
  the state a buy lands in (`MaxOrgCore.afterAdminGrant` ==
  `afterBuy` in shape — owned, rent timestamp spent), persists
  through the same data path, and is Protect-logged as an admin
  grant (`autosort_maxorg_admin_grant`, `via: admin`) — never
  dressed as a purchase. Granting an existing owner changes
  nothing and says so. (No `setexpiry` analogue: no named support
  case needs one — revoke + grant covers correction; §16.3.)
- **Target resolution (the ExcelCosmetics rule):** exact online
  name first, then the stored record of a player who has actually
  played here; a never-joined name is refused, so a typo can never
  touch a phantom account. Offline targets work (state is
  UUID-keyed in the plugin's own store).
- **Gates on the surface itself:** all three subcommands sit
  behind the existing `excelautosort.admin` permission (default
  op) inside the existing admin branch — console-runnable, and a
  non-admin player is refused by the same deny path as
  prune/reload (rig-proven, below). Tab completion covers the new
  subcommands, online player names, and the `max` token. The
  `/autosort` usage line (config + code fallback) now names
  `admin prune|reload|status|revoke|grant` — that one config line
  is the packet's only config merge.
- **Pure-core discipline kept:** the entitlement math lives in
  `MaxOrgCore` (`holdsAnything` / `afterRevoke` /
  `afterAdminGrant`), the Protect field table in `ProtectEvents`
  (`maxOrgAdmin`), the Bukkit service (`MaxOrgService.adminRevoke`
  / `adminGrantMax`) only executes + persists + logs. No new
  files; no behavior outside §16 changed — the purchase path,
  network view, filters, sort engine, and Bedrock surfaces are
  byte-for-byte the 0.2.0 logic.

## Tests

- **Unit: 35/35** (`tests/.../AutoSortTest.java`; all 29 carried
  0.2.0 tests pass unmodified, plus 6 new: revoke clears owned and
  rent at once (expiry zeroed, reads NONE not "lapsed");
  revoke-of-nothing is a clean idempotent no-op; a LAPSED record
  still holds something a revoke removes; the admin grant is
  permanent and lands in exactly the buy shape (incl. over a live
  rent); the admin Protect shapes carry their fields with
  `via: admin`; the admin surface is declared in plugin.yml
  (version 0.2.1, `excelautosort.admin` default op) and named in
  the shipped usage line).
- **Behavior rig, phase 6 (NEW, `rig/drive-admin.py`): 21/21**
  (Nov 9, 2026; Paper 26.3-231 sandbox, real GriefPrevention
  16.18.7 + ExcelProtect 0.1.7 + PlaceholderAPI, clientless fake
  players; every admin action dispatched from the real console
  sender through the production command path). Proven:
  console status renders a RENTED player (expiry instant + "29
  days left"), a NONE player, and an OWNED player after grant;
  **grant flips the live access check to OWNED and persists**
  (`maxorg.yml` read back off disk mid-boot carries the player
  with `owned: true`); a second grant reports the no-op;
  **revoke of the OWNED player flips the live check to NONE and
  removes the record from the file**; **revoke of the RENTED
  player cancels the rent outright** (entitlement reads NONE,
  file clean); a repeat revoke reports "nothing held" and changes
  nothing; a non-admin player's revoke AND grant attempts are
  refused and leave both targets' states untouched; a
  never-joined target is refused and no phantom record appears;
  the Protect items stream carries exactly 1
  `autosort_maxorg_admin_grant` + 2 `autosort_maxorg_admin_revoke`
  lines (no-op attempts publish nothing); the awake line names
  0.2.1 + build `EXC-AUTOSORT-0.2.1-WRV-20261109-01-c36331` with
  protect sink BOUND / gp rights BOUND. Evidence:
  `rig-evidence-admin/` in this folder (boot log, final
  `maxorg.yml` — sole surviving record is the permission-gate
  re-seed, header stamped with the 0.2.1 build ID — and the
  Protect items file).
- **Full regression rig (`rig/drive.py`, phases 1–5, the complete
  0.1.0 + 0.2.0 acceptance set against the 0.2.1 jar): 75/76** —
  with the one miss proven a rig-side flush race, not a defect:
  `M_rerental_restores_instantly` counted 1 GuestBot rent line in
  Protect's stream 8 ticks after the phase-5 re-rent click and
  wanted 2; the final evidence stream
  (`rig-evidence/maxorg-items-2026-11-09.jsonl`) carries BOTH
  $1,000 rent lines (21:07:05 phase 4; 21:08:20 phase 5 — the
  second line's timestamp is the very second of the verdict), and
  the case's other three legs all passed (balance 18000 =
  20000 − 1000 − 1000, access RENTED, network view open). This is
  the identical asynchronous-writer race the 0.2.0 record
  documents for runs 9/10. In substance the regression set is
  76/76; the tally is reported as run, with this evidence.
- **Deterministic double-build: byte-identical** (the build
  script's own two jars share the SHA-256 below), fingerprint
  gate PASS.

## Artifacts

- `ExcelAutoSort-0.2.1.jar` — 159,240 B — SHA-256
  `48a5c0fed431fc9853e0e92ff199b24887ab18485a5febb4132735759617e266`
  (+ `-repro.jar`, identical)
- `ExcelAutoSort-0.2.1-source.zip` — 94,174 B — SHA-256
  `99f8371e49db29fb0f8c9810238307a67b5847cffc7d74dc774eaa3a784c26c0`
  (40 files — the same file count as the 0.2.0 source zip: src/ +
  tests/ + plugin.yml + config.yml + local-build.sh; timestamps
  pinned 2026-11-09)
- Build ID `EXC-AUTOSORT-0.2.1-WRV-20261109-01-c36331`;
  fingerprint gate PASS; semantic markers carried forward + the
  admin-correction dialect added (FINGERPRINTS.md marker 9).
- 0.2.0 / 0.1.0 artifacts untouched (hashes re-verified at
  close-out: 0.2.0 jar `6164d943…`, 0.1.0 jar `00d01234…`).

## Named post-install player proofs (NOT rig-claimed)

- **Staff proof (Mike, console):** one real
  `/autosort admin status <player>` on a family account whose
  Max Organization state is known, matching the player's own
  `/autosort max` view. (Grant/revoke mechanics are rig-proven
  live; no family entitlement should be revoked as a test.)

## Open questions for Mike

None. The one judgment call — including `grant` — is decided and
reasoned above (ExcelCosmetics symmetry), not left open.
