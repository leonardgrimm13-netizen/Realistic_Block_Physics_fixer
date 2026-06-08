package de.leo.realisticblockphysicsfixer.core;

import de.leo.realisticblockphysicsfixer.RealisticBlockPhysicsFixer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ServerTickScheduler {
    private final FixModuleRegistry registry;

    public ServerTickScheduler(FixModuleRegistry registry) {
        this.registry = registry;
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        registry.onServerStarted(event.getServer());
    }

    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Detonate event) {
        registry.onExplosionDetonate(event);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            registry.onServerTick(event.getServer());
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        if (de.leo.realisticblockphysicsfixer.config.RBPFConfig.COMMAND_ENABLED.get()) {
            de.leo.realisticblockphysicsfixer.command.RBPFCommand.register(event.getDispatcher(), registry);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        registry.onServerStopping(event.getServer());
        RealisticBlockPhysicsFixer.LOGGER.debug("[{}] Server-side scheduler stopped.", RealisticBlockPhysicsFixer.MOD_ID);
    }
}
