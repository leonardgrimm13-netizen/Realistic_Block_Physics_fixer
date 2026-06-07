# Realistic Block Physics Fixer

Forge 1.20.1 server-side helper mod for modpacks that use **Realistic Block Physics** and heavy explosions.

## What it does

After Forge reports an explosion, this mod waits a configurable number of server ticks, plans a deduplicated scan area, checks suspicious blocks in loaded chunks only, and triggers vanilla server-side neighbor/block updates. This nudges Realistic Block Physics to re-evaluate blocks that are left floating after explosions.

## What it deliberately does not do

- It does not fake player left-clicks.
- It does not destroy blocks.
- It does not spawn FallingBlock entities.
- It does not replace Realistic Block Physics.
- It does not load chunks.
- It does not alter explosion strength, drops, or damage.

## Server-thread and async design

Minecraft worlds are not thread-safe. For that reason this mod never reads or writes block states off-thread. The async part only builds a list of candidate positions from immutable explosion data. Loaded-chunk checks, block-state checks and updates always happen on the main server thread and are spread over ticks with budgets.

## Build

Use Java 17 and Gradle with internet access:

```bash
gradle build
```

The jar will be in:

```text
build/libs/Realistic_Block_Physics_fixer-1.0.0.jar
```

Put the jar into the server `mods/` folder.

## Config

Forge creates this after first start:

```text
<world>/serverconfig/realistic_block_physics_fixer-server.toml
```

Recommended start values for a weapon/explosion modpack are already the defaults:

- delay after explosion: 2 ticks
- small scan radius: 5
- max block checks per scan: 3000
- max updates per scan: 1500
- loaded chunks only: true
- ignore block entities: true
- debug logging: false

## Testing checklist

1. Test one TNT in dirt/stone.
2. Test a TNT chain.
3. Test SuperbWarfare/TaCZ explosive weapons.
4. Test a large bomb.
5. Test near chests/machines.
6. Test near chunk borders.
7. Enable debug logging only while testing.

## Notes

The mod is intentionally conservative. If some floating blocks still remain, increase `smallScanRadius`, `maxBlockChecksPerScan`, or `maxUpdatesPerScan` carefully.

## Build-Fix für Gradle 9

Wenn dein System über SDKMAN bereits Gradle 9.x nutzt, verwende nicht direkt `gradle build`, sondern:

```bash
./build_gradle8.sh
```

Für Debug-Ausgabe:

```bash
./build_gradle8_info.sh
```

Grund: ForgeGradle 6.x unterstützt Gradle 9 noch nicht. Das Projekt baut daher mit Gradle 8.8.
