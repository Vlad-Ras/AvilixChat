package com.roften.multichat.client.ui;

import com.roften.multichat.chat.ChatChannel;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Client-only: holds the set of tabs (channels) that the user selected for merged viewing
 * via SHIFT + LMB.
 */
public final class MergedTabsState {
    private static final EnumSet<ChatChannel> MERGED = EnumSet.noneOf(ChatChannel.class);

    private MergedTabsState() {}

    public static void toggle(ChatChannel ch) {
        if (ch == null) return;
        if (MERGED.contains(ch)) MERGED.remove(ch);
        else MERGED.add(ch);
    }

    public static boolean contains(ChatChannel ch) {
        return ch != null && MERGED.contains(ch);
    }

    /** Merged view is considered active only when at least 2 channels are selected. */
    public static boolean isActive() {
        return MERGED.size() >= 2;
    }

    public static Set<ChatChannel> snapshot() {
        return Collections.unmodifiableSet(EnumSet.copyOf(MERGED));
    }
}
