package com.fest.visuals.api.utils.render.pipeline;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import java.nio.ByteBuffer;
import java.util.Arrays;
import net.minecraft.client.renderer.DynamicUniformStorage;

public record FestUniform(float[] values) implements DynamicUniformStorage.DynamicUniform {
    public static final int MAX_VEC4 = 8;
    public static final int SIZE = size();

    public static FestUniform of(float... values) {
        float[] padded = Arrays.copyOf(values, MAX_VEC4 * 4);
        return new FestUniform(padded);
    }

    @Override
    public void write(ByteBuffer buffer) {
        Std140Builder builder = Std140Builder.intoBuffer(buffer);
        for (int i = 0; i < MAX_VEC4; i++) {
            int o = i * 4;
            builder.putVec4(values[o], values[o + 1], values[o + 2], values[o + 3]);
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FestUniform other && Arrays.equals(values, other.values);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }

    private static int size() {
        Std140SizeCalculator calculator = new Std140SizeCalculator();
        for (int i = 0; i < MAX_VEC4; i++) {
            calculator.putVec4();
        }
        return calculator.get();
    }
}
