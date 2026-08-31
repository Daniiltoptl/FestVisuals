package com.fest.visuals.client.ui.clickgui.module;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import com.fest.visuals.api.module.setting.Setting;
import com.fest.visuals.api.utils.animation.AnimationUtil;
import com.fest.visuals.client.ui.UIComponent;
import com.fest.visuals.client.ui.clickgui.ClickGuiLayout;

@Getter
@RequiredArgsConstructor
public abstract class SettingComponent extends UIComponent {
    private final Setting<?> setting;
    private final AnimationUtil visibleAnimation = new AnimationUtil();

    /** Typed characters, for the settings that accept free text. */
    public void charTyped(char chr) {
    }

    public void updateHeight(float value) {
        setHeight(scaled(value));
    }

    /**
     * Settings only ever appear inside the click GUI, which is drawn smaller than the rest of the
     * interface — routing through the layout keeps a row the same size as the card around it.
     */
    @Override
    public float scaled(float value) {
        return ClickGuiLayout.scaled(value);
    }
}
