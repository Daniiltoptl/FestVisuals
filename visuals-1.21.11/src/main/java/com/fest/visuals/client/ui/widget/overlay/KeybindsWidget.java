package com.fest.visuals.client.ui.widget.overlay;

import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleManager;
import com.fest.visuals.api.system.backend.KeyStorage;
import com.fest.visuals.client.features.modules.hud.BindsHudModule;
import com.fest.visuals.client.ui.widget.ContainerWidget;

import java.util.*;

public class KeybindsWidget extends ContainerWidget {
    public KeybindsWidget() {
        super(3f, 120f);
    }

    @Override
    public String getName() {
        return "Keybinds";
    }

    @Override
    protected Map<String, ContainerElement.ColoredString> getCurrentData() {
        Map<String, ContainerElement.ColoredString> map = new HashMap<>();
        boolean pill = BindsHudModule.getInstance().showKeyPill.getValue();
        for (Module m : ModuleManager.getInstance().getModules()) {
            if (!m.isEnabled() || !m.hasBind()) continue;
            String key = KeyStorage.getBind(m.getBind());
            map.put(m.getName(), new ContainerElement.ColoredString(pill ? "[" + key + "]" : key));
        }
        return map;
    }

    @Override
    public float fontMul() { return BindsHudModule.getInstance().fontScale.getValue(); }

    @Override
    protected boolean alwaysVisible() { return BindsHudModule.getInstance().alwaysShow.getValue(); }
}
