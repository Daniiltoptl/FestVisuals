package com.fest.visuals.client.features.modules.render;

import lombok.Getter;
import org.lwjgl.glfw.GLFW;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.client.ui.clickgui.ScreenClickGUI;

@ModuleRegister(name = "Click GUI", desc = "Это меню", category = Category.RENDER, bind = GLFW.GLFW_KEY_RIGHT_SHIFT)
public class ClickGUIModule extends Module {
    @Getter private static final ClickGUIModule instance = new ClickGUIModule();

    public ClickGUIModule() {

    }

    @Override
    public void onEnable() {
        if (mc.gui.screen() != null) return;

        mc.gui.setScreen(ScreenClickGUI.getInstance());
    }

    @Override
    public void onEvent() {
        toggle();
    }
}
