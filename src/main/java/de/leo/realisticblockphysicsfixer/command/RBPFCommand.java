package de.leo.realisticblockphysicsfixer.command;

import com.mojang.brigadier.CommandDispatcher;
import de.leo.realisticblockphysicsfixer.config.RBPFConfig;
import de.leo.realisticblockphysicsfixer.core.FixModule;
import de.leo.realisticblockphysicsfixer.core.FixModuleRegistry;
import de.leo.realisticblockphysicsfixer.debug.ModuleStats;
import de.leo.realisticblockphysicsfixer.fixes.entityguard.FallingBlockEntityGuardFixModule;
import de.leo.realisticblockphysicsfixer.fixes.explosion.ExplosionPhysicsUpdateFixModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Map;

public final class RBPFCommand {
    private static final int ADMIN_PERMISSION_LEVEL = 2;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, FixModuleRegistry registry) {
        dispatcher.register(Commands.literal("rbpf")
                .requires(source -> source.hasPermission(ADMIN_PERMISSION_LEVEL))
                .then(Commands.literal("status").executes(context -> status(context.getSource(), registry)))
                .then(Commands.literal("modules").executes(context -> modules(context.getSource(), registry)))
                .then(Commands.literal("debug")
                        .then(Commands.literal("on").executes(context -> setDebug(context.getSource(), true)))
                        .then(Commands.literal("off").executes(context -> setDebug(context.getSource(), false))))
                .then(Commands.literal("explosion")
                        .then(Commands.literal("stats").executes(context -> explosionStats(context.getSource(), registry))))
                .then(Commands.literal("fallingblocks")
                        .then(Commands.literal("stats").executes(context -> fallingBlockStats(context.getSource(), registry)))));
    }

    private static int status(CommandSourceStack source, FixModuleRegistry registry) {
        source.sendSuccess(() -> Component.literal("RBPF modEnabled=" + RBPFConfig.MOD_ENABLED.get()
                + ", debug=" + RBPFConfig.runtimeDebugDescription()
                + ", modules=" + registry.modules().size()), false);
        for (ModuleStats stats : registry.moduleStats()) {
            source.sendSuccess(() -> Component.literal("- " + stats.moduleId()
                    + " enabled=" + stats.enabled()
                    + " queued=" + stats.queuedWork()
                    + " lastTick=" + stats.lastRunTick()
                    + " runs=" + stats.totalRuns()
                    + " errors=" + stats.totalErrors()), false);
        }
        return registry.modules().size();
    }

    private static int modules(CommandSourceStack source, FixModuleRegistry registry) {
        for (FixModule module : registry.modules()) {
            source.sendSuccess(() -> Component.literal(module.id() + " - " + module.displayName() + " - " + (module.isEnabled() ? "enabled" : "disabled")), false);
        }
        return registry.modules().size();
    }

    private static int setDebug(CommandSourceStack source, boolean enabled) {
        RBPFConfig.setRuntimeDebugLogging(enabled);
        source.sendSuccess(() -> Component.literal("RBPF runtime debug logging set to " + enabled + ". This does not rewrite the config file."), true);
        return 1;
    }

    private static int explosionStats(CommandSourceStack source, FixModuleRegistry registry) {
        ModuleStats stats = registry.find(ExplosionPhysicsUpdateFixModule.MODULE_ID)
                .map(FixModule::stats)
                .orElse(ModuleStats.empty(ExplosionPhysicsUpdateFixModule.MODULE_ID, false));
        source.sendSuccess(() -> Component.literal("Explosion module: enabled=" + stats.enabled() + ", queued=" + stats.queuedWork()
                + ", lastTick=" + stats.lastRunTick() + ", runs=" + stats.totalRuns() + ", errors=" + stats.totalErrors()), false);
        for (Map.Entry<String, Long> entry : stats.debugCounters().entrySet()) {
            source.sendSuccess(() -> Component.literal("- " + entry.getKey() + "=" + entry.getValue()), false);
        }
        if (!stats.lastErrorMessage().isBlank()) {
            source.sendSuccess(() -> Component.literal("lastError=" + stats.lastErrorMessage()), false);
        }
        return stats.queuedWork();
    }

    private static int fallingBlockStats(CommandSourceStack source, FixModuleRegistry registry) {
        ModuleStats stats = registry.find(FallingBlockEntityGuardFixModule.MODULE_ID)
                .map(FixModule::stats)
                .orElse(ModuleStats.empty(FallingBlockEntityGuardFixModule.MODULE_ID, false));
        source.sendSuccess(() -> Component.literal("Falling block guard: enabled=" + stats.enabled() + ", tracked=" + stats.queuedWork()
                + ", lastTick=" + stats.lastRunTick() + ", runs=" + stats.totalRuns() + ", errors=" + stats.totalErrors()), false);
        for (Map.Entry<String, Long> entry : stats.debugCounters().entrySet()) {
            source.sendSuccess(() -> Component.literal("- " + entry.getKey() + "=" + entry.getValue()), false);
        }
        if (!stats.lastErrorMessage().isBlank()) {
            source.sendSuccess(() -> Component.literal("lastError=" + stats.lastErrorMessage()), false);
        }
        return stats.queuedWork();
    }

    private RBPFCommand() {
    }
}
