package de.leo.realisticblockphysicsfixer.fixes.explosion;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;

public record ExplosionRecord(
        ResourceKey<Level> dimension,
        long center,
        List<Long> affectedPositions,
        int originalAffectedBlockCount,
        int minBuildHeight,
        int maxBuildHeight,
        long createdTick,
        long dueTick
) {
}
