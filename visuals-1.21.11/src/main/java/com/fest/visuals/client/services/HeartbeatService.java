package com.fest.visuals.client.services;


import net.minecraft.client.gui.screens.ChatScreen;
import lombok.Getter;
import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.client.KeyEvent;
import com.fest.visuals.api.event.events.client.TickEvent;
import com.fest.visuals.api.event.events.render.Render2DEvent;
import com.fest.visuals.api.module.ModuleManager;
import com.fest.visuals.api.system.client.GpsManager;
import com.fest.visuals.api.system.configs.ConfigSkin;
import com.fest.visuals.api.system.configs.MacroManager;
import com.fest.visuals.api.system.draggable.DraggableManager;
import com.fest.visuals.api.system.interfaces.QuickImports;
import com.fest.visuals.api.utils.render.AdaptiveBudget;

public class HeartbeatService implements QuickImports {
    @Getter private static final HeartbeatService instance = new HeartbeatService();

    public void load() {
        keyEvent();
        render2dEvent();
        tickEvent();
    }

    private void tickEvent() {
        TickEvent.getInstance().subscribe(new Listener<>(event -> {
            ConfigSkin.getInstance().fetchSkin();
        }));
    }

    private void render2dEvent() {
        Render2DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.screen instanceof ChatScreen) {
                DraggableManager.getInstance().getDraggables().forEach((s, draggable) -> {
                    if (draggable.getModule() == null || draggable.getModule().isEnabled()) {
                        draggable.onDraw();
                    }
                });
            }

            
            int targetFps = mc.options.framerateLimit().get();
            if (targetFps >= 260) {
                targetFps = mc.getWindow().getRefreshRate();
                if (targetFps == 0) targetFps = 60;
            }
            AdaptiveBudget.getInstance().sample(mc.getFps(), targetFps);

            GpsManager.getInstance().update(event.context());
        }));
    }

    private void keyEvent() {
        KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.action() != 1 || event.key() == -999 || event.key() == -1) return;

            int action = event.action();
            int key = event.key() + (event.mouse() ? -100 : 0);

            if (mc.screen == null) {
                ModuleManager.getInstance().getModules().forEach(module -> {
                    int bind = module.getBind();
                    if (bind == key && module.hasBind()) {
                        module.toggle();
                    }
                });

                MacroManager.getInstance().onKeyPressed(key);
            }
        }));
    }
}
