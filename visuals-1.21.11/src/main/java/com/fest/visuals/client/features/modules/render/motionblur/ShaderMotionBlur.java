package com.fest.visuals.client.features.modules.render.motionblur;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DynamicUniformStorage;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;
import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.render.Render3DEvent;
import com.fest.visuals.api.utils.other.TextUtil;
import com.fest.visuals.api.utils.render.pipeline.FestMotionBlurUniform;
import com.fest.visuals.api.utils.render.pipeline.FestPipelines;
import com.fest.visuals.api.utils.render.pipeline.FestTextures;

import java.util.OptionalDouble;
import java.util.OptionalInt;

public class ShaderMotionBlur {
    private final MotionBlurModule config;

    private @Nullable DynamicUniformStorage<FestMotionBlurUniform> uniforms;
    private @Nullable TextureTarget target;

    private final Matrix4f mvInverse = new Matrix4f();
    private final Matrix4f projInverse = new Matrix4f();
    private final Matrix4f prevModelView = new Matrix4f();
    private final Matrix4f prevProjection = new Matrix4f();
    private final Vector3f cameraPos = new Vector3f();
    private final Vector3f prevCameraPos = new Vector3f();

    private long lastNano;
    private float currentFPS = 0.0f;
    private float strength = 0.0f;

    public ShaderMotionBlur(MotionBlurModule config) {
        this.config = config;
    }

    public void registerShaderCallbacks() {
        Render3DEvent.getInstance().subscribe(new Listener<>(-1, event -> {
            long now = System.nanoTime();
            float deltaTime = (now - lastNano) / 1_000_000_000.0f;
            lastNano = now;

            currentFPS = (deltaTime > 0 && deltaTime < 1.0f) ? 1.0f / deltaTime : 0.0f;

            if (shouldRenderMotionBlur()) {
                applyMotionBlur();
            }
        }));
    }

    private boolean shouldRenderMotionBlur() {
        if (config.strength.getValue() == 0 || !config.isEnabled()) {
            return false;
        }
        if (FabricLoader.getInstance().isModLoaded("iris")) {
            TextUtil.sendMessage("Motion Blur не работает вместе с Iris — шейдеры занимают тот же проход.");
            config.setEnabled(false);
            return false;
        }

        return true;
    }

    private void applyMotionBlur() {
        Minecraft client = Minecraft.getInstance();
        RenderTarget main = client.getMainRenderTarget();

        MonitorInfoProvider.updateDisplayInfo();
        int displayRefreshRate = MonitorInfoProvider.getRefreshRate();

        float baseStrength = config.strength.getValue();
        strength = baseStrength;
        if (config.useRRC.getValue()) {
            float fpsOverRefresh = (displayRefreshRate > 0) ? currentFPS / displayRefreshRate : 1.0f;
            strength = baseStrength * Math.max(fpsOverRefresh, 1.0f);
        }

        int samples = getSampleAmountForFPS(currentFPS);

        if (uniforms == null) uniforms = new DynamicUniformStorage<>("FestVisuals MotionBlur", FestMotionBlurUniform.SIZE, 2);
        if (target == null || target.width != main.width || target.height != main.height) {
            if (target != null) target.destroyBuffers();
            target = new TextureTarget("FestVisuals MotionBlur", main.width, main.height, false);
        }

        GpuBufferSlice slice = uniforms.writeUniform(new FestMotionBlurUniform(
                mvInverse, projInverse, prevModelView, prevProjection,
                cameraPos, prevCameraPos,
                main.width, main.height, MotionBlurModule.BlurAlgorithm.BACKWARDS.ordinal(),
                strength, 1.0f / samples, samples, samples / 2f
        ));

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "FestVisuals MotionBlur", target.getColorTextureView(), java.util.OptionalInt.empty())) {
            pass.setPipeline(FestPipelines.MOTION_BLUR);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform(FestPipelines.UBO, slice);
            pass.bindTexture("MainSampler", main.getColorTextureView(), FestTextures.sampler());
            pass.bindTexture("MainDepthSampler", main.getDepthTextureView(), FestTextures.nearest());
            pass.draw(0, 3);
        }

        target.blitAndBlendToTexture(main.getColorTextureView());
        uniforms.endFrame();
    }

    private int getSampleAmountForFPS(float fps) {
        if (fps > 360) return 8;
        else if (fps > 120) return 10;
        else if (fps > 60) return 12;
        else return 20;
    }

    public void setFrameMotionBlur(Matrix4f modelView, Matrix4f previousModelView, Matrix4f projection, Matrix4f previousProjection, Vector3f camera, Vector3f previousCamera) {
        mvInverse.set(modelView).invert();
        projInverse.set(projection).invert();
        prevModelView.set(previousModelView);
        prevProjection.set(previousProjection);
        cameraPos.set(camera);
        prevCameraPos.set(previousCamera);
    }

    public void updateBlurStrength(float value) {
        strength = value;
    }
}
