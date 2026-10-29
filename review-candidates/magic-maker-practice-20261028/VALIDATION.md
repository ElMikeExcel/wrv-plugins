# Public source validation

Offline only. Original candidate unchanged; binary outputs kept outside this source export.

- Native `build.py`: passed compiler/linkage/callback/raw-load/lore/context checks and exact seven-class archive comparison. Output SHA256 `7f105d60970cefe20a91c6f5b6e439855686e6377237ba1bcf3bdf0299e3e3f0`, identical to reviewed native candidate.
- Additional native checks: 13 initial reservation/failure/union/reattach and 18 learned-retirement checks passed.
- Maker `build-offline.sh --jar`: all sources compiled, all 40 unit tests passed, package emitted successfully. SHA256 `b8ec22a1ec748ce64427954748c14dcdfe9f67f65ccb5be9f0c71e8b4c2227c4`. This sanitized/repackaged build is not asserted identical to the reviewed artifact.
- Packaging correction found during validation: Maker CHANGELOG.md was required by wrapper and had been omitted from export. Exact reviewed packaged resource restored, then full build/test/package rerun passed. Native dependencies.json and seven test sources were likewise added from exact reviewed inputs. See PACKAGING-ADDITIONS.json.
- All 157 source/default/resource manifest hashes verified; no binaries added to export.
- Coordinator Geyser/Cloudburst and preserved dependency-class build boundary remains as described in README. No claim of full clean coordinator source rebuild, real-player combat acceptance or deployment.
