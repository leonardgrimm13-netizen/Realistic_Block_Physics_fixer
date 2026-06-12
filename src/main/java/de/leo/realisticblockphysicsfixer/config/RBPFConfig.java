package de.leo.realisticblockphysicsfixer.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class RBPFConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue MOD_ENABLED;
    public static final ForgeConfigSpec.BooleanValue COMMAND_ENABLED;
    public static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING;
    public static final ForgeConfigSpec.BooleanValue RATE_LIMIT_WARNINGS;
    public static final ForgeConfigSpec.BooleanValue LOG_LOADED_MODULES;
    public static final ForgeConfigSpec.IntValue MAX_GLOBAL_WORK_PER_TICK;
    public static final ForgeConfigSpec.BooleanValue WARN_IF_REALISTIC_BLOCK_PHYSICS_MISSING;
    public static final ForgeConfigSpec.BooleanValue ASYNC_PLANNING_ENABLED;

    public static final ForgeConfigSpec.BooleanValue EXPLOSION_ENABLED;
    public static final ForgeConfigSpec.IntValue DELAY_TICKS;
    public static final ForgeConfigSpec.IntValue MERGE_WINDOW_TICKS;
    public static final ForgeConfigSpec.IntValue SMALL_SCAN_RADIUS;
    public static final ForgeConfigSpec.IntValue LARGE_SCAN_RADIUS;
    public static final ForgeConfigSpec.IntValue LARGE_EXPLOSION_AFFECTED_BLOCK_THRESHOLD;
    public static final ForgeConfigSpec.IntValue MAX_AFFECTED_POSITIONS_CAPTURED;
    public static final ForgeConfigSpec.IntValue MAX_BLOCK_CHECKS_PER_SCAN;
    public static final ForgeConfigSpec.IntValue MAX_BLOCK_UPDATES_PER_SCAN;
    public static final ForgeConfigSpec.IntValue LARGE_MAX_BLOCK_CHECKS_PER_SCAN;
    public static final ForgeConfigSpec.IntValue LARGE_MAX_BLOCK_UPDATES_PER_SCAN;
    public static final ForgeConfigSpec.IntValue MAX_QUEUED_EXPLOSION_RECORDS;
    public static final ForgeConfigSpec.IntValue MAX_ACTIVE_SCANS;
    public static final ForgeConfigSpec.IntValue MAX_SCANS_PER_TICK;
    public static final ForgeConfigSpec.IntValue MAX_SCAN_AGE_TICKS;
    public static final ForgeConfigSpec.IntValue MAX_PLANNING_JOBS;
    public static final ForgeConfigSpec.IntValue MAX_BLOCK_CHECKS_PER_TICK;
    public static final ForgeConfigSpec.IntValue MAX_BLOCK_UPDATES_PER_TICK;
    public static final ForgeConfigSpec.IntValue MAX_PENDING_UPDATES_PER_SCAN;
    public static final ForgeConfigSpec.IntValue DUPLICATE_UPDATE_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.BooleanValue LOADED_CHUNKS_ONLY;
    public static final ForgeConfigSpec.BooleanValue IGNORE_BLOCK_ENTITIES;
    public static final ForgeConfigSpec.BooleanValue SKIP_FLUID_BLOCKS;
    public static final ForgeConfigSpec.BooleanValue TREAT_REPLACEABLE_BELOW_AS_UNSUPPORTED;
    public static final ForgeConfigSpec.BooleanValue TREAT_NO_COLLISION_BELOW_AS_UNSUPPORTED;
    public static final ForgeConfigSpec.BooleanValue BLACKLIST_MODE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLOCK_BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLOCK_WHITELIST;
    public static final ForgeConfigSpec.BooleanValue SKIP_WHEN_TOO_MANY_FALLING_BLOCKS;
    public static final ForgeConfigSpec.IntValue FALLING_BLOCK_CHECK_RADIUS;
    public static final ForgeConfigSpec.IntValue FALLING_BLOCK_SOFT_LIMIT;
    public static final ForgeConfigSpec.BooleanValue SUMMARY_LOGGING;

    public static final ForgeConfigSpec.BooleanValue FALLING_BLOCK_GUARD_ENABLED;
    public static final ForgeConfigSpec.IntValue FALLING_BLOCK_GUARD_SCAN_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue FALLING_BLOCK_GUARD_MAX_ENTITIES_SCANNED_PER_LEVEL;
    public static final ForgeConfigSpec.IntValue FALLING_BLOCK_GUARD_SOFT_LIMIT_PER_LEVEL;
    public static final ForgeConfigSpec.IntValue FALLING_BLOCK_GUARD_HARD_LIMIT_PER_LEVEL;
    public static final ForgeConfigSpec.BooleanValue FALLING_BLOCK_GUARD_EMERGENCY_DISCARD_ABOVE_HARD_LIMIT;
    public static final ForgeConfigSpec.BooleanValue FALLING_BLOCK_GUARD_KEEP_ALIVE_ENABLED;
    public static final ForgeConfigSpec.IntValue FALLING_BLOCK_GUARD_KEEP_ALIVE_RESET_AT_TICKS;
    public static final ForgeConfigSpec.IntValue FALLING_BLOCK_GUARD_KEEP_ALIVE_RESET_TO_TICKS;
    public static final ForgeConfigSpec.IntValue FALLING_BLOCK_GUARD_STUCK_KEEP_ALIVE_AFTER_TICKS;
    public static final ForgeConfigSpec.IntValue FALLING_BLOCK_GUARD_TRACKING_TTL_TICKS;

    public static final ForgeConfigSpec.BooleanValue ENABLED;

    private static volatile Boolean runtimeDebugLogging;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("General server-side switches for Realistic Block Physics Fixer.").push("general");
        MOD_ENABLED = builder.comment("Master switch for all fix modules.").define("modEnabled", true);
        COMMAND_ENABLED = builder.comment("Registers /rbpf admin commands when true.").define("commandEnabled", true);
        WARN_IF_REALISTIC_BLOCK_PHYSICS_MISSING = builder.comment("Log a startup warning when no known Realistic Block Physics mod id is loaded.").define("warnIfRealisticBlockPhysicsMissing", true);
        builder.pop();

        builder.comment("Debug and logging behavior.").push("debug");
        DEBUG_LOGGING = builder.comment("Verbose debug logging. Keep false on production servers.").define("debugLogging", false);
        RATE_LIMIT_WARNINGS = builder.comment("Rate-limit repeated warnings to avoid log spam.").define("rateLimitWarnings", true);
        LOG_LOADED_MODULES = builder.comment("Log registered modules during mod construction.").define("logLoadedModules", true);
        SUMMARY_LOGGING = builder.comment("One-line summaries for completed explosion scans when debug logging is enabled.").define("summaryLogging", true);
        builder.pop();

        builder.comment("Global performance limits shared by all modules.").push("performance");
        MAX_GLOBAL_WORK_PER_TICK = builder.comment("Global abstract work budget available to all modules per server tick.").defineInRange("maxGlobalWorkPerTick", 8000, 1, 100000);
        ASYNC_PLANNING_ENABLED = builder.comment("Allow async planning that only processes immutable coordinate data and never touches Minecraft world objects.").define("asyncPlanningEnabled", true);
        builder.pop();

        builder.comment("Explosion Physics Update Fix module.").push("modules").push("explosion");
        EXPLOSION_ENABLED = builder.comment("Enable the explosion physics update fix module.").define("enabled", true);
        DELAY_TICKS = builder.comment("Ticks to wait after an explosion before scanning.").defineInRange("delayTicks", 2, 1, 40);
        MERGE_WINDOW_TICKS = builder.comment("Explosion records due in this tick window may be merged if spatially close.").defineInRange("mergeWindowTicks", 2, 0, 20);
        SMALL_SCAN_RADIUS = builder.comment("Normal scan radius around affected explosion blocks.").defineInRange("scanRadius", 5, 1, 16);
        LARGE_SCAN_RADIUS = builder.comment("Scan radius for large explosions.").defineInRange("largeScanRadius", 7, 1, 24);
        LARGE_EXPLOSION_AFFECTED_BLOCK_THRESHOLD = builder.comment("Affected block count that switches to large scan limits.").defineInRange("largeExplosionAffectedBlockThreshold", 220, 1, 100000);
        MAX_AFFECTED_POSITIONS_CAPTURED = builder.comment("Hard cap for copied affected block positions per explosion event.").defineInRange("maxAffectedPositionsCaptured", 4096, 128, 100000);
        MAX_BLOCK_CHECKS_PER_SCAN = builder.comment("Max candidate positions checked per normal explosion plan.").defineInRange("maxCheckedBlocksPerExplosion", 3000, 100, 100000);
        MAX_BLOCK_UPDATES_PER_SCAN = builder.comment("Max block/neighbor update targets per normal explosion plan.").defineInRange("maxUpdatesPerExplosion", 1500, 50, 100000);
        LARGE_MAX_BLOCK_CHECKS_PER_SCAN = builder.comment("Max candidate positions checked per large explosion plan.").defineInRange("largeMaxCheckedBlocksPerExplosion", 6000, 100, 200000);
        LARGE_MAX_BLOCK_UPDATES_PER_SCAN = builder.comment("Max block/neighbor update targets per large explosion plan.").defineInRange("largeMaxUpdatesPerExplosion", 2500, 50, 200000);
        MAX_QUEUED_EXPLOSION_RECORDS = builder.comment("Max delayed explosion records waiting for planning.").defineInRange("maxQueuedScans", 30, 1, 1000);
        MAX_ACTIVE_SCANS = builder.comment("Max active explosion scans processed over ticks.").defineInRange("maxActiveScans", 30, 1, 200);
        MAX_SCANS_PER_TICK = builder.comment("Max explosion scans visited per server tick.").defineInRange("maxScansPerTick", 4, 1, 100);
        MAX_SCAN_AGE_TICKS = builder.comment("Drop active scans older than this many ticks.").defineInRange("maxScanAgeTicks", 200, 20, 6000);
        MAX_PLANNING_JOBS = builder.comment("Max async coordinate-only planning jobs waiting/running at once.").defineInRange("maxPlanningJobs", 4, 1, 32);
        MAX_BLOCK_CHECKS_PER_TICK = builder.comment("Explosion module block-state check budget per server tick.").defineInRange("maxPositionsCheckedPerTick", 6000, 10, 50000);
        MAX_BLOCK_UPDATES_PER_TICK = builder.comment("Explosion module block/neighbor update budget per server tick.").defineInRange("maxUpdatesPerTick", 1000, 1, 10000);
        MAX_PENDING_UPDATES_PER_SCAN = builder.comment("Backpressure limit for update targets found before they can be applied.").defineInRange("maxPendingUpdatesPerScan", 512, 1, 10000);
        DUPLICATE_UPDATE_COOLDOWN_TICKS = builder.comment("Avoid updating the same block position again for this many ticks.").defineInRange("duplicateUpdateCooldownTicks", 10, 0, 200);
        LOADED_CHUNKS_ONLY = builder.comment("Never load chunks for the fixer.").define("onlyLoadedChunks", true);
        IGNORE_BLOCK_ENTITIES = builder.comment("Skip blocks with block entities such as chests, machines, or cables.").define("ignoreBlockEntities", true);
        SKIP_FLUID_BLOCKS = builder.comment("Do not directly target water/lava/fluid blocks.").define("ignoreFluids", true);
        TREAT_REPLACEABLE_BELOW_AS_UNSUPPORTED = builder.comment("Treat replaceable blocks below the target as unsupported.").define("treatReplaceableBelowAsUnsupported", true);
        TREAT_NO_COLLISION_BELOW_AS_UNSUPPORTED = builder.comment("Treat no-collision blocks below the target as unsupported.").define("treatNoCollisionBelowAsUnsupported", true);
        BLACKLIST_MODE = builder.comment("True = blacklist mode. False = only blockWhitelist can be targeted.").define("blacklistMode", true);
        BLOCK_BLACKLIST = builder.comment("Blocks that should never be targeted by the explosion fix.").defineListAllowEmpty("blockBlacklist", List.of("minecraft:bedrock", "minecraft:barrier", "minecraft:command_block", "minecraft:repeating_command_block", "minecraft:chain_command_block", "minecraft:structure_block", "minecraft:jigsaw", "minecraft:end_portal_frame", "minecraft:end_portal", "minecraft:nether_portal", "minecraft:moving_piston"), value -> value instanceof String);
        BLOCK_WHITELIST = builder.comment("Only used when blacklistMode=false.").defineListAllowEmpty("blockWhitelist", List.of(), value -> value instanceof String);
        SKIP_WHEN_TOO_MANY_FALLING_BLOCKS = builder.comment("Optional safety: skip a scan if many FallingBlock entities already exist near its center.").define("skipWhenTooManyFallingBlocks", false);
        FALLING_BLOCK_CHECK_RADIUS = builder.comment("Radius used by the optional FallingBlock soft guard.").defineInRange("fallingBlockCheckRadius", 32, 4, 128);
        FALLING_BLOCK_SOFT_LIMIT = builder.comment("Soft FallingBlock entity limit for the optional guard.").defineInRange("fallingBlockSoftLimit", 250, 1, 10000);
        builder.pop().pop();

        builder.comment("RBP Falling Block Entity Guard module.").push("modules").push("fallingBlockEntityGuard");
        FALLING_BLOCK_GUARD_ENABLED = builder.comment("Enable server-side monitoring of RBP falling block entities without linking against RBP classes.").define("enabled", true);
        FALLING_BLOCK_GUARD_SCAN_INTERVAL_TICKS = builder.comment("Ticks between entity guard scans. Higher values reduce overhead but react later.").defineInRange("scanIntervalTicks", 10, 1, 200);
        FALLING_BLOCK_GUARD_MAX_ENTITIES_SCANNED_PER_LEVEL = builder.comment("Max RBP falling block entities inspected per level per guard scan.").defineInRange("maxEntitiesScannedPerLevel", 512, 1, 20000);
        FALLING_BLOCK_GUARD_SOFT_LIMIT_PER_LEVEL = builder.comment("Warn when a level has more RBP falling blocks than this soft limit.").defineInRange("softLimitPerLevel", 350, 1, 100000);
        FALLING_BLOCK_GUARD_HARD_LIMIT_PER_LEVEL = builder.comment("Emergency hard limit. Only used when emergencyDiscardAboveHardLimit=true.").defineInRange("hardLimitPerLevel", 1200, 1, 100000);
        FALLING_BLOCK_GUARD_EMERGENCY_DISCARD_ABOVE_HARD_LIMIT = builder.comment("Last-resort crash protection: discard extra airborne RBP falling blocks above the hard limit. Keep false unless the server is otherwise crashing.").define("emergencyDiscardAboveHardLimit", false);
        FALLING_BLOCK_GUARD_KEEP_ALIVE_ENABLED = builder.comment("Reset RBP's private fallTime for safe, loaded, airborne physics blocks before the original 600 tick timeout discards them.").define("keepAliveEnabled", true);
        FALLING_BLOCK_GUARD_KEEP_ALIVE_RESET_AT_TICKS = builder.comment("When an airborne RBP falling block reaches at least this fallTime, reset it to keepAliveResetToTicks.").defineInRange("keepAliveResetAtTicks", 560, 20, 600);
        FALLING_BLOCK_GUARD_KEEP_ALIVE_RESET_TO_TICKS = builder.comment("New fallTime value after a keep-alive reset.").defineInRange("keepAliveResetToTicks", 120, 0, 559);
        FALLING_BLOCK_GUARD_STUCK_KEEP_ALIVE_AFTER_TICKS = builder.comment("Also keep alive airborne RBP falling blocks that have barely moved for this many ticks.").defineInRange("stuckKeepAliveAfterTicks", 200, 20, 6000);
        FALLING_BLOCK_GUARD_TRACKING_TTL_TICKS = builder.comment("Remove unseen entities from the guard's tracking cache after this many ticks.").defineInRange("trackingTtlTicks", 1200, 200, 20000);
        builder.pop().pop();

        SPEC = builder.build();
        ENABLED = MOD_ENABLED;
    }

    public static boolean isDebugLoggingEnabled() {
        Boolean override = runtimeDebugLogging;
        return override != null ? override : DEBUG_LOGGING.get();
    }

    public static void setRuntimeDebugLogging(Boolean enabled) {
        runtimeDebugLogging = enabled;
    }

    public static String runtimeDebugDescription() {
        Boolean override = runtimeDebugLogging;
        return override == null ? "config=" + DEBUG_LOGGING.get() : "runtime=" + override + " (config=" + DEBUG_LOGGING.get() + ")";
    }

    private RBPFConfig() {
    }
}
