package com.fest.visuals.client.services;

import lombok.Getter;
import lombok.Setter;
import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.other.WindowResizeEvent;
import com.fest.visuals.api.event.events.render.Render2DEvent;
import com.fest.visuals.api.system.interfaces.QuickImports;
import com.fest.visuals.api.utils.math.MathUtil;
import com.fest.visuals.api.utils.render.InterfaceConfig;

@Getter
public class RenderService implements QuickImports {
    @Getter private static final RenderService instance = new RenderService();

    @Setter private float scale = 1.0f;

    private final Listener<Render2DEvent.Render2DEventData> renderListener;

    public RenderService() {
        this.renderListener = new Listener<>(event -> {
            updateScale();
        });
    }

    public void load() {
        WindowResizeEvent.getInstance().subscribe(new Listener<>(event -> {
            register();
        }));
    }

    private void register() {
        Render2DEvent.getInstance().subscribe(renderListener);
    }

    public float scaled(float value) {
        return value * scale;
    }

    public void updateScale() {
        if (mc.getWindow() == null) return;

        float w = mc.getWindow().getGuiScaledWidth();
        float h = mc.getWindow().getGuiScaledHeight();

        float bW = 1366f / 2f;
        float bH = 768f / 2f;

        float newScale = Math.max(w / bW, h / bH) * InterfaceConfig.getScale();

        if (scale == newScale) {
            this.scale = newScale;
            Render2DEvent.getInstance().unsubscribe(renderListener);
            return;
        }

        scale = MathUtil.interpolate(scale, newScale, 0.15f);
    }
}
