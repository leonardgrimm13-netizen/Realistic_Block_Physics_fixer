package de.krasserstecher12.realisticblockphysicsfixer.fixes.entityguard;

import de.krasserstecher12.realisticblockphysicsfixer.RealisticBlockPhysicsFixer;
import de.krasserstecher12.realisticblockphysicsfixer.config.RBPFConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Shared, server-safe helpers for the RBP falling block guard and its optional mixin hook. */
public final class FallingBlockGuardSupport {
    public static final String RBP_ENTITY_ID = "rbp:falling_block";
    public static final String RBP_ENTITY_CLASS = "xbigellx.rbp.internal.entity.RealisticFallingBlockEntity";

    private static final String FALL_TIME_FIELD = "fallTime";
    private static final AtomicLong MIXIN_KEEP_ALIVE_RESETS = new AtomicLong();
    private static final AtomicLong MIXIN_SKIPPED_UNSAFE_POSITION = new AtomicLong();
    private static final AtomicLong REFLECTION_FAILURES = new AtomicLong();
    private static final AtomicBoolean REFLECTION_WARNING_LOGGED = new AtomicBoolean();

    private static volatile Field fallTimeField;
    private static volatile boolean fallTimeReflectionUnavailable;

    public static KeepAliveResult keepAliveIfNeeded(Entity entity, int stillTicks, String source) {
        if (entity == null || entity.level().isClientSide() || !RBPFConfig.FALLING_BLOCK_GUARD_KEEP_ALIVE_ENABLED.get()) {
            return KeepAliveResult.NOT_NEEDED;
        }
        if (entity.onGround() || entity.isRemoved()) {
            return KeepAliveResult.NOT_NEEDED;
        }
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return KeepAliveResult.NOT_NEEDED;
        }

        BlockPos pos = entity.blockPosition();
        if (!isSafeLoadedServerPosition(serverLevel, pos)) {
            if ("mixin".equals(source)) {
                MIXIN_SKIPPED_UNSAFE_POSITION.incrementAndGet();
            }
            return KeepAliveResult.UNSAFE_POSITION;
        }

        OptionalInt fallTime = getFallTime(entity);
        if (fallTime.isEmpty()) {
            return KeepAliveResult.REFLECTION_UNAVAILABLE;
        }

        int resetAt = RBPFConfig.FALLING_BLOCK_GUARD_KEEP_ALIVE_RESET_AT_TICKS.get();
        int currentFallTime = fallTime.getAsInt();
        if (currentFallTime < resetAt && stillTicks < RBPFConfig.FALLING_BLOCK_GUARD_STUCK_KEEP_ALIVE_AFTER_TICKS.get()) {
            return KeepAliveResult.NOT_NEEDED;
        }

        int resetTo = Math.min(RBPFConfig.FALLING_BLOCK_GUARD_KEEP_ALIVE_RESET_TO_TICKS.get(), Math.max(0, resetAt - 1));
        if (!setFallTime(entity, resetTo)) {
            return KeepAliveResult.REFLECTION_UNAVAILABLE;
        }

        if ("mixin".equals(source)) {
            MIXIN_KEEP_ALIVE_RESETS.incrementAndGet();
        }
        if (RBPFConfig.isDebugLoggingEnabled()) {
            RealisticBlockPhysicsFixer.LOGGER.debug(
                    "[{}] {} reset RBP falling block fallTime at {} in {} ({} -> {}).",
                    RealisticBlockPhysicsFixer.MOD_ID,
                    source,
                    pos,
                    serverLevel.dimension().location(),
                    currentFallTime,
                    resetTo
            );
        }
        return KeepAliveResult.RESET;
    }

    public static long mixinKeepAliveResets() {
        return MIXIN_KEEP_ALIVE_RESETS.get();
    }

    public static long mixinSkippedUnsafePosition() {
        return MIXIN_SKIPPED_UNSAFE_POSITION.get();
    }

    public static long reflectionFailures() {
        return REFLECTION_FAILURES.get();
    }

    public static boolean isReflectionAvailable() {
        return fallTimeField != null && !fallTimeReflectionUnavailable;
    }

    public static boolean isReflectionUnavailable() {
        return fallTimeReflectionUnavailable;
    }

    public static String reflectionStatus() {
        if (isReflectionAvailable()) {
            return "available";
        }
        if (fallTimeReflectionUnavailable) {
            return "unavailable";
        }
        return "unknown_until_rbp_entity_seen";
    }

    private static boolean isSafeLoadedServerPosition(ServerLevel level, BlockPos pos) {
        return pos.getY() >= level.getMinBuildHeight()
                && pos.getY() < level.getMaxBuildHeight()
                && level.hasChunkAt(pos);
    }

    private static OptionalInt getFallTime(Entity entity) {
        Field field = resolveFallTimeField(entity);
        if (field == null) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(field.getInt(entity));
        } catch (IllegalAccessException | IllegalArgumentException exception) {
            markReflectionUnavailable(exception);
            return OptionalInt.empty();
        }
    }

    private static boolean setFallTime(Entity entity, int value) {
        Field field = resolveFallTimeField(entity);
        if (field == null) {
            return false;
        }
        try {
            field.setInt(entity, value);
            return true;
        } catch (IllegalAccessException | IllegalArgumentException exception) {
            markReflectionUnavailable(exception);
            return false;
        }
    }

    private static Field resolveFallTimeField(Entity entity) {
        Field cached = fallTimeField;
        if (cached != null) {
            return cached;
        }
        if (fallTimeReflectionUnavailable || entity == null) {
            return null;
        }

        Class<?> current = entity.getClass();
        while (current != null && current != Entity.class) {
            try {
                Field field = current.getDeclaredField(FALL_TIME_FIELD);
                field.setAccessible(true);
                fallTimeField = field;
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (SecurityException exception) {
                markReflectionUnavailable(exception);
                return null;
            }
        }

        markReflectionUnavailable(null);
        return null;
    }

    private static void markReflectionUnavailable(Throwable throwable) {
        fallTimeReflectionUnavailable = true;
        REFLECTION_FAILURES.incrementAndGet();
        if (REFLECTION_WARNING_LOGGED.compareAndSet(false, true)) {
            if (throwable == null) {
                RealisticBlockPhysicsFixer.LOGGER.warn(
                        "[{}] Could not find RBP falling block field '{}'; lifetime keep-alive is disabled for this RBP version. "
                                + "Action: verify the installed Realistic Block Physics version, then run /rbpf fallingblocks health and either update RBPF/RBP or disable fallingBlockEntityGuard.keepAliveEnabled if intentional.",
                        RealisticBlockPhysicsFixer.MOD_ID,
                        FALL_TIME_FIELD
                );
            } else {
                RealisticBlockPhysicsFixer.LOGGER.warn(
                        "[{}] Could not access RBP falling block field '{}'; lifetime keep-alive is disabled for this RBP version: {}. "
                                + "Action: verify the installed Realistic Block Physics version, then run /rbpf fallingblocks health and either update RBPF/RBP or disable fallingBlockEntityGuard.keepAliveEnabled if intentional.",
                        RealisticBlockPhysicsFixer.MOD_ID,
                        FALL_TIME_FIELD,
                        throwable.toString()
                );
            }
        }
    }

    private FallingBlockGuardSupport() {
    }

    public enum KeepAliveResult {
        RESET,
        NOT_NEEDED,
        UNSAFE_POSITION,
        REFLECTION_UNAVAILABLE
    }
}
