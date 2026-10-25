# Rollback 0.1.7 -> 0.1.6
1. Quick off (no restart): night-hearing.enabled: false, then "tw reload". Every zombie TW pulled
   is released at once and every steered zombie stops walking to its door. Beds go back to the
   vanilla rule. Everything else is 0.1.6 behavior. (Steering alone: steer-to-doors: false.)
2. Full rollback: move ExcelTrainingWheels-0.1.7.jar out of plugins/, put the kept 0.1.6 jar back
   (sha256 974d159fd9f4cf9b750914f57424ebe21a736a8b846746bdd6dfe5b4bf1244e4). Restart.
3. Config: 0.1.6 ignores night-hearing.* and doors.wood-fallback-* (unknown keys are harmless), so
   the config needs no edit. Restore the step-2 backup only if something else changed.
4. Tip: if the staged tips.yml was installed, the night-hearing entry needs ExcelTrainingWheels>=0.1.7
   (needs-plugin), so it goes quiet by itself on 0.1.6. To remove it, put back
   plugins/ExcelTips/tips.yml.bak-20261025-nighthearing (the ClaimFly file, sha256 f69e7283...) and
   "tips reload". Do not roll tips.yml back further than that: the port warning and ClaimFly entries
   live in it too.
5. Verify: boot log "Enabling ExcelTrainingWheels v0.1.6", no "night hearing" line, zero SEVERE.
