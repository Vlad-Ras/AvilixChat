package com.roften.multichat.compat;

import com.roften.multichat.MultiChatConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Optional integration with the LuckPerms API.
 *
 * <p>Implemented via reflection so the mod has no hard dependency on LuckPerms.
 */
public final class LuckPermsCompat {
    private LuckPermsCompat() {}

    /**
     * Returns a formatted prefix component for a player, or empty if LuckPerms is not present.
     */
    public static Component getPrefix(ServerPlayer player) {
        String prefix = getPrefixString(player.getUUID());
        if (prefix == null || prefix.isBlank()) {
            return Component.empty();
        }

        // LuckPerms prefixes often contain accidental leading/trailing spaces.
        // We normalize them here and let the chat formatter add separators.
        String s = trimAscii(prefix);
        if (s.isEmpty()) {
            return Component.empty();
        }

        String mode = String.valueOf(MultiChatConfig.LUCKPERMS_PREFIX_FORMAT.get()).trim().toUpperCase();
        return switch (mode) {
            case "MINIMESSAGE" -> MiniMessageComponentParser.parse(s);
            case "PLAIN" -> Component.literal(s);
            case "LEGACY" -> LegacyComponentParser.parse(s);
            default -> {
                // AUTO
                if (MiniMessageComponentParser.looksLikeMiniMessage(s)) {
                    yield MiniMessageComponentParser.parse(s);
                }
                yield LegacyComponentParser.parse(s);
            }
        };
    }

    private static String trimAscii(String in) {
        if (in == null) return "";
        int start = 0;
        int end = in.length();
        while (start < end) {
            char c = in.charAt(start);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') break;
            start++;
        }
        while (end > start) {
            char c = in.charAt(end - 1);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') break;
            end--;
        }
        return in.substring(start, end);
    }

    /**
     * LuckPerms permission check. Returns:
     * - Boolean.TRUE / Boolean.FALSE if LuckPerms is present
     * - null if LuckPerms is not present or could not be queried
     */
    public static Boolean hasPermission(ServerPlayer player, String node) {
        if (player == null || node == null || node.isBlank()) return null;
        try {
            Object lp = getLuckPerms();
            if (lp == null) return null;

            Object userManager = lp.getClass().getMethod("getUserManager").invoke(lp);
            Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, player.getUUID());
            if (user == null) return null;

            Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
            Object permData = cachedData.getClass().getMethod("getPermissionData").invoke(cachedData);
            Object tristate = permData.getClass().getMethod("checkPermission", String.class).invoke(permData, node);

            // Tristate has asBoolean() in LuckPerms API
            Method asBoolean = tristate.getClass().getMethod("asBoolean");
            Object res = asBoolean.invoke(tristate);
            if (res instanceof Boolean b) return b;
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * CommandSource permission helper used in Brigadier .requires(...)
     */
    public static boolean hasPermission(CommandSourceStack source, String node, int vanillaFallbackLevel) {
        if (source == null) return false;
        if (source.getEntity() instanceof ServerPlayer sp) {
            Boolean lp = hasPermission(sp, node);
            if (lp != null) return lp;
        }
        return source.hasPermission(vanillaFallbackLevel);
    }

    private static String getPrefixString(UUID uuid) {
        try {
            Object lp = getLuckPerms();
            if (lp == null) return null;

            Object userManager = lp.getClass().getMethod("getUserManager").invoke(lp);
            Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, uuid);
            if (user == null) {
                return null;
            }

            Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
            Object metaData = cachedData.getClass().getMethod("getMetaData").invoke(cachedData);
            Object prefix = metaData.getClass().getMethod("getPrefix").invoke(metaData);
            return prefix instanceof String s ? s : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object getLuckPerms() {
        try {
            Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider");
            Method get = provider.getMethod("get");
            return get.invoke(null);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
