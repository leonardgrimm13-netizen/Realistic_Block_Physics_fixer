package de.leo.realisticblockphysicsfixer.event;

import de.leo.realisticblockphysicsfixer.RealisticBlockPhysicsFixer;
import de.leo.realisticblockphysicsfixer.runtime.ExplosionFixCoordinator;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ForgeEventHandler {
    private final ExplosionFixCoordinator coordinator;

    public ForgeEventHandler(ExplosionFixCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        try {
            coordinator.resetForServerStart();
        } catch (RuntimeException exception) {
            RealisticBlockPhysicsFixer.LOGGER.error("[{}] Failed to reset runtime state on server start.", RealisticBlockPhysicsFixer.MOD_ID, exception);
        }
    }

    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Detonate event) {
        try {
            coordinator.onExplosion(event);
        } catch (RuntimeException exception) {
            RealisticBlockPhysicsFixer.LOGGER.error("[{}] Failed to queue explosion scan; skipping this explosion.", RealisticBlockPhysicsFixer.MOD_ID, exception);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        try {
            coordinator.onServerTick();
        } catch (RuntimeException exception) {
            RealisticBlockPhysicsFixer.LOGGER.error("[{}] Failed during tick processing; pending scans remain queued where possible.", RealisticBlockPhysicsFixer.MOD_ID, exception);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        try {
            coordinator.shutdownForServerStop();
        } catch (RuntimeException exception) {
            RealisticBlockPhysicsFixer.LOGGER.error("[{}] Failed to shut down runtime state cleanly.", RealisticBlockPhysicsFixer.MOD_ID, exception);
        }
    }
}
