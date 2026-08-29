package com.fest.visuals.api.event.events.player.move;

import lombok.Getter;
import com.fest.visuals.api.event.events.Event;

public class CollisionEvent extends Event<CollisionEvent> {
    @Getter private static final CollisionEvent instance = new CollisionEvent();
}
