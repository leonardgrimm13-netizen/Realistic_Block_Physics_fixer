# Testing plan

Run these tests on a dedicated Forge 1.20.1 server with the target modpack.

## Single TNT

1. Place TNT near sand/gravel/unsupported terrain.
2. Ignite TNT.
3. Confirm a scan is queued after `delayTicks` and unsupported blocks receive updates.

## TNT chain

1. Build a chain of TNT in terrain with overhangs.
2. Watch TPS/MSPT.
3. Confirm duplicate work is reduced and the server does not freeze.

## Large explosion

1. Use a controlled large explosion from the pack.
2. Confirm queue limits and max scan/update budgets prevent a large single-tick spike.

## SuperbWarfare explosion

1. Trigger a representative SuperbWarfare explosive.
2. Confirm Forge explosion events produce delayed scans and no chunks are force-loaded.

## TaCZ/Addons explosion

1. Trigger a representative explosive projectile/grenade.
2. Confirm affected areas are scanned if Forge reports affected blocks or a center.

## BlockEntity test

1. Place chests/machines/cables near an explosion area.
2. With `ignoreBlockEntities=true`, confirm block entities are not directly targeted.

## Chunk-boundary test

1. Trigger explosions near loaded/unloaded chunk edges.
2. Confirm unloaded chunks are skipped and not loaded by the fixer.

## Server stress test

1. Install Spark or equivalent profiler.
2. Run TNT-chain and weapon-mod scenarios.
3. Track TPS/MSPT, log spam, queue drops, and config budget suitability.

## Regression checks

- No fake player or player interaction is created.
- No FallingBlock entities are spawned by this mod.
- No `setBlock` is used to force state changes.
- Debug logs remain quiet unless `debugLogging=true`.
