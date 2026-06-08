# Realistic Block Physics Fixer

Server-side Forge 1.20.1 helper for modpacks where explosions can leave unsupported blocks floating until a player manually hits them. The mod does **not** simulate clicks and does **not** implement physics; it waits a few ticks after Forge explosion events, scans loaded chunks around affected blocks, and nudges Minecraft block/neighbor updates so Realistic Block Physics or similar mods can re-evaluate the world naturally.

## What it does

- Listens to Forge `ExplosionEvent.Detonate` on the server.
- Copies affected block coordinates immediately and delays scanning by `delayTicks` (default: `2`).
- Merges near-in-time explosion work per dimension and deduplicates candidate positions.
- Checks only loaded chunks by default and respects build-height bounds.
- Ignores block entities and fluid targets by default.
- Uses blacklist/whitelist filtering and per-tick budgets to stay server-friendly.
- Calls normal block/neighbor update APIs; it never destroys blocks, spawns FallingBlocks, or changes explosion strength.
- Runs without a hard dependency on Realistic Block Physics and logs an optional informational warning if no known mod id is present.

## What it does not do

- Replace or modify explosions.
- Bypass claims, permissions, or protection mods.
- Load chunks.
- Touch `ServerLevel`, `BlockState`, chunks, block entities, or other Minecraft world objects off-thread.
- Fake left-click/player interaction.
- Spawn entities or implement a physics engine.

## Installation

Install the JAR on the Forge 1.20.1 server. Client installation is normally unnecessary for this server-side helper, but some launchers/modpack policies may still require matching mod lists.

Server config path:

```text
world/serverconfig/realistic_block_physics_fixer-server.toml
```

## Build

Use the Gradle 8 wrapper entrypoint:

```bash
./gradlew clean build
```

The output JAR is written to:

```text
build/libs/Realistic_Block_Physics_fixer-<version>.jar
```

ForgeGradle 6.x is not Gradle-9-ready. Do not build this project with Gradle 9; use the checked-in `./gradlew` entrypoint or another Gradle 8.x installation. The wrapper JAR is a maintainer-generated binary; if it is missing, regenerate it with `gradle wrapper --gradle-version 8.8 --distribution-type bin`, then use `./gradlew clean build`.

## Important config values

- `enabled=true`
- `warnIfRealisticBlockPhysicsMissing=true`
- `asyncPlanningEnabled=true`
- `delayTicks=2` (range 1-10)
- `smallScanRadius=5` (range 2-12)
- `maxBlockChecksPerScan=3000`
- `maxBlockUpdatesPerScan=1500`
- `maxQueuedScans=30`
- `maxScansPerTick=4`
- `maxPositionsCheckedPerTick=6000`
- `maxUpdatesPerTick=1000`
- `maxScanAgeTicks=200`
- `onlyLoadedChunks=true`
- `ignoreBlockEntities=true`
- `ignoreFluids=true`
- `blacklistMode=true`

For weapon-heavy packs (SuperbWarfare, TaCZ addons, large TNT chains), keep `delayTicks=2`, increase scan radius only cautiously, and prefer raising per-tick budgets gradually while watching MSPT with Spark.

## Known limits

This mod only nudges updates. Whether a block actually falls depends on Minecraft, Forge, and the installed physics mod. If another mod suppresses Forge explosion events or modifies terrain after the scan window, a manual rescan/restart may still be needed.

## Manual test plan

See [`docs/TESTING.md`](docs/TESTING.md) for TNT, chain explosion, weapon-mod, block-entity, chunk-boundary, and stress-test scenarios.
