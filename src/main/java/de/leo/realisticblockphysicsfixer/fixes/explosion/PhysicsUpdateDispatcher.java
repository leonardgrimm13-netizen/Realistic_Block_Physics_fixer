package de.leo.realisticblockphysicsfixer.fixes.explosion;

import de.leo.realisticblockphysicsfixer.config.RBPFConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class PhysicsUpdateDispatcher {
    private static final int SEND_BLOCK_UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS;

    public static void trigger(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return;
        }

        notifyTarget(level, pos, state);
        notifyNeighbors(level, pos, state);
    }

    private static void notifyTarget(ServerLevel level, BlockPos pos, BlockState state) {
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);

        // Make the target behave as if the support below changed.
        level.neighborChanged(pos, belowState.getBlock(), belowPos);

        // Vanilla neighbor update + client sync. This does not change the block state by itself.
        level.updateNeighborsAt(pos, state.getBlock());
        level.sendBlockUpdated(pos, state, state, SEND_BLOCK_UPDATE_FLAGS);
    }

    private static void notifyNeighbors(ServerLevel level, BlockPos pos, BlockState state) {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);

            if (RBPFConfig.LOADED_CHUNKS_ONLY.get() && !level.hasChunkAt(neighborPos)) {
                continue;
            }

            BlockState neighborState = level.getBlockState(neighborPos);
            if (neighborState.isAir()) {
                continue;
            }

            level.neighborChanged(neighborPos, state.getBlock(), pos);
            level.updateNeighborsAt(neighborPos, neighborState.getBlock());
            level.sendBlockUpdated(neighborPos, neighborState, neighborState, SEND_BLOCK_UPDATE_FLAGS);
        }
    }

    private PhysicsUpdateDispatcher() {
    }
}
