package com.fest.visuals.client.ui.clickgui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import com.fest.visuals.client.features.waypoints.Waypoint;
import com.fest.visuals.client.features.waypoints.WaypointManager;
import com.fest.visuals.api.utils.animation.AnimationUtil;
import com.fest.visuals.api.utils.animation.Easing;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.color.UIColors;
import com.fest.visuals.api.utils.math.MouseUtil;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.utils.render.ScissorUtil;
import com.fest.visuals.api.utils.render.fonts.Fonts;
import com.fest.visuals.api.utils.render.fonts.Icons;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static com.fest.visuals.api.system.interfaces.QuickImports.mc;

public class ClickGuiWaypoints {
    private final StringBuilder name = new StringBuilder();
    private final StringBuilder xText = new StringBuilder();
    private final StringBuilder yText = new StringBuilder();
    private final StringBuilder zText = new StringBuilder();
    private int typingField = 0; // 0=none, 1=name, 2=x, 3=y, 4=z
    private float scroll;
    private final AnimationUtil scrollAnimation = new AnimationUtil();
    
    private boolean addingWaypoint = false;
    
    public void close() {
        typingField = 0;
        addingWaypoint = false;
        name.setLength(0);
        xText.setLength(0);
        yText.setLength(0);
        zText.setLength(0);
    }
    
    private void prepopulateCoords() {
        if (mc.player != null) {
            xText.setLength(0);
            yText.setLength(0);
            zText.setLength(0);
            xText.append((int)mc.player.getX());
            yText.append((int)mc.player.getY());
            zText.append((int)mc.player.getZ());
            name.setLength(0);
            name.append("Waypoint ").append(WaypointManager.getInstance().getWaypoints().size() + 1);
        }
    }

    public void render(GuiGraphicsExtractor context, float windowX, float windowY, float alpha, int mouseX, int mouseY) {
        PoseStack matrices = RenderUtil.matrices();
        int full = (int) (alpha * 255f);
        float x = ClickGuiLayout.contentX(windowX) + ClickGuiLayout.scaled(12f);
        float y = ClickGuiLayout.contentY(windowY) + ClickGuiLayout.scaled(10f);
        float width = ClickGuiLayout.contentWidth() - ClickGuiLayout.scaled(24f);
        float fieldH = ClickGuiLayout.scaled(18f);
        
        // Auto Death Waypoint toggle
        float chkBoxSize = ClickGuiLayout.scaled(8f);
        boolean autoWp = WaypointManager.getInstance().isAutoDeathWaypoint();
        RenderUtil.RECT.draw(matrices, x, y, chkBoxSize, chkBoxSize, ClickGuiLayout.scaled(2f), autoWp ? UIColors.positiveColor(full) : UIColors.surfaceInner(full));
        Fonts.PS_MEDIUM.drawText(matrices, "Auto Death Waypoint", x + chkBoxSize + ClickGuiLayout.scaled(4f), y + chkBoxSize/2f - ClickGuiLayout.scaled(2.5f), ClickGuiLayout.scaled(5f), UIColors.textColor(full));

        y += ClickGuiLayout.scaled(16f);
        
        float btnW = ClickGuiLayout.scaled(40f);
        
        if (!addingWaypoint) {
            RenderUtil.RECT.draw(matrices, x, y, btnW, fieldH, fieldH / 2f, UIColors.primary(full));
            Fonts.PS_BOLD.drawCenteredText(matrices, "+ ADD", x + btnW / 2f, y + fieldH / 2f - ClickGuiLayout.scaled(3.5f), ClickGuiLayout.scaled(7f), UIColors.textColor(full));
            y += fieldH + ClickGuiLayout.scaled(10f);
        } else {
            float inputW = width * 0.4f;
            float coordW = (width - inputW - ClickGuiLayout.scaled(40f) - ClickGuiLayout.scaled(15f)) / 3f;
            
            float curX = x;
            drawField(matrices, curX, y, inputW, fieldH, name, "Name...", 1, full);
            curX += inputW + ClickGuiLayout.scaled(5f);
            
            drawField(matrices, curX, y, coordW, fieldH, xText, "X", 2, full);
            curX += coordW + ClickGuiLayout.scaled(5f);
            drawField(matrices, curX, y, coordW, fieldH, yText, "Y", 3, full);
            curX += coordW + ClickGuiLayout.scaled(5f);
            drawField(matrices, curX, y, coordW, fieldH, zText, "Z", 4, full);
            curX += coordW + ClickGuiLayout.scaled(5f);
            
            RenderUtil.RECT.draw(matrices, curX, y, btnW, fieldH, fieldH / 2f, UIColors.positiveColor(full));
            Fonts.PS_BOLD.drawCenteredText(matrices, "SAVE", curX + btnW / 2f, y + fieldH / 2f - ClickGuiLayout.scaled(3.5f), ClickGuiLayout.scaled(7f), UIColors.textColor(full));
            
            y += fieldH + ClickGuiLayout.scaled(10f);
        }
        
        float listY = y;
        float listH = ClickGuiLayout.contentHeight() - (listY - ClickGuiLayout.contentY(windowY)) - ClickGuiLayout.scaled(10f);
        
        List<Waypoint> waypoints = WaypointManager.getInstance().getWaypoints();
        scrollAnimation.run(scroll, 12, Easing.SINE_OUT);
        
        ScissorUtil.start(matrices, x, listY, width, listH);
        float row = listY - (float)scrollAnimation.getValue();
        
        for (Waypoint wp : waypoints) {
            RenderUtil.RECT.draw(matrices, x, row, width, fieldH, ClickGuiLayout.scaled(6f), UIColors.surfaceInner(full));
            
            RenderUtil.RECT.draw(matrices, x + ClickGuiLayout.scaled(4f), row + ClickGuiLayout.scaled(4f), ClickGuiLayout.scaled(10f), ClickGuiLayout.scaled(10f), ClickGuiLayout.scaled(2f), ColorUtil.setAlpha(wp.getColor(), full));
            Icons icon = Icons.find(wp.getIcon());
            if (icon != null) {
                Fonts.ICONS.drawText(matrices, icon.getLetter(), x + ClickGuiLayout.scaled(6f), row + ClickGuiLayout.scaled(6f), ClickGuiLayout.scaled(6f), UIColors.textColor(full));
            }
            
            Fonts.PS_BOLD.drawText(matrices, wp.getName(), x + ClickGuiLayout.scaled(18f), row + ClickGuiLayout.scaled(4f), ClickGuiLayout.scaled(6f), UIColors.textColor(full));
            Fonts.PS_MEDIUM.drawText(matrices, String.format("%.0f, %.0f, %.0f", wp.getX(), wp.getY(), wp.getZ()), x + ClickGuiLayout.scaled(18f), row + ClickGuiLayout.scaled(11f), ClickGuiLayout.scaled(4.5f), UIColors.inactiveTextColor(full));
            
            float delX = x + width - ClickGuiLayout.scaled(14f);
            if (Icons.TRASH != null) {
                Fonts.ICONS.drawText(matrices, Icons.TRASH.getLetter(), delX, row + ClickGuiLayout.scaled(6f), ClickGuiLayout.scaled(6f), UIColors.negativeColor(full));
            }
            
            row += fieldH + ClickGuiLayout.scaled(4f);
        }
        
        ScissorUtil.stop(matrices);
    }
    
    private void drawField(PoseStack matrices, float x, float y, float w, float h, StringBuilder sb, String placeholder, int id, int full) {
        RenderUtil.RECT.draw(matrices, x, y, w, h, ClickGuiLayout.scaled(6f), UIColors.surface(full));
        String cursor = (typingField == id) && System.currentTimeMillis() % 1000 > 500 ? "_" : "";
        String text = sb.length() == 0 && typingField != id ? placeholder : sb.toString() + cursor;
        Fonts.PS_MEDIUM.drawText(matrices, text, x + ClickGuiLayout.scaled(4f), y + h / 2f - ClickGuiLayout.scaled(3f), ClickGuiLayout.scaled(5f), UIColors.textColor(full));
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, float windowX, float windowY) {
        float x = ClickGuiLayout.contentX(windowX) + ClickGuiLayout.scaled(12f);
        float y = ClickGuiLayout.contentY(windowY) + ClickGuiLayout.scaled(10f);
        float width = ClickGuiLayout.contentWidth() - ClickGuiLayout.scaled(24f);
        float fieldH = ClickGuiLayout.scaled(18f);

        float chkBoxSize = ClickGuiLayout.scaled(8f);
        if (MouseUtil.isHovered(mouseX, mouseY, x, y, chkBoxSize, chkBoxSize)) {
            WaypointManager.getInstance().setAutoDeathWaypoint(!WaypointManager.getInstance().isAutoDeathWaypoint());
            WaypointManager.getInstance().save();
            return true;
        }

        y += ClickGuiLayout.scaled(16f);
        float btnW = ClickGuiLayout.scaled(40f);
        
        if (!addingWaypoint) {
            if (MouseUtil.isHovered(mouseX, mouseY, x, y, btnW, fieldH)) {
                addingWaypoint = true;
                prepopulateCoords();
                typingField = 1;
                return true;
            }
            y += fieldH + ClickGuiLayout.scaled(10f);
        } else {
            float inputW = width * 0.4f;
            float coordW = (width - inputW - ClickGuiLayout.scaled(40f) - ClickGuiLayout.scaled(15f)) / 3f;
            
            float curX = x;
            if (MouseUtil.isHovered(mouseX, mouseY, curX, y, inputW, fieldH)) { typingField = 1; return true; }
            curX += inputW + ClickGuiLayout.scaled(5f);
            if (MouseUtil.isHovered(mouseX, mouseY, curX, y, coordW, fieldH)) { typingField = 2; return true; }
            curX += coordW + ClickGuiLayout.scaled(5f);
            if (MouseUtil.isHovered(mouseX, mouseY, curX, y, coordW, fieldH)) { typingField = 3; return true; }
            curX += coordW + ClickGuiLayout.scaled(5f);
            if (MouseUtil.isHovered(mouseX, mouseY, curX, y, coordW, fieldH)) { typingField = 4; return true; }
            curX += coordW + ClickGuiLayout.scaled(5f);
            
            if (MouseUtil.isHovered(mouseX, mouseY, curX, y, btnW, fieldH)) {
                if (name.length() > 0 && mc.player != null) {
                    try {
                        double cx = Double.parseDouble(xText.toString());
                        double cy = Double.parseDouble(yText.toString());
                        double cz = Double.parseDouble(zText.toString());
                        String dim = mc.level.dimension().toString();
                        Waypoint wp = new Waypoint(name.toString(), cx, cy, cz, dim, UIColors.primary(255), "COORDS");
                        WaypointManager.getInstance().addWaypoint(wp);
                        addingWaypoint = false;
                        typingField = 0;
                    } catch (NumberFormatException ignored) {}
                }
                return true;
            }
            
            typingField = 0;
            y += fieldH + ClickGuiLayout.scaled(10f);
        }
        
        float listY = y;
        float listH = ClickGuiLayout.contentHeight() - (listY - ClickGuiLayout.contentY(windowY)) - ClickGuiLayout.scaled(10f);
        
        if (MouseUtil.isHovered(mouseX, mouseY, x, listY, width, listH)) {
            float row = listY - (float)scrollAnimation.getValue();
            List<Waypoint> waypoints = new ArrayList<>(WaypointManager.getInstance().getWaypoints());
            for (Waypoint wp : waypoints) {
                float delX = x + width - ClickGuiLayout.scaled(16f);
                if (MouseUtil.isHovered(mouseX, mouseY, delX, row, ClickGuiLayout.scaled(12f), fieldH)) {
                    WaypointManager.getInstance().removeWaypoint(wp);
                    return true;
                }
                row += fieldH + ClickGuiLayout.scaled(4f);
            }
        }
        
        return false;
    }
    
    public boolean mouseScrolled(double mouseX, double mouseY, double amount, float windowX, float windowY) {
        float listY = ClickGuiLayout.contentY(windowY) + ClickGuiLayout.scaled(10f + 16f + 18f + 10f);
        float listH = ClickGuiLayout.contentHeight() - (listY - ClickGuiLayout.contentY(windowY)) - ClickGuiLayout.scaled(10f);
        if (MouseUtil.isHovered(mouseX, mouseY, ClickGuiLayout.contentX(windowX), listY, ClickGuiLayout.contentWidth(), listH)) {
            scroll -= amount * 15f;
            float maxScroll = Math.max(0, WaypointManager.getInstance().getWaypoints().size() * (ClickGuiLayout.scaled(18f) + ClickGuiLayout.scaled(4f)) - listH);
            scroll = Mth.clamp(scroll, 0f, maxScroll);
            return true;
        }
        return false;
    }
    
    public boolean keyPressed(int key) {
        if (typingField == 0) return false;
        
        StringBuilder active = getActiveSB();
        if (key == GLFW.GLFW_KEY_BACKSPACE && active.length() > 0) {
            active.setLength(active.length() - 1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER) {
            typingField = 0;
            return true;
        }
        return false;
    }
    
    public boolean charTyped(char chr) {
        if (typingField == 0) return false;
        
        StringBuilder active = getActiveSB();
        if (typingField == 1) {
            if (active.length() < 24 && net.minecraft.util.StringUtil.isAllowedChatCharacter(chr)) {
                active.append(chr);
                return true;
            }
        } else {
            if (active.length() < 10 && (Character.isDigit(chr) || chr == '-' || chr == '.')) {
                active.append(chr);
                return true;
            }
        }
        return false;
    }
    
    private StringBuilder getActiveSB() {
        if (typingField == 1) return name;
        if (typingField == 2) return xText;
        if (typingField == 3) return yText;
        if (typingField == 4) return zText;
        return name;
    }
}