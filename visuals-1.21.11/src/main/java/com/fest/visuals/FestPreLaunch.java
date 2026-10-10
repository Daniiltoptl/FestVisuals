package com.fest.visuals;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/**
 * Runs before Minecraft's own main method.
 *
 * <p>At start-up the game builds a crash-report preview, which asks OSHI for this process's
 * performance counters. On Windows installs whose counter registry is damaged (common on
 * non-English systems, logged as "Unable to locate English counter names in registry Perflib 009")
 * OSHI falls back to a WMI query that can block the main thread for several seconds up to minutes
 * before the window even appears. The process counters are only cosmetic in that report, so they
 * are switched off, and any other WMI query gets a time limit.
 *
 * <p>OSHI reads {@code oshi.*} system properties when its config class first loads, which happens
 * after this entrypoint; values already set (by a launcher, say) are left alone.
 */
public class FestPreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        setIfAbsent("oshi.os.windows.perfproc.disabled", "true");
        setIfAbsent("oshi.util.wmi.timeout", "2000");
    }

    private static void setIfAbsent(String key, String value) {
        if (System.getProperty(key) == null) System.setProperty(key, value);
    }
}
