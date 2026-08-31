package com.fest.visuals.client.features.modules.utility;

import java.util.regex.Pattern;

import lombok.Getter;

import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.StringSetting;

/**
 * Purely visual privacy for streams: rewrites text the client is about to draw.
 *
 * <p>Runs on every rendered string through {@code MixinTextVisitFactory}, so it covers chat, the
 * tab list, scoreboards and name tags at once. Nothing leaves the client altered — the network
 * traffic is untouched.
 */
@ModuleRegister(name = "Name Protect", desc = "Скрывает ник и режим сервера", category = Category.OTHER)
public class NameProtectModule extends Module {
    @Getter private static final NameProtectModule instance = new NameProtectModule();

    public final StringSetting nickname = new StringSetting("Ник")
            .value("Player").placeholder("Player").maxLength(16);
    public final BooleanSetting hideMode = new BooleanSetting("Скрывать режим").value(false);
    public final StringSetting modeReplacement = new StringSetting("Замена режима")
            .value("???").placeholder("???").maxLength(16).setVisible(hideMode::getValue);

    /**
     * Anarchy mode labels: "Анархия 128", "анархия#5", "Anarchy 512", and the short server codes
     * ("ft3", "hw2", "rw1") those servers put in scoreboards and tab headers.
     */
    private static final Pattern MODE = Pattern.compile(
            "(?iu)(анархи\\p{L}*|anarchy)\\s*[#№]?\\s*\\d+"
                    + "|(?<![\\p{L}\\d])(ft|hw|rw|фт|хв|рв)\\s*[-#]?\\s*\\d{1,3}(?![\\p{L}\\d])");

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
