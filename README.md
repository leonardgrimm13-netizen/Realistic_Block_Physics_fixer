# Realistic Block Physics Fixer

Realistic Block Physics Fixer (RBPF) is a **server-side Minecraft Forge 1.20.1 mod** that provides a small, budgeted platform for compatibility and safety fixes around physics-like block behavior.

The project is now structured as a modular fix platform. The currently implemented production modules are the **Explosion Physics Update Fix** and the **Falling Block Entity Guard**.

## Current implemented fixes

### `explosion_physics_update_fix`

After `ExplosionEvent.Detonate`, the module queues a delayed server-thread scan around the explosion center and copied affected block positions. Suspicious blocks are nudged with normal block and neighbor updates so Realistic Block Physics can re-evaluate them.

The fix intentionally does **not**:

- implement its own block physics,
- spawn FallingBlock entities,
- destroy blocks,
- manipulate fluids directly,
- simulate fake player left-clicks,
- load chunks,
- access Minecraft world objects asynchronously.


### `falling_block_entity_guard`

RBP's own falling-block entity has a hard internal lifetime. On laggy servers or during large collapses this can make active physics blocks disappear before they land. The guard module monitors RBP falling-block entities server-side without importing RBP classes directly. It can reset the private lifetime counter for safe, loaded, airborne entities and logs when a level exceeds configurable soft limits. A small optional mixin runs at the start of RBP's falling-block `tick()` so the counter is clamped before RBP can execute its own hard discard.

The guard intentionally does **not** use client classes, does **not** load chunks, and does **not** discard entities by default. Its fallback scan has a hard `maxEntitiesVisitedPerLevel` cap because Forge does not expose a global RBP-only entity iterator without linking against RBP. A last-resort emergency discard option exists for crash prevention but is disabled unless the server owner explicitly enables it.

## Platform architecture

The code is split into focused packages:

- `core/` - module API, registry, tick/event scheduler, budgets, rate-limited logging.
- `config/` - global and module-ready Forge server config.
- `command/` - `/rbpf` admin commands.
- `debug/` - module stats and counters.
- `fixes/explosion/` - explosion update module implementation.
- `fixes/entityguard/` - RBP falling-block lifetime and overload guard.
- `fixes/template/` - disabled documentation-only example for future modules.
- `util/` - general helpers for chunk safety, positions, block ids, and thread checks.

Future fixes such as snow collision fixes, falling block update fixes, neighbor update fixes, modded explosion compatibility, or chunk-boundary safety fixes can be added as separate `FixModule` implementations.

## Requirements

- Minecraft Forge **1.20.1**
- Java **17**
- Gradle Wrapper **8.8**
- ForgeGradle **6.x**

## Installation

1. Build or download the mod jar.
2. Put the jar in the server `mods/` directory.
3. Restart the server.
4. Configure `config/realistic_block_physics_fixer-server.toml` after the first run.

The mod is designed for server-side behavior. Installing it on a dedicated server is the intended use case.

## Configuration

Config path:

```text
config/realistic_block_physics_fixer-server.toml
```

The config was reorganized into these categories:

- `general`
- `debug`
- `performance`
- `modules.explosion`
- `modules.fallingBlockEntityGuard`

Important keys include:

- `general.modEnabled`
- `general.commandEnabled`
- `debug.debugLogging`
- `debug.rateLimitWarnings`
- `debug.logLoadedModules`
- `performance.maxGlobalWorkPerTick`
- `modules.explosion.enabled`
- `modules.explosion.delayTicks`
- `modules.explosion.scanRadius`
- `modules.explosion.maxCheckedBlocksPerExplosion`
- `modules.explosion.maxUpdatesPerExplosion`
- `modules.explosion.maxScansPerTick`
- `modules.explosion.maxPositionsCheckedPerTick`
- `modules.explosion.maxUpdatesPerTick`
- `modules.explosion.onlyLoadedChunks`
- `modules.explosion.ignoreBlockEntities`
- `modules.explosion.ignoreFluids`
- `modules.explosion.blacklistMode`
- `modules.explosion.blockBlacklist`
- `modules.explosion.blockWhitelist`
- `modules.fallingBlockEntityGuard.enabled`
- `modules.fallingBlockEntityGuard.scanIntervalTicks`
- `modules.fallingBlockEntityGuard.maxEntitiesScannedPerLevel`
- `modules.fallingBlockEntityGuard.maxEntitiesVisitedPerLevel`
- `modules.fallingBlockEntityGuard.softLimitPerLevel`
- `modules.fallingBlockEntityGuard.hardLimitPerLevel`
- `modules.fallingBlockEntityGuard.emergencyDiscardAboveHardLimit`
- `modules.fallingBlockEntityGuard.keepAliveEnabled`
- `modules.fallingBlockEntityGuard.mixinKeepAliveEnabled`
- `modules.fallingBlockEntityGuard.keepAliveResetAtTicks`
- `modules.fallingBlockEntityGuard.keepAliveResetToTicks`

Some legacy keys from older config categories were renamed during the modularization. If upgrading from an older generated TOML, compare it with a freshly generated file and migrate values into `modules.explosion`.

## Commands

Commands require admin permission level 2 or higher:

- `/rbpf status` - shows global state, modules, queue sizes, and error counters.
- `/rbpf modules` - lists all registered fix modules and whether they are enabled.
- `/rbpf debug on` - enables runtime debug logging until restart or reload.
- `/rbpf debug off` - disables runtime debug logging until restart or reload.
- `/rbpf explosion stats` - shows explosion module counters.
- `/rbpf fallingblocks stats` - shows falling-block guard counters.

Runtime debug commands do not write the Forge config file.

### In-game guard check

1. Start a dedicated/server-integrated test world with RBP and RBPF installed.
2. Trigger falling physics with sand/gravel or a controlled explosion.
3. Run `/rbpf fallingblocks stats`. Useful counters are `seen`, `visited`, `keptAlive`, `mixinKeepAliveResets`, `reflectionFailures`, and `scanVisitLimitReached`.
4. Enable `/rbpf debug on` for a short test window if you need per-reset debug lines. A working keep-alive path logs whether `mixin` or the fallback `scan` reset `fallTime` and shows the old and new values.
5. If `reflectionUnavailable=1` or `reflectionFailures` increases, the RBP private field name changed or could not be accessed; the guard will not report false keep-alive success in that state.

## Performance and thread-safety rules

RBPF follows these rules:

- no chunk loading,
- no async world access,
- no entity spam,
- no player-action simulation,
- all `ServerLevel`, `Level`, `BlockState`, `BlockEntity`, chunk checks, and block updates stay on the server thread,
- each module must respect per-tick budgets,
- overload is handled by deferring or dropping work instead of freezing the server,
- repeated warnings are rate-limited.

Async work is only used for immutable coordinate-list planning. It must never read or write Minecraft world objects.

## Adding new fix modules

See [`docs/ADDING_FIX_MODULES.md`](docs/ADDING_FIX_MODULES.md). In short:

1. Create `fixes/<name>/<Name>FixModule.java`.
2. Implement `FixModule`.
3. Add config in `RBPFConfig`.
4. Register the module in `RealisticBlockPhysicsFixer`.
5. Use central scheduler callbacks or explicit module-owned Forge event wiring.
6. Respect budgets and server-thread-only rules.
7. Expose `ModuleStats`.
8. Document tests.

## Build

```bash
./gradlew clean build
```

The repository includes the real Gradle Wrapper files:

- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradle/wrapper/gradle-wrapper.jar`

`.gitignore` ignores build output but explicitly keeps `gradle-wrapper.jar`.

## CI

GitHub Actions runs `./gradlew clean build --no-daemon` on Java 17 and validates the wrapper.

## Troubleshooting

- If the wrapper cannot download Gradle 8.8, check network/proxy access to `services.gradle.org`.
- If Forge/Minecraft dependencies cannot resolve, check access to Forge, Maven Central, and Minecraft library repositories.
- Do not require SDKMAN for normal users; the wrapper is the supported path.
- If Gradle 9 warnings appear, keep using Gradle Wrapper 8.8 for ForgeGradle 6.x compatibility.

## Production validation for PR #4

### CI build artifact and dedicated-server smoke test

GitHub Actions runs `Build and dedicated server smoke test` for pushes and pull requests. The job:

1. builds the Forge 1.20.1 jar with Java 17,
2. verifies the jar contains `realistic_block_physics_fixer.mixins.json` and the manifest `MixinConfigs` entry,
3. starts a Forge dedicated server dev run with the Fixer mod and `eula=true`,
4. checks the server log for the RBPF startup line and the mixin config name,
5. uploads the built jar as the `realistic-block-physics-fixer-jar` artifact.

This CI smoke test intentionally does not bundle Realistic Block Physics, because RBP is not declared as a hard dependency of RBPF. To test with a real RBP jar locally:

```bash
./gradlew build
mkdir -p run/mods
cp build/libs/realistic_block_physics_fixer-*.jar run/mods/ 2>/dev/null || cp build/libs/Realistic_Block_Physics_fixer-*.jar run/mods/
cp /path/to/realistic-block-physics.jar run/mods/
echo eula=true > run/eula.txt
./gradlew runServer --no-daemon -Dmixin.debug.verbose=true
```

A healthy local boot should show the RBPF startup log, should not crash during mod loading, and should mention `realistic_block_physics_fixer.mixins.json` when Mixin verbose logging is enabled.

### Falling-block stress test checklist

1. Start a dedicated test server with Forge 1.20.1, RBP, and RBPF.
2. Run `/rbpf debug on` for the short test window.
3. Create normal RBP falling blocks with sand/gravel columns, then trigger a larger controlled explosion in a disposable area.
4. Run `/rbpf fallingblocks stats` and `/rbpf fallingblocks health`.
5. Good signs:
   - `health` reports `OK`, or `WARNING - fallTime reflection not proven yet` before any RBP falling entity has been seen.
   - `mixinKeepAliveResets` or `keptAlive` increases after long-running airborne RBP blocks.
   - `reflectionFailures=0` and no `reflectionUnavailable=1`.
   - `emergencyDiscarded=0` with the default config.
6. Warning/error signs:
   - `reflectionUnavailable=1` or `health` reports `BROKEN`: verify the installed RBP jar/version. Update RBPF/RBP if the private field changed, or intentionally disable `modules.fallingBlockEntityGuard.keepAliveEnabled` if you accept vanilla RBP lifetime behavior.
   - `scanVisitLimitReached` increases often: the fallback scan is hitting `maxEntitiesVisitedPerLevel`; inspect entity counts/TPS before raising it.
   - `emergencyDiscarded` increases: emergency discard was explicitly enabled and the hard limit was exceeded; use only as crash prevention.

### Falling-block guard default rationale

- `keepAliveResetAtTicks=560`: leaves a 40 tick buffer before RBP's hard `fallTime > 600` discard. With `mixinKeepAliveEnabled=true`, the check runs at the start of every RBP falling-block tick, so laggy server ticks still clamp before RBP's own discard branch executes.
- `keepAliveResetToTicks=120`: avoids repeatedly resetting every tick while still preserving enough age history to avoid treating every entity as brand new.
- `maxEntitiesVisitedPerLevel=4000`: hard cap for the fallback scan to prevent full-world entity walks on large servers.
- `maxEntitiesScannedPerLevel=512`: bounds reflection/tracking work per scan; the mixin path handles the time-critical keep-alive case.
- `softLimitPerLevel=350`: warns early that large collapses may be stressing the server.
- `hardLimitPerLevel=1200`: only used by the explicit emergency-discard mode.
- `emergencyDiscardAboveHardLimit=false`: no aggressive discards by default; server owners must opt in when preventing a crash is more important than preserving every physics entity.
