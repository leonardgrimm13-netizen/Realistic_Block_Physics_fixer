package de.leo.realisticblockphysicsfixer.fixes.entityguard;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

public final class RBPFMixinPlugin implements IMixinConfigPlugin {
    private static final String RBP_ENTITY_CLASS = "xbigellx.rbp.internal.entity.RealisticFallingBlockEntity";
    private static final List<String> KNOWN_RBP_MOD_IDS = List.of(
            "rbp",
            "realisticblockphysics",
            "realistic_block_physics",
            "realisticphysics",
            "realistic_physics"
    );

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!RBP_ENTITY_CLASS.equals(targetClassName)) {
            return false;
        }

        if (isKnownRbpModPresentDuringLoading()) {
            return true;
        }

        // The mixin plugin can run before Forge's normal ModList is fully constructed.
        // Fall back to a no-initialize class presence check so renamed/forked RBP jars can still work,
        // while missing or incompatible target classes simply skip the optional mixin.
        return isClassPresent(targetClassName);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean isKnownRbpModPresentDuringLoading() {
        try {
            Class<?> loadingModListClass = Class.forName("net.minecraftforge.fml.loading.LoadingModList", false, RBPFMixinPlugin.class.getClassLoader());
            Method getMethod = loadingModListClass.getMethod("get");
            Object loadingModList = getMethod.invoke(null);
            if (loadingModList == null) {
                return false;
            }
            Method getModFileByIdMethod = loadingModListClass.getMethod("getModFileById", String.class);
            for (String modId : KNOWN_RBP_MOD_IDS) {
                if (getModFileByIdMethod.invoke(loadingModList, modId) != null) {
                    return true;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // Forge loading internals are not guaranteed at mixin-plugin time; class presence fallback below is safer.
        }
        return false;
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, RBPFMixinPlugin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }
}
