package com.fest.visuals.api.utils.render;

import org.joml.Matrix4f;

/** The camera matrices of the level pass being drawn, kept for projecting world points to screen. */
public final class RenderMatrices {
    private static final Matrix4f MODEL_VIEW = new Matrix4f();
    private static final Matrix4f PROJECTION = new Matrix4f();

    private RenderMatrices() {
    }

    public static void update(Matrix4f modelView, Matrix4f projection) {
        MODEL_VIEW.set(modelView);
        PROJECTION.set(projection);
    }

    public static Matrix4f modelView() {
        return MODEL_VIEW;
    }

    public static Matrix4f projection() {
        return PROJECTION;
    }

    /** Projection times model-view: camera-relative world position to clip space. */
    public static Matrix4f viewProjection(Matrix4f dest) {
        return dest.set(PROJECTION).mul(MODEL_VIEW);
    }
}
