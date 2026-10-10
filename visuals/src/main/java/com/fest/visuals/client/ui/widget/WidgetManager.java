package com.fest.visuals.client.ui.widget;

import lombok.Getter;
import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.render.Render2DEvent;
import com.fest.visuals.api.utils.render.McText;
import com.fest.visuals.client.ui.widget.overlay.*;

import java.util.ArrayList;
import java.util.List;

@Getter
public class WidgetManager {
    @Getter private final static WidgetManager instance = new WidgetManager();

    private final List<Widget> widgets = new ArrayList<>();

    public void load() {
        register(
                new ArmorWidget(),
                new KeybindsWidget(),
                new PotionsWidget(),
                new ScoreboardWidget(),
                new StaffsWidget(),
                new CooldownsWidget(),

                new TargetInfoWidget(),
                new DynamicIslandWidget(),
                new InventoryWidget(),

                new FPSWidget(),
                new BPSWidget(),
                new XYZWidget()
        );


        Render2DEvent.getInstance().subscribe(new Listener<>(event -> {
            // Widgets that draw with the vanilla font need this frame's extractor.
            McText.setContext(event.context());

            // Under an inventory, chest or menu the widgets would split in two: their backdrops are
            // drawn before the GUI and land under the screen, their text and bars after it and land
            // on top. Vanilla's HUD is covered there anyway, so they simply sit out until it closes.
            // Chat stays: it is where widgets are dragged around.
            var screen = net.minecraft.client.Minecraft.getInstance().gui.screen();
            if (screen != null && !(screen instanceof net.minecraft.client.gui.screens.ChatScreen)) return;

            for (Widget widget : widgets) {
                if (widget.isEnabled()) widget.render(event);
            }
        }));
    }

    public void register(Widget... widgets) {
        this.widgets.addAll(List.of(widgets));
    }

    /** Routed from MixinMouse while a screen is open, so widget controls stay clickable. */
    public void onMouseClick(double mouseX, double mouseY, int button) {
        for (Widget widget : widgets) {
            if (widget.isEnabled()) widget.onMouseClick(mouseX, mouseY, button);
        }
    }

    public Widget byName(String name) {
        for (Widget w : widgets) {
            if (w.getName().equalsIgnoreCase(name)) return w;
        }
        return null;
    }
}
