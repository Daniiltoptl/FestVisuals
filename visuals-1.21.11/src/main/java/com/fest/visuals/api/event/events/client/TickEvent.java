package com.fest.visuals.api.event.events.client;

import lombok.Getter;
import com.fest.visuals.api.event.events.Event;

public class TickEvent extends Event<TickEvent> {
    @Getter private static final TickEvent instance = new TickEvent();
}
