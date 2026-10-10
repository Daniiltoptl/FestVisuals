package com.fest.visuals.client.features.modules.hud;

import com.fest.visuals.api.module.Module;
import com.fest.visuals.client.ui.widget.Widget;
import com.fest.visuals.client.ui.widget.WidgetManager;

/**
 * Base for HUD modules that wrap a rendered widget. onEnable/onDisable mirror
 * the enabled state onto the widget instance registered in WidgetManager.
 */
public abstract class HudModule extends Module {
    protected HudModule() {
        // Default-on so widgets are visible on first launch. ConfigManager may override.
        setEnabled(true, true);
    }

    protected abstract String widgetName();

    protected Widget widget() {
        return WidgetManager.getInstance().byName(widgetName());
    }

    /**
     * Pushes the module state onto the widget. Called after both managers have loaded: a module
     * constructed before {@code WidgetManager} finished would otherwise look up a widget that did
     * not exist yet and silently never enable it.
     */
    public void syncWidget() {
        Widget w = widget();
        if (w != null) w.setEnabled(isEnabled());
    }

    @Override
    public void onEnable() {
        Widget w = widget();
        if (w != null) w.setEnabled(true);
    }

    @Override
    public void onDisable() {
        Widget w = widget();
        if (w != null) w.setEnabled(false);
    }

    @Override
    public void onEvent() {}
}
