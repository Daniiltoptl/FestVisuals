package com.fest.visuals.api.system.rpc;

import eu.donyka.discord.DiscordRPC;
import eu.donyka.discord.discord.RichPresence;
import eu.donyka.discord.discord.RichPresenceBuilder;
import com.fest.visuals.api.system.backend.ClientInfo;
import com.fest.visuals.api.system.interfaces.QuickImports;

public class DiscordRPCManager implements QuickImports {
    private static final DiscordRPCManager INSTANCE = new DiscordRPCManager();
    public static DiscordRPCManager getInstance() { return INSTANCE; }

    private DiscordRPC rpc;
    private final String APPLICATION_ID = "1178385960824045610"; // Placeholder application ID. User must change to their own Application ID named "FestVisuals 26.2"
    private boolean started;
    private final long startTimestamp;

    public DiscordRPCManager() {
        this.startTimestamp = System.currentTimeMillis() / 1000;
    }

    public void start() {
        if (started) return;
        rpc = new DiscordRPC();
        
        try {
            rpc.init(APPLICATION_ID, false);
            updatePresence();
            started = true;
            
            // Thread to periodically update presence if server IP changes
            Thread t = new Thread(() -> {
                while (started) {
                    try {
                        Thread.sleep(2000);
                        updatePresence();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }, "Discord-RPC-Update-Thread");
            t.setDaemon(true);
            t.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (!started || rpc == null) return;
        rpc.shutdown();
        started = false;
    }

    public void updatePresence() {
        if (rpc == null) return;
        
        String state = "В главном меню";
        if (mc.level != null && mc.player != null) {
            if (mc.getCurrentServer() != null) {
                state = "Играет на " + mc.getCurrentServer().ip;
            } else {
                state = "Одиночная игра";
            }
        }
        
        RichPresence presence = RichPresenceBuilder.builder()
                .details("FestVisuals 26.2")
                .state(state)
                .startTimestamp(startTimestamp)
                // We use generic keys or URLs. The user needs to set up the avatar on Discord Developer Portal as 'logo' or pass an imgur URL
                .largeImageKey("logo") 
                .largeImageText("FestVisuals 26.2")
                .button1("Сайт", "https://festvisuals.fun")
                .button2("Телеграмм", "https://t.me/example")
                .build();
                
        rpc.updatePresence(presence);
    }
}