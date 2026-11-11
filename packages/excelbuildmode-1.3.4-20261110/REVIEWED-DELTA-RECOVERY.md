# REVIEWED DELTA RECOVERY — ExcelBuildMode 1.3.3 (reviewed) → source (Nov 10, 2026)

Desk recovery for the ExcelBuildMode 1.3.4 combined rebuild (board
supervisor decision D5). Method: byte-level diff of the two jars, then
`javap -c -p` disassembly of the differing classes, with constant-pool
indices normalized so only semantic differences remain. Raw disassembly
and normalized diffs are in `recovery-evidence/` in this folder.
No flagged tool output was used; every fact below derives from clean
on-disk reads and hashes taken in this build run.

## Inputs (hash-verified in this run)

- Preserved reviewed build (the live jar's exact copy):
  `~/workspace/requal/menujoin143/preserved/ExcelBuildMode-1.3.3.jar`
  — 90,044 B — SHA-256
  `3662dcb73ca08ac1677e4257f64eae489ab9486e484ecc447ea2779cedc3ee1c`
- Genuine 1.3.2:
  `~/workspace/excelbuildmode-pilot/ExcelBuildMode-1.3.2.jar`
  — 89,718 B — SHA-256
  `26d356b4c6266200a30ee891fec4727ba11456491264dfb76df9c8615d600116`

## File-level diff (48 entries in each jar)

Identical file sets. Only THREE entries differ; all 42 core classes,
`SurvivalSnapshot`, `WorldEditLimitBridge`, both inner classes,
`config.yml`, and `META-INF/MANIFEST.MF` are byte-identical:

| Entry | Preserved (reviewed) | Genuine 1.3.2 |
|---|---|---|
| `plugin.yml` | bc4fc10f…, 1,587 B | 76dd28e5…, 1,191 B |
| `paper/BuildPolicy.class` | 0748e3b2…, 6,330 B | 09614238…, 6,064 B |
| `paper/ExcelBuildMode.class` | f84df119…, 45,039 B | 7772b47b…, 44,900 B |

## plugin.yml delta (read verbatim from both jars)

- version `'1.3.2'` → `'1.3.3'`
- description gains: " 1.3.3 adds excelbuildmode.reviewed - a reviewed
  builder may keep standing WorldEdit permissions and still use Build Mode."
- NEW permission block (verbatim from the preserved jar):
  ```
  excelbuildmode.reviewed:
    description: Reviewed builder. Keeps standing WorldEdit permissions outside Build Mode and may still enter Build Mode (skips the outside-WorldEdit entry refusal and the exit residual-permission probe). Not a WorldEdit node.
    default: false
  ```
- commands/usage unchanged (`/buildmode [on|off|status]`).

## BuildPolicy delta (normalized javap diff — the WHOLE delta)

1. New constant:
   `public static final String REVIEWED_PERMISSION = "excelbuildmode.reviewed";`
2. New method (bytecode: `a && !b && c`):
   ```java
   public static boolean refusesOutsideWorldEdit(boolean worldEdit, boolean reviewed, boolean hasOutsideWorldEdit) {
       return worldEdit && !reviewed && hasOutsideWorldEdit;
   }
   ```
3. New method (bytecode: `a && !b`):
   ```java
   public static boolean probesResidualWorldEdit(boolean worldEdit, boolean reviewed) {
       return worldEdit && !reviewed;
   }
   ```
Nothing else: the WorldEdit probe set, session permissions, harvest
policy, and command policy are byte-identical.

## ExcelBuildMode delta (normalized javap diff — the WHOLE delta)

String-constant diff for the whole class: the ONLY added string is
`excelbuildmode.reviewed` (×2). Method list unchanged. Banner strings
unchanged (the reviewed build kept the 1.3.2 banner text). Exactly two
call sites change:

1. **Entry refusal** (`enter(Player)`). 1.3.2 bytecode:
   `if (worldEdit && WORLD_EDIT_PERMISSIONS.stream().anyMatch(p::hasPermission)) { tell(refusal); return; }`
   Reviewed bytecode pushes, in order: the `worldEdit` field,
   `p.hasPermission("excelbuildmode.reviewed")`, and the same
   `anyMatch` stream result, then calls
   `BuildPolicy.refusesOutsideWorldEdit(ZZZ)Z` and branches on it.
   Reconstructed source:
   ```java
   if (BuildPolicy.refusesOutsideWorldEdit(worldEdit, p.hasPermission(BuildPolicy.REVIEWED_PERMISSION),
           BuildPolicy.WORLD_EDIT_PERMISSIONS.stream().anyMatch(p::hasPermission))) {
       tell(p, "You already have WorldEdit permissions outside Build Mode. An administrator must review those before temporary access can be used."); return;
   }
   ```
   (The call site loads the literal string; `REVIEWED_PERMISSION` is a
   compile-time constant, so referencing the constant compiles to the
   identical `ldc` — the reconstruction is bytecode-faithful either way.)
2. **Exit residual-permission probe** (`removeAttachment(Player)`).
   1.3.2 bytecode: `if (worldEdit) for (node : WORLD_EDIT_PERMISSIONS) if (p.hasPermission(node)) throw …`
   Reviewed bytecode pushes the `worldEdit` field and
   `p.hasPermission("excelbuildmode.reviewed")`, calls
   `BuildPolicy.probesResidualWorldEdit(ZZ)Z`, and runs the same loop
   only when it returns true. Reconstructed source:
   ```java
   if (BuildPolicy.probesResidualWorldEdit(worldEdit, p.hasPermission(BuildPolicy.REVIEWED_PERMISSION)))
       for (String node : BuildPolicy.WORLD_EDIT_PERMISSIONS)
           if (p.hasPermission(node)) throw new IllegalStateException("WorldEdit permission remains after attachment removal: " + node);
   ```

All remaining normalized-diff lines are branch-target renumbering from
the two insertions above. No other behavior differs.

## Faithfulness statement

The delta is FULLY recovered: two pure boolean helpers, one constant,
two call-site conditions, and the plugin.yml block — every element is
pinned by the disassembly, not inferred. The reconstruction is proven
two ways in this build (see BUILD-RECORD.md): (a) the recovered
helpers' truth tables are pinned by ReviewedTests, and (b) a control
build of the recovered delta on the genuine 1.3.2 tree compiles to
classes whose normalized javap output matches the preserved jar's two
differing classes instruction-for-instruction (plugin.yml compared
verbatim apart from the 1.3.4 version/description lines).
