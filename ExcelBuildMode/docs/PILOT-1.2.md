# ExcelBuildMode 1.2 — what changed

One change over 1.1, made Oct 14, 2026 at Mike's direction after Marvin's
live acceptance run: **WorldEdit brushes now work inside a build session.**

Marvin's run found edits and selections working but //brush and /br
refused. Cause: the pilot's fixed WorldEdit permission grant (16 nodes)
never included any brush nodes. The spec named clipboard copy/paste, not
brushes, so 1.1 shipped exactly that list.

1.2 adds 25 nodes to BuildPolicy.WORLD_EDIT_PERMISSIONS (41 total):

- 21 brush families: apply, biome, clipboard, cylinder, deform, ex,
  feature, forest, gravity, heightmap, item, lower, morph, paint, raise,
  set, smooth, snow, snowsmooth, sphere, splatter
- 4 brush option commands: options.material, options.range,
  options.size, options.mask

Deliberately excluded: `worldedit.brush.butcher`. The butcher brush
kills entities in a radius; that is combat, not construction, and it
fights the pilot's no-combat design.

Everything else is unchanged from 1.1: Moon's frozen 1.0 recovery core
is untouched, the pilot gates are untouched, plugin.yml version is 1.2,
build output is `target/ExcelBuildMode-1.2.jar`. Brush operations remain
under the server's WorldEdit limits (5,000-block cap, history, command
logging) like every other session edit.

Status at build time: BUILT, automated tests passed, local boot smoke
test passed. NOT installed. Install is a jar swap at an empty-server
restart under the standard protocol; the live config.yml, cohort files
and recovery data are preserved. Marvin's brush check on the live server
is the remaining verification after install.
