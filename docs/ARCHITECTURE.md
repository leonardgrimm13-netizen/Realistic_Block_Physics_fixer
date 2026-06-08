# Architecture

RBPF is a modular server-side fix platform for Forge 1.20.1.

## Core system

`core/FixModuleRegistry` owns all registered modules. It prevents duplicate IDs, exposes lookup/status APIs, and wraps each module call so a broken module does not crash the whole server.

`core/ServerTickScheduler` is the single Forge Event Bus subscriber. It receives server lifecycle events, server ticks, explosion events, and command registration, then delegates to the registry. This keeps event registration centralized and avoids duplicate or chaotic event wiring.

`core/BudgetManager` provides a global per-tick work budget. Individual modules also keep their own detailed budgets.

`core/RateLimitedLogger` prevents repeated warnings from flooding server logs.

## FixModule interface

A module implements:

- `id()` - stable machine-readable ID.
- `displayName()` - human-readable name.
- `isEnabled()` - checks global and module config.
- `onRegister(FixContext)` - registration-time setup.
- `onServerStarted(MinecraftServer)` - reset state.
- `onExplosionDetonate(ExplosionEvent.Detonate)` - optional event callback.
- `onServerTick(MinecraftServer, BudgetManager)` - budgeted per-tick work.
- `onServerStopping(MinecraftServer)` / `shutdown()` - cleanup.
- `stats()` - command/debug status.

## Lifecycle

1. Mod constructor registers Forge config.
2. Mod constructor creates `FixModuleRegistry`.
3. Modules are registered once.
4. `ServerTickScheduler` is registered once on `MinecraftForge.EVENT_BUS`.
5. Server start resets module state.
6. Forge events/ticks are delegated through the registry.
7. Server stop shuts down module state.

## Server-thread-only rule

Minecraft world access must stay on the server thread:

- `ServerLevel` / `Level` access,
- `BlockState` reads,
- `BlockEntity` reads,
- chunk loaded checks,
- block and neighbor updates,
- entity operations,
- Forge event handling.

Async work is allowed only for immutable data such as packed long coordinates, sorting, deduplication, or plan construction without world objects.

## Budget system

There are two levels of protection:

1. `performance.maxGlobalWorkPerTick` limits total platform work.
2. Module configs limit module-specific work, for example explosion scan counts, checked positions, updates, active scans, queued records, and planning jobs.

When overloaded, modules should defer work to a later tick or drop stale/excess work.

## Explosion module example

`fixes/explosion/ExplosionPhysicsUpdateFixModule` owns the explosion fix. It delegates detailed queueing and processing to `ExplosionFixCoordinator`.

Flow:

1. `ExplosionEvent.Detonate` is received on the server.
2. Center and affected positions are copied as immutable packed longs.
3. Work is queued for `delayTicks` in the same dimension.
4. Due explosions are clustered only when temporally and spatially close.
5. Coordinate plans can be generated async without world access.
6. Active scans run on the server thread with budgets.
7. Only loaded chunks are checked.
8. Suspicious blocks receive block/neighbor updates.
