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
 * <p>Works by driving the field of view option rather than by patching the projection, which
 * keeps it compatible with anything else that reads the FOV (shaders, dynamic FOV effects). The
 * player's own setting is captured when the zoom starts and put back exactly when it ends, so a
 * crash or a disable mid-zoom cannot leave the view stuck.
 *
 * <p>The bind is a module setting, not the module bind, so holding it zooms instead of toggling.
 */
@ModuleRegister(name = "Zoom", desc = "Приближает вид по зажатой клавише", category = Category.OTHER)
public class ZoomModule extends Module {
    @Getter private static final ZoomModule instance = new ZoomModule();

    public final BindSetting key = new BindSetting("Клавиша").value(org.lwjgl.glfw.GLFW.GLFW_KEY_V);
    public final SliderSetting factor = new SliderSetting("Кратность").value(4f).range(1.5f, 15f).step(0.5f);
    public final SliderSetting smoothness = new SliderSetting("Плавность").value(340f).range(60f, 900f).step(20f);
    public final BooleanSetting scroll = new BooleanSetting("Колёсиком").value(true);
    public final BooleanSetting slowSensitivity = new BooleanSetting("Замедлять мышь").value(true);

    private final AnimationUtil zoomAnimation = new AnimationUtil();
    private final AnimationUtil factorAnimation = new AnimationUtil();

    private Integer originalFov;
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

    /** True while the key is held and the player is actually in the world. */
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
            originalFov = mc.options.fov().get();
            originalSensitivity = mc.options.sensitivity().get();
            factorAnimation.setValue(factor.getValue());
            zooming = true;
        }

        if (!zooming) return;

        long duration = Math.max(1L, (long) smoothness.getValue().floatValue());

        zoomAnimation.update();
        zoomAnimation.run(wanted ? 1.0 : 0.0, duration, wanted ? Easing.EXPO_OUT : Easing.CUBIC_OUT);
        float progress = (float) zoomAnimation.getValue();

        if (!wanted && progress <= 0.01f) {
            restore();
            return;
        }

        // The magnification eases too, so a scroll glides to the new zoom instead of snapping and
        // fighting the in/out animation for the same frame.
        factorAnimation.update();
        factorAnimation.run(factor.getValue(), duration / 2L + 1L, Easing.EXPO_OUT);
        float smoothFactor = Math.max(1f, (float) factorAnimation.getValue());

        float target = originalFov / smoothFactor;
        int fov = Math.max(1, Math.round(Mth.lerp(progress, originalFov, target)));

        // Writing the same value every frame is what let another zoom mod win the tug of war;
        // only touch the option when it actually has to change.
        if (mc.options.fov().get() != fov) mc.options.fov().set(fov);

        if (slowSensitivity.getValue()) {
            double eased = originalSensitivity * (1.0 - 0.7 * progress);
            mc.options.sensitivity().set(eased);
        }
    }

    /** Scroll while zoomed changes the magnification. Called from the mouse mixin. */
    public boolean onScroll(double amount) {
        if (!isEnabled() || !zooming || !scroll.getValue()) return false;

        float next = factor.getValue() + (float) amount * 0.5f;
        factor.setValue(Mth.clamp(next, factor.getMin(), factor.getMax()));
        return true;
    }

    public boolean isZooming() {
        return zooming;
    }

    private void restore() {
        if (originalFov != null) mc.options.fov().set(originalFov);
        if (originalSensitivity != null) mc.options.sensitivity().set(originalSensitivity);

        originalFov = null;
        originalSensitivity = null;
        zooming = false;
        zoomAnimation.setValue(0.0);
    }
}
