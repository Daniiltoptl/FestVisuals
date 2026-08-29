package com.fest.visuals.inject.other;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.fest.visuals.api.event.events.other.FramebufferResizeEvent;
import com.fest.visuals.api.event.events.other.WindowResizeEvent;

@Mixin(Window.class)
public class MixinWindow {
    @Shadow @Final private long handle;

    @Inject(method = "onResize", at = @At("RETURN"))
    public void windowResizeHook(long window, int width, int height, CallbackInfo ci) {
        WindowResizeEvent.getInstance().call();
    }

    @Inject(method = "onFramebufferResize", at = @At("RETURN"))
    public void framebufferResizeHook(long window, int width, int height, CallbackInfo callbackInfo) {
        if (window == handle) {
            FramebufferResizeEvent.getInstance().call();
        }
    }
}
