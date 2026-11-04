# Base verification — WandBars UX build (Oct 30, 2026)
Shipped jar: ~/workspace/wandbars-fix/ExcelWandBars-0.3.4-faketapfix.jar
SHA-256 d8a38fc637bc3e21be9fec6ef30f0e091aba738831ae40a8e1383474cf3179de (matches packet record)
Base tree: packet coordinator src (~/workspace/wandbars-fix/work/packet/source/coordinator/src)
  + fake-tap fix TapBarHook.java / core/WandSlotPlan.java from ~/workspace/wandbars-fix/source/
Toolchain: Temurin 25.0.4.1 (~/workspace/.jdk25), flags --release 25 -encoding UTF-8 -g:source,lines -proc:none,
  classpath = wandbars-fix/work/repro/cp.txt (packet Magic candidate + packet WandBars candidate + Geyser + Paper 26.3 libs)
Result:
- All com.winkreyes.excelwandbars classes reproduce BYTE-FOR-BYTE, except the 5 Geyser hook classes.
- GeyserTapHook/GeyserHoldHook (+inners) reproduce BYTE-FOR-BYTE when compiled --release 21
  (shipped classes are class-file v65; the rest of the jar is v69). Earlier-build toolchain artifact.
- Shipped-only classes with no coordinator source (26): FirstObservationFloor (11; source exists in
  packet native-tests/constructor-src) and wrv/magic/eligibility (15; sources not in packet).
  Neither family is touched by this release; both carry over byte-for-byte in the repack.
- Therefore: every class this release MODIFIES has a byte-verified source baseline.
