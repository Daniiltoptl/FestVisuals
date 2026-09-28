package com.fest.visuals.api.utils.render.display;

import java.awt.Color;

import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import com.fest.visuals.api.system.interfaces.QuickImports;

/**
 * Flat geometry in the world: rings, discs, boxes, ribbons and camera-facing sprites.
 *
 * <p>Everything takes world coordinates and converts them to camera-relative ones itself, and
 * everything is emitted as quads, so any quad layer ({@code occludedQuads}, {@code xrayQuads},
 * a textured one) can take it. Lines are ribbons rather than GL lines: line width is a hint
 * drivers clamp to one pixel.
 */
@UtilityClass
public class WorldShapes implements QuickImports {
    public Vec3 camera() {
        return mc.gameRenderer.mainCamera().position();
    }

    /** Right and up unit vectors of the camera, for sprites that always face the viewer. */
    public Vec3[] cameraAxes() {
        Camera camera = mc.gameRenderer.mainCamera();
        double yaw = Math.toRadians(camera.yRot());
        double pitch = Math.toRadians(camera.xRot());

        Vec3 look = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
        Vec3 up = right.cross(look).normalize();
        return new Vec3[]{right, up};
    }

    public void quad(VertexConsumer buffer, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int ca, int cb, int cc, int cd) {
        Vec3 cam = camera();
        buffer.addVertex(matrix, (float) (a.x - cam.x), (float) (a.y - cam.y), (float) (a.z - cam.z)).setColor(ca);
        buffer.addVertex(matrix, (float) (b.x - cam.x), (float) (b.y - cam.y), (float) (b.z - cam.z)).setColor(cb);
        buffer.addVertex(matrix, (float) (c.x - cam.x), (float) (c.y - cam.y), (float) (c.z - cam.z)).setColor(cc);
        buffer.addVertex(matrix, (float) (d.x - cam.x), (float) (d.y - cam.y), (float) (d.z - cam.z)).setColor(cd);
    }

    public void quad(VertexConsumer buffer, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
        quad(buffer, matrix, a, b, c, d, color, color, color, color);
    }

    /**
     * A flat ring lying on the XZ plane. {@code sweep} of {@code 2π} closes it; less draws an arc
     * starting at {@code start}, which is how the dashed and spinning variants are made.
     */
    public void ring(VertexConsumer buffer, Matrix4f matrix, Vec3 centre, double innerRadius, double outerRadius,
                     double start, double sweep, int segments, int innerColor, int outerColor) {
        for (int i = 0; i < segments; i++) {
            double a0 = start + sweep * i / segments;
            double a1 = start + sweep * (i + 1) / segments;
            double c0 = Math.cos(a0), s0 = Math.sin(a0);
            double c1 = Math.cos(a1), s1 = Math.sin(a1);

            quad(buffer, matrix,
                    centre.add(c0 * innerRadius, 0, s0 * innerRadius),
                    centre.add(c0 * outerRadius, 0, s0 * outerRadius),
                    centre.add(c1 * outerRadius, 0, s1 * outerRadius),
                    centre.add(c1 * innerRadius, 0, s1 * innerRadius),
                    innerColor, outerColor, outerColor, innerColor);
        }
    }

    /** A filled disc on the XZ plane, coloured from the centre out to the rim. */
    public void disc(VertexConsumer buffer, Matrix4f matrix, Vec3 centre, double radius, int segments,
                     int centreColor, int edgeColor) {
        ring(buffer, matrix, centre, 0.0, radius, 0.0, Math.PI * 2.0, segments, centreColor, edgeColor);
    }

    /** A vertical band around the ring's rim: the "wall" that makes a flat radius readable. */
    public void wall(VertexConsumer buffer, Matrix4f matrix, Vec3 centre, double radius, double height,
                     int segments, int bottomColor, int topColor) {
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2.0 * i / segments;
            double a1 = Math.PI * 2.0 * (i + 1) / segments;
            Vec3 p0 = centre.add(Math.cos(a0) * radius, 0, Math.sin(a0) * radius);
            Vec3 p1 = centre.add(Math.cos(a1) * radius, 0, Math.sin(a1) * radius);

            quad(buffer, matrix, p0, p1, p1.add(0, height, 0), p0.add(0, height, 0),
                    bottomColor, bottomColor, topColor, topColor);
        }
    }

    public void boxFill(VertexConsumer buffer, Matrix4f matrix, AABB box, int color) {
        Vec3 a = new Vec3(box.minX, box.minY, box.minZ);
        Vec3 b = new Vec3(box.maxX, box.minY, box.minZ);
        Vec3 c = new Vec3(box.maxX, box.minY, box.maxZ);
        Vec3 d = new Vec3(box.minX, box.minY, box.maxZ);
        Vec3 e = new Vec3(box.minX, box.maxY, box.minZ);
        Vec3 f = new Vec3(box.maxX, box.maxY, box.minZ);
        Vec3 g = new Vec3(box.maxX, box.maxY, box.maxZ);
        Vec3 h = new Vec3(box.minX, box.maxY, box.maxZ);

        quad(buffer, matrix, a, b, c, d, color);
        quad(buffer, matrix, e, h, g, f, color);
        quad(buffer, matrix, a, e, f, b, color);
        quad(buffer, matrix, b, f, g, c, color);
        quad(buffer, matrix, c, g, h, d, color);
        quad(buffer, matrix, d, h, e, a, color);
    }

    /** The twelve edges of a box as thin bars, so the thickness is the same at any distance. */
    public void boxOutline(VertexConsumer buffer, Matrix4f matrix, AABB box, double thickness, int color) {
        double t = thickness / 2.0;
        double x1 = box.minX, y1 = box.minY, z1 = box.minZ;
        double x2 = box.maxX, y2 = box.maxY, z2 = box.maxZ;

        for (double y : new double[]{y1, y2}) {
            boxFill(buffer, matrix, new AABB(x1 - t, y - t, z1 - t, x2 + t, y + t, z1 + t), color);
            boxFill(buffer, matrix, new AABB(x1 - t, y - t, z2 - t, x2 + t, y + t, z2 + t), color);
            boxFill(buffer, matrix, new AABB(x1 - t, y - t, z1 - t, x1 + t, y + t, z2 + t), color);
            boxFill(buffer, matrix, new AABB(x2 - t, y - t, z1 - t, x2 + t, y + t, z2 + t), color);
        }
        for (double x : new double[]{x1, x2}) {
            for (double z : new double[]{z1, z2}) {
                boxFill(buffer, matrix, new AABB(x - t, y1 - t, z - t, x + t, y2 + t, z + t), color);
            }
        }
    }

    /** A segment drawn as a ribbon turned towards the camera. */
    public void line(VertexConsumer buffer, Matrix4f matrix, Vec3 from, Vec3 to, double width, int fromColor, int toColor) {
        Vec3 direction = to.subtract(from);
        if (direction.lengthSqr() < 1.0E-8) return;

        Vec3 toCamera = camera().subtract(from.add(to).scale(0.5));
        Vec3 side = direction.cross(toCamera);
        if (side.lengthSqr() < 1.0E-8) return;
        side = side.normalize().scale(width / 2.0);

        quad(buffer, matrix, from.subtract(side), from.add(side), to.add(side), to.subtract(side),
                fromColor, fromColor, toColor, toColor);
    }

    /** A textured square that always faces the camera, rotated in its own plane by {@code roll}. */
    public void sprite(VertexConsumer buffer, Matrix4f matrix, Vec3 centre, double size, double roll, int color) {
        Vec3[] axes = cameraAxes();
        double cos = Math.cos(roll), sin = Math.sin(roll);
        Vec3 right = axes[0].scale(cos).add(axes[1].scale(sin)).scale(size);
        Vec3 up = axes[1].scale(cos).subtract(axes[0].scale(sin)).scale(size);

        Vec3 cam = camera();
        Vec3[] corners = {
                centre.subtract(right).subtract(up),
                centre.add(right).subtract(up),
                centre.add(right).add(up),
                centre.subtract(right).add(up)
        };
        float[][] uv = {{0f, 1f}, {1f, 1f}, {1f, 0f}, {0f, 0f}};

        for (int i = 0; i < 4; i++) {
            Vec3 p = corners[i];
            buffer.addVertex(matrix, (float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z))
                    .setUv(uv[i][0], uv[i][1]).setColor(color);
        }
    }

    public int argb(Color color, float alphaScale) {
        int alpha = Math.max(0, Math.min(255, Math.round(color.getAlpha() * alphaScale)));
        return (alpha << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
    }
}
