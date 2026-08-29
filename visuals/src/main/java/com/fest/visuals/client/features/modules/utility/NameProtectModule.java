package com.fest.visuals.client.features.modules.utility;

import lombok.Getter;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.ModeSetting;

/**
 * Чисто визуальная приватность (например для стримов): заменяет
 * подстроку с вашим ником в тексте, который клиент сам рисует на экране.
 * Не влияет на сетевой протокол и не даёт никакого игрового преимущества.
 */
@ModuleRegister(name = "Name Protect", desc = "Визуально изменяет ник игрока", category = Category.OTHER)
public class NameProtectModule extends Module {
    @Getter private static final NameProtectModule instance = new NameProtectModule();

    public final ModeSetting replacement = new ModeSetting("Замена").value("Player").values("Player", "Anonymous", "***");

    public NameProtectModule() {
        addSettings(replacement);
    }

    public String apply(String text) {
        if (!isEnabled() || mc.player == null || text == null || text.isEmpty()) return text;

        String realName = mc.player.getGameProfile().name();
        if (realName == null || realName.isEmpty()) return text;

        return text.replace(realName, replacement.getValue());
    }

    @Override
    public void onEvent() {
    }
}
