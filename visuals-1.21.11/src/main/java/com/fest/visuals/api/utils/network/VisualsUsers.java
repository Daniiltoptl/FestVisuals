package com.fest.visuals.api.utils.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class VisualsUsers {
    private static final Set<String> USERS = new HashSet<>();
    private static final String USERS_URL = "https://raw.githubusercontent.com/Daniiltoptl/FestVisuals/main/users.txt";

    public static void fetch() {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(USERS_URL);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.trim().isEmpty()) {
                            USERS.add(line.trim().toLowerCase());
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        });
    }

    public static boolean hasVisuals(String username) {
        return USERS.contains(username.toLowerCase());
    }
}
