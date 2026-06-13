package de.KrasserStecher12.realisticblockphysicsfixer.fixes.entityguard;

import de.KrasserStecher12.realisticblockphysicsfixer.RealisticBlockPhysicsFixer;
import de.KrasserStecher12.realisticblockphysicsfixer.config.RBPFConfig;
import de.KrasserStecher12.realisticblockphysicsfixer.core.BudgetManager;
import de.KrasserStecher12.realisticblockphysicsfixer.core.FixContext;
import de.KrasserStecher12.realisticblockphysicsfixer.core.FixModule;
import de.KrasserStecher12.realisticblockphysicsfixer.debug.DebugStats;
import de.KrasserStecher12.realisticblockphysicsfixer.debug.ModuleStats;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Server-only defensive guard for RBP falling block entities.
 *
 * <p>This module intentionally avoids compile-time references to RBP classes.  It identifies the entity by
 * registry id/class name and uses reflection only for the private age counter that makes RBP discard active blocks
 * after a hard-coded lifetime.</p>
 */
public final class FallingBlockEntityGuardFixModule implements FixModule {
    public static final String MODULE_ID = "falling_block_entity_guard";

    private final DebugStats debugStats = new DebugStats();
    private final Map<UUID, TrackedEntity> trackedEntities = new HashMap<>();

    private FixContext context;
    private long lastRunTick = -1L;
    private long totalRuns;
    private long totalErrors;
    private String lastErrorMessage = "";

    @Override
    public String id() {
        return MODULE_ID;
    }

    @Override
    public String displayName() {
        return "RBP Falling Block Entity Guard";
    }

    @Override
    public boolean isEnabled() {
        return RBPFConfig.FALLING_BLOCK_GUARD_ENABLED.get();
    }

    @Override
    public void onRegister(FixContext context) {
        this.context = context;
    }

    @Override
    public void onServerTick(MinecraftServer server, BudgetManager budget) {
        if (server == null || !server.isSameThread()) {
            return;
        }

        long now = server.overworld().getGameTime();
        int interval = Math.max(1, RBPFConfig.FALLING_BLOCK_GUARD_SCAN_INTERVAL_TICKS.get());
        if (lastRunTick >= 0 && now - lastRunTick < interval) {
            return;
        }

        lastRunTick = now;
        totalRuns++;
        pruneOldTracking(now);

        int globalSeen = 0;
        int globalKeptAlive = 0;
        int globalEmergencyDiscarded = 0;
        int globalVisited = 0;

        try {
            for (ServerLevel level : server.getAllLevels()) {
                ScanResult result = scanLevel(level, budget, now);
                globalSeen += result.seen();
                globalKeptAlive += result.keptAlive();
                globalEmergencyDiscarded += result.emergencyDiscarded();
                globalVisited += result.visited();
                if (budget.remainingGlobalWork() <= 0) {
                    debugStats.increment("budgetExhausted");
                    break;
                }
            }
        } catch (Throwable throwable) {
            recordError(throwable);
            context.rateLimitedLogger().warn(
                    MODULE_ID + ".tick",
                    "[{}] Falling block guard failed during server tick: {}",
                    RealisticBlockPhysicsFixer.MOD_ID,
                    throwable.toString()
            );
        }

        if (globalVisited > 0) {
            debugStats.add("visited", globalVisited);
        }
        if (globalSeen > 0) {
            debugStats.add("seen", globalSeen);
        }
        if (globalKeptAlive > 0) {
            debugStats.add("keptAlive", globalKeptAlive);
        }
        if (globalEmergencyDiscarded > 0) {
            debugStats.add("emergencyDiscarded", globalEmergencyDiscarded);
        }
    }

    private ScanResult scanLevel(ServerLevel level, BudgetManager budget, long now) {
        if (level == null) {
            return ScanResult.EMPTY;
        }

        int maxVisited = RBPFConfig.FALLING_BLOCK_GUARD_MAX_ENTITIES_VISITED_PER_LEVEL.get();
        int maxProcessed = RBPFConfig.FALLING_BLOCK_GUARD_MAX_ENTITIES_SCANNED_PER_LEVEL.get();
        int softLimit = RBPFConfig.FALLING_BLOCK_GUARD_SOFT_LIMIT_PER_LEVEL.get();
        int hardLimit = RBPFConfig.FALLING_BLOCK_GUARD_HARD_LIMIT_PER_LEVEL.get();
        int visited = 0;
        int seen = 0;
        int processed = 0;
        int keptAlive = 0;
        int emergencyDiscarded = 0;

        for (Entity entity : level.getAllEntities()) {
            visited++;
            if (visited > maxVisited) {
                debugStats.increment("scanVisitLimitReached");
                break;
            }

            if (!isRealisticFallingBlock(entity)) {
                continue;
            }

            seen++;

            if (RBPFConfig.FALLING_BLOCK_GUARD_EMERGENCY_DISCARD_ABOVE_HARD_LIMIT.get()
                    && seen > hardLimit
                    && isSafeEmergencyDiscardCandidate(entity)) {
                entity.discard();
                emergencyDiscarded++;
                continue;
            }

            if (processed >= maxProcessed || !budget.tryConsume(1)) {
                continue;
            }
            processed++;

            TrackDecision decision = trackAndMaybeKeepAlive(entity, now);
            if (decision == TrackDecision.KEPT_ALIVE) {
                keptAlive++;
            }
        }

        if (seen > softLimit) {
            context.rateLimitedLogger().warn(
                    MODULE_ID + ".softLimit." + level.dimension().location(),
                    "[{}] RBP falling block count in {} is above soft limit: {} > {}. "
                            + "Consider lowering explosion limits or enabling emergency discard only as a last resort.",
                    RealisticBlockPhysicsFixer.MOD_ID,
                    level.dimension().location(),
                    seen,
                    softLimit
            );
        }

        return new ScanResult(visited, seen, keptAlive, emergencyDiscarded);
    }

    private TrackDecision trackAndMaybeKeepAlive(Entity entity, long now) {
        UUID uuid = entity.getUUID();
        BlockPos pos = entity.blockPosition();
        Vec3 delta = entity.getDeltaMovement();
        TrackedEntity previous = trackedEntities.get(uuid);
        int stillTicks = previous == null || !previous.position().equals(pos) || delta.lengthSqr() > 1.0E-5D
                ? 0
                : previous.stillTicks() + Math.max(1, RBPFConfig.FALLING_BLOCK_GUARD_SCAN_INTERVAL_TICKS.get());
        trackedEntities.put(uuid, new TrackedEntity(pos.immutable(), now, stillTicks));

        FallingBlockGuardSupport.KeepAliveResult result = FallingBlockGuardSupport.keepAliveIfNeeded(entity, stillTicks, "scan");
        if (result == FallingBlockGuardSupport.KeepAliveResult.RESET) {
            return TrackDecision.KEPT_ALIVE;
        }
        if (result == FallingBlockGuardSupport.KeepAliveResult.REFLECTION_UNAVAILABLE) {
            debugStats.increment("keepAliveReflectionUnavailable");
        } else if (result == FallingBlockGuardSupport.KeepAliveResult.UNSAFE_POSITION) {
            debugStats.increment("keepAliveSkippedUnsafePosition");
        }
        return TrackDecision.NONE;
    }

    private static boolean isRealisticFallingBlock(Entity entity) {
        if (entity == null) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id != null && FallingBlockGuardSupport.RBP_ENTITY_ID.equals(id.toString())) {
            return true;
        }
        return FallingBlockGuardSupport.RBP_ENTITY_CLASS.equals(entity.getClass().getName());
    }

    private static boolean isSafeEmergencyDiscardCandidate(Entity entity) {
        return !entity.isPassenger() && !entity.isVehicle() && !entity.onGround();
    }

    private void pruneOldTracking(long now) {
        long maxAge = Math.max(200L, RBPFConfig.FALLING_BLOCK_GUARD_TRACKING_TTL_TICKS.get());
        Iterator<Map.Entry<UUID, TrackedEntity>> iterator = trackedEntities.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TrackedEntity> entry = iterator.next();
            if (now - entry.getValue().lastSeenTick() > maxAge) {
                iterator.remove();
            }
        }
    }

    private void recordError(Throwable throwable) {
        totalErrors++;
        lastErrorMessage = throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
        debugStats.increment("errors");
    }

    @Override
    public ModuleStats stats() {
        return new ModuleStats(
                MODULE_ID,
                isEnabled(),
                trackedEntities.size(),
                lastRunTick,
                totalRuns,
                totalErrors,
                lastErrorMessage,
                statsSnapshot()
        );
    }

    private Map<String, Long> statsSnapshot() {
        Map<String, Long> snapshot = new HashMap<>(debugStats.snapshot());
        snapshot.put("mixinKeepAliveResets", FallingBlockGuardSupport.mixinKeepAliveResets());
        snapshot.put("mixinSkippedUnsafePosition", FallingBlockGuardSupport.mixinSkippedUnsafePosition());
        snapshot.put("reflectionFailures", FallingBlockGuardSupport.reflectionFailures());
        if (FallingBlockGuardSupport.isReflectionAvailable()) {
            snapshot.put("reflectionAvailable", 1L);
        }
        if (FallingBlockGuardSupport.isReflectionUnavailable()) {
            snapshot.put("reflectionUnavailable", 1L);
        }
        return Map.copyOf(snapshot);
    }

    @Override
    public void shutdown() {
        trackedEntities.clear();
    }

    private record TrackedEntity(BlockPos position, long lastSeenTick, int stillTicks) {
    }

    private record ScanResult(int visited, int seen, int keptAlive, int emergencyDiscarded) {
        private static final ScanResult EMPTY = new ScanResult(0, 0, 0, 0);
    }

    private enum TrackDecision {
        NONE,
        KEPT_ALIVE
    }
}
