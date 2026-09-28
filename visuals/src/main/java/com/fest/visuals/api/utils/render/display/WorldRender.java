package com.fest.visuals.api.utils.render.display;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import com.fest.visuals.api.system.interfaces.QuickImports;
import com.fest.visuals.api.utils.render.pipeline.FestLayers;

public class WorldRender implements QuickImports {
    private SubmitNodeCollector collector;
    private final java.util.List<RecordedBatch> batches = new java.util.ArrayList<>();

    public void beginFrame(SubmitNodeCollector collector) {
        this.collector = collector;
        this.batches.clear();
    }

    /**
     * Flushes whatever is queued. The collector is deliberately kept: several renderers submit
     * inside the same pass, and one of them calling this used to leave every later renderer with
     * nothing to submit into. It is replaced on the next {@link #beginFrame}.
     */
    public void finishFrame() {
        submitBatches();
    }

    public VertexConsumer buffer(RenderType layer) {
        RecordedBatch batch = new RecordedBatch(layer);
        batches.add(batch);
        return batch.consumer;
    }

    public VertexConsumer textured(Identifier texture) {
        return buffer(FestLayers.textured(texture));
    }

    /** Flat, depth-tested, translucent quads: hidden behind blocks and mobs. */
    public VertexConsumer occludedQuads() {
        return buffer(FestLayers.OCCLUDED_QUADS);
    }

    /** Flat translucent quads drawn on top of everything. */
    public VertexConsumer xrayQuads() {
        return buffer(FestLayers.XRAY_QUADS);
    }

    public VertexConsumer lines() {
        return buffer(FestLayers.DEBUG_LINES);
    }

    public void startRender(PoseStack matrixStack) {
        matrixStack.pushPose();
    }

    public void endRender(PoseStack matrixStack) {
        submitBatches();
        matrixStack.popPose();
    }

    private void submitBatches() {
        if (collector == null || batches.isEmpty()) return;
        for (RecordedBatch batch : batches) {
            var operations = java.util.List.copyOf(batch.operations);
            collector.submitCustomGeometry(new PoseStack(), batch.layer,
                    (pose, target) -> operations.forEach(operation -> operation.accept(target)));
        }
        batches.clear();
    }

    private static final class RecordedBatch {
        private final RenderType layer;
        private final java.util.List<java.util.function.Consumer<VertexConsumer>> operations = new java.util.ArrayList<>();
        private final VertexConsumer consumer = new VertexConsumer() {
            @Override public VertexConsumer addVertex(float x, float y, float z) { operations.add(v -> v.addVertex(x, y, z)); return this; }
            @Override public VertexConsumer setColor(int r, int g, int b, int a) { operations.add(v -> v.setColor(r, g, b, a)); return this; }
            @Override public VertexConsumer setColor(int color) { operations.add(v -> v.setColor(color)); return this; }
            @Override public VertexConsumer setUv(float u, float v) { operations.add(out -> out.setUv(u, v)); return this; }
            @Override public VertexConsumer setUv1(int u, int v) { operations.add(out -> out.setUv1(u, v)); return this; }
            @Override public VertexConsumer setUv2(int u, int v) { operations.add(out -> out.setUv2(u, v)); return this; }
            @Override public VertexConsumer setNormal(float x, float y, float z) { operations.add(v -> v.setNormal(x, y, z)); return this; }
            @Override public VertexConsumer setLineWidth(float width) { operations.add(v -> v.setLineWidth(width)); return this; }
        };

        private RecordedBatch(RenderType layer) {
            this.layer = layer;
        }
    }
}
