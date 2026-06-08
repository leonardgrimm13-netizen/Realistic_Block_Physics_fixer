package de.leo.realisticblockphysicsfixer.runtime;

import de.leo.realisticblockphysicsfixer.RealisticBlockPhysicsFixer;
import de.leo.realisticblockphysicsfixer.config.RBPFConfig;
import de.leo.realisticblockphysicsfixer.filter.BlockFilters;
import de.leo.realisticblockphysicsfixer.scan.ActiveScan;
import de.leo.realisticblockphysicsfixer.scan.ExplosionRecord;
import de.leo.realisticblockphysicsfixer.scan.PlanRequest;
import de.leo.realisticblockphysicsfixer.scan.PlanningJob;
import de.leo.realisticblockphysicsfixer.scan.ScanPlan;
import de.leo.realisticblockphysicsfixer.scan.ScanPlanner;
import de.leo.realisticblockphysicsfixer.update.RecentUpdateCache;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class ExplosionFixCoordinator {
    private final List<ExplosionRecord> delayedRecords = new ArrayList<>();
    private final List<PlanningJob> planningJobs = new ArrayList<>();
    private final ArrayDeque<ActiveScan> activeScans = new ArrayDeque<>();
    private final ScanPlanner planner = new ScanPlanner();
    private final RecentUpdateCache recentUpdates = new RecentUpdateCache();

    private long droppedExplosionRecords;
    private long droppedPlans;
    private long lastFilterRefreshTick = -1L;

    public void resetForServerStart() {
        delayedRecords.clear();
        planningJobs.clear();
        activeScans.clear();
        recentUpdates.clear();
        planner.ensureRunning();
        droppedExplosionRecords = 0;
        droppedPlans = 0;
        lastFilterRefreshTick = -1L;
    }

    public void shutdownForServerStop() {
        delayedRecords.clear();
        planningJobs.clear();
        activeScans.clear();
        recentUpdates.clear();
        planner.shutdown();
    }

    public void onExplosion(ExplosionEvent.Detonate event) {
        if (!RBPFConfig.ENABLED.get()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = getExplosionCenter(event.getExplosion()).orElse(null);
        List<BlockPos> affectedBlocks = event.getAffectedBlocks();
        int originalAffectedCount = affectedBlocks == null ? 0 : affectedBlocks.size();
        if (originalAffectedCount == 0 && center == null) {
            return;
        }

        int maxCaptured = RBPFConfig.MAX_AFFECTED_POSITIONS_CAPTURED.get();
        List<Long> affected = new ArrayList<>(Math.min(originalAffectedCount, maxCaptured));
        int copied = 0;
        if (affectedBlocks != null) {
            for (BlockPos pos : affectedBlocks) {
                if (copied >= maxCaptured) {
                    break;
                }
                affected.add(pos.asLong());
                copied++;
            }
        }

        BlockPos centerPos = center != null ? BlockPos.containing(center) : approximateCenterFromAffected(affected);
        long dueTick = level.getGameTime() + RBPFConfig.DELAY_TICKS.get();
        ExplosionRecord record = new ExplosionRecord(
                level.dimension(),
                centerPos.asLong(),
                affected,
                originalAffectedCount,
                level.getMinBuildHeight(),
                level.getMaxBuildHeight(),
                level.getGameTime(),
                dueTick
        );

        if (delayedRecords.size() >= RBPFConfig.MAX_QUEUED_EXPLOSION_RECORDS.get()) {
            delayedRecords.remove(0);
            droppedExplosionRecords++;
        }
        delayedRecords.add(record);

        if (RBPFConfig.DEBUG_LOGGING.get()) {
            RealisticBlockPhysicsFixer.LOGGER.info(
                    "[{}] Explosion queued: dim={}, affected={}, copied={}, dueTick={}",
                    RealisticBlockPhysicsFixer.MOD_ID,
                    level.dimension().location(),
                    originalAffectedCount,
                    copied,
                    dueTick
            );
        }
    }

    public void onServerTick() {
        if (!RBPFConfig.ENABLED.get()) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        long now = server.overworld().getGameTime();
        refreshFiltersOccasionally(now);
        recentUpdates.cleanup(now);

        collectFinishedPlanningJobs(server, now);
        startDuePlanningJobs(server, now);
        processActiveScans(server, now);
    }

    private static Optional<Vec3> getExplosionCenter(Explosion explosion) {
        if (explosion == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(explosion.getPosition());
    }

    private static BlockPos approximateCenterFromAffected(List<Long> affected) {
        if (affected.isEmpty()) {
            return BlockPos.ZERO;
        }
        long x = 0;
        long y = 0;
        long z = 0;
        for (long packed : affected) {
            x += BlockPos.getX(packed);
            y += BlockPos.getY(packed);
            z += BlockPos.getZ(packed);
        }
        int count = affected.size();
        return new BlockPos((int) (x / count), (int) (y / count), (int) (z / count));
    }

    private void refreshFiltersOccasionally(long now) {
        if (lastFilterRefreshTick < 0 || now - lastFilterRefreshTick >= 20) {
            BlockFilters.refreshFromConfig();
            lastFilterRefreshTick = now;
        }
    }

    private void collectFinishedPlanningJobs(MinecraftServer server, long now) {
        planningJobs.removeIf(job -> {
            CompletableFuture<ScanPlan> future = job.future();
            if (!future.isDone()) {
                return false;
            }

            try {
                ScanPlan plan = future.join();
                if (plan.candidates().isEmpty()) {
                    return true;
                }

                if (activeScans.size() >= RBPFConfig.MAX_ACTIVE_SCANS.get()) {
                    droppedPlans++;
                    if (RBPFConfig.DEBUG_LOGGING.get()) {
                        RealisticBlockPhysicsFixer.LOGGER.warn(
                                "[{}] Dropping completed scan plan because maxActiveScans was reached. droppedPlans={}",
                                RealisticBlockPhysicsFixer.MOD_ID,
                                droppedPlans
                        );
                    }
                    return true;
                }

                ServerLevel level = server.getLevel(plan.dimension());
                if (level == null) {
                    return true;
                }

                ActiveScan activeScan = new ActiveScan(plan, now);
                if (activeScan.shouldSkipBecauseOfFallingBlockGuard(level)) {
                    if (RBPFConfig.DEBUG_LOGGING.get()) {
                        RealisticBlockPhysicsFixer.LOGGER.info(
                                "[{}] Skipped scan because FallingBlock soft guard was reached at {}.",
                                RealisticBlockPhysicsFixer.MOD_ID,
                                BlockPos.of(plan.center())
                        );
                    }
                    return true;
                }

                activeScans.add(activeScan);
                if (RBPFConfig.DEBUG_LOGGING.get()) {
                    RealisticBlockPhysicsFixer.LOGGER.info(
                            "[{}] Scan plan activated: dim={}, candidates={}, radius={}, large={}",
                            RealisticBlockPhysicsFixer.MOD_ID,
                            plan.dimension().location(),
                            plan.candidates().size(),
                            plan.scanRadius(),
                            plan.largeProfile()
                    );
                }
            } catch (Throwable throwable) {
                RealisticBlockPhysicsFixer.LOGGER.error("[{}] Failed to activate scan plan.", RealisticBlockPhysicsFixer.MOD_ID, throwable);
            }
            return true;
        });
    }

    private void startDuePlanningJobs(MinecraftServer server, long now) {
        if (planningJobs.size() >= RBPFConfig.MAX_PLANNING_JOBS.get()) {
            return;
        }

        List<ExplosionRecord> due = new ArrayList<>();
        long mergeWindow = RBPFConfig.MERGE_WINDOW_TICKS.get();
        delayedRecords.removeIf(record -> {
            if (record.dueTick() <= now + mergeWindow) {
                due.add(record);
                return true;
            }
            return false;
        });

        if (due.isEmpty()) {
            return;
        }

        Map<ResourceKey<Level>, List<ExplosionRecord>> byDimension = new HashMap<>();
        for (ExplosionRecord record : due) {
            byDimension.computeIfAbsent(record.dimension(), ignored -> new ArrayList<>()).add(record);
        }

        for (Map.Entry<ResourceKey<Level>, List<ExplosionRecord>> entry : byDimension.entrySet()) {
            if (planningJobs.size() >= RBPFConfig.MAX_PLANNING_JOBS.get()) {
                // Put remaining records back instead of silently losing them.
                delayedRecords.addAll(entry.getValue());
                continue;
            }

            ServerLevel level = server.getLevel(entry.getKey());
            if (level == null) {
                continue;
            }

            PlanRequest request = mergeRecordsIntoPlanRequest(entry.getKey(), entry.getValue(), level);
            CompletableFuture<ScanPlan> future = planner.planAsync(request);
            planningJobs.add(new PlanningJob(future, now));
        }
    }

    private PlanRequest mergeRecordsIntoPlanRequest(ResourceKey<Level> dimension, List<ExplosionRecord> records, ServerLevel level) {
        records.sort(Comparator.comparingLong(ExplosionRecord::createdTick));

        int totalAffected = 0;
        for (ExplosionRecord record : records) {
            totalAffected += record.originalAffectedBlockCount();
        }
        boolean large = totalAffected >= RBPFConfig.LARGE_EXPLOSION_AFFECTED_BLOCK_THRESHOLD.get();

        int radius = large ? RBPFConfig.LARGE_SCAN_RADIUS.get() : RBPFConfig.SMALL_SCAN_RADIUS.get();
        int maxChecks = large ? RBPFConfig.LARGE_MAX_BLOCK_CHECKS_PER_SCAN.get() : RBPFConfig.MAX_BLOCK_CHECKS_PER_SCAN.get();
        int maxUpdates = large ? RBPFConfig.LARGE_MAX_BLOCK_UPDATES_PER_SCAN.get() : RBPFConfig.MAX_BLOCK_UPDATES_PER_SCAN.get();

        LinkedHashSet<Long> seeds = new LinkedHashSet<>();
        long center = records.get(0).center();
        for (ExplosionRecord record : records) {
            seeds.add(record.center());
            for (long pos : record.affectedPositions()) {
                seeds.add(pos);
                if (seeds.size() >= maxChecks) {
                    break;
                }
            }
            if (seeds.size() >= maxChecks) {
                break;
            }
        }

        return new PlanRequest(
                dimension,
                center,
                new ArrayList<>(seeds),
                radius,
                maxChecks,
                maxUpdates,
                level.getMinBuildHeight(),
                level.getMaxBuildHeight(),
                RBPFConfig.MAX_ASYNC_PLANNING_VOLUME.get(),
                large
        );
    }

    private void processActiveScans(MinecraftServer server, long now) {
        int checksBudget = RBPFConfig.MAX_BLOCK_CHECKS_PER_TICK.get();
        int updatesBudget = RBPFConfig.MAX_BLOCK_UPDATES_PER_TICK.get();
        int scansToVisit = Math.min(activeScans.size(), RBPFConfig.MAX_SCANS_PER_TICK.get());

        for (int i = 0; i < scansToVisit && checksBudget > 0 && updatesBudget > 0; i++) {
            ActiveScan scan = activeScans.pollFirst();
            if (scan == null) {
                return;
            }

            if (now - scan.activatedTick() > RBPFConfig.MAX_SCAN_AGE_TICKS.get()) {
                if (RBPFConfig.DEBUG_LOGGING.get()) {
                    RealisticBlockPhysicsFixer.LOGGER.warn("[{}] Dropping stale scan in {} after {} ticks.", RealisticBlockPhysicsFixer.MOD_ID, scan.dimension().location(), now - scan.activatedTick());
                }
                continue;
            }

            ServerLevel level = server.getLevel(scan.dimension());
            if (level == null) {
                continue;
            }

            ActiveScan.TickResult result;
            try {
                result = scan.process(level, recentUpdates, now, checksBudget, updatesBudget);
            } catch (Throwable throwable) {
                RealisticBlockPhysicsFixer.LOGGER.error(
                        "[{}] Scan crashed and was aborted to keep the server running.",
                        RealisticBlockPhysicsFixer.MOD_ID,
                        throwable
                );
                continue;
            }

            checksBudget -= result.checksUsed();
            updatesBudget -= result.updatesUsed();

            if (!scan.isFinished()) {
                activeScans.addLast(scan);
            } else if (RBPFConfig.DEBUG_LOGGING.get() && RBPFConfig.SUMMARY_LOGGING.get()) {
                RealisticBlockPhysicsFixer.LOGGER.info(
                        "[{}] Scan done: dim={}, candidates={}, checked={}, suspicious={}, updated={}, skippedUnloaded={}, skippedDuplicate={}, droppedPending={}",
                        RealisticBlockPhysicsFixer.MOD_ID,
                        scan.dimension().location(),
                        scan.totalCandidates(),
                        scan.checkedCount(),
                        scan.suspiciousCount(),
                        scan.updatedCount(),
                        scan.skippedUnloadedCount(),
                        scan.skippedDuplicateCount(),
                        scan.droppedPendingUpdateCount()
                );
            }
        }
    }
}
