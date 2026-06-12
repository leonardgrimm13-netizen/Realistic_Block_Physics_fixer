package de.leo.realisticblockphysicsfixer;

import com.mojang.logging.LogUtils;
import de.leo.realisticblockphysicsfixer.config.RBPFConfig;
import de.leo.realisticblockphysicsfixer.core.FixContext;
import de.leo.realisticblockphysicsfixer.core.FixModuleRegistry;
import de.leo.realisticblockphysicsfixer.core.RateLimitedLogger;
import de.leo.realisticblockphysicsfixer.core.ServerTickScheduler;
import de.leo.realisticblockphysicsfixer.fixes.entityguard.FallingBlockEntityGuardFixModule;
import de.leo.realisticblockphysicsfixer.fixes.explosion.ExplosionPhysicsUpdateFixModule;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(RealisticBlockPhysicsFixer.MOD_ID)
public final class RealisticBlockPhysicsFixer {
    public static final String MOD_ID = "realistic_block_physics_fixer";
    public static final Logger LOGGER = LogUtils.getLogger();

    private final FixModuleRegistry moduleRegistry;

    public RealisticBlockPhysicsFixer() {
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.SERVER,
                RBPFConfig.SPEC,
                MOD_ID + "-server.toml"
        );

        RateLimitedLogger rateLimitedLogger = new RateLimitedLogger(LOGGER);
        this.moduleRegistry = new FixModuleRegistry(new FixContext(MOD_ID, LOGGER, rateLimitedLogger));
        registerModules(moduleRegistry);

        MinecraftForge.EVENT_BUS.register(new ServerTickScheduler(moduleRegistry));

        LOGGER.info("{} loaded for Forge 1.20.1 with {} fix module(s).", MOD_ID, moduleRegistry.modules().size());
        warnIfRealisticBlockPhysicsIsNotDetected();
    }

    private static void registerModules(FixModuleRegistry registry) {
        registry.register(new ExplosionPhysicsUpdateFixModule());
        registry.register(new FallingBlockEntityGuardFixModule());
    }

    private static void warnIfRealisticBlockPhysicsIsNotDetected() {
        boolean detected = ModList.get().isLoaded("rbp")
                || ModList.get().isLoaded("realisticblockphysics")
                || ModList.get().isLoaded("realistic_block_physics")
                || ModList.get().isLoaded("realisticphysics")
                || ModList.get().isLoaded("realistic_physics");

        if (RBPFConfig.WARN_IF_REALISTIC_BLOCK_PHYSICS_MISSING.get() && !detected) {
            LOGGER.warn("Realistic Block Physics was not detected by a known mod id. "
                    + "The fixer can still run, but it only nudges block updates and does not provide its own physics.");
        }
    }
}
