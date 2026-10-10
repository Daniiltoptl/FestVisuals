package com.fest.visuals.client.features.modules.utility;

import lombok.Getter;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;

@ModuleRegister(name = "Chat Helper", desc = "Улучшения чата: стак одинаковых сообщений.", category = Category.OTHER)
public class ChatHelperModule extends Module {
    @Getter private static final ChatHelperModule instance = new ChatHelperModule();

    public final BooleanSetting stackMessages = new BooleanSetting("Стакать сообщения").value(true);

    public ChatHelperModule() {
        addSettings(stackMessages);
    }

    @Override public void onEvent() {}
}
