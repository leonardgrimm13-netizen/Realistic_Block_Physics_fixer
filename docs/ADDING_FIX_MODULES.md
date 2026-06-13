# Adding Fix Modules

## 1. Create a module package

Create a new package under:

```text
src/main/java/de/KrasserStecher12/realisticblockphysicsfixer/fixes/<name>/
```

Example:

```text
fixes/snow/SnowLayerCollisionFixModule.java
```

## 2. Implement `FixModule`

Implement `core/FixModule` and provide a stable ID:

```java
public final class SnowLayerCollisionFixModule implements FixModule {
    public String id() { return "snow_layer_collision_fix"; }
    public String displayName() { return "Snow Layer Collision Fix"; }
    public boolean isEnabled() { return RBPFConfig.MOD_ENABLED.get() && RBPFConfig.SNOW_ENABLED.get(); }
    public void onRegister(FixContext context) { }
}
```

Keep the module small. Put coordinator/scanner/helper logic in separate classes inside the same fix package.

## 3. Add config

Add module config under an appropriate category in `RBPFConfig`, usually `modules.<name>`.

Include at least:

- `enabled`,
- per-tick budget values,
- queue limits,
- safety toggles.

## 4. Register the module

Add the module in `RealisticBlockPhysicsFixer.registerModules`:

```java
registry.register(new SnowLayerCollisionFixModule());
```

The registry prevents duplicate IDs.

## 5. Wire events and ticks cleanly

Prefer central callbacks from `ServerTickScheduler` and `FixModuleRegistry`.

If a module needs a new Forge event type, add one clear callback to `FixModule`, dispatch it from `ServerTickScheduler`, and document which module uses it. Do not register multiple anonymous event handlers unless there is a strong reason.

## 6. Respect budgets and thread rules

Never access Minecraft world objects off-thread. Keep async work limited to immutable data.

Every module should have queue limits and per-tick limits. If overloaded, defer or drop work.

## 7. Add debug stats

Return `ModuleStats` from `stats()` and include useful counters:

- detected events,
- queued work,
- completed work,
- skipped work,
- dropped work,
- errors.

## 8. Document test cases

Update `docs/TESTING.md` with module-specific tests:

- enabled/disabled behavior,
- normal scenario,
- overload scenario,
- chunk boundary behavior,
- server-thread safety assumptions,
- command/status visibility.
