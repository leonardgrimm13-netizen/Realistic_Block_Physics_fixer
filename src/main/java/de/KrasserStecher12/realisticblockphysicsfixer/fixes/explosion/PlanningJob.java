package de.KrasserStecher12.realisticblockphysicsfixer.fixes.explosion;

import java.util.concurrent.CompletableFuture;

public record PlanningJob(CompletableFuture<ScanPlan> future, long createdTick) {
}
