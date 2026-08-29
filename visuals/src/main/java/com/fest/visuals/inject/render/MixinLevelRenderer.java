package com.fest.visuals.inject.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.fest.visuals.client.features.modules.render.motionblur.MotionBlurModule;

import com.fest.visuals.client.features.modules.render.BlockHighlightModule;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.mojang.blaze3d.vertex.VertexConsumer;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
    @Unique private Matrix4f prevModelView = new Matrix4f();
    @Unique private Matrix4f prevProjection = new Matrix4f();
    @Unique private Vector3f prevCameraPos = new Vector3f();

    @Inject(method = "render", at = @At("HEAD"))
    private void setMatrices(GraphicsResourceAllocator allocator, DeltaTracker tickCounter, boolean renderBlockOutline,
                             CameraRenderState camera, Matrix4fc positionMatrix, GpuBufferSlice fogBuffer,
                             Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        MotionBlurModule.getInstance().shader.setFrameMotionBlur(new Matrix4f(positionMatrix), prevModelView,
                new Matrix4f(camera.projectionMatrix), prevProjection, cameraPos(camera), prevCameraPos);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void setOldMatrices(GraphicsResourceAllocator allocator, DeltaTracker tickCounter, boolean renderBlockOutline,
                                CameraRenderState camera, Matrix4fc positionMatrix, GpuBufferSlice fogBuffer,
                                Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        prevModelView = new Matrix4f(positionMatrix);
        prevProjection = new Matrix4f(camera.projectionMatrix);
        prevCameraPos = cameraPos(camera);
    }

    @Unique
    private Vector3f cameraPos(CameraRenderState camera) {
        return new Vector3f(
                (float) (camera.pos.x % 30000f),
                (float) (camera.pos.y % 30000f),
                (float) (camera.pos.z % 30000f)
        );
    }

    @Inject(method = "submitBlockOutline", at = @At("HEAD"), cancellable = true)
    private void customBlockOutline(PoseStack poseStack, SubmitNodeCollector collector, LevelRenderState state, CallbackInfo ci) {
        BlockHighlightModule module = BlockHighlightModule.getInstance();
        if (module != null && module.isEnabled()) {
            BlockOutlineRenderState outline = state.blockOutlineRenderState;
            if (outline == null) return;
            
            ci.cancel();

            Vector3f cam = cameraPos(state.cameraRenderState);
            net.minecraft.core.BlockPos pos = outline.pos();
            double x = pos.getX() - state.cameraRenderState.pos.x;
            double y = pos.getY() - state.cameraRenderState.pos.y;
            double z = pos.getZ() - state.cameraRenderState.pos.z;

            java.awt.Color c = new java.awt.Color(255, 120, 0, 200);

            if (module.mode.getValue().equals("Р В РЎв„ўР В Р’В°Р РЋР С“Р РЋРІР‚С™Р В РЎвЂўР В РЎВР В Р вЂ¦Р РЋРІР‚в„–Р В РІвЂћвЂ“")) {
                c = module.color.getValue();
            }

            VoxelShape shape = outline.shape();
            RenderUtil.WORLD.beginFrame(collector); RenderUtil.WORLD.startRender(poseStack);
            VertexConsumer buffer = RenderUtil.WORLD.buffer(com.fest.visuals.api.utils.render.pipeline.FestLayers.DEBUG_LINES);
            Matrix4f matrix = poseStack.last().pose(); float width = module.lineWidth.getValue(); buffer.setLineWidth(width);

            for (AABB box : shape.toAabbs()) {
                float minX = (float)(x + box.minX);
                float minY = (float)(y + box.minY);
                float minZ = (float)(z + box.minZ);
                float maxX = (float)(x + box.maxX);
                float maxY = (float)(y + box.maxY);
                float maxZ = (float)(z + box.maxZ);

                buffer.addVertex(matrix, minX, minY, minZ).setColor(c.getRGB()).setNormal(poseStack.last(), 1, 0, 0);
                buffer.addVertex(matrix, maxX, minY, minZ).setColor(c.getRGB()).setNormal(poseStack.last(), 1, 0, 0);
                buffer.addVertex(matrix, maxX, minY, minZ).setColor(c.getRGB()).setNormal(poseStack.last(), 0, 1, 0);
                buffer.addVertex(matrix, maxX, maxY, minZ).setColor(c.getRGB()).setNormal(poseStack.last(), 0, 1, 0);
                buffer.addVertex(matrix, maxX, maxY, minZ).setColor(c.getRGB()).setNormal(poseStack.last(), -1, 0, 0);
                buffer.addVertex(matrix, minX, maxY, minZ).setColor(c.getRGB()).setNormal(poseStack.last(), -1, 0, 0);
                buffer.addVertex(matrix, minX, maxY, minZ).setColor(c.getRGB()).setNormal(poseStack.last(), 0, -1, 0);
                buffer.addVertex(matrix, minX, minY, minZ).setColor(c.getRGB()).setNormal(poseStack.last(), 0, -1, 0);

                buffer.addVertex(matrix, minX, minY, maxZ).setColor(c.getRGB()).setNormal(poseStack.last(), 1, 0, 0);
                buffer.addVertex(matrix, maxX, minY, maxZ).setColor(c.getRGB()).setNormal(poseStack.last(), 1, 0, 0);
                buffer.addVertex(matrix, maxX, minY, maxZ).setColor(c.getRGB()).setNormal(poseStack.last(), 0, 1, 0);
                buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(c.getRGB()).setNormal(poseStack.last(), 0, 1, 0);
                buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(c.getRGB()).setNormal(poseStack.last(), -1, 0, 0);
                buffer.addVertex(matrix, minX, maxY, maxZ).setColor(c.getRGB()).setNormal(poseStack.last(), -1, 0, 0);
                buffer.addVertex(matrix, minX, maxY, maxZ).setColor(c.getRGB()).setNormal(poseStack.last(), 0, -1, 0);
                buffer.addVertex(matrix, minX, minY, maxZ).setColor(c.getRGB()).setNormal(poseStack.last(), 0, -1, 0);

                buffer.addVertex(matrix, minX, minY, minZ).setColor(c.getRGB()).setNormal(poseStack.last(), 0, 0, 1);
                buffer.addVertex(matrix, minX, minY, maxZ).setColor(c.getRGB()).setNormal(poseStack.last(), 0, 0, 1);
                buffer.addVertex(matrix, maxX, minY, minZ).setColor(c.getRGB()).setNormal(poseStack.last(), 0, 0, 1);
                buffer.addVertex(matrix, maxX, minY, maxZ).setColor(c.getRGB()).setNormal(poseStack.last(), 0, 0, 1);
                buffer.addVertex(matrix, maxX, maxY, minZ).setColor(c.getRGB()).setNormal(poseStack.last(), 0, 0, 1);
                buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(c.getRGB()).setNormal(poseStack.last(), 0, 0, 1);
                buffer.addVertex(matrix, minX, maxY, minZ).setColor(c.getRGB()).setNormal(poseStack.last(), 0, 0, 1);
                buffer.addVertex(matrix, minX, maxY, maxZ).setColor(c.getRGB()).setNormal(poseStack.last(), 0, 0, 1);
            }
            RenderUtil.WORLD.endRender(poseStack); RenderUtil.WORLD.finishFrame();
        }
    }
}