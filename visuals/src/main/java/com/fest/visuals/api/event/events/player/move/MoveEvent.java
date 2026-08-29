package com.fest.visuals.api.event.events.player.move;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.phys.Vec3;
import com.fest.visuals.api.event.events.Event;

public class MoveEvent extends Event<MoveEvent.MoveEventData> {
    @Getter private static final MoveEvent instance = new MoveEvent();

    @Getter
    @Setter
    @AllArgsConstructor
    public static class MoveEventData {
        private double x;
        private double y;
        private double z;

        public void set(Vec3 vec3d) {
            x = vec3d.x();
            y = vec3d.y();
            z = vec3d.z();
        }
    }
}
