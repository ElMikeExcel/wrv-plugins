# ExcelGuide 0.3.0 rollback

Back to 0.2.3. One restart. Nothing else needs to change.

1. Remove `ExcelGuide-0.3.0.jar` from `plugins/`.
2. Put the backed-up 0.2.3 jar back (from step 1 of INSTALL).
3. Restart (reason, then `restartwarn`). Bedrock players rejoin.

What happens to the data:
- 0.2.3 ignores `peer-review.yml` and `peer-tip.yml`; they stay on disk
  untouched (tested), so going forward again later keeps the queue,
  approved answers and tip OKs.
- 0.2.3 reads `peer-answers.yml` again. It is `[]`, so 0.2.3 logs the
  same harmless "peer file root is not a map; starting with an empty
  store" warning it logged after the Oct 24 clear, and starts learning
  the 0.2.3 way again (unreviewed). If you don't want that, set
  `peer-learning.enabled: false` in the live config before restarting.
- `config.yml`, `learned.yml`, `knowledge.yml` and `intents.yml` are
  untouched by 0.3.0, so nothing to restore there.

Full restore instead: stop, replace `plugins/ExcelGuide/` with the
`plugins/ExcelGuide.bak-<date>-030/` copy, put the 0.2.3 jar back, start.
