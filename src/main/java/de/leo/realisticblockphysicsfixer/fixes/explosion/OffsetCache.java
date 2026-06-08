package de.leo.realisticblockphysicsfixer.fixes.explosion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OffsetCache {
    private static final Map<Integer, List<Offset>> CACHE = new HashMap<>();

    public static synchronized List<Offset> getOffsets(int radius) {
        return CACHE.computeIfAbsent(radius, OffsetCache::buildOffsets);
    }

    private static List<Offset> buildOffsets(int radius) {
        ArrayList<Offset> offsets = new ArrayList<>((radius * 2 + 1) * (radius * 2 + 1) * (radius * 2 + 1));
        int radiusSq = radius * radius;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    int distanceSq = x * x + y * y + z * z;
                    if (distanceSq <= radiusSq) {
                        offsets.add(new Offset(x, y, z, distanceSq));
                    }
                }
            }
        }

        offsets.sort(Comparator.comparingInt(Offset::distanceSq));
        return List.copyOf(offsets);
    }

    private OffsetCache() {
    }
}
