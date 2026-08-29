package com.fest.visuals.client.features.modules.render;

import lombok.Getter;
import net.minecraft.client.CameraType;
import lombok.Setter;
import net.minecraft.world.level.block.*;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import com.fest.visuals.api.event.EventListener;
import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.player.other.UpdateEvent;
import com.fest.visuals.api.event.events.render.Render3DEvent;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.SliderSetting;
import com.fest.visuals.api.utils.animation.AnimationUtil;
import com.fest.visuals.api.utils.animation.Easing;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.color.UIColors;
import com.fest.visuals.api.utils.math.MathUtil;
import com.fest.visuals.api.utils.math.TimerUtil;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.system.files.FileUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@ModuleRegister(name = "Trails", desc = "Оставляет след за игроками", category = Category.RENDER)
public class TrailsModule extends Module {
    @Getter private static final TrailsModule instance = new TrailsModule();

    private final SliderSetting length = new SliderSetting("Length").value(1500f).range(500f, 3000f).step(100f);
    private final SliderSetting size = new SliderSetting("Size").value(0.2f).range(0.05f, 0.3f).step(0.01f);
    private final BooleanSetting renderInFirstPerson = new BooleanSetting("In first person").value(false);
    private final BooleanSetting physics = new BooleanSetting("Physics").value(true);
    private final SliderSetting fadeTime = new SliderSetting("Fade Time").value(250f).range(100f, 1000f).step(50f);

    private final List<TrailParticle> particles = new ArrayList<>();
    private final Identifier bloomTexture = FileUtil.getImage("particles/glow");

    // ета кагуне как у канеки курва
    public TrailsModule() {
        addSettings(length, size, renderInFirstPerson, physics, fadeTime);
    }

    @Override
    public void onEnable() {
        particles.clear();
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            particles.removeIf(particle -> particle.shouldRemove(length.getValue().intValue()));

            boolean isFirstPerson = mc.options.getCameraType() == CameraType.FIRST_PERSON;

            if (isFirstPerson && !renderInFirstPerson.getValue()) {
                return;
            }

            Vec3 playerPos = mc.player.position();
            particles.add(new TrailParticle(
                    new Vec3(playerPos.x, playerPos.y + mc.player.getBbHeight() * (isFirstPerson ? 0.2 : 0.5), playerPos.z),
                    particles.size()
            ));
        }));

        EventListener renderEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            PoseStack matrixStack = event.matrixStack();

            drawTrail(matrixStack, physics.getValue(), size.getValue(), fadeTime.getValue().intValue(), length.getValue().intValue(), particles);
        }));

        addEvents(updateEvent, renderEvent);
    }

    private void drawTrail(PoseStack matrixStack,
                          boolean physics, float size, int fadeTime, int length,
                          List<TrailParticle> particles
    ) {
        int index = 0;

        for (TrailParticle particle : particles) {
            particle.update(physics);
            if (index > 0) {
                TrailParticle prevParticle = particles.get(index - 1);
                Vec3 prevPos = prevParticle.getPosition();
                Vec3 currentPos = particle.getPosition();

                float smoothFactor = 0.2f;
                Vec3 smoothedPos = new Vec3(
                        MathUtil.interpolate(prevPos.x, currentPos.x, smoothFactor),
                        MathUtil.interpolate(prevPos.y, currentPos.y, smoothFactor),
                        MathUtil.interpolate(prevPos.z, currentPos.z, smoothFactor)
                );
                prevParticle.setPosition(smoothedPos);
            }

            RenderUtil.WORLD.startRender(matrixStack);
            renderParticle(matrixStack, particle, size, fadeTime, length, particles);
            RenderUtil.WORLD.endRender(matrixStack);

            index++;
        }
    }

    private void renderParticle(PoseStack matrixStack, TrailParticle particle, float size, int fadeTime, int length, List<TrailParticle> particles) {
        particle.handleAlphaTransitions(fadeTime, length);
        Color color = ColorUtil.setAlpha(UIColors.gradient(particle.getIndex() * 30), (int) particle.getAlpha());

        Vec3 pos = particle.getPosition();

        float bloomSize = size;
        if (particles.indexOf(particle) > 0) {
            TrailParticle prev = particles.get(particles.indexOf(particle) - 1);
            double distance = pos.distanceTo(prev.getPosition());
            bloomSize = (float) Math.max(size, Math.min(size * 3.3, distance * 4));
        }

        Matrix4f matrix = matrixStack.last().pose();
        Camera gameRendererCamera = mc.gameRenderer.mainCamera();
        Vec3 renderCamera = mc.getEntityRenderDispatcher().camera.position();


        matrixStack.translate(pos.x - renderCamera.x, pos.y - renderCamera.y, pos.z - renderCamera.z);
        matrixStack.mulPose(Axis.YP.rotationDegrees(-gameRendererCamera.yRot()));
        matrixStack.mulPose(Axis.XP.rotationDegrees(gameRendererCamera.xRot()));

        VertexConsumer bufferBuilder = RenderUtil.WORLD.textured(bloomTexture);
        bufferBuilder.addVertex(matrix, bloomSize, -bloomSize, 0f).setUv(0f, 1f).setColor(color.getRGB());
        bufferBuilder.addVertex(matrix, -bloomSize, -bloomSize, 0f).setUv(1f, 1f).setColor(color.getRGB());
        bufferBuilder.addVertex(matrix, -bloomSize, bloomSize, 0f).setUv(1f, 0f).setColor(color.getRGB());
        bufferBuilder.addVertex(matrix, bloomSize, bloomSize, 0f).setUv(0f, 0f).setColor(color.getRGB());

    }

    @Getter
    @Setter
    public static class TrailParticle {
        private Vec3 position;
        private Vec3 velocity;
        private final int index;
        private final TimerUtil timer = new TimerUtil();
        private final AnimationUtil alphaAnimation = new AnimationUtil();
        private float alpha = 255f;

        public TrailParticle(Vec3 position, int index) {
            this.position = position;
            this.velocity = new Vec3(
                    MathUtil.randomInRange(-0.01, 0.01),
                    MathUtil.randomInRange(-0.01, 0.01),
                    MathUtil.randomInRange(-0.01, 0.01)
            );
            this.index = index;
        }

        public void handleAlphaTransitions(int fadeTime, int maxLife) {
            alphaAnimation.update();
            float currentAlpha = (float) alphaAnimation.getValue();

            if (currentAlpha <= 0.0 && !timer.finished(fadeTime)) {
                alphaAnimation.run(255.0, fadeTime, Easing.LINEAR);
            }

            if (currentAlpha >= 255.0 && timer.finished(maxLife - fadeTime)) {
                alphaAnimation.run(0.0, fadeTime, Easing.LINEAR);
            }

            alpha = (float) alphaAnimation.getValue();
        }

        public boolean shouldRemove(int maxLife) {
            double distance = position.distanceTo(mc.player.position());
            boolean expired = timer.finished(maxLife) && alpha <= 0.0;

            return distance >= 80 || expired;
        }

        public void update(boolean enablePhysics) {
            if (enablePhysics) {
                applyPhysics();
            } else {
                updateWithoutPhysics();
            }
        }

        private void applyPhysics() {
            if (isSolidBlock(position.x, position.y, position.z + velocity.z)) {
                velocity = new Vec3(velocity.x, velocity.y, -velocity.z * 0.8);
            }

            if (isSolidBlock(position.x, position.y + velocity.y, position.z)) {
                velocity = new Vec3(velocity.x * 0.999, -velocity.y * 0.7, velocity.z * 0.999);
            }

            if (isSolidBlock(position.x + velocity.x, position.y, position.z)) {
                velocity = new Vec3(-velocity.x * 0.8, velocity.y, velocity.z);
            }

            updateWithoutPhysics();
        }

        private void updateWithoutPhysics() {
            position = position.add(velocity);
            velocity = velocity.scale(0.999);
        }

        private boolean isSolidBlock(double x, double y, double z) {
            BlockPos pos = BlockPos.containing(x, y, z);
            BlockState state = mc.level.getBlockState(pos);
            Block block = state.getBlock();
            return isValidBlock(block);
        }

        private boolean isValidBlock(Block block) {
            return !(block instanceof AirBlock)
                    && !(block instanceof ButtonBlock)
                    && !(block instanceof TorchBlock)
                    && !(block instanceof LeverBlock)
                    && !(block instanceof BasePressurePlateBlock)
                    && !(block instanceof CarpetBlock)
                    && !(block instanceof LiquidBlock);
        }
    }
}
