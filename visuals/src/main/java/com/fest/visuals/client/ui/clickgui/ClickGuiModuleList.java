package com.fest.visuals.client.ui.clickgui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleManager;
import com.fest.visuals.api.utils.animation.AnimationUtil;
import com.fest.visuals.api.utils.animation.Easing;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.color.UIColors;
import com.fest.visuals.api.utils.math.MouseUtil;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.utils.render.ScissorUtil;
import com.fest.visuals.api.utils.render.fonts.Fonts;
import com.fest.visuals.client.features.modules.render.ClickGUIModule;
import com.fest.visuals.client.ui.clickgui.module.ModuleComponent;

/**
 * The two-column module grid.
 *
 * <p>Rows enter with a stagger keyed off the moment the list last changed — switching a tab or
 * typing in the search field restarts it, so the whole grid cascades in instead of appearing at
 * once. Right-clicking a row asks the screen to open its settings card.
 */
public class ClickGuiModuleList {
    /** Lets the screen own the settings cards without the list knowing about them. */
    public interface SettingsOpener {
        void open(ModuleComponent component, float rowX, float rowY);
    }

    private static final long ENTER_DURATION = 380L;
    private static final long ENTER_STAGGER = 26L;

    private final Map<Module, ModuleComponent> components = new HashMap<>();
    private final AnimationUtil scrollAnimation = new AnimationUtil();

    private SettingsOpener opener;
    private float scroll;
    private long enterTime = System.currentTimeMillis();
    private String lastSignature = "";

    public void setOpener(SettingsOpener opener) {
        this.opener = opener;
    }

    public void rebuild() {
        components.clear();
    }

    public void resetScroll() {
        scroll = 0f;
        scrollAnimation.setValue(0);
    }

    /** Replays the cascade — called whenever the visible set changes. */
    public void restart() {
        enterTime = System.currentTimeMillis();
        resetScroll();
    }

    public void render(GuiGraphicsExtractor context, float windowX, float windowY, float alpha, ClickGuiTab tab,
                       String query, int mouseX, int mouseY, float delta) {
        PoseStack matrices = RenderUtil.matrices();
        List<ModuleComponent> visible = visible(tab, query);

        String signature = tab.name() + "/" + query + "/" + visible.size();
        if (!signature.equals(lastSignature)) {
            lastSignature = signature;
            enterTime = System.currentTimeMillis();
        }

        float areaX = ClickGuiLayout.contentX(windowX);
        float areaY = ClickGuiLayout.contentY(windowY);
        float areaW = ClickGuiLayout.contentWidth();
        float areaH = ClickGuiLayout.contentHeight() - ClickGuiLayout.scaled(8f);

        if (visible.isEmpty()) {
            float size = ClickGuiLayout.scaled(7.5f);
            Fonts.PS_MEDIUM.drawCenteredText(matrices, "Ничего не найдено", areaX + areaW / 2f,
                    areaY + ClickGuiLayout.scaled(30f), size, UIColors.inactiveTextColor((int) (alpha * 160f)));
            return;
        }

        scrollAnimation.update();
        scrollAnimation.run(scroll, 300, Easing.EXPO_OUT);
        float offset = (float) scrollAnimation.getValue();

        float columnWidth = ClickGuiLayout.columnWidth();
        float rowHeight = ClickGuiLayout.rowHeight();
        float left = windowX + ClickGuiLayout.pad();
        long now = System.currentTimeMillis();

        ScissorUtil.start(matrices, areaX, areaY, areaW, areaH);

        for (int i = 0; i < visible.size(); i++) {
            ModuleComponent card = visible.get(i);
            int row = i / 2;
            boolean right = i % 2 == 1;

            float x = left + (right ? columnWidth + ClickGuiLayout.columnGap() : 0f);
            float y = areaY + offset + row * rowHeight;

            float raw = (now - enterTime - i * ENTER_STAGGER) / (float) ENTER_DURATION;
            float appear = Easing.EXPO_OUT.apply(Mth.clamp(raw, 0f, 1f));

            card.setX(x);
            card.setY(y);
            card.setWidth(columnWidth);
            card.setHeight(rowHeight - ClickGuiLayout.scaled(4f));
            card.setAlpha(alpha);
            card.setAppear(appear);
            card.render(context, mouseX, mouseY, delta);

            // Hairline between rows, skipped under the last one of each column.
            boolean lastInColumn = i + 2 >= visible.size();
            if (!lastInColumn) {
                Color line = ColorUtil.setAlpha(UIColors.inactiveTextColor(), (int) (appear * alpha * 26f));
                RenderUtil.RECT.draw(matrices, x, y + rowHeight - ClickGuiLayout.scaled(4f), columnWidth,
                        ClickGuiLayout.scaled(0.5f), 0f, line);
            }
        }

        ScissorUtil.stop(matrices);

        float contentH = ((visible.size() + 1) / 2) * rowHeight;
        float minScroll = Math.min(areaH - contentH, 0f);
        scroll = Mth.clamp(scroll, minScroll, 0f);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, float windowX, float windowY, ClickGuiTab tab, String query) {
        if (!hovered(mouseX, mouseY, windowX, windowY)) return false;

        for (ModuleComponent card : visible(tab, query)) {
            if (!MouseUtil.isHovered(mouseX, mouseY, card.getX(), card.getY(), card.getWidth(), card.getHeight())) continue;

            if (button == 1) {
                if (card.hasSettings() && opener != null) {
                    opener.open(card, card.getX(), card.getY());
                }
                return true;
            }
            card.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button, ClickGuiTab tab, String query) {
        for (ModuleComponent card : visible(tab, query)) {
            card.mouseReleased(mouseX, mouseY, button);
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount, float windowX, float windowY) {
        if (!hovered(mouseX, mouseY, windowX, windowY)) return false;
        scroll += (float) (amount * ClickGuiLayout.scaled(18f));
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers, ClickGuiTab tab, String query) {
        for (ModuleComponent card : visible(tab, query)) {
            card.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    private boolean hovered(double mouseX, double mouseY, float windowX, float windowY) {
        return MouseUtil.isHovered(mouseX, mouseY,
                ClickGuiLayout.contentX(windowX),
                ClickGuiLayout.contentY(windowY),
                ClickGuiLayout.contentWidth(),
                ClickGuiLayout.contentHeight());
    }

    private List<ModuleComponent> visible(ClickGuiTab tab, String query) {
        String filter = query == null ? "" : query.toLowerCase();
        List<Module> modules = ModuleManager.getInstance().getModules().stream()
                .filter(module -> !(module instanceof ClickGUIModule))
                .filter(module -> filter.isEmpty()
                        ? tab.isCategory() && module.getCategory() == tab.getCategory()
                        : module.getName().toLowerCase().contains(filter)
                          || module.getDescription().toLowerCase().contains(filter))
                .sorted(Comparator.comparing(Module::getName))
                .toList();

        List<ModuleComponent> result = new ArrayList<>();
        for (Module module : modules) {
            result.add(components.computeIfAbsent(module, ModuleComponent::new));
        }
        return result;
    }
}
