package de.KrasserStecher12.realisticblockphysicsfixer.fixes.explosion;

import de.KrasserStecher12.realisticblockphysicsfixer.config.RBPFConfig;
import de.KrasserStecher12.realisticblockphysicsfixer.core.BudgetManager;
import de.KrasserStecher12.realisticblockphysicsfixer.core.FixContext;
import de.KrasserStecher12.realisticblockphysicsfixer.core.FixModule;
import de.KrasserStecher12.realisticblockphysicsfixer.debug.ModuleStats;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.level.ExplosionEvent;

public final class ExplosionPhysicsUpdateFixModule implements FixModule {
    public static final String MODULE_ID = "explosion_physics_update_fix";

    private final ExplosionFixCoordinator coordinator = new ExplosionFixCoordinator();

    @Override
    public String id() {
        return MODULE_ID;
    }

    @Override
    public String displayName() {
        return "Explosion Physics Update Fix";
    }

    @Override
    public boolean isEnabled() {
        return RBPFConfig.MOD_ENABLED.get() && RBPFConfig.EXPLOSION_ENABLED.get();
    }

    @Override
    public void onRegister(FixContext context) {
        if (RBPFConfig.LOG_LOADED_MODULES.get()) {
            context.logger().info("[{}] Registered module '{}' ({})", context.modId(), id(), displayName());
        }
    }

    @Override
    public void onServerStarted(MinecraftServer server) {
        coordinator.resetForServerStart();
    }

    @Override
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        coordinator.onExplosion(event);
    }

    @Override
    public void onServerTick(MinecraftServer server, BudgetManager budget) {
        if (budget.tryConsume(1)) {
            coordinator.onServerTick(server);
        } else {
            coordinator.incrementDroppedDueToBudget();
        }
    }

    @Override
    public void onServerStopping(MinecraftServer server) {
        coordinator.shutdownForServerStop();
    }

    @Override
    public void shutdown() {
        coordinator.shutdownForServerStop();
    }

    @Override
    public ModuleStats stats() {
        return coordinator.stats(id(), isEnabled());
    }

    public ExplosionFixCoordinator coordinator() {
        return coordinator;
    }
}
