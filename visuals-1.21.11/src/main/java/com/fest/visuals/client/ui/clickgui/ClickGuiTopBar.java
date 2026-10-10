package com.fest.visuals.client.ui.clickgui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import org.lwjgl.glfw.GLFW;

import com.fest.visuals.api.utils.animation.AnimationUtil;
import com.fest.visuals.api.utils.animation.Easing;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.color.UIColors;
import com.fest.visuals.api.utils.math.MouseUtil;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.utils.render.ScissorUtil;
import com.fest.visuals.api.utils.render.fonts.Fonts;

/**
 * Search field and tab strip along the top of the panel.
 *
 * <p>Pills are icon-only at rest and grow to show their label when selected or hovered, so seven
 * tabs fit beside the search field without crowding. The widths are animated per tab, which also
 * carries the selection: the strip re-flows around whichever pill is currently opening.
 *
 * <p>Hit boxes come from the bounds recorded during the last frame РІР‚вЂќ the same numbers the player is
 * looking at, which is what keeps an animated strip clickable.
 */
public class ClickGuiTopBar {
    private final Map<ClickGuiTab, AnimationUtil> expand = new EnumMap<>(ClickGuiTab.class);
    private final Map<ClickGuiTab, AnimationUtil> hover = new EnumMap<>(ClickGuiTab.class);
    private final List<Bound> bounds = new ArrayList<>();

    private final AnimationUtil searchAnimation = new AnimationUtil();
    private final StringBuilder search = new StringBuilder();
    private boolean searching;

    private float searchX, searchY, searchW, searchH;

    public ClickGuiTopBar() {
        for (ClickGuiTab tab : ClickGuiTab.strip()) {
            AnimationUtil select = new AnimationUtil();
            select.setValue(tab == ClickGuiTab.RENDER ? 1.0 : 0.0);
            expand.put(tab, select);
            hover.put(tab, new AnimationUtil());
        }
    }

    public String query() { return search.toString(); }
    public boolean isSearching() { return searching; }
    public void resetSearchFocus() { searching = false; }
    public void clearSearch() { search.setLength(0); searching = false; }

    public void render(PoseStack matrices, float windowX, float windowY, float alpha, ClickGuiTab selected, double mouseX, double mouseY) {
        int full = (int) (alpha * 255f);

        renderSearch(matrices, windowX, windowY, alpha, full, mouseX, mouseY);
        renderTabs(matrices, windowX, windowY, alpha, full, selected, mouseX, mouseY);

        float line = ClickGuiLayout.scaled(0.5f);
        Color edge = ColorUtil.setAlpha(UIColors.inactiveTextColor(), (int) (0.14f * full));
        RenderUtil.RECT.draw(matrices, windowX + ClickGuiLayout.pad(), windowY + ClickGuiLayout.topBarHeight() - line,
                ClickGuiLayout.windowWidth() - ClickGuiLayout.pad() * 2f, line, 0f, edge);
    }

    private void renderSearch(PoseStack matrices, float windowX, float windowY, float alpha, int full, double mouseX, double mouseY) {
        searchAnimation.update();
        searchAnimation.run(searching || !search.isEmpty() ? 1.0 : 0.0, 340, Easing.EXPO_OUT);
        float focus = (float) searchAnimation.getValue();

        searchH = ClickGuiLayout.scaled(21f);
        searchW = ClickGuiLayout.scaled(84f) + ClickGuiLayout.scaled(34f) * focus;
        searchX = windowX + ClickGuiLayout.pad();
        searchY = windowY + (ClickGuiLayout.topBarHeight() - searchH) / 2f;

        float round = searchH / 2f;
        boolean over = MouseUtil.isHovered(mouseX, mouseY, searchX, searchY, searchW, searchH);

        int fill = (int) ((0.55f * focus + (over ? 0.25f : 0f)) * 80f * alpha);
        if (fill > 0) {
            RenderUtil.RECT.draw(matrices, searchX, searchY, searchW, searchH, round, ColorUtil.setAlpha(UIColors.surfaceInner(), fill));
        }
        if (focus > 0.01f) {
            // Focus ring: a primary hairline that draws itself along the bottom of the field.
            float ring = ClickGuiLayout.scaled(0.6f);
            Color ringColor = ColorUtil.setAlpha(UIColors.primary(), (int) (focus * alpha * 150f));
            RenderUtil.RECT.draw(matrices, searchX, searchY + searchH - ring, searchW * focus, ring, ring, ringColor);
        }

        float icon = ClickGuiLayout.scaled(8f);
        Color iconColor = ColorUtil.interpolate(UIColors.primary(full), UIColors.inactiveTextColor(full), focus);
        ClickGuiIcons.search(matrices, searchX + ClickGuiLayout.scaled(7f), searchY + (searchH - icon) / 2f, icon, iconColor, UIColors.blur(full));

        float textX = searchX + ClickGuiLayout.scaled(19f);
        float fontSize = ClickGuiLayout.scaled(7f);
        String text = search.isEmpty() ? "Поиск" : search.toString();
        Color textColor = search.isEmpty()
                ? UIColors.inactiveTextColor((int) (0.6f * full))
                : UIColors.textColor(full);

        ScissorUtil.start(matrices, searchX, searchY, searchW - ClickGuiLayout.scaled(4f), searchH);
        Fonts.PS_MEDIUM.drawText(matrices, text, textX, searchY + (searchH - fontSize) / 2f, fontSize, textColor);

        if (searching) {
            float blink = (float) ((Math.sin(System.currentTimeMillis() / 300.0) + 1.0) / 2.0);
            float caretX = textX + Fonts.PS_MEDIUM.getWidth(search.toString(), fontSize) + ClickGuiLayout.scaled(1f);
            RenderUtil.RECT.draw(matrices, caretX, searchY + (searchH - fontSize) / 2f, ClickGuiLayout.scaled(0.6f), fontSize,
                    0f, ColorUtil.setAlpha(UIColors.primary(), (int) (blink * alpha * 255f)));
        }
        ScissorUtil.stop(matrices);
    }

    private void renderTabs(PoseStack matrices, float windowX, float windowY, float alpha, int full, ClickGuiTab selected, double mouseX, double mouseY) {
        float iconSize = ClickGuiLayout.scaled(8.5f);
        float height = ClickGuiLayout.scaled(23f);
        float padX = ClickGuiLayout.scaled(7f);
        float gap = ClickGuiLayout.scaled(3f);
        float fontSize = ClickGuiLayout.scaled(7f);

        // Two passes: measure the animated widths, then lay the strip out right-aligned.
        ClickGuiTab[] tabs = ClickGuiTab.strip();
        float total = 0f;
        float[] widths = new float[tabs.length];

        for (int i = 0; i < widths.length; i++) {
            ClickGuiTab tab = tabs[i];

            AnimationUtil selectAnimation = expand.get(tab);
            selectAnimation.update();
            selectAnimation.run(tab == selected ? 1.0 : 0.0, 420, Easing.EXPO_OUT);

            AnimationUtil hoverAnimation = hover.get(tab);
            boolean over = i < bounds.size() && bounds.get(i).contains(mouseX, mouseY);
            hoverAnimation.update();
            hoverAnimation.run(over ? 1.0 : 0.0, 260, Easing.EXPO_OUT);

            float open = Math.max((float) selectAnimation.getValue(), (float) hoverAnimation.getValue());
            float label = Fonts.PS_BOLD.getWidth(tab.getLabel(), fontSize) + gap;
            widths[i] = iconSize + padX * 2f + label * open;
            total += widths[i] + gap + leadingGap(tabs, i);
        }
        total -= gap;

        float x = windowX + ClickGuiLayout.windowWidth() - ClickGuiLayout.pad() - total;
        float y = windowY + (ClickGuiLayout.topBarHeight() - height) / 2f;

        bounds.clear();
        for (int i = 0; i < widths.length; i++) {
            ClickGuiTab tab = tabs[i];
            float width = widths[i];
            x += leadingGap(tabs, i);
            float select = (float) expand.get(tab).getValue();
            float over = (float) hover.get(tab).getValue();
            float open = Math.max(select, over);
            float round = height / 2f;

            bounds.add(new Bound(tab, x, y, width, height));

            if (select > 0.01f) {
                // Selected pill: primary body with a soft glow spilling out of its lower edge.
                Color left = ColorUtil.setAlpha(UIColors.primary(), (int) (select * alpha * 255f));
                Color right = ColorUtil.setAlpha(UIColors.secondary(), (int) (select * alpha * 255f));
                RenderUtil.GRADIENT_RECT.draw(matrices, x, y, width, height, round, left, right, left, right);

                Color glow = ColorUtil.setAlpha(UIColors.primary(), (int) (select * alpha * 55f));
                Color clear = ColorUtil.setAlpha(UIColors.primary(), 0);
                RenderUtil.GRADIENT_RECT.draw(matrices, x + width * 0.12f, y + height * 0.86f, width * 0.76f,
                        ClickGuiLayout.scaled(5f), round, glow, glow, clear, clear);
            }
            if (over > 0.01f && select < 0.99f) {
                RenderUtil.RECT.draw(matrices, x, y, width, height, round,
                        ColorUtil.setAlpha(UIColors.surfaceInner(), (int) (over * (1f - select) * alpha * 190f)));
            }

            Color behind = select > 0.5f ? UIColors.primary(full) : UIColors.blur(full);
            Color iconColor = ColorUtil.interpolate(
                    ColorUtil.setAlpha(Color.WHITE, full),
                    ColorUtil.interpolate(UIColors.textColor(full), UIColors.inactiveTextColor(full), over), select);

            ClickGuiIcons.tab(matrices, tab, x + padX, y + (height - iconSize) / 2f, iconSize, iconColor, behind);

            if (open > 0.02f) {
                ScissorUtil.start(matrices, x, y, width, height);
                Color labelColor = ColorUtil.setAlpha(iconColor, (int) (net.minecraft.util.Mth.clamp(open * open * alpha, 0f, 1f) * 255f));
                Fonts.PS_BOLD.drawText(matrices, tab.getLabel(), x + padX + iconSize + gap,
                        y + (height - fontSize) / 2f, fontSize, labelColor);
                ScissorUtil.stop(matrices);
            }

            x += width + gap;
        }
    }

    /**
     * Extra air in front of the first non-category tab, splitting the strip into the module
     * categories on one side and the client tools on the other.
     */
    private static float leadingGap(ClickGuiTab[] tabs, int index) {
        boolean firstTool = index > 0 && !tabs[index].isCategory() && tabs[index - 1].isCategory();
        return firstTool ? ClickGuiLayout.scaled(22f) : 0f;
    }

    public ClickGuiTab clickedTab(double mouseX, double mouseY) {
        for (Bound bound : bounds) {
            if (bound.contains(mouseX, mouseY)) return bound.tab;
        }
        return null;
    }

    public boolean clickedSearch(double mouseX, double mouseY) {
        boolean hit = MouseUtil.isHovered(mouseX, mouseY, searchX, searchY, searchW, searchH);
        searching = hit;
        return hit;
    }

    public boolean keyPressed(int keyCode) {
        if (!searching) return false;
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!search.isEmpty()) search.deleteCharAt(search.length() - 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (!search.isEmpty()) {
                search.setLength(0);
                return true;
            }
            searching = false;
            return true;
        }
        return keyCode != GLFW.GLFW_KEY_ENTER;
    }

    public boolean charTyped(char chr) {
        if (!searching) return false;
        if (search.length() < 20 && !Character.isISOControl(chr)) {
            search.append(chr);
            return true;
        }
        return false;
    }

    private record Bound(ClickGuiTab tab, float x, float y, float width, float height) {
        boolean contains(double mouseX, double mouseY) {
            return MouseUtil.isHovered(mouseX, mouseY, x, y, width, height);
        }
    }
}
