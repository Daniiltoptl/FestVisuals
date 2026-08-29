package com.fest.visuals.inject.input;

import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.fest.visuals.api.event.events.client.KeyEvent;
import com.fest.visuals.api.system.backend.SharedClass;

@Mixin(KeyboardHandler.class)
public class MixinKeyboard {
    @Inject(method = "keyPress", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/FramerateLimitTracker;onInputReceived()V"))
    public void keyPressHook(long window, int action, net.minecraft.client.input.KeyEvent input, CallbackInfo ci) {
        if (SharedClass.player() == null) return;

        KeyEvent.getInstance().call(new KeyEvent.KeyEventData(input.key(), action, false));
    }
}
