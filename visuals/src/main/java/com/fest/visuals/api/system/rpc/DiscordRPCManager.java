package com.fest.visuals.api.system.rpc;

import eu.donyka.discord.DiscordRPC;
import eu.donyka.discord.discord.RichPresence;
import eu.donyka.discord.discord.RichPresenceBuilder;
import com.fest.visuals.api.system.interfaces.QuickImports;

/**
 * Discord Rich Presence for FestVisuals.
 *
 * <p>The bold activity title ("Играет в …") is the name of the Discord application behind
 * {@link #APPLICATION_ID}, set in the Discord Developer Portal — no code can change it. The
 * picture is the art asset uploaded there under the key {@code logo} (repository file
 * {@code assets/discord/logo.png}).
 *
 * <p>Discord rate-limits presence updates, so the presence is only resent when what it shows
 * actually changes.
 */
public class DiscordRPCManager implements QuickImports {
    private static final DiscordRPCManager INSTANCE = new DiscordRPCManager();
    public static DiscordRPCManager getInstance() { return INSTANCE; }

    /** Discord application whose name is shown as the activity title: rename it to "FestVisuals 26.2". */
    private static final String APPLICATION_ID = "1378057680316268685";
    private static final String TITLE = "FestVisuals 26.2";
    private static final String TELEGRAM = "https://t.me/festvisuals";
    private static final String SITE = "https://festvisuals.pro";

    private final long startTimestamp = System.currentTimeMillis() / 1000;
    private DiscordRPC rpc;
    private volatile boolean started;
    private String lastState;

    public void start() {
        if (started) return;
        try {
            rpc = new DiscordRPC();
            rpc.init(APPLICATION_ID, false);
            started = true;
            updatePresence();

            Thread thread = new Thread(() -> {
                while (started) {
                    try {
                        Thread.sleep(5000);
                        updatePresence();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    } catch (RuntimeException ignored) {
                        // Discord not running or IPC hiccup; the next round tries again.
                    }
                }
            }, "FestVisuals-Discord-RPC");
            thread.setDaemon(true);
            thread.start();
        } catch (Exception e) {
            started = false;
        }
    }

    public void stop() {
        started = false;
        if (rpc != null) rpc.shutdown();
    }

    private String currentState() {
        if (mc.level == null || mc.player == null) return "В главном меню";
        if (mc.getCurrentServer() != null) return "Играет на " + mc.getCurrentServer().ip;
        return "Одиночная игра";
    }

    public void updatePresence() {
        if (rpc == null || !started) return;

        String state = currentState();
        if (state.equals(lastState)) return;
        lastState = state;

        RichPresence presence = RichPresenceBuilder.builder()
                .details(TITLE)
                .state(state)
                .startTimestamp(startTimestamp)
                .largeImageKey("logo")
                .largeImageText(TITLE)
                .button1("Telegram", TELEGRAM)
                .button2("Сайт", SITE)
                .build();
        rpc.updatePresence(presence);
    }
}
