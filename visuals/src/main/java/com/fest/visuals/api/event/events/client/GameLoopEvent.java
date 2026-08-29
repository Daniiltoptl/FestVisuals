package com.fest.visuals.api.event.events.client;

import lombok.Getter;
import com.fest.visuals.api.event.events.Event;

public class GameLoopEvent extends Event<GameLoopEvent> {
    @Getter private static final GameLoopEvent instance = new GameLoopEvent();
}
