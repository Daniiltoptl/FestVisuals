package com.fest.visuals.api.utils.render;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.experimental.UtilityClass;
import com.fest.visuals.api.utils.render.display.*;
import com.fest.visuals.api.utils.render.pipeline.FestRenderer;

@UtilityClass
public class RenderUtil {
    public RectRender RECT = new RectRender();
    public BlurRectRender BLUR_RECT = new BlurRectRender();
    public GradientRectRender GRADIENT_RECT = new GradientRectRender();
    public TextureRectRender TEXTURE_RECT = new TextureRectRender();

    public OtherRender OTHER = new OtherRender();
    public WorldRender WORLD = new WorldRender();
    public BoxRender BOX = new BoxRender();

    public PoseStack matrices() {
        return FestRenderer.getInstance().matrices();
    }
}
