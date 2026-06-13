package de.KrasserStecher12.realisticblockphysicsfixer.fixes.explosion;

import de.KrasserStecher12.realisticblockphysicsfixer.config.RBPFConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class RecentUpdateCache {
    private final Map<DimensionPosKey, Long> expireTicks = new HashMap<>();

    public boolean tryMark(ResourceKey<Level> dimension, long packedPos, long now) {
        int cooldown = RBPFConfig.DUPLICATE_UPDATE_COOLDOWN_TICKS.get();
        if (cooldown <= 0) {
            return true;
        }

        DimensionPosKey key = new DimensionPosKey(dimension.location().toString(), packedPos);
        Long expiresAt = expireTicks.get(key);
        if (expiresAt != null && expiresAt >= now) {
            return false;
        }

        expireTicks.put(key, now + cooldown);
        return true;
    }

    public void cleanup(long now) {
        if (expireTicks.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<DimensionPosKey, Long>> iterator = expireTicks.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() < now) {
                iterator.remove();
            }
        }
    }

    public void clear() {
        expireTicks.clear();
    }

    private record DimensionPosKey(String dimension, long packedPos) {
    }
}
