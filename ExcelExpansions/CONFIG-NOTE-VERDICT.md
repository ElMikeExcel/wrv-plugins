# SECTION 7.8 CONFIG-NOTE VERDICT - ExcelExpansions 0.1.1 filing prep

Question: the shipped config.yml carries a "rig-owed" releaser note
(the flip "is a rig-owed confirmation - not assumed here"). Rig run
335 SETTLED section 7.8 (restart applies the flip; em reload does
not). Is config.yml an in-jar resource (updating it changes the
candidate hash, needs a rebuild) or a loose file placed at install
(updatable without touching the jar)?

## Evidence (build tree, all hashes computed Nov 14 2026)

- src/main/resources/config.yml = 2,991 B /
  3a2de1b3b5fe0b02cad2d4462525efd7db00c31ac8f078b9164a0eb26493e17a
- Folder-root config.yml (build folder) = same 2,991 B / 3a2de1b3...
  (byte-identical to src; re-synced by the builder, BUILD-RECORD-011).
- build.sh copies src/main/resources/{plugin.yml, config.yml,
  releases.yml} into work/classes/ and jars them: config.yml IS an
  in-jar resource. Extracted from the candidate jar this lane:
  config.yml 2,991 B / 3a2de1b3... - identical to src. Same for
  releases.yml (55,651 B / 9fa280f3...) and plugin.yml (740 B /
  4be4101f...).
- Host code (ExcelExpansions.java onEnable, line 43-44):
  saveDefaultConfig(); then rows load via
  getResource("releases.yml"). saveDefaultConfig() is the Bukkit
  call that copies the jar's config.yml resource to the live file
  plugins/ExcelExpansions/config.yml ONLY if that file does not
  already exist; it never overwrites. Every runtime read after that
  uses the LIVE file via getConfig().

## Verdict: BOTH - and the loose-file route is the filing answer

- As a build artifact, config.yml is an in-jar resource: editing
  src config.yml and rebuilding changes the jar, so the candidate
  would no longer be 61,736 B /
  b9d26233fbef56520b4d9bd8d200093d85fa103e7ae51d17b50b729f9749ba91
  and the run-335 rig stamp (which ran on exactly those bytes)
  would be void. A rebuild for a comment-only wording change is
  NOT done and NOT needed.
- As a runtime artifact, config.yml is a loose live file placed in
  plugins/ExcelExpansions/ at first enable. Placing a pre-edited
  copy there BEFORE the first boot means saveDefaultConfig()
  leaves it in place and the plugin runs on the settled wording
  from the first boot, with the jar byte-untouched.

## Exact filing consequence

- The packet stages filing/config.yml.settled (3,062 B /
  3c2ab3308d682bbaedc4ab06099ada5e546964f7974b496813b80fedcecce9eb)
  = the shipped config with ONLY the releaser note's last sentences
  replaced by the settled 7.8 wording (diff verified: 3 lines
  changed, comments only, no key or value changes). The failsafe
  places it as plugins/ExcelExpansions/config.yml before Start.
- Candidate hash and rig proof are unaffected. Future rebuilds
  should fold the same wording into src (one-line note for the
  build lane; not part of this filing).
- releases.yml is different: it has NO loose-file path - the host
  reads it only from the jar (getResource). Its FINAL-manifest
  data is already inside the candidate (see INSTALL-PACKET.md),
  so no filing action is owed for it either.
