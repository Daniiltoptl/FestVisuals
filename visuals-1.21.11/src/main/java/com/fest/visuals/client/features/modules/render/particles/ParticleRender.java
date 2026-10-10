package com.fest.visuals.client.features.modules.render.particles;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import com.fest.visuals.api.system.files.FileUtil;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.system.interfaces.QuickImports;
import com.fest.visuals.api.utils.animation.AnimationUtil;
import com.fest.visuals.api.utils.animation.Easing;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.color.UIColors;
import com.fest.visuals.api.utils.math.MathUtil;
import com.fest.visuals.api.utils.math.TimerUtil;
import com.fest.visuals.api.utils.player.PlayerUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.awt.*;
import java.util.ArrayDeque;
import java.util.Deque;

@Setter
@Getter
@Accessors(fluent = true, chain = true)
public class ParticleRender implements QuickImports {
    private float prevX, prevY, prevZ;
    private float x, y, z;
    private float motionX, motionY, motionZ;
    private int lifeTime;
    private int maxLife;
    private float prevSize = 0f;
    private float rotation, prevRotation = 0f;
    private float rotateSpeed = 20f, size;
    private int index;
    private Identifier identifier;
    private boolean dropPhysics, rotating;

    private float spawnDuration, dyingDuration;

    private final TimerUtil timerUtil = new TimerUtil();
    private final AnimationUtil alphaAnimation = new AnimationUtil();
    private boolean gravityFalls = false;

    private boolean trail;
    private float trailLength = 5f;
    private boolean dyingEffect;
    private final Deque<Vec3> trailPoints = new ArrayDeque<>();

    public ParticleRender(float x, float y, float z, int lifeTime) {
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
        this.x = x;
        this.y = y;
        this.z = z;
        this.maxLife = MathUtil.randomInRange(Math.max(lifeTime / 2, 0), lifeTime);
        this.rotation = MathUtil.randomInRange(-180f, 180f);
    }

    public static String[] textures = new String[]{
            "Spark", "Star", "Heart", "Dollar", "Snowflake", "Glow", "Ball",
    };

    public static Identifier getTexture(String mode) {
        return switch (mode) {
            case "Spark" -> FileUtil.getImage("particles/spark_" + MathUtil.randomInRange(1, 4));
            default -> FileUtil.getImage("particles/" + mode.toLowerCase());
        };
    }

    public boolean update() {
        float gravity = gravityFalls ? (float) alphaAnimation.getValue() * 0.3f : 1f;

        prevX = x;
        prevY = y;
        prevZ = z;

        x += motionX;
        y += motionY * gravity;
        z += motionZ;

        double speed = Math.sqrt((motionX * motionX + motionZ * motionZ));
        float halfSize = prevSize;

        if (posBlock(x, y - halfSize - 0.05f, z)) {
            motionY = -motionY / 1.1f;
            motionX /= 1.1f;
            motionZ /= 1.1f;
        } else {
            if (posBlock(x - (float) speed - halfSize, y, z - (float) speed - halfSize) ||
                    posBlock(x + (float) speed + halfSize, y, z + (float) speed + halfSize) ||
                    posBlock(x + (float) speed + halfSize, y, z - (float) speed - halfSize) ||
                    posBlock(x - (float) speed - halfSize, y, z + (float) speed + halfSize) ||
                    posBlock(x + (float) speed + halfSize, y, z) ||
                    posBlock(x - (float) speed - halfSize, y, z) ||
                    posBlock(x, y, z + (float) speed + halfSize) ||
                    posBlock(x, y, z - (float) speed - halfSize)) {
                motionX = -motionX;
                motionZ = -motionZ;
                maxLife--;
            } else if (dropPhysics) {
                motionY -= 0.02f;
            }
        }

        prevRotation = rotation;
        rotation -= (prevRotation > 0) ? -rotateSpeed : rotateSpeed;

        if (!gravityFalls) {
            float scale = 1.1f;
            motionX /= scale;
            motionY /= scale;
            motionZ /= scale;
        }

        if (trail) {
            trailPoints.addFirst(new Vec3(x, y, z));
            while (trailPoints.size() > trailLength) trailPoints.removeLast();
        }

        return mc.player.position().distanceTo(new Vec3(x, y, z)) >= 80 ||
                alphaAnimation.getValue() <= 0.0 && timerUtil.finished((spawnDuration + dyingDuration + maxLife) * 50);
    }

    private float alphaPC() {
        return (float) alphaAnimation.getValue();
    }

    private int alpha() {
        return (int) (255 * alphaPC());
    }

    public void updateAlpha() {
        alphaAnimation.update();

        float alphaAnim = alphaPC();

        if (alphaAnim <= 0.0 && !timerUtil.finished(spawnDuration * 50))
            alphaAnimation.run(1.0, (long) (spawnDuration * 50), Easing.QUINT_OUT);

        if (alphaAnim >= 1.0 && timerUtil.finished((spawnDuration + maxLife) * 50))
            alphaAnimation.run(0.0, (long) (dyingDuration * 50), Easing.QUINT_OUT);
    }

    public void render(PoseStack matrixStack) {
        if (!PlayerUtil.canSee(new Vec3(x, y, z))) return;

        Matrix4f matrix4f = matrixStack.last().pose();
        Camera camera = mc.gameRenderer.getMainCamera();
        Color primaryColor = ColorUtil.setAlpha(UIColors.gradient(index * 90), alpha());
        Vec3 interpolatedPos = interpolatePosition(prevX, prevY, prevZ, x, y, z);

        float halfSize = MathUtil.interpolate(prevSize, (size * alphaPC()));
        prevSize = halfSize;

        matrixStack.translate(interpolatedPos.x, interpolatedPos.y, interpolatedPos.z);
        matrixStack.mulPose(Axis.YP.rotationDegrees(-camera.yRot()));
        matrixStack.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
        if (rotating) matrixStack.mulPose(Axis.ZP.rotationDegrees(MathUtil.interpolate(prevRotation, rotation)));

        VertexConsumer buffer = RenderUtil.WORLD.textured(identifier);
        buffer.addVertex(matrix4f, halfSize, -halfSize, 0f).setUv(0f, 1f).setColor(primaryColor.getRGB());
        buffer.addVertex(matrix4f, -halfSize, -halfSize, 0f).setUv(1f, 1f).setColor(primaryColor.getRGB());
        buffer.addVertex(matrix4f, -halfSize, halfSize, 0f).setUv(1f, 0f).setColor(primaryColor.getRGB());
        buffer.addVertex(matrix4f, halfSize, halfSize, 0f).setUv(0f, 0f).setColor(primaryColor.getRGB());
    }

    public void renderTrail(PoseStack matrixStack) {
        if (!trail || trailPoints.size() <= 1) return;
        if (!PlayerUtil.canSee(new Vec3(x, y, z))) return;

        VertexConsumer buf = RenderUtil.WORLD.lines();

        Matrix4f mat = matrixStack.last().pose();
        Vec3 cam = mc.getEntityRenderDispatcher().camera.position();
        Color col = ColorUtil.setAlpha(UIColors.gradient(index * 90), alpha());

        double interpX = MathUtil.interpolate(prevX, x);
        double interpY = MathUtil.interpolate(prevY, y);
        double interpZ = MathUtil.interpolate(prevZ, z);

        Vec3 last = null;
        for (Vec3 p : trailPoints) {
            double smoothX = MathUtil.interpolate(p.x, interpX, 0.05);
            double smoothY = MathUtil.interpolate(p.y, interpY, 0.05);
            double smoothZ = MathUtil.interpolate(p.z, interpZ, 0.05);
            Vec3 smooth = new Vec3(smoothX, smoothY, smoothZ);

            if (last != null) {
                buf.addVertex(mat, (float)(last.x - cam.x), (float)(last.y - cam.y), (float)(last.z - cam.z))
                        .setColor(col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha());
                buf.addVertex(mat, (float)(smooth.x - cam.x), (float)(smooth.y - cam.y), (float)(smooth.z - cam.z))
                        .setColor(col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha());
            }
            last = smooth;
        }
        
    }

    private boolean posBlock(float x, float y, float z) {
        Block block = mc.level != null ? mc.level.getBlockState(BlockPos.containing(x, y, z)).getBlock() : null;
        return block != null &&
                !(block instanceof AirBlock) &&
                block != Blocks.WATER &&
                block != Blocks.LAVA &&
                block != Blocks.SEAGRASS &&
                block != Blocks.TALL_SEAGRASS &&
                block != Blocks.SHORT_GRASS &&
                block != Blocks.TALL_GRASS &&
                block != Blocks.FERN &&
                block != Blocks.DEAD_BUSH &&
                block != Blocks.VINE &&
                block != Blocks.SNOW &&
                block != Blocks.POPPY &&
                block != Blocks.DANDELION &&
                block != Blocks.BROWN_MUSHROOM &&
                block != Blocks.RED_MUSHROOM;
    }

    private Vec3 interpolatePosition(float prevX, float prevY, float prevZ, float currentX, float currentY, float currentZ) {
        Vec3 cameraPos = mc.getEntityRenderDispatcher().camera.position();
        double cameraX = cameraPos.x;
        double cameraY = cameraPos.y;
        double cameraZ = cameraPos.z;

        double interpolatedX = MathUtil.interpolate(prevX, currentX) - cameraX;
        double interpolatedY = MathUtil.interpolate(prevY, currentY) - cameraY;
        double interpolatedZ = MathUtil.interpolate(prevZ, currentZ) - cameraZ;

        return new Vec3(interpolatedX, interpolatedY, interpolatedZ);
    }
}
