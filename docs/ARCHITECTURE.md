# Architecture

## Pipeline

1. `ForgeEventHandler` listens to `ExplosionEvent.Detonate`.
2. `ExplosionFixCoordinator` stores a small immutable explosion record.
3. After `delayTicks`, due explosions are merged per dimension.
4. `ScanPlanner` builds candidate positions asynchronously without touching the world.
5. Completed plans become `ActiveScan` instances.
6. `ActiveScan` checks blocks and triggers updates with per-tick budgets on the server thread.
7. `RecentUpdateCache` avoids hammering the same block repeatedly during chain explosions.

## Threading rule

Only pure data work is async. These actions are never async:

- `level.getBlockState`
- `level.getBlockEntity`
- `level.hasChunkAt`
- `level.neighborChanged`
- `level.updateNeighborsAt`
- `level.sendBlockUpdated`

## Performance controls

- max queued explosion records
- max planning jobs
- max active scans
- max generated candidates per scan
- max block checks per tick
- max block updates per tick
- duplicate update cooldown
- block blacklist/whitelist
- optional falling-block soft guard
