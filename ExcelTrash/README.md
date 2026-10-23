# ExcelTrash 0.1.0
Original Wink Reyes Valley plugin (MIT). `/trash` (alias `/bin`) opens a 4-row chest titled "Trash".
Rows 1-3 = storage; row 4 = buttons (lime dye "Recover last trash" at slot 31, gray panes elsewhere).
On close, items are deleted; the LAST batch per player is kept 5 minutes (memory + plugins/ExcelTrash/batches.dat,
atomic write). `/trash undo` or the button returns it once (overflow drops at feet). Empty closes are ignored.
Log: plugins/ExcelTrash/trash.log (TRASH/UNDO lines with stack + item counts per material).
Permission: exceltrash.use (default true). No config, no reload command (nothing to reload).
Dupe guards: button-row clicks cancelled; drags touching the button row cancelled; shift-click into the GUI
is handled manually into storage slots only; undo removes the batch (and persists) before returning items.
Build: `bash local-build.sh` (JDK 25, --release 21, Paper API libs, JUnit 5 console). 8/8 tests.
