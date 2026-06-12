package de.leo.realisticblockphysicsfixer.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class ChunkSafety {
    public static boolean isLoaded(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos);
    }

    private ChunkSafety() {
    }
}
