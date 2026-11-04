# Dependencies

This plugin is the project's own code (Wink Reyes Valley). It has no upstream plugin source dependency: it is compiled against the Paper API and the server's installed plugin APIs only, as recorded in the build folder's SOURCE.md.

The bridge reads the third-party AutoSort plugin's live state (AutoSort 0.1.17 by nothub, MIT) through reflection at runtime and publishes it as a PlaceholderAPI expansion. AutoSort itself is upstream and unmodified; no upstream source is included.
