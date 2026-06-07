package de.leo.realisticblockphysicsfixer.scan;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;

public record ScanPlan(
        ResourceKey<Level> dimension,
        long center,
        List<Long> candidates,
        int scanRadius,
        int maxUpdates,
        boolean largeProfile
) {
}
