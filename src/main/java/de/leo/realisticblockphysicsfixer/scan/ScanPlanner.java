package de.leo.realisticblockphysicsfixer.scan;

import de.leo.realisticblockphysicsfixer.RealisticBlockPhysicsFixer;
import de.leo.realisticblockphysicsfixer.config.RBPFConfig;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class ScanPlanner {
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

    private ExecutorService executor;

    public synchronized void ensureRunning() {
        if (executor == null || executor.isShutdown() || executor.isTerminated()) {
            executor = Executors.newSingleThreadExecutor(new PlannerThreadFactory());
        }
    }

    public CompletableFuture<ScanPlan> planAsync(PlanRequest request) {
        if (!RBPFConfig.ASYNC_PLANNING_ENABLED.get()) {
            return CompletableFuture.completedFuture(buildPlan(request));
        }

        ensureRunning();
        return CompletableFuture.supplyAsync(() -> buildPlan(request), executor)
                .exceptionally(throwable -> {
                    RealisticBlockPhysicsFixer.LOGGER.error(
                            "[{}] Async scan planning failed; returning empty plan.",
                            RealisticBlockPhysicsFixer.MOD_ID,
                            throwable
                    );
                    return new ScanPlan(
                            request.dimension(),
                            request.center(),
                            List.of(),
                            request.scanRadius(),
                            request.maxUpdates(),
                            request.largeProfile()
                    );
                });
    }

    public synchronized void shutdown() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private static ScanPlan buildPlan(PlanRequest request) {
        if (request.seeds().isEmpty()) {
            return new ScanPlan(request.dimension(), request.center(), List.of(), request.scanRadius(), request.maxUpdates(), request.largeProfile());
        }

        List<Long> candidates = generateFromSeeds(request);

        return new ScanPlan(
                request.dimension(),
                request.center(),
                candidates,
                request.scanRadius(),
                request.maxUpdates(),
                request.largeProfile()
        );
    }

    private static List<Long> generateFromSeeds(PlanRequest request) {
        List<Offset> offsets = OffsetCache.getOffsets(request.scanRadius());
        LinkedHashSet<Long> candidates = new LinkedHashSet<>(request.maxCandidates());

        candidates.add(request.center());
        for (long seed : request.seeds()) {
            if (candidates.size() >= request.maxCandidates()) {
                break;
            }

            int sx = BlockPos.getX(seed);
            int sy = BlockPos.getY(seed);
            int sz = BlockPos.getZ(seed);

            for (Offset offset : offsets) {
                int y = sy + offset.y();
                if (y < request.minBuildHeight() || y >= request.maxBuildHeight()) {
                    continue;
                }

                candidates.add(BlockPos.asLong(sx + offset.x(), y, sz + offset.z()));
                if (candidates.size() >= request.maxCandidates()) {
                    break;
                }
            }
        }

        ArrayList<Long> result = new ArrayList<>(candidates);
        result.sort(Comparator.comparingLong(pos -> distanceSq(pos, request.center())));
        return result;
    }

    private static long distanceSq(long a, long b) {
        long dx = (long) BlockPos.getX(a) - BlockPos.getX(b);
        long dy = (long) BlockPos.getY(a) - BlockPos.getY(b);
        long dz = (long) BlockPos.getZ(a) - BlockPos.getZ(b);
        return dx * dx + dy * dy + dz * dz;
    }

    private static final class PlannerThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, RealisticBlockPhysicsFixer.MOD_ID + "-planner-" + THREAD_COUNTER.incrementAndGet());
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((t, e) -> RealisticBlockPhysicsFixer.LOGGER.error(
                    "[{}] Uncaught exception in planner thread {}.",
                    RealisticBlockPhysicsFixer.MOD_ID,
                    t.getName(),
                    e
            ));
            return thread;
        }
    }
}
