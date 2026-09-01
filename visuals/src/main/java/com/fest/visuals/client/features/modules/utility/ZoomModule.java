package com.fest.visuals.client.features.modules.utility;

import lombok.Getter;
import net.minecraft.util.Mth;

import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.render.Render2DEvent;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BindSetting;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.SliderSetting;
import com.fest.visuals.api.utils.animation.AnimationUtil;
import com.fest.visuals.api.utils.animation.Easing;

/**
 * Hold a key to zoom; scroll to change how far.
 *
 * <p>The magnification is applied to the camera field of view in {@code MixinCamera}, not to the
 * FOV option. The option validates what it is given and silently keeps the old value below its
 * floor, which capped the zoom at roughly two times and made deeper scrolling look like the view
 * springing back out.
 */
@ModuleRegister(name = "Zoom", desc = "Приближает вид по зажатой клавише", category = Category.OTHER)
public class ZoomModule extends Module {
    @Getter private static final ZoomModule instance = new ZoomModule();

    public final BindSetting key = new BindSetting("Клавиша").value(org.lwjgl.glfw.GLFW.GLFW_KEY_V);
    public final SliderSetting factor = new SliderSetting("Кратность").value(4f).range(1.5f, 30f).step(0.5f);
    public final SliderSetting smoothness = new SliderSetting("Плавность").value(340f).range(60f, 900f).step(20f);
    public final BooleanSetting scroll = new BooleanSetting("Колёсиком").value(true);
    public final BooleanSetting slowSensitivity = new BooleanSetting("Замедлять мышь").value(true);

    private final AnimationUtil zoomAnimation = new AnimationUtil();
    private final AnimationUtil factorAnimation = new AnimationUtil();

    private Double originalSensitivity;
    private boolean zooming;

    public ZoomModule() {
        addSettings(key, factor, smoothness, scroll, slowSensitivity);
    }

    @Override
    public void onEvent() {
        addEvents(Render2DEvent.getInstance().subscribe(new Listener<>(event -> update())));
    }

    @Override
    public void onDisable() {
        restore();
    }

    /** True while the key is held and the player is actually looking at the world. */
    private boolean shouldZoom() {
        if (mc.player == null || mc.level == null || mc.gui.screen() != null) return false;

        int bind = key.getValue();
        if (bind == -999) return false;

        return bind < 0
                ? org.lwjgl.glfw.GLFW.glfwGetMouseButton(mc.getWindow().handle(), bind + 100) == 1
                : com.mojang.blaze3d.platform.InputConstants.isKeyDown(mc.getWindow(), bind);
    }

    private void update() {
        boolean wanted = shouldZoom();

        if (wanted && !zooming) {
            originalSensitivity = mc.options.sensitivity().get();
            factorAnimation.setValue(factor.getValue());
            zooming = true;
        }

        if (!zooming) return;

        long duration = Math.max(1L, (long) smoothness.getValue().floatValue());

        zoomAnimation.update();
        zoomAnimation.run(wanted ? 1.0 : 0.0, duration, wanted ? Easing.EXPO_OUT : Easing.CUBIC_OUT);

        // Magnification eases too, so a scroll glides to the new zoom instead of snapping.
        factorAnimation.update();
        factorAnimation.run(factor.getValue(), duration / 2L + 1L, Easing.EXPO_OUT);

        if (!wanted && zoomAnimation.getValue() <= 0.01) {
            restore();
            return;
        }

        if (slowSensitivity.getValue() && originalSensitivity != null) {
            mc.options.sensitivity().set(originalSensitivity * (1.0 - 0.7 * zoomAnimation.getValue()));
        }
    }

    /**
     * What the camera field of view is multiplied by. One means untouched, which is what every
     * frame outside a zoom gets.
     */
    public float fovMultiplier() {
        if (!isEnabled() || !zooming) return 1f;

        float progress = (float) zoomAnimation.getValue();
        float smoothFactor = Math.max(1f, (float) factorAnimation.getValue());

        return 1f / Mth.lerp(progress, 1f, smoothFactor);
    }

    /** Scroll while zoomed changes the magnification. Called from the mouse mixin. */
    public boolean onScroll(double amount) {
        if (!isEnabled() || !zooming || !scroll.getValue()) return false;

        // Step proportionally: one notch should feel the same at 2x as it does at 20x.
        float current = factor.getValue();
        float next = current + (float) amount * Math.max(0.5f, current * 0.15f);
        factor.setValue(Mth.clamp(next, factor.getMin(), factor.getMax()));
        return true;
    }

    public boolean isZooming() {
        return zooming;
    }

    private void restore() {
        if (originalSensitivity != null) mc.options.sensitivity().set(originalSensitivity);

        originalSensitivity = null;
        zooming = false;
        zoomAnimation.setValue(0.0);
    }
}
