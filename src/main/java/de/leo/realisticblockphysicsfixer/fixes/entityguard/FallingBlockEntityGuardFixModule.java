package de.leo.realisticblockphysicsfixer.fixes.entityguard;

import de.leo.realisticblockphysicsfixer.RealisticBlockPhysicsFixer;
import de.leo.realisticblockphysicsfixer.config.RBPFConfig;
import de.leo.realisticblockphysicsfixer.core.BudgetManager;
import de.leo.realisticblockphysicsfixer.core.FixContext;
import de.leo.realisticblockphysicsfixer.core.FixModule;
import de.leo.realisticblockphysicsfixer.debug.DebugStats;
import de.leo.realisticblockphysicsfixer.debug.ModuleStats;
import de.leo.realisticblockphysicsfixer.util.ChunkSafety;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
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

    private static final String RBP_ENTITY_ID = "rbp:falling_block";
    private static final String RBP_ENTITY_CLASS = "xbigellx.rbp.internal.entity.RealisticFallingBlockEntity";
    private static final String FALL_TIME_FIELD = "fallTime";

    private final DebugStats debugStats = new DebugStats();
    private final Map<UUID, TrackedEntity> trackedEntities = new HashMap<>();

    private FixContext context;
    private Field fallTimeField;
    private boolean fallTimeReflectionFailed;
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

        try {
            for (ServerLevel level : server.getAllLevels()) {
                ScanResult result = scanLevel(level, budget, now);
                globalSeen += result.seen();
                globalKeptAlive += result.keptAlive();
                globalEmergencyDiscarded += result.emergencyDiscarded();
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

    private ScanResult scanLevel(ServerLevel level, BudgetManager budget, long now) throws ReflectiveOperationException {
        if (level == null) {
            return ScanResult.EMPTY;
        }

        int maxProcessed = RBPFConfig.FALLING_BLOCK_GUARD_MAX_ENTITIES_SCANNED_PER_LEVEL.get();
        int softLimit = RBPFConfig.FALLING_BLOCK_GUARD_SOFT_LIMIT_PER_LEVEL.get();
        int hardLimit = RBPFConfig.FALLING_BLOCK_GUARD_HARD_LIMIT_PER_LEVEL.get();
        int seen = 0;
        int processed = 0;
        int keptAlive = 0;
        int emergencyDiscarded = 0;

        for (Entity entity : level.getAllEntities()) {
            if (!isRealisticFallingBlock(entity)) {
                continue;
            }

            seen++;
            if (processed >= maxProcessed || !budget.tryConsume(1)) {
                continue;
            }
            processed++;

            TrackDecision decision = trackAndMaybeKeepAlive(level, entity, now);
            if (decision == TrackDecision.KEPT_ALIVE) {
                keptAlive++;
            }

            if (RBPFConfig.FALLING_BLOCK_GUARD_EMERGENCY_DISCARD_ABOVE_HARD_LIMIT.get()
                    && seen > hardLimit
                    && isSafeEmergencyDiscardCandidate(entity)) {
                entity.discard();
                emergencyDiscarded++;
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

        return new ScanResult(seen, keptAlive, emergencyDiscarded);
    }

    private TrackDecision trackAndMaybeKeepAlive(ServerLevel level, Entity entity, long now) throws ReflectiveOperationException {
        UUID uuid = entity.getUUID();
        BlockPos pos = entity.blockPosition();
        Vec3 delta = entity.getDeltaMovement();
        TrackedEntity previous = trackedEntities.get(uuid);
        int stillTicks = previous == null || !previous.position().equals(pos) || delta.lengthSqr() > 1.0E-5D
                ? 0
                : previous.stillTicks() + Math.max(1, RBPFConfig.FALLING_BLOCK_GUARD_SCAN_INTERVAL_TICKS.get());
        trackedEntities.put(uuid, new TrackedEntity(pos.immutable(), now, stillTicks));

        if (!RBPFConfig.FALLING_BLOCK_GUARD_KEEP_ALIVE_ENABLED.get()) {
            return TrackDecision.NONE;
        }
        if (entity.onGround() || entity.isRemoved()) {
            return TrackDecision.NONE;
        }
        if (!isInsideWorld(level, pos) || !ChunkSafety.isLoaded(level, pos)) {
            debugStats.increment("keepAliveSkippedUnsafePosition");
            return TrackDecision.NONE;
        }

        int fallTime = getFallTime(entity);
        int resetAt = RBPFConfig.FALLING_BLOCK_GUARD_KEEP_ALIVE_RESET_AT_TICKS.get();
        if (fallTime < resetAt && stillTicks < RBPFConfig.FALLING_BLOCK_GUARD_STUCK_KEEP_ALIVE_AFTER_TICKS.get()) {
            return TrackDecision.NONE;
        }

        int resetTo = Math.min(RBPFConfig.FALLING_BLOCK_GUARD_KEEP_ALIVE_RESET_TO_TICKS.get(), Math.max(0, resetAt - 1));
        setFallTime(entity, resetTo);
        if (RBPFConfig.isDebugLoggingEnabled()) {
            RealisticBlockPhysicsFixer.LOGGER.debug(
                    "[{}] Extended lifetime of RBP falling block {} at {} in {} (fallTime {} -> {}, stillTicks={}).",
                    RealisticBlockPhysicsFixer.MOD_ID,
                    uuid,
                    pos,
                    level.dimension().location(),
                    fallTime,
                    resetTo,
                    stillTicks
            );
        }
        return TrackDecision.KEPT_ALIVE;
    }

    private static boolean isRealisticFallingBlock(Entity entity) {
        if (entity == null) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id != null && RBP_ENTITY_ID.equals(id.toString())) {
            return true;
        }
        return RBP_ENTITY_CLASS.equals(entity.getClass().getName());
    }

    private static boolean isInsideWorld(ServerLevel level, BlockPos pos) {
        return pos.getY() >= level.getMinBuildHeight() && pos.getY() < level.getMaxBuildHeight();
    }

    private static boolean isSafeEmergencyDiscardCandidate(Entity entity) {
        return !entity.isPassenger() && !entity.isVehicle() && !entity.onGround();
    }

    private int getFallTime(Entity entity) throws ReflectiveOperationException {
        Field field = resolveFallTimeField(entity);
        if (field == null) {
            return Integer.MIN_VALUE;
        }
        return field.getInt(entity);
    }

    private void setFallTime(Entity entity, int value) throws ReflectiveOperationException {
        Field field = resolveFallTimeField(entity);
        if (field != null) {
            field.setInt(entity, value);
        }
    }

    private Field resolveFallTimeField(Entity entity) throws NoSuchFieldException {
        if (fallTimeField != null || fallTimeReflectionFailed) {
            return fallTimeField;
        }
        Class<?> current = entity.getClass();
        while (current != null && current != Entity.class) {
            try {
                Field field = current.getDeclaredField(FALL_TIME_FIELD);
                field.setAccessible(true);
                fallTimeField = field;
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        fallTimeReflectionFailed = true;
        context.rateLimitedLogger().warn(
                MODULE_ID + ".reflection",
                "[{}] Could not find RBP falling block field '{}'; lifetime keep-alive is disabled for this RBP version.",
                RealisticBlockPhysicsFixer.MOD_ID,
                FALL_TIME_FIELD
        );
        return null;
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
                debugStats.snapshot()
        );
    }

    @Override
    public void shutdown() {
        trackedEntities.clear();
    }

    private record TrackedEntity(BlockPos position, long lastSeenTick, int stillTicks) {
    }

    private record ScanResult(int seen, int keptAlive, int emergencyDiscarded) {
        private static final ScanResult EMPTY = new ScanResult(0, 0, 0);
    }

    private enum TrackDecision {
        NONE,
        KEPT_ALIVE
    }
}
