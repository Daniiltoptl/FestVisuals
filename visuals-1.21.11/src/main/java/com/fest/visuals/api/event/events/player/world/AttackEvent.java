package com.fest.visuals.api.event.events.player.world;

import lombok.Getter;
import net.minecraft.world.entity.Entity;
import com.fest.visuals.api.event.events.Event;

public class AttackEvent extends Event<AttackEvent.AttackEventData> {
    @Getter private static final AttackEvent instance = new AttackEvent();

    public record AttackEventData(Entity entity) {}
}
