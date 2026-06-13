package de.KrasserStecher12.realisticblockphysicsfixer.mixin;

import de.KrasserStecher12.realisticblockphysicsfixer.config.RBPFConfig;
import de.KrasserStecher12.realisticblockphysicsfixer.fixes.entityguard.FallingBlockGuardSupport;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = FallingBlockGuardSupport.RBP_ENTITY_CLASS, remap = false)
public abstract class RealisticFallingBlockEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"), remap = false, require = 0)
    private void realisticBlockPhysicsFixer$keepAliveBeforeRbpDiscard(CallbackInfo callbackInfo) {
        if (!RBPFConfig.FALLING_BLOCK_GUARD_ENABLED.get() || !RBPFConfig.FALLING_BLOCK_GUARD_MIXIN_KEEP_ALIVE_ENABLED.get()) {
            return;
        }
        FallingBlockGuardSupport.keepAliveIfNeeded((Entity) (Object) this, 0, "mixin");
    }
}
