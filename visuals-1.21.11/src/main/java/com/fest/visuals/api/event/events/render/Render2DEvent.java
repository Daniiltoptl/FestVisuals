package com.fest.visuals.api.event.events.render;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import com.fest.visuals.api.event.events.Event;

public class Render2DEvent extends Event<Render2DEvent.Render2DEventData> {
    @Getter private static final Render2DEvent instance = new Render2DEvent();

    public record Render2DEventData(GuiGraphics context, PoseStack matrixStack, float partialTicks) { }
}
