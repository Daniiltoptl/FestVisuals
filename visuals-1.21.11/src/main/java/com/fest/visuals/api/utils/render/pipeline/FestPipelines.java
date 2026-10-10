package com.fest.visuals.api.utils.render.pipeline;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import com.fest.visuals.api.system.backend.ClientInfo;

@UtilityClass
public class FestPipelines {
    public final String UBO = "EvaParams";

    public final RenderPipeline RECT = RenderPipelines.register(gui("rect", "rect/rect", DefaultVertexFormat.POSITION_COLOR).build());
    public final RenderPipeline GRADIENT_RECT = RenderPipelines.register(gui("gradient_rect", "rect/gradient_rect", DefaultVertexFormat.POSITION_COLOR).build());
    public final RenderPipeline TEXTURE_RECT = RenderPipelines.register(gui("texture_rect", "rect/texture_rect", DefaultVertexFormat.POSITION_TEX_COLOR).build());
    public final RenderPipeline BLURRED_RECT = RenderPipelines.register(gui("blurred_rect", "rect/blurred_rect", DefaultVertexFormat.POSITION_TEX_COLOR).build());
    public final RenderPipeline TEXT_10 = text("text_10", 10.0f);
    public final RenderPipeline TEXT_12 = text("text_12", 12.0f);
    public final RenderPipeline TEXT_32 = text("text_32", 32.0f);
    public final RenderPipeline TEXT = TEXT_32;

    public final RenderPipeline BLUR_DOWNSCALE = fullscreen("blur_downscale", "post/blur/downscale");
    public final RenderPipeline BLUR_UPSCALE = fullscreen("blur_upscale", "post/blur/upscale");
    public final RenderPipeline MOTION_BLUR = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(id("pipeline/motion_blur"))
            .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
            .withFragmentShader(shader("post/motionblur/motion_blur"))
            .withSampler("MainSampler")
            .withSampler("MainDepthSampler")
            .withUniform(UBO, UniformType.UNIFORM_BUFFER)
            .build());

    public void register() {
    }

    private RenderPipeline.Builder gui(String name, String shader, VertexFormat format) {
        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET);
        if (format == DefaultVertexFormat.POSITION_TEX_COLOR) {
            builder.withSampler("Sampler0");
        }
        return builder.withUniform(UBO, UniformType.UNIFORM_BUFFER)
                .withLocation(id("pipeline/" + name))
                .withVertexShader(shader(shader))
                .withFragmentShader(shader(shader))
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withVertexFormat(format, VertexFormat.Mode.QUADS);
    }

    private RenderPipeline fullscreen(String name, String shader) {
        return RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                .withLocation(id("pipeline/" + name))
                .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
                .withFragmentShader(shader(shader))
                .withSampler("InSampler")
                .withUniform(UBO, UniformType.UNIFORM_BUFFER)
                .build());
    }

    private RenderPipeline text(String name, float range) {
        return RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                .withLocation(id("pipeline/" + name))
                .withFragmentShader(shader("msdf_text"))
                .withShaderDefine("MSDF_RANGE", range)
                .withUniform(UBO, UniformType.UNIFORM_BUFFER)
                .build());
    }

    private Identifier shader(String path) {
        return Identifier.fromNamespaceAndPath(ClientInfo.NAME.toLowerCase(), "core/" + path);
    }

    private Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ClientInfo.NAME.toLowerCase(), path);
    }
}
