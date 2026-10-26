# ExcelWhatsNew 0.1.1: install summary (public copy)

The full step list (with panel and account details) is kept off GitHub. Summary:
- Jar swap, needs one restart. Bundled into the same boot as ExcelWandBars 0.2.6.
- Drift guard: the live ExcelWhatsNew-0.1.0.jar must be sha256 691ee97a... or this part is skipped.
- Backup: the old jar is renamed to ExcelWhatsNew-0.1.0.jar.bak-20261026-newer (not loadable).
- Upload under a temp name, rename, read the sha back (ca35e86d...) before Start.
- The live config.yml is NOT edited: the new text.newer-button key falls back to "Newer news".
- House rules: two 0-player reads, clock gate, restart notice and countdown if anyone is on,
  owner-only whitelist during a daytime install, set back to off after the boot is verified.
- After the boot: one What's New entry (reload only) announcing the button.
- Rollback: rename the 0.1.1 jar off, rename the 0.1.0 backup back, restart.
