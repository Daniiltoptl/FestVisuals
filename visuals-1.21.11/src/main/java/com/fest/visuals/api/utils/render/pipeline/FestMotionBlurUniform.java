package com.fest.visuals.api.utils.render.pipeline;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import net.minecraft.client.renderer.DynamicUniformStorage;

public record FestMotionBlurUniform(Matrix4f mvInverse, Matrix4f projInverse, Matrix4f prevModelView, Matrix4f prevProjection,
                                   Vector3f cameraPos, Vector3f prevCameraPos, float width, float height, float algorithm,
                                   float blendFactor, float inverseSamples, float samples, float halfSamples) implements DynamicUniformStorage.DynamicUniform {
    public static final int SIZE = new Std140SizeCalculator()
            .putMat4f().putMat4f().putMat4f().putMat4f()
            .putVec4().putVec4().putVec4().putVec4()
            .get();

    @Override
    public void write(ByteBuffer buffer) {
        Std140Builder.intoBuffer(buffer)
                .putMat4f(mvInverse)
                .putMat4f(projInverse)
                .putMat4f(prevModelView)
                .putMat4f(prevProjection)
                .putVec4(cameraPos.x, cameraPos.y, cameraPos.z, 0f)
                .putVec4(prevCameraPos.x, prevCameraPos.y, prevCameraPos.z, 0f)
                .putVec4(width, height, algorithm, 0f)
                .putVec4(blendFactor, inverseSamples, samples, halfSamples);
    }
}
