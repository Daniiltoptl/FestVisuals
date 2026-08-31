package com.fest.visuals.client.ui.clickgui.module;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;

import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.setting.*;
import com.fest.visuals.api.system.backend.KeyStorage;
import com.fest.visuals.api.utils.animation.AnimationUtil;
import com.fest.visuals.api.utils.animation.Easing;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.color.UIColors;
import com.fest.visuals.api.utils.math.MouseUtil;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.utils.render.ScissorUtil;
import com.fest.visuals.api.utils.render.fonts.Fonts;
import com.fest.visuals.client.ui.UIComponent;
import com.fest.visuals.client.ui.clickgui.ClickGuiLayout;
import com.fest.visuals.client.ui.clickgui.module.settings.*;

/**
 * One row of the module grid: name, description and a switch.
 *
 * <p>The row owns its settings components but never draws them — right-clicking hands them to a
 * floating card instead, so the grid keeps a fixed rhythm no matter how many settings a module has.
 *
 * <p>Three animations run per row: the entrance slide the grid drives through {@link #setAppear},
 * a hover lift, and the switch itself, whose knob overshoots and leaves a short glow pulse behind
 * when the module comes on.
 */
@Getter
@Setter
public class ModuleComponent extends UIComponent {
    private final List<SettingComponent> settings = new ArrayList<>();
    private final Module module;

    private final AnimationUtil enableAnimation = new AnimationUtil();
    private final AnimationUtil hoverAnimation = new AnimationUtil();

    /** 0 while the row is still flying in, 1 once it has landed. */
    private float appear = 1f;
    private boolean bind;
    private long pulseStart = -1L;

    public ModuleComponent(Module module) {
        this.module = module;
        for (Setting<?> setting : module.getSettings()) {
            if (setting instanceof BooleanSetting bool) settings.add(new BooleanComponent(bool));
            if (setting instanceof MultiBooleanSetting multi) settings.add(new MultiBooleanComponent(multi));
            if (setting instanceof ModeSetting mode) settings.add(new ModeComponent(mode));
            if (setting instanceof SliderSetting slider) settings.add(new SliderComponent(slider));
            if (setting instanceof ColorSetting color) settings.add(new ColorComponent(color));
            if (setting instanceof RunSetting run) settings.add(new ButtonComponent(run));
            if (setting instanceof BindSetting bindSetting) settings.add(new BindComponent(bindSetting));
            if (setting instanceof StringSetting string) settings.add(new StringComponent(string));
            if (setting instanceof CanvasSetting canvasSetting) settings.add(new CanvasComponent(canvasSetting));
        }
        enableAnimation.setValue(module.isEnabled() ? 1.0 : 0.0);
    }

    public boolean hasSettings() {
        return !settings.isEmpty();
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        PoseStack matrices = RenderUtil.matrices();

        enableAnimation.update();
        enableAnimation.run(module.isEnabled() ? 1.0 : 0.0, 420, Easing.BACK_OUT);

        boolean over = MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight());
        hoverAnimation.update();
        hoverAnimation.run(over ? 1.0 : 0.0, 260, Easing.EXPO_OUT);

        float hover = (float) hoverAnimation.getValue();
        float enabled = (float) Math.max(0.0, Math.min(1.0, enableAnimation.getValue()));
        float alpha = getAlpha() * appear;
        int full = (int) (alpha * 255f);

        // Entrance: the row slides in from the left and settles.
        float slide = ClickGuiLayout.scaled(16f) * (1f - appear);
        float x = getX() - slide;
        float y = getY();

        if (hover > 0.01f) {
            float inset = ClickGuiLayout.scaled(7f);
            float lift = ClickGuiLayout.scaled(1f) * hover;
            RenderUtil.RECT.draw(matrices, x - inset, y - lift, getWidth() + inset * 2f, getHeight(),
                    ClickGuiLayout.scaled(9f), ColorUtil.setAlpha(UIColors.surfaceInner(), (int) (hover * alpha * 130f)));

            // Accent bar that grows out of the left edge on hover.
            float barH = getHeight() * 0.44f * hover;
            RenderUtil.RECT.draw(matrices, x - inset, y + (getHeight() - barH) / 2f, ClickGuiLayout.scaled(1.6f), barH,
                    ClickGuiLayout.scaled(0.8f), ColorUtil.setAlpha(UIColors.primary(), (int) (hover * alpha * 235f)));
        }

        float shift = ClickGuiLayout.scaled(2.5f) * hover;
        float nameSize = ClickGuiLayout.scaled(7.6f);
        float descSize = ClickGuiLayout.scaled(6.2f);

        float switchW = ClickGuiLayout.scaled(19f);
        float textLimit = getWidth() - switchW - ClickGuiLayout.scaled(10f);

        Color nameColor = ColorUtil.interpolate(
                ColorUtil.setAlpha(Color.WHITE, full),
                UIColors.inactiveTextColor((int) (full * 0.92f)), Math.max(enabled, hover * 0.55f));

        String description = bind ? bindLabel() : module.getDescription();
        Color descColor = bind
                ? ColorUtil.setAlpha(UIColors.primary(), full)
                : UIColors.inactiveTextColor((int) (full * 0.62f));

        ScissorUtil.start(matrices, x, y - ClickGuiLayout.scaled(2f), textLimit, getHeight() + ClickGuiLayout.scaled(4f));
        Fonts.PS_BOLD.drawText(matrices, module.getName(), x + shift, y + ClickGuiLayout.scaled(5f), nameSize, nameColor);
        Fonts.PS_MEDIUM.drawText(matrices, description, x + shift, y + ClickGuiLayout.scaled(15.5f), descSize, descColor);
        ScissorUtil.stop(matrices);

        renderSwitch(matrices, x + getWidth() - switchW, y + (getHeight() - ClickGuiLayout.scaled(10.5f)) / 2f, switchW, alpha, enabled, hover);
    }

    private void renderSwitch(PoseStack matrices, float x, float y, float width, float alpha, float enabled, float hover) {
        float height = ClickGuiLayout.scaled(10.5f);
        float round = height / 2f;
        int full = (int) (alpha * 255f);

        // Glow pulse fired by the last toggle-on, expanding out of the track and fading.
        if (pulseStart > 0) {
            float pulse = (System.currentTimeMillis() - pulseStart) / 520f;
            if (pulse >= 1f) {
                pulseStart = -1L;
            } else {
                float grow = ClickGuiLayout.scaled(7f) * Easing.EXPO_OUT.apply(pulse);
                int glow = (int) ((1f - pulse) * (1f - pulse) * alpha * 90f);
                RenderUtil.RECT.draw(matrices, x - grow, y - grow, width + grow * 2f, height + grow * 2f,
                        round + grow, ColorUtil.setAlpha(UIColors.primary(), glow));
            }
        }

        Color off = ColorUtil.setAlpha(UIColors.surfaceInner(), (int) ((0.85f + 0.15f * hover) * full));
        Color on = ColorUtil.setAlpha(UIColors.primary(), full);
        Color onSecond = ColorUtil.setAlpha(UIColors.secondary(), full);

        RenderUtil.RECT.draw(matrices, x, y, width, height, round, off);
        if (enabled > 0.01f) {
            Color left = ColorUtil.setAlpha(on, (int) (enabled * full));
            Color right = ColorUtil.setAlpha(onSecond, (int) (enabled * full));
            RenderUtil.GRADIENT_RECT.draw(matrices, x, y, width, height, round, left, right, left, right);
        }

        float knob = height - ClickGuiLayout.scaled(3f);
        float travel = width - knob - ClickGuiLayout.scaled(3f);
        float knobX = x + ClickGuiLayout.scaled(1.5f) + travel * (float) enableAnimation.getValue();
        knobX = Math.max(x + ClickGuiLayout.scaled(1.5f), Math.min(x + width - knob - ClickGuiLayout.scaled(1.5f), knobX));

        RenderUtil.RECT.draw(matrices, knobX, y + ClickGuiLayout.scaled(1.5f), knob, knob, knob / 2f,
                ColorUtil.setAlpha(Color.WHITE, full));
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (bind && button != 0 && button != 1 && button != 2) {
            module.setBind(-100 + button);
            bind = false;
            return;
        }
        if (!MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) return;

        if (button == 0) {
            module.toggle();
            if (module.isEnabled()) pulseStart = System.currentTimeMillis();
        }
        if (button == 2) bind = !bind;
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!bind) return;
        boolean clear = keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE;
        module.setBind(clear ? -999 : keyCode);
        bind = false;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    @Override
    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
    }

    private String bindLabel() {
        return module.getBind() == -999 ? "Нажмите клавишу..." : "Бинд — " + KeyStorage.getBind(module.getBind());
    }
}
