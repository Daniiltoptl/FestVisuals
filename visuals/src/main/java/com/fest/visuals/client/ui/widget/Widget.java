package com.fest.visuals.client.ui.widget;

import lombok.Getter;
import lombok.Setter;
import com.fest.visuals.api.event.events.render.Render2DEvent;
import com.fest.visuals.api.system.draggable.Draggable;
import com.fest.visuals.api.system.draggable.DraggableManager;
import com.fest.visuals.api.system.interfaces.IRenderer;
import com.fest.visuals.api.system.interfaces.QuickImports;
import com.fest.visuals.api.utils.animation.Easing;
import com.fest.visuals.api.utils.render.fonts.Font;
import com.fest.visuals.api.utils.render.fonts.Fonts;
import com.fest.visuals.client.services.RenderService;

@Getter
@Setter
public abstract class Widget implements QuickImports, IRenderer {
    protected Widget(float x, float y) {
        this.draggable = create(x, y, getName());
    }

    private final Easing easing = Easing.SINE_OUT;
    private final long duration = 100;

    public abstract String getName();
    private final Draggable draggable;
    private boolean enabled;

    private Draggable create(float x, float y, String name) {
        // No owning module: each widget is gated by its own HUD module before it renders,
        // so the draggable itself does not need one.
        return DraggableManager.getInstance().create(null, name, x, y);
    }

    public void render(Render2DEvent.Render2DEventData event) {
        render(event.matrixStack());
    }

    public float scaled(float value) {
        return RenderService.getInstance().scaled(value);
    }

    public float getScale() { return RenderService.getInstance().getScale(); }
    public float getGap() { return scaled(3f); }
    public Font getMediumFont() { return Fonts.PS_MEDIUM; }
    public Font getSemiBoldFont() { return Fonts.PS_BOLD; }

    /** Per-widget font multiplier hook. Widget subclasses override to plug in HUD-module font scale. */
    public float fontMul() { return 1f; }

    /**
     * Called for widgets with interactive controls when a mouse button goes down while a screen
     * is open (so the cursor is free). Coordinates are in GUI space.
     */
    public void onMouseClick(double mouseX, double mouseY, int button) {}
}
