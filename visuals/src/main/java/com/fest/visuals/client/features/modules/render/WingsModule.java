package com.fest.visuals.client.features.modules.render;

import lombok.Getter;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.SliderSetting;


@ModuleRegister(name = "Wings", desc = "\u0410\u043D\u0438\u043C\u0438\u0440\u043E\u0432\u0430\u043D\u043D\u044B\u0435 \u043D\u0435\u043E\u043D\u043E\u0432\u044B\u0435 \u043A\u0440\u044B\u043B\u044C\u044F \u043D\u0430 \u0441\u043F\u0438\u043D\u0435.", category = Category.RENDER)
public class WingsModule extends Module {
    @Getter private static final WingsModule instance = new WingsModule();

    public final SliderSetting scale = new SliderSetting("\u0420\u0430\u0437\u043C\u0435\u0440").value(1.0f).range(0.5f, 2.0f).step(0.1f);
    public final BooleanSetting neon = new BooleanSetting("\u041D\u0435\u043E\u043D").value(true);
    public final BooleanSetting renderOnOthers = new BooleanSetting("\u0412\u0438\u0434\u0435\u0442\u044C \u043D\u0430 \u0434\u0440\u0443\u0433\u0438\u0445").value(true);
    
    public WingsModule() {
        addSettings(scale, neon, renderOnOthers);
    }

    @Override
    public void onEvent() {
    }
}
