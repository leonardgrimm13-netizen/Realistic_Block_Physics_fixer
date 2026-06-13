package de.krasserstecher12.realisticblockphysicsfixer.util;

import de.krasserstecher12.realisticblockphysicsfixer.config.RBPFConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class BlockIdUtil {
    private static volatile boolean whitelistMode;
    private static volatile Set<String> blacklist = Set.of();
    private static volatile Set<String> whitelist = Set.of();

    public static void refreshFromConfig() {
        whitelistMode = !RBPFConfig.BLACKLIST_MODE.get();
        blacklist = normalize(RBPFConfig.BLOCK_BLACKLIST.get());
        whitelist = normalize(RBPFConfig.BLOCK_WHITELIST.get());
    }

    public static boolean isAllowed(ResourceLocation blockId) {
        String id = normalize(blockId.toString());
        if (whitelistMode) {
            return whitelist.contains(id);
        }
        return !blacklist.contains(id);
    }

    private static Set<String> normalize(Iterable<? extends String> ids) {
        HashSet<String> normalized = new HashSet<>();
        for (String id : ids) {
            if (id == null || id.isBlank()) {
                continue;
            }
            normalized.add(normalize(id));
        }
        return Set.copyOf(normalized);
    }

    private static String normalize(String id) {
        return id.trim().toLowerCase(Locale.ROOT);
    }

    private BlockIdUtil() {
    }
}
