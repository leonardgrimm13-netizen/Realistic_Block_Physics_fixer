package de.leo.realisticblockphysicsfixer.scan;

import net.minecraft.core.BlockPos;

import java.util.List;

public record CandidateBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, long volume) {
    public static CandidateBounds fromSeeds(List<Long> seeds, long center, int radius, int minBuildHeight, int maxBuildHeight) {
        int minX = BlockPos.getX(center);
        int minY = BlockPos.getY(center);
        int minZ = BlockPos.getZ(center);
        int maxX = minX;
        int maxY = minY;
        int maxZ = minZ;

        for (long seed : seeds) {
            int x = BlockPos.getX(seed);
            int y = BlockPos.getY(seed);
            int z = BlockPos.getZ(seed);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        minX -= radius;
        minY = Math.max(minBuildHeight, minY - radius);
        minZ -= radius;
        maxX += radius;
        maxY = Math.min(maxBuildHeight - 1, maxY + radius);
        maxZ += radius;

        long volume = ((long) maxX - minX + 1L) * ((long) maxY - minY + 1L) * ((long) maxZ - minZ + 1L);
        return new CandidateBounds(minX, minY, minZ, maxX, maxY, maxZ, Math.max(0L, volume));
    }
}
