package com.fest.visuals.client.features.modules.render;

import lombok.Getter;

import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.ModeSetting;
import com.fest.visuals.api.module.setting.SliderSetting;

/**
 * Renders the world at a different aspect ratio than the window: the "stretched 4:3" look,
 * without touching the resolution or the launcher.
 *
 * <p>Only the level projection changes; the HUD and menus keep their proportions. Switching
 * eases between ratios over a short moment instead of snapping the whole picture.
 */
@ModuleRegister(name = "Aspect Ratio", desc = "Своё соотношение сторон, например растянутые 4:3", category = Category.RENDER)
public class AspectRatioModule extends Module {
    @Getter private static final AspectRatioModule instance = new AspectRatioModule();

    public final ModeSetting preset = new ModeSetting("Соотношение").value("4:3")
            .values("4:3", "5:4", "16:10", "1:1", "21:9", "Своё");
    public final SliderSetting custom = new SliderSetting("Своё значение").value(1.33f).range(0.5f, 3f).step(0.01f)
            .setVisible(() -> preset.is("Своё"));
    public final SliderSetting smoothness = new SliderSetting("Плавность").value(250f).range(0f, 1000f).step(25f);

    /** The ratio actually in use this frame, eased towards the target. Zero means "leave it". */
    private float shown;
    private long lastUpdate;

    public AspectRatioModule() {
        addSettings(preset, custom, smoothness);
    }

    @Override
    public void onEvent() {
    }

    private float target() {
        return switch (preset.getValue()) {
            case "5:4" -> 5f / 4f;
            case "16:10" -> 16f / 10f;
            case "1:1" -> 1f;
            case "21:9" -> 21f / 9f;
            case "Своё" -> custom.getValue();
            default -> 4f / 3f;
        };
    }

    private float windowRatio() {
        float height = Math.max(1, mc.getWindow().getHeight());
        return mc.getWindow().getWidth() / height;
    }

    /**
     * Called by the camera every time it sets up the projection. Returns the ratio to use, or zero
     * once the module is off and the transition back to the window ratio has finished.
     */
    public float currentRatio() {
        long now = System.currentTimeMillis();
        float dt = lastUpdate == 0 ? 1f : Math.min(0.2f, (now - lastUpdate) / 1000f);
        lastUpdate = now;

        float window = windowRatio();
        float goal = isEnabled() ? target() : window;
        if (shown <= 0f) shown = window;

        float speed = smoothness.getValue() <= 0f ? 1f : Math.min(1f, dt * 1000f / smoothness.getValue() * 3f);
        shown += (goal - shown) * speed;

        if (!isEnabled() && Math.abs(shown - window) < 0.002f) {
            shown = 0f;
            return 0f;
        }
        return shown;
    }
}
