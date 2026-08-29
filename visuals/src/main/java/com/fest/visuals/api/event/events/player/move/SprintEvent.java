package com.fest.visuals.api.event.events.player.move;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import com.fest.visuals.api.event.events.Event;
import com.fest.visuals.api.utils.player.DirectionalInput;

public class SprintEvent extends Event<SprintEvent.SprintEventData> {
    @Getter private static final SprintEvent instance = new SprintEvent();

    @Override
    public boolean call(SprintEventData any) {
        
        super.call(any);
        return any.isSprint();
    }

    @Setter
    @Getter
    @lombok.AllArgsConstructor
    public static class SprintEventData {
        private boolean sprint;

        private final DirectionalInput directionalInput;
    }
}
