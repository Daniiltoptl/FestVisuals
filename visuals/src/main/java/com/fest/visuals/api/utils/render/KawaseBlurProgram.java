package com.fest.visuals.api.utils.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.experimental.UtilityClass;
import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.other.FramebufferResizeEvent;
import com.fest.visuals.api.system.interfaces.QuickImports;
import com.fest.visuals.api.utils.framelimiter.FrameLimiter;
import com.fest.visuals.api.utils.render.pipeline.FestPipelines;
import com.fest.visuals.api.utils.render.pipeline.FestRenderer;
import com.fest.visuals.api.utils.render.pipeline.FestTextures;
import com.fest.visuals.api.utils.render.pipeline.FestUniform;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class KawaseBlurProgram implements QuickImports {
    public final List<TextureTarget> fbos = new ArrayList<>();

    private boolean init = false;
    private final FrameLimiter f = new FrameLimiter(false);

    public void load() {
        if (!init) {
            recreate();
            init = true;
        }

        FramebufferResizeEvent.getInstance().subscribe(new Listener<>(event -> recreate()));
    }

    public void recreate() {
        if (mc.getWindow() == null) return;

        fbos.forEach(RenderTarget::destroyBuffers);
        fbos.clear();

        // Dual-filter Kawase: each level is half the size of the one before it. Every pass used to
        // run at full window resolution, which cost several times more for the same result.
        for (int i = 0; i <= InterfaceConfig.getPasses(); i++) {
            fbos.add(createFbo(i + 1));
        }
    }

    public void render(PoseStack matrixStack) {
        if (InterfaceConfig.getGlassy() == 1f || fbos.isEmpty()) return;

        f.execute(40, () -> {
            int actualPasses = Math.max(fbos.size() - 1, 1);

            applyBlurPass(FestPipelines.BLUR_DOWNSCALE, mc.gameRenderer.mainRenderTarget(), fbos.getFirst(), 0, actualPasses);

            for (int i = 0; i < actualPasses; i++) {
                applyBlurPass(FestPipelines.BLUR_DOWNSCALE, fbos.get(i), fbos.get(i + 1), i + 1, actualPasses);
            }

            for (int i = actualPasses; i > 0; i--) {
                applyBlurPass(FestPipelines.BLUR_UPSCALE, fbos.get(i), fbos.get(i - 1), i, actualPasses);
            }
        });
    }

    private void applyBlurPass(RenderPipeline pipeline, RenderTarget source, RenderTarget destination, int pass, int actualPasses) {
        // The offset is in source texels. A smaller level has bigger texels, so scale it down to
        // keep the blur the same width on screen as it was at full resolution.
        float texelScale = source.width / (float) Math.max(1, mc.getWindow().getWidth());
        FestRenderer.getInstance().fullscreen(pipeline, destination, source.getColorTextureView(), FestTextures.sampler(), FestUniform.of(
                0f, 0f, 0f, 0f,
                0.5f / source.width, 0.5f / source.height, InterfaceConfig.getOffset() * (pass / (float) actualPasses) * texelScale, 0f
        ));
    }

    private TextureTarget createFbo(int level) {
        int width = Math.max(1, mc.getWindow().getWidth() >> level);
        int height = Math.max(1, mc.getWindow().getHeight() >> level);
        return new TextureTarget("FestVisuals Blur", width, height, false, mc.gameRenderer.mainRenderTarget().getColorTexture().getFormat());
    }
}
