package de.leo.realisticblockphysicsfixer.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class RBPFConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue ASYNC_PLANNING_ENABLED;
    public static final ForgeConfigSpec.BooleanValue WARN_IF_REALISTIC_BLOCK_PHYSICS_MISSING;
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

    public static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING;
    public static final ForgeConfigSpec.BooleanValue SUMMARY_LOGGING;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Realistic Block Physics Fixer - server config").push("general");
        ENABLED = builder.comment("Master switch for the mod.").define("enabled", true);
        WARN_IF_REALISTIC_BLOCK_PHYSICS_MISSING = builder.comment("Log a startup warning when no known Realistic Block Physics mod id is loaded. This is informational only; there is no hard dependency.").define("warnIfRealisticBlockPhysicsMissing", true);
        ASYNC_PLANNING_ENABLED = builder.comment(
                "If true, candidate position planning runs on a small daemon worker. "
                        + "World access still always stays on the server thread."
        ).define("asyncPlanningEnabled", true);
        DELAY_TICKS = builder.comment("Ticks to wait after an explosion before scanning.")
                .defineInRange("delayTicks", 2, 1, 10);
        MERGE_WINDOW_TICKS = builder.comment(
                "Explosion records due in the same small window are merged per dimension to reduce duplicate work."
        ).defineInRange("mergeWindowTicks", 2, 0, 10);
        builder.pop();

        builder.push("scan_profiles");
        SMALL_SCAN_RADIUS = builder.comment("Normal scan radius around affected explosion blocks.")
                .defineInRange("smallScanRadius", 5, 2, 12);
        LARGE_SCAN_RADIUS = builder.comment("Scan radius for large explosions.")
                .defineInRange("largeScanRadius", 7, 2, 16);
        LARGE_EXPLOSION_AFFECTED_BLOCK_THRESHOLD = builder.comment(
                "If an explosion/merged burst has at least this many affected blocks, the large profile is used."
        ).defineInRange("largeExplosionAffectedBlockThreshold", 220, 1, 100000);
        MAX_AFFECTED_POSITIONS_CAPTURED = builder.comment(
                "Hard cap for copied affected block positions per explosion event. Higher values use more memory during giant explosions."
        ).defineInRange("maxAffectedPositionsCaptured", 4096, 128, 100000);
        MAX_BLOCK_CHECKS_PER_SCAN = builder.comment("Max candidate block checks for a normal merged scan.")
                .defineInRange("maxBlockChecksPerScan", 3000, 100, 100000);
        MAX_BLOCK_UPDATES_PER_SCAN = builder.comment("Max real block/neighbor update targets for a normal merged scan.")
                .defineInRange("maxBlockUpdatesPerScan", 1500, 50, 100000);
        LARGE_MAX_BLOCK_CHECKS_PER_SCAN = builder.comment("Max candidate block checks for a large merged scan.")
                .defineInRange("largeMaxBlockChecksPerScan", 6000, 100, 200000);
        LARGE_MAX_BLOCK_UPDATES_PER_SCAN = builder.comment("Max real block/neighbor update targets for a large merged scan.")
                .defineInRange("largeMaxBlockUpdatesPerScan", 2500, 50, 200000);
        builder.pop();

        builder.push("budgets");
        MAX_QUEUED_EXPLOSION_RECORDS = builder.comment("Max delayed explosion records waiting for merge/planning.")
                .defineInRange("maxQueuedScans", 30, 1, 1000);
        MAX_ACTIVE_SCANS = builder.comment("Max active scans processed over ticks. Extra completed plans are dropped to protect the server.")
                .defineInRange("maxActiveScans", 30, 1, 200);
        MAX_SCANS_PER_TICK = builder.comment("Max active scans visited per server tick.")
                .defineInRange("maxScansPerTick", 4, 1, 100);
        MAX_SCAN_AGE_TICKS = builder.comment("Drop active scans older than this many ticks to avoid stale work after large chain explosions.")
                .defineInRange("maxScanAgeTicks", 200, 20, 6000);
        MAX_PLANNING_JOBS = builder.comment("Max async planning jobs waiting/running at once.")
                .defineInRange("maxPlanningJobs", 4, 1, 32);
        MAX_BLOCK_CHECKS_PER_TICK = builder.comment("Global block-state checks budget per server tick.")
                .defineInRange("maxPositionsCheckedPerTick", 6000, 10, 50000);
        MAX_BLOCK_UPDATES_PER_TICK = builder.comment("Global block/neighbor update target budget per server tick.")
                .defineInRange("maxUpdatesPerTick", 1000, 1, 10000);
        MAX_PENDING_UPDATES_PER_SCAN = builder.comment("Backpressure limit for update targets found before they can be applied.")
                .defineInRange("maxPendingUpdatesPerScan", 512, 1, 10000);
        DUPLICATE_UPDATE_COOLDOWN_TICKS = builder.comment(
                "Avoid updating the same block position again for this many ticks during chain explosions."
        ).defineInRange("duplicateUpdateCooldownTicks", 10, 0, 200);
        builder.pop();

        builder.push("block_detection");
        LOADED_CHUNKS_ONLY = builder.comment("Never load chunks for the fixer. Strongly recommended for servers.")
                .define("onlyLoadedChunks", true);
        IGNORE_BLOCK_ENTITIES = builder.comment("Skip blocks with block entities such as chests/machines/cables.")
                .define("ignoreBlockEntities", true);
        SKIP_FLUID_BLOCKS = builder.comment("Do not directly target water/lava/fluid blocks.")
                .define("ignoreFluids", true);
        TREAT_REPLACEABLE_BELOW_AS_UNSUPPORTED = builder.comment(
                "Treat grass/plants/snow-like replaceable blocks below the target as unsupported."
        ).define("treatReplaceableBelowAsUnsupported", true);
        TREAT_NO_COLLISION_BELOW_AS_UNSUPPORTED = builder.comment(
                "Treat no-collision blocks below the target as unsupported."
        ).define("treatNoCollisionBelowAsUnsupported", true);
        BLACKLIST_MODE = builder.comment(
                "True = blacklist mode. False = only blocks in blockWhitelist can be targeted."
        ).define("blacklistMode", true);
        BLOCK_BLACKLIST = builder.comment("Blocks that should never be targeted by the fixer.")
                .defineListAllowEmpty("blockBlacklist", List.of(
                        "minecraft:bedrock",
                        "minecraft:barrier",
                        "minecraft:command_block",
                        "minecraft:repeating_command_block",
                        "minecraft:chain_command_block",
                        "minecraft:structure_block",
                        "minecraft:jigsaw",
                        "minecraft:end_portal_frame",
                        "minecraft:end_portal",
                        "minecraft:nether_portal",
                        "minecraft:moving_piston"
                ), value -> value instanceof String);
        BLOCK_WHITELIST = builder.comment("Only used when blacklistMode=false.")
                .defineListAllowEmpty("blockWhitelist", List.of(), value -> value instanceof String);
        builder.pop();

        builder.push("falling_block_guard");
        SKIP_WHEN_TOO_MANY_FALLING_BLOCKS = builder.comment(
                "Optional safety: skip a scan if many FallingBlock entities already exist near its center. Disabled by default."
        ).define("skipWhenTooManyFallingBlocks", false);
        FALLING_BLOCK_CHECK_RADIUS = builder.comment("Radius used by the optional FallingBlock soft guard.")
                .defineInRange("fallingBlockCheckRadius", 32, 4, 128);
        FALLING_BLOCK_SOFT_LIMIT = builder.comment("Soft FallingBlock entity limit for the optional guard.")
                .defineInRange("fallingBlockSoftLimit", 250, 1, 10000);
        builder.pop();

        builder.push("logging");
        DEBUG_LOGGING = builder.comment("Very verbose debug logging. Keep false on production servers.")
                .define("debugLogging", false);
        SUMMARY_LOGGING = builder.comment("Short one-line summaries for completed scans when debug logging is enabled.")
                .define("summaryLogging", true);
        builder.pop();

        SPEC = builder.build();
    }

    private RBPFConfig() {
    }
}
