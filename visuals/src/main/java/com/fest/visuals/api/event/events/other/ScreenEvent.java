package com.fest.visuals.api.event.events.other;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import com.fest.visuals.api.event.events.Event;

import java.util.ArrayList;
import java.util.List;

public class ScreenEvent extends Event<ScreenEvent.ScreenEventData> {
    @Getter private static final ScreenEvent instance = new ScreenEvent();

    @Getter
    @Accessors(fluent = true)
    @AllArgsConstructor
    public static class ScreenEventData {
        private final Screen screen;
        private final List<Button> buttons = new ArrayList<>();
    }
}
