package com.fest.visuals.client.features.modules.render;

import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.ColorSetting;
import com.fest.visuals.api.module.setting.ModeSetting;
import com.fest.visuals.api.module.setting.SliderSetting;
import lombok.Getter;

import java.awt.Color;

@ModuleRegister(name = "Block Highlight", desc = "Обводка блоков", category = Category.RENDER)
public class BlockHighlightModule extends Module {
    @Getter private static final BlockHighlightModule instance = new BlockHighlightModule();

    public final ModeSetting mode = new ModeSetting("Режим").value("Клиент").values("Клиент", "Кастомный");
    public final ColorSetting color = new ColorSetting("Цвет").value(new Color(255, 120, 0, 200)).setVisible(() -> mode.getValue().equals("Кастомный"));
    public final SliderSetting lineWidth = new SliderSetting("Ширина").value(3f).range(1f, 5f).step(0.5f).setVisible(() -> mode.getValue().equals("Кастомный"));

    public BlockHighlightModule() {
        addSettings(mode, color, lineWidth);
    }

    @Override
    public void onEvent() {}
}