package de.krasserstecher12.realisticblockphysicsfixer.debug;

import java.util.Map;

public record ModuleStats(
        String moduleId,
        boolean enabled,
        int queuedWork,
        long lastRunTick,
        long totalRuns,
        long totalErrors,
        String lastErrorMessage,
        Map<String, Long> debugCounters
) {
    public static ModuleStats empty(String moduleId, boolean enabled) {
        return new ModuleStats(moduleId, enabled, 0, -1L, 0L, 0L, "", Map.of());
    }
}
