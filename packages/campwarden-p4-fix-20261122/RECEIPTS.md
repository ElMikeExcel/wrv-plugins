# RECEIPTS — P-CAMP-WARDEN-DOOR-SEAL install packet

## G1 live-premise receipt (FRESH, same-day, Homebase route)

- Claim: live file exists at the path below with the stated bytes.
- Live path (Homebase): D:\muse-servers\wrv\plugins\EliteMobs\
  custombosses\primis\primis_bloodtemple_climb_miniboss_1_p4.yml
- Server-relative path: plugins/EliteMobs/custombosses/primis/
  primis_bloodtemple_climb_miniboss_1_p4.yml
- Pull method: Homebase SSH (ssh -F ~/.ssh/config mike-pc);
  remote Get-FileHash + byte pull via ReadAllBytes/base64.
- Pull timestamp: 2026-11-22 01:02 AM CT (2026-11-22T07:02:28Z).
- Pulled copy: live-pull/primis_bloodtemple_climb_miniboss_1_p4.yml
  in this folder.
- Size: 2,815 B
- SHA-256: 7661989fe8c3870838d54bd43b72609b2aa2ef15c88e70c7dafc20ca60cc1f53
- Remote Get-FileHash agreed: 7661989FE8C3870838D54BD43B72609B2AA2EF15C88E70C7DAFC20CA60CC1F53
  (uppercase rendering of the same hash); remote Length 2815;
  remote LastWriteTime 11/14/2026 9:03:37 AM.
- Cross-check: pulled bytes cmp-identical to the verify record's
  evidence copy (camp-warden-verify-20261122/evidence/
  primis_bloodtemple_climb_miniboss_1_p4.yml, 2,815 B / 7661989f...).
- Result: MATCHES the expected gate (2,815 B / 7661989f...). PASS.

## Candidate receipt

- Path: primis_bloodtemple_climb_miniboss_1_p4.yml in this folder,
  copied byte-identical from camp-warden-verify-20261122/candidate/.
- Size: 3,485 B
- SHA-256: 831f86d3566bc8ed3968acc256bc32e7afa2240d9609210a711953e67be020b6
- Census (script, python yaml): candidate onRemoveBlockStates = 30,
  onSpawnBlockStates = 0; live onRemoveBlockStates = 15,
  onSpawnBlockStates = 0.
- Diff (live -> candidate): exactly 15 lines appended, all Door B
  open/restore entries at offset z -14.5, verbatim from P1
  onSpawnBlockStates. No other line differs.

## Verify-record basis

- ~/workspace/camp-warden-verify-20261122/VERIFY-RECORD.md
  13,288 B / SHA-256 33fd902e... (supervisor-verified).
- Its sections 4/6: bytecode-proven DEATH path applies the current
  phase's onRemoveBlockStates; P4 is terminal, so the 30-entry list
  restores both doors at boss death. No content-only edit can
  reopen a door while the boss is alive (accepted, D188 option a).
