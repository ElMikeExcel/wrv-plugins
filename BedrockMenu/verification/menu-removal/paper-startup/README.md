# Disposable BedrockMenu Paper 26.2 startup check

Never install the test probe on a real server: it shuts down its test server
after checking. Do not commit binaries, server worlds, credentials, eula.txt,
full server logs or generated caches. No production server/data is used.

## Verified result: 2026-10-13

The exact original-identity candidate completed **two actual Paper 26.2 #123
boots / 56 passing checks / zero failed assertions** on Temurin 25.0.4+7.
The second boot is a clean restart. Both child processes exited normally with
code 0, disabled BedrockMenu cleanly, and released `127.0.0.1:25581`. The exact
candidate SHA-256 remained unchanged, and a final independent port check passed.
See `evidence/summary.json` and both per-boot check reports.

The real server verified original plugin identity/version, enabled state,
command ownership/executors, console rejection paths, registered teleport
listeners, plugin classloader resolution, and both correct seven-entry arrays.
The Minecraft EULA was accepted before the first boot; acceptance details and
the EULA file are not part of this public evidence.

Six earlier actual Paper API/JDK25 linkage checks also pass, with all four
production classes loaded and both arrays verified; see
`evidence/real-api-linkage.log`. Those checks do not instantiate a plugin or
start a server and are separate from the two completed server boots.

External Minecraft authentication-key and Paper update/version lookups failed
because those hostnames could not be resolved in the test runtime. Bundled JOML/Netty emitted
platform warnings. There were no BedrockMenu enable/disable errors. These
external-service warnings do not establish authenticated client connectivity;
Geyser, Essentials and real clients remain untested. No production action was
performed.

## Inputs

The exact original-identity candidate is `BedrockMenu-1.4.2-menu.1.jar`, SHA-256
`fef68e9a06053767fa8223811e4ca2b1294a05daefa0e7bb11346ceba730f276`.

Official runtime artifacts, independently checksum-verified:

- [Paper 26.2 build 123 metadata](https://fill.papermc.io/v3/projects/paper/versions/26.2/builds/123),
  archive SHA-256 `7b7b3b43c009103e1971a0576c26f655a7dd9b56a0a2a4438e352c03a7fecd08`
- [Temurin 25.0.4+7 release](https://github.com/adoptium/temurin25-binaries/releases/tag/jdk-25.0.4%2B7),
  `OpenJDK25U-jdk_x64_linux_hotspot_25.0.4_7.tar.gz`, SHA-256
  `e58fcdcd637b25c03ca84cbbcefc70d11efb8f4b4cbd05decc9f661769d77f94`

Direct official URLs and hashes are also in `evidence/verified-artifacts.json`.
Python 3.12+ is required for safe archive extraction. The runner uses no external
Python packages. All tests are confined to generated folders beneath this
harness directory.

## Prepare without starting a server

Copy this source-only harness to a fresh directory and provide the exact inputs:

```sh
python3 prepare_runtime.py \
  --paper-jar /path/to/paper-26.2-123.jar \
  --jdk-archive /path/to/OpenJDK25U-jdk_x64_linux_hotspot_25.0.4_7.tar.gz \
  --candidate /path/to/BedrockMenu-1.4.2-menu.1.jar
```

Preparation verifies the three inputs and all extracted bundled libraries,
compiles the original test probe against the exact Paper API, and reruns the
six no-server linkage checks. It refuses an existing runtime directory, does
not create eula.txt, and does not start Paper. The fixed test address is
`127.0.0.1:25581`; authentication and whitelist are required; RCON, query and the
management server are disabled. No real players are admitted.

## Run only after EULA acceptance is established

Review and accept the [Minecraft EULA](https://www.minecraft.net/en-us/eula)
before supplying `server/eula.txt` with `eula=true`. The scripts never create or
change acceptance. Then run:

```sh
python3 run_startup_check.py
```

The runner rejects absent acceptance, wrong artifact hashes, extra plugins,
unsafe network settings or a busy test port. It obtains the exact Mojang source
URL/hash pinned in official Paperclip metadata, verifies it, and runs official
Paperclip patch-only preparation. It then boots the actual Paper server twice,
including a clean restart, with the exact unchanged candidate and original
test-only probe.

Each boot performs 28 checks: actual runtime version/build, enabled
plugin identity/version, original classloader, both seven-entry arrays, real
command ownership/executors, console rejection paths, registered teleport
listeners and absence of the explicitly untested integrations. The main-thread
probe writes its results and requests normal shutdown. The runner verifies a
normal exit, candidate enable/disable logs, unchanged candidate/EULA hashes and
a free loopback port after each run. A startup timeout stops only its own child.

Results will be written to `evidence/summary.json` and per-boot reports. Do not
report a pass until these files show two completed boots and 56 passing checks.

## Limits

A successful actual-Paper startup test still does not establish Geyser/Floodgate
form rendering, phone or Java-client interaction, actual Essentials teleport
integration, DeluxeMenus routing, player permissions or behavior alongside the
complete installed plugin set. Geyser, Essentials and real network clients are
explicitly absent from this fixture. Existing 1.4.2 behavior is preserved; the
unrelated 1.4.3 spawn repairs and canceled runtime renames are excluded.
