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
            if (batch.opCount == 0) continue;
            // The batch is never written again once submitted: buffer() always hands out a new one.
            collector.submitCustomGeometry(new PoseStack(), batch.layer, (pose, target) -> batch.replay(target));
        }
        batches.clear();
    }

    /**
     * Vertex calls recorded into flat primitive arrays and replayed when the collector draws.
     *
     * <p>This used to store one lambda per attribute call, so a single hitbox cost a few hundred
     * allocations every frame. Opcodes plus packed floats and ints make recording allocation-free
     * apart from the occasional array growth.
     */
    private static final class RecordedBatch {
        private static final byte VERTEX = 0, COLOR = 1, COLOR4 = 2, UV = 3, UV1 = 4, UV2 = 5, NORMAL = 6, LINE_WIDTH = 7;

        private final RenderType layer;
        private byte[] ops = new byte[256];
        private int opCount;
        private float[] floats = new float[512];
        private int floatCount;
        private int[] ints = new int[256];
        private int intCount;

        private final VertexConsumer consumer = new VertexConsumer() {
            @Override public VertexConsumer addVertex(float x, float y, float z) { op(VERTEX); f(x); f(y); f(z); return this; }
            @Override public VertexConsumer setColor(int r, int g, int b, int a) { op(COLOR4); i(r); i(g); i(b); i(a); return this; }
            @Override public VertexConsumer setColor(int color) { op(COLOR); i(color); return this; }
            @Override public VertexConsumer setUv(float u, float v) { op(UV); f(u); f(v); return this; }
            @Override public VertexConsumer setUv1(int u, int v) { op(UV1); i(u); i(v); return this; }
            @Override public VertexConsumer setUv2(int u, int v) { op(UV2); i(u); i(v); return this; }
            @Override public VertexConsumer setNormal(float x, float y, float z) { op(NORMAL); f(x); f(y); f(z); return this; }
            @Override public VertexConsumer setLineWidth(float width) { op(LINE_WIDTH); f(width); return this; }
        };

        private RecordedBatch(RenderType layer) {
            this.layer = layer;
        }

        private void op(byte op) {
            if (opCount == ops.length) ops = java.util.Arrays.copyOf(ops, ops.length * 2);
            ops[opCount++] = op;
        }

        private void f(float value) {
            if (floatCount == floats.length) floats = java.util.Arrays.copyOf(floats, floats.length * 2);
            floats[floatCount++] = value;
        }

        private void i(int value) {
            if (intCount == ints.length) ints = java.util.Arrays.copyOf(ints, ints.length * 2);
            ints[intCount++] = value;
        }

        private void replay(VertexConsumer out) {
            int fi = 0, ii = 0;
            for (int k = 0; k < opCount; k++) {
                switch (ops[k]) {
                    case VERTEX -> { out.addVertex(floats[fi], floats[fi + 1], floats[fi + 2]); fi += 3; }
                    case COLOR -> out.setColor(ints[ii++]);
                    case COLOR4 -> { out.setColor(ints[ii], ints[ii + 1], ints[ii + 2], ints[ii + 3]); ii += 4; }
                    case UV -> { out.setUv(floats[fi], floats[fi + 1]); fi += 2; }
                    case UV1 -> { out.setUv1(ints[ii], ints[ii + 1]); ii += 2; }
                    case UV2 -> { out.setUv2(ints[ii], ints[ii + 1]); ii += 2; }
                    case NORMAL -> { out.setNormal(floats[fi], floats[fi + 1], floats[fi + 2]); fi += 3; }
                    case LINE_WIDTH -> out.setLineWidth(floats[fi++]);
                    default -> { }
                }
            }
        }
    }
}
