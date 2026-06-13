package de.KrasserStecher12.realisticblockphysicsfixer.fixes.explosion;

import de.KrasserStecher12.realisticblockphysicsfixer.config.RBPFConfig;
import de.KrasserStecher12.realisticblockphysicsfixer.util.BlockIdUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public final class FloatingBlockDetector {
    public static boolean shouldUpdate(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (state.isAir()) {
            return false;
        }

        if (RBPFConfig.SKIP_FLUID_BLOCKS.get() && !state.getFluidState().isEmpty()) {
            return false;
        }

        if (state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }

        if (RBPFConfig.IGNORE_BLOCK_ENTITIES.get() && level.getBlockEntity(pos) != null) {
            return false;
        }

        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (blockId == null || !BlockIdUtil.isAllowed(blockId)) {
            return false;
        }

        return isPotentiallyUnsupported(level, pos);
    }

    private static boolean isPotentiallyUnsupported(ServerLevel level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        if (RBPFConfig.LOADED_CHUNKS_ONLY.get() && !level.hasChunkAt(belowPos)) {
            return false;
        }

        BlockState below = level.getBlockState(belowPos);

        if (below.isAir()) {
            return true;
        }

        if (RBPFConfig.TREAT_REPLACEABLE_BELOW_AS_UNSUPPORTED.get() && below.canBeReplaced()) {
            return true;
        }

        return RBPFConfig.TREAT_NO_COLLISION_BELOW_AS_UNSUPPORTED.get()
                && below.getCollisionShape(level, belowPos).isEmpty();
    }

    private FloatingBlockDetector() {
    }
}
