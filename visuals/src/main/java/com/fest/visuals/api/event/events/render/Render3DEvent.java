package com.fest.visuals.api.event.events.render;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import com.fest.visuals.api.event.events.Event;

public class Render3DEvent extends Event<Render3DEvent.Render3DEventData> {
    @Getter private static final Render3DEvent instance = new Render3DEvent();

    public record Render3DEventData(PoseStack matrixStack, float partialTicks,
                                    SubmitNodeCollector collector, CameraRenderState cameraState) { }
}
