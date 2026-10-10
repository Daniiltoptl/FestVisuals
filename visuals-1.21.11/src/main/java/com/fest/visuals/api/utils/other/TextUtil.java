package com.fest.visuals.api.utils.other;

import lombok.experimental.UtilityClass;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.*;
import com.fest.visuals.api.system.backend.ClientInfo;
import com.fest.visuals.api.system.interfaces.QuickImports;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.color.UIColors;
import com.fest.visuals.api.utils.render.fonts.MsdfGlyph;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class TextUtil implements QuickImports {
    public java.util.List<MsdfGlyph.ColoredGlyph> parseTextToColoredGlyphs(Component text) {
        java.util.List<MsdfGlyph.ColoredGlyph> result = new ArrayList<>();
        parseTextRecursive(text, 0xFFFFFFFF, result);
        return result;
    }

    private void parseTextRecursive(Component text, int currentColor, List<MsdfGlyph.ColoredGlyph> result) {
        Style style = text.getStyle();
        int color = style.getColor() != null ? style.getColor().getValue() | 0xFF000000 : currentColor;

        ComponentContents content = text.getContents();
        String raw = "";

        if (content instanceof PlainTextContents.LiteralContents(String string)) {
            raw = string;
        } else if (content instanceof TranslatableContents translatable) {
            raw = translatable.getKey();
        } else if (content instanceof KeybindContents keybind) {
            raw = keybind.getName();
        }

        raw = ReplaceUtil.replaceSymbols(raw);

        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if ((c == 'ย' || c == 'ง') && i + 1 < raw.length()) {
                i++;
                continue;
            }
            result.add(new MsdfGlyph.ColoredGlyph(c, color));
        }

        for (Component sibling : text.getSiblings()) {
            parseTextRecursive(sibling, color, result);
        }
    }

    public void sendMessage(String message) {
        mc.gui.getChat().addMessage(Component.literal("").append(gradient(ClientInfo.NAME, true)).append(Component.literal(ChatFormatting.GRAY + " >> " + ChatFormatting.RESET + message)));
    }

    public String getDurationText(int ticks) {
        if (ticks == -1) {
            return "**:**";
        }

        int seconds = ticks / 20;
        int minutes = seconds / 60;
        int hours = minutes / 60;

        minutes %= 60;
        int remainingSeconds = seconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, remainingSeconds);
        } else if (minutes > 0) {
            return String.format("%d:%02d", minutes, remainingSeconds);
        }
        return String.format("%ds", seconds);
    }

    public MutableComponent gradient(String message, boolean bold) {
        MutableComponent text = Component.empty();
        int length = message.length();
        for (int i = 0; i < length; i++) {
            Color color = ColorUtil.gradient((float) i / (length - 1), UIColors.primary(), UIColors.secondary());
            text.append(Component.literal(String.valueOf(message.charAt(i))).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color.getRGB())).withBold(bold)));
        }
        return text;
    }
}
