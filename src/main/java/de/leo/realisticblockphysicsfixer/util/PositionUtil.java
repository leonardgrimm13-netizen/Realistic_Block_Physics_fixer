package de.leo.realisticblockphysicsfixer.util;

import net.minecraft.core.BlockPos;

import java.util.Collection;

public final class PositionUtil {
    public static BlockPos average(Collection<Long> packedPositions) {
        if (packedPositions.isEmpty()) {
            return BlockPos.ZERO;
        }
        long x = 0L;
        long y = 0L;
        long z = 0L;
        for (long packed : packedPositions) {
            x += BlockPos.getX(packed);
            y += BlockPos.getY(packed);
            z += BlockPos.getZ(packed);
        }
        int count = packedPositions.size();
        return new BlockPos((int) (x / count), (int) (y / count), (int) (z / count));
    }

    private PositionUtil() {
    }
}
