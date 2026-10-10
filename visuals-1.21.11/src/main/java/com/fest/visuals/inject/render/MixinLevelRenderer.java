package com.fest.visuals.inject.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import com.fest.visuals.api.event.events.render.Render3DEvent;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.fest.visuals.client.features.modules.render.motionblur.MotionBlurModule;

import com.fest.visuals.client.features.modules.render.BlockHighlightModule;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.mojang.blaze3d.vertex.VertexConsumer;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
    @Unique private Matrix4f prevModelView = new Matrix4f();
    @Unique private Matrix4f prevProjection = new Matrix4f();
    @Unique private Vector3f prevCameraPos = new Vector3f();

    @Shadow @Final private SubmitNodeStorage submitNodeStorage;
    @Shadow @Final private LevelRenderState levelRenderState;

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void setMatrices(GraphicsResourceAllocator allocator, DeltaTracker tickCounter, boolean renderBlockOutline,
                             Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix, Matrix4f cullingMatrix,
                             GpuBufferSlice fogBuffer, Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        com.fest.visuals.api.utils.render.RenderMatrices.update(positionMatrix, projectionMatrix);
        MotionBlurModule.getInstance().shader.setFrameMotionBlur(new Matrix4f(positionMatrix), prevModelView,
                new Matrix4f(projectionMatrix), prevProjection, cameraPos(camera), prevCameraPos);
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void setOldMatrices(GraphicsResourceAllocator allocator, DeltaTracker tickCounter, boolean renderBlockOutline,
                                Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix, Matrix4f cullingMatrix,
                                GpuBufferSlice fogBuffer, Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        prevModelView = new Matrix4f(positionMatrix);
        prevProjection = new Matrix4f(projectionMatrix);
        prevCameraPos = cameraPos(camera);
    }

    @Unique
    private Vector3f cameraPos(Camera camera) {
        return new Vector3f(
                (float) (camera.position().x % 30000f),
                (float) (camera.position().y % 30000f),
                (float) (camera.position().z % 30000f)
        );
    }

    /** The world-space draw point of the mod's 3D features: entities are submitted here, camera-relative. */
    @Inject(method = "submitEntities", at = @At("TAIL"))
    private void festvisuals$render3d(PoseStack poseStack, LevelRenderState state, SubmitNodeCollector collector, CallbackInfo ci) {
        RenderUtil.WORLD.beginFrame(collector);
        Render3DEvent.getInstance().call(new Render3DEvent.Render3DEventData(
                poseStack,
                Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false),
                collector, state.cameraRenderState));
        RenderUtil.WORLD.finishFrame();
    }

    @Inject(method = "renderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void customBlockOutline(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, boolean bl, LevelRenderState state, CallbackInfo ci) {
        SubmitNodeCollector collector = this.submitNodeStorage;
        BlockHighlightModule module = BlockHighlightModule.getInstance();
        if (module != null && module.isEnabled()) {
            BlockOutlineRenderState outline = state.blockOutlineRenderState;
            if (outline == null) return;
            
            ci.cancel();

            net.minecraft.core.BlockPos pos = outline.pos();
            double[] anim = module.animate(pos);
            double x = pos.getX() - state.cameraRenderState.pos.x + anim[0];
            double y = pos.getY() - state.cameraRenderState.pos.y + anim[1];
            double z = pos.getZ() - state.cameraRenderState.pos.z + anim[2];
            float visible = (float) anim[3];

            java.awt.Color c = module.outlineColor();

            VoxelShape shape = outline.shape();
            RenderUtil.WORLD.beginFrame(collector);
            RenderUtil.WORLD.startRender(poseStack);

            VertexConsumer buffer = RenderUtil.WORLD.buffer(com.fest.visuals.api.utils.render.pipeline.FestLayers.QUADS);
            Matrix4f matrix = poseStack.last().pose();

            // Edges are solid boxes rather than GL lines. Line width is a hint a driver may clamp
            // to one pixel, which left the slider doing nothing, and lines thin out with distance
            // unevenly; a box has the same thickness on every edge.
            float half = Math.max(0.2f, module.lineWidth.getValue()) * 0.004f;
            int argb = (Math.round(c.getAlpha() * visible) << 24) | (c.getRGB() & 0xFFFFFF);

            if (module.fill.getValue()) {
                VertexConsumer fillBuffer = RenderUtil.WORLD.occludedQuads();
                float breathe = (float) (0.75 + 0.25 * Math.sin(System.currentTimeMillis() / 300.0));
                int fillArgb = (Math.round(255 * module.fillAlpha.getValue() * breathe * visible) << 24) | (c.getRGB() & 0xFFFFFF);
                for (AABB box : shape.toAabbs()) {
                    float e = 0.002f;
                    festvisuals$bar(fillBuffer, matrix, fillArgb,
                            (float) (x + box.minX) - e, (float) (y + box.minY) - e, (float) (z + box.minZ) - e,
                            (float) (x + box.maxX) + e, (float) (y + box.maxY) + e, (float) (z + box.maxZ) + e);
                }
            }

            for (AABB box : shape.toAabbs()) {
                float minX = (float) (x + box.minX);
                float minY = (float) (y + box.minY);
                float minZ = (float) (z + box.minZ);
                float maxX = (float) (x + box.maxX);
                float maxY = (float) (y + box.maxY);
                float maxZ = (float) (z + box.maxZ);

                for (float ey : new float[]{minY, maxY}) {
                    for (float ez : new float[]{minZ, maxZ}) {
                        festvisuals$bar(buffer, matrix, argb,
                                minX - half, ey - half, ez - half, maxX + half, ey + half, ez + half);
                    }
                }
                for (float ex : new float[]{minX, maxX}) {
                    for (float ez : new float[]{minZ, maxZ}) {
                        festvisuals$bar(buffer, matrix, argb,
                                ex - half, minY - half, ez - half, ex + half, maxY + half, ez + half);
                    }
                }
                for (float ex : new float[]{minX, maxX}) {
                    for (float ey : new float[]{minY, maxY}) {
                        festvisuals$bar(buffer, matrix, argb,
                                ex - half, ey - half, minZ - half, ex + half, ey + half, maxZ + half);
                    }
                }
            }

            RenderUtil.WORLD.endRender(poseStack); RenderUtil.WORLD.finishFrame();
        }
    }

    /** One edge of the outline: an axis-aligned box emitted as six quads. */
    @Unique
    private void festvisuals$bar(VertexConsumer buffer, Matrix4f matrix, int color,
                                 float x1, float y1, float z1, float x2, float y2, float z2) {
        festvisuals$quad(buffer, matrix, color, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1);
        festvisuals$quad(buffer, matrix, color, x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2);
        festvisuals$quad(buffer, matrix, color, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1);
        festvisuals$quad(buffer, matrix, color, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2);
        festvisuals$quad(buffer, matrix, color, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1);
        festvisuals$quad(buffer, matrix, color, x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2);
    }

    @Unique
    private void festvisuals$quad(VertexConsumer buffer, Matrix4f matrix, int color,
                      float ax, float ay, float az, float bx, float by, float bz,
                      float cx, float cy, float cz, float dx, float dy, float dz) {
        buffer.addVertex(matrix, ax, ay, az).setColor(color);
        buffer.addVertex(matrix, bx, by, bz).setColor(color);
        buffer.addVertex(matrix, cx, cy, cz).setColor(color);
        buffer.addVertex(matrix, dx, dy, dz).setColor(color);
    }
}
