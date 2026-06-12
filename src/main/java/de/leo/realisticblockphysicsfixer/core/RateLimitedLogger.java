package de.leo.realisticblockphysicsfixer.core;

import de.leo.realisticblockphysicsfixer.config.RBPFConfig;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public final class RateLimitedLogger {
    private static final long DEFAULT_INTERVAL_MILLIS = 30_000L;

    private final Logger logger;
    private final Map<String, Long> lastWarningMillis = new HashMap<>();

    public RateLimitedLogger(Logger logger) {
        this.logger = logger;
    }

    public void warn(String key, String message, Object... args) {
        if (!RBPFConfig.RATE_LIMIT_WARNINGS.get()) {
            logger.warn(message, args);
            return;
        }

        long now = System.currentTimeMillis();
        long last = lastWarningMillis.getOrDefault(key, Long.MIN_VALUE);
        if (now - last >= DEFAULT_INTERVAL_MILLIS) {
            lastWarningMillis.put(key, now);
            logger.warn(message, args);
        }
    }

    public void clear() {
        lastWarningMillis.clear();
    }
}
