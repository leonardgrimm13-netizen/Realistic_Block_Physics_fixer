package de.leo.realisticblockphysicsfixer;

import com.mojang.logging.LogUtils;
import de.leo.realisticblockphysicsfixer.config.RBPFConfig;
import de.leo.realisticblockphysicsfixer.event.ForgeEventHandler;
import de.leo.realisticblockphysicsfixer.runtime.ExplosionFixCoordinator;
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

    private final ExplosionFixCoordinator coordinator;

    public RealisticBlockPhysicsFixer() {
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.SERVER,
                RBPFConfig.SPEC,
                MOD_ID + "-server.toml"
        );

        this.coordinator = new ExplosionFixCoordinator();
        MinecraftForge.EVENT_BUS.register(new ForgeEventHandler(this.coordinator));

        LOGGER.info("{} loaded for Forge 1.20.1.", MOD_ID);
        warnIfRealisticBlockPhysicsIsNotDetected();
    }

    private static void warnIfRealisticBlockPhysicsIsNotDetected() {
        // The exact mod id can differ between releases/forks, so this is only a helpful warning.
        boolean detected = ModList.get().isLoaded("realisticblockphysics")
                || ModList.get().isLoaded("realistic_block_physics")
                || ModList.get().isLoaded("realisticphysics")
                || ModList.get().isLoaded("realistic_physics");

        if (!detected) {
            LOGGER.warn("Realistic Block Physics was not detected by a known mod id. "
                    + "The fixer can still run, but it only nudges block updates and does not provide its own physics.");
        }
    }
}
