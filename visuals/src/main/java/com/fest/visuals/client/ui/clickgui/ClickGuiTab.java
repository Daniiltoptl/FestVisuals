package com.fest.visuals.client.ui.clickgui;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import com.fest.visuals.api.module.Category;

@Getter
@RequiredArgsConstructor
public enum ClickGuiTab {
    RENDER(Category.RENDER, "Render", "Настройки визуалов"),
    HUD(Category.HUD, "HUD", "Настройки интерфейса"),
    PLAYER(Category.PLAYER, "Игрок", "Настройки игрока"),
    OTHER(Category.OTHER, "Разное", "Различные настройки"),
    CONFIGS(null, "Конфиги", "Управление конфигами"),
    WAYPOINTS(null, "Метки", "Управление метками"),
    THEME(null, "Темы", "Настройки внешнего вида");

    private final Category category;
    private final String label;
    private final String description;

    public boolean isCategory() {
        return category != null;
    }

    public static ClickGuiTab[] strip() {
        return values();
    }

    public static ClickGuiTab of(Category category) {
        for (ClickGuiTab tab : values()) {
            if (tab.category == category) return tab;
        }
        return RENDER;
    }
}