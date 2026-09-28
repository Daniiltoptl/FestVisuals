package com.fest.visuals.api.event.events.player.world;

import lombok.Getter;
import net.minecraft.world.entity.Entity;
import com.fest.visuals.api.event.events.Event;

/** The local player's swing qualifies as a critical hit, fired as the attack is resolved. */
public class CritEvent extends Event<CritEvent.CritEventData> {
    @Getter private static final CritEvent instance = new CritEvent();

    public record CritEventData(Entity target) {}
}
