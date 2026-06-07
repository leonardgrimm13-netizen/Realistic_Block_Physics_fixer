package de.leo.realisticblockphysicsfixer.event;

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
        coordinator.resetForServerStart();
    }

    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Detonate event) {
        coordinator.onExplosion(event);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            coordinator.onServerTick();
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        coordinator.shutdownForServerStop();
    }
}
