package com.fest.visuals.api.utils.render.pipeline;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.PrimitiveTopology;
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
    private final RenderPipeline LINES_PIPELINE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(id("pipeline/esp_lines"))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build());

    private final RenderPipeline QUADS_PIPELINE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(id("pipeline/esp_quads"))
            .withCull(false)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
            .build());

    private final RenderPipeline TEXTURED_PIPELINE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(id("pipeline/esp_textured"))
            .withVertexShader(Identifier.withDefaultNamespace("core/position_tex_color"))
            .withFragmentShader(Identifier.withDefaultNamespace("core/position_tex_color"))
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withCull(false)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .build());

    private final RenderPipeline DEBUG_LINES_PIPELINE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(id("pipeline/esp_debug_lines"))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.DEBUG_LINES)
            .withCull(false)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
            .build());

    public final RenderType LINES = RenderType.create("festvisuals_esp_lines", RenderSetup.builder(LINES_PIPELINE).createRenderSetup());
    public final RenderType QUADS = RenderType.create("festvisuals_esp_quads", RenderSetup.builder(QUADS_PIPELINE).sortOnUpload().createRenderSetup());
    // 26.2 only supports vertex sorting for QUADS. Passing a sorting strategy
    // to DEBUG_LINES makes StagedVertexBuffer abort the entire render frame.
    public final RenderType DEBUG_LINES = RenderType.create("festvisuals_debug_lines", RenderSetup.builder(DEBUG_LINES_PIPELINE).createRenderSetup());

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
