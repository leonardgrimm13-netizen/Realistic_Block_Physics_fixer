package de.krasserstecher12.realisticblockphysicsfixer.core;

import de.krasserstecher12.realisticblockphysicsfixer.RealisticBlockPhysicsFixer;
import de.krasserstecher12.realisticblockphysicsfixer.config.RBPFConfig;
import de.krasserstecher12.realisticblockphysicsfixer.debug.ModuleStats;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.level.ExplosionEvent;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class FixModuleRegistry {
    private final Map<String, FixModule> modules = new LinkedHashMap<>();
    private final FixContext context;

    public FixModuleRegistry(FixContext context) {
        this.context = context;
    }

    public void register(FixModule module) {
        FixModule existing = modules.putIfAbsent(module.id(), module);
        if (existing != null) {
            throw new IllegalArgumentException("Duplicate fix module id: " + module.id());
        }
        module.onRegister(context);
    }

    public Optional<FixModule> find(String id) {
        return Optional.ofNullable(modules.get(id));
    }

    public Collection<FixModule> modules() {
        return List.copyOf(modules.values());
    }

    public List<ModuleStats> moduleStats() {
        return modules.values().stream().map(FixModule::stats).toList();
    }

    public void onServerStarted(MinecraftServer server) {
        forEachSafely("server start", module -> module.onServerStarted(server));
    }

    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!RBPFConfig.MOD_ENABLED.get()) {
            return;
        }
        forEachSafely("explosion detonate", module -> {
            if (module.isEnabled()) {
                module.onExplosionDetonate(event);
            }
        });
    }

    public void onServerTick(MinecraftServer server) {
        if (!RBPFConfig.MOD_ENABLED.get()) {
            return;
        }
        BudgetManager budget = new BudgetManager(RBPFConfig.MAX_GLOBAL_WORK_PER_TICK.get());
        forEachSafely("server tick", module -> {
            if (module.isEnabled() && budget.remainingGlobalWork() > 0) {
                module.onServerTick(server, budget);
            }
        });
    }

    public void onServerStopping(MinecraftServer server) {
        forEachSafely("server stop", module -> module.onServerStopping(server));
        context.rateLimitedLogger().clear();
    }

    private void forEachSafely(String action, ModuleConsumer consumer) {
        for (FixModule module : modules.values()) {
            try {
                consumer.accept(module);
            } catch (Throwable throwable) {
                RealisticBlockPhysicsFixer.LOGGER.error(
                        "[{}] Module '{}' failed during {}; continuing with other modules.",
                        RealisticBlockPhysicsFixer.MOD_ID,
                        module.id(),
                        action,
                        throwable
                );
            }
        }
    }

    @FunctionalInterface
    private interface ModuleConsumer {
        void accept(FixModule module);
    }
}
