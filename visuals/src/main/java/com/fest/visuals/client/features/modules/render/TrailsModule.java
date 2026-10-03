package com.fest.visuals.client.features.modules.render;

import lombok.Getter;
import net.minecraft.client.CameraType;
import lombok.Setter;
import net.minecraft.world.level.block.*;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
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
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@ModuleRegister(name = "Trails", desc = "Оставляет след за игроками", category = Category.RENDER)
public class TrailsModule extends Module {
    @Getter private static final TrailsModule instance = new TrailsModule();

    private final SliderSetting length = new SliderSetting("Длина следа").value(1500f).range(500f, 3000f).step(100f);
    private final SliderSetting size = new SliderSetting("Размер").value(0.2f).range(0.05f, 0.3f).step(0.01f);
    private final SliderSetting stretch = new SliderSetting("Высота").value(3.5f).range(0.5f, 8f).step(0.1f);
    private final SliderSetting narrow = new SliderSetting("Ширина").value(0.18f).range(0.05f, 1f).step(0.01f);
    private final SliderSetting brightness = new SliderSetting("Яркость").value(0.5f).range(0.1f, 1f).step(0.05f);
    private final BooleanSetting throughWalls = new BooleanSetting("Сквозь блоки").value(false);
    private final BooleanSetting renderInFirstPerson = new BooleanSetting("От первого лица").value(false);
    private final BooleanSetting physics = new BooleanSetting("Физика").value(true);
    private final SliderSetting fadeTime = new SliderSetting("Затухание").value(250f).range(100f, 1000f).step(50f);

    private final List<TrailParticle> particles = new ArrayList<>();

    /** Where the last mark was dropped, so a standing player does not pile a stack of them up. */
    private Vec3 lastSpawn;

    // ета кагуне как у канеки курва
    public TrailsModule() {
        addSettings(length, size, stretch, narrow, brightness, throughWalls,
                renderInFirstPerson, physics, fadeTime);
    }

    @Override
    public void onEnable() {
        particles.clear();
        lastSpawn = null;
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

            // Standing still used to drop a mark every tick, which stacked into a bright column.
            if (lastSpawn != null && lastSpawn.distanceToSqr(playerPos) < 0.0025) return;
            lastSpawn = playerPos;

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

    

    private void drawTrail(PoseStack matrixStack, boolean physics, float size, int fadeTime, int length, List<TrailParticle> particles) {
        if (particles.size() < 2) return;

        Vec3 renderCamera = mc.getEntityRenderDispatcher().camera.position();
        VertexConsumer bufferBuilder = throughWalls.getValue()
                ? RenderUtil.WORLD.xrayQuads()
                : RenderUtil.WORLD.occludedQuads();

        RenderUtil.WORLD.startRender(matrixStack);
        
        Matrix4f matrix = matrixStack.last().pose();
        
        // Update particles first
        for (TrailParticle particle : particles) {
            particle.update(physics);
            particle.handleAlphaTransitions(fadeTime, length);
        }
        
        for (int i = 1; i < particles.size(); i++) {
            TrailParticle particle = particles.get(i);
            TrailParticle prevParticle = particles.get(i - 1);
            
            Color color = ColorUtil.setAlpha(UIColors.gradient(particle.getIndex() * 30), (int) (particle.getAlpha() * brightness.getValue()));
            Color prevColor = ColorUtil.setAlpha(UIColors.gradient(prevParticle.getIndex() * 30), (int) (prevParticle.getAlpha() * brightness.getValue()));
            
            int argb = color.getRGB();
            int prevArgb = prevColor.getRGB();

            Vec3 pos = new Vec3(particle.x, particle.y, particle.z).subtract(renderCamera);
            Vec3 pPos = new Vec3(prevParticle.x, prevParticle.y, prevParticle.z).subtract(renderCamera);

            float halfHeight = size * stretch.getValue(); 
            float halfWidth = size * narrow.getValue();

            Vec3 dir = pos.subtract(pPos);
            double dirLen = dir.length();
            Vec3 perp = new Vec3(0, 0, 0);
            if (dirLen > 0.0001) {
                perp = new Vec3(-dir.z, 0, dir.x).normalize().scale(halfWidth);
            }

            // Horizontal ribbon
            bufferBuilder.addVertex(matrix, (float)(pos.x + perp.x), (float)(pos.y), (float)(pos.z + perp.z)).setColor(argb);
            bufferBuilder.addVertex(matrix, (float)(pos.x - perp.x), (float)(pos.y), (float)(pos.z - perp.z)).setColor(argb);
            bufferBuilder.addVertex(matrix, (float)(pPos.x - perp.x), (float)(pPos.y), (float)(pPos.z - perp.z)).setColor(prevArgb);
            bufferBuilder.addVertex(matrix, (float)(pPos.x + perp.x), (float)(pPos.y), (float)(pPos.z + perp.z)).setColor(prevArgb);

            // Vertical ribbon
            bufferBuilder.addVertex(matrix, (float)pos.x, (float)(pos.y + halfHeight), (float)pos.z).setColor(argb);
            bufferBuilder.addVertex(matrix, (float)pos.x, (float)(pos.y - halfHeight), (float)pos.z).setColor(argb);
            bufferBuilder.addVertex(matrix, (float)pPos.x, (float)(pPos.y - halfHeight), (float)pPos.z).setColor(prevArgb);
            bufferBuilder.addVertex(matrix, (float)pPos.x, (float)(pPos.y + halfHeight), (float)pPos.z).setColor(prevArgb);
        }
        
        RenderUtil.WORLD.endRender(matrixStack);
    }

    @Getter
    @Setter
    public static class TrailParticle {
        public double x, y, z;
        public double vx, vy, vz;
        private final int index;
        private final long startTime;
        private float alpha = 255f;

        public TrailParticle(Vec3 position, int index) {
            this.x = position.x;
            this.y = position.y;
            this.z = position.z;
            this.vx = MathUtil.randomInRange(-0.01, 0.01);
            this.vy = MathUtil.randomInRange(-0.01, 0.01);
            this.vz = MathUtil.randomInRange(-0.01, 0.01);
            this.index = index;
            this.startTime = System.currentTimeMillis();
        }

        public void handleAlphaTransitions(int fadeTime, int maxLife) {
            long age = System.currentTimeMillis() - startTime;
            if (age > maxLife - fadeTime) {
                float progress = (float)(age - (maxLife - fadeTime)) / fadeTime;
                alpha = Math.max(0, 255f * (1f - progress));
            }
        }

        public void update(boolean enablePhysics) {
            if (enablePhysics) applyPhysics();
            else updateWithoutPhysics();
        }

        private static final net.minecraft.core.BlockPos.MutableBlockPos MUTABLE_POS = new net.minecraft.core.BlockPos.MutableBlockPos();

        private void applyPhysics() {
            if (isSolidBlock(x, y, z + vz)) vz = -vz * 0.8;
            if (isSolidBlock(x, y + vy, z)) {
                vx *= 0.999;
                vy = -vy * 0.7;
                vz *= 0.999;
            }
            if (isSolidBlock(x + vx, y, z)) vx = -vx * 0.8;

            vy -= 0.005; // gravity
            vx *= 0.98; // friction
            vy *= 0.98;
            vz *= 0.98;

            x += vx;
            y += vy;
            z += vz;
        }

        private void updateWithoutPhysics() {
            vy -= 0.005;
            vx *= 0.98;
            vy *= 0.98;
            vz *= 0.98;
            x += vx;
            y += vy;
            z += vz;
        }

        private boolean isSolidBlock(double bx, double by, double bz) {
            MUTABLE_POS.set(bx, by, bz);
            net.minecraft.world.level.block.state.BlockState state = mc.level.getBlockState(MUTABLE_POS);
            return isValidBlock(state.getBlock());
        }

        private boolean isValidBlock(net.minecraft.world.level.block.Block block) {
            return block != net.minecraft.world.level.block.Blocks.AIR && 
                   block != net.minecraft.world.level.block.Blocks.WATER && 
                   block != net.minecraft.world.level.block.Blocks.LAVA;
        }

        public int getIndex() { return index; }
        public float getAlpha() { return alpha; }
        public boolean shouldRemove(int maxLife) { return System.currentTimeMillis() - startTime > maxLife; }
    }
}
