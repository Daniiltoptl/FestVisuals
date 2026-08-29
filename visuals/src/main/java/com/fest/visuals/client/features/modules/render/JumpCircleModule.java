package com.fest.visuals.client.features.modules.render;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.renderer.*;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import com.fest.visuals.api.event.EventListener;
import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.player.move.JumpEvent;
import com.fest.visuals.api.event.events.player.other.UpdateEvent;
import com.fest.visuals.api.event.events.render.Render3DEvent;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.ModeSetting;
import com.fest.visuals.api.module.setting.SliderSetting;
import com.fest.visuals.api.system.files.FileUtil;
import com.fest.visuals.api.utils.animation.AnimationUtil;
import com.fest.visuals.api.utils.animation.Easing;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.color.UIColors;
import com.fest.visuals.api.utils.math.TimerUtil;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@ModuleRegister(name = "Jump Circle", desc = "Круг под ногами при прыжке", category = Category.RENDER)
public class JumpCircleModule extends Module {
    @Getter private static final JumpCircleModule instance = new JumpCircleModule();

    private final ModeSetting texture = new ModeSetting("Texture").value("Glow").values("Lean", "Glow");
    private final ModeSetting outAnimation = new ModeSetting("Out animation").value("None").values("In", "None");
    private final SliderSetting size = new SliderSetting("Size").value(2f).range(0.1f, 3f).step(0.1f);
    private final SliderSetting lifeTime = new SliderSetting("Life time").value(10f).range(1f, 30f).step(1f);
    private final SliderSetting spawnDur = new SliderSetting("Spawn duration").value(6f).range(1f, 30f).step(1f);
    private final SliderSetting dyingDur = new SliderSetting("Dying duration").value(4f).range(1f, 30f).step(1f);

    private final List<Circle> circles = new ArrayList<>();

    public JumpCircleModule() {
        addSettings(texture, outAnimation, size, lifeTime, spawnDur, dyingDur);
    }

    private String texture() {
        return "circle/" + (texture.is("Lean") ? "lean" : "glow_fat");
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            circles.removeIf(Circle::update);
        }));

        EventListener renderEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            PoseStack matrixStack = event.matrixStack();
            RenderUtil.WORLD.startRender(matrixStack);


            for (Circle circle : circles) {
                circle.render(matrixStack);
            }

            RenderUtil.WORLD.endRender(matrixStack);
        }));

        EventListener jumpEvent = JumpEvent.getInstance().subscribe(new Listener<>(event -> {
            circles.add(new Circle(
                    mc.player.position().add(0, 0.150, 0),
                    size.getValue(),
                    circles.size() * 9,
                    lifeTime.getValue().intValue() * 50,
                    spawnDur.getValue().intValue() * 50,
                    dyingDur.getValue().intValue() * 50,
                    outAnimation.getValue()
            ));
        }));

        addEvents(updateEvent, renderEvent, jumpEvent);
    }

    @RequiredArgsConstructor
    private static class Circle {
        private final Vec3 position;
        private final float size;
        private final int index;
        private final int lifeTime;
        private final int spawnDur;
        private final int dyingDur;
        private final String animMode;

        private final TimerUtil timerUtil = new TimerUtil();
        private final AnimationUtil animation = new AnimationUtil();
        private final AnimationUtil sizeAnimation = new AnimationUtil();
        private boolean isBack = false;

        public boolean update() {
            if (timerUtil.finished((spawnDur + lifeTime))) {
                isBack = true;
            }

            return animation.getValue() <= 0.1 && isBack;
        }

        public void render(PoseStack matrixStack) {
            animation.update();
            sizeAnimation.update();

            sizeAnimation.run(
                    isBack ? (animMode.contains("None") ? 1.0 : 0.0) : 1.0,
                    isBack ? dyingDur : spawnDur,
                    Easing.SINE_OUT
            );
            animation.run(
                    isBack ? 0.0 : 1.0,
                    isBack ? dyingDur : spawnDur,
                    Easing.SINE_OUT
            );

            float anim = (float) animation.getValue();
            int alpha = (int) (anim * 255);
            float scale = (float) (sizeAnimation.getValue() * size);

            Color color1 = ColorUtil.setAlpha(UIColors.gradient(index), alpha);
            Color color2 = ColorUtil.setAlpha(UIColors.gradient(index + 90), alpha);
            Color color3 = ColorUtil.setAlpha(UIColors.gradient(index + 180), alpha);
            Color color4 = ColorUtil.setAlpha(UIColors.gradient(index + 240), alpha);

            VertexConsumer buffer = RenderUtil.WORLD.textured(FileUtil.getImage(JumpCircleModule.getInstance().texture()));
            matrixStack.pushPose();
            matrixStack.translate(
                    position.x - mc.getEntityRenderDispatcher().camera.position().x(),
                    position.y - mc.getEntityRenderDispatcher().camera.position().y(),
                    position.z - mc.getEntityRenderDispatcher().camera.position().z()
            );
            matrixStack.mulPose(Axis.XP.rotationDegrees(90));
            matrixStack.mulPose(Axis.ZP.rotationDegrees(timerUtil.getElapsedTime()));
            Matrix4f matrix = matrixStack.last().pose();

            buffer.addVertex(matrix, scale, -scale, 0).setUv(0, 1f).setColor(color1.getRGB());
            buffer.addVertex(matrix, -scale, -scale, 0).setUv(1f, 1f).setColor(color4.getRGB());
            buffer.addVertex(matrix, -scale, scale, 0).setUv(1f, 0).setColor(color3.getRGB());
            buffer.addVertex(matrix, scale, scale, 0).setUv(0, 0).setColor(color2.getRGB());


            matrixStack.popPose();
        }
    }
}
