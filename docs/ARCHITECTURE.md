# Architecture

## Runtime pipeline

1. `ForgeEventHandler` catches `ExplosionEvent.Detonate` and server ticks with crash-safe wrappers.
2. `ExplosionFixCoordinator` copies immutable explosion data (`BlockPos.asLong` values), applies delay/queue limits, clusters only already-due nearby explosions per dimension, and enforces per-tick budgets.
3. `ScanPlanner` turns copied coordinates into deduplicated scan candidates. Async planning is enabled by default and only processes primitive coordinate data; it never touches Minecraft world objects.
4. `ActiveScan` checks candidates incrementally on the server thread.
5. `FloatingBlockDetector` applies world-state checks: loaded chunk, bounds, non-air, non-fluid, not unbreakable, block entity policy, block filter, and weak/missing support below.
6. `PhysicsUpdateDispatcher` dispatches block and neighbor updates without changing states, creating drops, spawning entities, or faking players.
7. `RecentUpdateCache` reduces duplicate updates during chain explosions.

## Threading rule

World access is server-thread-only. The following must never happen async/off-thread:

- `ServerLevel.hasChunkAt`
- `ServerLevel.getBlockState`
- `ServerLevel.getBlockEntity`
- `ChunkAccess` access
- `neighborChanged`, `updateNeighborsAt`, `sendBlockUpdated`

Async work, if enabled, is limited to immutable coordinate deduplication/sorting/batch planning.

## Performance controls

The server config controls scan radius, per-explosion max checks/updates, global per-tick check/update budgets, active-scan count, max scans visited per tick, stale-scan age, duplicate update cooldown, block filters, and optional FallingBlock soft guard.
