package com.fest.visuals.inject.client;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.fest.visuals.api.command.CommandManager;

@Mixin(ClientPacketListener.class)
public class MixinClientPlayNetworkHandler {
    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void sendChatMessage(String content, CallbackInfo ci) {
        CommandManager.getInstance().executeCommands(content, ci);
    }
}
