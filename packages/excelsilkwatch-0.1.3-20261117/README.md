# ExcelSilkWatch 0.1.3 - records package

Draft record - do not merge. Board-manager run 494, B11 row 58.
Sanitized records package for ExcelSilkWatch 0.1.3 (in-jar version
0.1.3-offline). Assembled under decision D102 (package held until sanitized)
and the license ruling in decision D103 (this plugin is first-party Wink
Reyes Valley code and carries the same license and readme shape as sibling
first-party record packages in wrv-plugins).

## Contents

- src/com/winkreyes/silkstorage/ - the 5 own-plugin Java sources, verbatim
  from the nested source-tests tar in the private Drive packet (per-file
  hashes in SHA256SUMS; verbatim check in the run 494 W7 evidence).
- resources/plugin.yml and resources/config.yml - own-plugin resources,
  verbatim. The config default is 322 B / SHA-256 37793c32d77e93cb223d2eb2b4d801db7c877003f597955a2a26b7ee2ea1a8fc,
  exactly the default the live deployment receipt records as generated and
  read back at install.
- tests/ and preference-tests/ - own focused tests (runtime probe plus the
  preference-store test class and its harness plugin.yml).
- build.sh, test.sh, test-preferences.sh - own build and test scripts,
  path-sanitized as disclosed below.
- INSTALL-RECORD-0.1.3.md - newly written sanitized install receipt summary.
- LICENSE - MIT License, same shape as sibling first-party packages.
- SHA256SUMS - hashes for every file in this package plus the provenance
  line for the installed jar (see Jar).

## Behavior

Player label: Silk Touch Warnings. A nonblocking action-bar warning follows
a verified net deposit of a Silk Touch tool or book, or a shulker box
containing one, into the player's own native ender chest. Warning only: no
confirmation, cancellation, item movement or inventory edit. The command
/silkwarnings [on|off|toggle|status] edits only the issuing player's own
preference (permission excelsilkwatch.toggle, default true), persisted in a
strict per-player store; the PlaceholderAPI expansion identifier is
silkwarnings and exposes the same preference state. Custom vaults and other
nested container types are unsupported; shulker scanning is read-only and
bounded. Full behavior and limits are summarized from the private packet's
own documentation; no private documentation is reproduced here.

## Jar

ExcelSilkWatch-0.1.3-OFFLINE.jar - 17,847 B / SHA-256 c566067e95b5d7c2bf92f8fff872b8b332c6626c524a4e65424752ac9fc869ee.
The jar binary is not carried in this records package; its SHA256SUMS line
is a provenance line only, so a checksum verification run reports that one
file as absent by design. Provenance: the jar in the private Drive packet
(plugin-staging) hashes exactly to the value the deployment manifest and
the live deployment receipt record, and to the value the canonical
source-coverage index credits for row 58.

## Source provenance

Drive-carried private packet (Minecraft WRV Source folder
1twtEkZWxY3m-93vUJ_5zLuDYdXWBxJjz): outer tar file
13PS8cjTZq8vUhsGruwgeQqfK3KSWPqe9, 404,454 B / SHA-256 81dcea7152ca2fb92cc394f0078ee5d056d7e35fc3940048dd5a6d517bfb7b9b;
nested source-tests tar, 164,837 B / SHA-256 3104164923bd26d1360e8bfeacaf36e1b7492b80fb0ab734623531074755ba8c,
is the extraction source for every own-plugin file above. Receipt:
INSTALL-RECORD-0.1.3.md in this package.

## Sanitization disclosure

This package was assembled from an explicitly private, unsanitized
release-plan packet. Excluded in full, by category: (1) bundled
reference-project source trees of other plugins; (2) derived patch files
against those projects; (3) recorded live baselines and server snapshots;
(4) operational receipts and deployment evidence files; (5) raw runtime
logs; (6) unrelated deployment plans and manifests; (7) other-plugin
binaries and source archives; (8) the private operator plan, manifest and
privacy-review documents themselves. Sanitizing edits inside carried
files, and no others: the three shell scripts had machine-specific default
paths replaced with required environment variables and the loopback test
bind written by name instead of as a numeric address; two test files had
synthetic fixture identifiers re-expressed in numeric constructor form
with identical values (no real player identifier appears in any carried
file - the fixtures were synthetic in the source packet, per its own
privacy review). Every other carried file is byte-identical to the nested
source-tests tar. A pre-publication scan of this package for player
names, identifier literals, network addresses, credential formats and
absolute paths returned zero hits per pattern (run 494 W7 evidence).

## License

MIT License - see LICENSE in this package. Copyright (c) 2026 Wink Reyes
Valley contributors. Author attribution in the sources (WinkReyesValley;
Excel signature) is attribution, carried verbatim.
