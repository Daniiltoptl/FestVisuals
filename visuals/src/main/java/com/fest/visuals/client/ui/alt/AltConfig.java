package com.fest.visuals.client.ui.alt;

import com.fest.visuals.api.system.backend.ClientInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class AltConfig {
    private static final File ALT_FILE = new File(System.getProperty("user.dir"), ClientInfo.NAME + "/alts.txt");
    private static final List<String> alts = new ArrayList<>();

    public static void load() {
        alts.clear();
        if (ALT_FILE.exists()) {
            try {
                alts.addAll(Files.readAllLines(ALT_FILE.toPath()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        try {
            ALT_FILE.getParentFile().mkdirs();
            Files.write(ALT_FILE.toPath(), alts);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getAlts() {
        return alts;
    }

    public static void addAlt(String name) {
        if (!alts.contains(name)) {
            alts.add(name);
            save();
        }
    }

    public static void removeAlt(String name) {
        if (alts.remove(name)) {
            save();
        }
    }
}
