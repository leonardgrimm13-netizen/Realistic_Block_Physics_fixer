package de.leo.realisticblockphysicsfixer.debug;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class DebugStats {
    private final Map<String, AtomicLong> counters = new LinkedHashMap<>();

    public void increment(String key) {
        add(key, 1L);
    }

    public void add(String key, long amount) {
        counters.computeIfAbsent(key, ignored -> new AtomicLong()).addAndGet(amount);
    }

    public Map<String, Long> snapshot() {
        LinkedHashMap<String, Long> snapshot = new LinkedHashMap<>();
        counters.forEach((key, value) -> snapshot.put(key, value.get()));
        return Map.copyOf(snapshot);
    }

    public void clear() {
        counters.clear();
    }
}
