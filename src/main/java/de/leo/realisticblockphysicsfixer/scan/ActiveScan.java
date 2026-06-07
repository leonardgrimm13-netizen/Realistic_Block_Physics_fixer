package de.leo.realisticblockphysicsfixer.scan;

import de.leo.realisticblockphysicsfixer.config.RBPFConfig;
import de.leo.realisticblockphysicsfixer.detect.FloatingBlockDetector;
import de.leo.realisticblockphysicsfixer.update.PhysicsUpdateDispatcher;
import de.leo.realisticblockphysicsfixer.update.RecentUpdateCache;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.List;

public final class ActiveScan {
    private final ScanPlan plan;
    private final List<Long> candidates;
    private final ArrayDeque<Long> pendingUpdates = new ArrayDeque<>();
    private final long activatedTick;

    private int candidateIndex;
    private int checkedCount;
    private int suspiciousCount;
    private int updatedCount;
    private int skippedUnloadedCount;
    private int skippedDuplicateCount;
    private int droppedPendingUpdateCount;

    public ActiveScan(ScanPlan plan, long activatedTick) {
        this.plan = plan;
        this.candidates = plan.candidates();
        this.activatedTick = activatedTick;
    }

    public ResourceKey<Level> dimension() {
        return plan.dimension();
    }

    public int totalCandidates() {
        return candidates.size();
    }

    public int checkedCount() {
        return checkedCount;
    }

    public int suspiciousCount() {
        return suspiciousCount;
    }

    public int updatedCount() {
        return updatedCount;
    }

    public int skippedUnloadedCount() {
        return skippedUnloadedCount;
    }

    public int skippedDuplicateCount() {
        return skippedDuplicateCount;
    }

    public int droppedPendingUpdateCount() {
        return droppedPendingUpdateCount;
    }

    public boolean shouldSkipBecauseOfFallingBlockGuard(ServerLevel level) {
        if (!RBPFConfig.SKIP_WHEN_TOO_MANY_FALLING_BLOCKS.get()) {
            return false;
        }

        BlockPos center = BlockPos.of(plan.center());
        int radius = RBPFConfig.FALLING_BLOCK_CHECK_RADIUS.get();
        AABB area = new AABB(center).inflate(radius);
        int count = level.getEntitiesOfClass(FallingBlockEntity.class, area).size();
        return count >= RBPFConfig.FALLING_BLOCK_SOFT_LIMIT.get();
    }

    public TickResult process(ServerLevel level, RecentUpdateCache recentUpdates, long now, int checkBudget, int updateBudget) {
        int checksUsed = 0;
        int updatesUsed = 0;

        while (updatesUsed < updateBudget && !pendingUpdates.isEmpty()) {
            long packed = pendingUpdates.removeFirst();
            if (tryApplyUpdate(level, recentUpdates, now, packed)) {
                updatesUsed++;
            }
        }

        while (checksUsed < checkBudget
                && updatesUsed < updateBudget
                && candidateIndex < candidates.size()
                && updatedCount < plan.maxUpdates()) {
            long packed = candidates.get(candidateIndex++);
            BlockPos pos = BlockPos.of(packed);
            checksUsed++;
            checkedCount++;

            if (RBPFConfig.LOADED_CHUNKS_ONLY.get() && !level.hasChunkAt(pos)) {
                skippedUnloadedCount++;
                continue;
            }

            if (!FloatingBlockDetector.shouldUpdate(level, pos)) {
                continue;
            }

            suspiciousCount++;
            if (tryApplyUpdate(level, recentUpdates, now, packed)) {
                updatesUsed++;
            } else if (pendingUpdates.size() < RBPFConfig.MAX_PENDING_UPDATES_PER_SCAN.get()) {
                pendingUpdates.addLast(packed);
            } else {
                droppedPendingUpdateCount++;
            }
        }

        return new TickResult(checksUsed, updatesUsed);
    }

    private boolean tryApplyUpdate(ServerLevel level, RecentUpdateCache recentUpdates, long now, long packed) {
        if (updatedCount >= plan.maxUpdates()) {
            return false;
        }

        if (!recentUpdates.tryMark(plan.dimension(), packed, now)) {
            skippedDuplicateCount++;
            return false;
        }

        BlockPos pos = BlockPos.of(packed);
        if (RBPFConfig.LOADED_CHUNKS_ONLY.get() && !level.hasChunkAt(pos)) {
            skippedUnloadedCount++;
            return false;
        }

        if (!FloatingBlockDetector.shouldUpdate(level, pos)) {
            return false;
        }

        PhysicsUpdateDispatcher.trigger(level, pos);
        updatedCount++;
        return true;
    }

    public boolean isFinished() {
        return (candidateIndex >= candidates.size() || updatedCount >= plan.maxUpdates()) && pendingUpdates.isEmpty();
    }

    public long activatedTick() {
        return activatedTick;
    }

    public record TickResult(int checksUsed, int updatesUsed) {
    }
}
