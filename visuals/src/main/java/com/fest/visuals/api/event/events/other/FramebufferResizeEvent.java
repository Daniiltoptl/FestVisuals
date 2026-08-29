package com.fest.visuals.api.event.events.other;

import lombok.Getter;
import com.fest.visuals.api.event.events.Event;

public class FramebufferResizeEvent extends Event<FramebufferResizeEvent> {
    @Getter private static final FramebufferResizeEvent instance = new FramebufferResizeEvent();
}
