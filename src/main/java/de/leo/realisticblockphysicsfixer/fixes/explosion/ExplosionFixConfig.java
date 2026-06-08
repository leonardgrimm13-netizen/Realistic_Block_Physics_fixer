package de.leo.realisticblockphysicsfixer.fixes.explosion;

import de.leo.realisticblockphysicsfixer.config.FixModuleConfig;
import de.leo.realisticblockphysicsfixer.config.RBPFConfig;

public final class ExplosionFixConfig implements FixModuleConfig {
    @Override
    public boolean enabled() {
        return RBPFConfig.EXPLOSION_ENABLED.get();
    }

    public int delayTicks() {
        return RBPFConfig.DELAY_TICKS.get();
    }
}
