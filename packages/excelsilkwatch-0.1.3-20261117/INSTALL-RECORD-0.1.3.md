# ExcelSilkWatch 0.1.3 - INSTALL RECORD (SANITIZED)

Newly written sanitized receipt summary, board-manager run 494, B11 row 58.
This file is not a copy of the private deployment receipt. It carries only
the install facts the canonical source-coverage index already credits for
row 58, restated without operational detail. The private receipt and the
private release-plan packet stay private (decision D102).

## Install

- Installed 2026-10-29 05:55 UTC (October 29, 12:55 AM CDT). Pre-manifest
  install (no INSTALLED heading exists in the install manifest or its
  archive for this plugin).
- Gates at install: player count 0 recorded at 05:51:01 UTC; clean stop
  05:51:27 UTC; server start accepted 05:55:03 UTC; Done 05:55:25 UTC;
  normal access restored and confirmed at 05:55:51 UTC.
- Enable verification at boot: ExcelSilkWatch 0.1.3-offline enabled
  (native ender-chest and bounded shulker warning-only observer, persistent
  per-player toggle); the PlaceholderAPI expansion silkwarnings
  0.1.3-offline registered at 05:55:21 UTC; the help listing confirms the
  /silkwarnings [on|off|toggle|status] command. No new startup error for
  this plugin was observed.
- Installed alongside it in the same window: a menu-plugin companion
  update that this record does not carry (other-plugin artifact, excluded
  from this package by category).

## Artifact

- ExcelSilkWatch-0.1.3-OFFLINE.jar - 17,847 B / SHA-256 c566067e95b5d7c2bf92f8fff872b8b332c6626c524a4e65424752ac9fc869ee,
  server-verified at install and exactly matching the private packet's
  plugin-staging jar and its deployment manifest entry.
- Generated config at install read back exactly as the reviewed 322-byte
  default: SHA-256 37793c32d77e93cb223d2eb2b4d801db7c877003f597955a2a26b7ee2ea1a8fc
  (the same bytes as resources/config.yml in this package). No prior
  SilkWatch jar or data folder existed before this install.

## Provenance chain

- Private packet: Drive file 13PS8cjTZq8vUhsGruwgeQqfK3KSWPqe9
  (ExcelSilkWatch-0.1.3-PRIVATE-RELEASE-PLAN-20261029.tar.gz), 404,454 B /
  SHA-256 81dcea7152ca2fb92cc394f0078ee5d056d7e35fc3940048dd5a6d517bfb7b9b,
  in Drive folder 1twtEkZWxY3m-93vUJ_5zLuDYdXWBxJjz.
- Nested source-tests tar inside it: 164,837 B / SHA-256 3104164923bd26d1360e8bfeacaf36e1b7492b80fb0ab734623531074755ba8c.
- Private deployment receipt: Drive file 1Wxrbhtpn8ddLvtj7hnNqEg1pv0-SprqI,
  5,257 B / SHA-256 1dcf0e00e6c84fd9e183fa01eb4692a0922dfea0fe78fd0eb79036503ca14065.
  Its operational detail (approvals, backups of other plugins, panel
  evidence) is deliberately not reproduced here.
- Corroboration: the canonical source-coverage index row 58 credits this
  install on the two Drive-carried hashes above; the Nov 15 census lists
  the jar and its data folder live.

## Verification summary (sanitized)

From the private packet's own verification document, restated without
logs or paths: PASS - 102 of 102 runtime harness cases on first boot and
again after restart, and 22 of 22 preference-store cases, run offline
against the exact jar above (Paper 26.2, JDK 25, real unmodified
PlaceholderAPI 2.12.3 at runtime). Branding cases confirm the plugin
name, main class, version and data-folder identity above. Raw runtime
logs are excluded from this package by category.

## Open at install

Real-player acceptance (default and saved toggle behavior, Java and
Bedrock settings rendering, live deposit warnings) was still pending when
the receipt was written; it is not a condition of this receipt. Rollback
is narrow by design: stop safely, preserve any preferences data created
since install, and retain the jar inactive.
