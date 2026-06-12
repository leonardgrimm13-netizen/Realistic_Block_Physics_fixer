# Testing

## Build test

Run:

```bash
./gradlew clean build
```

Expected: build succeeds with Java 17, Gradle Wrapper 8.8, and ForgeGradle 6.x.

## Server start test

1. Put the built jar in a Forge 1.20.1 dedicated server `mods/` folder.
2. Start the server.
3. Confirm the log says RBPF loaded and registered modules.
4. Confirm `config/realistic_block_physics_fixer-server.toml` is generated.

## Explosion fix test

1. Install Realistic Block Physics and RBPF.
2. Create a controlled explosion near unsupported blocks.
3. Confirm the explosion fix queues work after `ExplosionEvent.Detonate`.
4. Confirm scans do not run before `modules.explosion.delayTicks`.
5. Confirm only loaded chunks are checked.
6. Confirm suspicious blocks receive updates and are re-evaluated by the physics mod.
7. Confirm RBPF does not spawn FallingBlocks, destroy blocks, or simulate player actions.

## Config test

Test these cases:

- `general.modEnabled=false` disables all expensive module work.
- `modules.explosion.enabled=false` disables explosion work only.
- `modules.explosion.onlyLoadedChunks=true` avoids chunk loading.
- blacklist/whitelist entries affect targeted blocks.
- `debug.debugLogging=false` suppresses verbose logs.

## Command test

As an admin (permission level 2+), run:

```text
/rbpf status
/rbpf modules
/rbpf debug on
/rbpf debug off
/rbpf explosion stats
```

As a normal player, confirm the commands are unavailable.

## Performance test

Use Spark, timings, or MSPT monitoring:

1. Trigger several explosions.
2. Observe that work is spread across ticks.
3. Lower budgets and confirm work is deferred/dropped instead of freezing the server.
4. Confirm warnings are rate-limited.

## Disabled-module test

Set `modules.explosion.enabled=false`, restart or reload as appropriate, trigger explosions, then confirm:

- no explosion scans are queued,
- `/rbpf modules` reports the module disabled,
- `/rbpf explosion stats` counters do not increase from disabled explosions.
