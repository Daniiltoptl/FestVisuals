package com.fest.visuals.api.utils.other;

import net.minecraft.network.chat.Component;
import com.fest.visuals.client.features.modules.utility.NameProtectModule;

public class ReplaceUtil {
    public static String replace(String text) {
        return text;
    }
    public static String replaceSymbols(String text) { return text; }
    public static Component replaceSymbols(Component text) { return text; }

    public static String protectedString(String text) {
        return NameProtectModule.getInstance().apply(text);
    }
}
