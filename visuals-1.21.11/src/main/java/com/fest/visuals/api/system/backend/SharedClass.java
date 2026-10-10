package com.fest.visuals.api.system.backend;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import com.fest.visuals.api.system.interfaces.QuickImports;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@UtilityClass
public class SharedClass implements QuickImports {
    public LocalPlayer player() {
        return Minecraft.getInstance().player;
    }

    public ClientLevel world() {
        return Minecraft.getInstance().level;
    }

    public boolean inPvp() {
        if (mc == null || mc.gui == null) return false;

        BossHealthOverlay bossOverlayGui = mc.gui.getBossOverlay();
        Map<UUID, LerpingBossEvent> bossBars = bossOverlayGui.events;

        for (LerpingBossEvent bossInfo : bossBars.values()) {
            String nameStrLower = bossInfo.getName().getString().toLowerCase(Locale.ROOT);
            if (nameStrLower.contains("pvp") || nameStrLower.contains("пвп")) {
                return true;
            }
        }
        return false;
    }

    public boolean openFolder(String path) {
        String os = getOSName().toLowerCase();
        try {
            if (os.contains("win")) {
                Runtime.getRuntime().exec("explorer \"" + path + "\"");
                return true;
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", path});
                return true;
            } else if (os.contains("lin")) {
                String[] fileManagers = {
                        "thunar",   // XFCE
                        "dolphin",  // KDE
                        "nautilus", // GNOME
                        "nemo",     // Cinnamon
                        "pcmanfm",  // LXDE
                        "caja",     // MATE
                        "konqueror" // KDE alternative
                };

                for (String manager : fileManagers) {
                    try {
                        Process p = Runtime.getRuntime().exec(new String[]{manager, path});
                        if (p.isAlive()) return true;
                    } catch (IOException ignored) {}
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getOSName() {
        if (System.getProperty("java.vendor").toLowerCase().contains("android") || System.getProperty("java.vm.vendor").toLowerCase().contains("android")) {
            return "Android";
        }

        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) return "Windows";
        if (osName.contains("mac")) return "MacOS";
        if (osName.contains("lin")) return "Linux";
        if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            return "Linux/Unix";
        }
        return "Unknown";
    }
}
