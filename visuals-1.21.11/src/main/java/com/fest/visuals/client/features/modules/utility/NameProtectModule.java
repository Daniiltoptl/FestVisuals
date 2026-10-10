package com.fest.visuals.client.features.modules.utility;

import java.util.regex.Pattern;

import lombok.Getter;

import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.StringSetting;

@ModuleRegister(name = "Name Protect", desc = "Визуальное скрытие режима и ника", category = Category.OTHER)
public class NameProtectModule extends Module {
    @Getter private static final NameProtectModule instance = new NameProtectModule();

    public final StringSetting nickname = new StringSetting("Ник")
            .value("FestVisuals").placeholder("FestVisuals").maxLength(16);
    public final BooleanSetting hideMode = new BooleanSetting("Скрывать режим").value(false);
    public final StringSetting modeReplacement = new StringSetting("Замена режима")
            .value("\u0418\u043c\u044f").placeholder("\u0418\u043c\u044f").maxLength(16).setVisible(hideMode::getValue);

    private static final Pattern MODE = Pattern.compile(
            "(?iu)(\u0410\u043D\u0430\u0440\u0445\\p{L}*|anarchy|\u0433\u0440\u0438\u0444\\p{L}*|grief)(?:\\s*[#-]?\\s*\\d+)?"
                    + "|(?<![\\p{L}\\d])(ft|hw|rw|\u0444\u0442|\u0445\u0432|\u0440\u0432)(?:\\s*[-#]?\\s*\\d{1,3})?(?![\\p{L}\\d])(?![\\s_]*(?:Helper|\u0425\u0435\u043B\u043F\u0435\u0440))");

    public NameProtectModule() {
        addSettings(nickname, hideMode, modeReplacement);
    }

    public String apply(String text) {
        if (!isEnabled() || mc.player == null || text == null || text.isEmpty()) return text;

        String result = text;

        String realName = mc.player.getGameProfile().name();
        if (realName != null && !realName.isEmpty() && !nickname.isEmpty()) {
            result = result.replace(realName, nickname.getValue());
        }

        if (hideMode.getValue()) {
            result = MODE.matcher(result).replaceAll(java.util.regex.Matcher.quoteReplacement(modeReplacement.getValue()));
        }

        return result;
    }

    @Override
    public void onEvent() {
    }
}
