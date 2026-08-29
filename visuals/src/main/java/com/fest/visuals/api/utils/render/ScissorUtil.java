package com.fest.visuals.api.utils.render;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.experimental.UtilityClass;
import com.fest.visuals.api.utils.render.pipeline.FestRenderer;

@UtilityClass
public class ScissorUtil {
    public void start(PoseStack matrixStack, float x, float y, float width, float height) {
        matrixStack.pushPose();
        FestRenderer.getInstance().pushScissor(x, y, width, height);
    }

    public void stop(PoseStack matrixStack) {
        FestRenderer.getInstance().popScissor();
        matrixStack.popPose();
    }
}
