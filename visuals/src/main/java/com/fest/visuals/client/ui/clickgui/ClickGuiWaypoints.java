package com.fest.visuals.client.ui.clickgui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.color.UIColors;
import com.fest.visuals.api.utils.math.MouseUtil;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.utils.render.ScissorUtil;
import com.fest.visuals.api.utils.render.fonts.Fonts;
import com.fest.visuals.api.utils.render.fonts.Icons;
import com.fest.visuals.client.features.waypoints.Waypoint;
import com.fest.visuals.client.features.waypoints.WaypointManager;

import static com.fest.visuals.api.system.interfaces.QuickImports.mc;

/**
 * The waypoints tab: a header with the two automatic-mark switches, a form that unfolds to add a
 * mark by hand, and a list of cards with distance, dimension and — for event marks — the time
 * left.
 *
 * <p>Every rectangle is worked out in one place ({@link Layout}) and used by drawing, clicking
 * and scrolling alike, so the three can never disagree about where something is.
 */
public class ClickGuiWaypoints {
    private static final Color[] SWATCHES = {
            new Color(255, 120, 40), new Color(255, 70, 90), new Color(180, 90, 255),
            new Color(70, 170, 255), new Color(70, 220, 140), new Color(255, 215, 60)
    };

    private final StringBuilder name = new StringBuilder();
    private final StringBuilder xText = new StringBuilder();
    private final StringBuilder yText = new StringBuilder();
    private final StringBuilder zText = new StringBuilder();
    /** 0 none, 1 name, 2 x, 3 y, 4 z. */
    private int typingField;
    private int swatch;

    private boolean adding;
    private float formOpen;
    private float deathKnob;
    private float eventKnob;
    private float scroll;
    private float scrollShown;
    private long lastFrame;
    private long openedAt = System.currentTimeMillis();
    private final Map<UUID, Float> hover = new HashMap<>();

    public void close() {
        typingField = 0;
        adding = false;
        name.setLength(0);
        xText.setLength(0);
        yText.setLength(0);
        zText.setLength(0);
        openedAt = System.currentTimeMillis();
    }

    private static float s(float value) {
        return ClickGuiLayout.scaled(value);
    }

    /** All positions for one frame. */
    private final class Layout {
        final float x, y, width, height;
        final float headerH = s(22f);
        final float toggleW = s(64f), toggleH = s(14f);
        final float deathX, eventX, toggleY;
        final float addY, addH = s(18f);
        final float formY, formH;
        final float listY, listH;
        final float cardH = s(26f), cardGap = s(5f);

        Layout(float windowX, float windowY) {
            x = ClickGuiLayout.contentX(windowX) + s(12f);
            y = ClickGuiLayout.contentY(windowY) + s(8f);
            width = ClickGuiLayout.contentWidth() - s(24f);
            height = ClickGuiLayout.contentHeight() - s(16f);

            toggleY = y + (headerH - toggleH) / 2f;
            eventX = x + width - toggleW;
            deathX = eventX - toggleW - s(6f);

            addY = y + headerH + s(6f);
            formY = addY + addH + s(6f);
            formH = s(58f) * formOpen;
            listY = formY + formH + (formOpen > 0.01f ? s(6f) : 0f);
            listH = y + height - listY;
        }

        float fieldY() { return formY + s(6f); }
        float fieldH() { return s(16f); }
        float nameW() { return width * 0.36f; }
        float coordW() { return (width - nameW() - s(6f) * 3 - s(8f)) / 3f; }
        float fieldX(int field) {
            if (field == 1) return x + s(4f);
            return x + s(4f) + nameW() + s(6f) + (field - 2) * (coordW() + s(6f));
        }
        float fieldW(int field) { return field == 1 ? nameW() - s(4f) : coordW(); }
        float swatchY() { return formY + s(28f); }
        float swatchSize() { return s(12f); }
        float swatchX(int i) { return x + s(4f) + i * (swatchSize() + s(5f)); }
        float saveW() { return s(58f); }
        float saveX() { return x + width - saveW() - s(4f); }
        float cancelX() { return saveX() - saveW() - s(5f); }
        float buttonsY() { return formY + s(26f); }
        float buttonH() { return s(16f); }
        float cardY(int index) { return listY + index * (cardH + cardGap) - scrollShown; }
        float deleteX() { return x + width - s(20f); }
    }

    public void render(GuiGraphicsExtractor context, float windowX, float windowY, float alpha, int mouseX, int mouseY) {
        PoseStack matrices = RenderUtil.matrices();
        int full = (int) (alpha * 255f);

        long now = System.currentTimeMillis();
        float dt = lastFrame == 0 ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        float ease = Math.min(1f, dt * 14f);

        formOpen += ((adding ? 1f : 0f) - formOpen) * ease;
        WaypointManager manager = WaypointManager.getInstance();
        deathKnob += ((manager.isAutoDeathWaypoint() ? 1f : 0f) - deathKnob) * ease;
        eventKnob += ((manager.isAutoEventWaypoints() ? 1f : 0f) - eventKnob) * ease;

        Layout l = new Layout(windowX, windowY);
        List<Waypoint> waypoints = manager.getWaypoints();

        // Header: the window already titles the tab, so this line only carries the count.
        String count = waypoints.size() + " " + plural(waypoints.size());
        Fonts.PS_MEDIUM.drawText(matrices, count, l.x, l.y + l.headerH / 2f - s(3.5f), s(7f), UIColors.inactiveTextColor(full));

        drawToggle(matrices, l.deathX, l.toggleY, l.toggleW, l.toggleH, "Смерть", deathKnob, full, mouseX, mouseY);
        drawToggle(matrices, l.eventX, l.toggleY, l.toggleW, l.toggleH, "Ивенты FT", eventKnob, full, mouseX, mouseY);

        // Add button.
        boolean addHover = MouseUtil.isHovered(mouseX, mouseY, l.x, l.addY, l.width, l.addH);
        RenderUtil.RECT.draw(matrices, l.x, l.addY, l.width, l.addH, l.addH / 2f,
                ColorUtil.setAlpha(UIColors.primary(), (int) (full * (adding ? 0.18f : addHover ? 0.3f : 0.2f))));
        String addLabel = adding ? "Новая метка" : "+  Добавить метку здесь";
        Fonts.PS_BOLD.drawCenteredText(matrices, addLabel, l.x + l.width / 2f, l.addY + l.addH / 2f - s(3.5f), s(7f),
                UIColors.primary(full));

        if (formOpen > 0.01f) drawForm(matrices, l, full, mouseX, mouseY);

        // List.
        float maxScroll = Math.max(0f, waypoints.size() * (l.cardH + l.cardGap) - l.cardGap - l.listH);
        scroll = Mth.clamp(scroll, 0f, maxScroll);
        scrollShown += (scroll - scrollShown) * ease;

        if (waypoints.isEmpty()) {
            Fonts.PS_MEDIUM.drawCenteredText(matrices, "Меток пока нет — добавь вручную или дождись ивента FT",
                    l.x + l.width / 2f, l.listY + s(20f), s(6f), UIColors.inactiveTextColor(full));
            return;
        }

        ScissorUtil.start(matrices, l.x - s(2f), l.listY, l.width + s(4f), l.listH);
        Map<UUID, Float> alive = new HashMap<>();
        for (int i = 0; i < waypoints.size(); i++) {
            Waypoint wp = waypoints.get(i);
            float cardY = l.cardY(i);
            if (cardY + l.cardH < l.listY || cardY > l.listY + l.listH) continue;

            // Cards arrive one after another when the tab opens.
            float appear = Mth.clamp((now - openedAt - i * 35L) / 220f, 0f, 1f);
            appear = 1f - (1f - appear) * (1f - appear);
            float slide = (1f - appear) * s(10f);
            int a = (int) (full * appear);

            boolean over = MouseUtil.isHovered(mouseX, mouseY, l.x, cardY, l.width, l.cardH)
                    && MouseUtil.isHovered(mouseX, mouseY, l.x, l.listY, l.width, l.listH);
            float h = hover.getOrDefault(wp.getId(), 0f);
            h += ((over ? 1f : 0f) - h) * ease;
            alive.put(wp.getId(), h);

            drawCard(matrices, l, wp, cardY + slide, a, h, mouseX, mouseY);
        }
        hover.clear();
        hover.putAll(alive);
        ScissorUtil.stop(matrices);
    }

    private void drawToggle(PoseStack matrices, float x, float y, float w, float h, String label, float knob,
                            int full, int mouseX, int mouseY) {
        boolean over = MouseUtil.isHovered(mouseX, mouseY, x, y, w, h);
        RenderUtil.RECT.draw(matrices, x, y, w, h, h / 2f, UIColors.surfaceInner((int) (full * (over ? 1f : 0.85f))));

        float trackW = s(16f), trackH = s(8f);
        float trackX = x + w - trackW - s(4f);
        float trackY = y + (h - trackH) / 2f;
        Color track = ColorUtil.interpolate(UIColors.positiveColor(full), UIColors.inactiveKnob(full), knob);
        RenderUtil.RECT.draw(matrices, trackX, trackY, trackW, trackH, trackH / 2f, track);
        float dot = trackH - s(2f);
        RenderUtil.RECT.draw(matrices, trackX + s(1f) + (trackW - dot - s(2f)) * knob, trackY + s(1f), dot, dot, dot / 2f,
                ColorUtil.setAlpha(Color.WHITE, full));

        Fonts.PS_MEDIUM.drawText(matrices, label, x + s(5f), y + h / 2f - s(2.8f), s(5.5f),
                ColorUtil.interpolate(UIColors.textColor(full), UIColors.inactiveTextColor(full), knob));
    }

    private void drawForm(PoseStack matrices, Layout l, int full, int mouseX, int mouseY) {
        int a = (int) (full * formOpen);
        ScissorUtil.start(matrices, l.x, l.formY, l.width, l.formH);

        RenderUtil.RECT.draw(matrices, l.x, l.formY, l.width, s(58f), s(8f), UIColors.surfaceInner(a));

        drawField(matrices, l.fieldX(1), l.fieldY(), l.fieldW(1), l.fieldH(), name, "Название", 1, a);
        drawField(matrices, l.fieldX(2), l.fieldY(), l.fieldW(2), l.fieldH(), xText, "X", 2, a);
        drawField(matrices, l.fieldX(3), l.fieldY(), l.fieldW(3), l.fieldH(), yText, "Y", 3, a);
        drawField(matrices, l.fieldX(4), l.fieldY(), l.fieldW(4), l.fieldH(), zText, "Z", 4, a);

        for (int i = 0; i < SWATCHES.length; i++) {
            float size = l.swatchSize();
            float sx = l.swatchX(i), sy = l.swatchY();
            if (i == swatch) {
                RenderUtil.RECT.draw(matrices, sx - s(1.5f), sy - s(1.5f), size + s(3f), size + s(3f), (size + s(3f)) / 2f,
                        ColorUtil.setAlpha(Color.WHITE, a));
            }
            RenderUtil.RECT.draw(matrices, sx, sy, size, size, size / 2f, ColorUtil.setAlpha(SWATCHES[i], a));
        }

        boolean saveOver = MouseUtil.isHovered(mouseX, mouseY, l.saveX(), l.buttonsY(), l.saveW(), l.buttonH());
        boolean cancelOver = MouseUtil.isHovered(mouseX, mouseY, l.cancelX(), l.buttonsY(), l.saveW(), l.buttonH());
        RenderUtil.RECT.draw(matrices, l.saveX(), l.buttonsY(), l.saveW(), l.buttonH(), l.buttonH() / 2f,
                ColorUtil.setAlpha(UIColors.positiveColor(), (int) (a * (saveOver ? 1f : 0.8f))));
        Fonts.PS_BOLD.drawCenteredText(matrices, "Сохранить", l.saveX() + l.saveW() / 2f, l.buttonsY() + l.buttonH() / 2f - s(3f),
                s(6f), ColorUtil.setAlpha(Color.WHITE, a));
        RenderUtil.RECT.draw(matrices, l.cancelX(), l.buttonsY(), l.saveW(), l.buttonH(), l.buttonH() / 2f,
                UIColors.surface((int) (a * (cancelOver ? 1f : 0.8f))));
        Fonts.PS_BOLD.drawCenteredText(matrices, "Отмена", l.cancelX() + l.saveW() / 2f, l.buttonsY() + l.buttonH() / 2f - s(3f),
                s(6f), UIColors.inactiveTextColor(a));

        ScissorUtil.stop(matrices);
    }

    private void drawField(PoseStack matrices, float x, float y, float w, float h, StringBuilder text, String placeholder,
                           int id, int alpha) {
        boolean focused = typingField == id;
        RenderUtil.RECT.draw(matrices, x, y, w, h, s(5f), UIColors.surface(alpha));
        if (focused) {
            RenderUtil.RECT.draw(matrices, x, y + h - s(1.5f), w, s(1.5f), s(0.75f), UIColors.primary(alpha));
        }
        String cursor = focused && System.currentTimeMillis() % 1000 > 500 ? "_" : "";
        boolean empty = text.length() == 0 && !focused;
        Fonts.PS_MEDIUM.drawText(matrices, empty ? placeholder : text + cursor, x + s(5f), y + h / 2f - s(3f), s(6f),
                empty ? UIColors.inactiveTextColor(alpha) : UIColors.textColor(alpha));
    }

    private void drawCard(PoseStack matrices, Layout l, Waypoint wp, float y, int alpha, float hoverAmount,
                          int mouseX, int mouseY) {
        RenderUtil.RECT.draw(matrices, l.x, y, l.width, l.cardH, s(8f),
                UIColors.surfaceInner((int) (alpha * (0.8f + 0.2f * hoverAmount))));

        Color color = wp.getColor();
        RenderUtil.RECT.draw(matrices, l.x, y, s(3f), l.cardH, s(1.5f), ColorUtil.setAlpha(color, alpha));

        float iconSize = s(16f);
        float iconX = l.x + s(8f), iconY = y + (l.cardH - iconSize) / 2f;
        RenderUtil.RECT.draw(matrices, iconX, iconY, iconSize, iconSize, iconSize / 2f, ColorUtil.setAlpha(color, (int) (alpha * 0.25f)));
        Icons icon = Icons.find(wp.getIcon());
        if (icon != null) {
            Fonts.ICONS.drawCenteredText(matrices, icon.getLetter(), iconX + iconSize / 2f, iconY + iconSize / 2f - s(3.5f), s(7f),
                    ColorUtil.setAlpha(color, alpha));
        }

        float textX = iconX + iconSize + s(6f);
        Fonts.PS_BOLD.drawText(matrices, wp.getName(), textX, y + s(5f), s(7f), UIColors.textColor(alpha));

        StringBuilder info = new StringBuilder(String.format("%.0f  %.0f  %.0f", wp.getX(), wp.getY(), wp.getZ()));
        if (mc.player != null) {
            double dx = wp.getX() - mc.player.getX(), dz = wp.getZ() - mc.player.getZ();
            info.append("    ").append((int) Math.sqrt(dx * dx + dz * dz)).append(" м");
        }
        String dim = dimension(wp.getDimension());
        if (!dim.isEmpty()) info.append("    ").append(dim);
        Fonts.PS_MEDIUM.drawText(matrices, info.toString(), textX, y + s(15f), s(5.5f), UIColors.inactiveTextColor(alpha));

        float rightEdge = l.deleteX() - s(6f);
        if (wp.getExpiresAt() != 0) {
            long left = Math.max(0, (wp.getExpiresAt() - System.currentTimeMillis()) / 1000);
            String timer = String.format("%d:%02d", left / 60, left % 60);
            float tw = Fonts.PS_BOLD.getWidth(timer, s(6f)) + s(10f);
            RenderUtil.RECT.draw(matrices, rightEdge - tw, y + (l.cardH - s(12f)) / 2f, tw, s(12f), s(6f),
                    ColorUtil.setAlpha(color, (int) (alpha * 0.22f)));
            Fonts.PS_BOLD.drawCenteredText(matrices, timer, rightEdge - tw / 2f, y + l.cardH / 2f - s(3f), s(6f),
                    ColorUtil.setAlpha(color, alpha));
        }

        boolean delOver = MouseUtil.isHovered(mouseX, mouseY, l.deleteX(), y + (l.cardH - s(14f)) / 2f, s(14f), s(14f));
        RenderUtil.RECT.draw(matrices, l.deleteX(), y + (l.cardH - s(14f)) / 2f, s(14f), s(14f), s(7f),
                ColorUtil.setAlpha(UIColors.negativeColor(), (int) (alpha * (delOver ? 0.35f : 0.12f * hoverAmount))));
        if (Icons.TRASH != null) {
            Fonts.ICONS.drawCenteredText(matrices, Icons.TRASH.getLetter(), l.deleteX() + s(7f), y + l.cardH / 2f - s(3f), s(6f),
                    UIColors.negativeColor((int) (alpha * (0.5f + 0.5f * Math.max(hoverAmount, delOver ? 1f : 0f)))));
        }
    }

    private static String plural(int n) {
        int mod10 = n % 10, mod100 = n % 100;
        if (mod10 == 1 && mod100 != 11) return "метка";
        if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return "метки";
        return "меток";
    }

    private static String dimension(String key) {
        if (key == null) return "";
        String lower = key.toLowerCase();
        if (lower.contains("the_nether")) return "Незер";
        if (lower.contains("the_end")) return "Энд";
        if (lower.contains("overworld")) return "Верхний мир";
        return "";
    }

    private void prepopulate() {
        name.setLength(0);
        xText.setLength(0);
        yText.setLength(0);
        zText.setLength(0);
        name.append("Метка ").append(WaypointManager.getInstance().getWaypoints().size() + 1);
        if (mc.player != null) {
            xText.append((int) Math.floor(mc.player.getX()));
            yText.append((int) Math.floor(mc.player.getY()));
            zText.append((int) Math.floor(mc.player.getZ()));
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, float windowX, float windowY) {
        Layout l = new Layout(windowX, windowY);
        WaypointManager manager = WaypointManager.getInstance();

        if (MouseUtil.isHovered(mouseX, mouseY, l.deathX, l.toggleY, l.toggleW, l.toggleH)) {
            manager.setAutoDeathWaypoint(!manager.isAutoDeathWaypoint());
            manager.save();
            return true;
        }
        if (MouseUtil.isHovered(mouseX, mouseY, l.eventX, l.toggleY, l.toggleW, l.toggleH)) {
            manager.setAutoEventWaypoints(!manager.isAutoEventWaypoints());
            manager.save();
            return true;
        }

        if (MouseUtil.isHovered(mouseX, mouseY, l.x, l.addY, l.width, l.addH)) {
            adding = !adding;
            if (adding) {
                prepopulate();
                typingField = 1;
            } else {
                typingField = 0;
            }
            return true;
        }

        if (adding && MouseUtil.isHovered(mouseX, mouseY, l.x, l.formY, l.width, l.formH)) {
            for (int field = 1; field <= 4; field++) {
                if (MouseUtil.isHovered(mouseX, mouseY, l.fieldX(field), l.fieldY(), l.fieldW(field), l.fieldH())) {
                    typingField = field;
                    return true;
                }
            }
            for (int i = 0; i < SWATCHES.length; i++) {
                if (MouseUtil.isHovered(mouseX, mouseY, l.swatchX(i), l.swatchY(), l.swatchSize(), l.swatchSize())) {
                    swatch = i;
                    return true;
                }
            }
            if (MouseUtil.isHovered(mouseX, mouseY, l.saveX(), l.buttonsY(), l.saveW(), l.buttonH())) {
                save();
                return true;
            }
            if (MouseUtil.isHovered(mouseX, mouseY, l.cancelX(), l.buttonsY(), l.saveW(), l.buttonH())) {
                adding = false;
                typingField = 0;
                return true;
            }
            typingField = 0;
            return true;
        }

        if (MouseUtil.isHovered(mouseX, mouseY, l.x, l.listY, l.width, l.listH)) {
            List<Waypoint> waypoints = new ArrayList<>(manager.getWaypoints());
            for (int i = 0; i < waypoints.size(); i++) {
                float cardY = l.cardY(i);
                if (MouseUtil.isHovered(mouseX, mouseY, l.deleteX(), cardY + (l.cardH - s(14f)) / 2f, s(14f), s(14f))) {
                    manager.removeWaypoint(waypoints.get(i));
                    return true;
                }
            }
        }

        typingField = 0;
        return false;
    }

    private void save() {
        if (name.length() == 0 || mc.level == null) return;
        try {
            double cx = Double.parseDouble(xText.toString());
            double cy = Double.parseDouble(yText.toString());
            double cz = Double.parseDouble(zText.toString());
            Waypoint wp = new Waypoint(name.toString(), cx + 0.5, cy, cz + 0.5, mc.level.dimension().toString(),
                    SWATCHES[swatch], "COORDS");
            WaypointManager.getInstance().addWaypoint(wp);
            adding = false;
            typingField = 0;
        } catch (NumberFormatException ignored) {
            // Leave the form open so the bad coordinate can be fixed.
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount, float windowX, float windowY) {
        Layout l = new Layout(windowX, windowY);
        if (!MouseUtil.isHovered(mouseX, mouseY, l.x, l.listY, l.width, l.listH)) return false;
        scroll -= (float) amount * (l.cardH + l.cardGap);
        return true;
    }

    /** Only claims keys while a field is focused, so Escape still closes the GUI otherwise. */
    public boolean keyPressed(int key) {
        if (typingField == 0) return false;

        StringBuilder active = active();
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (active.length() > 0) active.setLength(active.length() - 1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_TAB) {
            typingField = typingField % 4 + 1;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            save();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            typingField = 0;
            return true;
        }
        return true;
    }

    public boolean charTyped(char chr) {
        if (typingField == 0) return false;

        StringBuilder active = active();
        if (typingField == 1) {
            if (active.length() < 24 && net.minecraft.util.StringUtil.isAllowedChatCharacter(chr)) active.append(chr);
        } else if (active.length() < 10 && (Character.isDigit(chr) || (chr == '-' && active.length() == 0) || chr == '.')) {
            active.append(chr);
        }
        return true;
    }

    private StringBuilder active() {
        return switch (typingField) {
            case 2 -> xText;
            case 3 -> yText;
            case 4 -> zText;
            default -> name;
        };
    }
}
