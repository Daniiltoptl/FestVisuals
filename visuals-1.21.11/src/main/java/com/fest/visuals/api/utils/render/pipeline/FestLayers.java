package com.fest.visuals.api.utils.render.pipeline;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import com.fest.visuals.api.system.backend.ClientInfo;

import java.util.function.Function;

@UtilityClass
public class FestLayers {
    // 1.21.11 uses the ordinary depth buffer (near = 0): vanilla's pipelines test LEQUAL.
    private final RenderPipeline LINES_PIPELINE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(id("pipeline/esp_lines"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build());

    private final RenderPipeline QUADS_PIPELINE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(id("pipeline/esp_quads"))
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(true)
            .build());

    private final RenderPipeline TEXTURED_PIPELINE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(id("pipeline/esp_textured"))
            .withVertexShader(Identifier.withDefaultNamespace("core/position_tex_color"))
            .withFragmentShader(Identifier.withDefaultNamespace("core/position_tex_color"))
            .withBlend(BlendFunction.LIGHTNING)
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .build());

    /**
     * Flat world quads that respect the depth buffer: geometry behind a block or a mob is hidden
     * instead of shining through it, and the blend is ordinary translucency rather than the
     * additive one the ESP layers use, which turns overlapping quads into a white glare.
     */
    private final RenderPipeline OCCLUDED_QUADS_PIPELINE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(id("pipeline/occluded_quads"))
            .withCull(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            // Depth write stays off so overlapping translucent quads do not cull each other.
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .build());

    /** Same flat quads, drawn over everything: for shapes meant to be seen through walls. */
    private final RenderPipeline XRAY_QUADS_PIPELINE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(id("pipeline/xray_quads"))
            .withCull(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build());

    private final RenderPipeline DEBUG_LINES_PIPELINE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(id("pipeline/esp_debug_lines"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.DEBUG_LINES)
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(true)
            .build());

    public final RenderType LINES = RenderType.create("festvisuals_esp_lines", RenderSetup.builder(LINES_PIPELINE).createRenderSetup());
    public final RenderType QUADS = RenderType.create("festvisuals_esp_quads", RenderSetup.builder(QUADS_PIPELINE).sortOnUpload().createRenderSetup());
    // 26.2 only supports vertex sorting for QUADS. Passing a sorting strategy
    // to DEBUG_LINES makes StagedVertexBuffer abort the entire render frame.
    public final RenderType DEBUG_LINES = RenderType.create("festvisuals_debug_lines", RenderSetup.builder(DEBUG_LINES_PIPELINE).createRenderSetup());
    public final RenderType XRAY_QUADS = RenderType.create("festvisuals_xray_quads", RenderSetup.builder(XRAY_QUADS_PIPELINE).sortOnUpload().createRenderSetup());
    public final RenderType OCCLUDED_QUADS = RenderType.create("festvisuals_occluded_quads", RenderSetup.builder(OCCLUDED_QUADS_PIPELINE).sortOnUpload().createRenderSetup());

    private final Function<Identifier, RenderType> TEXTURED = Util.memoize(texture ->
            RenderType.create("festvisuals_esp_textured", RenderSetup.builder(TEXTURED_PIPELINE).withTexture("Sampler0", texture).sortOnUpload().createRenderSetup()));

    public void register() {
    }

    public RenderType textured(Identifier texture) {
        return TEXTURED.apply(texture);
    }

    private Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ClientInfo.NAME.toLowerCase(), path);
    }
}
