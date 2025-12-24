package com.roften.multichat.chat.client;

import com.roften.multichat.MultiChatMod;
import com.roften.multichat.chat.ChatChannel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatEvent;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;

import java.util.Locale;

@EventBusSubscriber(modid = MultiChatMod.MODID, value = Dist.CLIENT)
public final class ClientChatHooks {
    private ClientChatHooks() {}

    @SubscribeEvent
    public static void onClientChatSend(ClientChatEvent event) {
        String msg = event.getMessage();
        if (msg == null || msg.isBlank()) return;

        // Aliases:
        // "!" at the beginning forces GLOBAL (strip the '!').
        if (!msg.isEmpty() && msg.charAt(0) == '!') {
            String out = msg.substring(1);
            if (out.startsWith(" ")) out = out.substring(1);
            event.setMessage(out);
            return;
        }

        // IMPORTANT:
        // Do NOT rewrite /opm on the client.
        // Clan chat is handled server-side by executing /opm when the CLAN channel is selected.
        // Players should still be able to use /opm as a real command.

        if (msg.startsWith("/")) return;

        // IMPORTANT:
        // Do NOT treat any '#' prefix as "channel selection".
        // Players can start messages with HEX colors: "#RRGGBB ...".
        // We only skip when the message already contains our explicit channel selector.
        ChatChannel.ParseResult parsed = ChatChannel.parseOutgoing(msg);

        // IMPORTANT (CLAN routing):
        // CLAN is handled by the server router (it routes to OPaC party members when OPaC is present).
        // So on the client we only prefix the outgoing message with "#c " when the CLAN tab is selected.

        boolean hasExplicitChannelPrefix = parsed.channel() != ChatChannel.GLOBAL && !parsed.message().equals(msg);
        if (hasExplicitChannelPrefix) {
            // Keep explicit selectors like "$l" / "#t" as-is (handled by the server router).
            return;
        }

        ChatChannel sendAs = ClientChatState.getSendChannel();
        if (sendAs == ChatChannel.GLOBAL) return;

        String prefix = switch (sendAs) {
            case LOCAL -> "#l ";
            case TRADE -> "#t ";
            case CLAN -> "#c ";
            case ADMIN -> "#a ";
            case GLOBAL -> "";
        };

        if (!prefix.isEmpty()) {
            event.setMessage(prefix + msg);
        }
    }

    // (command sending helper removed)

    @SubscribeEvent
    public static void onChatReceived(ClientChatReceivedEvent event) {
        // Routing/filtering is handled in ChatComponentMixin at the final addMessage(...) stage.
        // Keeping this event hook (empty) avoids version-to-version differences where some mods
        // expect the event class to be loaded.
    }
}
