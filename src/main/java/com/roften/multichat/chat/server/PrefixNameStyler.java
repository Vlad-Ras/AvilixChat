package com.roften.multichat.chat.server;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies the (possibly gradient) color from a LuckPerms prefix to the sender name.
 */
public final class PrefixNameStyler {
    private PrefixNameStyler() {}

    public static Component styleName(ServerPlayer player, Component prefix) {
        final String name = player.getGameProfile().getName();
        if (prefix == null || prefix.getString().isEmpty()) {
            return Component.literal(name);
        }

        List<Integer> stops = new ArrayList<>();
        collectColors(prefix, stops);

        // If no explicit colors in prefix, keep vanilla name.
        if (stops.isEmpty()) {
            return Component.literal(name);
        }

        // IMPORTANT:
        // The user requested *no gradients* on nicknames.
        // Even if LuckPerms prefixes contain multiple color stops, we apply a single solid color.
        int rgb = stops.get(0);
        return Component.literal(name).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
    }

    private static void collectColors(Component c, List<Integer> out) {
        if (c == null) return;
        var color = c.getStyle().getColor();
        if (color != null) {
            int rgb = color.getValue();
            // Keep only sequentially-unique colors to represent the gradient stops.
            if (out.isEmpty() || out.get(out.size() - 1) != rgb) {
                out.add(rgb);
            }
        }
        for (Component sib : c.getSiblings()) {
            collectColors(sib, out);
        }
    }

    // Gradient helpers removed (solid color only).
}
