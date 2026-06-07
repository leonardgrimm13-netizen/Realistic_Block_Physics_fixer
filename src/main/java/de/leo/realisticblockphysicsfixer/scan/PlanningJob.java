package de.leo.realisticblockphysicsfixer.scan;

import java.util.concurrent.CompletableFuture;

public record PlanningJob(CompletableFuture<ScanPlan> future, long createdTick) {
}
