package com.fest.visuals.api.event.events.other;

import lombok.Getter;
import com.fest.visuals.api.event.events.Event;

public class WindowResizeEvent extends Event<WindowResizeEvent> {
    @Getter private static final WindowResizeEvent instance = new WindowResizeEvent();
}
