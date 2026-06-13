package de.krasserstecher12.realisticblockphysicsfixer.core;

import de.krasserstecher12.realisticblockphysicsfixer.debug.ModuleStats;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.level.ExplosionEvent;

/** Server-side extension point for independent Forge fix modules. */
public interface FixModule {
    String id();

    String displayName();

    boolean isEnabled();

    void onRegister(FixContext context);

    default void onServerStarted(MinecraftServer server) {
    }

    default void onExplosionDetonate(ExplosionEvent.Detonate event) {
    }

    default void onServerTick(MinecraftServer server, BudgetManager budget) {
    }

    default void onServerStopping(MinecraftServer server) {
        shutdown();
    }

    default ModuleStats stats() {
        return ModuleStats.empty(id(), isEnabled());
    }

    default void shutdown() {
    }
}
