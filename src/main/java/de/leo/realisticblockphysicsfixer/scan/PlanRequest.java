package de.leo.realisticblockphysicsfixer.scan;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;

public record PlanRequest(
        ResourceKey<Level> dimension,
        long center,
        List<Long> seeds,
        int scanRadius,
        int maxCandidates,
        int maxUpdates,
        int minBuildHeight,
        int maxBuildHeight,
        int maxAsyncPlanningVolume,
        boolean largeProfile
) {
}
