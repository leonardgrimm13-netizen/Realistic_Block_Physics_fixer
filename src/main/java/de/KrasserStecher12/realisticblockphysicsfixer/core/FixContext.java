package de.KrasserStecher12.realisticblockphysicsfixer.core;

import org.slf4j.Logger;

public record FixContext(String modId, Logger logger, RateLimitedLogger rateLimitedLogger) {
}
