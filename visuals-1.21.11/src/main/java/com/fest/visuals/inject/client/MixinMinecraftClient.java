package com.fest.visuals.inject.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.fest.visuals.FestVisuals;
import com.fest.visuals.api.event.events.client.GameLoopEvent;
import com.fest.visuals.api.event.events.client.TickEvent;
import com.fest.visuals.api.system.backend.SharedClass;
import com.fest.visuals.api.utils.framelimiter.FrameLimiter;

@Mixin(Minecraft.class)
public class MixinMinecraftClient {
    @Unique
    private final FrameLimiter frameLimiter = new FrameLimiter(false);

    @Inject(method = "runTick", at = @At("HEAD"))
    public void gameLoopHook(boolean tick, CallbackInfo ci) {
        frameLimiter.execute(60, () -> GameLoopEvent.getInstance().call());
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void preTickHook(CallbackInfo ci) {
        if (SharedClass.player() == null) return;

        TickEvent.getInstance().call();
    }

    @Inject(method = "close", at = @At("HEAD"))
    public void closeHook(CallbackInfo ci) {
        FestVisuals.getInstance().onClose();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void initHook(GameConfig args, CallbackInfo ci) {
        FestVisuals.getInstance().postLoad();
    }
}
