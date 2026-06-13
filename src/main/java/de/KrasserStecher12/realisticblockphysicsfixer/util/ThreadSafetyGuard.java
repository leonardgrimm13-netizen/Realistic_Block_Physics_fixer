package de.KrasserStecher12.realisticblockphysicsfixer.util;

import net.minecraft.server.MinecraftServer;

public final class ThreadSafetyGuard {
    public static boolean isServerThread(MinecraftServer server) {
        return server != null && server.isSameThread();
    }

    public static void requireServerThread(MinecraftServer server, String operation) {
        if (!isServerThread(server)) {
            throw new IllegalStateException(operation + " must run on the Minecraft server thread");
        }
    }

    private ThreadSafetyGuard() {
    }
}
