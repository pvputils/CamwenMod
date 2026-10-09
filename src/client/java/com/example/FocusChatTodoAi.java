package com.example;

import com.example.Configs.Config;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.regex.Pattern;

/** Captures server text briefly after a user-submitted FOCUS query. */
public final class FocusChatTodoAi {
    private static final long RESPONSE_NANOS = 5_000_000_000L;
    private static final Pattern WORD = Pattern.compile("[A-Za-z0-9_]+");
    private static Object pendingConnection;
    private static long deadline;

    private FocusChatTodoAi() {}

    public static void initialize() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) receive(message.getString());
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> pendingConnection = null);
    }

    public static void submit(String input) {
        Minecraft client = Minecraft.getInstance();
        var connection = client.getConnection();
        String message = input.strip();
        if (connection == null || client.player == null || message.isEmpty() || message.equals("/")) return;
        if (message.length() > 256 || message.indexOf('\n') >= 0 || message.indexOf('\r') >= 0) {
            client.player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "FOCUS message must be one line of at most 256 characters."));
            return;
        }
        UntitledClient.config.focusChatMessage = message;
        UntitledClient.config.saveConfig();
        pendingConnection = connection;
        deadline = System.nanoTime() + RESPONSE_NANOS;
        if (message.startsWith("/")) connection.sendCommand(message.substring(1));
        else connection.sendChat(message);
    }

    private static void receive(String message) {
        var connection = Minecraft.getInstance().getConnection();
        if (pendingConnection == null || connection != pendingConnection) return;
        if (System.nanoTime() - deadline >= 0) {
            pendingConnection = null;
            return;
        }
        Map<String, UUID> online = new HashMap<>();
        for (var player : connection.getOnlinePlayers()) {
            online.put(player.getProfile().name().toLowerCase(Locale.ROOT), player.getProfile().id());
        }
        boolean changed = false;
        for (UUID uuid : matchingPlayers(message, online)) {
            if (UntitledClient.config.nameplateUuids.put(uuid, Config.NameplateTeam.FOCUS) != Config.NameplateTeam.FOCUS) {
                changed = true;
            }
        }
        if (changed) UntitledClient.config.saveConfig();
    }

    public static Set<UUID> matchingPlayers(String message, Map<String, UUID> online) {
        Set<UUID> matches = new HashSet<>();
        var words = WORD.matcher(message);
        while (words.find()) {
            String word = words.group();
            if (word.length() > 16) continue;
            UUID uuid = online.get(word.toLowerCase(Locale.ROOT));
            if (uuid != null) matches.add(uuid);
        }
        return matches;
    }
}
