package com.fest.visuals.api.utils.render.pipeline;


import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import com.fest.visuals.api.system.interfaces.QuickImports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Optional;
import java.util.Stack;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import net.minecraft.client.renderer.DynamicUniformStorage;

public final class FestRenderer implements QuickImports {
    private static final FestRenderer INSTANCE = new FestRenderer();

    /**
     * Separate queue for anything that has to sit *under* the vanilla GUI, such as a widget
     * background whose foreground is drawn through DrawContext. It owns its own allocator so it
     * can be flushed on its own without invalidating the meshes still queued on the main one.
     */
    private static final FestRenderer BACKDROP = new FestRenderer();

    private static FestRenderer active = INSTANCE;

    private static final int MIN_BUFFER_SIZE = 4096;

    private final ByteBufferBuilder allocator = new ByteBufferBuilder(786432);
    private final List<Command> commands = new ArrayList<>();
    private final Map<VertexFormat, VertexStorage> storages = new HashMap<>();
    private final PoseStack matrices = new PoseStack();

    private @Nullable DynamicUniformStorage<FestUniform> uniforms;
    private @Nullable CachedOrthoProjectionMatrixBuffer projection;
    private final Stack<ScissorBox> scissorStack = new Stack<>();

    public static FestRenderer getInstance() {
        return active;
    }

    /**
     * Routes everything the action draws into the backdrop queue. Used by widgets that place
     * DrawContext content (item icons) on top of their own background: the GUI is painted
     * between the two flushes, so the background cannot be queued on the main renderer.
     */
    public static void withBackdrop(Runnable action) {
        active = BACKDROP;
        try {
            action.run();
        } finally {
            active = INSTANCE;
        }
    }

    /** Drawn after the blur targets are built but before the vanilla GUI paints over them. */
    public static void flushBackdrop() {
        BACKDROP.flush();
    }

    public PoseStack matrices() {
        return matrices;
    }

    public BufferBuilder begin(RenderPipeline pipeline) {
        return new BufferBuilder(allocator, pipeline.getVertexFormatMode(), pipeline.getVertexFormat());
    }

    public void submit(BufferBuilder builder, RenderPipeline pipeline, TextureSetup texture, FestUniform uniform) {
        MeshData built = builder.build();
        if (built == null) return;
        commands.add(new Command(built, pipeline, texture, uniform, scissorStack.isEmpty() ? null : scissorStack.peek()));
    }

    public void submit(BufferBuilder builder, RenderPipeline pipeline, FestUniform uniform) {
        submit(builder, pipeline, TextureSetup.noTexture(), uniform);
    }

    public GpuBufferSlice writeUniform(FestUniform uniform) {
        if (uniforms == null) uniforms = new DynamicUniformStorage<>("FestVisuals Params", FestUniform.SIZE, 512);
        return uniforms.writeUniform(uniform);
    }

    public void fullscreen(RenderPipeline pipeline, RenderTarget target, GpuTextureView source, GpuSampler sampler, FestUniform uniform) {
        GpuBufferSlice slice = writeUniform(uniform);

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "FestVisuals post", target.getColorTextureView(), OptionalInt.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform(FestPipelines.UBO, slice);
            pass.bindTexture("InSampler", source, sampler);
            pass.draw(0, 3);
        }
    }

            public void pushScissor(float x, float y, float width, float height) {
        float scale = (float) mc.getWindow().getGuiScale();
        int left = (int) (x * scale);
        int bottom = (int) ((mc.getWindow().getGuiScaledHeight() - y - height) * scale);
        int w = (int) (width * scale);
        int h = (int) (height * scale);
        if (!scissorStack.isEmpty()) {
            ScissorBox parent = scissorStack.peek();
            int pRight = parent.x() + parent.width();
            int pTop = parent.y() + parent.height();
            int right = Math.min(left + w, pRight);
            int top = Math.min(bottom + h, pTop);
            left = Math.max(left, parent.x());
            bottom = Math.max(bottom, parent.y());
            w = Math.max(0, right - left);
            h = Math.max(0, top - bottom);
        }
        scissorStack.push(new ScissorBox(left, bottom, w, h));
    }

        public void popScissor() {
        if (!scissorStack.isEmpty()) scissorStack.pop();
    }

    /**
     * Drops any clip or matrix a caller failed to pop. Both stacks live across frames, so one
     * missed pop would otherwise clip or displace everything drawn from then on. Screens that own
     * a whole frame call this before they start drawing.
     */
    public void resetState() {
        scissorStack.clear();
        for (int i = 0; i < 64 && !matrices.isEmpty(); i++) matrices.popPose();
        matrices.setIdentity();
    }

    public void flush() {
        if (commands.isEmpty()) {
            if (uniforms != null) uniforms.endFrame();
            allocator.clear();
            return;
        }

        Window window = mc.getWindow();
        RenderTarget framebuffer = mc.getMainRenderTarget();
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

        if (projection == null) projection = new CachedOrthoProjectionMatrixBuffer("festvisuals", 1000.0f, 21000.0f, true);
        if (uniforms == null) uniforms = new DynamicUniformStorage<>("FestVisuals Params", FestUniform.SIZE, 512);

        int[] baseVertices = uploadVertices(encoder);

        int maxIndices = 0;
        for (Command command : commands) maxIndices = Math.max(maxIndices, command.built.drawState().indexCount());

        RenderSystem.AutoStorageIndexBuffer shape = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer indexBuffer = shape.getBuffer(maxIndices);
        VertexFormat.IndexType indexType = shape.type();

        RenderSystem.backupProjectionMatrix();
        float guiWidth = (float) window.getWidth() / (float) window.getGuiScale();
        float guiHeight = (float) window.getHeight() / (float) window.getGuiScale();
        RenderSystem.setProjectionMatrix(projection.getBuffer(guiWidth, guiHeight), ProjectionType.ORTHOGRAPHIC);
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f().setTranslation(0f, 0f, -11000f), new Vector4f(1f, 1f, 1f, 1f), new Vector3f(), new Matrix4f());

        GpuBufferSlice[] slices = new GpuBufferSlice[commands.size()];
        for (int i = 0; i < commands.size(); i++) slices[i] = uniforms.writeUniform(commands.get(i).uniform);

        try (RenderPass pass = encoder.createRenderPass(() -> "FestVisuals GUI", framebuffer.getColorTextureView(), OptionalInt.empty(), framebuffer.useDepth ? framebuffer.getDepthTextureView() : null, OptionalDouble.empty())) {
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);

            for (int i = 0; i < commands.size(); i++) {
                Command command = commands.get(i);

                pass.setPipeline(command.pipeline);
                pass.setVertexBuffer(0, storages.get(command.pipeline.getVertexFormat()).buffer);
                pass.setUniform(FestPipelines.UBO, slices[i]);

                if (command.scissor != null) {
                    int left = Math.max(0, command.scissor.x);
                    int bottom = Math.max(0, command.scissor.y);
                    int right = Math.min(framebuffer.width, command.scissor.x + command.scissor.width);
                    int top = Math.min(framebuffer.height, command.scissor.y + command.scissor.height);
                    if (right <= left || top <= bottom) continue;
                    pass.enableScissor(left, bottom, right - left, top - bottom);
                } else {
                    pass.disableScissor();
                }

                if (command.texture.texure0() != null) pass.bindTexture("Sampler0", command.texture.texure0(), command.texture.sampler0());

                pass.setIndexBuffer(indexBuffer, indexType);
                pass.drawIndexed(baseVertices[i], 0, command.built.drawState().indexCount(), 1);
            }
        }

        RenderSystem.restoreProjectionMatrix();
        uniforms.endFrame();
        for (Command command : commands) command.built.close();
        commands.clear();
        allocator.clear();
    }

    private int[] uploadVertices(CommandEncoder encoder) {
        Map<VertexFormat, Integer> sizes = new HashMap<>();
        int[] offsets = new int[commands.size()];
        int[] baseVertices = new int[commands.size()];

        for (int i = 0; i < commands.size(); i++) {
            Command command = commands.get(i);
            VertexFormat format = command.pipeline.getVertexFormat();
            int offset = sizes.getOrDefault(format, 0);

            offsets[i] = offset;
            baseVertices[i] = offset / format.getVertexSize();
            sizes.put(format, offset + command.built.drawState().vertexCount() * format.getVertexSize());
        }

        sizes.forEach((format, size) -> storages.computeIfAbsent(format, VertexStorage::new).ensureCapacity(size));

        for (int i = 0; i < commands.size(); i++) {
            Command command = commands.get(i);
            VertexFormat format = command.pipeline.getVertexFormat();
            int size = command.built.drawState().vertexCount() * format.getVertexSize();
            encoder.writeToBuffer(storages.get(format).buffer.slice(offsets[i], size), command.built.vertexBuffer());
        }

        return baseVertices;
    }

    private static final class VertexStorage {
        private final VertexFormat format;
        private GpuBuffer buffer;
        private int capacity;

        private VertexStorage(VertexFormat format) {
            this.format = format;
        }

        private void ensureCapacity(int size) {
            if (buffer != null && capacity >= size) return;
            if (buffer != null) buffer.close();
            capacity = Math.max(size, MIN_BUFFER_SIZE);
            buffer = RenderSystem.getDevice().createBuffer(() -> "FestVisuals vertices " + format, GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, capacity);
        }
    }

    private record Command(MeshData built, RenderPipeline pipeline, TextureSetup texture, FestUniform uniform, @Nullable ScissorBox scissor) { }

    private record ScissorBox(int x, int y, int width, int height) { }
}
