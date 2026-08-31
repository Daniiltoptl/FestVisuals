package com.fest.visuals.inject.input;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.fest.visuals.api.event.events.client.KeyEvent;
import com.fest.visuals.api.system.backend.SharedClass;
import com.fest.visuals.api.system.draggable.DraggableManager;
import com.fest.visuals.client.ui.clickgui.ScreenClickGUI;
import com.fest.visuals.client.features.modules.utility.ZoomModule;
import com.fest.visuals.client.ui.widget.WidgetManager;

@Mixin(MouseHandler.class)
public class MixinMouse {
    @Inject(method = "grabMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"), cancellable = true)
    private void lockCursorHook(CallbackInfo ci) {
        if (Minecraft.getInstance().gui.screen() instanceof ScreenClickGUI) {
            ci.cancel();
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void zoomScrollHook(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (ZoomModule.getInstance().onScroll(vertical)) {
            ci.cancel();
        }
    }

    @Inject(method = "onButton", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/FramerateLimitTracker;onInputReceived()V"))
    public void mousePressHook(long window, MouseButtonInfo input, int action, CallbackInfo ci) {
        if (SharedClass.player() == null) return;

        int button = input.button();
        KeyEvent.getInstance().call(new KeyEvent.KeyEventData(button, action, true));

        DraggableManager.getInstance().getDraggables().forEach((s, draggable) -> {
            if (draggable.getModule() == null || draggable.getModule().isEnabled()) {
                if (action == 0) {
                    draggable.onRelease(button);
                } else if (action == 1) {
                    draggable.onClick(button);
                }
            }
        });

        // Widget controls only take clicks while the cursor is free, otherwise they would
        // swallow attack/use in-game. Runs after the draggables so a control can cancel a drag.
        Minecraft mc = Minecraft.getInstance();
        if (action == 1 && mc.gui.screen() != null) {
            double scale = mc.getWindow().getGuiScale();
            WidgetManager.getInstance().onMouseClick(mc.mouseHandler.xpos() / scale, mc.mouseHandler.ypos() / scale, button);
        }
    }
}
