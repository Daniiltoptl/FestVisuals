package com.fest.visuals.client.features.waypoints;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.render.Render3DEvent;
import com.fest.visuals.api.event.events.render.Render2DEvent;
import com.fest.visuals.api.system.interfaces.QuickImports;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.utils.render.fonts.Fonts;

import java.awt.Color;
import java.util.List;

public class WaypointsRender implements QuickImports {
    private static final WaypointsRender INSTANCE = new WaypointsRender();
    public static WaypointsRender getInstance() { return INSTANCE; }
    
    private Matrix4f lastProj = new Matrix4f();
    private Matrix4f lastModelView = new Matrix4f();

    public void init() {
        Render3DEvent.getInstance().subscribe(new com.fest.visuals.api.event.Listener<Render3DEvent.Render3DEventData>(this::onRender3D));
        Render2DEvent.getInstance().subscribe(new com.fest.visuals.api.event.Listener<Render2DEvent.Render2DEventData>(this::onRender2D));
    }

    public void onRender3D(Render3DEvent.Render3DEventData event) {
        lastProj = com.fest.visuals.api.utils.render.RenderMatrices.projection();
        Camera camera = mc.gameRenderer.getMainCamera();
        lastModelView.rotation(camera.rotation().conjugate(new org.joml.Quaternionf()));
    }

    public void onRender2D(Render2DEvent.Render2DEventData event) {
        if (mc.player == null || mc.level == null) return;
        
        String dim = mc.level.dimension().toString();
        List<Waypoint> waypoints = WaypointManager.getInstance().getWaypoints();
        
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.position();
        
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        
        for (Waypoint wp : waypoints) {
            if (!wp.getDimension().equals(dim)) continue;
            
            Vec3 playerPos = mc.player.position();
            double dist = Math.sqrt(Math.pow(playerPos.x - wp.getX(), 2) + Math.pow(playerPos.y - wp.getY(), 2) + Math.pow(playerPos.z - wp.getZ(), 2));
            
            if (dist > 5000) continue;
            
            double x = wp.getX() - camPos.x;
            double y = wp.getY() - camPos.y + 1.0;
            double z = wp.getZ() - camPos.z;
            
            Vector4f pos = new Vector4f((float)x, (float)y, (float)z, 1.0f);
            pos.mul(lastModelView);
            
            if (pos.z > 0) continue; // Behind camera
            
            pos.mul(lastProj);
            
            if (pos.w <= 0) continue;
            
            pos.x /= pos.w;
            pos.y /= pos.w;
            
            float screenX = (pos.x + 1.0f) * 0.5f * width;
            float screenY = (1.0f - pos.y) * 0.5f * height;
            
            PoseStack ms = event.matrixStack();
            ms.pushPose();
            ms.translate(screenX, screenY, 0);
            
            String text = wp.getName() + " [" + (int)dist + "m]";
            float textWidth = Fonts.PS_BOLD.getWidth(text, 8f);
            
            RenderUtil.RECT.draw(ms, -textWidth/2f - 3f, -5f, textWidth + 6f, 13f, 3f, new Color(0, 0, 0, 100));
            
            Fonts.PS_BOLD.drawCenteredText(ms, text, 0, 0, 8f, wp.getColor());
            com.fest.visuals.api.utils.render.fonts.Icons icon = com.fest.visuals.api.utils.render.fonts.Icons.find(wp.getIcon());
            if (icon != null) {
                Fonts.ICONS.drawCenteredText(ms, icon.getLetter(), 0, -10f, 10f, wp.getColor());
            }
            
            ms.popPose();
        }
    }
}