package de.krasserstecher12.realisticblockphysicsfixer.fixes.explosion;

import de.krasserstecher12.realisticblockphysicsfixer.config.FixModuleConfig;
import de.krasserstecher12.realisticblockphysicsfixer.config.RBPFConfig;

public final class ExplosionFixConfig implements FixModuleConfig {
    @Override
    public boolean enabled() {
        return RBPFConfig.EXPLOSION_ENABLED.get();
    }

    public int delayTicks() {
        return RBPFConfig.DELAY_TICKS.get();
    }
}
